@echo off
echo Stopping All Financer Services...
echo.

echo [1/2] Stopping Microservices...
docker-compose -f docker-compose.services.yml down

echo.
echo [2/2] Stopping Infrastructure...
docker-compose down

echo.
echo ✅ All services stopped!
echo.
echo To remove all containers and volumes: 
echo docker-compose down -v --remove-orphans
echo docker-compose -f docker-compose.services.yml down -v --remove-orphans