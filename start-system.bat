@echo off
setlocal enabledelayedexpansion

echo ==============================================
echo 🚀 Starting Financer System - Complete Setup
echo ==============================================

REM Parar todos os containers existentes
echo [INFO] Stopping existing containers...
docker-compose down --remove-orphans 2>nul

REM Remover rede existente se houver
echo [INFO] Cleaning up existing networks...
docker network rm financer-network 2>nul
docker network rm financer_financer-network 2>nul

REM Limpar volumes órfãos (opcional)
echo [INFO] Cleaning up unused volumes...
docker volume prune -f

echo.
echo 📋 Starting services in order...
echo.

REM 1. Iniciar infraestrutura primeiro
echo [INFO] Step 1/3: Starting Infrastructure (Database, Messaging)...
docker-compose -f docker-compose.infrastructure.yml up -d
if !errorlevel! neq 0 (
    echo [ERROR] Failed to start infrastructure
    exit /b 1
)

echo [INFO] Waiting for infrastructure to be ready...
timeout /t 30 /nobreak >nul

REM Verificar se PostgreSQL está pronto
echo [INFO] Checking PostgreSQL health...
:check_postgres
docker exec financer-postgres pg_isready -U financer -d financer >nul 2>&1
if !errorlevel! neq 0 (
    echo [WAIT] PostgreSQL not ready yet, waiting...
    timeout /t 5 /nobreak >nul
    goto check_postgres
)
echo [OK] PostgreSQL is ready!

REM Verificar se MongoDB está pronto
echo [INFO] Checking MongoDB health...
:check_mongodb
docker exec financer-mongodb mongosh --eval "db.adminCommand('ping')" >nul 2>&1
if !errorlevel! neq 0 (
    echo [WAIT] MongoDB not ready yet, waiting...
    timeout /t 5 /nobreak >nul
    goto check_mongodb
)
echo [OK] MongoDB is ready!

REM 2. Iniciar serviços de aplicação
echo [INFO] Step 2/3: Starting Application Services...
docker-compose -f docker-compose.services.yml up -d
if !errorlevel! neq 0 (
    echo [ERROR] Failed to start application services
    exit /b 1
)

echo [INFO] Waiting for services to initialize...
timeout /t 45 /nobreak >nul

REM 3. Iniciar monitoramento (opcional)
echo [INFO] Step 3/3: Starting Monitoring Stack (Optional)...
docker-compose -f docker-compose.monitoring.yml up -d
if !errorlevel! neq 0 (
    echo [WARN] Monitoring stack failed to start, continuing without monitoring...
)

echo.
echo ✅ System startup completed!
echo.

REM Verificar status dos serviços
echo 📊 Service Status:
echo ==================
docker-compose -f docker-compose.infrastructure.yml -f docker-compose.services.yml ps

echo.
echo 🌐 Service URLs:
echo ================
echo • Config Server:      http://localhost:8888
echo • Eureka Server:      http://localhost:8761
echo • API Gateway:        http://localhost:8090
echo • Account Service:    http://localhost:8081
echo • Transaction Service: http://localhost:8082
echo • Orchestration Service: http://localhost:8085
echo • PostgreSQL:         localhost:5432 (user: financer/financer123)
echo • MongoDB:            localhost:27017 (user: financer/financer123)
echo • Kafka UI:           http://localhost:8080
echo • Prometheus:         http://localhost:9090
echo • Grafana:            http://localhost:3000 (admin/admin123)
echo.

echo 🔧 Health Check Commands:
echo ==========================
echo docker exec financer-postgres pg_isready -U financer
echo docker exec financer-mongodb mongosh --eval "db.adminCommand('ping')"
echo curl http://localhost:8888/actuator/health
echo curl http://localhost:8761/actuator/health
echo curl http://localhost:8090/actuator/health
echo.

echo 📋 Next Steps:
echo ===============
echo 1. Wait 2-3 minutes for all services to fully initialize
echo 2. Check service health: docker-compose logs [service-name]
echo 3. Run API tests: start-api-tests.bat
echo 4. Stop system: docker-compose down
echo.

pause