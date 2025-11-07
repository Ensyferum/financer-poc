package com.financer.account.dto;

import com.financer.account.entity.Account.AccountType;
import com.financer.account.entity.Account.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account Response DTO
 * 
 * Data Transfer Object for account information responses.
 * Contains all account information returned to clients.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private UUID id;
    private String accountNumber;
    private String ownerName;
    private String ownerDocument;
    private String ownerEmail;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private String description;
    private String branchCode;
    private String bankCode;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    /**
     * Convenience method to get account type description
     */
    public String getAccountTypeDescription() {
        return accountType != null ? accountType.getDescription() : null;
    }

    /**
     * Convenience method to get account status description
     */
    public String getAccountStatusDescription() {
        return status != null ? status.getDescription() : null;
    }

    /**
     * Convenience method to check if account is active
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Convenience method to format balance for display
     */
    public String getFormattedBalance() {
        return balance != null ? String.format("R$ %.2f", balance) : "R$ 0,00";
    }
}