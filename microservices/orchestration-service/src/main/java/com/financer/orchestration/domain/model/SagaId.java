package com.financer.orchestration.domain.model;

import com.financer.shared.validation.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * Value Object representing a Saga ID
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Getter
@EqualsAndHashCode
public class SagaId implements ValueObject {
    
    private final UUID value;

    private SagaId(UUID value) {
        this.value = value;
    }

    public static SagaId of(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Saga ID cannot be null");
        }
        return new SagaId(value);
    }

    public static SagaId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Saga ID cannot be null or empty");
        }
        try {
            return new SagaId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Saga ID format: " + value);
        }
    }

    public static SagaId generate() {
        return new SagaId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}