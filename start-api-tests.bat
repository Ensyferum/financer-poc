@echo off
setlocal enabledelayedexpansion

echo ==============================================
echo 🧪 Financer System - API Integration Tests
echo ==============================================

REM Configurar cores (se suportado)
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

echo %BLUE%Testing all Financer microservices...%NC%
echo.

REM Função para testar endpoint
:test_endpoint
set "service_name=%~1"
set "url=%~2"
set "expected_status=%~3"

echo Testing %service_name%: %url%

REM Usar curl para testar o endpoint
curl -s -o nul -w "%%{http_code}" "%url%" > temp_response.txt
set /p response_code=<temp_response.txt
del temp_response.txt

if "%response_code%"=="%expected_status%" (
    echo %GREEN%✅ %service_name% - OK (HTTP %response_code%)%NC%
) else (
    echo %RED%❌ %service_name% - FAILED (HTTP %response_code%, expected %expected_status%)%NC%
)
echo.
goto :eof

REM Esperar um pouco para garantir que os serviços estejam prontos
echo %YELLOW%⏳ Waiting for services to be fully ready...%NC%
timeout /t 10 /nobreak >nul
echo.

echo %BLUE%🔍 Starting Health Checks...%NC%
echo ================================================
echo.

REM Testar Config Server
call :test_endpoint "Config Server" "http://localhost:8888/actuator/health" "200"

REM Testar Eureka Server
call :test_endpoint "Eureka Server" "http://localhost:8761/actuator/health" "200"

REM Testar API Gateway
call :test_endpoint "API Gateway" "http://localhost:8090/actuator/health" "200"

REM Testar Account Service
call :test_endpoint "Account Service" "http://localhost:8081/actuator/health" "200"

REM Testar Transaction Service  
call :test_endpoint "Transaction Service" "http://localhost:8082/actuator/health" "200"

REM Testar Orchestration Service
call :test_endpoint "Orchestration Service" "http://localhost:8085/orchestration-service/actuator/health" "200"

echo %BLUE%🔍 Testing Business APIs...%NC%
echo ================================================
echo.

REM Testar endpoints de negócio
echo %YELLOW%Testing Account Service Business API...%NC%
curl -s -X GET "http://localhost:8081/api/v1/accounts" -H "Content-Type: application/json" > account_response.txt
if !errorlevel! equ 0 (
    echo %GREEN%✅ Account Service API - Accessible%NC%
) else (
    echo %RED%❌ Account Service API - Failed%NC%
)
echo.

echo %YELLOW%Testing Transaction Service Business API...%NC%
curl -s -X GET "http://localhost:8082/api/v1/transactions" -H "Content-Type: application/json" > transaction_response.txt
if !errorlevel! equ 0 (
    echo %GREEN%✅ Transaction Service API - Accessible%NC%
) else (
    echo %RED%❌ Transaction Service API - Failed%NC%
)
echo.

echo %YELLOW%Testing Orchestration Service Business API...%NC%
curl -s -X GET "http://localhost:8085/orchestration-service/api/v1/sagas/health" -H "Content-Type: application/json" > orchestration_response.txt
if !errorlevel! equ 0 (
    echo %GREEN%✅ Orchestration Service API - Accessible%NC%
) else (
    echo %RED%❌ Orchestration Service API - Failed%NC%
)
echo.

REM Testar via API Gateway
echo %BLUE%🔍 Testing via API Gateway...%NC%
echo ================================================
echo.

echo %YELLOW%Testing Account Service via Gateway...%NC%
curl -s -X GET "http://localhost:8090/account-service/api/v1/accounts" -H "Content-Type: application/json" > gateway_account_response.txt
if !errorlevel! equ 0 (
    echo %GREEN%✅ Gateway -> Account Service - OK%NC%
) else (
    echo %RED%❌ Gateway -> Account Service - Failed%NC%
)

echo %YELLOW%Testing Transaction Service via Gateway...%NC%
curl -s -X GET "http://localhost:8090/transaction-service/api/v1/transactions" -H "Content-Type: application/json" > gateway_transaction_response.txt
if !errorlevel! equ 0 (
    echo %GREEN%✅ Gateway -> Transaction Service - OK%NC%
) else (
    echo %RED%❌ Gateway -> Transaction Service - Failed%NC%
)

echo %YELLOW%Testing Orchestration Service via Gateway...%NC%
curl -s -X GET "http://localhost:8090/orchestration-service/api/v1/sagas/health" -H "Content-Type: application/json" > gateway_orchestration_response.txt
if !errorlevel! equ 0 (
    echo %GREEN%✅ Gateway -> Orchestration Service - OK%NC%
) else (
    echo %RED%❌ Gateway -> Orchestration Service - Failed%NC%
)
echo.

REM Testar criação de dados de exemplo
echo %BLUE%🔍 Testing Data Creation...%NC%
echo ================================================
echo.

echo %YELLOW%Creating test account...%NC%
curl -s -X POST "http://localhost:8081/api/v1/accounts" ^
  -H "Content-Type: application/json" ^
  -d "{\"accountNumber\":\"12345678\",\"accountType\":\"CHECKING\",\"customerId\":\"test-customer-1\",\"initialBalance\":1000.00}" > create_account_response.txt

if !errorlevel! equ 0 (
    echo %GREEN%✅ Account creation - OK%NC%
) else (
    echo %RED%❌ Account creation - Failed%NC%
)

echo %YELLOW%Creating test transaction...%NC%
curl -s -X POST "http://localhost:8082/api/v1/transactions" ^
  -H "Content-Type: application/json" ^
  -d "{\"accountId\":\"12345678\",\"amount\":100.00,\"transactionType\":\"DEPOSIT\",\"description\":\"Test deposit\"}" > create_transaction_response.txt

if !errorlevel! equ 0 (
    echo %GREEN%✅ Transaction creation - OK%NC%
) else (
    echo %RED%❌ Transaction creation - Failed%NC%
)
echo.

REM Verificar Eureka Service Discovery
echo %BLUE%🔍 Testing Service Discovery...%NC%
echo ================================================
echo.

curl -s "http://localhost:8761/eureka/apps" > eureka_apps.txt
findstr /i "account-service" eureka_apps.txt >nul
if !errorlevel! equ 0 (
    echo %GREEN%✅ Account Service registered in Eureka%NC%
) else (
    echo %RED%❌ Account Service not found in Eureka%NC%
)

findstr /i "transaction-service" eureka_apps.txt >nul
if !errorlevel! equ 0 (
    echo %GREEN%✅ Transaction Service registered in Eureka%NC%
) else (
    echo %RED%❌ Transaction Service not found in Eureka%NC%
)

findstr /i "orchestration-service" eureka_apps.txt >nul
if !errorlevel! equ 0 (
    echo %GREEN%✅ Orchestration Service registered in Eureka%NC%
) else (
    echo %RED%❌ Orchestration Service not found in Eureka%NC%
)
echo.

REM Limpar arquivos temporários
del *_response.txt 2>nul
del eureka_apps.txt 2>nul

echo %BLUE%📊 Test Summary%NC%
echo ================================================
echo.
echo %GREEN%✅ Tests completed!%NC%
echo.
echo %YELLOW%🔗 Useful URLs:%NC%
echo • Eureka Dashboard: http://localhost:8761
echo • API Gateway Health: http://localhost:8090/actuator/health
echo • Config Server: http://localhost:8888
echo • Kafka UI: http://localhost:8080
echo • Grafana: http://localhost:3000 (admin/admin123)
echo • Prometheus: http://localhost:9090
echo.

echo %YELLOW%📋 Manual Test Commands:%NC%
echo ================================================
echo curl http://localhost:8081/api/v1/accounts
echo curl http://localhost:8082/api/v1/transactions  
echo curl http://localhost:8085/orchestration-service/api/v1/sagas/health
echo.
echo curl -X POST http://localhost:8085/orchestration-service/api/v1/sagas ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"sagaType\":\"TRANSFER\",\"businessKey\":\"test-transfer-001\"}"
echo.

pause