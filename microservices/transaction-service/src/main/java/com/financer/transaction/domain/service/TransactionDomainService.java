package com.financer.transaction.domain.service;

import com.financer.transaction.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Transaction Domain Service
 * 
 * Core business logic for transaction processing with functional programming patterns.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionDomainService {

    private final AccountService accountService;

    /**
     * Validate transaction business rules
     */
    public Mono<ValidationResult> validateTransaction(Transaction transaction) {
        return switch (transaction.getType()) {
            case DEPOSIT -> validateDeposit(transaction);
            case WITHDRAWAL -> validateWithdrawal(transaction);
            case TRANSFER -> validateTransfer(transaction);
            case PAYMENT -> validatePayment(transaction);
            case REFUND -> validateRefund(transaction);
            case ADJUSTMENT -> validateAdjustment(transaction);
            case FEE -> validateFee(transaction);
            case INTEREST -> validateInterest(transaction);
        };
    }

    /**
     * Calculate transaction fees
     */
    public Mono<Money> calculateFees(Transaction transaction) {
        return switch (transaction.getType()) {
            case DEPOSIT -> Mono.just(Money.zero(transaction.getAmount().getCurrency()));
            case WITHDRAWAL -> calculateWithdrawalFees(transaction);
            case TRANSFER -> calculateTransferFees(transaction);
            case PAYMENT -> calculatePaymentFees(transaction);
            case REFUND -> Mono.just(Money.zero(transaction.getAmount().getCurrency()));
            case ADJUSTMENT -> Mono.just(Money.zero(transaction.getAmount().getCurrency()));
            case FEE -> Mono.just(Money.zero(transaction.getAmount().getCurrency()));
            case INTEREST -> Mono.just(Money.zero(transaction.getAmount().getCurrency()));
        };
    }

    /**
     * Execute transaction business logic
     */
    public Mono<Void> executeTransaction(Transaction transaction) {
        log.info("Executing transaction: {} of type: {}", transaction.getId(), transaction.getType());
        
        return switch (transaction.getType()) {
            case DEPOSIT -> executeDeposit(transaction);
            case WITHDRAWAL -> executeWithdrawal(transaction);
            case TRANSFER -> executeTransfer(transaction);
            case PAYMENT -> executePayment(transaction);
            case REFUND -> executeRefund(transaction);
            case ADJUSTMENT -> executeAdjustment(transaction);
            case FEE -> executeFee(transaction);
            case INTEREST -> executeInterest(transaction);
        };
    }

    // Private validation methods

    private Mono<ValidationResult> validateDeposit(Transaction transaction) {
        return accountService.isAccountActiveAndExists(transaction.getSourceAccountId())
            .map(exists -> exists ? 
                ValidationResult.valid() : 
                ValidationResult.invalid("Account does not exist or is inactive"))
            .onErrorReturn(ValidationResult.invalid("Error validating account"));
    }

    private Mono<ValidationResult> validateWithdrawal(Transaction transaction) {
        return accountService.isAccountActiveAndExists(transaction.getSourceAccountId())
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.just(ValidationResult.invalid("Account does not exist or is inactive"));
                }
                return accountService.hasSufficientBalance(
                    transaction.getSourceAccountId(), 
                    transaction.getTotalAmount().getAmount()
                )
                .map(hasSufficientBalance -> hasSufficientBalance ? 
                    ValidationResult.valid() : 
                    ValidationResult.invalid("Insufficient balance"));
            })
            .onErrorReturn(ValidationResult.invalid("Error validating withdrawal"));
    }

    private Mono<ValidationResult> validateTransfer(Transaction transaction) {
        Mono<Boolean> sourceValid = accountService.isAccountActiveAndExists(transaction.getSourceAccountId());
        Mono<Boolean> destinationValid = transaction.getDestinationAccount()
            .map(accountService::isAccountActiveAndExists)
            .orElse(Mono.just(false));
        Mono<Boolean> balanceValid = accountService.hasSufficientBalance(
            transaction.getSourceAccountId(), 
            transaction.getTotalAmount().getAmount()
        );

        return Mono.zip(sourceValid, destinationValid, balanceValid)
            .map(tuple -> {
                if (!tuple.getT1()) {
                    return ValidationResult.invalid("Source account does not exist or is inactive");
                }
                if (!tuple.getT2()) {
                    return ValidationResult.invalid("Destination account does not exist or is inactive");
                }
                if (!tuple.getT3()) {
                    return ValidationResult.invalid("Insufficient balance");
                }
                return ValidationResult.valid();
            })
            .onErrorReturn(ValidationResult.invalid("Error validating transfer"));
    }

    private Mono<ValidationResult> validatePayment(Transaction transaction) {
        return validateWithdrawal(transaction); // Same rules as withdrawal
    }

    private Mono<ValidationResult> validateRefund(Transaction transaction) {
        return validateDeposit(transaction); // Same rules as deposit
    }

    private Mono<ValidationResult> validateAdjustment(Transaction transaction) {
        return accountService.isAccountActiveAndExists(transaction.getSourceAccountId())
            .map(exists -> exists ? 
                ValidationResult.valid() : 
                ValidationResult.invalid("Account does not exist or is inactive"))
            .onErrorReturn(ValidationResult.invalid("Error validating adjustment"));
    }

    private Mono<ValidationResult> validateFee(Transaction transaction) {
        return validateWithdrawal(transaction); // Same rules as withdrawal
    }

    private Mono<ValidationResult> validateInterest(Transaction transaction) {
        return validateDeposit(transaction); // Same rules as deposit
    }

    // Private fee calculation methods

    private Mono<Money> calculateWithdrawalFees(Transaction transaction) {
        // Simple fee calculation - 0.1% of amount, minimum $1.00
        Money baseAmount = transaction.getAmount();
        Money calculatedFee = baseAmount.multiply(java.math.BigDecimal.valueOf(0.001));
        Money minimumFee = Money.of(1.00, baseAmount.getCurrency());
        
        Money finalFee = calculatedFee.isGreaterThan(minimumFee) ? calculatedFee : minimumFee;
        return Mono.just(finalFee);
    }

    private Mono<Money> calculateTransferFees(Transaction transaction) {
        // Transfer fee: $2.00 flat fee
        return Mono.just(Money.of(2.00, transaction.getAmount().getCurrency()));
    }

    private Mono<Money> calculatePaymentFees(Transaction transaction) {
        return calculateWithdrawalFees(transaction); // Same as withdrawal
    }

    // Private execution methods

    private Mono<Void> executeDeposit(Transaction transaction) {
        return accountService.credit(
            transaction.getSourceAccountId(),
            transaction.getAmount().getAmount(),
            transaction.getCorrelationId()
        );
    }

    private Mono<Void> executeWithdrawal(Transaction transaction) {
        return accountService.debit(
            transaction.getSourceAccountId(),
            transaction.getTotalAmount().getAmount(),
            transaction.getCorrelationId()
        );
    }

    private Mono<Void> executeTransfer(Transaction transaction) {
        return transaction.getDestinationAccount()
            .map(destinationId -> 
                accountService.debit(
                    transaction.getSourceAccountId(),
                    transaction.getTotalAmount().getAmount(),
                    transaction.getCorrelationId()
                ).then(
                    accountService.credit(
                        destinationId,
                        transaction.getAmount().getAmount(),
                        transaction.getCorrelationId()
                    )
                )
            )
            .orElse(Mono.error(new IllegalStateException("Transfer requires destination account")));
    }

    private Mono<Void> executePayment(Transaction transaction) {
        return executeWithdrawal(transaction); // Same as withdrawal
    }

    private Mono<Void> executeRefund(Transaction transaction) {
        return executeDeposit(transaction); // Same as deposit
    }

    private Mono<Void> executeAdjustment(Transaction transaction) {
        return executeDeposit(transaction); // Adjustments are typically credits
    }

    private Mono<Void> executeFee(Transaction transaction) {
        return executeWithdrawal(transaction); // Fees are debits
    }

    private Mono<Void> executeInterest(Transaction transaction) {
        return executeDeposit(transaction); // Interest is typically credit
    }

    /**
     * Validation Result inner class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}