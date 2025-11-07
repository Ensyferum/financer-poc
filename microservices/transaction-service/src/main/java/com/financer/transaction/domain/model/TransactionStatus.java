package com.financer.transaction.domain.model;

/**
 * Transaction Status enumeration
 * 
 * @author Financer Team
 * @version 1.0.0
 */
public enum TransactionStatus {
    
    PENDING("Pendente", "Transaction created but not processed yet"),
    PROCESSING("Processando", "Transaction is being processed"),
    COMPLETED("Concluída", "Transaction successfully completed"),
    FAILED("Falhou", "Transaction failed due to business rules or technical issues"),
    CANCELLED("Cancelada", "Transaction was cancelled by user or system"),
    REVERSED("Revertida", "Transaction was reversed/rolled back");

    private final String description;
    private final String details;

    TransactionStatus(String description, String details) {
        this.description = description;
        this.details = details;
    }

    public String getDescription() {
        return description;
    }

    public String getDetails() {
        return details;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REVERSED;
    }

    public boolean canTransitionTo(TransactionStatus newStatus) {
        // Prevent transitions from terminal states
        if (this.isTerminal() && newStatus != this) {
            return false;
        }

        return switch (this) {
            case PENDING -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == COMPLETED || newStatus == FAILED || newStatus == CANCELLED;
            case COMPLETED -> newStatus == REVERSED;
            case FAILED, CANCELLED, REVERSED -> false;
        };
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    public boolean canBeReversed() {
        return this == COMPLETED;
    }
}