@echo off
setlocal enabledelayedexpansion

echo ==============================================
echo 🛑 Stopping Financer System - Complete Shutdown
echo ==============================================

echo [INFO] Stopping all services...

REM Parar serviços em ordem reversa
echo [INFO] Step 1/3: Stopping Monitoring Stack...
docker-compose -f docker-compose.monitoring.yml down 2>nul

echo [INFO] Step 2/3: Stopping Application Services...
docker-compose -f docker-compose.services.yml down 2>nul

echo [INFO] Step 3/3: Stopping Infrastructure...
docker-compose -f docker-compose.infrastructure.yml down 2>nul

REM Parar todos os containers relacionados ao financer (fallback)
echo [INFO] Stopping any remaining Financer containers...
for /f "tokens=1" %%i in ('docker ps -q --filter "name=financer"') do (
    docker stop %%i >nul 2>&1
    docker rm %%i >nul 2>&1
)

echo.
echo 🧹 Cleanup Options:
echo ===================
echo [1] Keep data (recommended for development)
echo [2] Remove all data (clean slate)
echo [3] Remove everything including images
echo.

set /p choice="Choose option (1-3): "

if "%choice%"=="2" (
    echo [INFO] Removing volumes...
    docker volume rm financer_postgres_data 2>nul
    docker volume rm financer_mongodb_data 2>nul
    docker volume rm financer_kafka_data 2>nul
    docker volume rm financer_zookeeper_data 2>nul
    docker volume rm financer_zookeeper_logs 2>nul
    docker volume rm financer_prometheus_data 2>nul
    docker volume rm financer_grafana_data 2>nul
    docker volume rm financer_alertmanager_data 2>nul
    docker volume rm financer_loki_data 2>nul
    docker volume rm financer_tempo_data 2>nul
    echo [OK] Volumes removed!
)

if "%choice%"=="3" (
    echo [INFO] Removing volumes...
    docker volume rm financer_postgres_data 2>nul
    docker volume rm financer_mongodb_data 2>nul
    docker volume rm financer_kafka_data 2>nul
    docker volume rm financer_zookeeper_data 2>nul
    docker volume rm financer_zookeeper_logs 2>nul
    docker volume rm financer_prometheus_data 2>nul
    docker volume rm financer_grafana_data 2>nul
    docker volume rm financer_alertmanager_data 2>nul
    docker volume rm financer_loki_data 2>nul
    docker volume rm financer_tempo_data 2>nul
    
    echo [INFO] Removing Financer images...
    for /f "tokens=1" %%i in ('docker images --filter "reference=financer/*" -q') do (
        docker rmi %%i 2>nul
    )
    echo [OK] Images removed!
)

REM Limpar redes órfãs
echo [INFO] Cleaning up networks...
docker network rm financer-network 2>nul
docker network rm financer_financer-network 2>nul

echo.
echo ✅ System shutdown completed!
echo.

REM Verificar se ainda há containers rodando
set "remaining_containers="
for /f "tokens=1" %%i in ('docker ps -q --filter "name=financer" 2^>nul') do (
    set "remaining_containers=true"
)

if defined remaining_containers (
    echo ⚠️  Some containers are still running:
    docker ps --filter "name=financer"
    echo.
    echo Run: docker stop $(docker ps -q --filter "name=financer")
    echo.
) else (
    echo 🎯 All Financer containers stopped successfully!
)

echo 📊 System Status:
echo ==================
echo Containers: 
docker ps --filter "name=financer" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo Volumes:
docker volume ls --filter "name=financer" --format "table {{.Name}}\t{{.Driver}}"

echo.
echo Networks:
docker network ls --filter "name=financer" --format "table {{.Name}}\t{{.Driver}}"

echo.
pause