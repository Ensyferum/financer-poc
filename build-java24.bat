@echo off
setlocal enabledelayedexpansion

REM Build script para o projeto Financer com Java 24
REM Atualizado para incluir o Orchestration Service com CAMUNDA BPM

echo ==============================================
echo 🚀 Building Financer Project with Java 24...
echo ==============================================

REM Verificar se Java 24 está disponível
echo [INFO] Checking Java version...
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STR=%%g
)
set JAVA_VERSION_STR=!JAVA_VERSION_STR:"=!
for /f "delims=." %%a in ("!JAVA_VERSION_STR!") do set JAVA_VERSION=%%a

if not "!JAVA_VERSION!"=="24" (
    echo [WARN] Java 24 not detected. Current version: !JAVA_VERSION!
    echo [WARN] The build will continue but may not use all Java 24 features
)

REM Build das bibliotecas compartilhadas primeiro
echo [INFO] Building shared libraries...
call mvnw clean install -pl shared/common-lib -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Failed to build common-lib
    exit /b 1
)

echo [INFO] Building Eureka integration library...
call mvnw clean install -pl shared/eureka-integration -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Failed to build eureka-integration
    exit /b 1
)

REM Build dos serviços de infraestrutura
echo [INFO] Building infrastructure services...
call mvnw clean install -pl microservices/config-server -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Failed to build config-server
    exit /b 1
)

call mvnw clean install -pl microservices/eureka-server -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Failed to build eureka-server
    exit /b 1
)

call mvnw clean install -pl microservices/api-gateway -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Failed to build api-gateway
    exit /b 1
)

REM Build dos serviços de negócio
echo [INFO] Building business services...
call mvnw clean install -pl microservices/account-service -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Failed to build account-service
    exit /b 1
)

call mvnw clean install -pl microservices/transaction-service -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Failed to build transaction-service
    exit /b 1
)

echo [INFO] Building Orchestration Service with CAMUNDA BPM (Java 24)...
call mvnw clean install -pl microservices/orchestration-service -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Failed to build orchestration-service
    exit /b 1
)

REM Build das imagens Docker
echo [INFO] Building Docker images...

echo [INFO] Building infrastructure service images...
docker build -t financer/config-server:latest microservices/config-server/
docker build -t financer/eureka-server:latest microservices/eureka-server/
docker build -t financer/api-gateway:latest microservices/api-gateway/

echo [INFO] Building business service images...
docker build -t financer/account-service:latest microservices/account-service/
docker build -f microservices/transaction-service/Dockerfile -t financer/transaction-service:latest .
docker build -f microservices/orchestration-service/Dockerfile -t financer/orchestration-service:latest .

REM Verificar se as imagens foram criadas
echo [INFO] Verifying Docker images...
docker images | findstr financer

REM Criar rede Docker se não existir
echo [INFO] Creating Docker network...
docker network create financer-network 2>nul || echo Network already exists

echo.
echo ✅ Build completed successfully!
echo.
echo 🔧 Services built with Java 24 support:
echo    • Config Server
echo    • Eureka Server
echo    • API Gateway
echo    • Account Service
echo    • Transaction Service (Java 24)
echo    • Orchestration Service (Java 24 + CAMUNDA BPM 7.22.0)
echo.
echo 📋 Next steps:
echo    1. Start infrastructure: docker-compose up -d
echo    2. Check services: docker-compose ps
echo    3. View logs: docker-compose logs -f [service-name]
echo    4. CAMUNDA Cockpit: http://localhost:8085/orchestration-service
echo.
echo 🎯 Java 24 Features enabled:
echo    • Preview features: --enable-preview
echo    • Container support: -XX:+UseContainerSupport
echo    • G1GC with optimizations
echo    • String deduplication
echo.

pause