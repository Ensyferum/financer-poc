@echo off
setlocal enabledelayedexpansion

echo ==============================================
echo 📊 Financer System - Status Check
echo ==============================================

echo 🐳 Docker Containers Status:
echo ================================================
docker ps --filter "name=financer" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo 🌐 Networks Status:
echo ================================================
docker network ls --filter "name=financer" --format "table {{.Name}}\t{{.Driver}}\t{{.Scope}}"

echo.
echo 💾 Volumes Status:
echo ================================================
docker volume ls --filter "name=financer" --format "table {{.Name}}\t{{.Driver}}"

echo.
echo 🔍 Health Checks:
echo ================================================

REM Verificar cada serviço
set services=config-server eureka-server api-gateway account-service transaction-service orchestration-service postgres mongodb

for %%s in (%services%) do (
    docker inspect financer-%%s >nul 2>&1
    if !errorlevel! equ 0 (
        for /f "tokens=*" %%h in ('docker inspect --format="{{.State.Health.Status}}" financer-%%s 2^>nul') do (
            set health=%%h
        )
        if "!health!"=="" set health=no health check
        
        for /f "tokens=*" %%st in ('docker inspect --format="{{.State.Status}}" financer-%%s 2^>nul') do (
            set status=%%st
        )
        
        if "!status!"=="running" (
            if "!health!"=="healthy" (
                echo ✅ financer-%%s: !status! ^(!health!^)
            ) else if "!health!"=="no health check" (
                echo 🔶 financer-%%s: !status! ^(no health check^)
            ) else (
                echo ⚠️  financer-%%s: !status! ^(!health!^)
            )
        ) else (
            echo ❌ financer-%%s: !status!
        )
    ) else (
        echo ❌ financer-%%s: not found
    )
)

echo.
echo 🌐 Service URLs (if running):
echo ================================================
echo • Config Server:         http://localhost:8888
echo • Eureka Server:         http://localhost:8761  
echo • API Gateway:           http://localhost:8090
echo • Account Service:       http://localhost:8081
echo • Transaction Service:   http://localhost:8082
echo • Orchestration Service: http://localhost:8085
echo • PostgreSQL:            localhost:5432
echo • MongoDB:               localhost:27017
echo • Kafka UI:              http://localhost:8080
echo • Prometheus:            http://localhost:9090
echo • Grafana:               http://localhost:3000

echo.
echo 🔧 Quick Commands:
echo ================================================
echo • Start system:    start-system.bat
echo • Stop system:     stop-system.bat
echo • Run API tests:   start-api-tests.bat
echo • View logs:       docker-compose logs -f [service-name]
echo • Restart service: docker-compose restart [service-name]
echo.

REM Verificar se curl está disponível para testes
where curl >nul 2>&1
if !errorlevel! equ 0 (
    echo 🧪 Quick Health Test:
    echo ================================================
    
    echo Testing Config Server...
    curl -s -o nul -w "HTTP %%{http_code}\n" http://localhost:8888/actuator/health 2>nul || echo Not accessible
    
    echo Testing Eureka Server...
    curl -s -o nul -w "HTTP %%{http_code}\n" http://localhost:8761/actuator/health 2>nul || echo Not accessible
    
    echo Testing API Gateway...
    curl -s -o nul -w "HTTP %%{http_code}\n" http://localhost:8090/actuator/health 2>nul || echo Not accessible
    
    echo.
) else (
    echo ℹ️  Install curl for quick health tests
    echo.
)

pause