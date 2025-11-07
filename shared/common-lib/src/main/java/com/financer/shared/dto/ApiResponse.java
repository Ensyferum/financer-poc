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
    
    public static <T> ApiResponse<T> builder() {
        return new ApiResponse<>();
    }
    
    public ApiResponse<T> success(boolean success) {
        this.success = success;
        return this;
    }
    
    public ApiResponse<T> message(String message) {
        this.message = message;
        return this;
    }
    
    public ApiResponse<T> data(T data) {
        this.data = data;
        return this;
    }
    
    public ApiResponse<T> build() {
        this.timestamp = LocalDateTime.now();
        return this;
    }
    
    public ApiResponse<T> withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
}