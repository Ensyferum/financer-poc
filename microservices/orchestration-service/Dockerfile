# Orchestration Service Dockerfile
# Multi-stage build for Java 21 with CAMUNDA BPM

FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy parent pom and this service's pom
COPY pom.xml ./
COPY microservices/orchestration-service/pom.xml ./microservices/orchestration-service/

# Copy shared libraries first
COPY shared/ ./shared/

# Build shared libraries first
RUN mvn clean install -pl shared/common-lib,shared/eureka-integration -DskipTests

# Copy source code
COPY microservices/orchestration-service/src/ ./microservices/orchestration-service/src/

# Build the application
RUN cd microservices/orchestration-service && \
    mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create app user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the jar file from builder stage
COPY --from=builder /app/microservices/orchestration-service/target/*.jar app.jar

# Change ownership to app user
RUN chown -R appuser:appgroup /app

# Switch to app user
USER appuser

# Expose port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8085/orchestration-service/actuator/health || exit 1

# JVM options optimized for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -XX:+UnlockExperimentalVMOptions \
               -XX:+EnableJVMCI \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=docker"

# Application arguments
ENV APP_ARGS="--server.port=8085 \
              --management.endpoints.web.exposure.include=health,info,prometheus"

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $APP_ARGS"]

# Labels for metadata
LABEL maintainer="financer-team@company.com"
LABEL version="1.0.0"
LABEL description="Orchestration Service with CAMUNDA BPM - Java 21"
LABEL java.version="21"
LABEL camunda.version="7.22.0"