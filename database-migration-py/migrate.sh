#!/bin/bash
# ================================================================
# Financer Database Migration Tool - Linux/macOS Script  
# Enhanced Python serverless migration system
# ================================================================

COMMAND=$1
ENVIRONMENT=${2:-local}

if [ -z "$COMMAND" ]; then
    echo "Usage: ./migrate.sh [migrate|info|validate|history|clean] [local|docker|prod]"
    echo ""
    echo "Commands:"
    echo "  migrate   - Execute all pending migrations"
    echo "  info      - Show migration status"
    echo "  validate  - Validate migration checksums" 
    echo "  history   - Show execution history"
    echo "  clean     - Clean database (DANGER!)"
    echo ""
    echo "Environments:"
    echo "  local     - Connect to localhost (default)"
    echo "  docker    - Connect to Docker containers"
    echo "  prod      - Connect to production (use with caution)"
    exit 1
fi

echo "🚀 Financer Database Migration Tool v2.0.0 (Python)"
echo "===================================================="
echo "Command: $COMMAND"
echo "Environment: $ENVIRONMENT"
echo ""

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed or not in PATH"
    echo "Please install Python 3.8+ and try again"
    exit 1
fi

# Check if virtual environment exists, create if not
if [ ! -d "venv" ]; then
    echo "📦 Creating Python virtual environment..."
    python3 -m venv venv
    if [ $? -ne 0 ]; then
        echo "❌ Failed to create virtual environment"
        exit 1
    fi
fi

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Install/upgrade dependencies
echo "📦 Installing dependencies..."
pip install -r requirements.txt --quiet --disable-pip-version-check

if [ $? -ne 0 ]; then
    echo "❌ Failed to install dependencies"
    exit 1
fi

# Create logs directories if they don't exist
mkdir -p logs/database-migration
mkdir -p logs/migration-reports

# Execute migration
echo "🔧 Executing migration command: $COMMAND"
echo "📝 Logs will be written to: logs/database-migration/"
echo "📊 Reports will be saved to: logs/migration-reports/"
echo ""

python migrate.py "$COMMAND" --environment "$ENVIRONMENT" --verbose

MIGRATION_EXIT_CODE=$?

# Deactivate virtual environment
deactivate

if [ $MIGRATION_EXIT_CODE -ne 0 ]; then
    echo ""
    echo "❌ Migration command failed"
    echo "📋 Check logs in logs/database-migration/ for details"
    exit $MIGRATION_EXIT_CODE
fi

echo ""
echo "✅ Migration completed successfully!"
echo "📋 Detailed logs available in: logs/database-migration/"
echo "📊 Execution report saved in: logs/migration-reports/"