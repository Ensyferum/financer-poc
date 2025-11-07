package com.financer.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Account Service Application
 * 
 * Microservice responsible for account management operations including:
 * - Account creation and management
 * - Account balance operations
 * - Account status and verification
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.financer.account", "com.financer.common"})
@EnableDiscoveryClient
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}