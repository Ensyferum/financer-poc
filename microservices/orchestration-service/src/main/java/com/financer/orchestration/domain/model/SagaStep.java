package com.financer.orchestration.domain.model;

import com.financer.shared.audit.AuditableEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Saga Step Domain Entity
 * 
 * Represents an individual step within a Saga transaction.
 * Each step corresponds to an action that can be executed and compensated.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Entity
@Table(name = "saga_steps")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaStep extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", nullable = false)
    private Saga saga;
    
    @Column(name = "step_name", length = 100, nullable = false)
    private String stepName;
    
    @Column(name = "step_type", length = 50, nullable = false)
    private String stepType;
    
    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SagaStepStatus status;
    
    @Column(name = "service_name", length = 100, nullable = false)
    private String serviceName;
    
    @Column(name = "action_name", length = 100, nullable = false)
    private String actionName;
    
    @Column(name = "compensation_action", length = 100)
    private String compensationAction;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_step_parameters", joinColumns = @JoinColumn(name = "step_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value", columnDefinition = "TEXT")
    private Map<String, String> parameters;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "compensation_started_at")
    private LocalDateTime compensationStartedAt;
    
    @Column(name = "compensation_completed_at")
    private LocalDateTime compensationCompletedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;
    
    @Column(name = "max_retry_attempts", nullable = false)
    private int maxRetryAttempts = 3;
    
    @Column(name = "timeout_seconds")
    private Long timeoutSeconds;

    // Constructors
    public SagaStep(String stepName, String stepType, int sequenceOrder, 
                   String serviceName, String actionName, String compensationAction) {
        this.stepName = stepName;
        this.stepType = stepType;
        this.sequenceOrder = sequenceOrder;
        this.serviceName = serviceName;
        this.actionName = actionName;
        this.compensationAction = compensationAction;
        this.status = SagaStepStatus.PENDING;
    }

    // Business Methods
    public void start() {
        validateStatusTransition(SagaStepStatus.EXECUTING);
        this.status = SagaStepStatus.EXECUTING;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        validateStatusTransition(SagaStepStatus.COMPLETED);
        this.status = SagaStepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        validateStatusTransition(SagaStepStatus.FAILED);
        this.status = SagaStepStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void startCompensation() {
        validateStatusTransition(SagaStepStatus.COMPENSATING);
        this.status = SagaStepStatus.COMPENSATING;
        this.compensationStartedAt = LocalDateTime.now();
    }

    public void compensate() {
        validateStatusTransition(SagaStepStatus.COMPENSATED);
        this.status = SagaStepStatus.COMPENSATED;
        this.compensationCompletedAt = LocalDateTime.now();
    }

    public void skip() {
        if (status == SagaStepStatus.PENDING) {
            this.status = SagaStepStatus.SKIPPED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public boolean canRetry() {
        return retryCount < maxRetryAttempts;
    }

    public boolean isCompleted() {
        return status == SagaStepStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == SagaStepStatus.FAILED;
    }

    public boolean isCompensating() {
        return status == SagaStepStatus.COMPENSATING;
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public boolean hasCompensationAction() {
        return compensationAction != null && !compensationAction.trim().isEmpty();
    }

    private void validateStatusTransition(SagaStepStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition step %s from %s to %s", stepName, status, targetStatus)
            );
        }
    }

    // Utility Methods
    public void addParameter(String key, String value) {
        this.parameters.put(key, value);
    }

    public String getParameter(String key) {
        return this.parameters.get(key);
    }

    @Override
    public String toString() {
        return String.format("SagaStep{id=%d, stepName='%s', status=%s, serviceName='%s'}", 
                id, stepName, status, serviceName);
    }
}