package com.financer.eureka.config;

import com.financer.eureka.client.FunctionalHttpClient;
import com.financer.eureka.functional.FunctionalServiceDiscovery;
import com.financer.eureka.health.ServiceHealthMonitor;
import com.financer.eureka.loadbalancer.FunctionalLoadBalancer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import com.netflix.discovery.EurekaClient;

import jakarta.annotation.PostConstruct;

/**
 * Configuração automática para integração com Eureka
 * 
 * Configura automaticamente todos os componentes funcionais para
 * service discovery, load balancing e monitoramento de saúde.
 */
@Slf4j
@AutoConfiguration(before = EurekaClientAutoConfiguration.class)
@ConditionalOnClass(EurekaClient.class)
@EnableConfigurationProperties(EurekaIntegrationProperties.class)
@RequiredArgsConstructor
public class EurekaIntegrationAutoConfiguration {
    
    private final EurekaIntegrationProperties properties;
    private final Environment environment;
    
    @PostConstruct
    public void logConfiguration() {
        log.info("========================================");
        log.info("  Financer Eureka Integration Library");
        log.info("  Enhanced with Functional Patterns");
        log.info("========================================");
        log.info("Eureka Server URL: {}", getEurekaUrl());
        log.info("Lease Renewal Interval: {}s", properties.getLeaseRenewalInterval());
        log.info("Lease Expiration Duration: {}s", properties.getLeaseExpirationDuration());
        log.info("Register with Eureka: {}", properties.getRegisterWithEureka());
        log.info("Fetch Registry: {}", properties.getFetchRegistry());
        log.info("Health Check Enabled: {}", properties.getEnableHealthCheck());
        log.info("Load Balancing Strategy: Round Robin (default)");
        
        if (isDockerEnvironment()) {
            log.info("Docker Environment Detected");
            log.info("Docker Hostname: {}", properties.getDocker().getHostname());
            log.info("Docker Prefer IP: {}", properties.getDocker().getPreferIpAddress());
        }
        log.info("========================================");
    }
    
    /**
     * Configurador customizado do Eureka
     */
    @Bean
    @ConditionalOnMissingBean
    public EurekaConfigurationCustomizer eurekaConfigurationCustomizer() {
        return new EurekaConfigurationCustomizer(properties, environment);
    }
    
    /**
     * Service Discovery funcional
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(DiscoveryClient.class)
    public FunctionalServiceDiscovery functionalServiceDiscovery(DiscoveryClient discoveryClient) {
        log.info("Creating FunctionalServiceDiscovery bean");
        return new FunctionalServiceDiscovery(discoveryClient);
    }
    
    /**
     * Load Balancer funcional com estratégia round-robin por padrão
     */
    @Bean
    @ConditionalOnMissingBean
    public FunctionalLoadBalancer functionalLoadBalancer() {
        log.info("Creating FunctionalLoadBalancer bean with round-robin strategy");
        return FunctionalLoadBalancer.roundRobin();
    }
    
    /**
     * Monitor de saúde dos serviços
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(DiscoveryClient.class)
    public ServiceHealthMonitor serviceHealthMonitor(DiscoveryClient discoveryClient) {
        log.info("Creating ServiceHealthMonitor bean");
        return new ServiceHealthMonitor(discoveryClient);
    }
    
    /**
     * RestTemplate padrão para HTTP calls
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        log.info("Creating default RestTemplate bean");
        return new RestTemplate();
    }
    
    /**
     * Cliente HTTP funcional
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass({DiscoveryClient.class, RestTemplate.class})
    public FunctionalHttpClient functionalHttpClient(DiscoveryClient discoveryClient, 
                                                    FunctionalLoadBalancer loadBalancer,
                                                    RestTemplate restTemplate) {
        log.info("Creating FunctionalHttpClient bean");
        return new FunctionalHttpClient(discoveryClient, loadBalancer, restTemplate);
    }
    
    private String getEurekaUrl() {
        return isDockerEnvironment() ? properties.getDockerUrl() : properties.getLocalUrl();
    }
    
    private boolean isDockerEnvironment() {
        // Detecção automática de ambiente Docker
        return environment.getActiveProfiles().length > 0 && 
               java.util.Arrays.asList(environment.getActiveProfiles()).contains("docker") ||
               System.getenv("DOCKER_ENVIRONMENT") != null ||
               System.getProperty("java.net.preferIPv4Stack") != null;
    }
}