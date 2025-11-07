package com.financer.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Transaction Service Application
 * 
 * Microservice for transaction processing with CQRS and Event Sourcing patterns.
 * 
 * Features:
 * - Reactive programming with Spring WebFlux
 * - Event sourcing with MongoDB
 * - CQRS with PostgreSQL read models
 * - Kafka event publishing
 * - Functional programming patterns
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {
    "com.financer.transaction",
    "com.financer.common",
    "com.financer.eureka"
})
@EnableFeignClients
@EnableKafka
@EnableJpaRepositories(basePackages = "com.financer.transaction.infrastructure.persistence.jpa")
@EnableReactiveMongoRepositories(basePackages = "com.financer.transaction.infrastructure.persistence.mongo")
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}