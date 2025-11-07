@echo off
setlocal enabledelayedexpansion

echo ===================================
echo   FINANCER MONITORING STATUS CHECK
echo ===================================

:: Verificar se o Docker está rodando
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker não está rodando
    pause
    exit /b 1
)

echo [INFO] Status dos serviços de monitoramento:
echo.

:: Lista de serviços para verificar
set services=financer-prometheus financer-grafana financer-alertmanager financer-jaeger financer-loki financer-tempo financer-node-exporter financer-cadvisor financer-promtail

for %%s in (%services%) do (
    docker ps --filter "name=%%s" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | findstr "%%s" >nul
    if !errorlevel! equ 0 (
        docker ps --filter "name=%%s" --format "%%s - {{.Status}}"
    ) else (
        echo %%s - NOT RUNNING
    )
)

echo.
echo [INFO] URLs dos serviços:
echo   - Grafana:       http://localhost:3000
echo   - Prometheus:    http://localhost:9090
echo   - Alertmanager:  http://localhost:9093
echo   - Jaeger:        http://localhost:16686
echo   - Loki:          http://localhost:3100
echo   - Tempo:         http://localhost:3200
echo   - Node Exporter: http://localhost:9100
echo   - cAdvisor:      http://localhost:8080

echo.
echo [INFO] Verificando conectividade...

:: Testar endpoints principais
set endpoints=localhost:3000 localhost:9090 localhost:9093 localhost:16686

for %%e in (%endpoints%) do (
    curl -s --connect-timeout 5 http://%%e >nul 2>&1
    if !errorlevel! equ 0 (
        echo   ✓ %%e - Acessível
    ) else (
        echo   ✗ %%e - Não acessível
    )
)

echo.
echo [INFO] Para ver logs detalhados: docker-compose -f docker-compose.monitoring.yml logs [service_name]
echo.
pause