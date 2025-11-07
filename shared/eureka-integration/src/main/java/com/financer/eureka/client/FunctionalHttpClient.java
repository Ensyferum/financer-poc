package com.financer.eureka.client;

import com.financer.eureka.functional.FunctionalServiceDiscovery;
import com.financer.eureka.loadbalancer.FunctionalLoadBalancer;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Cliente HTTP Funcional com Service Discovery
 * 
 * Integra descoberta de serviços, load balancing e chamadas HTTP
 * em uma interface funcional e composável.
 */
@Slf4j
public class FunctionalHttpClient {
    
    private final FunctionalServiceDiscovery serviceDiscovery;
    private final FunctionalLoadBalancer loadBalancer;
    private final RestTemplate restTemplate;
    private final Duration defaultTimeout;
    
    public FunctionalHttpClient(DiscoveryClient discoveryClient, 
                               FunctionalLoadBalancer loadBalancer,
                               RestTemplate restTemplate) {
        this.serviceDiscovery = new FunctionalServiceDiscovery(discoveryClient);
        this.loadBalancer = loadBalancer;
        this.restTemplate = restTemplate;
        this.defaultTimeout = Duration.ofSeconds(30);
    }
    
    /**
     * Builder para criar requests funcionais
     */
    public RequestBuilder to(String serviceName) {
        return new RequestBuilder(serviceName);
    }
    
    /**
     * Builder fluente para construção de requests
     */
    public class RequestBuilder {
        private final String serviceName;
        private String path = "";
        private HttpMethod method = HttpMethod.GET;
        private HttpHeaders headers = new HttpHeaders();
        private Object body;
        private Class<?> responseType = String.class;
        private Duration timeout = defaultTimeout;
        private Supplier<Object> fallback = () -> null;
        
        public RequestBuilder(String serviceName) {
            this.serviceName = serviceName;
        }
        
        public RequestBuilder path(String path) {
            this.path = path != null ? path : "";
            return this;
        }
        
        public RequestBuilder get() {
            this.method = HttpMethod.GET;
            return this;
        }
        
        public RequestBuilder post() {
            this.method = HttpMethod.POST;
            return this;
        }
        
        public RequestBuilder put() {
            this.method = HttpMethod.PUT;
            return this;
        }
        
        public RequestBuilder delete() {
            this.method = HttpMethod.DELETE;
            return this;
        }
        
        public RequestBuilder method(HttpMethod method) {
            this.method = method;
            return this;
        }
        
        public RequestBuilder header(String name, String value) {
            this.headers.add(name, value);
            return this;
        }
        
        public RequestBuilder headers(Map<String, String> headers) {
            headers.forEach(this.headers::add);
            return this;
        }
        
        public RequestBuilder contentType(MediaType contentType) {
            this.headers.setContentType(contentType);
            return this;
        }
        
        public RequestBuilder accept(MediaType... mediaTypes) {
            this.headers.setAccept(java.util.Arrays.asList(mediaTypes));
            return this;
        }
        
        public RequestBuilder body(Object body) {
            this.body = body;
            return this;
        }
        
        public RequestBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public <T> RequestBuilder expectingType(Class<T> responseType) {
            this.responseType = responseType;
            return this;
        }
        
        public <T> RequestBuilder fallback(Supplier<T> fallback) {
            this.fallback = (Supplier<Object>) fallback;
            return this;
        }
        
        /**
         * Executa a requisição de forma síncrona
         */
        @SuppressWarnings("unchecked")
        public <T> Optional<T> execute() {
            try {
                return findServiceInstance()
                        .map(this::buildUrl)
                        .map(url -> makeRequest(url, (Class<T>) responseType))
                        .or(() -> {
                            log.warn("Service '{}' not available, using fallback", serviceName);
                            return Optional.ofNullable((T) fallback.get());
                        });
                        
            } catch (Exception e) {
                log.error("Error executing request to service '{}': {}", serviceName, e.getMessage());
                return Optional.ofNullable((T) fallback.get());
            }
        }
        
        /**
         * Executa a requisição de forma assíncrona
         */
        public <T> CompletableFuture<Optional<T>> executeAsync() {
            return CompletableFuture.supplyAsync(this::<T>execute)
                    .exceptionally(throwable -> {
                        log.error("Async request failed for service '{}': {}", serviceName, throwable.getMessage());
                        return Optional.ofNullable((T) fallback.get());
                    });
        }
        
        /**
         * Executa e retorna resultado com informações detalhadas
         */
        @SuppressWarnings("unchecked")
        public <T> ServiceCallResult<T> executeWithDetails() {
            long startTime = System.currentTimeMillis();
            
            try {
                Optional<ServiceInstance> instanceOpt = findServiceInstance();
                
                if (instanceOpt.isEmpty()) {
                    return ServiceCallResult.<T>builder()
                            .success(false)
                            .serviceName(serviceName)
                            .error("Service not found")
                            .duration(Duration.ofMillis(System.currentTimeMillis() - startTime))
                            .fallbackUsed(true)
                            .result((T) fallback.get())
                            .build();
                }
                
                ServiceInstance instance = instanceOpt.get();
                String url = buildUrl(instance);
                
                T result = makeRequest(url, (Class<T>) responseType);
                
                return ServiceCallResult.<T>builder()
                        .success(true)
                        .serviceName(serviceName)
                        .instanceUsed(instance)
                        .url(url)
                        .duration(Duration.ofMillis(System.currentTimeMillis() - startTime))
                        .result(result)
                        .build();
                        
            } catch (Exception e) {
                log.error("Detailed request failed for service '{}': {}", serviceName, e.getMessage());
                
                return ServiceCallResult.<T>builder()
                        .success(false)
                        .serviceName(serviceName)
                        .error(e.getMessage())
                        .duration(Duration.ofMillis(System.currentTimeMillis() - startTime))
                        .fallbackUsed(true)
                        .result((T) fallback.get())
                        .build();
            }
        }
        
        private Optional<ServiceInstance> findServiceInstance() {
            return loadBalancer.selectInstance(
                    serviceDiscovery.findAllServices(serviceName)
            );
        }
        
        private String buildUrl(ServiceInstance instance) {
            String baseUrl = String.format("http://%s:%d", instance.getHost(), instance.getPort());
            String cleanPath = path.startsWith("/") ? path : "/" + path;
            return baseUrl + cleanPath;
        }
        
        private <T> T makeRequest(String url, Class<T> responseType) {
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            
            log.debug("Making {} request to: {}", method, url);
            
            ResponseEntity<T> response = restTemplate.exchange(
                    url, method, entity, responseType
            );
            
            log.debug("Response status: {}", response.getStatusCode());
            return response.getBody();
        }
    }
    
    /**
     * Resultado de uma chamada de serviço com detalhes
     */
    @Data
    @Builder
    public static class ServiceCallResult<T> {
        private boolean success;
        private String serviceName;
        private ServiceInstance instanceUsed;
        private String url;
        private Duration duration;
        private String error;
        private boolean fallbackUsed;
        private T result;
    }
    
    /**
     * Factory methods para criação conveniente
     */
    public static FunctionalHttpClient create(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        return new FunctionalHttpClient(discoveryClient, FunctionalLoadBalancer.roundRobin(), restTemplate);
    }
    
    public static FunctionalHttpClient createWithRandomLoadBalancing(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        return new FunctionalHttpClient(discoveryClient, FunctionalLoadBalancer.random(), restTemplate);
    }
    
    public static FunctionalHttpClient createWithWeightedLoadBalancing(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        return new FunctionalHttpClient(discoveryClient, FunctionalLoadBalancer.weighted(), restTemplate);
    }
}