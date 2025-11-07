package com.financer.account.dto;

import com.financer.account.entity.Account.AccountType;
import com.financer.account.entity.Account.AccountStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Account Creation Request DTO
 * 
 * Data Transfer Object for account creation operations.
 * Contains all necessary information to create a new account.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
public class CreateAccountRequest {

    @NotBlank(message = "Owner name is required")
    @Size(max = 255, message = "Owner name must not exceed 255 characters")
    private String ownerName;

    @NotBlank(message = "Owner document is required")
    @Pattern(regexp = "\\d{11}|\\d{14}", message = "Owner document must be a valid CPF (11 digits) or CNPJ (14 digits)")
    private String ownerDocument;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String ownerEmail;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance must be non-negative")
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @Size(max = 10, message = "Branch code must not exceed 10 characters")
    private String branchCode;

    @Size(max = 10, message = "Bank code must not exceed 10 characters")
    private String bankCode;

    @DecimalMin(value = "0.0", inclusive = true, message = "Daily limit must be non-negative")
    private BigDecimal dailyLimit;

    @DecimalMin(value = "0.0", inclusive = true, message = "Monthly limit must be non-negative")
    private BigDecimal monthlyLimit;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}