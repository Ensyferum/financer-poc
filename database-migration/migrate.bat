@echo off
REM ================================================================
REM Enhanced Database Migration Scripts for Windows
REM Usage: migrate.bat [command] [environment]
REM Commands: migrate, info, validate, clean, history
REM Environments: local, docker
REM ================================================================

set COMMAND=%1
set ENVIRONMENT=%2

if "%COMMAND%"=="" (
    echo Usage: migrate.bat [migrate^|info^|validate^|clean^|history] [local^|docker]
    echo.
    echo Commands:
    echo   migrate   - Execute all pending migrations
    echo   info      - Show migration status
    echo   validate  - Validate migration checksums
    echo   clean     - Clean database (DANGER!)
    echo   history   - Show execution history
    echo.
    echo Environments:
    echo   local     - Connect to localhost (default)
    echo   docker    - Connect to Docker containers
    exit /b 1
)

if "%ENVIRONMENT%"=="" set ENVIRONMENT=local

echo 🚀 Enhanced Financer Database Migration Tool v2.0.0
echo ====================================================
echo Command: %COMMAND%
echo Environment: %ENVIRONMENT%
echo.

REM Set Spring profile based on environment
if "%ENVIRONMENT%"=="docker" (
    set SPRING_PROFILES_ACTIVE=docker
) else (
    set SPRING_PROFILES_ACTIVE=local
)

REM Create logs directory if it doesn't exist
if not exist "logs\database-migration" mkdir "logs\database-migration"
if not exist "logs\migration-reports" mkdir "logs\migration-reports"

REM Build the application
echo 📦 Building migration tool...
call mvn clean compile -q

if %ERRORLEVEL% neq 0 (
    echo ❌ Build failed
    exit /b 1
)

REM Run the migration with Spring Boot
echo 🔧 Executing migration command: %COMMAND%
echo 📝 Logs will be written to: logs/database-migration/
echo 📊 Reports will be saved to: logs/migration-reports/
echo.

call mvn spring-boot:run -Dspring-boot.run.arguments="%COMMAND%" -Dspring-boot.run.profiles="%SPRING_PROFILES_ACTIVE%" -q

if %ERRORLEVEL% neq 0 (
    echo ❌ Migration command failed
    echo 📋 Check logs in logs/database-migration/ for details
    exit /b 1
)

echo.
echo ✅ Migration completed successfully!
echo 📋 Detailed logs available in: logs/database-migration/
echo 📊 Execution report saved in: logs/migration-reports/