# Changelog

All notable changes to the Financer project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-06

### Added - Initial Release

#### Infrastructure Services
- **Config Server v1.0.0**: Spring Cloud Config Server with native file repository
  - Native configuration storage with file-based repository
  - Docker containerization with health checks
  - Centralized configuration management for all microservices
  
- **Eureka Server v1.0.0**: Service Discovery and Registration
  - Netflix Eureka service discovery implementation
  - Docker networking and service registration
  - Health checks and monitoring endpoints
  
- **API Gateway v1.0.0**: Reactive Gateway with Load Balancing
  - Spring Cloud Gateway with reactive stack
  - Route configuration and load balancing
  - Circuit breaker patterns and resilience
  - Fixed Spring MVC vs WebFlux dependency conflicts

#### Business Services
- **Account Service v1.0.0**: Complete Account Management
  - PostgreSQL database integration with UUID primary keys
  - JPA entities with validation and business logic
  - Complete CRUD operations via REST API
  - Custom repository queries and pagination
  - Service layer with balance management
  - DTO pattern for request/response handling
  - Docker containerization with health checks

#### Shared Components
- **Common Library v1.0.0**: Shared Utilities and Configuration
  - Base entity classes with audit fields
  - Common API response patterns
  - Centralized logging configuration with Logback
  - Shared validation and utility classes

#### Development Infrastructure
- **Centralized Logging System**: Dynamic log management
  - Logback configuration with 5MB file rotation
  - Date-time based log file naming
  - Error and normal log separation
  - Volume mapping to local logs directory
  - Configurable log levels per environment

- **Semantic Versioning**: Automated version management
  - MAJOR.MINOR.PATCH versioning for all components
  - Automated version update scripts (Windows & Linux)
  - Centralized version configuration
  - Changelog automation

#### Infrastructure Setup
- Docker Compose configuration for:
  - PostgreSQL 16 with dedicated databases
  - MongoDB 7 for document storage
  - Apache Kafka with Zookeeper for event streaming
  - Schema Registry for Kafka message schemas
  - Kafka UI for monitoring and management

### Technical Details
- **Java 21**: Latest LTS version with modern features
- **Spring Boot 3.2.0**: Latest stable release
- **Spring Cloud 2023.0.0**: Cloud-native microservices
- **Maven Multi-module**: Organized project structure
- **Docker**: Full containerization with health checks
- **PostgreSQL 16**: Primary relational database
- **Lombok**: Reduced boilerplate code

### Testing & Quality
- Health check endpoints for all services
- Service registration and discovery verification
- Database connectivity and migration testing
- API endpoint functionality validation

## [Unreleased]

### Planned Features
- **Transaction Service v0.1.0**: MongoDB-based transaction processing
- **Orchestration Service v0.1.0**: Kafka-based event orchestration
- **Angular Frontend**: Responsive web application
- Authentication and authorization service
- API rate limiting and throttling
- Distributed tracing with Zipkin
- Metrics collection with Prometheus

---

## Version Format

This project uses [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking changes that require manual intervention
- **MINOR**: New features that are backward compatible
- **PATCH**: Bug fixes and minor improvements

## Component Versioning

Each microservice maintains its own version while following the project's semantic versioning standards:

- `config-server`: Infrastructure service versions
- `eureka-server`: Service discovery versions  
- `api-gateway`: Gateway and routing versions
- `account-service`: Business service versions
- `transaction-service`: Transaction processing versions
- `orchestration-service`: Event orchestration versions
- `common-lib`: Shared library versions# 1.0.1 - 07/11/2025 - patch update 
#   - eureka-server: Updated to 1.0.1 
 
