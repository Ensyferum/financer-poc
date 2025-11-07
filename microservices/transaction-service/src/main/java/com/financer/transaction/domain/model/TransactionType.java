package com.financer.transaction.domain.model;

/**
 * Transaction Types enumeration
 * 
 * @author Financer Team
 * @version 1.0.0
 */
public enum TransactionType {
    
    DEPOSIT("Depósito", "Credit operation - money coming into account"),
    WITHDRAWAL("Saque", "Debit operation - money leaving account"),
    TRANSFER("Transferência", "Money transfer between accounts"),
    PAYMENT("Pagamento", "Payment to external parties"),
    REFUND("Estorno", "Refund of previous transaction"),
    ADJUSTMENT("Ajuste", "Manual balance adjustment"),
    FEE("Taxa", "Service fee charge"),
    INTEREST("Juros", "Interest credit or debit");

    private final String description;
    private final String details;

    TransactionType(String description, String details) {
        this.description = description;
        this.details = details;
    }

    public String getDescription() {
        return description;
    }

    public String getDetails() {
        return details;
    }

    public boolean isCredit() {
        return this == DEPOSIT || this == REFUND || this == INTEREST;
    }

    public boolean isDebit() {
        return this == WITHDRAWAL || this == PAYMENT || this == FEE;
    }

    public boolean requiresDestination() {
        return this == TRANSFER;
    }

    public boolean allowsNegativeBalance() {
        return this == ADJUSTMENT || this == FEE;
    }

    public boolean requiresFee() {
        return this == WITHDRAWAL || this == TRANSFER || this == PAYMENT;
    }
}