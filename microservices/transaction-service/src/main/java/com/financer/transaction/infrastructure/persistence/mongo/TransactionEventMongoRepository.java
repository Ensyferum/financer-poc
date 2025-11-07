package com.financer.transaction.infrastructure.persistence.mongo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * MongoDB Repository for Transaction Events
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Repository
public interface TransactionEventMongoRepository extends ReactiveMongoRepository<TransactionEventDocument, String> {

    /**
     * Find events by transaction ID
     */
    Flux<TransactionEventDocument> findByTransactionId(String transactionId);

    /**
     * Find events by account ID
     */
    @Query("{ $or: [ { 'sourceAccountId': ?0 }, { 'destinationAccountId': ?0 } ] }")
    Flux<TransactionEventDocument> findByAccountId(String accountId);

    /**
     * Find events by account ID and date range
     */
    @Query("{ $and: [ " +
           "{ $or: [ { 'sourceAccountId': ?0 }, { 'destinationAccountId': ?0 } ] }, " +
           "{ 'createdAt': { $gte: ?1, $lte: ?2 } } ] }")
    Flux<TransactionEventDocument> findByAccountIdAndDateRange(String accountId, LocalDateTime from, LocalDateTime to);

    /**
     * Find events by correlation ID
     */
    Flux<TransactionEventDocument> findByCorrelationId(String correlationId);

    /**
     * Find pending transactions older than specified time
     */
    @Query("{ $and: [ { 'status': 'PENDING' }, { 'createdAt': { $lt: ?0 } } ] }")
    Flux<TransactionEventDocument> findPendingOlderThan(LocalDateTime cutoffTime);

    /**
     * Count events by account ID
     */
    @Query(value = "{ $or: [ { 'sourceAccountId': ?0 }, { 'destinationAccountId': ?0 } ] }", count = true)
    Mono<Long> countByAccountId(String accountId);

    /**
     * Find by status
     */
    Flux<TransactionEventDocument> findByStatus(String status);

    /**
     * Find by type
     */
    Flux<TransactionEventDocument> findByType(String type);

    /**
     * Find recent transactions
     */
    @Query("{ 'createdAt': { $gte: ?0 } }")
    Flux<TransactionEventDocument> findRecentTransactions(LocalDateTime since);
}