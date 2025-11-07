package com.financer.eureka.health;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Health Monitor para serviços descobertos via Eureka
 * 
 * Monitora a saúde dos serviços registrados e fornece informações
 * sobre disponibilidade, latência e status dos serviços.
 */
@Slf4j
@Component
public class ServiceHealthMonitor {
    
    private final DiscoveryClient discoveryClient;
    private final Map<String, ServiceHealthStatus> healthCache = new ConcurrentHashMap<>();
    private final Duration cacheTimeout = Duration.ofMinutes(1);
    
    public ServiceHealthMonitor(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }
    
    /**
     * Verifica a saúde geral do sistema de serviços
     */
    public SystemHealthReport getSystemHealth() {
        Map<String, Object> details = new ConcurrentHashMap<>();
        
        try {
            List<String> services = discoveryClient.getServices();
            Map<String, ServiceHealthSummary> healthSummaries = services.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            this::getServiceHealthSummary
                    ));
            
            details.put("totalServices", services.size());
            details.put("services", healthSummaries);
            
            long healthyServices = healthSummaries.values().stream()
                    .mapToLong(summary -> summary.getHealthyInstances())
                    .sum();
            
            long totalInstances = healthSummaries.values().stream()
                    .mapToLong(summary -> summary.getTotalInstances())
                    .sum();
            
            details.put("totalInstances", totalInstances);
            details.put("healthyInstances", healthyServices);
            details.put("healthPercentage", 
                       totalInstances > 0 ? (healthyServices * 100.0 / totalInstances) : 100.0);
            
            // Considera saudável se pelo menos 80% das instâncias estão funcionando
            boolean isHealthy = totalInstances == 0 || (healthyServices * 100.0 / totalInstances) >= 80.0;
            
            return SystemHealthReport.builder()
                    .healthy(isHealthy)
                    .totalServices(services.size())
                    .totalInstances((int) totalInstances)
                    .healthyInstances((int) healthyServices)
                    .healthPercentage(totalInstances > 0 ? (healthyServices * 100.0 / totalInstances) : 100.0)
                    .services(healthSummaries)
                    .lastCheck(Instant.now())
                    .build();
                   
        } catch (Exception e) {
            log.error("Error checking system health", e);
            return SystemHealthReport.builder()
                    .healthy(false)
                    .error(e.getMessage())
                    .lastCheck(Instant.now())
                    .build();
        }
    }
    
    /**
     * Obtém sumário de saúde para um serviço específico
     */
    public ServiceHealthSummary getServiceHealthSummary(String serviceName) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            
            if (instances == null || instances.isEmpty()) {
                return ServiceHealthSummary.builder()
                        .serviceName(serviceName)
                        .totalInstances(0)
                        .healthyInstances(0)
                        .status(ServiceStatus.NOT_FOUND)
                        .lastCheck(Instant.now())
                        .build();
            }
            
            // Em uma implementação real, verificaria health checks de cada instância
            long healthyCount = instances.stream()
                    .mapToLong(instance -> isInstanceHealthy(instance) ? 1 : 0)
                    .sum();
            
            ServiceStatus status = determineServiceStatus(instances.size(), healthyCount);
            
            return ServiceHealthSummary.builder()
                    .serviceName(serviceName)
                    .totalInstances(instances.size())
                    .healthyInstances((int) healthyCount)
                    .status(status)
                    .lastCheck(Instant.now())
                    .instances(instances.stream()
                              .map(this::createInstanceHealth)
                              .toList())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting health summary for service: {}", serviceName, e);
            return ServiceHealthSummary.builder()
                    .serviceName(serviceName)
                    .totalInstances(0)
                    .healthyInstances(0)
                    .status(ServiceStatus.ERROR)
                    .lastCheck(Instant.now())
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Verifica saúde de uma instância específica
     */
    private boolean isInstanceHealthy(ServiceInstance instance) {
        String key = instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort();
        
        ServiceHealthStatus cached = healthCache.get(key);
        if (cached != null && cached.isValid(cacheTimeout)) {
            return cached.isHealthy();
        }
        
        // Em uma implementação real, faria uma chamada HTTP para /actuator/health
        // Por enquanto, assume que instâncias registradas estão saudáveis
        boolean healthy = true;
        
        healthCache.put(key, new ServiceHealthStatus(healthy, Instant.now()));
        return healthy;
    }
    
    /**
     * Cria informações de saúde para uma instância
     */
    private InstanceHealth createInstanceHealth(ServiceInstance instance) {
        return InstanceHealth.builder()
                .host(instance.getHost())
                .port(instance.getPort())
                .serviceId(instance.getServiceId())
                .healthy(isInstanceHealthy(instance))
                .metadata(instance.getMetadata())
                .lastCheck(Instant.now())
                .build();
    }
    
    /**
     * Determina status do serviço baseado nas instâncias
     */
    private ServiceStatus determineServiceStatus(int total, long healthy) {
        if (total == 0) {
            return ServiceStatus.NOT_FOUND;
        }
        
        double healthPercentage = (healthy * 100.0) / total;
        
        if (healthPercentage == 100.0) {
            return ServiceStatus.HEALTHY;
        } else if (healthPercentage >= 50.0) {
            return ServiceStatus.DEGRADED;
        } else {
            return ServiceStatus.UNHEALTHY;
        }
    }
    
    /**
     * Monitora saúde de forma assíncrona
     */
    public CompletableFuture<ServiceHealthSummary> monitorServiceAsync(String serviceName) {
        return CompletableFuture.supplyAsync(() -> getServiceHealthSummary(serviceName))
                .exceptionally(throwable -> {
                    log.error("Async health monitoring failed for: {}", serviceName, throwable);
                    return ServiceHealthSummary.builder()
                            .serviceName(serviceName)
                            .status(ServiceStatus.ERROR)
                            .error(throwable.getMessage())
                            .lastCheck(Instant.now())
                            .build();
                });
    }
    
    /**
     * Status de saúde de um serviço
     */
    public enum ServiceStatus {
        HEALTHY, DEGRADED, UNHEALTHY, NOT_FOUND, ERROR
    }
    
    /**
     * Relatório de saúde do sistema
     */
    @Data
    @Builder
    public static class SystemHealthReport {
        private boolean healthy;
        private int totalServices;
        private int totalInstances;
        private int healthyInstances;
        private double healthPercentage;
        private Map<String, ServiceHealthSummary> services;
        private String error;
        private Instant lastCheck;
    }
    
    /**
     * Sumário de saúde de um serviço
     */
    @Data
    @Builder
    public static class ServiceHealthSummary {
        private String serviceName;
        private int totalInstances;
        private int healthyInstances;
        private ServiceStatus status;
        private Instant lastCheck;
        private String error;
        private List<InstanceHealth> instances;
    }
    
    /**
     * Informações de saúde de uma instância
     */
    @Data
    @Builder
    public static class InstanceHealth {
        private String host;
        private int port;
        private String serviceId;
        private boolean healthy;
        private Map<String, String> metadata;
        private Instant lastCheck;
        private String error;
    }
    
    /**
     * Cache de status de saúde
     */
    private static class ServiceHealthStatus {
        private final boolean healthy;
        private final Instant timestamp;
        
        public ServiceHealthStatus(boolean healthy, Instant timestamp) {
            this.healthy = healthy;
            this.timestamp = timestamp;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public boolean isValid(Duration timeout) {
            return Instant.now().isBefore(timestamp.plus(timeout));
        }
    }
}