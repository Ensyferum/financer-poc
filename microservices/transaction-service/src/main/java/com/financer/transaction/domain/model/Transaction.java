package com.financer.transaction.domain.model;

import com.financer.shared.audit.AuditableEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Domain Entity
 * 
 * Core domain entity representing a financial transaction.
 * Implements Domain-Driven Design patterns and event sourcing.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction extends AuditableEntity {
    
    private TransactionId id;
    private AccountId sourceAccountId;
    private AccountId destinationAccountId; // Null for deposits/withdrawals
    private Money amount;
    private Money fee;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String reference;
    private String correlationId;
    private LocalDateTime executedAt;
    private String reasonCode;
    private String metadata;

    // Event sourcing support
    private final List<TransactionEvent> events = new ArrayList<>();

    private Transaction(TransactionId id, 
                       AccountId sourceAccountId, 
                       AccountId destinationAccountId,
                       Money amount, 
                       Money fee,
                       TransactionType type, 
                       String description, 
                       String reference,
                       String correlationId) {
        this.id = id;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.fee = fee != null ? fee : Money.zero(amount.getCurrency());
        this.type = type;
        this.status = TransactionStatus.PENDING;
        this.description = description;
        this.reference = reference;
        this.correlationId = correlationId;
        
        // Add creation event
        addEvent(TransactionEvent.created(this));
    }

    /**
     * Factory method to create a deposit transaction
     */
    public static Transaction createDeposit(AccountId accountId, 
                                          Money amount, 
                                          String description,
                                          String reference,
                                          String correlationId) {
        validateDepositParameters(accountId, amount);
        
        return new Transaction(
            TransactionId.generate(),
            accountId,
            null, // No destination for deposits
            amount,
            Money.zero(amount.getCurrency()),
            TransactionType.DEPOSIT,
            description,
            reference,
            correlationId
        );
    }

    /**
     * Factory method to create a withdrawal transaction
     */
    public static Transaction createWithdrawal(AccountId accountId, 
                                             Money amount, 
                                             Money fee,
                                             String description,
                                             String reference,
                                             String correlationId) {
        validateWithdrawalParameters(accountId, amount);
        
        return new Transaction(
            TransactionId.generate(),
            accountId,
            null, // No destination for withdrawals
            amount,
            fee,
            TransactionType.WITHDRAWAL,
            description,
            reference,
            correlationId
        );
    }

    /**
     * Factory method to create a transfer transaction
     */
    public static Transaction createTransfer(AccountId sourceAccountId,
                                           AccountId destinationAccountId,
                                           Money amount,
                                           Money fee,
                                           String description,
                                           String reference,
                                           String correlationId) {
        validateTransferParameters(sourceAccountId, destinationAccountId, amount);
        
        return new Transaction(
            TransactionId.generate(),
            sourceAccountId,
            destinationAccountId,
            amount,
            fee,
            TransactionType.TRANSFER,
            description,
            reference,
            correlationId
        );
    }

    /**
     * Process the transaction
     */
    public void process() {
        if (!status.canTransitionTo(TransactionStatus.PROCESSING)) {
            throw new IllegalStateException("Cannot process transaction in status: " + status);
        }
        
        this.status = TransactionStatus.PROCESSING;
        addEvent(TransactionEvent.processing(this));
    }

    /**
     * Complete the transaction
     */
    public void complete() {
        if (!status.canTransitionTo(TransactionStatus.COMPLETED)) {
            throw new IllegalStateException("Cannot complete transaction in status: " + status);
        }
        
        this.status = TransactionStatus.COMPLETED;
        this.executedAt = LocalDateTime.now();
        addEvent(TransactionEvent.completed(this));
    }

    /**
     * Fail the transaction
     */
    public void fail(String reasonCode) {
        if (!status.canTransitionTo(TransactionStatus.FAILED)) {
            throw new IllegalStateException("Cannot fail transaction in status: " + status);
        }
        
        this.status = TransactionStatus.FAILED;
        this.reasonCode = reasonCode;
        addEvent(TransactionEvent.failed(this, reasonCode));
    }

    /**
     * Cancel the transaction
     */
    public void cancel(String reasonCode) {
        if (!status.canTransitionTo(TransactionStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel transaction in status: " + status);
        }
        
        this.status = TransactionStatus.CANCELLED;
        this.reasonCode = reasonCode;
        addEvent(TransactionEvent.cancelled(this, reasonCode));
    }

    /**
     * Reverse the transaction
     */
    public void reverse(String reasonCode) {
        if (!status.canTransitionTo(TransactionStatus.REVERSED)) {
            throw new IllegalStateException("Cannot reverse transaction in status: " + status);
        }
        
        this.status = TransactionStatus.REVERSED;
        this.reasonCode = reasonCode;
        addEvent(TransactionEvent.reversed(this, reasonCode));
    }

    /**
     * Get total amount including fees
     */
    public Money getTotalAmount() {
        return amount.add(fee);
    }

    /**
     * Check if transaction involves account
     */
    public boolean involvesAccount(AccountId accountId) {
        return sourceAccountId.equals(accountId) || 
               (destinationAccountId != null && destinationAccountId.equals(accountId));
    }

    /**
     * Check if transaction is completed
     */
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * Check if transaction is terminal
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * Get destination account if exists
     */
    public Optional<AccountId> getDestinationAccount() {
        return Optional.ofNullable(destinationAccountId);
    }

    /**
     * Get events (immutable view)
     */
    public List<TransactionEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Clear events (for event sourcing persistence)
     */
    public void clearEvents() {
        events.clear();
    }

    // Private helper methods

    private void addEvent(TransactionEvent event) {
        events.add(event);
    }

    private static void validateDepositParameters(AccountId accountId, Money amount) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null || amount.isZero()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private static void validateWithdrawalParameters(AccountId accountId, Money amount) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null || amount.isZero()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private static void validateTransferParameters(AccountId sourceAccountId, 
                                                 AccountId destinationAccountId, 
                                                 Money amount) {
        if (sourceAccountId == null) {
            throw new IllegalArgumentException("Source Account ID cannot be null");
        }
        if (destinationAccountId == null) {
            throw new IllegalArgumentException("Destination Account ID cannot be null");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }
        if (amount == null || amount.isZero()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}