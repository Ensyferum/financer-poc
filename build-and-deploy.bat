@echo off
echo ===========================================
echo    FINANCER - Build e Deploy Versionado
echo ===========================================

setlocal enabledelayedexpansion

REM Read versions from VERSION.properties
echo 📖 Carregando versões do projeto...
for /f "tokens=1,2 delims==" %%i in ('findstr "_VERSION=" VERSION.properties') do (
    set %%i=%%j
    echo   %%i = %%j
)

echo.
echo [1/5] Fazendo clean e build dos microservicos...

echo   🔨 Config Server v!CONFIG_SERVER_VERSION!
cd microservices\config-server
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro no build do config-server
    exit /b 1
)
echo   ✅ Config Server compilado

cd ..\eureka-server
echo   🔨 Eureka Server v!EUREKA_SERVER_VERSION!
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro no build do eureka-server
    exit /b 1
)
echo   ✅ Eureka Server compilado

cd ..\api-gateway
echo   🔨 API Gateway v!API_GATEWAY_VERSION!
call mvn clean package -DskipTests  
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro no build do api-gateway
    exit /b 1
)
echo   ✅ API Gateway compilado

cd ..\account-service
echo   🔨 Account Service v!ACCOUNT_SERVICE_VERSION!
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro no build do account-service
    exit /b 1
)
echo   ✅ Account Service compilado

cd ..\..

echo.
echo [2/5] Construindo imagens Docker com versões...
echo   🐳 Building financer/config-server:!CONFIG_SERVER_VERSION!
docker build -t financer/config-server:!CONFIG_SERVER_VERSION! -t financer/config-server:latest microservices/config-server/

echo   🐳 Building financer/eureka-server:!EUREKA_SERVER_VERSION!
docker build -t financer/eureka-server:!EUREKA_SERVER_VERSION! -t financer/eureka-server:latest microservices/eureka-server/

echo   🐳 Building financer/api-gateway:!API_GATEWAY_VERSION!
docker build -t financer/api-gateway:!API_GATEWAY_VERSION! -t financer/api-gateway:latest microservices/api-gateway/

echo   🐳 Building financer/account-service:!ACCOUNT_SERVICE_VERSION!
docker build -t financer/account-service:!ACCOUNT_SERVICE_VERSION! -t financer/account-service:latest microservices/account-service/

echo.
echo [3/5] Parando containers existentes...
docker-compose down

echo.
echo [4/5] Atualizando docker-compose com versões atuais...
powershell -Command "(Get-Content docker-compose.services.yml) -replace 'financer/config-server:[^\\s]*', 'financer/config-server:!CONFIG_SERVER_VERSION!' | Set-Content docker-compose.services.yml.tmp ; Move-Item docker-compose.services.yml.tmp docker-compose.services.yml"
powershell -Command "(Get-Content docker-compose.services.yml) -replace 'financer/eureka-server:[^\\s]*', 'financer/eureka-server:!EUREKA_SERVER_VERSION!' | Set-Content docker-compose.services.yml.tmp ; Move-Item docker-compose.services.yml.tmp docker-compose.services.yml"
powershell -Command "(Get-Content docker-compose.services.yml) -replace 'financer/api-gateway:[^\\s]*', 'financer/api-gateway:!API_GATEWAY_VERSION!' | Set-Content docker-compose.services.yml.tmp ; Move-Item docker-compose.services.yml.tmp docker-compose.services.yml"
powershell -Command "(Get-Content docker-compose.services.yml) -replace 'financer/account-service:[^\\s]*', 'financer/account-service:!ACCOUNT_SERVICE_VERSION!' | Set-Content docker-compose.services.yml.tmp ; Move-Item docker-compose.services.yml.tmp docker-compose.services.yml"

echo.
echo [5/5] Subindo toda a stack versionada...
docker-compose up -d

echo.
echo ===========================================
echo ✅ Stack Financer iniciada com sucesso!
echo ===========================================
echo.
echo 🚀 SERVIÇOS VERSIONADOS:
echo - Config Server v!CONFIG_SERVER_VERSION!: http://localhost:8888
echo - Eureka Server v!EUREKA_SERVER_VERSION!: http://localhost:8761  
echo - API Gateway v!API_GATEWAY_VERSION!: http://localhost:8090
echo - Account Service v!ACCOUNT_SERVICE_VERSION!: http://localhost:8081
echo.
echo 🗄️ INFRAESTRUTURA:
echo - PostgreSQL: localhost:5432
echo - MongoDB: localhost:27017
echo - Kafka UI: http://localhost:8080
echo - Schema Registry: http://localhost:8082
echo.
echo 🐳 IMAGENS DOCKER CRIADAS:
docker images financer/* --format "  - {{.Repository}}:{{.Tag}}"
echo.
echo 💡 COMANDOS ÚTEIS:
echo   docker-compose ps                    # Ver status
echo   docker-compose logs -f [service]     # Ver logs
echo   docker images financer/*            # Ver imagens
echo   docker-images.bat                   # Gerenciar imagens
echo.

endlocal