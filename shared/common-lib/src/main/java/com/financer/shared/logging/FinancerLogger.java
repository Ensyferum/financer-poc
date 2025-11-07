package com.financer.shared.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utilitário para logs padronizados do sistema
 * Padrão: [DOMAIN][FUNCTION][STEP] - Description
 */
public class FinancerLogger {
    
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String USER_ID_KEY = "userId";
    private static final String DOMAIN_KEY = "domain";
    private static final String FUNCTION_KEY = "function";
    private static final String STEP_KEY = "step";
    
    private final Logger logger;
    
    public FinancerLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }
    
    public static FinancerLogger getLogger(Class<?> clazz) {
        return new FinancerLogger(clazz);
    }
    
    /**
     * Inicia o contexto de log para uma nova operação
     */
    public void startContext(Domain domain, String function, String userId) {
        MDC.put(CORRELATION_ID_KEY, UUID.randomUUID().toString());
        MDC.put(USER_ID_KEY, userId != null ? userId : "SYSTEM");
        MDC.put(DOMAIN_KEY, domain.getCode());
        MDC.put(FUNCTION_KEY, function);
    }
    
    /**
     * Limpa o contexto de log
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Log de informação com padrão
     */
    public void info(ExecutionStep step, String description, Object... params) {
        MDC.put(STEP_KEY, step.getCode());
        String message = formatMessage(step, description);
        logger.info(message, params);
    }
    
    /**
     * Log de warning com padrão
     */
    public void warn(ExecutionStep step, String description, Object... params) {
        MDC.put(STEP_KEY, step.getCode());
        String message = formatMessage(step, description);
        logger.warn(message, params);
    }
    
    /**
     * Log de erro com padrão
     */
    public void error(ExecutionStep step, String description, Throwable throwable, Object... params) {
        MDC.put(STEP_KEY, step.getCode());
        String message = formatMessage(step, description);
        logger.error(message, throwable, params);
    }
    
    /**
     * Log de debug com padrão
     */
    public void debug(ExecutionStep step, String description, Object... params) {
        MDC.put(STEP_KEY, step.getCode());
        String message = formatMessage(step, description);
        logger.debug(message, params);
    }
    
    private String formatMessage(ExecutionStep step, String description) {
        String domain = MDC.get(DOMAIN_KEY);
        String function = MDC.get(FUNCTION_KEY);
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        
        return String.format("[%s][%s][%s][%s] - %s", 
            domain != null ? domain : "UNKNOWN",
            function != null ? function : "UNKNOWN", 
            step.getCode(),
            correlationId != null ? correlationId.substring(0, 8) : "UNKNOWN",
            description);
    }
}