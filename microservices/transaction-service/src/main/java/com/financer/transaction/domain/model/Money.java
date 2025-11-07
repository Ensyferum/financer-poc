package com.financer.transaction.domain.model;

import com.financer.shared.validation.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object representing monetary amounts
 * 
 * Immutable value object for handling money with proper precision and validation.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Getter
@EqualsAndHashCode
public class Money implements ValueObject {
    
    private static final int PRECISION = 2;
    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount.setScale(PRECISION, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return new Money(amount, currency.toUpperCase());
    }

    public static Money of(double amount, String currency) {
        return of(BigDecimal.valueOf(amount), currency);
    }

    public static Money brl(BigDecimal amount) {
        return of(amount, "BRL");
    }

    public static Money brl(double amount) {
        return of(amount, "BRL");
    }

    public static Money zero(String currency) {
        return of(BigDecimal.ZERO, currency);
    }

    public static Money zeroBrl() {
        return zero("BRL");
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return of(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result cannot be negative");
        }
        return of(result, this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative");
        }
        return of(this.amount.multiply(multiplier), this.currency);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        return !isGreaterThanOrEqual(other);
    }

    public boolean isLessThanOrEqual(Money other) {
        return !isGreaterThan(other);
    }

    @Override
    public String toString() {
        return String.format("%s %.2f", currency, amount.doubleValue());
    }
}