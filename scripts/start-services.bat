@echo off
echo Starting Financer Microservices...
echo.

REM Change to project root directory (parent of scripts)
cd /d "%~dp0\.."

echo Checking if infrastructure is running...
docker-compose ps postgres mongodb kafka | findstr "Up" >nul
if %errorlevel% neq 0 (
    echo ❌ Infrastructure services are not running!
    echo Please start them first with: scripts\start-infrastructure.bat
    pause
    exit /b 1
)

echo.
echo Infrastructure is running. Starting microservices...
docker-compose -f docker-compose.services.yml up --build -d

echo.
echo ✅ Microservices started!
echo.
echo Services available:
echo - Config Server: http://localhost:8888
echo - Eureka Server: http://localhost:8761  
echo - API Gateway: http://localhost:8080
echo.
echo To check status: docker-compose -f docker-compose.services.yml ps
echo To view logs: docker-compose -f docker-compose.services.yml logs -f [service-name]