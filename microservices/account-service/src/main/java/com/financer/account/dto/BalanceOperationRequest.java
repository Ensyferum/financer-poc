package com.financer.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Balance Operation Request DTO
 * 
 * Data Transfer Object for balance operation requests (deposit/withdrawal).
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
public class BalanceOperationRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String description;

    private String referenceId;

    /**
     * Operation Type Enumeration
     */
    public enum OperationType {
        DEPOSIT("Deposit"),
        WITHDRAWAL("Withdrawal");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}