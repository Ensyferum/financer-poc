package com.financer.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Balance Operation Response DTO
 * 
 * Data Transfer Object for balance operation responses.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceOperationResponse {

    private UUID accountId;
    private String accountNumber;
    private BigDecimal previousBalance;
    private BigDecimal currentBalance;
    private BigDecimal operationAmount;
    private String operationType;
    private String description;
    private String referenceId;
    private LocalDateTime operationTime;
    private boolean success;
    private String message;

    /**
     * Static factory method for successful operation
     */
    public static BalanceOperationResponse success(String accountNumber, BigDecimal previousBalance, 
                                                   BigDecimal currentBalance, BigDecimal operationAmount,
                                                   String operationType, String description, String referenceId) {
        BalanceOperationResponse response = new BalanceOperationResponse();
        response.setAccountNumber(accountNumber);
        response.setPreviousBalance(previousBalance);
        response.setCurrentBalance(currentBalance);
        response.setOperationAmount(operationAmount);
        response.setOperationType(operationType);
        response.setDescription(description);
        response.setReferenceId(referenceId);
        response.setOperationTime(LocalDateTime.now());
        response.setSuccess(true);
        response.setMessage("Operation completed successfully");
        return response;
    }

    /**
     * Static factory method for failed operation
     */
    public static BalanceOperationResponse failure(String accountNumber, String message) {
        BalanceOperationResponse response = new BalanceOperationResponse();
        response.setAccountNumber(accountNumber);
        response.setOperationTime(LocalDateTime.now());
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}