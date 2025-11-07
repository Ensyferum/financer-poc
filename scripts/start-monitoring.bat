@echo off
setlocal enabledelayedexpansion

echo ===================================
echo     FINANCER MONITORING STARTUP
echo ===================================

:: Verificar se o Docker está rodando
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker não está rodando. Inicie o Docker Desktop primeiro.
    pause
    exit /b 1
)

:: Verificar se a rede existe
docker network ls | findstr "financer_financer-network" >nul
if %errorlevel% neq 0 (
    echo [INFO] Criando rede financer_financer-network...
    docker network create financer_financer-network
)

echo [INFO] Iniciando stack de monitoramento...

:: Subir serviços de monitoramento
docker-compose -f docker-compose.monitoring.yml up -d

if %errorlevel% neq 0 (
    echo [ERROR] Falha ao iniciar stack de monitoramento
    pause
    exit /b 1
)

echo.
echo [SUCCESS] Stack de monitoramento iniciada com sucesso!
echo.
echo Serviços disponíveis:
echo   - Grafana:       http://localhost:3000 (admin/admin123)
echo   - Prometheus:    http://localhost:9090
echo   - Alertmanager:  http://localhost:9093
echo   - Jaeger:        http://localhost:16686
echo   - Loki:          http://localhost:3100
echo   - Tempo:         http://localhost:3200
echo   - Node Exporter: http://localhost:9100
echo   - cAdvisor:      http://localhost:8080
echo.

:: Aguardar serviços estarem prontos
echo [INFO] Aguardando serviços ficarem prontos...
timeout /t 30 /nobreak >nul

:: Verificar health dos serviços
echo [INFO] Verificando status dos serviços...

set services=prometheus grafana alertmanager jaeger loki tempo node-exporter cadvisor
for %%s in (%services%) do (
    docker ps --filter "name=financer-%%s" --filter "status=running" | findstr "financer-%%s" >nul
    if !errorlevel! equ 0 (
        echo   ✓ %%s - Running
    ) else (
        echo   ✗ %%s - Not running
    )
)

echo.
echo [INFO] Para verificar logs: docker-compose -f docker-compose.monitoring.yml logs -f [service_name]
echo [INFO] Para parar: scripts\stop-monitoring.bat
echo.
pause