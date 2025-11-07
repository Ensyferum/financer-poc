package com.financer.orchestration.domain.model;

import com.financer.shared.audit.AuditableEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Saga Domain Entity
 * 
 * Core domain entity representing a distributed transaction using the Saga pattern.
 * Manages the orchestration of multiple microservices and handles compensation logic.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Entity
@Table(name = "sagas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Saga extends AuditableEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "business_key", length = 255, unique = true)
    private String businessKey;
    
    @Column(name = "saga_type", length = 100, nullable = false)
    private String sagaType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SagaStatus status;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "compensation_started_at")
    private LocalDateTime compensationStartedAt;
    
    @Column(name = "compensation_completed_at")
    private LocalDateTime compensationCompletedAt;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_context", joinColumns = @JoinColumn(name = "saga_id"))
    @MapKeyColumn(name = "context_key")
    @Column(name = "context_value", columnDefinition = "TEXT")
    private Map<String, String> context;
    
    @OneToMany(mappedBy = "saga", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SagaStep> steps = new ArrayList<>();
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;
    
    @Column(name = "max_retry_attempts", nullable = false)
    private int maxRetryAttempts = 3;

    // Constructors
    public Saga(SagaId sagaId, String businessKey, String sagaType, String correlationId) {
        this.id = sagaId.toString();
        this.businessKey = businessKey;
        this.sagaType = sagaType;
        this.correlationId = correlationId;
        this.status = SagaStatus.INITIATED;
        this.startedAt = LocalDateTime.now();
    }

    // Business Methods
    public void start() {
        validateStatusTransition(SagaStatus.EXECUTING);
        this.status = SagaStatus.EXECUTING;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        validateStatusTransition(SagaStatus.COMPLETED);
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        validateStatusTransition(SagaStatus.FAILED);
        this.status = SagaStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void startCompensation() {
        validateStatusTransition(SagaStatus.COMPENSATING);
        this.status = SagaStatus.COMPENSATING;
        this.compensationStartedAt = LocalDateTime.now();
    }

    public void compensate() {
        validateStatusTransition(SagaStatus.COMPENSATED);
        this.status = SagaStatus.COMPENSATED;
        this.compensationCompletedAt = LocalDateTime.now();
    }

    public void abort() {
        if (!status.isTerminal()) {
            this.status = SagaStatus.ABORTED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public boolean canRetry() {
        return retryCount < maxRetryAttempts;
    }

    public void addStep(SagaStep step) {
        step.setSaga(this);
        this.steps.add(step);
    }

    public List<SagaStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public List<SagaStep> getCompletedSteps() {
        return steps.stream()
                .filter(step -> step.getStatus() == SagaStepStatus.COMPLETED)
                .toList();
    }

    public List<SagaStep> getFailedSteps() {
        return steps.stream()
                .filter(step -> step.getStatus() == SagaStepStatus.FAILED)
                .toList();
    }

    public boolean isCompleted() {
        return status == SagaStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == SagaStatus.FAILED;
    }

    public boolean isCompensating() {
        return status == SagaStatus.COMPENSATING;
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    private void validateStatusTransition(SagaStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", status, targetStatus)
            );
        }
    }

    // Utility Methods
    public SagaId getSagaId() {
        return SagaId.of(this.id);
    }

    public void updateContext(String key, String value) {
        this.context.put(key, value);
    }

    public String getContextValue(String key) {
        return this.context.get(key);
    }

    @Override
    public String toString() {
        return String.format("Saga{id='%s', businessKey='%s', sagaType='%s', status=%s}", 
                id, businessKey, sagaType, status);
    }
}