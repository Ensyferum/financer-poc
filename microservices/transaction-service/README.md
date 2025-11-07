# 🏦 **Transaction Service**

## 📋 **Visão Geral**

O Transaction Service é um microserviço responsável pelo processamento de transações financeiras, implementando padrões de **CQRS (Command Query Responsibility Segregation)** e **Event Sourcing** com programação funcional reativa.

## 🎯 **Funcionalidades Principais**

### ✅ **Tipos de Transação Suportados**
- **DEPOSIT** - Depósitos em conta
- **WITHDRAWAL** - Saques com validação de saldo
- **TRANSFER** - Transferências entre contas
- **PAYMENT** - Pagamentos para terceiros
- **REFUND** - Estornos de transações
- **ADJUSTMENT** - Ajustes manuais de saldo
- **FEE** - Cobrança de taxas
- **INTEREST** - Crédito de juros

### 🔄 **Estados da Transação**
- **PENDING** - Criada, aguardando processamento
- **PROCESSING** - Em processamento
- **COMPLETED** - Concluída com sucesso
- **FAILED** - Falhou por regra de negócio
- **CANCELLED** - Cancelada pelo usuário/sistema
- **REVERSED** - Revertida/estornada

### 🏗️ **Arquitetura**

```
├── Domain Layer
│   ├── Models (Transaction, Money, AccountId)
│   ├── Value Objects (TransactionId, Money)
│   ├── Domain Services (TransactionDomainService)
│   └── Repository Interfaces
├── Application Layer
│   ├── Use Cases (Create, Process, Search)
│   ├── DTOs (Request/Response)
│   └── Application Services
├── Infrastructure Layer
│   ├── MongoDB (Event Sourcing)
│   ├── PostgreSQL (Read Models)
│   ├── Feign Clients (Account Service)
│   └── REST Controllers
```

## 🛠️ **Tecnologias Utilizadas**

### **Core Framework**
- **Spring Boot 3.2** - Framework base
- **Spring WebFlux** - Programação reativa
- **Spring Data MongoDB Reactive** - Event sourcing
- **Spring Data JPA** - Read models (PostgreSQL)

### **Integração & Comunicação**
- **Spring Cloud OpenFeign** - Comunicação com Account Service
- **Spring Cloud Config** - Configuração centralizada
- **Spring Cloud Eureka** - Service discovery
- **Spring Kafka** - Event publishing

### **Observabilidade**
- **Spring Actuator** - Health checks e métricas
- **Micrometer** - Métricas customizadas
- **Structured Logging** - Logs padronizados

### **Testes & Qualidade**
- **JUnit 5** - Testes unitários
- **TestContainers** - Testes de integração
- **Reactor Test** - Testes reativos

## 🚀 **Como Executar**

### **Pré-requisitos**
```bash
# Serviços necessários
- Config Server (porta 8888)
- Eureka Server (porta 8761)
- MongoDB (porta 27017)
- PostgreSQL (porta 5432)
- Account Service (porta 8081)
```

### **Executar Localmente**
```bash
# 1. Compilar o projeto
mvn clean compile

# 2. Executar os testes
mvn test

# 3. Executar a aplicação
mvn spring-boot:run

# 4. Verificar saúde
curl http://localhost:8083/actuator/health
```

### **Docker**
```bash
# 1. Build da imagem
docker build -t financer/transaction-service:1.0.0 .

# 2. Executar container
docker run -p 8083:8083 financer/transaction-service:1.0.0
```

## 📡 **API Endpoints**

### **Transações**
```http
POST   /api/transactions              # Criar transação
POST   /api/transactions/{id}/process # Processar transação
GET    /api/transactions/{id}         # Buscar por ID
GET    /api/transactions/search       # Buscar com filtros
GET    /api/transactions/account/{id} # Buscar por conta
POST   /api/transactions/{id}/cancel  # Cancelar transação
POST   /api/transactions/{id}/reverse # Reverter transação
GET    /api/transactions/stats        # Estatísticas
GET    /api/transactions/health       # Health check
```

### **Exemplo de Criação de Transação**
```json
POST /api/transactions
{
  "sourceAccountId": "123e4567-e89b-12d3-a456-426614174000",
  "destinationAccountId": "123e4567-e89b-12d3-a456-426614174001",
  "amount": 100.50,
  "currency": "BRL",
  "type": "TRANSFER",
  "description": "Transferência entre contas",
  "reference": "TXN-20241107-001",
  "correlationId": "corr-123456789"
}
```

### **Exemplo de Resposta**
```json
{
  "success": true,
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174002",
    "sourceAccountId": "123e4567-e89b-12d3-a456-426614174000",
    "destinationAccountId": "123e4567-e89b-12d3-a456-426614174001",
    "amount": 100.50,
    "currency": "BRL",
    "fee": 2.00,
    "totalAmount": 102.50,
    "type": "TRANSFER",
    "status": "PENDING",
    "description": "Transferência entre contas",
    "correlationId": "corr-123456789",
    "createdAt": "2024-11-07T14:30:00",
    "updatedAt": "2024-11-07T14:30:00"
  },
  "message": "Transaction created successfully"
}
```

## 🔍 **Padrões Implementados**

### **Domain-Driven Design (DDD)**
- **Value Objects**: Money, TransactionId, AccountId
- **Entities**: Transaction
- **Domain Services**: Business logic validation
- **Repository Pattern**: Data access abstraction

### **CQRS (Command Query Responsibility Segregation)**
- **Commands**: Create, Process, Cancel, Reverse
- **Queries**: Search, GetById, GetByAccount
- **Separate Models**: Write (MongoDB) / Read (PostgreSQL)

### **Event Sourcing**
- **Events**: TransactionCreated, TransactionProcessed, etc.
- **Event Store**: MongoDB collections
- **Event Replay**: State reconstruction

### **Functional Programming**
- **Immutable Objects**: Value objects e DTOs
- **Pure Functions**: Domain logic
- **Monadic Patterns**: Reactive streams
- **Function Composition**: Use case chains

### **Reactive Programming**
- **Non-blocking I/O**: WebFlux + MongoDB Reactive
- **Backpressure**: Reactive streams
- **Error Handling**: Reactive error operators
- **Composable Operations**: Mono/Flux chains

## 🔒 **Validações de Negócio**

### **Depósitos (DEPOSIT)**
- Conta de destino deve existir e estar ativa
- Valor deve ser maior que zero
- Sem validação de saldo (operação de crédito)

### **Saques (WITHDRAWAL)**
- Conta de origem deve existir e estar ativa
- Valor deve ser maior que zero
- Saldo suficiente (valor + taxa)
- Cálculo automático de taxas

### **Transferências (TRANSFER)**
- Contas origem e destino devem existir e estar ativas
- Contas origem e destino devem ser diferentes
- Saldo suficiente na conta origem (valor + taxa)
- Taxa fixa de R$ 2,00

### **Pagamentos (PAYMENT)**
- Mesmas validações dos saques
- Taxa de 0,1% do valor (mínimo R$ 1,00)

## 📊 **Observabilidade**

### **Métricas Expostas**
```
# Prometheus metrics disponíveis em /actuator/prometheus
transaction_created_total        # Total de transações criadas
transaction_processed_total      # Total de transações processadas  
transaction_failed_total         # Total de transações falhadas
transaction_processing_duration  # Tempo de processamento
account_balance_requests_total   # Requests para Account Service
```

### **Health Checks**
```bash
# Health check básico
GET /actuator/health

# Health check detalhado
GET /actuator/health/detail

# Métricas da aplicação
GET /actuator/metrics

# Informações da aplicação
GET /actuator/info
```

### **Logs Estruturados**
```json
{
  "timestamp": "2024-11-07T14:30:00.000Z",
  "level": "INFO",
  "logger": "com.financer.transaction.application.usecase.CreateTransactionUseCase",
  "message": "Transaction created successfully",
  "domain": "TRANSACTION",
  "function": "createTransaction",
  "step": "COMPLETION",
  "correlationId": "corr-123456789",
  "transactionId": "txn-123456789"
}
```

## 🧪 **Testes**

### **Estrutura de Testes**
```
src/test/java/
├── unit/          # Testes unitários (domínio)
├── integration/   # Testes de integração
├── contract/      # Testes de contrato
└── e2e/          # Testes end-to-end
```

### **Executar Testes**
```bash
# Todos os testes
mvn test

# Apenas testes unitários
mvn test -Dtest="*Test"

# Apenas testes de integração
mvn test -Dtest="*IT"

# Com coverage
mvn test jacoco:report
```

## 🔧 **Configuração**

### **Profiles Disponíveis**
- **dev** - Desenvolvimento local
- **test** - Testes automatizados
- **docker** - Execução em container
- **prod** - Produção

### **Variáveis de Ambiente**
```env
SPRING_PROFILES_ACTIVE=dev
SPRING_CLOUD_CONFIG_URI=http://config-server:8888
EUREKA_SERVER_URL=http://eureka-server:8761/eureka
MONGODB_URI=mongodb://localhost:27017/financer_transactions
POSTGRES_URL=jdbc:postgresql://localhost:5432/financer
ACCOUNT_SERVICE_URL=http://account-service:8081
```

## 📈 **Roadmap**

### **Próximas Funcionalidades**
- [ ] **Transações Recorrentes** - Agendamento automático
- [ ] **Limite de Transações** - Controle por período
- [ ] **Auditoria Avançada** - Trilha completa de eventos
- [ ] **Reconciliação** - Validação com sistemas externos
- [ ] **GraphQL API** - Query flexível
- [ ] **Stream Processing** - Kafka Streams para analytics

### **Melhorias Técnicas**
- [ ] **Cache Distribuído** - Redis para read models
- [ ] **Snapshots** - Otimização do event sourcing
- [ ] **Saga Pattern** - Transações distribuídas
- [ ] **Circuit Breaker** - Resilience4j
- [ ] **Rate Limiting** - Controle de taxa de requests

## 📞 **Suporte**

### **Contatos da Equipe**
- **Tech Lead**: financer-team@company.com
- **DevOps**: devops@company.com
- **Product**: product@company.com

### **Links Úteis**
- [Documentação Completa](http://docs.financer.com)
- [API Documentation](http://localhost:8083/swagger-ui.html)
- [Monitoring Dashboard](http://grafana.financer.com)
- [Log Aggregation](http://kibana.financer.com)

---

**Status**: ✅ **Pronto para Produção**  
**Versão**: 1.0.0  
**Última Atualização**: 2024-11-07