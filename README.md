# Financer - Sistema de Gestão Financeira

## 🚀 Ambiente Docker - Estrutura Modular ✅ ESTÁVEL

Este projeto utiliza uma arquitetura de microserviços com Docker Compose modular para facilitar o desenvolvimento e deployment.

**🎯 Status Atual**: Ambiente funcionando perfeitamente - Ponto de rollback estável

## 🏗️ Arquitetura

### Backend
- **Java 21** com Spring Boot 3.2
- **Microserviços** com Spring Cloud
- **PostgreSQL** para dados relacionais
- **MongoDB** para dados não-relacionais
- **Apache Kafka** para mensageria
- **Docker** e Docker Compose para containerização

### Frontend
- **Angular** (versão mais recente)
- Interface responsiva e moderna

### Infraestrutura
- **Docker Compose** para ambiente local
- **Infrastructure as Code (IaC)** para configurações
- **Camunda** para workflow de solicitações

## 📁 Estrutura do Projeto

```
financer/
├── microservices/           # Microserviços
│   ├── config-server/       # Servidor de configuração
│   ├── eureka-server/       # Service discovery
│   ├── api-gateway/         # Gateway da API
│   ├── account-service/     # Gestão de contas
│   ├── transaction-service/ # Gestão de transações
│   └── orchestration-service/ # Orquestração
├── shared/                  # Bibliotecas compartilhadas
│   └── common-lib/          # Utilitários comuns
├── infrastructure/          # Configurações de infraestrutura
├── frontend/               # Aplicação Angular
├── tests/                  # Testes funcionais com Robot Framework
└── docs/                   # Documentação
```

## 🚀 Como Executar

### Pré-requisitos
- Docker e Docker Compose
- Java 21 (para build local)
- Git

### ⚡ Início Rápido

**1. Iniciar tudo:**
```cmd
docker-compose down ; docker-compose up -d
```

**2. Verificar status:**
```cmd
docker-compose ps
```

**3. Logs dos serviços:**
```cmd
docker-compose logs -f financer-account-service
```

### 🎯 ROLLBACK - Voltar ao Estado Estável

**Se algo der errado, use este ponto de rollback estável:**

```cmd
# Parar todos os serviços
docker-compose down

# Voltar ao estado estável
git checkout v1.0.0-stable

# Subir ambiente estável novamente
docker-compose up -d

# Verificar que tudo está funcionando
docker-compose ps
```

**Tag de Rollback**: `v1.0.0-stable`
- ✅ Todos os serviços funcionando
- ✅ Health checks configurados
- ✅ Portas sem conflito
- ✅ Eureka registrando serviços
- ✅ Arquitetura modular

### 📋 Verificação Pós-Rollback

Após o rollback, verifique:

1. **Containers rodando**:
   ```cmd
   docker-compose ps
   ```

2. **Eureka Dashboard**: http://localhost:8761
3. **Kafka UI**: http://localhost:8080
4. **Account Service**: http://localhost:8081/actuator/health
5. **API Gateway**: http://localhost:8090/actuator/health

### 🔄 Arquitetura Modular Docker Compose

O projeto utiliza uma estrutura modular com 3 arquivos Docker Compose:

#### `docker-compose.yml` (Principal)
- Orquestra toda a aplicação
- Inclui infraestrutura e serviços
- Network: `financer-network`

#### `docker-compose.infrastructure.yml` 
- **PostgreSQL** (5432) - Banco principal
- **MongoDB** (27017) - Dados não-relacionais  
- **Kafka** (9092) - Mensageria
- **Schema Registry** (8082) - Schemas Kafka
- **Kafka UI** (8080) - Interface Kafka
- **Zookeeper** (2181) - Coordenação Kafka

#### `docker-compose.services.yml`
- **Config Server** (8888) - Configuração centralizada
- **Eureka Server** (8761) - Service discovery
- **API Gateway** (8090) - Gateway principal
- **Account Service** (8081) - Gestão de contas

### 🎮 Comandos de Deploy

#### Opção 1: Sistema Completo (Recomendado)
```cmd
# Parar tudo e reiniciar (usando ';' como separador)
docker-compose down ; docker-compose up -d

# Verificar status
docker-compose ps

# Build e start completo
build-and-deploy.bat
```

#### Opção 2: Deploy Modular

**Só Infraestrutura:**
```cmd
docker-compose -f docker-compose.infrastructure.yml up -d
```

**Só Serviços:**
```cmd  
docker-compose -f docker-compose.services.yml up -d
```

**Build específico:**
```cmd
build-services.bat
```

### 📊 Comandos de Monitoramento

```cmd
# Status de todos os containers
docker-compose ps

# Logs específicos (substitua [service] pelo nome)
docker-compose logs -f financer-account-service
docker-compose logs -f financer-eureka-server
docker-compose logs -f financer-postgres

# Rebuild de serviço específico
docker-compose up --build -d financer-account-service

# Health check de serviços
curl http://localhost:8761    # Eureka
curl http://localhost:8081/actuator/health    # Account Service
curl http://localhost:8090/actuator/health    # API Gateway
```

### 🛠️ Troubleshooting

**Se Eureka não registra serviços:**
1. Verifique se Config Server está rodando: http://localhost:8888
2. Verifique logs: `docker-compose logs -f financer-eureka-server`
3. Restart dos serviços: `docker-compose restart`

**Se houver conflito de portas:**
- Verifique se nenhuma aplicação local usa as portas: 8080, 8081, 8088, 8090, 8761, 8888, 5432, 27017, 9092

**Para limpar tudo e recomeçar:**
```cmd
docker-compose down -v ; docker system prune -f ; docker-compose up -d
```

## 🧪 Testes

### Testes Unitários
```bash
mvn test
```

### Testes Funcionais
```bash
cd tests
robot --outputdir results tests/
```

## 📚 Funcionalidades

### Core
- ✅ Gestão de Contas Bancárias
- ✅ Gestão de Cartões (Crédito/Débito)
- ✅ Controle de Transações (PIX, Boleto, DOC/TED)
- ✅ Controle de Faturas
- ✅ Balanço Unificado e por Conta

### Recursos Avançados
- ✅ Soft Delete (exclusão virtual)
- ✅ Auditoria completa de alterações
- ✅ Controle de estado de solicitações
- ✅ Logs padronizados
- ✅ Documentação automática com Swagger

## 📊 Monitoramento

- **Swagger UI**: Documentação da API
- **Kafka UI**: Monitoramento de mensagens
- **Logs estruturados**: Padrão domínio + função + etapa + descrição

## 🔧 Desenvolvimento

### Padrões de Log
```java
// Exemplo de uso do logger padronizado
FinancerLogger logger = FinancerLogger.getLogger(AccountService.class);
logger.startContext(Domain.ACCOUNT, "createAccount", userId);
logger.info(ExecutionStep.START, "Iniciando criação de conta");
```

### Estrutura de Camadas
- **Controller**: Exposição da API
- **Service**: Lógica de negócio
- **Repository**: Acesso a dados
- **Entity**: Modelos de dados
- **DTO**: Transferência de dados

### Git Flow
- `main`: Produção
- `develop`: Desenvolvimento
- `feature/*`: Novas funcionalidades
- `hotfix/*`: Correções urgentes

## 🎯 Próximas Etapas

1. ✅ Estrutura base do projeto
2. 🔄 Infraestrutura local (PostgreSQL, MongoDB, Kafka)
3. ⏳ Servidor de configuração
4. ⏳ API Gateway e Service Discovery
5. ⏳ Microserviço de contas
6. ⏳ Sistema de logs padronizado

---

Para mais informações, consulte a documentação em `docs/`.