package com.financer.orchestration.domain.model;

/**
 * Saga Status enumeration
 * 
 * Represents the current state of a Saga transaction.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
public enum SagaStatus {
    /**
     * Saga has been initiated but not yet started
     */
    INITIATED("INITIATED", "Saga has been initiated"),
    
    /**
     * Saga is currently executing
     */
    EXECUTING("EXECUTING", "Saga is executing"),
    
    /**
     * Saga has completed successfully
     */
    COMPLETED("COMPLETED", "Saga completed successfully"),
    
    /**
     * Saga has failed and compensation is required
     */
    FAILED("FAILED", "Saga failed"),
    
    /**
     * Saga is performing compensation actions
     */
    COMPENSATING("COMPENSATING", "Saga is compensating"),
    
    /**
     * Saga has been fully compensated
     */
    COMPENSATED("COMPENSATED", "Saga has been compensated"),
    
    /**
     * Saga has been aborted
     */
    ABORTED("ABORTED", "Saga has been aborted");

    private final String code;
    private final String description;

    SagaStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == COMPENSATED || this == ABORTED;
    }

    public boolean canTransitionTo(SagaStatus target) {
        return switch (this) {
            case INITIATED -> target == EXECUTING || target == ABORTED;
            case EXECUTING -> target == COMPLETED || target == FAILED || target == ABORTED;
            case FAILED -> target == COMPENSATING || target == ABORTED;
            case COMPENSATING -> target == COMPENSATED || target == ABORTED;
            case COMPLETED, COMPENSATED, ABORTED -> false;
        };
    }
}