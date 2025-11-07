package com.financer.orchestration.domain.service;

import com.financer.orchestration.domain.model.Saga;
import com.financer.orchestration.domain.model.SagaId;
import com.financer.orchestration.domain.model.SagaStep;
import com.financer.orchestration.domain.model.SagaStepStatus;
import com.financer.orchestration.domain.repository.SagaRepository;
import com.financer.shared.logging.FinancerLogger;
import com.financer.shared.logging.Domain;
import com.financer.shared.logging.ExecutionStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Saga Domain Service
 * 
 * Core business logic for Saga operations including orchestration,
 * compensation, and state management.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SagaDomainService {

    private final SagaRepository sagaRepository;
    private final FinancerLogger logger = FinancerLogger.getLogger(SagaDomainService.class);

    /**
     * Creates a new Saga instance
     */
    public Saga createSaga(String businessKey, String sagaType, String correlationId) {
        logger.info(ExecutionStep.START, "Creating new saga: businessKey={}, type={}", businessKey, sagaType);
        
        // Check if saga already exists
        if (sagaRepository.existsByBusinessKey(businessKey)) {
            throw new IllegalArgumentException("Saga with business key already exists: " + businessKey);
        }

        SagaId sagaId = SagaId.generate();
        Saga saga = new Saga(sagaId, businessKey, sagaType, correlationId);
        
        logger.info(ExecutionStep.PERSISTENCE, "Saving new saga: {}", saga.getId());
        return sagaRepository.save(saga);
    }

    /**
     * Starts execution of a Saga
     */
    public void startSaga(SagaId sagaId) {
        logger.info(ExecutionStep.PROCESSING, "Starting saga execution: {}", sagaId);
        
        Saga saga = findSagaById(sagaId);
        saga.start();
        
        logger.info(ExecutionStep.PERSISTENCE, "Saga started: {}", sagaId);
        sagaRepository.save(saga);
    }

    /**
     * Completes a Saga successfully
     */
    public void completeSaga(SagaId sagaId) {
        logger.info(ExecutionStep.PROCESSING, "Completing saga: {}", sagaId);
        
        Saga saga = findSagaById(sagaId);
        
        // Verify all steps are completed
        List<SagaStep> incompleteSteps = saga.getSteps().stream()
                .filter(step -> step.getStatus() != SagaStepStatus.COMPLETED && 
                               step.getStatus() != SagaStepStatus.SKIPPED)
                .toList();
        
        if (!incompleteSteps.isEmpty()) {
            throw new IllegalStateException("Cannot complete saga with incomplete steps: " + incompleteSteps.size());
        }
        
        saga.complete();
        sagaRepository.save(saga);
        
        logger.info(ExecutionStep.FINISH, "Saga completed successfully: {}", sagaId);
    }

    /**
     * Fails a Saga and initiates compensation if needed
     */
    public void failSaga(SagaId sagaId, String errorMessage) {
        logger.info(ExecutionStep.PROCESSING, "Failing saga: {} - {}", sagaId, errorMessage);
        
        Saga saga = findSagaById(sagaId);
        saga.fail(errorMessage);
        
        // Check if compensation is needed
        List<SagaStep> completedSteps = saga.getCompletedSteps();
        if (!completedSteps.isEmpty()) {
            logger.info(ExecutionStep.PROCESSING, "Saga has completed steps, compensation required: {}", sagaId);
            saga.startCompensation();
        }
        
        sagaRepository.save(saga);
        logger.info(ExecutionStep.FINISH, "Saga failed: {}", sagaId);
    }

    /**
     * Compensates a Saga by rolling back completed steps
     */
    public void compensateSaga(SagaId sagaId) {
        logger.info(ExecutionStep.PROCESSING, "Compensating saga: {}", sagaId);
        
        Saga saga = findSagaById(sagaId);
        
        if (!saga.isCompensating()) {
            throw new IllegalStateException("Saga is not in compensating state: " + saga.getStatus());
        }
        
        // Get completed steps that need compensation (in reverse order)
        List<SagaStep> stepsToCompensate = saga.getCompletedSteps().stream()
                .filter(SagaStep::hasCompensationAction)
                .sorted((a, b) -> Integer.compare(b.getSequenceOrder(), a.getSequenceOrder()))
                .toList();
        
        logger.info(ExecutionStep.PROCESSING, "Found {} steps to compensate for saga: {}", 
                   stepsToCompensate.size(), sagaId);
        
        // Mark saga as compensated when all steps are handled
        boolean allCompensated = stepsToCompensate.stream()
                .allMatch(step -> step.getStatus() == SagaStepStatus.COMPENSATED);
        
        if (allCompensated) {
            saga.compensate();
            logger.info(ExecutionStep.FINISH, "Saga compensation completed: {}", sagaId);
        }
        
        sagaRepository.save(saga);
    }

    /**
     * Aborts a Saga
     */
    public void abortSaga(SagaId sagaId, String reason) {
        logger.info(ExecutionStep.PROCESSING, "Aborting saga: {} - {}", sagaId, reason);
        
        Saga saga = findSagaById(sagaId);
        saga.abort();
        saga.setErrorMessage(reason);
        
        sagaRepository.save(saga);
        logger.info(ExecutionStep.FINISH, "Saga aborted: {}", sagaId);
    }

    /**
     * Adds a step to a Saga
     */
    public void addStepToSaga(SagaId sagaId, SagaStep step) {
        logger.info(ExecutionStep.PROCESSING, "Adding step to saga: {} - {}", sagaId, step.getStepName());
        
        Saga saga = findSagaById(sagaId);
        saga.addStep(step);
        
        sagaRepository.save(saga);
        logger.info(ExecutionStep.PERSISTENCE, "Step added to saga: {}", step.getStepName());
    }

    /**
     * Updates saga context
     */
    public void updateSagaContext(SagaId sagaId, String key, String value) {
        logger.info(ExecutionStep.PROCESSING, "Updating saga context: {} - {}={}", sagaId, key, value);
        
        Saga saga = findSagaById(sagaId);
        saga.updateContext(key, value);
        
        sagaRepository.save(saga);
    }

    /**
     * Validates if a saga can be executed
     */
    public ValidationResult validateSagaExecution(Saga saga) {
        logger.info(ExecutionStep.VALIDATION, "Validating saga execution: {}", saga.getId());
        
        // Check if saga is in valid state
        if (saga.isTerminal()) {
            return ValidationResult.invalid("Saga is already in terminal state: " + saga.getStatus());
        }
        
        // Check if all required steps are defined
        if (saga.getSteps().isEmpty()) {
            return ValidationResult.invalid("Saga has no steps defined");
        }
        
        // Validate step sequence
        List<Integer> sequences = saga.getSteps().stream()
                .map(SagaStep::getSequenceOrder)
                .sorted()
                .toList();
        
        for (int i = 0; i < sequences.size(); i++) {
            if (sequences.get(i) != i + 1) {
                return ValidationResult.invalid("Invalid step sequence order");
            }
        }
        
        return ValidationResult.valid();
    }

    /**
     * Finds saga by ID
     */
    @Transactional(readOnly = true)
    public Saga findSagaById(SagaId sagaId) {
        return sagaRepository.findById(sagaId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Saga not found: " + sagaId));
    }

    /**
     * Finds saga by business key
     */
    @Transactional(readOnly = true)
    public Optional<Saga> findSagaByBusinessKey(String businessKey) {
        return sagaRepository.findByBusinessKey(businessKey);
    }

    /**
     * Gets all active sagas
     */
    @Transactional(readOnly = true)
    public List<Saga> getActiveSagas() {
        return sagaRepository.findActiveSagas();
    }

    /**
     * Gets sagas that need compensation
     */
    @Transactional(readOnly = true)
    public List<Saga> getSagasNeedingCompensation() {
        return sagaRepository.findSagasNeedingCompensation();
    }

    /**
     * Validation result helper class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}