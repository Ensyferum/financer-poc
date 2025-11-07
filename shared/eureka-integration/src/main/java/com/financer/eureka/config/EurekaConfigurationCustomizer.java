package com.financer.eureka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

/**
 * Customizador de configuração do Eureka baseado no ambiente
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "financer.eureka.registerWithEureka", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EurekaConfigurationCustomizer {
    
    private final EurekaIntegrationProperties properties;
    private final Environment environment;
    
    @PostConstruct
    public void customizeEurekaConfiguration() {
        configureEurekaProperties();
    }
    
    private void configureEurekaProperties() {
        boolean isDocker = isDockerEnvironment();
        
        // Configuração dinâmica das propriedades do sistema
        setSystemProperty("eureka.client.service-url.defaultZone", 
                         isDocker ? properties.getDockerUrl() : properties.getLocalUrl());
        
        setSystemProperty("eureka.client.register-with-eureka", 
                         properties.getRegisterWithEureka().toString());
        
        setSystemProperty("eureka.client.fetch-registry", 
                         properties.getFetchRegistry().toString());
        
        setSystemProperty("eureka.instance.lease-renewal-interval-in-seconds", 
                         properties.getLeaseRenewalInterval().toString());
        
        setSystemProperty("eureka.instance.lease-expiration-duration-in-seconds", 
                         properties.getLeaseExpirationDuration().toString());
        
        // Configuração específica para Docker
        if (isDocker) {
            configureDockerSpecificProperties();
        } else {
            configureLocalSpecificProperties();
        }
        
        // Health Check
        if (properties.getEnableHealthCheck()) {
            setSystemProperty("eureka.client.healthcheck.enabled", "true");
        }
        
        log.info("Eureka configuration customized for environment: {}", 
                isDocker ? "Docker" : "Local");
    }
    
    private void configureDockerSpecificProperties() {
        setSystemProperty("eureka.instance.prefer-ip-address", 
                         properties.getDocker().getPreferIpAddress().toString());
        
        if (properties.getDocker().getHostname() != null && 
            !properties.getDocker().getHostname().trim().isEmpty()) {
            setSystemProperty("eureka.instance.hostname", properties.getDocker().getHostname());
        } else {
            // Usar o nome da aplicação como hostname se não especificado
            String appName = environment.getProperty("spring.application.name");
            if (appName != null) {
                setSystemProperty("eureka.instance.hostname", appName);
            }
        }
    }
    
    private void configureLocalSpecificProperties() {
        setSystemProperty("eureka.instance.prefer-ip-address", 
                         properties.getPreferIpAddress().toString());
    }
    
    private void setSystemProperty(String key, String value) {
        // Só define se não estiver já definido
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
            log.debug("Set system property: {} = {}", key, value);
        }
    }
    
    private boolean isDockerEnvironment() {
        // Múltiplas formas de detecção do ambiente Docker
        return environment.getActiveProfiles().length > 0 && 
               java.util.Arrays.asList(environment.getActiveProfiles()).contains("docker") ||
               System.getenv("DOCKER_ENVIRONMENT") != null ||
               System.getenv("HOSTNAME") != null && System.getenv("HOSTNAME").contains("-") ||
               System.getProperty("java.net.preferIPv4Stack") != null;
    }
}