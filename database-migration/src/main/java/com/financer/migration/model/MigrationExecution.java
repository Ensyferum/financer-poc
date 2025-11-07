package com.financer.migration.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a migration execution record
 */
public class MigrationExecution {
    
    private String executionId;
    private String command;
    private String environment;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ExecutionStatus status;
    private String errorMessage;
    private List<MigrationResult> postgresResults;
    private List<String> mongodbResults;
    private String executedBy;
    private String reportPath;
    
    public enum ExecutionStatus {
        STARTED, IN_PROGRESS, SUCCESS, PARTIAL_SUCCESS, FAILED, CANCELLED
    }
    
    // Constructors
    public MigrationExecution() {}
    
    public MigrationExecution(String executionId, String command, String environment) {
        this.executionId = executionId;
        this.command = command;
        this.environment = environment;
        this.startTime = LocalDateTime.now();
        this.status = ExecutionStatus.STARTED;
        this.executedBy = System.getProperty("user.name", "system");
    }
    
    // Getters and Setters
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public List<MigrationResult> getPostgresResults() { return postgresResults; }
    public void setPostgresResults(List<MigrationResult> postgresResults) { this.postgresResults = postgresResults; }
    
    public List<String> getMongodbResults() { return mongodbResults; }
    public void setMongodbResults(List<String> mongodbResults) { this.mongodbResults = mongodbResults; }
    
    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    
    public String getReportPath() { return reportPath; }
    public void setReportPath(String reportPath) { this.reportPath = reportPath; }
    
    // Helper methods
    public long getDurationInSeconds() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).getSeconds();
        }
        return 0;
    }
    
    public boolean isSuccessful() {
        return status == ExecutionStatus.SUCCESS || status == ExecutionStatus.PARTIAL_SUCCESS;
    }
    
    public void markAsCompleted() {
        this.endTime = LocalDateTime.now();
        if (this.status == ExecutionStatus.IN_PROGRESS || this.status == ExecutionStatus.STARTED) {
            this.status = ExecutionStatus.SUCCESS;
        }
    }
    
    public void markAsFailed(String errorMessage) {
        this.endTime = LocalDateTime.now();
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}