@echo off
REM =====================================================
REM Financer Docker Images Management
REM =====================================================
REM Lista, limpa e gerencia imagens Docker do projeto

setlocal enabledelayedexpansion

set ACTION=%1
if "%ACTION%"=="" set ACTION=list

if "%ACTION%"=="list" goto list_images
if "%ACTION%"=="clean" goto clean_images  
if "%ACTION%"=="prune" goto prune_images
if "%ACTION%"=="build-all" goto build_all
if "%ACTION%"=="help" goto usage
goto usage

:usage
echo.
echo =====================================================
echo Financer Docker Images Management
echo =====================================================
echo.
echo Usage: %0 [action]
echo.
echo Actions:
echo   list       List all Financer Docker images (default)
echo   clean      Remove old Financer images (keep latest)
echo   prune      Remove all unused Docker images
echo   build-all  Build all services with current versions
echo   help       Show this help message
echo.
echo Examples:
echo   %0                  # List all images
echo   %0 list             # List all images  
echo   %0 clean            # Clean old versions
echo   %0 build-all        # Build all services
echo.
goto end

:list_images
echo.
echo 📦 FINANCER DOCKER IMAGES
echo =====================================
echo.
echo 🔍 All Financer images:
docker images financer/* --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
echo.
echo 📊 Image count by service:
for /f "tokens=1" %%i in ('docker images financer/* --format "{{.Repository}}" ^| sort ^| uniq -c 2^>nul') do echo   %%i
echo.
echo 💾 Total size:
docker images financer/* --format "{{.Size}}" | powershell -Command "$input | Measure-Object -Sum | Select-Object -ExpandProperty Sum"
echo.
goto end

:clean_images
echo.
echo 🧹 CLEANING OLD FINANCER IMAGES
echo =====================================
echo.
echo 🔍 Finding images to remove (keeping latest and current versions)...

REM Get current versions from VERSION.properties
for /f "tokens=2 delims==" %%i in ('findstr "_VERSION=" VERSION.properties') do (
    set VERSION_LINE=%%i
    echo Keeping version: !VERSION_LINE!
)

echo.
echo 🗑️ Removing old images...
REM Remove images that are not 'latest' and not current versions
for /f "tokens=1,2" %%i in ('docker images financer/* --format "{{.Repository}} {{.Tag}}"') do (
    set REPO=%%i
    set TAG=%%j
    if "!TAG!" neq "latest" (
        REM Check if this tag matches any current version
        set KEEP=false
        for /f "tokens=2 delims==" %%k in ('findstr "_VERSION=" VERSION.properties') do (
            if "!TAG!"=="%%k" set KEEP=true
        )
        if "!KEEP!"=="false" (
            echo   Removing !REPO!:!TAG!
            docker rmi !REPO!:!TAG! 2>nul
        )
    )
)

echo.
echo ✅ Cleanup completed
echo.
goto list_images

:prune_images
echo.
echo 🧹 PRUNING ALL UNUSED DOCKER IMAGES
echo =====================================
echo.
echo ⚠️ This will remove all unused Docker images (not just Financer)
echo Are you sure? (Y/N)
set /p CONFIRM=
if /i "%CONFIRM%" neq "Y" (
    echo Operation cancelled
    goto end
)

docker image prune -a -f
echo.
echo ✅ Docker image pruning completed
echo.
goto end

:build_all
echo.
echo 🔨 BUILDING ALL FINANCER SERVICES
echo =====================================
echo.

REM Read current versions and build images
echo 📖 Reading current versions...
for /f "tokens=1,2 delims==" %%i in ('findstr "_VERSION=" VERSION.properties') do (
    set VAR_NAME=%%i
    set VERSION=%%j
    
    REM Extract service name from variable name
    set SERVICE_NAME=!VAR_NAME:_VERSION=!
    set SERVICE_NAME=!SERVICE_NAME:CONFIG_SERVER=config-server!
    set SERVICE_NAME=!SERVICE_NAME:EUREKA_SERVER=eureka-server!
    set SERVICE_NAME=!SERVICE_NAME:API_GATEWAY=api-gateway!
    set SERVICE_NAME=!SERVICE_NAME:ACCOUNT_SERVICE=account-service!
    set SERVICE_NAME=!SERVICE_NAME:TRANSACTION_SERVICE=transaction-service!
    set SERVICE_NAME=!SERVICE_NAME:ORCHESTRATION_SERVICE=orchestration-service!
    
    if "!SERVICE_NAME!" neq "PROJECT" if "!SERVICE_NAME!" neq "COMMON_LIB" (
        echo.
        echo 🔨 Building !SERVICE_NAME!:!VERSION!
        set SERVICE_PATH=microservices/!SERVICE_NAME!
        if exist !SERVICE_PATH! (
            cd !SERVICE_PATH!
            echo   → Building financer/!SERVICE_NAME!:!VERSION!
            docker build -t financer/!SERVICE_NAME!:!VERSION! -t financer/!SERVICE_NAME!:latest .
            if errorlevel 1 (
                echo   ❌ Failed to build !SERVICE_NAME!
            ) else (
                echo   ✅ Built financer/!SERVICE_NAME!:!VERSION!
            )
            cd ..\..
        ) else (
            echo   ⚠️ Directory !SERVICE_PATH! not found, skipping
        )
    )
)

echo.
echo ✅ Build process completed
echo.
goto list_images

:end
endlocal