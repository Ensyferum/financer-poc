package com.financer.orchestration.infrastructure.camunda;

import com.financer.orchestration.domain.model.Saga;
import com.financer.shared.logging.FinancerLogger;
import com.financer.shared.logging.ExecutionStep;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing CAMUNDA workflows for Sagas
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SagaWorkflowService {

    private final RuntimeService runtimeService;
    private final FinancerLogger logger = FinancerLogger.getLogger(SagaWorkflowService.class);

    // Workflow process definition keys
    private static final String TRANSFER_SAGA_PROCESS = "transfer-saga-process";
    private static final String PAYMENT_SAGA_PROCESS = "payment-saga-process";
    private static final String ACCOUNT_CREATION_SAGA_PROCESS = "account-creation-saga-process";

    /**
     * Starts a CAMUNDA workflow for a Saga
     */
    public String startSagaWorkflow(Saga saga, Map<String, String> context) {
        logger.info(ExecutionStep.PROCESSING, "Starting workflow for saga: {} type: {}", 
                   saga.getId(), saga.getSagaType());

        // Prepare process variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("sagaId", saga.getId());
        variables.put("businessKey", saga.getBusinessKey());
        variables.put("sagaType", saga.getSagaType());
        variables.put("correlationId", saga.getCorrelationId());
        
        // Add context variables
        if (context != null) {
            context.forEach(variables::put);
        }

        // Determine process definition key based on saga type
        String processDefinitionKey = getProcessDefinitionKey(saga.getSagaType());
        
        // Start process instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            processDefinitionKey,
            saga.getBusinessKey(),
            variables
        );

        logger.info(ExecutionStep.PROCESSING, "Workflow started: processId={} for saga={}", 
                   processInstance.getProcessInstanceId(), saga.getId());

        return processInstance.getProcessInstanceId();
    }

    /**
     * Signals completion of a saga step
     */
    public void signalStepCompletion(String processInstanceId, String stepName, Map<String, Object> variables) {
        logger.info(ExecutionStep.PROCESSING, "Signaling step completion: process={} step={}", 
                   processInstanceId, stepName);

        String messageName = stepName + "_COMPLETED";
        
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put("stepStatus", "COMPLETED");
        variables.put("completedAt", java.time.LocalDateTime.now());

        runtimeService.createMessageCorrelation(messageName)
                .processInstanceId(processInstanceId)
                .setVariables(variables)
                .correlate();
    }

    /**
     * Signals failure of a saga step
     */
    public void signalStepFailure(String processInstanceId, String stepName, String errorMessage) {
        logger.info(ExecutionStep.PROCESSING, "Signaling step failure: process={} step={} error={}", 
                   processInstanceId, stepName, errorMessage);

        String messageName = stepName + "_FAILED";
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("stepStatus", "FAILED");
        variables.put("errorMessage", errorMessage);
        variables.put("failedAt", java.time.LocalDateTime.now());

        runtimeService.createMessageCorrelation(messageName)
                .processInstanceId(processInstanceId)
                .setVariables(variables)
                .correlate();
    }

    /**
     * Triggers compensation for a saga
     */
    public void triggerCompensation(String processInstanceId, String reason) {
        logger.info(ExecutionStep.PROCESSING, "Triggering compensation: process={} reason={}", 
                   processInstanceId, reason);

        Map<String, Object> variables = new HashMap<>();
        variables.put("compensationReason", reason);
        variables.put("compensationStartedAt", java.time.LocalDateTime.now());

        runtimeService.createMessageCorrelation("TRIGGER_COMPENSATION")
                .processInstanceId(processInstanceId)
                .setVariables(variables)
                .correlate();
    }

    /**
     * Aborts a running workflow
     */
    public void abortWorkflow(String processInstanceId, String reason) {
        logger.info(ExecutionStep.PROCESSING, "Aborting workflow: process={} reason={}", 
                   processInstanceId, reason);

        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    /**
     * Gets workflow status
     */
    public boolean isWorkflowActive(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .active()
                .count() > 0;
    }

    /**
     * Gets process variables
     */
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    private String getProcessDefinitionKey(String sagaType) {
        return switch (sagaType.toUpperCase()) {
            case "TRANSFER" -> TRANSFER_SAGA_PROCESS;
            case "PAYMENT" -> PAYMENT_SAGA_PROCESS;
            case "ACCOUNT_CREATION" -> ACCOUNT_CREATION_SAGA_PROCESS;
            default -> throw new IllegalArgumentException("Unknown saga type: " + sagaType);
        };
    }
}