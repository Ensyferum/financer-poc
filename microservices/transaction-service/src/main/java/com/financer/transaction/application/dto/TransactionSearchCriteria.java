package com.financer.transaction.application.dto;

import com.financer.transaction.domain.model.TransactionStatus;
import com.financer.transaction.domain.model.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transaction Search Criteria DTO
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
public class TransactionSearchCriteria {

    private String accountId;
    private List<TransactionType> types;
    private List<TransactionStatus> statuses;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String correlationId;
    private String reference;
    private String description;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

    /**
     * Check if account filter is set
     */
    public boolean hasAccountFilter() {
        return accountId != null && !accountId.trim().isEmpty();
    }

    /**
     * Check if type filter is set
     */
    public boolean hasTypeFilter() {
        return types != null && !types.isEmpty();
    }

    /**
     * Check if status filter is set
     */
    public boolean hasStatusFilter() {
        return statuses != null && !statuses.isEmpty();
    }

    /**
     * Check if amount range filter is set
     */
    public boolean hasAmountRangeFilter() {
        return minAmount != null || maxAmount != null;
    }

    /**
     * Check if date range filter is set
     */
    public boolean hasDateRangeFilter() {
        return fromDate != null || toDate != null;
    }

    /**
     * Get effective from date (default to 30 days ago if not set)
     */
    public LocalDateTime getEffectiveFromDate() {
        return fromDate != null ? fromDate : LocalDateTime.now().minusDays(30);
    }

    /**
     * Get effective to date (default to now if not set)
     */
    public LocalDateTime getEffectiveToDate() {
        return toDate != null ? toDate : LocalDateTime.now();
    }

    /**
     * Validate search criteria
     */
    public boolean isValid() {
        if (hasAmountRangeFilter() && minAmount != null && maxAmount != null) {
            return minAmount.compareTo(maxAmount) <= 0;
        }
        if (hasDateRangeFilter() && fromDate != null && toDate != null) {
            return fromDate.isBefore(toDate) || fromDate.isEqual(toDate);
        }
        return true;
    }
}