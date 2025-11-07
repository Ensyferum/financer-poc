package com.financer.transaction.domain.service;

import com.financer.transaction.domain.model.AccountId;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Account Service Interface
 * 
 * Domain service for interacting with account operations.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
public interface AccountService {

    /**
     * Check if account exists and is active
     */
    Mono<Boolean> isAccountActiveAndExists(AccountId accountId);

    /**
     * Get account balance
     */
    Mono<BigDecimal> getAccountBalance(AccountId accountId);

    /**
     * Check if account has sufficient balance
     */
    Mono<Boolean> hasSufficientBalance(AccountId accountId, BigDecimal amount);

    /**
     * Reserve balance for transaction
     */
    Mono<Void> reserveBalance(AccountId accountId, BigDecimal amount, String correlationId);

    /**
     * Release reserved balance
     */
    Mono<Void> releaseReservedBalance(AccountId accountId, BigDecimal amount, String correlationId);

    /**
     * Execute debit operation
     */
    Mono<Void> debit(AccountId accountId, BigDecimal amount, String correlationId);

    /**
     * Execute credit operation
     */
    Mono<Void> credit(AccountId accountId, BigDecimal amount, String correlationId);

    /**
     * Get account currency
     */
    Mono<String> getAccountCurrency(AccountId accountId);
}