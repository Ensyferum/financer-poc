package com.financer.orchestration.application.dto;

import com.financer.orchestration.domain.model.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for Saga operations
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaResponse {

    private String sagaId;
    private String businessKey;
    private String sagaType;
    private SagaStatus status;
    private String correlationId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Map<String, String> context;
    private String errorMessage;
    private int retryCount;
    private int maxRetryAttempts;
}