package com.financer.eureka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Propriedades de configuração para integração com Eureka
 */
@Data
@Validated
@ConfigurationProperties(prefix = "financer.eureka")
public class EurekaIntegrationProperties {
    
    /**
     * URL do Eureka Server para desenvolvimento local
     */
    @NotBlank
    private String localUrl = "http://localhost:8761/eureka/";
    
    /**
     * URL do Eureka Server para ambiente Docker
     */
    @NotBlank
    private String dockerUrl = "http://financer-eureka-server:8761/eureka/";
    
    /**
     * Intervalo de renovação do lease em segundos
     */
    @Positive
    private Integer leaseRenewalInterval = 30;
    
    /**
     * Duração de expiração do lease em segundos
     */
    @Positive
    private Integer leaseExpirationDuration = 90;
    
    /**
     * Se deve preferir endereço IP ao invés de hostname
     */
    @NotNull
    private Boolean preferIpAddress = true;
    
    /**
     * Se deve registrar com Eureka
     */
    @NotNull
    private Boolean registerWithEureka = true;
    
    /**
     * Se deve buscar registro do Eureka
     */
    @NotNull
    private Boolean fetchRegistry = true;
    
    /**
     * Timeout de conexão em segundos
     */
    @Positive
    private Integer connectionTimeout = 5;
    
    /**
     * Timeout de leitura em segundos
     */
    @Positive
    private Integer readTimeout = 8;
    
    /**
     * Habilita health check via Eureka
     */
    @NotNull
    private Boolean enableHealthCheck = true;
    
    /**
     * Configurações específicas para Docker
     */
    private DockerConfig docker = new DockerConfig();
    
    @Data
    public static class DockerConfig {
        /**
         * Prefere IP ao invés de hostname no Docker
         */
        private Boolean preferIpAddress = false;
        
        /**
         * Hostname customizado para Docker
         */
        private String hostname;
    }
}