package com.financer.migration.model;

/**
 * Represents the result of a single migration script execution
 */
public class MigrationResult {
    
    private String version;
    private String description;
    private String type; // SQL, BASELINE, etc.
    private String state; // SUCCESS, FAILED, etc.
    private String script;
    private String checksum;
    private long executionTime;
    private String errorDetails;
    
    // Constructors
    public MigrationResult() {}
    
    public MigrationResult(String version, String description, String state) {
        this.version = version;
        this.description = description;
        this.state = state;
    }
    
    // Getters and Setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    
    // Helper methods
    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(state) || "PENDING".equalsIgnoreCase(state);
    }
}