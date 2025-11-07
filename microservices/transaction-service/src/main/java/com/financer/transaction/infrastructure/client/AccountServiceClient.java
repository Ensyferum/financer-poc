package com.financer.transaction.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Feign Client for Account Service
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@FeignClient(name = "account-service", path = "/api/accounts")
public interface AccountServiceClient {

    @GetMapping("/{accountId}")
    Mono<AccountResponse> getAccount(@PathVariable("accountId") String accountId);

    @PostMapping("/{accountId}/deposit")
    Mono<BalanceOperationResponse> deposit(
        @PathVariable("accountId") String accountId,
        @RequestBody BalanceOperationRequest request
    );

    @PostMapping("/{accountId}/withdraw")
    Mono<BalanceOperationResponse> withdraw(
        @PathVariable("accountId") String accountId,
        @RequestBody BalanceOperationRequest request
    );

    @GetMapping("/{accountId}/balance")
    Mono<BalanceResponse> getBalance(@PathVariable("accountId") String accountId);

    // DTOs

    record AccountResponse(
        String id,
        String accountNumber,
        String ownerName,
        String accountType,
        BigDecimal balance,
        String status,
        String currency
    ) {}

    record BalanceOperationRequest(
        BigDecimal amount,
        String description,
        String referenceId
    ) {}

    record BalanceOperationResponse(
        String accountId,
        String operationType,
        BigDecimal operationAmount,
        BigDecimal previousBalance,
        BigDecimal currentBalance,
        boolean success,
        String message
    ) {}

    record BalanceResponse(
        String accountId,
        BigDecimal balance,
        String currency,
        BigDecimal availableBalance,
        BigDecimal reservedBalance
    ) {}
}