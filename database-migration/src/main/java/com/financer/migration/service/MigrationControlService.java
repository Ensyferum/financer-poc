package com.financer.migration.service;

import com.financer.migration.config.MigrationProperties;
import com.financer.migration.model.MigrationExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for tracking migration executions in the database
 */
@Service
public class MigrationControlService {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationControlService.class);
    
    @Autowired
    private MigrationProperties migrationProperties;
    
    /**
     * Initialize the migration control table if it doesn't exist
     */
    public void initializeControlTable() {
        if (!migrationProperties.getControl().isEnabled()) {
            logger.info("Migration control is disabled, skipping control table initialization");
            return;
        }
        
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS %s (
                execution_id VARCHAR(36) PRIMARY KEY,
                command VARCHAR(50) NOT NULL,
                environment VARCHAR(20) NOT NULL,
                start_time TIMESTAMP NOT NULL,
                end_time TIMESTAMP,
                status VARCHAR(20) NOT NULL,
                error_message TEXT,
                postgres_migrations_count INTEGER DEFAULT 0,
                mongodb_migrations_count INTEGER DEFAULT 0,
                executed_by VARCHAR(100) NOT NULL,
                report_path VARCHAR(500),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.formatted(migrationProperties.getControl().getTableName());
        
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            
            statement.execute(createTableSql);
            logger.info("Migration control table '{}' initialized successfully", 
                       migrationProperties.getControl().getTableName());
            
        } catch (SQLException e) {
            logger.error("Failed to initialize migration control table", e);
            throw new RuntimeException("Failed to initialize migration control table", e);
        }
    }
    
    /**
     * Start a new migration execution record
     */
    public MigrationExecution startExecution(String command, String environment) {
        if (!migrationProperties.getControl().isEnabled()) {
            // Return a basic execution object without persisting
            return new MigrationExecution(UUID.randomUUID().toString(), command, environment);
        }
        
        MigrationExecution execution = new MigrationExecution(
            UUID.randomUUID().toString(), command, environment);
        
        String insertSql = """
            INSERT INTO %s (execution_id, command, environment, start_time, status, executed_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """.formatted(migrationProperties.getControl().getTableName());
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql)) {
            
            statement.setString(1, execution.getExecutionId());
            statement.setString(2, execution.getCommand());
            statement.setString(3, execution.getEnvironment());
            statement.setTimestamp(4, Timestamp.valueOf(execution.getStartTime()));
            statement.setString(5, execution.getStatus().name());
            statement.setString(6, execution.getExecutedBy());
            
            statement.executeUpdate();
            logger.info("Started migration execution: {}", execution.getExecutionId());
            
        } catch (SQLException e) {
            logger.error("Failed to start migration execution record", e);
            // Don't fail the migration because of control table issues
        }
        
        return execution;
    }
    
    /**
     * Update execution with progress
     */
    public void updateExecutionProgress(MigrationExecution execution) {
        if (!migrationProperties.getControl().isEnabled()) {
            return;
        }
        
        String updateSql = """
            UPDATE %s SET 
                status = ?, 
                postgres_migrations_count = ?, 
                mongodb_migrations_count = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE execution_id = ?
            """.formatted(migrationProperties.getControl().getTableName());
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(updateSql)) {
            
            statement.setString(1, execution.getStatus().name());
            statement.setInt(2, execution.getPostgresResults() != null ? execution.getPostgresResults().size() : 0);
            statement.setInt(3, execution.getMongodbResults() != null ? execution.getMongodbResults().size() : 0);
            statement.setString(4, execution.getExecutionId());
            
            statement.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to update migration execution progress", e);
        }
    }
    
    /**
     * Complete execution record
     */
    public void completeExecution(MigrationExecution execution) {
        if (!migrationProperties.getControl().isEnabled()) {
            return;
        }
        
        String updateSql = """
            UPDATE %s SET 
                end_time = ?, 
                status = ?, 
                error_message = ?,
                postgres_migrations_count = ?, 
                mongodb_migrations_count = ?,
                report_path = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE execution_id = ?
            """.formatted(migrationProperties.getControl().getTableName());
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(updateSql)) {
            
            statement.setTimestamp(1, execution.getEndTime() != null ? 
                                   Timestamp.valueOf(execution.getEndTime()) : 
                                   Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(2, execution.getStatus().name());
            statement.setString(3, execution.getErrorMessage());
            statement.setInt(4, execution.getPostgresResults() != null ? execution.getPostgresResults().size() : 0);
            statement.setInt(5, execution.getMongodbResults() != null ? execution.getMongodbResults().size() : 0);
            statement.setString(6, execution.getReportPath());
            statement.setString(7, execution.getExecutionId());
            
            statement.executeUpdate();
            logger.info("Completed migration execution: {} with status: {}", 
                       execution.getExecutionId(), execution.getStatus());
            
        } catch (SQLException e) {
            logger.error("Failed to complete migration execution record", e);
        }
    }
    
    /**
     * Get recent migration executions
     */
    public List<MigrationExecution> getRecentExecutions(int limit) {
        if (!migrationProperties.getControl().isEnabled()) {
            return new ArrayList<>();
        }
        
        List<MigrationExecution> executions = new ArrayList<>();
        String selectSql = """
            SELECT execution_id, command, environment, start_time, end_time, status, 
                   error_message, postgres_migrations_count, mongodb_migrations_count, 
                   executed_by, report_path
            FROM %s 
            ORDER BY start_time DESC 
            LIMIT ?
            """.formatted(migrationProperties.getControl().getTableName());
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {
            
            statement.setInt(1, limit);
            ResultSet resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                MigrationExecution execution = new MigrationExecution();
                execution.setExecutionId(resultSet.getString("execution_id"));
                execution.setCommand(resultSet.getString("command"));
                execution.setEnvironment(resultSet.getString("environment"));
                execution.setStartTime(resultSet.getTimestamp("start_time").toLocalDateTime());
                
                Timestamp endTime = resultSet.getTimestamp("end_time");
                if (endTime != null) {
                    execution.setEndTime(endTime.toLocalDateTime());
                }
                
                execution.setStatus(MigrationExecution.ExecutionStatus.valueOf(resultSet.getString("status")));
                execution.setErrorMessage(resultSet.getString("error_message"));
                execution.setExecutedBy(resultSet.getString("executed_by"));
                execution.setReportPath(resultSet.getString("report_path"));
                
                executions.add(execution);
            }
            
        } catch (SQLException e) {
            logger.error("Failed to retrieve recent migration executions", e);
        }
        
        return executions;
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            migrationProperties.getPostgres().getUrl(),
            migrationProperties.getPostgres().getUsername(),
            migrationProperties.getPostgres().getPassword()
        );
    }
}