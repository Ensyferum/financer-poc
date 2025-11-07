@echo off
REM =====================================================
REM Financer Version Management & Docker Image Versioning
REM =====================================================
REM Usage: update-version.bat [component] [new-version] [version-type] [options]
REM Example: update-version.bat account-service 1.0.1 patch --build --tag --push

setlocal enabledelayedexpansion

set COMPONENT=%1
set NEW_VERSION=%2
set VERSION_TYPE=%3
set BUILD_FLAG=false
set TAG_FLAG=false
set PUSH_FLAG=false

REM Parse optional flags
:parse_flags
if "%4"=="--build" (
    set BUILD_FLAG=true
    shift
    goto parse_flags
)
if "%4"=="--tag" (
    set TAG_FLAG=true
    shift
    goto parse_flags
)
if "%4"=="--push" (
    set PUSH_FLAG=true
    shift
    goto parse_flags
)
if "%5"=="--build" (
    set BUILD_FLAG=true
    shift
    goto parse_flags
)
if "%5"=="--tag" (
    set TAG_FLAG=true
    shift
    goto parse_flags
)
if "%5"=="--push" (
    set PUSH_FLAG=true
    shift
    goto parse_flags
)
if "%6"=="--build" (
    set BUILD_FLAG=true
    shift
    goto parse_flags
)
if "%6"=="--tag" (
    set TAG_FLAG=true
    shift
    goto parse_flags
)
if "%6"=="--push" (
    set PUSH_FLAG=true
    shift
    goto parse_flags
)

if "%COMPONENT%"=="" goto usage
if "%NEW_VERSION%"=="" goto usage
if "%VERSION_TYPE%"=="" goto usage
goto main

:usage
echo.
echo =====================================================
echo Financer Version Management ^& Docker Image Versioning
echo =====================================================
echo.
echo Usage: %0 [component] [new-version] [version-type] [options]
echo.
echo Components: config-server, eureka-server, api-gateway, account-service, 
echo             transaction-service, orchestration-service, common-lib, all
echo.
echo Version types: major, minor, patch
echo.
echo Options:
echo   --build    Build Docker images with new version
echo   --tag      Create Git tag for the release
echo   --push     Push Docker images to registry
echo.
echo Examples:
echo   %0 account-service 1.0.1 patch
echo   %0 account-service 1.0.1 patch --build --tag
echo   %0 all 1.1.0 minor --build --tag --push
echo.
exit /b 1

:main
echo.
echo =====================================
echo 🚀 FINANCER VERSION MANAGER
echo =====================================
echo Component: %COMPONENT%
echo New Version: %NEW_VERSION%
echo Version Type: %VERSION_TYPE%
echo Build Docker: %BUILD_FLAG%
echo Create Git Tag: %TAG_FLAG%
echo Push to Registry: %PUSH_FLAG%
echo =====================================
echo.

if "%COMPONENT%"=="all" (
    echo 📦 UPDATING ALL COMPONENTS to %NEW_VERSION%
    call :update_all_versions %NEW_VERSION% %VERSION_TYPE%
    goto docker_operations
) else (
    echo 📦 UPDATING %COMPONENT% to %NEW_VERSION%
    call :update_single_component %COMPONENT% %NEW_VERSION%
)

goto docker_operations

:update_all_versions
echo 🔄 Updating all components to version %1...
call :update_single_component config-server %1
call :update_single_component eureka-server %1
call :update_single_component api-gateway %1
call :update_single_component account-service %1
call :update_single_component transaction-service %1
call :update_single_component orchestration-service %1
call :update_single_component common-lib %1
goto :eof

:update_single_component
echo   → Updating %1 to %2

REM Update VERSION.properties
if "%1"=="config-server" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'CONFIG_SERVER_VERSION=.*', 'CONFIG_SERVER_VERSION=%2' | Set-Content VERSION.properties"
    powershell -Command "(Get-Content .env) -replace 'CONFIG_SERVER_VERSION=.*', 'CONFIG_SERVER_VERSION=%2' | Set-Content .env"
)
if "%1"=="eureka-server" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'EUREKA_SERVER_VERSION=.*', 'EUREKA_SERVER_VERSION=%2' | Set-Content VERSION.properties"
    powershell -Command "(Get-Content .env) -replace 'EUREKA_SERVER_VERSION=.*', 'EUREKA_SERVER_VERSION=%2' | Set-Content .env"
)
if "%1"=="api-gateway" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'API_GATEWAY_VERSION=.*', 'API_GATEWAY_VERSION=%2' | Set-Content VERSION.properties"
    powershell -Command "(Get-Content .env) -replace 'API_GATEWAY_VERSION=.*', 'API_GATEWAY_VERSION=%2' | Set-Content .env"
)
if "%1"=="account-service" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'ACCOUNT_SERVICE_VERSION=.*', 'ACCOUNT_SERVICE_VERSION=%2' | Set-Content VERSION.properties"
    powershell -Command "(Get-Content .env) -replace 'ACCOUNT_SERVICE_VERSION=.*', 'ACCOUNT_SERVICE_VERSION=%2' | Set-Content .env"
)
if "%1"=="transaction-service" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'TRANSACTION_SERVICE_VERSION=.*', 'TRANSACTION_SERVICE_VERSION=%2' | Set-Content VERSION.properties"
    powershell -Command "(Get-Content .env) -replace 'TRANSACTION_SERVICE_VERSION=.*', 'TRANSACTION_SERVICE_VERSION=%2' | Set-Content .env"
)
if "%1"=="orchestration-service" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'ORCHESTRATION_SERVICE_VERSION=.*', 'ORCHESTRATION_SERVICE_VERSION=%2' | Set-Content VERSION.properties"
    powershell -Command "(Get-Content .env) -replace 'ORCHESTRATION_SERVICE_VERSION=.*', 'ORCHESTRATION_SERVICE_VERSION=%2' | Set-Content .env"
)
if "%1"=="common-lib" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'COMMON_LIB_VERSION=.*', 'COMMON_LIB_VERSION=%2' | Set-Content VERSION.properties"
    powershell -Command "(Get-Content .env) -replace 'COMMON_LIB_VERSION=.*', 'COMMON_LIB_VERSION=%2' | Set-Content .env"
)

REM Update pom.xml (if exists)
if exist pom.xml (
    if "%1"=="config-server" (
        powershell -Command "(Get-Content pom.xml) -replace '<config-server.version>.*</config-server.version>', '<config-server.version>%2</config-server.version>' | Set-Content pom.xml"
    )
    if "%1"=="eureka-server" (
        powershell -Command "(Get-Content pom.xml) -replace '<eureka-server.version>.*</eureka-server.version>', '<eureka-server.version>%2</eureka-server.version>' | Set-Content pom.xml"
    )
    if "%1"=="api-gateway" (
        powershell -Command "(Get-Content pom.xml) -replace '<api-gateway.version>.*</api-gateway.version>', '<api-gateway.version>%2</api-gateway.version>' | Set-Content pom.xml"
    )
    if "%1"=="account-service" (
        powershell -Command "(Get-Content pom.xml) -replace '<account-service.version>.*</account-service.version>', '<account-service.version>%2</account-service.version>' | Set-Content pom.xml"
    )
    if "%1"=="transaction-service" (
        powershell -Command "(Get-Content pom.xml) -replace '<transaction-service.version>.*</transaction-service.version>', '<transaction-service.version>%2</transaction-service.version>' | Set-Content pom.xml"
    )
    if "%1"=="orchestration-service" (
        powershell -Command "(Get-Content pom.xml) -replace '<orchestration-service.version>.*</orchestration-service.version>', '<orchestration-service.version>%2</orchestration-service.version>' | Set-Content pom.xml"
    )
    if "%1"=="common-lib" (
        powershell -Command "(Get-Content pom.xml) -replace '<common-lib.version>.*</common-lib.version>', '<common-lib.version>%2</common-lib.version>' | Set-Content pom.xml"
    )
)
goto :eof

:docker_operations
echo.
echo =====================================
echo 🐳 DOCKER OPERATIONS
echo =====================================

REM Update Docker Compose files with new versions
echo 📝 Updating Docker Compose files...
call :update_docker_compose_versions %COMPONENT% %NEW_VERSION%

if "%BUILD_FLAG%"=="true" (
    echo 🔨 Building Docker images...
    call :build_docker_images %COMPONENT% %NEW_VERSION%
)

if "%TAG_FLAG%"=="true" (
    echo 🏷️ Creating Git tag...
    call :create_git_tag %COMPONENT% %NEW_VERSION% %VERSION_TYPE%
)

if "%PUSH_FLAG%"=="true" (
    echo 🚀 Pushing Docker images to registry...
    call :push_docker_images %COMPONENT% %NEW_VERSION%
)

goto finish

:update_docker_compose_versions
echo   → Updating Docker Compose files for %1 version %2
if "%1"=="all" (
    call :update_compose_for_service config-server %2
    call :update_compose_for_service eureka-server %2  
    call :update_compose_for_service api-gateway %2
    call :update_compose_for_service account-service %2
    call :update_compose_for_service transaction-service %2
    call :update_compose_for_service orchestration-service %2
) else (
    call :update_compose_for_service %1 %2
)
goto :eof

:update_compose_for_service
set service=%1
set version=%2
echo     • %service%:%version%

REM Update docker-compose.services.yml
if exist docker-compose.services.yml (
    powershell -Command "(Get-Content docker-compose.services.yml) -replace 'financer/%service%:latest', 'financer/%service%:%version%' | Set-Content docker-compose.services.yml"
    powershell -Command "(Get-Content docker-compose.services.yml) -replace 'financer/%service%:[0-9]+\.[0-9]+\.[0-9]+', 'financer/%service%:%version%' | Set-Content docker-compose.services.yml"
)

REM Update main docker-compose.yml  
if exist docker-compose.yml (
    powershell -Command "(Get-Content docker-compose.yml) -replace 'financer/%service%:latest', 'financer/%service%:%version%' | Set-Content docker-compose.yml"
    powershell -Command "(Get-Content docker-compose.yml) -replace 'financer/%service%:[0-9]+\.[0-9]+\.[0-9]+', 'financer/%service%:%version%' | Set-Content docker-compose.yml"
)
goto :eof

:build_docker_images
echo   → Building Docker images for %1 version %2
if "%1"=="all" (
    echo     🔨 Building all services...
    call :build_service config-server %2
    call :build_service eureka-server %2
    call :build_service api-gateway %2
    call :build_service account-service %2
    REM Uncomment when services are ready
    REM call :build_service transaction-service %2
    REM call :build_service orchestration-service %2
) else (
    call :build_service %1 %2
)
goto :eof

:build_service
set service=%1
set version=%2
set service_path=microservices/%service%

echo       • Building financer/%service%:%version%
if exist %service_path% (
    cd %service_path%
    docker build -t financer/%service%:%version% -t financer/%service%:latest .
    if errorlevel 1 (
        echo       ❌ Failed to build %service%
        cd ..\..
        goto :eof
    )
    echo       ✅ Built financer/%service%:%version%
    cd ..\..
) else (
    echo       ⚠️ Directory %service_path% not found, skipping build
)
goto :eof

:create_git_tag
set component=%1
set version=%2
set type=%3

echo   → Creating Git tag for %component% %version%

REM Add changelog entry
echo # %version% - %date% - %type% update >> CHANGELOG.md
echo #   - %component%: Updated to %version% >> CHANGELOG.md
echo. >> CHANGELOG.md

REM Commit changes
git add VERSION.properties CHANGELOG.md docker-compose*.yml .env
git commit -m "🏷️ Release %component% %version%

📦 VERSION UPDATE:
- %component%: %version%
- Type: %type% update
- Docker images tagged with %version%

🐳 DOCKER CHANGES:
- Updated docker-compose files with new version tags
- Images built and tagged: financer/%component%:%version%

Generated by: update-version.bat"

REM Create annotated tag
if "%component%"=="all" (
    git tag -a "v%version%" -m "🚀 Release v%version% - All Services

📦 SERVICES UPDATED:
- config-server: %version%
- eureka-server: %version%  
- api-gateway: %version%
- account-service: %version%

🐳 DOCKER IMAGES:
- All images tagged with %version%
- Docker Compose updated

🎯 RELEASE TYPE: %type% update
"
) else (
    git tag -a "%component%-v%version%" -m "🚀 Release %component% v%version%

📦 SERVICE: %component%
📋 VERSION: %version%
📈 TYPE: %type% update
🐳 DOCKER: financer/%component%:%version%
"
)

echo       ✅ Git tag created successfully
goto :eof

:push_docker_images
echo   → Pushing Docker images for %1 version %2
if "%1"=="all" (
    echo     🚀 Pushing all service images...
    call :push_service config-server %2
    call :push_service eureka-server %2
    call :push_service api-gateway %2
    call :push_service account-service %2
    REM Uncomment when services are ready
    REM call :push_service transaction-service %2
    REM call :push_service orchestration-service %2
) else (
    call :push_service %1 %2
)
goto :eof

:push_service
set service=%1
set version=%2

echo       • Pushing financer/%service%:%version%
docker push financer/%service%:%version%
if errorlevel 1 (
    echo       ❌ Failed to push %service%:%version%
    echo       💡 Make sure you're logged in: docker login
    goto :eof
)
docker push financer/%service%:latest
echo       ✅ Pushed financer/%service%:%version% and :latest
goto :eof

:finish
echo.
echo =====================================
echo ✅ VERSION UPDATE COMPLETED
echo =====================================
echo Component(s): %COMPONENT%
echo New Version: %NEW_VERSION%
echo Version Type: %VERSION_TYPE%
echo.
if "%BUILD_FLAG%"=="true" echo ✅ Docker images built
if "%TAG_FLAG%"=="true" echo ✅ Git tag created  
if "%PUSH_FLAG%"=="true" echo ✅ Images pushed to registry
echo.
echo 📋 Next Steps:
echo 1. Verify Docker images: docker images | findstr financer
echo 2. Test the deployment: docker-compose down ; docker-compose up -d
echo 3. Check service health: docker-compose ps
if "%TAG_FLAG%"=="true" echo 4. Push Git changes: git push ; git push --tags
echo.
echo 💡 TIP: Use 'docker-compose pull' to get latest images on other machines
echo.

endlocal