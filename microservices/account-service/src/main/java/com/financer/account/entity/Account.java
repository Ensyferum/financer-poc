package com.financer.account.entity;

import com.financer.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Account Entity
 * 
 * Represents a financial account in the system with balance management
 * and status tracking capabilities.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_number", columnList = "accountNumber", unique = true),
    @Index(name = "idx_account_status", columnList = "status"),
    @Index(name = "idx_account_type", columnList = "accountType"),
    @Index(name = "idx_owner_name", columnList = "ownerName")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class Account extends BaseEntity {

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Account number is required")
    @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 characters")
    private String accountNumber;

    @Column(name = "owner_name", nullable = false, length = 255)
    @NotBlank(message = "Owner name is required")
    @Size(max = 255, message = "Owner name must not exceed 255 characters")
    private String ownerName;

    @Column(name = "owner_document", nullable = false, length = 20)
    @NotBlank(message = "Owner document is required")
    @Pattern(regexp = "\\d{11}|\\d{14}", message = "Owner document must be a valid CPF (11 digits) or CNPJ (14 digits)")
    private String ownerDocument;

    @Column(name = "owner_email", length = 100)
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String ownerEmail;

    @Column(name = "account_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must be non-negative")
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Account status is required")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "branch_code", length = 10)
    @Size(max = 10, message = "Branch code must not exceed 10 characters")
    private String branchCode;

    @Column(name = "bank_code", length = 10)
    @Size(max = 10, message = "Bank code must not exceed 10 characters")
    private String bankCode;

    @Column(name = "daily_limit", precision = 15, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily limit must be non-negative")
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", precision = 15, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Monthly limit must be non-negative")
    private BigDecimal monthlyLimit;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Account Type Enumeration
     */
    public enum AccountType {
        CHECKING("Checking Account"),
        SAVINGS("Savings Account"),
        INVESTMENT("Investment Account"),
        BUSINESS("Business Account");

        private final String description;

        AccountType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Account Status Enumeration
     */
    public enum AccountStatus {
        ACTIVE("Active"),
        INACTIVE("Inactive"),
        SUSPENDED("Suspended"),
        CLOSED("Closed"),
        PENDING_VERIFICATION("Pending Verification");

        private final String description;

        AccountStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Business logic method to check if account is active
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Business logic method to check if account can perform transactions
     */
    public boolean canPerformTransactions() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Business logic method to check if account has sufficient balance
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    /**
     * Business logic method to add balance
     */
    public void addBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }

    /**
     * Business logic method to subtract balance
     */
    public void subtractBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && hasSufficientBalance(amount)) {
            this.balance = this.balance.subtract(amount);
        }
    }
}