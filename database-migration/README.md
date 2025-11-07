# Database Migration Tool - Unified

**Sistema de migração de banco de dados consolidado e unificado para o projeto Financer.**

## 🚀 Características

- **Lightweight**: Baseado em Java com minimal Spring Boot overhead
- **Versionado**: Controle completo de versões usando Flyway (PostgreSQL) e scripts personalizados (MongoDB)
- **Multi-database**: Suporte robusto para PostgreSQL e MongoDB
- **Serverless**: Executável como container temporário ou aplicação standalone
- **Config Server Integration**: Integração com Spring Cloud Config Server
- **Execution Tracking**: Rastreamento completo de execuções com audit trail
- **Flexível**: Suporte para ambientes local, Docker e produção
- **Reporting**: Relatórios detalhados de execução e status

## 📁 Estrutura Consolidada

```
database-migration/
├── src/main/
│   ├── java/com/financer/migration/
│   │   ├── MigrationRunner.java          # Executor principal unificado
│   │   ├── config/                       # Configurações centralizadas
│   │   ├── engines/                      # Engines PostgreSQL e MongoDB
│   │   ├── tracking/                     # Sistema de rastreamento
│   │   └── reporting/                    # Geração de relatórios
│   └── resources/db/migration/
│       ├── postgresql/                   # Scripts SQL versionados (Flyway)
│       │   ├── V1.0.0__Create_initial_database_structure.sql
│       │   ├── V2.0.0__Create_transaction_service_schema.sql
│       │   ├── V2.1.0__Create_balance_service_schema.sql
│       │   └── V2.2.0__Create_orchestration_service_schema.sql
│       └── mongodb/                      # Scripts MongoDB versionados
│           └── V1.0.0__Create_initial_collections.json
├── migrate.bat                           # Script Windows
├── migrate.sh                            # Script Linux/Mac
├── Dockerfile                            # Container otimizado
└── pom.xml                              # Configuração Maven unificada
```

## 🔗 Consolidação de Projetos

Este projeto consolidou as funcionalidades de três projetos anteriores:

1. **`database/`** - Scripts básicos de inicialização → **Integrado**
2. **`database-migration/`** - Sistema Java principal → **Base mantida**
3. **`database-migration-py/`** - Sistema Python com recursos avançados → **Funcionalidades migradas**

### Funcionalidades Migradas do Sistema Python:
- ✅ Execution tracking e audit trail
- ✅ Relatórios detalhados de execução
- ✅ Configuração centralizada via Config Server
- ✅ Sistema de logging avançado
- ✅ Validação de checksum de migrations
- ✅ Histórico completo de execuções
- ✅ MongoDB schemas com validação JSON

## 🛠️ Uso

### Executar Localmente

```bash
# Windows
migrate.bat migrate local

# Linux/Mac
./migrate.sh migrate local
```

### Executar no Docker

```bash
# Windows
migrate.bat migrate docker

# Linux/Mac
./migrate.sh migrate docker
```

### Comandos Disponíveis

| Comando | Descrição |
|---------|-----------|
| `migrate` | Executa todas as migrações pendentes |
| `info` | Mostra o status das migrações |
| `validate` | Valida as migrações |
| `clean` | Limpa o banco de dados (⚠️ CUIDADO!) |

### Usando Maven Diretamente

```bash
# Compilar
mvn clean compile

# Executar migração
mvn exec:java -Dexec.args="migrate"

# Ver informações
mvn exec:java -Dexec.args="info"

# Validar migrações
mvn exec:java -Dexec.args="validate"
```

### Usando o JAR

```bash
# Construir JAR
mvn clean package

# Executar
java -jar target/database-migration-*-shaded.jar migrate
```

## 🐳 Docker

### Construir a imagem

```bash
mvn clean package
docker build -t financer-migration .
```

### Executar como container temporário

```bash
# Migração
docker run --rm --network financer_financer-network financer-migration migrate

# Informações
docker run --rm --network financer_financer-network financer-migration info

# Com variáveis de ambiente customizadas
docker run --rm \
  -e POSTGRES_URL="jdbc:postgresql://my-postgres:5432/mydb" \
  -e POSTGRES_USER="myuser" \
  -e POSTGRES_PASSWORD="mypass" \
  financer-migration migrate
```

## 🔧 Configuração

### Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/financer_accounts` | URL do PostgreSQL |
| `POSTGRES_USER` | `financer_user` | Usuário do PostgreSQL |
| `POSTGRES_PASSWORD` | `financer123` | Senha do PostgreSQL |
| `MONGO_URL` | `mongodb://localhost:27017` | URL do MongoDB |
| `MONGO_DATABASE` | `financer` | Nome do banco MongoDB |

### Profiles Maven

| Profile | Descrição |
|---------|-----------|
| `local` | Configuração para desenvolvimento local (padrão) |
| `docker` | Configuração para containers Docker |

## 📝 Criando Migrações

### PostgreSQL (Flyway)

1. Crie um arquivo SQL em `src/main/resources/db/migration/postgresql/`
2. Use o padrão de nomenclatura: `V{versão}__{descrição}.sql`
3. Exemplo: `V1.2.0__Add_user_table.sql`

```sql
-- V1.2.0__Add_user_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### MongoDB

1. Crie um arquivo JS em `src/main/resources/db/migration/mongodb/`
2. Use o padrão de nomenclatura: `V{versão}__{descrição}.js`
3. Exemplo: `V1.3.0__Add_user_profiles.js`

```javascript
// V1.3.0__Add_user_profiles.js
db.createCollection("user_profiles", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["userId", "profileData"],
            properties: {
                userId: { bsonType: "string" },
                profileData: { bsonType: "object" }
            }
        }
    }
});
```

## 🔄 CI/CD Integration

### GitHub Actions

```yaml
- name: Run Database Migrations
  run: |
    cd database-migration
    ./migrate.sh migrate docker
```

### Docker Compose

```yaml
services:
  migration:
    build: ./database-migration
    depends_on:
      - postgres
      - mongodb
    command: ["migrate"]
    environment:
      - POSTGRES_URL=jdbc:postgresql://postgres:5432/financer_accounts
      - MONGO_URL=mongodb://mongodb:27017
```

## 📊 Monitoramento

O tool exibe informações detalhadas sobre o status das migrações:

```
🚀 Financer Database Migration Tool v1.0.0
============================================
📦 Starting database migrations...
🐘 Executing PostgreSQL migrations...
✅ PostgreSQL: 2 migrations executed
🍃 Executing MongoDB migrations...
📄 Created 'transactions' collection
📄 Created 'transaction_events' collection
📄 Created 'transaction_audit' collection
✅ MongoDB: Migrations executed successfully
✅ All migrations completed successfully!
```

## ⚠️ Considerações de Segurança

1. **Senhas**: Nunca use senhas padrão em produção
2. **Variáveis de Ambiente**: Use secrets management para credenciais
3. **Rede**: Execute em redes isoladas quando possível
4. **Backups**: Sempre faça backup antes de executar migrações
5. **Validação**: Use `validate` antes de `migrate` em produção

## 🤝 Contribuindo

1. Adicione suas migrações seguindo o padrão de versionamento
2. Teste localmente antes de fazer commit
3. Documente mudanças significativas no schema
4. Use transações quando necessário para rollback automático