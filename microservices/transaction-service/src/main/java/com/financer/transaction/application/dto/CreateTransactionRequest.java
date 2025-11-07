package com.financer.transaction.application.dto;

import com.financer.transaction.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Create Transaction Request DTO
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
public class CreateTransactionRequest {

    @NotBlank(message = "Source account ID is required")
    private String sourceAccountId;

    private String destinationAccountId; // Optional, required for transfers

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    private String description;

    private String reference;

    @NotBlank(message = "Correlation ID is required")
    private String correlationId;

    private BigDecimal fee; // Optional, will be calculated if not provided

    /**
     * Validation for transfer transactions
     */
    public boolean isValidTransfer() {
        return type != TransactionType.TRANSFER || 
               (destinationAccountId != null && !destinationAccountId.trim().isEmpty());
    }

    /**
     * Check if fee calculation is needed
     */
    public boolean needsFeeCalculation() {
        return fee == null && type.requiresFee();
    }

    /**
     * Get effective fee (zero if null)
     */
    public BigDecimal getEffectiveFee() {
        return fee != null ? fee : BigDecimal.ZERO;
    }
}