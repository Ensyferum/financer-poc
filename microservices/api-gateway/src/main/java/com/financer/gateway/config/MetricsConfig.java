package com.financer.gateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de métricas customizadas para o API Gateway
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter requestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("gateway.requests.total")
            .description("Total number of requests through the gateway")
            .register(meterRegistry);
    }

    @Bean
    public Counter errorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("gateway.errors.total")
            .description("Total number of errors in the gateway")
            .register(meterRegistry);
    }

    @Bean
    public Counter authenticationFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("gateway.auth.failures.total")
            .description("Total number of authentication failures")
            .register(meterRegistry);
    }

    @Bean
    public Counter maliciousRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("gateway.security.blocked.total")
            .description("Total number of blocked malicious requests")
            .register(meterRegistry);
    }

    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("gateway.request.duration")
            .description("Request processing time")
            .register(meterRegistry);
    }
}