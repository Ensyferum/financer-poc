#!/bin/bash

# Build script para o projeto Financer com Java 24
# Atualizado para incluir o Orchestration Service com CAMUNDA BPM

set -e

echo "🚀 Building Financer Project with Java 24..."
echo "=============================================="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para imprimir com cores
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar se Java 24 está disponível
print_status "Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | grep "openjdk version" | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" != "24" ]; then
    print_warning "Java 24 not detected. Current version: $JAVA_VERSION"
    print_warning "The build will continue but may not use all Java 24 features"
fi

# Build das bibliotecas compartilhadas primeiro
print_status "Building shared libraries..."
./mvnw clean install -pl shared/common-lib -DskipTests

print_status "Building Eureka integration library..."
./mvnw clean install -pl shared/eureka-integration -DskipTests

# Build dos serviços de infraestrutura
print_status "Building infrastructure services..."
./mvnw clean install -pl microservices/config-server -DskipTests
./mvnw clean install -pl microservices/eureka-server -DskipTests
./mvnw clean install -pl microservices/api-gateway -DskipTests

# Build dos serviços de negócio
print_status "Building business services..."
./mvnw clean install -pl microservices/account-service -DskipTests
./mvnw clean install -pl microservices/transaction-service -DskipTests

print_status "Building Orchestration Service with CAMUNDA BPM (Java 24)..."
./mvnw clean install -pl microservices/orchestration-service -DskipTests

# Build das imagens Docker
print_status "Building Docker images..."

print_status "Building infrastructure service images..."
docker build -t financer/config-server:latest microservices/config-server/
docker build -t financer/eureka-server:latest microservices/eureka-server/
docker build -t financer/api-gateway:latest microservices/api-gateway/

print_status "Building business service images..."
docker build -t financer/account-service:latest microservices/account-service/
docker build -f microservices/transaction-service/Dockerfile -t financer/transaction-service:latest .
docker build -f microservices/orchestration-service/Dockerfile -t financer/orchestration-service:latest .

# Verificar se as imagens foram criadas
print_status "Verifying Docker images..."
docker images | grep financer

# Criar rede Docker se não existir
print_status "Creating Docker network..."
docker network create financer-network 2>/dev/null || true

print_status "✅ Build completed successfully!"
print_status "🔧 Services built with Java 24 support:"
print_status "   • Config Server"
print_status "   • Eureka Server"  
print_status "   • API Gateway"
print_status "   • Account Service"
print_status "   • Transaction Service (Java 24)"
print_status "   • Orchestration Service (Java 24 + CAMUNDA BPM 7.22.0)"
print_status ""
print_status "📋 Next steps:"
print_status "   1. Start infrastructure: docker-compose up -d"
print_status "   2. Check services: docker-compose ps"
print_status "   3. View logs: docker-compose logs -f [service-name]"
print_status "   4. CAMUNDA Cockpit: http://localhost:8085/orchestration-service"
print_status ""
print_status "🎯 Java 24 Features enabled:"
print_status "   • Preview features: --enable-preview"
print_status "   • Container support: -XX:+UseContainerSupport"
print_status "   • G1GC with optimizations"
print_status "   • String deduplication"