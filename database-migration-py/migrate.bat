@echo off
REM ================================================================
REM Financer Database Migration Tool - Windows Script
REM Enhanced Python serverless migration system
REM ================================================================

set COMMAND=%1
set ENVIRONMENT=%2

if "%COMMAND%"=="" (
    echo Usage: migrate.bat [migrate^|info^|validate^|history^|clean] [local^|docker^|prod]
    echo.
    echo Commands:
    echo   migrate   - Execute all pending migrations
    echo   info      - Show migration status
    echo   validate  - Validate migration checksums
    echo   history   - Show execution history
    echo   clean     - Clean database ^(DANGER!^)
    echo.
    echo Environments:
    echo   local     - Connect to localhost ^(default^)
    echo   docker    - Connect to Docker containers
    echo   prod      - Connect to production ^(use with caution^)
    exit /b 1
)

if "%ENVIRONMENT%"=="" set ENVIRONMENT=local

echo 🚀 Financer Database Migration Tool v2.0.0 (Python)
echo ====================================================
echo Command: %COMMAND%
echo Environment: %ENVIRONMENT%
echo.

REM Check if Python is installed
python --version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Python is not installed or not in PATH
    echo Please install Python 3.8+ and try again
    exit /b 1
)

REM Check if virtual environment exists, create if not
if not exist "venv" (
    echo 📦 Creating Python virtual environment...
    python -m venv venv
    if %ERRORLEVEL% neq 0 (
        echo ❌ Failed to create virtual environment
        exit /b 1
    )
)

REM Activate virtual environment
echo 🔧 Activating virtual environment...
call venv\Scripts\activate.bat

REM Install/upgrade dependencies
echo 📦 Installing dependencies...
pip install -r requirements.txt --quiet --disable-pip-version-check

if %ERRORLEVEL% neq 0 (
    echo ❌ Failed to install dependencies
    exit /b 1
)

REM Create logs directory if it doesn't exist
if not exist "logs\database-migration" mkdir "logs\database-migration"
if not exist "logs\migration-reports" mkdir "logs\migration-reports"

REM Execute migration
echo 🔧 Executing migration command: %COMMAND%
echo 📝 Logs will be written to: logs/database-migration/
echo 📊 Reports will be saved to: logs/migration-reports/
echo.

python migrate.py %COMMAND% --environment %ENVIRONMENT% --verbose

set MIGRATION_EXIT_CODE=%ERRORLEVEL%

REM Deactivate virtual environment
deactivate

if %MIGRATION_EXIT_CODE% neq 0 (
    echo.
    echo ❌ Migration command failed
    echo 📋 Check logs in logs/database-migration/ for details
    exit /b %MIGRATION_EXIT_CODE%
)

echo.
echo ✅ Migration completed successfully!
echo 📋 Detailed logs available in: logs/database-migration/
echo 📊 Execution report saved in: logs/migration-reports/