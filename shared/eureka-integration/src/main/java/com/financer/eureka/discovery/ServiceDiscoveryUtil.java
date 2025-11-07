package com.financer.eureka.discovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Utilitário para descoberta e acesso a serviços via Eureka
 */
@Slf4j
@Service
@ConditionalOnClass(DiscoveryClient.class)
@RequiredArgsConstructor
public class ServiceDiscoveryUtil {
    
    private final DiscoveryClient discoveryClient;
    private final Random random = new Random();
    
    /**
     * Busca uma instância de serviço por nome
     */
    public Optional<ServiceInstance> getServiceInstance(String serviceName) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            
            if (instances == null || instances.isEmpty()) {
                log.warn("No instances found for service: {}", serviceName);
                return Optional.empty();
            }
            
            // Load balancing simples - escolhe aleatoriamente uma instância
            ServiceInstance instance = instances.get(random.nextInt(instances.size()));
            log.debug("Selected instance for service '{}': {}:{}", 
                     serviceName, instance.getHost(), instance.getPort());
            
            return Optional.of(instance);
            
        } catch (Exception e) {
            log.error("Error discovering service instance for: {}", serviceName, e);
            return Optional.empty();
        }
    }
    
    /**
     * Busca todas as instâncias de um serviço
     */
    public List<ServiceInstance> getAllServiceInstances(String serviceName) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            log.debug("Found {} instances for service: {}", 
                     instances != null ? instances.size() : 0, serviceName);
            return instances;
        } catch (Exception e) {
            log.error("Error discovering service instances for: {}", serviceName, e);
            return List.of();
        }
    }
    
    /**
     * Constrói URL base para um serviço
     */
    public Optional<String> getServiceBaseUrl(String serviceName) {
        return getServiceInstance(serviceName)
                .map(instance -> String.format("http://%s:%d", 
                                              instance.getHost(), instance.getPort()));
    }
    
    /**
     * Constrói URL completa para um endpoint de serviço
     */
    public Optional<String> getServiceUrl(String serviceName, String path) {
        return getServiceBaseUrl(serviceName)
                .map(baseUrl -> {
                    String cleanPath = path.startsWith("/") ? path : "/" + path;
                    return baseUrl + cleanPath;
                });
    }
    
    /**
     * Lista todos os serviços disponíveis
     */
    public List<String> getAvailableServices() {
        try {
            List<String> services = discoveryClient.getServices();
            log.debug("Available services: {}", services);
            return services;
        } catch (Exception e) {
            log.error("Error retrieving available services", e);
            return List.of();
        }
    }
    
    /**
     * Verifica se um serviço está disponível
     */
    public boolean isServiceAvailable(String serviceName) {
        return !getAllServiceInstances(serviceName).isEmpty();
    }
    
    /**
     * Obtém informações detalhadas sobre um serviço
     */
    public ServiceInfo getServiceInfo(String serviceName) {
        List<ServiceInstance> instances = getAllServiceInstances(serviceName);
        
        return ServiceInfo.builder()
                .name(serviceName)
                .instanceCount(instances.size())
                .available(!instances.isEmpty())
                .instances(instances)
                .build();
    }
    
    /**
     * Informações sobre um serviço
     */
    @lombok.Data
    @lombok.Builder
    public static class ServiceInfo {
        private String name;
        private int instanceCount;
        private boolean available;
        private List<ServiceInstance> instances;
    }
}