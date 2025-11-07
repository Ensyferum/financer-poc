package com.financer.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO padrão para respostas da API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String correlationId;
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now(), null);
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operação realizada com sucesso");
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now(), null);
    }
    
    public ApiResponse<T> withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
}