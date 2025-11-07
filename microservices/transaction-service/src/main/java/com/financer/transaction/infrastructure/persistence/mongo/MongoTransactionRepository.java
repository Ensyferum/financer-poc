package com.financer.transaction.infrastructure.persistence.mongo;

import com.financer.transaction.domain.model.*;
import com.financer.transaction.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * MongoDB Implementation of Transaction Repository
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MongoTransactionRepository implements TransactionRepository {

    private final TransactionEventMongoRepository mongoRepository;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        TransactionEventDocument document = convertToDocument(transaction);
        
        return mongoRepository.save(document)
            .map(this::convertToDomain);
    }

    @Override
    public Mono<Transaction> findById(TransactionId id) {
        return mongoRepository.findByTransactionId(id.toString())
            .collectList()
            .flatMap(documents -> {
                if (documents.isEmpty()) {
                    return Mono.empty();
                }
                // Get the latest version (event sourcing reconstruction)
                return Mono.just(convertToDomain(documents.get(documents.size() - 1)));
            });
    }

    @Override
    public Flux<Transaction> findByAccountId(AccountId accountId) {
        return mongoRepository.findByAccountId(accountId.toString())
            .map(this::convertToDomain);
    }

    @Override
    public Flux<Transaction> findByAccountIdAndDateRange(AccountId accountId, LocalDateTime from, LocalDateTime to) {
        return mongoRepository.findByAccountIdAndDateRange(accountId.toString(), from, to)
            .map(this::convertToDomain);
    }

    @Override
    public Flux<Transaction> findByCorrelationId(String correlationId) {
        return mongoRepository.findByCorrelationId(correlationId)
            .map(this::convertToDomain);
    }

    @Override
    public Flux<Transaction> findPendingTransactionsOlderThan(LocalDateTime cutoffTime) {
        return mongoRepository.findPendingOlderThan(cutoffTime)
            .map(this::convertToDomain);
    }

    @Override
    public Mono<Long> countByAccountId(AccountId accountId) {
        return mongoRepository.countByAccountId(accountId.toString());
    }

    @Override
    public Mono<Boolean> existsById(TransactionId id) {
        return mongoRepository.existsById(id.toString());
    }

    @Override
    public Mono<Void> deleteById(TransactionId id) {
        return mongoRepository.deleteById(id.toString());
    }

    // Conversion methods

    private TransactionEventDocument convertToDocument(Transaction transaction) {
        TransactionEventDocument document = new TransactionEventDocument();
        document.setId(transaction.getId().toString());
        document.setTransactionId(transaction.getId().toString());
        document.setSourceAccountId(transaction.getSourceAccountId().toString());
        document.setDestinationAccountId(
            transaction.getDestinationAccount()
                .map(AccountId::toString)
                .orElse(null)
        );
        document.setAmount(transaction.getAmount().getAmount());
        document.setCurrency(transaction.getAmount().getCurrency());
        document.setFee(transaction.getFee().getAmount());
        document.setType(transaction.getType().name());
        document.setStatus(transaction.getStatus().name());
        document.setDescription(transaction.getDescription());
        document.setReference(transaction.getReference());
        document.setCorrelationId(transaction.getCorrelationId());
        document.setCreatedAt(transaction.getCreatedAt());
        document.setUpdatedAt(transaction.getUpdatedAt());
        document.setExecutedAt(transaction.getExecutedAt());
        document.setReasonCode(transaction.getReasonCode());
        document.setMetadata(transaction.getMetadata());

        // Convert events
        document.setEvents(
            transaction.getEvents().stream()
                .map(this::convertEventToData)
                .collect(Collectors.toList())
        );

        return document;
    }

    private Transaction convertToDomain(TransactionEventDocument document) {
        // This is a simplified conversion - in a real event sourcing implementation,
        // we would reconstruct the domain object from events
        
        AccountId sourceAccountId = AccountId.of(document.getSourceAccountId());
        AccountId destinationAccountId = document.getDestinationAccountId() != null ?
            AccountId.of(document.getDestinationAccountId()) : null;
        Money amount = Money.of(document.getAmount(), document.getCurrency());
        Money fee = Money.of(document.getFee(), document.getCurrency());
        TransactionType type = TransactionType.valueOf(document.getType());

        // Create transaction based on type
        Transaction transaction = switch (type) {
            case DEPOSIT -> Transaction.createDeposit(
                sourceAccountId, amount, document.getDescription(),
                document.getReference(), document.getCorrelationId()
            );
            case WITHDRAWAL -> Transaction.createWithdrawal(
                sourceAccountId, amount, fee, document.getDescription(),
                document.getReference(), document.getCorrelationId()
            );
            case TRANSFER -> Transaction.createTransfer(
                sourceAccountId, destinationAccountId, amount, fee,
                document.getDescription(), document.getReference(), document.getCorrelationId()
            );
            default -> throw new IllegalArgumentException("Unsupported transaction type: " + type);
        };

        // Apply status changes based on document status
        TransactionStatus currentStatus = TransactionStatus.valueOf(document.getStatus());
        if (currentStatus != TransactionStatus.PENDING) {
            // This is a simplified state reconstruction
            // In a real implementation, we would replay events
            switch (currentStatus) {
                case PROCESSING -> transaction.process();
                case COMPLETED -> {
                    transaction.process();
                    transaction.complete();
                }
                case FAILED -> {
                    transaction.process();
                    transaction.fail(document.getReasonCode());
                }
                case CANCELLED -> transaction.cancel(document.getReasonCode());
                case REVERSED -> {
                    transaction.process();
                    transaction.complete();
                    transaction.reverse(document.getReasonCode());
                }
            }
        }

        // Clear events since we're not implementing full event sourcing here
        transaction.clearEvents();

        return transaction;
    }

    private TransactionEventDocument.EventData convertEventToData(TransactionEvent event) {
        TransactionEventDocument.EventData eventData = new TransactionEventDocument.EventData();
        eventData.setEventId(event.getEventId().toString());
        eventData.setEventType(event.getEventType());
        eventData.setOccurredAt(event.getOccurredAt());
        eventData.setPayload(event.getPayload());
        eventData.setMetadata(event.getMetadata());
        return eventData;
    }
}