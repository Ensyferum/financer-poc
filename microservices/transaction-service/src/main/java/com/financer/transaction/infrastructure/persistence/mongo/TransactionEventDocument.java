package com.financer.transaction.infrastructure.persistence.mongo;

import com.financer.transaction.domain.model.TransactionId;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB Document for Transaction Events (Event Sourcing)
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Document(collection = "transaction_events")
@Data
public class TransactionEventDocument {

    @Id
    private String id;

    @Indexed
    private String transactionId;

    @Indexed
    private String sourceAccountId;

    private String destinationAccountId;

    private BigDecimal amount;

    private String currency;

    private BigDecimal fee;

    private String type;

    @Indexed
    private String status;

    private String description;

    private String reference;

    @Indexed
    private String correlationId;

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime executedAt;

    private String reasonCode;

    private String metadata;

    // Event sourcing fields
    private List<EventData> events;

    @Data
    public static class EventData {
        private String eventId;
        private String eventType;
        private LocalDateTime occurredAt;
        private String payload;
        private String metadata;
    }
}