package com.financer.transaction.domain.model;

import com.financer.shared.validation.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * Value Object representing a Transaction ID
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Getter
@EqualsAndHashCode
public class TransactionId implements ValueObject {
    
    private final UUID value;

    private TransactionId(UUID value) {
        this.value = value;
    }

    public static TransactionId of(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        return new TransactionId(value);
    }

    public static TransactionId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        try {
            return new TransactionId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Transaction ID format: " + value);
        }
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}