package com.financer.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro para coleta de métricas do gateway
 */
@Component
public class MetricsFilter implements GlobalFilter, Ordered {

    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Timer requestTimer;
    private final MeterRegistry meterRegistry;

    public MetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestCounter = Counter.builder("gateway.requests.total")
            .description("Total requests")
            .register(meterRegistry);
        this.errorCounter = Counter.builder("gateway.errors.total")
            .description("Total errors")
            .register(meterRegistry);
        this.requestTimer = Timer.builder("gateway.request.duration")
            .description("Request duration")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return chain.filter(exchange)
            .doOnSuccess(aVoid -> {
                sample.stop(requestTimer);
                requestCounter.increment();
                
                var statusCode = exchange.getResponse().getStatusCode();
                if (statusCode != null && statusCode.isError()) {
                    errorCounter.increment();
                }
                
                // Métricas por serviço
                String targetService = getTargetService(exchange);
                if (targetService != null) {
                    Counter.builder("gateway.service.requests")
                        .tag("service", targetService)
                        .tag("status", statusCode != null ? statusCode.toString() : "unknown")
                        .register(meterRegistry)
                        .increment();
                }
            })
            .doOnError(throwable -> {
                sample.stop(requestTimer);
                errorCounter.increment();
            });
    }

    private String getTargetService(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().toString();
        if (path.startsWith("/api/accounts")) {
            return "account-service";
        } else if (path.startsWith("/api/transactions")) {
            return "transaction-service";
        } else if (path.startsWith("/api/orchestration")) {
            return "orchestration-service";
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 0; // Execute in the middle
    }
}