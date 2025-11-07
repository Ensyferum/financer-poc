package com.financer.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro para adicionar headers de segurança nas respostas
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            
            // Headers de segurança
            response.getHeaders().add("X-Content-Type-Options", "nosniff");
            response.getHeaders().add("X-Frame-Options", "DENY");
            response.getHeaders().add("X-XSS-Protection", "1; mode=block");
            response.getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            response.getHeaders().add("Content-Security-Policy", "default-src 'self'");
            response.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
            response.getHeaders().add("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            
            // Cache control para APIs
            if (exchange.getRequest().getPath().toString().startsWith("/api/")) {
                response.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
                response.getHeaders().add("Pragma", "no-cache");
                response.getHeaders().add("Expires", "0");
            }
        }));
    }

    @Override
    public int getOrder() {
        return 1; // Execute after main processing
    }
}