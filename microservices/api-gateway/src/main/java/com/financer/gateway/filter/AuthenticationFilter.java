package com.financer.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financer.shared.dto.ApiResponse;
import com.financer.shared.logging.Domain;
import com.financer.shared.logging.ExecutionStep;
import com.financer.shared.logging.FinancerLogger;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

/**
 * Filtro de autenticação e autorização baseado em JWT
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final FinancerLogger logger = FinancerLogger.getLogger(AuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Endpoints que não precisam de autenticação
    private final List<String> openEndpoints = List.of(
        "/actuator/health",
        "/actuator/info",
        "/api/accounts/public",
        "/api/auth/login",
        "/api/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // Verificar se é endpoint público
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Extrair token JWT
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn(ExecutionStep.VALIDATION, "Missing or invalid authorization header for path: {}", path);
            return onError(exchange, "Authentication required", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        
        try {
            // Validar token JWT (simulado - em produção usar biblioteca JWT real)
            JwtClaims claims = validateJwtToken(token);
            
            // Adicionar informações do usuário aos headers
            ServerHttpRequest mutatedRequest = request.mutate()
                .header(USER_ID_HEADER, claims.getUserId())
                .header(USER_ROLE_HEADER, claims.getRole())
                .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

            logger.info(ExecutionStep.AUTHORIZATION, 
                "User authenticated - ID: {}, Role: {}, Path: {}", 
                claims.getUserId(), claims.getRole(), path);

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            logger.error(ExecutionStep.ERROR, "JWT validation failed: {}", e, e.getMessage());
            return onError(exchange, "Invalid token", HttpStatus.FORBIDDEN);
        }
    }

    private boolean isOpenEndpoint(String path) {
        return openEndpoints.stream().anyMatch(path::startsWith);
    }

    private JwtClaims validateJwtToken(String token) {
        // Simulação de validação JWT - em produção usar biblioteca real como jjwt
        // Esta é uma implementação simplificada para demonstração
        if ("valid-token-123".equals(token)) {
            return new JwtClaims("user-001", "USER", "john.doe@financer.com");
        } else if ("admin-token-456".equals(token)) {
            return new JwtClaims("admin-001", "ADMIN", "admin@financer.com");
        }
        throw new RuntimeException("Invalid token");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        ApiResponse<Void> errorResponse = ApiResponse.<Void>builder()
            .success(false)
            .message(message)
            .build();

        try {
            String body = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            logger.error(ExecutionStep.ERROR, "Error serializing error response", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -2; // Execute before logging filter
    }

    /**
     * Claims extraídos do JWT token
     */
    private static class JwtClaims {
        private final String userId;
        private final String role;
        private final String email;

        public JwtClaims(String userId, String role, String email) {
            this.userId = userId;
            this.role = role;
            this.email = email;
        }

        public String getUserId() { return userId; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
    }
}