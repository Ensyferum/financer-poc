package com.financer.account.dto;

import com.financer.account.entity.Account.AccountType;
import com.financer.account.entity.Account.AccountStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Account Update Request DTO
 * 
 * Data Transfer Object for account update operations.
 * Contains fields that can be modified after account creation.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
public class UpdateAccountRequest {

    @Size(max = 255, message = "Owner name must not exceed 255 characters")
    private String ownerName;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String ownerEmail;

    private AccountStatus status;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 10, message = "Branch code must not exceed 10 characters")
    private String branchCode;

    @Size(max = 10, message = "Bank code must not exceed 10 characters")
    private String bankCode;

    @DecimalMin(value = "0.0", inclusive = true, message = "Daily limit must be non-negative")
    private BigDecimal dailyLimit;

    @DecimalMin(value = "0.0", inclusive = true, message = "Monthly limit must be non-negative")
    private BigDecimal monthlyLimit;
}