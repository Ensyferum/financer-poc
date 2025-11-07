FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="financer-team"
LABEL description="Transaction Service for Financer microservices"

# Install wget for health checks
RUN apk add --no-cache wget

WORKDIR /app

# Copy the JAR file
COPY target/transaction-service-*.jar app.jar

# Expose the port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dspring.profiles.active=docker"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]