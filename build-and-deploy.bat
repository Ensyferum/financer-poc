@echo off
echo ===========================================
echo    FINANCER - Build e Deploy Completo
echo ===========================================

echo.
echo [1/4] Fazendo clean e build dos microservicos...
cd microservices\config-server
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Erro no build do config-server
    exit /b 1
)

cd ..\eureka-server  
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Erro no build do eureka-server
    exit /b 1
)

cd ..\api-gateway
call mvn clean package -DskipTests  
if %ERRORLEVEL% neq 0 (
    echo Erro no build do api-gateway
    exit /b 1
)

cd ..\account-service
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Erro no build do account-service
    exit /b 1
)

cd ..\..

echo.
echo [2/4] Construindo imagens Docker...
docker-compose build

echo.
echo [3/4] Parando containers existentes...
docker-compose down

echo.
echo [4/4] Subindo toda a stack...
docker-compose up -d

echo.
echo ===========================================
echo Stack financer iniciada com sucesso!
echo ===========================================
echo.
echo Servicos disponiveis:
echo - Config Server: http://localhost:8888
echo - Eureka Server: http://localhost:8761  
echo - API Gateway: http://localhost:8090
echo - Account Service: http://localhost:8081
echo - PostgreSQL: localhost:5432
echo - MongoDB: localhost:27017
echo - Kafka UI: http://localhost:8080
echo.