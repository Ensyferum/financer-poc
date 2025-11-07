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
- Java 21
- Maven 3.9+
- Node.js (para o frontend)

### Opção 1: Sistema Completo (Recomendado)
```bash
# Inicia toda a infraestrutura e serviços
start-all.bat

# Para parar tudo
stop-all.bat
```

### Opção 2: Passo a Passo

#### 1. Infraestrutura
```bash
# Iniciar apenas a infraestrutura (PostgreSQL, MongoDB, Kafka)
start-infrastructure.bat
```

#### 2. Construir Serviços
```bash
# Compilar todos os microserviços
build-services.bat
```

#### 3. Microserviços
```bash
# Iniciar os microserviços (Config Server, Eureka, API Gateway)
start-services.bat
```

### Comandos Úteis
```bash
# Ver status dos serviços
docker-compose ps
docker-compose -f docker-compose.services.yml ps

# Ver logs
docker-compose logs -f [nome-do-serviço]
docker-compose -f docker-compose.services.yml logs -f [nome-do-serviço]

# Rebuild de um serviço específico
docker-compose -f docker-compose.services.yml up --build -d [nome-do-serviço]
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