package com.financer.eureka.functional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Service Discovery Utilities
 * 
 * Implementa padrões funcionais para descoberta de serviços com operações
 * composáveis, tolerância a falhas e programação assíncrona.
 */
@Slf4j
public final class FunctionalServiceDiscovery {
    
    private final DiscoveryClient discoveryClient;
    
    public FunctionalServiceDiscovery(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }
    
    /**
     * Busca serviço com fallback funcional
     */
    public <T> Optional<T> findServiceAndApply(
            String serviceName,
            Function<ServiceInstance, T> mapper,
            Supplier<T> fallback) {
        
        return findService(serviceName)
                .map(mapper)
                .or(() -> {
                    log.warn("Service '{}' not found, using fallback", serviceName);
                    return Optional.ofNullable(fallback.get());
                });
    }
    
    /**
     * Busca serviço com filtro customizado
     */
    public Optional<ServiceInstance> findServiceWithFilter(
            String serviceName,
            Predicate<ServiceInstance> filter) {
        
        return findAllServices(serviceName)
                .stream()
                .filter(filter)
                .findFirst();
    }
    
    /**
     * Busca assíncrona de serviço
     */
    public CompletableFuture<Optional<ServiceInstance>> findServiceAsync(String serviceName) {
        return CompletableFuture.supplyAsync(() -> findService(serviceName))
                .exceptionally(throwable -> {
                    log.error("Async service discovery failed for: {}", serviceName, throwable);
                    return Optional.empty();
                });
    }
    
    /**
     * Aplica função a todas as instâncias de um serviço
     */
    public <T> List<T> mapAllInstances(String serviceName, Function<ServiceInstance, T> mapper) {
        return findAllServices(serviceName)
                .stream()
                .map(mapper)
                .toList();
    }
    
    /**
     * Conta instâncias que atendem a um critério
     */
    public long countInstancesWhere(String serviceName, Predicate<ServiceInstance> predicate) {
        return findAllServices(serviceName)
                .stream()
                .filter(predicate)
                .count();
    }
    
    /**
     * Verifica se existe pelo menos uma instância que atende ao critério
     */
    public boolean anyInstanceMatches(String serviceName, Predicate<ServiceInstance> predicate) {
        return findAllServices(serviceName)
                .stream()
                .anyMatch(predicate);
    }
    
    /**
     * Verifica se todas as instâncias atendem ao critério
     */
    public boolean allInstancesMatch(String serviceName, Predicate<ServiceInstance> predicate) {
        return findAllServices(serviceName)
                .stream()
                .allMatch(predicate);
    }
    
    /**
     * Encontra a primeira instância disponível ou retorna Optional.empty()
     */
    public Optional<ServiceInstance> findService(String serviceName) {
        try {
            return findAllServices(serviceName)
                    .stream()
                    .findFirst();
        } catch (Exception e) {
            log.error("Error finding service: {}", serviceName, e);
            return Optional.empty();
        }
    }
    
    /**
     * Encontra todas as instâncias de um serviço
     */
    public List<ServiceInstance> findAllServices(String serviceName) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            return instances != null ? instances : List.of();
        } catch (Exception e) {
            log.error("Error finding all instances for service: {}", serviceName, e);
            return List.of();
        }
    }
    
    /**
     * Predicados úteis para filtragem
     */
    public static final class Predicates {
        
        public static Predicate<ServiceInstance> hasPort(int port) {
            return instance -> instance.getPort() == port;
        }
        
        public static Predicate<ServiceInstance> hasHost(String host) {
            return instance -> host.equals(instance.getHost());
        }
        
        public static Predicate<ServiceInstance> isSecure() {
            return instance -> instance.isSecure();
        }
        
        public static Predicate<ServiceInstance> hasMetadata(String key, String value) {
            return instance -> value.equals(instance.getMetadata().get(key));
        }
        
        public static Predicate<ServiceInstance> isHealthy() {
            return instance -> {
                // Implementar verificação de saúde se necessário
                return true; // Por padrão, assume que está saudável
            };
        }
        
        private Predicates() {
            // Utility class
        }
    }
    
    /**
     * Mappers úteis para transformação
     */
    public static final class Mappers {
        
        public static Function<ServiceInstance, String> toUrl() {
            return instance -> String.format("http://%s:%d", 
                                            instance.getHost(), instance.getPort());
        }
        
        public static Function<ServiceInstance, String> toSecureUrl() {
            return instance -> String.format("https://%s:%d", 
                                            instance.getHost(), instance.getPort());
        }
        
        public static Function<ServiceInstance, String> toHost() {
            return ServiceInstance::getHost;
        }
        
        public static Function<ServiceInstance, Integer> toPort() {
            return ServiceInstance::getPort;
        }
        
        public static Function<ServiceInstance, String> toServiceId() {
            return ServiceInstance::getServiceId;
        }
        
        private Mappers() {
            // Utility class
        }
    }
}