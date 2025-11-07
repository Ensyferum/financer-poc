package com.financer.transaction.domain.repository;

import com.financer.transaction.domain.model.AccountId;
import com.financer.transaction.domain.model.Transaction;
import com.financer.transaction.domain.model.TransactionId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Transaction Repository Interface
 * 
 * Repository pattern for transaction persistence with reactive support.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
public interface TransactionRepository {

    /**
     * Save a transaction
     */
    Mono<Transaction> save(Transaction transaction);

    /**
     * Find transaction by ID
     */
    Mono<Transaction> findById(TransactionId id);

    /**
     * Find transactions by account ID
     */
    Flux<Transaction> findByAccountId(AccountId accountId);

    /**
     * Find transactions by account ID and date range
     */
    Flux<Transaction> findByAccountIdAndDateRange(AccountId accountId, 
                                                LocalDateTime from, 
                                                LocalDateTime to);

    /**
     * Find transactions by correlation ID
     */
    Flux<Transaction> findByCorrelationId(String correlationId);

    /**
     * Find pending transactions older than specified time
     */
    Flux<Transaction> findPendingTransactionsOlderThan(LocalDateTime cutoffTime);

    /**
     * Count transactions by account ID
     */
    Mono<Long> countByAccountId(AccountId accountId);

    /**
     * Check if transaction exists
     */
    Mono<Boolean> existsById(TransactionId id);

    /**
     * Delete transaction (for testing purposes only)
     */
    Mono<Void> deleteById(TransactionId id);
}