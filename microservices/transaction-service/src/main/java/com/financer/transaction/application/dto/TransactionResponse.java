package com.financer.transaction.application.dto;

import com.financer.transaction.domain.model.TransactionStatus;
import com.financer.transaction.domain.model.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Response DTO
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
public class TransactionResponse {

    private String id;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal fee;
    private BigDecimal totalAmount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String reference;
    private String correlationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime executedAt;
    private String reasonCode;
    private String metadata;

    /**
     * Check if transaction is completed
     */
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * Check if transaction is terminal
     */
    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

    /**
     * Get formatted amount with currency
     */
    public String getFormattedAmount() {
        return String.format("%s %.2f", currency, amount.doubleValue());
    }

    /**
     * Get formatted total amount with currency
     */
    public String getFormattedTotalAmount() {
        return String.format("%s %.2f", currency, totalAmount.doubleValue());
    }
}