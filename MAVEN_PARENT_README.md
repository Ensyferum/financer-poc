# Maven Parent POM - Financer Project

## Visão Geral

O POM pai do projeto Financer foi configurado com uma estrutura robusta e abrangente para gerenciar dependências, plugins e configurações de build para todos os microserviços do sistema.

## Características Principais

### ✅ Gerenciamento Centralizado de Dependências

- **Spring Boot 3.2.0** - Framework principal
- **Spring Cloud 2023.0.0** - Microserviços e cloud patterns
- **CAMUNDA 7.20.0** - Engine de workflow para orquestração
- **PostgreSQL 42.7.1** - Driver de banco de dados
- **MongoDB 4.11.1** - Driver NoSQL
- **Flyway 10.0.1** - Versionamento de schemas

### 🔧 Plugins de Build Configurados

- **Compiler Plugin** - Compilação Java 21 com annotation processors
- **Surefire Plugin** - Testes unitários
- **Failsafe Plugin** - Testes de integração
- **JaCoCo Plugin** - Cobertura de código
- **Checkstyle Plugin** - Qualidade de código
- **SpotBugs Plugin** - Análise de bugs

### 🎯 Profiles Disponíveis

#### 1. `dev` (Padrão)
```bash
mvn clean compile
# ou
mvn clean compile -Pdev
```
- Profile ativo por padrão
- Executa todos os testes
- Ideal para desenvolvimento local

#### 2. `test`
```bash
mvn clean test -Ptest
```
- Configurado para ambiente de testes
- Executa testes unitários e de integração

#### 3. `staging`
```bash
mvn clean package -Pstaging
```
- Para ambiente de staging
- Pula testes de integração
- Executa apenas testes unitários

#### 4. `prod`
```bash
mvn clean package -Pprod
```
- Para ambiente de produção
- Pula todos os testes
- Build otimizado para produção

#### 5. `docker`
```bash
mvn clean package -Pdocker
```
- Para builds em containers Docker
- Configurações específicas para containerização

#### 6. `quality`
```bash
mvn clean verify -Pquality
```
- Executa verificações de qualidade de código
- Ativa Checkstyle e SpotBugs

## Estrutura de Módulos

```
financer-parent/
├── microservices/
│   ├── config-server/
│   ├── eureka-server/
│   ├── api-gateway/
│   └── account-service/
├── shared/
│   ├── common-lib/
│   └── eureka-integration/
└── database-migration/
```

## Comandos Úteis

### Build Completo
```bash
mvn clean install
```

### Testes com Cobertura
```bash
mvn clean test jacoco:report
```

### Análise de Qualidade
```bash
mvn clean verify -Pquality
```

### Build para Produção
```bash
mvn clean package -Pprod
```

### Verificar Profiles Ativos
```bash
mvn help:active-profiles
```

### Build Específico por Módulo
```bash
# Na raiz do projeto
mvn clean install -pl database-migration
```

## Configurações de Qualidade

### Checkstyle
- Configuração baseada no Google Java Style Guide
- Arquivo: `checkstyle.xml`
- Supressões: `checkstyle-suppressions.xml`

### SpotBugs
- Análise estática de código
- Configurado para máximo esforço e baixo threshold
- Falha o build em caso de problemas

### JaCoCo
- Cobertura de código automática
- Relatórios gerados em `target/site/jacoco/`

## Dependências Principais

### Core
- Spring Boot Starter
- Spring Cloud Config
- Spring Security

### Banco de Dados
- PostgreSQL Driver
- MongoDB Driver
- Flyway Core

### Teste
- JUnit 5
- AssertJ
- Testcontainers

### Utilitários
- Lombok
- MapStruct
- Jackson
- Caffeine Cache

## Troubleshooting

### Problema com Versões
Se encontrar warnings sobre versões de expressão, isso é normal para projetos multi-módulo e não afeta a funcionalidade.

### Falhas de Qualidade
Para pular temporariamente verificações de qualidade:
```bash
mvn clean install -Dcheckstyle.skip=true -Dspotbugs.skip=true
```

### Problemas de Dependency Resolution
Limpar cache local do Maven:
```bash
mvn dependency:purge-local-repository
```

## Status de Implementação

✅ **CONCLUÍDO** - Maven Parent Project
- Configuração abrangente de dependências
- Múltiplos profiles para diferentes ambientes  
- Plugins de qualidade e build configurados
- Documentação completa

## Próximos Passos

1. **Eureka Integration Library** - Biblioteca compartilhada para service discovery
2. **Common Library** - Utilitários compartilhados entre serviços
3. **Config Server Setup** - Servidor de configuração centralizada