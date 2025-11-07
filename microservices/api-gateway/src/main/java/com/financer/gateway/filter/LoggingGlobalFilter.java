package com.financer.gateway.filter;

import com.financer.shared.logging.Domain;
import com.financer.shared.logging.ExecutionStep;
import com.financer.shared.logging.FinancerLogger;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtro global para adicionar correlation ID e logging
 */
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final FinancerLogger logger = FinancerLogger.getLogger(LoggingGlobalFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Gerar ou recuperar correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Recuperar user ID se presente
        String userId = request.getHeaders().getFirst(USER_ID_HEADER);
        
        // Iniciar contexto de log
        logger.startContext(Domain.INFRASTRUCTURE, "gateway-routing", userId);
        
        // Log da requisição
        logger.info(ExecutionStep.START, 
            "Routing request - Method: {}, Path: {}, Target: {}", 
            request.getMethod(), 
            request.getPath(), 
            getTargetService(request.getPath().toString()));
        
        // Adicionar correlation ID no header da requisição
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(CORRELATION_ID_HEADER, correlationId)
            .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build();
        
        // Adicionar correlation ID na resposta
        mutatedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        
        return chain.filter(mutatedExchange)
            .doOnSuccess(aVoid -> {
                logger.info(ExecutionStep.FINISH, 
                    "Request completed - Status: {}", 
                    mutatedExchange.getResponse().getStatusCode());
                logger.clearContext();
            })
            .doOnError(throwable -> {
                logger.error(ExecutionStep.ERROR, 
                    "Request failed: {}", 
                    throwable, 
                    throwable.getMessage());
                logger.clearContext();
            });
    }
    
    private String getTargetService(String path) {
        if (path.startsWith("/api/accounts")) {
            return "account-service";
        } else if (path.startsWith("/api/transactions")) {
            return "transaction-service";
        } else if (path.startsWith("/api/orchestration")) {
            return "orchestration-service";
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }
}