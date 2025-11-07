package com.financer.orchestration.infrastructure.web;

import com.financer.orchestration.application.dto.StartSagaRequest;
import com.financer.orchestration.application.dto.SagaResponse;
import com.financer.orchestration.application.usecase.StartSagaUseCase;
import com.financer.orchestration.domain.model.SagaId;
import com.financer.orchestration.domain.service.SagaDomainService;
import com.financer.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Saga orchestration operations
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sagas")
@RequiredArgsConstructor
public class SagaController {

    private final StartSagaUseCase startSagaUseCase;
    private final SagaDomainService sagaDomainService;

    /**
     * Start a new Saga transaction
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SagaResponse>> startSaga(@Valid @RequestBody StartSagaRequest request) {
        SagaResponse response = startSagaUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Saga started successfully"));
    }

    /**
     * Get Saga by ID
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<ApiResponse<SagaResponse>> getSaga(@PathVariable String sagaId) {
        var saga = sagaDomainService.findSagaById(SagaId.of(sagaId));
        var response = convertToResponse(saga);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get Saga by business key
     */
    @GetMapping("/business-key/{businessKey}")
    public ResponseEntity<ApiResponse<SagaResponse>> getSagaByBusinessKey(@PathVariable String businessKey) {
        var sagaOpt = sagaDomainService.findSagaByBusinessKey(businessKey);
        
        if (sagaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        var response = convertToResponse(sagaOpt.get());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all active Sagas
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SagaResponse>>> getActiveSagas() {
        var sagas = sagaDomainService.getActiveSagas();
        var responses = sagas.stream()
                .map(this::convertToResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get Sagas that need compensation
     */
    @GetMapping("/compensation-needed")
    public ResponseEntity<ApiResponse<List<SagaResponse>>> getSagasNeedingCompensation() {
        var sagas = sagaDomainService.getSagasNeedingCompensation();
        var responses = sagas.stream()
                .map(this::convertToResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Abort a Saga
     */
    @PostMapping("/{sagaId}/abort")
    public ResponseEntity<ApiResponse<Void>> abortSaga(
            @PathVariable String sagaId,
            @RequestBody AbortSagaRequest request) {
        
        sagaDomainService.abortSaga(SagaId.of(sagaId), request.reason());
        return ResponseEntity.ok(ApiResponse.success(null, "Saga aborted successfully"));
    }

    /**
     * Force compensation for a Saga
     */
    @PostMapping("/{sagaId}/compensate")
    public ResponseEntity<ApiResponse<Void>> compensateSaga(@PathVariable String sagaId) {
        sagaDomainService.compensateSaga(SagaId.of(sagaId));
        return ResponseEntity.ok(ApiResponse.success(null, "Saga compensation initiated"));
    }

    /**
     * Get Saga health status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SagaHealthResponse>> getSagaHealth() {
        var activeSagas = sagaDomainService.getActiveSagas();
        var sagasNeedingCompensation = sagaDomainService.getSagasNeedingCompensation();
        
        var health = new SagaHealthResponse(
                activeSagas.size(),
                sagasNeedingCompensation.size(),
                "HEALTHY"
        );
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    private SagaResponse convertToResponse(com.financer.orchestration.domain.model.Saga saga) {
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

    // Helper DTOs
    public record AbortSagaRequest(String reason) {}
    
    public record SagaHealthResponse(
            int activeSagas,
            int sagasNeedingCompensation,
            String status
    ) {}
}