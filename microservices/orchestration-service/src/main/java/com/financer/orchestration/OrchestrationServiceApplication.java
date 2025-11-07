package com.financer.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Orchestration Service Application
 * 
 * Workflow orchestration service using CAMUNDA with Saga pattern 
 * for distributed transactions and complex business processes.
 * 
 * Features:
 * - CAMUNDA BPM workflow engine
 * - Saga pattern implementation
 * - Distributed transaction coordination
 * - Compensation logic handling
 * - Business process automation
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrchestrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestrationServiceApplication.class, args);
    }
}