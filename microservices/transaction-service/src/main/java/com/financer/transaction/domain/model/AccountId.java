package com.financer.transaction.domain.model;

import com.financer.shared.validation.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * Value Object representing an Account ID
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Getter
@EqualsAndHashCode
public class AccountId implements ValueObject {
    
    private final UUID value;

    private AccountId(UUID value) {
        this.value = value;
    }

    public static AccountId of(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        return new AccountId(value);
    }

    public static AccountId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        try {
            return new AccountId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Account ID format: " + value);
        }
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}