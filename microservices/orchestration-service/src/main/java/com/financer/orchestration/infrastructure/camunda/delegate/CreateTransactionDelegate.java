package com.financer.orchestration.infrastructure.camunda.delegate;

import com.financer.orchestration.infrastructure.feign.TransactionServiceClient;
import com.financer.shared.logging.FinancerLogger;
import com.financer.shared.logging.ExecutionStep;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * CAMUNDA delegate for creating transactions
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Component("createTransactionDelegate")
@RequiredArgsConstructor
public class CreateTransactionDelegate implements JavaDelegate {

    private final TransactionServiceClient transactionServiceClient;
    private final FinancerLogger logger = FinancerLogger.getLogger(CreateTransactionDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(ExecutionStep.START, "Executing create transaction delegate");

        try {
            // Extract variables from process
            String sagaId = (String) execution.getVariable("sagaId");
            String sourceAccountId = (String) execution.getVariable("sourceAccountId");
            String destinationAccountId = (String) execution.getVariable("destinationAccountId");
            BigDecimal amount = (BigDecimal) execution.getVariable("amount");
            String currency = (String) execution.getVariable("currency");
            String transactionType = (String) execution.getVariable("transactionType");

            logger.info(ExecutionStep.PROCESSING, "Creating transaction: sagaId={}, amount={}, type={}", 
                       sagaId, amount, transactionType);

            // Call transaction service
            var request = CreateTransactionRequest.builder()
                    .sourceAccountId(sourceAccountId)
                    .destinationAccountId(destinationAccountId)
                    .amount(amount)
                    .currency(currency)
                    .type(transactionType)
                    .correlationId(sagaId)
                    .build();

            var response = transactionServiceClient.createTransaction(request);

            // Store transaction ID for compensation
            execution.setVariable("transactionId", response.getId());
            execution.setVariable("transactionStatus", "CREATED");

            logger.info(ExecutionStep.FINISH, "Transaction created successfully: transactionId={}", 
                       response.getId());

        } catch (Exception e) {
            logger.error(ExecutionStep.ERROR, "Failed to create transaction: {}", e.getMessage(), e);
            execution.setVariable("transactionError", e.getMessage());
            throw e;
        }
    }

    // Helper DTO classes
    public static class CreateTransactionRequest {
        private String sourceAccountId;
        private String destinationAccountId;
        private BigDecimal amount;
        private String currency;
        private String type;
        private String correlationId;

        public static CreateTransactionRequestBuilder builder() {
            return new CreateTransactionRequestBuilder();
        }

        public static class CreateTransactionRequestBuilder {
            private CreateTransactionRequest request = new CreateTransactionRequest();

            public CreateTransactionRequestBuilder sourceAccountId(String sourceAccountId) {
                request.sourceAccountId = sourceAccountId;
                return this;
            }

            public CreateTransactionRequestBuilder destinationAccountId(String destinationAccountId) {
                request.destinationAccountId = destinationAccountId;
                return this;
            }

            public CreateTransactionRequestBuilder amount(BigDecimal amount) {
                request.amount = amount;
                return this;
            }

            public CreateTransactionRequestBuilder currency(String currency) {
                request.currency = currency;
                return this;
            }

            public CreateTransactionRequestBuilder type(String type) {
                request.type = type;
                return this;
            }

            public CreateTransactionRequestBuilder correlationId(String correlationId) {
                request.correlationId = correlationId;
                return this;
            }

            public CreateTransactionRequest build() {
                return request;
            }
        }
    }
}