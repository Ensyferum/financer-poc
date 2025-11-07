package com.financer.transaction.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Event for Event Sourcing
 * 
 * Represents domain events that occur during transaction lifecycle.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionEvent {
    
    private final UUID eventId;
    private final TransactionId transactionId;
    private final String eventType;
    private final LocalDateTime occurredAt;
    private final String payload;
    private final String metadata;

    public static TransactionEvent created(Transaction transaction) {
        return new TransactionEvent(
            UUID.randomUUID(),
            transaction.getId(),
            "TRANSACTION_CREATED",
            LocalDateTime.now(),
            buildCreatedPayload(transaction),
            transaction.getCorrelationId()
        );
    }

    public static TransactionEvent processing(Transaction transaction) {
        return new TransactionEvent(
            UUID.randomUUID(),
            transaction.getId(),
            "TRANSACTION_PROCESSING",
            LocalDateTime.now(),
            buildProcessingPayload(transaction),
            transaction.getCorrelationId()
        );
    }

    public static TransactionEvent completed(Transaction transaction) {
        return new TransactionEvent(
            UUID.randomUUID(),
            transaction.getId(),
            "TRANSACTION_COMPLETED",
            LocalDateTime.now(),
            buildCompletedPayload(transaction),
            transaction.getCorrelationId()
        );
    }

    public static TransactionEvent failed(Transaction transaction, String reasonCode) {
        return new TransactionEvent(
            UUID.randomUUID(),
            transaction.getId(),
            "TRANSACTION_FAILED",
            LocalDateTime.now(),
            buildFailedPayload(transaction, reasonCode),
            transaction.getCorrelationId()
        );
    }

    public static TransactionEvent cancelled(Transaction transaction, String reasonCode) {
        return new TransactionEvent(
            UUID.randomUUID(),
            transaction.getId(),
            "TRANSACTION_CANCELLED",
            LocalDateTime.now(),
            buildCancelledPayload(transaction, reasonCode),
            transaction.getCorrelationId()
        );
    }

    public static TransactionEvent reversed(Transaction transaction, String reasonCode) {
        return new TransactionEvent(
            UUID.randomUUID(),
            transaction.getId(),
            "TRANSACTION_REVERSED",
            LocalDateTime.now(),
            buildReversedPayload(transaction, reasonCode),
            transaction.getCorrelationId()
        );
    }

    // Payload builders

    private static String buildCreatedPayload(Transaction transaction) {
        return String.format(
            "{\"type\":\"%s\",\"amount\":\"%s\",\"sourceAccount\":\"%s\",\"destinationAccount\":\"%s\",\"description\":\"%s\"}",
            transaction.getType(),
            transaction.getAmount(),
            transaction.getSourceAccountId(),
            transaction.getDestinationAccountId() != null ? transaction.getDestinationAccountId() : "null",
            transaction.getDescription() != null ? transaction.getDescription() : ""
        );
    }

    private static String buildProcessingPayload(Transaction transaction) {
        return String.format(
            "{\"status\":\"%s\",\"transactionId\":\"%s\"}",
            transaction.getStatus(),
            transaction.getId()
        );
    }

    private static String buildCompletedPayload(Transaction transaction) {
        return String.format(
            "{\"status\":\"%s\",\"executedAt\":\"%s\",\"totalAmount\":\"%s\"}",
            transaction.getStatus(),
            transaction.getExecutedAt(),
            transaction.getTotalAmount()
        );
    }

    private static String buildFailedPayload(Transaction transaction, String reasonCode) {
        return String.format(
            "{\"status\":\"%s\",\"reasonCode\":\"%s\"}",
            transaction.getStatus(),
            reasonCode
        );
    }

    private static String buildCancelledPayload(Transaction transaction, String reasonCode) {
        return String.format(
            "{\"status\":\"%s\",\"reasonCode\":\"%s\"}",
            transaction.getStatus(),
            reasonCode
        );
    }

    private static String buildReversedPayload(Transaction transaction, String reasonCode) {
        return String.format(
            "{\"status\":\"%s\",\"reasonCode\":\"%s\"}",
            transaction.getStatus(),
            reasonCode
        );
    }
}