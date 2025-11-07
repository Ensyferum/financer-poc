package com.financer.orchestration.infrastructure.feign;

import com.financer.shared.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Feign client for Transaction Service communication
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@FeignClient(
    name = "transaction-service",
    path = "/transaction/api/v1/transactions",
    fallback = TransactionServiceClientFallback.class
)
public interface TransactionServiceClient {

    @PostMapping
    ApiResponse<TransactionResponse> createTransaction(@RequestBody CreateTransactionRequest request);

    @PostMapping("/{transactionId}/process")
    ApiResponse<TransactionResponse> processTransaction(@PathVariable String transactionId);

    @PostMapping("/{transactionId}/cancel")
    ApiResponse<Void> cancelTransaction(@PathVariable String transactionId, @RequestBody CancelTransactionRequest request);

    @GetMapping("/{transactionId}")
    ApiResponse<TransactionResponse> getTransaction(@PathVariable String transactionId);

    @GetMapping("/correlation/{correlationId}")
    ApiResponse<TransactionResponse> getTransactionByCorrelationId(@PathVariable String correlationId);

    // DTOs
    record CreateTransactionRequest(
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        String type,
        String correlationId,
        String description
    ) {}

    record CancelTransactionRequest(
        String reason,
        String correlationId
    ) {}

    record TransactionResponse(
        String id,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        BigDecimal fee,
        BigDecimal totalAmount,
        String type,
        String status,
        String correlationId,
        String description,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
    ) {}
}