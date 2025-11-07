# ================================================================
# Lightweight Database Migration Container
# This creates a minimal container for running database migrations
# ================================================================

FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks (optional)
RUN apk add --no-cache curl

# Create app directory
WORKDIR /app

# Copy the migration JAR (will be built by Maven)
COPY target/database-migration-*-shaded.jar migration.jar

# Create non-root user for security
RUN addgroup -g 1000 migration && \
    adduser -D -s /bin/sh -u 1000 -G migration migration

# Change ownership
RUN chown -R migration:migration /app

# Switch to non-root user
USER migration

# Set default environment variables
ENV POSTGRES_URL="jdbc:postgresql://financer-postgres:5432/financer_accounts"
ENV POSTGRES_USER="financer_user"
ENV POSTGRES_PASSWORD="financer123"
ENV MONGO_URL="mongodb://financer-mongodb:27017"
ENV MONGO_DATABASE="financer"

# Default command (can be overridden)
ENTRYPOINT ["java", "-jar", "migration.jar"]
CMD ["migrate"]

# Add labels for better container management
LABEL name="financer-database-migration" \
      description="Lightweight database migration tool for Financer" \
      version="1.0.0" \
      maintainer="Financer Development Team"