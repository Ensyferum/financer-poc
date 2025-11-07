@echo off
echo Starting Complete Financer System...
echo.

REM Change to project root directory (parent of scripts)
cd /d "%~dp0\.."

echo [1/2] Starting Infrastructure...
call scripts\start-infrastructure.bat

echo.
echo [2/2] Building and Starting Services...
call scripts\build-services.bat

if %errorlevel% neq 0 (
    echo ❌ Failed to build services
    pause
    exit /b 1
)

call scripts\start-services.bat

echo.
echo 🚀 Complete Financer System Started!
echo.
echo Dashboard URLs:
echo - Eureka Dashboard: http://localhost:8761
echo - Kafka UI: http://localhost:8080
echo - API Gateway Health: http://localhost:8080/actuator/health
echo.
echo To stop everything: stop-all.bat