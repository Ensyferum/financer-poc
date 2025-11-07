package com.financer.transaction.infrastructure.client;

import com.financer.transaction.domain.model.AccountId;
import com.financer.transaction.domain.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Implementation of AccountService using Feign Client
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeignAccountService implements AccountService {

    private final AccountServiceClient accountServiceClient;

    @Override
    public Mono<Boolean> isAccountActiveAndExists(AccountId accountId) {
        return accountServiceClient.getAccount(accountId.toString())
            .map(account -> "ACTIVE".equals(account.status()))
            .onErrorReturn(false)
            .doOnNext(result -> log.debug("Account {} active check: {}", accountId, result));
    }

    @Override
    public Mono<BigDecimal> getAccountBalance(AccountId accountId) {
        return accountServiceClient.getBalance(accountId.toString())
            .map(AccountServiceClient.BalanceResponse::balance)
            .doOnNext(balance -> log.debug("Account {} balance: {}", accountId, balance))
            .onErrorReturn(BigDecimal.ZERO);
    }

    @Override
    public Mono<Boolean> hasSufficientBalance(AccountId accountId, BigDecimal amount) {
        return getAccountBalance(accountId)
            .map(balance -> balance.compareTo(amount) >= 0)
            .doOnNext(sufficient -> log.debug("Account {} sufficient balance for {}: {}", accountId, amount, sufficient));
    }

    @Override
    public Mono<Void> reserveBalance(AccountId accountId, BigDecimal amount, String correlationId) {
        // In a real implementation, this would call a specific reserve endpoint
        // For now, we'll just log the operation
        log.info("Reserving balance {} for account {} with correlation {}", amount, accountId, correlationId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> releaseReservedBalance(AccountId accountId, BigDecimal amount, String correlationId) {
        // In a real implementation, this would call a specific release endpoint
        // For now, we'll just log the operation
        log.info("Releasing reserved balance {} for account {} with correlation {}", amount, accountId, correlationId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> debit(AccountId accountId, BigDecimal amount, String correlationId) {
        AccountServiceClient.BalanceOperationRequest request = 
            new AccountServiceClient.BalanceOperationRequest(amount, "Transaction debit", correlationId);
        
        return accountServiceClient.withdraw(accountId.toString(), request)
            .doOnNext(response -> log.info("Debited {} from account {}: {}", amount, accountId, response.success()))
            .onErrorMap(error -> new RuntimeException("Failed to debit account " + accountId + ": " + error.getMessage()))
            .then();
    }

    @Override
    public Mono<Void> credit(AccountId accountId, BigDecimal amount, String correlationId) {
        AccountServiceClient.BalanceOperationRequest request = 
            new AccountServiceClient.BalanceOperationRequest(amount, "Transaction credit", correlationId);
        
        return accountServiceClient.deposit(accountId.toString(), request)
            .doOnNext(response -> log.info("Credited {} to account {}: {}", amount, accountId, response.success()))
            .onErrorMap(error -> new RuntimeException("Failed to credit account " + accountId + ": " + error.getMessage()))
            .then();
    }

    @Override
    public Mono<String> getAccountCurrency(AccountId accountId) {
        return accountServiceClient.getAccount(accountId.toString())
            .map(account -> account.currency() != null ? account.currency() : "BRL")
            .onErrorReturn("BRL") // Default to BRL if not found
            .doOnNext(currency -> log.debug("Account {} currency: {}", accountId, currency));
    }
}