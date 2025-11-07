package com.financer.orchestration.application.usecase;

import com.financer.orchestration.application.dto.StartSagaRequest;
import com.financer.orchestration.application.dto.SagaResponse;
import com.financer.orchestration.domain.model.Saga;
import com.financer.orchestration.domain.model.SagaId;
import com.financer.orchestration.domain.service.SagaDomainService;
import com.financer.orchestration.infrastructure.camunda.SagaWorkflowService;
import com.financer.shared.logging.FinancerLogger;
import com.financer.shared.logging.Domain;
import com.financer.shared.logging.ExecutionStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case for starting Saga transactions
 * 
 * Orchestrates the creation and initiation of distributed transactions
 * using the Saga pattern with CAMUNDA workflow engine.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StartSagaUseCase {

    private final SagaDomainService sagaDomainService;
    private final SagaWorkflowService workflowService;
    private final FinancerLogger logger = FinancerLogger.getLogger(StartSagaUseCase.class);

    /**
     * Starts a new Saga transaction
     */
    public SagaResponse execute(StartSagaRequest request) {
        logger.startContext(Domain.ORCHESTRATION, "START_SAGA", "SYSTEM");
        
        try {
            logger.info(ExecutionStep.START, "Starting new saga: businessKey={}, type={}", 
                       request.getBusinessKey(), request.getSagaType());
            
            // Validate request
            validateRequest(request);
            
            // Create saga in domain
            Saga saga = sagaDomainService.createSaga(
                request.getBusinessKey(),
                request.getSagaType(),
                request.getCorrelationId()
            );
            
            // Update context
            if (request.getContext() != null) {
                request.getContext().forEach(saga::updateContext);
            }
            
            // Start workflow process
            logger.info(ExecutionStep.PROCESSING, "Starting workflow for saga: {}", saga.getId());
            String processInstanceId = workflowService.startSagaWorkflow(saga, request.getContext());
            
            // Update saga with process instance ID
            saga.updateContext("processInstanceId", processInstanceId);
            
            // Start saga execution
            sagaDomainService.startSaga(saga.getSagaId());
            
            logger.info(ExecutionStep.FINISH, "Saga started successfully: {}", saga.getId());
            return convertToResponse(saga);
            
        } catch (Exception e) {
            logger.error(ExecutionStep.ERROR, "Failed to start saga: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start saga: " + e.getMessage(), e);
        } finally {
            logger.clearContext();
        }
    }

    private void validateRequest(StartSagaRequest request) {
        logger.info(ExecutionStep.VALIDATION, "Validating start saga request");
        
        if (request.getBusinessKey() == null || request.getBusinessKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Business key is required");
        }
        
        if (request.getSagaType() == null || request.getSagaType().trim().isEmpty()) {
            throw new IllegalArgumentException("Saga type is required");
        }
        
        // Check if saga already exists
        if (sagaDomainService.findSagaByBusinessKey(request.getBusinessKey()).isPresent()) {
            throw new IllegalArgumentException("Saga with business key already exists: " + request.getBusinessKey());
        }
    }

    private SagaResponse convertToResponse(Saga saga) {
        return SagaResponse.builder()
                .sagaId(saga.getId())
                .businessKey(saga.getBusinessKey())
                .sagaType(saga.getSagaType())
                .status(saga.getStatus())
                .correlationId(saga.getCorrelationId())
                .startedAt(saga.getStartedAt())
                .completedAt(saga.getCompletedAt())
                .context(saga.getContext())
                .errorMessage(saga.getErrorMessage())
                .retryCount(saga.getRetryCount())
                .maxRetryAttempts(saga.getMaxRetryAttempts())
                .build();
    }
}