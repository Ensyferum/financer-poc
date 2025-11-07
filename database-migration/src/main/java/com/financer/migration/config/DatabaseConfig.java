package com.financer.migration.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Database configuration based on Migration Properties
 */
@Configuration
@EnableConfigurationProperties(MigrationProperties.class)
public class DatabaseConfig {

    @Autowired
    private MigrationProperties migrationProperties;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        MigrationProperties.PostgresConfig postgres = migrationProperties.getPostgres();
        
        config.setJdbcUrl(postgres.getUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        
        // Connection pool settings
        config.setConnectionTimeout(20000);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }
}