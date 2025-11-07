@echo off
setlocal enabledelayedexpansion

echo ===================================
echo     FINANCER MONITORING SHUTDOWN
echo ===================================

echo [INFO] Parando stack de monitoramento...

:: Parar e remover containers
docker-compose -f docker-compose.monitoring.yml down

if %errorlevel% neq 0 (
    echo [WARNING] Alguns serviços podem não ter parado corretamente
) else (
    echo [SUCCESS] Stack de monitoramento parada com sucesso!
)

:: Opção para limpar volumes (dados persistentes)
echo.
set /p cleanup="Deseja remover volumes de dados? (y/N): "
if /i "%cleanup%"=="y" (
    echo [INFO] Removendo volumes de dados...
    docker volume rm prometheus_data grafana_data alertmanager_data loki_data tempo_data 2>nul
    echo [INFO] Volumes removidos
)

:: Opção para limpar imagens não utilizadas
echo.
set /p cleanup_images="Deseja limpar imagens não utilizadas? (y/N): "
if /i "%cleanup_images%"=="y" (
    echo [INFO] Limpando imagens não utilizadas...
    docker image prune -f
    echo [INFO] Imagens limpas
)

echo.
echo [INFO] Para reiniciar: scripts\start-monitoring.bat
echo.
pause