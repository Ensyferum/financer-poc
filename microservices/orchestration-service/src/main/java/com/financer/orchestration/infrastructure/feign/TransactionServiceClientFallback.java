package com.financer.orchestration.infrastructure.feign;

import com.financer.shared.dto.ApiResponse;
import com.financer.shared.logging.FinancerLogger;
import com.financer.shared.logging.ExecutionStep;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for Transaction Service Client
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Component
public class TransactionServiceClientFallback implements TransactionServiceClient {

    private final FinancerLogger logger = FinancerLogger.getLogger(TransactionServiceClientFallback.class);

    @Override
    public ApiResponse<TransactionResponse> createTransaction(CreateTransactionRequest request) {
        logger.error(ExecutionStep.ERROR, "Transaction service unavailable for create transaction");
        return ApiResponse.error("Transaction service is currently unavailable");
    }

    @Override
    public ApiResponse<TransactionResponse> processTransaction(String transactionId) {
        logger.error(ExecutionStep.ERROR, "Transaction service unavailable for process transaction: {}", transactionId);
        return ApiResponse.error("Transaction service is currently unavailable");
    }

    @Override
    public ApiResponse<Void> cancelTransaction(String transactionId, CancelTransactionRequest request) {
        logger.error(ExecutionStep.ERROR, "Transaction service unavailable for cancel transaction: {}", transactionId);
        return ApiResponse.error("Transaction service is currently unavailable");
    }

    @Override
    public ApiResponse<TransactionResponse> getTransaction(String transactionId) {
        logger.error(ExecutionStep.ERROR, "Transaction service unavailable for get transaction: {}", transactionId);
        return ApiResponse.error("Transaction service is currently unavailable");
    }

    @Override
    public ApiResponse<TransactionResponse> getTransactionByCorrelationId(String correlationId) {
        logger.error(ExecutionStep.ERROR, "Transaction service unavailable for get transaction by correlation: {}", correlationId);
        return ApiResponse.error("Transaction service is currently unavailable");
    }
}