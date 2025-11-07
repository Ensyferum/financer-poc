package com.financer.transaction.application.usecase;

import com.financer.shared.logging.FinancerLogger;
import com.financer.shared.logging.Domain;
import com.financer.shared.logging.ExecutionStep;
import com.financer.transaction.application.dto.TransactionResponse;
import com.financer.transaction.domain.model.Transaction;
import com.financer.transaction.domain.model.TransactionId;
import com.financer.transaction.domain.repository.TransactionRepository;
import com.financer.transaction.domain.service.TransactionDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Process Transaction Use Case
 * 
 * Application service for processing pending transactions.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ProcessTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final TransactionDomainService domainService;
    private final FinancerLogger logger = FinancerLogger.getLogger(ProcessTransactionUseCase.class);

    public Mono<TransactionResponse> execute(String transactionId) {
        logger.startContext(Domain.TRANSACTION, "processTransaction", transactionId);
        logger.info(ExecutionStep.START, "Processing transaction: " + transactionId);

        return findTransaction(transactionId)
            .flatMap(this::processTransaction)
            .flatMap(this::executeBusinessLogic)
            .flatMap(this::completeTransaction)
            .flatMap(this::saveTransaction)
            .map(this::convertToResponse)
            .onErrorResume(this::handleProcessingError)
            .doOnSuccess(response -> {
                logger.info(ExecutionStep.FINISH, "Transaction processed successfully: " + response.getId());
            });
    }

    private Mono<Transaction> findTransaction(String transactionId) {
        logger.info(ExecutionStep.PERSISTENCE, "Finding transaction: " + transactionId);

        try {
            TransactionId id = TransactionId.of(transactionId);
            return transactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Transaction not found: " + transactionId)));
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Invalid transaction ID format: " + transactionId));
        }
    }

    private Mono<Transaction> processTransaction(Transaction transaction) {
        logger.info(ExecutionStep.PROCESSING, "Changing transaction status to PROCESSING");

        try {
            transaction.process();
            return Mono.just(transaction);
        } catch (Exception e) {
            return Mono.error(new IllegalStateException("Cannot process transaction: " + e.getMessage()));
        }
    }

    private Mono<Transaction> executeBusinessLogic(Transaction transaction) {
        logger.info(ExecutionStep.PROCESSING, "Executing transaction business logic");

        return domainService.executeTransaction(transaction)
            .then(Mono.just(transaction))
            .onErrorMap(error -> new IllegalStateException("Transaction execution failed: " + error.getMessage()));
    }

    private Mono<Transaction> completeTransaction(Transaction transaction) {
        logger.info(ExecutionStep.PROCESSING, "Completing transaction");

        try {
            transaction.complete();
            return Mono.just(transaction);
        } catch (Exception e) {
            return Mono.error(new IllegalStateException("Cannot complete transaction: " + e.getMessage()));
        }
    }

    private Mono<Transaction> saveTransaction(Transaction transaction) {
        logger.info(ExecutionStep.PERSISTENCE, "Saving processed transaction");
        return transactionRepository.save(transaction);
    }

    private Mono<TransactionResponse> handleProcessingError(Throwable error) {
        logger.error(ExecutionStep.ERROR, "Transaction processing failed: " + error.getMessage(), error);

        // For now, we'll return a generic error response
        // In a real implementation, you might want to extract transaction ID from the context differently
        return Mono.error(error);
    }

    private TransactionResponse convertToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId().toString());
        response.setSourceAccountId(transaction.getSourceAccountId().toString());
        response.setDestinationAccountId(
            transaction.getDestinationAccount()
                .map(accountId -> accountId.toString())
                .orElse(null)
        );
        response.setAmount(transaction.getAmount().getAmount());
        response.setCurrency(transaction.getAmount().getCurrency());
        response.setFee(transaction.getFee().getAmount());
        response.setTotalAmount(transaction.getTotalAmount().getAmount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setReference(transaction.getReference());
        response.setCorrelationId(transaction.getCorrelationId());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        response.setExecutedAt(transaction.getExecutedAt());
        response.setReasonCode(transaction.getReasonCode());
        response.setMetadata(transaction.getMetadata());

        return response;
    }

    private TransactionResponse createErrorResponse(String transactionId, Throwable error) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transactionId);
        response.setReasonCode("PROCESSING_ERROR");
        response.setMetadata("Error: " + error.getMessage());
        return response;
    }
}