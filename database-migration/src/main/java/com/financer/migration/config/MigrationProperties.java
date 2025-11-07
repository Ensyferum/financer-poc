package com.financer.migration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Migration configuration properties loaded from Config Server
 */
@Configuration
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    private PostgresConfig postgres = new PostgresConfig();
    private MongodbConfig mongodb = new MongodbConfig();
    private ControlConfig control = new ControlConfig();
    private ExecutionConfig execution = new ExecutionConfig();

    // Getters and Setters
    public PostgresConfig getPostgres() { return postgres; }
    public void setPostgres(PostgresConfig postgres) { this.postgres = postgres; }

    public MongodbConfig getMongodb() { return mongodb; }
    public void setMongodb(MongodbConfig mongodb) { this.mongodb = mongodb; }

    public ControlConfig getControl() { return control; }
    public void setControl(ControlConfig control) { this.control = control; }

    public ExecutionConfig getExecution() { return execution; }
    public void setExecution(ExecutionConfig execution) { this.execution = execution; }

    public static class PostgresConfig {
        private String url;
        private String username;
        private String password;
        private String schema = "public";

        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
    }

    public static class MongodbConfig {
        private String url;
        private String database;

        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
    }

    public static class ControlConfig {
        private boolean enabled = true;
        private String tableName = "migration_execution_history";

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
    }

    public static class ExecutionConfig {
        private boolean generateReport = true;
        private String reportFormat = "JSON";
        private int maxRetries = 3;
        private int timeoutMinutes = 30;

        // Getters and Setters
        public boolean isGenerateReport() { return generateReport; }
        public void setGenerateReport(boolean generateReport) { this.generateReport = generateReport; }

        public String getReportFormat() { return reportFormat; }
        public void setReportFormat(String reportFormat) { this.reportFormat = reportFormat; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public int getTimeoutMinutes() { return timeoutMinutes; }
        public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
    }
}