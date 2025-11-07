@echo off
echo Building Financer Microservices...

REM Change to project root directory (parent of scripts)
cd /d "%~dp0\.."

echo.
echo [0/5] Installing Parent POM...
call mvn clean install -N
if %errorlevel% neq 0 (
    echo Failed to install parent POM
    exit /b 1
)

echo.
echo [1/5] Building and Installing Common Library...
cd shared\common-lib
call mvn clean install
if %errorlevel% neq 0 (
    echo Failed to build common-lib
    exit /b 1
)
cd ..\..

echo.
echo [2/5] Building Config Server...
cd microservices\config-server
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Failed to build config-server
    exit /b 1
)
cd ..\..

echo.
echo [3/5] Building Eureka Server...
cd microservices\eureka-server
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Failed to build eureka-server
    exit /b 1
)
cd ..\..

echo.
echo [4/6] Building API Gateway...
cd microservices\api-gateway
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Failed to build api-gateway
    exit /b 1
)
cd ..\..

echo.
echo [5/6] Building Account Service...
cd microservices\account-service
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Failed to build account-service
    exit /b 1
)
cd ..\..

echo.
echo ✅ All services built successfully!
echo.
echo JAR files created:
echo - Config Server: microservices\config-server\target\config-server-1.0.0-SNAPSHOT.jar
echo - Eureka Server: microservices\eureka-server\target\eureka-server-1.0.0-SNAPSHOT.jar  
echo - API Gateway: microservices\api-gateway\target\api-gateway-1.0.0-SNAPSHOT.jar
echo - Account Service: microservices\account-service\target\account-service-1.0.0-SNAPSHOT.jar
echo.
echo To start the services, run:
echo docker-compose -f docker-compose.services.yml up --build