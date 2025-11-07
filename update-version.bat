@echo off
REM Financer Version Management Script for Windows
REM Usage: update-version.bat [component] [new-version] [version-type]
REM Example: update-version.bat account-service 1.0.1 patch

setlocal enabledelayedexpansion

set COMPONENT=%1
set NEW_VERSION=%2
set VERSION_TYPE=%3

if "%COMPONENT%"=="" goto usage
if "%NEW_VERSION%"=="" goto usage
if "%VERSION_TYPE%"=="" goto usage
goto main

:usage
echo Usage: %0 [component] [new-version] [version-type]
echo Components: config-server, eureka-server, api-gateway, account-service, transaction-service, orchestration-service, common-lib
echo Version types: major, minor, patch
echo Example: %0 account-service 1.0.1 patch
exit /b 1

:main
echo Updating %COMPONENT% to version %NEW_VERSION% (%VERSION_TYPE% update)

REM Update VERSION.properties
if "%COMPONENT%"=="config-server" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'CONFIG_SERVER_VERSION=.*', 'CONFIG_SERVER_VERSION=%NEW_VERSION%' | Set-Content VERSION.properties"
)
if "%COMPONENT%"=="eureka-server" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'EUREKA_SERVER_VERSION=.*', 'EUREKA_SERVER_VERSION=%NEW_VERSION%' | Set-Content VERSION.properties"
)
if "%COMPONENT%"=="api-gateway" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'API_GATEWAY_VERSION=.*', 'API_GATEWAY_VERSION=%NEW_VERSION%' | Set-Content VERSION.properties"
)
if "%COMPONENT%"=="account-service" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'ACCOUNT_SERVICE_VERSION=.*', 'ACCOUNT_SERVICE_VERSION=%NEW_VERSION%' | Set-Content VERSION.properties"
)
if "%COMPONENT%"=="transaction-service" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'TRANSACTION_SERVICE_VERSION=.*', 'TRANSACTION_SERVICE_VERSION=%NEW_VERSION%' | Set-Content VERSION.properties"
)
if "%COMPONENT%"=="orchestration-service" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'ORCHESTRATION_SERVICE_VERSION=.*', 'ORCHESTRATION_SERVICE_VERSION=%NEW_VERSION%' | Set-Content VERSION.properties"
)
if "%COMPONENT%"=="common-lib" (
    powershell -Command "(Get-Content VERSION.properties) -replace 'COMMON_LIB_VERSION=.*', 'COMMON_LIB_VERSION=%NEW_VERSION%' | Set-Content VERSION.properties"
)

REM Update pom.xml
if "%COMPONENT%"=="config-server" (
    powershell -Command "(Get-Content pom.xml) -replace '<config-server.version>.*</config-server.version>', '<config-server.version>%NEW_VERSION%</config-server.version>' | Set-Content pom.xml"
)
if "%COMPONENT%"=="eureka-server" (
    powershell -Command "(Get-Content pom.xml) -replace '<eureka-server.version>.*</eureka-server.version>', '<eureka-server.version>%NEW_VERSION%</eureka-server.version>' | Set-Content pom.xml"
)
if "%COMPONENT%"=="api-gateway" (
    powershell -Command "(Get-Content pom.xml) -replace '<api-gateway.version>.*</api-gateway.version>', '<api-gateway.version>%NEW_VERSION%</api-gateway.version>' | Set-Content pom.xml"
)
if "%COMPONENT%"=="account-service" (
    powershell -Command "(Get-Content pom.xml) -replace '<account-service.version>.*</account-service.version>', '<account-service.version>%NEW_VERSION%</account-service.version>' | Set-Content pom.xml"
)
if "%COMPONENT%"=="transaction-service" (
    powershell -Command "(Get-Content pom.xml) -replace '<transaction-service.version>.*</transaction-service.version>', '<transaction-service.version>%NEW_VERSION%</transaction-service.version>' | Set-Content pom.xml"
)
if "%COMPONENT%"=="orchestration-service" (
    powershell -Command "(Get-Content pom.xml) -replace '<orchestration-service.version>.*</orchestration-service.version>', '<orchestration-service.version>%NEW_VERSION%</orchestration-service.version>' | Set-Content pom.xml"
)
if "%COMPONENT%"=="common-lib" (
    powershell -Command "(Get-Content pom.xml) -replace '<common-lib.version>.*</common-lib.version>', '<common-lib.version>%NEW_VERSION%</common-lib.version>' | Set-Content pom.xml"
)

REM Add changelog entry
echo # %NEW_VERSION% - %date% - %VERSION_TYPE% update >> CHANGELOG.md
echo #   - %COMPONENT%: Updated to %NEW_VERSION% >> CHANGELOG.md
echo. >> CHANGELOG.md

echo Successfully updated %COMPONENT% to version %NEW_VERSION%
echo.
echo Don't forget to:
echo 1. Update the changelog with specific changes
echo 2. Rebuild the affected services
echo 3. Tag the release in git: git tag %COMPONENT%-%NEW_VERSION%
echo 4. Push changes: git push ^&^& git push --tags

endlocal