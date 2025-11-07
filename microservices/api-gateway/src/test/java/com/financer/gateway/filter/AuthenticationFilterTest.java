package com.financer.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

/**
 * Testes unitários para AuthenticationFilter
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter();
    }

    @Test
    void shouldAllowPublicEndpoints() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = authenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldRejectRequestWithoutAuthHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/accounts/private").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = authenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(chain, never()).filter(exchange);
    }

    @Test
    void shouldAcceptValidToken() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/accounts/private")
            .header("Authorization", "Bearer valid-token-123")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = authenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/accounts/private")
            .header("Authorization", "Bearer invalid-token")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = authenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(chain, never()).filter(exchange);
    }
}