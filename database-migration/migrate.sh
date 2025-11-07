#!/bin/bash
# ================================================================
# Enhanced Database Migration Scripts for Linux/Mac
# Usage: ./migrate.sh [command] [environment]
# Commands: migrate, info, validate, clean, history
# Environments: local, docker
# ================================================================

COMMAND=$1
ENVIRONMENT=${2:-local}

if [ -z "$COMMAND" ]; then
    echo "Usage: ./migrate.sh [migrate|info|validate|clean|history] [local|docker]"
    echo ""
    echo "Commands:"
    echo "  migrate   - Execute all pending migrations"
    echo "  info      - Show migration status"
    echo "  validate  - Validate migration checksums"
    echo "  clean     - Clean database (DANGER!)"
    echo "  history   - Show execution history"
    echo ""
    echo "Environments:"
    echo "  local     - Connect to localhost (default)"
    echo "  docker    - Connect to Docker containers"
    exit 1
fi

echo "🚀 Enhanced Financer Database Migration Tool v2.0.0"
echo "===================================================="
echo "Command: $COMMAND"
echo "Environment: $ENVIRONMENT"
echo ""

# Set Spring profile based on environment
if [ "$ENVIRONMENT" = "docker" ]; then
    export SPRING_PROFILES_ACTIVE="docker"
else
    export SPRING_PROFILES_ACTIVE="local"
fi

# Create logs directory if it doesn't exist
mkdir -p logs/database-migration
mkdir -p logs/migration-reports

# Build the application
echo "📦 Building migration tool..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

# Run the migration with Spring Boot
echo "🔧 Executing migration command: $COMMAND"
echo "📝 Logs will be written to: logs/database-migration/"
echo "📊 Reports will be saved to: logs/migration-reports/"
echo ""

mvn spring-boot:run -Dspring-boot.run.arguments="$COMMAND" -Dspring-boot.run.profiles="$SPRING_PROFILES_ACTIVE" -q

if [ $? -ne 0 ]; then
    echo "❌ Migration command failed"
    echo "📋 Check logs in logs/database-migration/ for details"
    exit 1
fi

echo ""
echo "✅ Migration completed successfully!"
echo "📋 Detailed logs available in: logs/database-migration/"
echo "📊 Execution report saved in: logs/migration-reports/"