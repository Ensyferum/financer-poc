package com.financer.migration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.financer.migration.config.MigrationProperties;
import com.financer.migration.model.MigrationExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating migration execution reports
 */
@Service
public class MigrationReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationReportService.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    
    @Autowired
    private MigrationProperties migrationProperties;
    
    private final ObjectMapper objectMapper;
    
    public MigrationReportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Generate execution report
     */
    public String generateReport(MigrationExecution execution) {
        if (!migrationProperties.getExecution().isGenerateReport()) {
            logger.info("Report generation is disabled");
            return null;
        }
        
        try {
            // Create reports directory if it doesn't exist
            Path reportsDir = Paths.get("logs", "migration-reports");
            Files.createDirectories(reportsDir);
            
            // Generate filename with timestamp
            String timestamp = execution.getStartTime().format(FILENAME_FORMATTER);
            String filename = String.format("migration-report-%s-%s.json", 
                                           timestamp, execution.getExecutionId());
            Path reportPath = reportsDir.resolve(filename);
            
            // Create report content
            MigrationReport report = createReportContent(execution);
            
            // Write report to file
            objectMapper.writeValue(reportPath.toFile(), report);
            
            String reportFilePath = reportPath.toString();
            execution.setReportPath(reportFilePath);
            
            logger.info("Migration report generated: {}", reportFilePath);
            return reportFilePath;
            
        } catch (IOException e) {
            logger.error("Failed to generate migration report", e);
            return null;
        }
    }
    
    /**
     * Generate summary report for console output
     */
    public void generateSummaryReport(MigrationExecution execution) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("\n")
               .append("╔══════════════════════════════════════════════════════════════╗\n")
               .append("║                    MIGRATION EXECUTION REPORT               ║\n")
               .append("╠══════════════════════════════════════════════════════════════╣\n");
        
        // Basic information
        summary.append(String.format("║ Execution ID: %-42s ║\n", execution.getExecutionId()));
        summary.append(String.format("║ Command: %-47s ║\n", execution.getCommand()));
        summary.append(String.format("║ Environment: %-43s ║\n", execution.getEnvironment()));
        summary.append(String.format("║ Status: %-48s ║\n", getStatusWithEmoji(execution.getStatus())));
        summary.append(String.format("║ Duration: %-44s ║\n", formatDuration(execution.getDurationInSeconds())));
        summary.append(String.format("║ Executed by: %-43s ║\n", execution.getExecutedBy()));
        
        summary.append("╠══════════════════════════════════════════════════════════════╣\n");
        
        // PostgreSQL Results
        if (execution.getPostgresResults() != null) {
            summary.append(String.format("║ PostgreSQL Migrations: %-33s ║\n", 
                                        execution.getPostgresResults().size() + " executed"));
            
            long successfulPostgres = execution.getPostgresResults().stream()
                                               .mapToLong(r -> r.isSuccessful() ? 1 : 0)
                                               .sum();
            
            summary.append(String.format("║ ├─ Successful: %-38s ║\n", successfulPostgres));
            summary.append(String.format("║ └─ Failed: %-43s ║\n", 
                                        execution.getPostgresResults().size() - successfulPostgres));
        }
        
        // MongoDB Results
        if (execution.getMongodbResults() != null) {
            summary.append(String.format("║ MongoDB Operations: %-37s ║\n", 
                                        execution.getMongodbResults().size() + " executed"));
        }
        
        summary.append("╠══════════════════════════════════════════════════════════════╣\n");
        
        // Error information
        if (execution.getErrorMessage() != null) {
            summary.append("║ ERROR DETAILS:                                               ║\n");
            String[] errorLines = execution.getErrorMessage().split("\n");
            for (String line : errorLines) {
                if (line.length() > 56) {
                    line = line.substring(0, 53) + "...";
                }
                summary.append(String.format("║ %s%-56s ║\n", "  ", line));
            }
            summary.append("╠══════════════════════════════════════════════════════════════╣\n");
        }
        
        // Report file path
        if (execution.getReportPath() != null) {
            summary.append(String.format("║ Report saved to: %-40s ║\n", 
                                        shortenPath(execution.getReportPath(), 40)));
        }
        
        summary.append("╚══════════════════════════════════════════════════════════════╝\n");
        
        System.out.println(summary.toString());
        logger.info("Migration execution completed - ID: {}, Status: {}, Duration: {}s", 
                   execution.getExecutionId(), execution.getStatus(), execution.getDurationInSeconds());
    }
    
    private MigrationReport createReportContent(MigrationExecution execution) {
        MigrationReport report = new MigrationReport();
        report.setExecutionId(execution.getExecutionId());
        report.setCommand(execution.getCommand());
        report.setEnvironment(execution.getEnvironment());
        report.setStartTime(execution.getStartTime());
        report.setEndTime(execution.getEndTime());
        report.setStatus(execution.getStatus().name());
        report.setDurationSeconds(execution.getDurationInSeconds());
        report.setErrorMessage(execution.getErrorMessage());
        report.setPostgresResults(execution.getPostgresResults());
        report.setMongodbResults(execution.getMongodbResults());
        report.setExecutedBy(execution.getExecutedBy());
        report.setGeneratedAt(LocalDateTime.now());
        
        // Add system information
        report.setSystemInfo(new MigrationReport.SystemInfo());
        
        return report;
    }
    
    private String getStatusWithEmoji(MigrationExecution.ExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> "✅ " + status.name();
            case PARTIAL_SUCCESS -> "⚠️ " + status.name();
            case FAILED -> "❌ " + status.name();
            case IN_PROGRESS -> "🔄 " + status.name();
            case CANCELLED -> "⏹️ " + status.name();
            default -> "📝 " + status.name();
        };
    }
    
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return hours + "h " + minutes + "m " + secs + "s";
        }
    }
    
    private String shortenPath(String path, int maxLength) {
        if (path.length() <= maxLength) {
            return path;
        }
        return "..." + path.substring(path.length() - maxLength + 3);
    }
    
    /**
     * Report data structure for JSON serialization
     */
    public static class MigrationReport {
        private String executionId;
        private String command;
        private String environment;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private long durationSeconds;
        private String errorMessage;
        private Object postgresResults;
        private Object mongodbResults;
        private String executedBy;
        private LocalDateTime generatedAt;
        private SystemInfo systemInfo;
        
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
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Object getPostgresResults() { return postgresResults; }
        public void setPostgresResults(Object postgresResults) { this.postgresResults = postgresResults; }
        
        public Object getMongodbResults() { return mongodbResults; }
        public void setMongodbResults(Object mongodbResults) { this.mongodbResults = mongodbResults; }
        
        public String getExecutedBy() { return executedBy; }
        public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
        
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        
        public SystemInfo getSystemInfo() { return systemInfo; }
        public void setSystemInfo(SystemInfo systemInfo) { this.systemInfo = systemInfo; }
        
        public static class SystemInfo {
            private String javaVersion = System.getProperty("java.version");
            private String osName = System.getProperty("os.name");
            private String osVersion = System.getProperty("os.version");
            private String userHome = System.getProperty("user.home");
            private String workingDirectory = System.getProperty("user.dir");
            
            // Getters and Setters
            public String getJavaVersion() { return javaVersion; }
            public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
            
            public String getOsName() { return osName; }
            public void setOsName(String osName) { this.osName = osName; }
            
            public String getOsVersion() { return osVersion; }
            public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
            
            public String getUserHome() { return userHome; }
            public void setUserHome(String userHome) { this.userHome = userHome; }
            
            public String getWorkingDirectory() { return workingDirectory; }
            public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }
        }
    }
}