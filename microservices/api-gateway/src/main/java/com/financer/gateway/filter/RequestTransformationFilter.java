package com.financer.gateway.filter;

import com.financer.shared.logging.Domain;
import com.financer.shared.logging.ExecutionStep;
import com.financer.shared.logging.FinancerLogger;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Filtro para sanitização e transformação de requisições
 */
@Component
public class RequestTransformationFilter implements GlobalFilter, Ordered {

    private static final FinancerLogger logger = FinancerLogger.getLogger(RequestTransformationFilter.class);

    // Padrões para detectar tentativas de XSS
    private static final Pattern XSS_PATTERN = Pattern.compile(
        ".*(<script.*?>.*?</script>|javascript:|vbscript:|onload=|onerror=|onclick=)", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Padrões para detectar tentativas de SQL Injection
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s+.*",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Validar parâmetros de query
        String queryString = request.getURI().getQuery();
        if (queryString != null && containsMaliciousContent(queryString)) {
            logger.warn(ExecutionStep.VALIDATION, 
                "Malicious content detected in query parameters: {}", queryString);
            return handleMaliciousRequest(exchange);
        }

        // Validar headers suspeitos
        if (containsSuspiciousHeaders(request)) {
            logger.warn(ExecutionStep.VALIDATION, 
                "Suspicious headers detected from IP: {}", getClientIp(request));
            return handleMaliciousRequest(exchange);
        }

        // Para requisições POST/PUT, validar o body
        if (shouldValidateBody(request)) {
            return validateAndTransformBody(exchange, chain);
        }

        return chain.filter(exchange);
    }

    private boolean containsMaliciousContent(String content) {
        return XSS_PATTERN.matcher(content).matches() || 
               SQL_INJECTION_PATTERN.matcher(content).matches();
    }

    private boolean containsSuspiciousHeaders(ServerHttpRequest request) {
        // Verificar headers suspeitos
        String userAgent = request.getHeaders().getFirst("User-Agent");
        if (userAgent != null && userAgent.toLowerCase().contains("bot")) {
            return true;
        }

        // Verificar tentativas de bypass de CORS
        String origin = request.getHeaders().getFirst("Origin");
        if (origin != null && !isAllowedOrigin(origin)) {
            return true;
        }

        return false;
    }

    private boolean isAllowedOrigin(String origin) {
        // Lista de origens permitidas - em produção vir de configuração
        return origin.contains("financer.com") || 
               origin.contains("localhost") ||
               origin.contains("127.0.0.1");
    }

    private boolean shouldValidateBody(ServerHttpRequest request) {
        String method = request.getMethod().name();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private Mono<Void> validateAndTransformBody(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

        ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody().map(dataBuffer -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    String body = new String(content, StandardCharsets.UTF_8);

                    // Sanitizar o conteúdo
                    String sanitizedBody = sanitizeContent(body);
                    
                    if (!body.equals(sanitizedBody)) {
                        logger.info(ExecutionStep.TRANSFORMATION, 
                            "Request body sanitized for path: {}", request.getPath());
                    }

                    return bufferFactory.wrap(sanitizedBody.getBytes(StandardCharsets.UTF_8));
                });
            }
        };

        return chain.filter(exchange.mutate().request(decorator).build());
    }

    private String sanitizeContent(String content) {
        if (content == null) return content;

        // Remove caracteres potencialmente perigosos
        content = content.replaceAll("<script[^>]*>.*?</script>", "");
        content = content.replaceAll("javascript:", "");
        content = content.replaceAll("vbscript:", "");
        content = content.replaceAll("onload=", "");
        content = content.replaceAll("onerror=", "");
        content = content.replaceAll("onclick=", "");

        // Escapar caracteres HTML básicos
        content = content.replace("<", "&lt;");
        content = content.replace(">", "&gt;");
        content = content.replace("\"", "&quot;");
        content = content.replace("'", "&#x27;");

        return content;
    }

    private Mono<Void> handleMaliciousRequest(ServerWebExchange exchange) {
        // Log da tentativa de ataque
        ServerHttpRequest request = exchange.getRequest();
        logger.warn(ExecutionStep.SECURITY, 
            "Malicious request blocked - IP: {}, Path: {}, User-Agent: {}", 
            getClientIp(request),
            request.getPath(),
            request.getHeaders().getFirst("User-Agent"));

        // Retornar 403 Forbidden
        exchange.getResponse().setRawStatusCode(403);
        return exchange.getResponse().setComplete();
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return -3; // Execute before authentication
    }
}