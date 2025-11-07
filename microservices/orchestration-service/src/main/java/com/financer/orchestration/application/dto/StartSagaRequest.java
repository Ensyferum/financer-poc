package com.financer.orchestration.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request DTO for starting a new Saga
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSagaRequest {

    @NotBlank(message = "Business key is required")
    private String businessKey;

    @NotBlank(message = "Saga type is required")
    private String sagaType;

    private String correlationId;

    @NotNull(message = "Context is required")
    private Map<String, String> context;

    private String description;
}