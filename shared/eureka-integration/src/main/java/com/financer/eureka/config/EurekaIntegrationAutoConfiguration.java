package com.financer.eureka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.netflix.discovery.EurekaClient;

import jakarta.annotation.PostConstruct;

/**
 * Configuração automática para integração com Eureka
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
        log.info("========================================");
        log.info("Eureka Server URL: {}", getEurekaUrl());
        log.info("Lease Renewal Interval: {}s", properties.getLeaseRenewalInterval());
        log.info("Lease Expiration Duration: {}s", properties.getLeaseExpirationDuration());
        log.info("Register with Eureka: {}", properties.getRegisterWithEureka());
        log.info("Fetch Registry: {}", properties.getFetchRegistry());
        log.info("Health Check Enabled: {}", properties.getEnableHealthCheck());
        
        if (isDockerEnvironment()) {
            log.info("Docker Environment Detected");
            log.info("Docker Hostname: {}", properties.getDocker().getHostname());
            log.info("Docker Prefer IP: {}", properties.getDocker().getPreferIpAddress());
        }
        log.info("========================================");
    }
    
    @Bean
    @ConditionalOnMissingBean
    public EurekaConfigurationCustomizer eurekaConfigurationCustomizer() {
        return new EurekaConfigurationCustomizer(properties, environment);
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