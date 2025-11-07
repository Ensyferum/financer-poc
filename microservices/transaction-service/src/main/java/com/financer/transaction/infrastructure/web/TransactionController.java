package com.financer.transaction.infrastructure.web;

import com.financer.shared.dto.ApiResponse;
import com.financer.transaction.application.dto.CreateTransactionRequest;
import com.financer.transaction.application.dto.TransactionResponse;
import com.financer.transaction.application.dto.TransactionSearchCriteria;
import com.financer.transaction.application.usecase.CreateTransactionUseCase;
import com.financer.transaction.application.usecase.ProcessTransactionUseCase;
import com.financer.transaction.domain.model.TransactionStatus;
import com.financer.transaction.domain.model.TransactionType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transaction REST Controller
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final ProcessTransactionUseCase processTransactionUseCase;

    /**
     * Create a new transaction
     */
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        
        return createTransactionUseCase.execute(request)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Transaction created successfully")))
            .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to create transaction")));
    }

    /**
     * Process a pending transaction
     */
    @PostMapping("/{transactionId}/process")
    public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> processTransaction(
            @PathVariable String transactionId) {
        
        return processTransactionUseCase.execute(transactionId)
            .map(response -> ResponseEntity.ok(
                ApiResponse.success(response, "Transaction processed successfully")))
            .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to process transaction")));
    }

    /**
     * Get transaction by ID
     */
    @GetMapping("/{transactionId}")
    public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> getTransaction(
            @PathVariable String transactionId) {
        
        // This would be implemented with a GetTransactionUseCase
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(ApiResponse.error("Get transaction endpoint not implemented yet")));
    }

    /**
     * Search transactions with criteria
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<List<TransactionResponse>>>> searchTransactions(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) List<TransactionType> types,
            @RequestParam(required = false) List<TransactionStatus> statuses,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        TransactionSearchCriteria criteria = new TransactionSearchCriteria();
        criteria.setAccountId(accountId);
        criteria.setTypes(types);
        criteria.setStatuses(statuses);
        criteria.setMinAmount(minAmount);
        criteria.setMaxAmount(maxAmount);
        criteria.setFromDate(fromDate);
        criteria.setToDate(toDate);
        criteria.setCorrelationId(correlationId);
        criteria.setReference(reference);
        criteria.setDescription(description);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy(sortBy);
        criteria.setSortDirection(sortDirection);

        // This would be implemented with a SearchTransactionsUseCase
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(ApiResponse.error("Search transactions endpoint not implemented yet")));
    }

    /**
     * Get transactions by account ID
     */
    @GetMapping("/account/{accountId}")
    public Flux<TransactionResponse> getTransactionsByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // This would be implemented with a GetTransactionsByAccountUseCase
        return Flux.empty();
    }

    /**
     * Cancel a transaction
     */
    @PostMapping("/{transactionId}/cancel")
    public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> cancelTransaction(
            @PathVariable String transactionId,
            @RequestParam(required = false) String reason) {
        
        // This would be implemented with a CancelTransactionUseCase
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(ApiResponse.error("Cancel transaction endpoint not implemented yet")));
    }

    /**
     * Reverse a completed transaction
     */
    @PostMapping("/{transactionId}/reverse")
    public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> reverseTransaction(
            @PathVariable String transactionId,
            @RequestParam(required = false) String reason) {
        
        // This would be implemented with a ReverseTransactionUseCase
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(ApiResponse.error("Reverse transaction endpoint not implemented yet")));
    }

    /**
     * Get transaction statistics
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<ApiResponse<TransactionStatsResponse>>> getTransactionStats(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate) {
        
        // This would be implemented with a GetTransactionStatsUseCase
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(ApiResponse.error("Transaction stats endpoint not implemented yet")));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<ApiResponse<String>>> healthCheck() {
        return Mono.just(ResponseEntity.ok(
            ApiResponse.success("OK", "Transaction service is healthy")));
    }

    // Inner class for statistics response
    public static class TransactionStatsResponse {
        private long totalTransactions;
        private BigDecimal totalAmount;
        private long successfulTransactions;
        private long failedTransactions;
        private BigDecimal averageAmount;
        
        // Getters and setters would be here
    }
}