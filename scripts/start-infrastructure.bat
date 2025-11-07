@echo off
echo Starting Infrastructure Services (PostgreSQL, MongoDB, Kafka)...
echo.

REM Change to project root directory (parent of scripts)
cd /d "%~dp0\.."

docker-compose up -d postgres mongodb zookeeper kafka schema-registry kafka-ui

echo.
echo ✅ Infrastructure services started!
echo.
echo Services available:
echo - PostgreSQL: localhost:5432
echo - MongoDB: localhost:27017  
echo - Kafka: localhost:9092
echo - Kafka UI: http://localhost:8080
echo - Schema Registry: localhost:8081
echo.
echo To check status: docker-compose ps
echo To view logs: docker-compose logs -f [service-name]