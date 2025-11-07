package com.financer.orchestration.infrastructure.web;

import com.financer.orchestration.domain.model.SagaStep;
import com.financer.orchestration.domain.service.SagaDomainService;
import com.financer.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Saga Step operations
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/saga-steps")
@RequiredArgsConstructor
public class SagaStepController {

    private final SagaDomainService sagaDomainService;

    /**
     * Get steps for a specific Saga
     */
    @GetMapping("/saga/{sagaId}")
    public ResponseEntity<ApiResponse<List<SagaStepResponse>>> getSagaSteps(@PathVariable String sagaId) {
        var saga = sagaDomainService.findSagaById(com.financer.orchestration.domain.model.SagaId.of(sagaId));
        var steps = saga.getSteps().stream()
                .map(this::convertToResponse)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(steps));
    }

    /**
     * Get step by step ID
     */
    @GetMapping("/{stepId}")
    public ResponseEntity<ApiResponse<SagaStepResponse>> getSagaStep(@PathVariable String stepId) {
        var step = sagaDomainService.findStepById(stepId);
        var response = convertToResponse(step);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get failed steps across all Sagas
     */
    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<List<SagaStepResponse>>> getFailedSteps() {
        var failedSteps = sagaDomainService.getFailedSteps();
        var responses = failedSteps.stream()
                .map(this::convertToResponse)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Retry a failed step
     */
    @PostMapping("/{stepId}/retry")
    public ResponseEntity<ApiResponse<Void>> retryStep(@PathVariable String stepId) {
        sagaDomainService.retryFailedStep(stepId);
        return ResponseEntity.ok(ApiResponse.success(null, "Step retry initiated"));
    }

    private SagaStepResponse convertToResponse(SagaStep step) {
        return SagaStepResponse.builder()
                .stepId(step.getStepId())
                .sagaId(step.getSaga().getId())
                .stepName(step.getStepName())
                .stepType(step.getStepType())
                .status(step.getStatus())
                .sequence(step.getSequence())
                .startedAt(step.getStartedAt())
                .completedAt(step.getCompletedAt())
                .output(step.getOutput())
                .errorMessage(step.getErrorMessage())
                .retryCount(step.getRetryCount())
                .compensationData(step.getCompensationData())
                .build();
    }

    // Response DTO
    @lombok.Builder
    public record SagaStepResponse(
            String stepId,
            com.financer.orchestration.domain.model.SagaId sagaId,
            String stepName,
            String stepType,
            com.financer.orchestration.domain.model.SagaStepStatus status,
            int sequence,
            java.time.LocalDateTime startedAt,
            java.time.LocalDateTime completedAt,
            String output,
            String errorMessage,
            int retryCount,
            String compensationData
    ) {}
}