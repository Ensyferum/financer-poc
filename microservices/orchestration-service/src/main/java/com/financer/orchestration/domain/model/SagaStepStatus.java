package com.financer.orchestration.domain.model;

/**
 * Saga Step Status enumeration
 * 
 * Represents the current state of an individual step within a Saga.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
public enum SagaStepStatus {
    /**
     * Step is pending execution
     */
    PENDING("PENDING", "Step is pending execution"),
    
    /**
     * Step is currently executing
     */
    EXECUTING("EXECUTING", "Step is executing"),
    
    /**
     * Step has completed successfully
     */
    COMPLETED("COMPLETED", "Step completed successfully"),
    
    /**
     * Step has failed
     */
    FAILED("FAILED", "Step failed"),
    
    /**
     * Step is being compensated
     */
    COMPENSATING("COMPENSATING", "Step is being compensated"),
    
    /**
     * Step has been compensated
     */
    COMPENSATED("COMPENSATED", "Step has been compensated"),
    
    /**
     * Step has been skipped
     */
    SKIPPED("SKIPPED", "Step has been skipped");

    private final String code;
    private final String description;

    SagaStepStatus(String code, String description) {
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
        return this == COMPLETED || this == COMPENSATED || this == SKIPPED;
    }

    public boolean canTransitionTo(SagaStepStatus target) {
        return switch (this) {
            case PENDING -> target == EXECUTING || target == SKIPPED;
            case EXECUTING -> target == COMPLETED || target == FAILED;
            case FAILED -> target == COMPENSATING;
            case COMPENSATING -> target == COMPENSATED;
            case COMPLETED, COMPENSATED, SKIPPED -> false;
        };
    }
}