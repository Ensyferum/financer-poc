# Financer Database Migration System (Python)

🚀 **Serverless Python migration system** with Config Server integration, centralized logging, execution tracking, and detailed reporting.

## 🌟 Features

- **🔄 Serverless Architecture** - No persistent services, true serverless execution
- **🏗️ Config Server Integration** - Load configurations from Spring Cloud Config Server
- **📊 Dual Database Support** - PostgreSQL and MongoDB migrations
- **📋 Execution Tracking** - Complete audit trail of all migration executions
- **📈 Detailed Reporting** - JSON reports and beautiful console summaries
- **🎯 Centralized Logging** - Execution-specific logs with rotation
- **✅ Flyway Compatible** - PostgreSQL schema versioning compatible with Flyway
- **🔍 Migration Validation** - Checksum validation for migration integrity
- **📚 Execution History** - Complete history of all migration runs

## 🛠️ Requirements

- **Python 3.8+**
- **PostgreSQL** (for schema migrations and control tracking)
- **MongoDB** (optional, for NoSQL migrations)
- **Spring Cloud Config Server** (for centralized configuration)

## 🚀 Quick Start

### 1. Install Dependencies

```bash
# Windows
migrate.bat info

# Linux/macOS  
./migrate.sh info
```

The scripts will automatically:
- Create Python virtual environment
- Install required dependencies
- Execute the migration command

### 2. Configure Environment

Create or update `.env` file:

```bash
# Config Server Settings
CONFIG_SERVER_URL=http://localhost:8888
CONFIG_SERVER_USERNAME=config-admin
CONFIG_SERVER_PASSWORD=config123

# Environment
ENVIRONMENT=local
DEBUG=false
```

### 3. Run Migrations

```bash
# Execute all pending migrations
migrate.bat migrate local

# Show migration status
migrate.bat info local

# Show execution history
migrate.bat history local

# Validate migrations
migrate.bat validate local
```

## 📁 Project Structure

```
database-migration-py/
├── src/financer/migration/
│   ├── config.py              # Configuration management
│   ├── logging_config.py      # Centralized logging
│   ├── execution_control.py   # Execution tracking
│   ├── postgres_engine.py     # PostgreSQL migration engine
│   ├── mongodb_engine.py      # MongoDB migration engine
│   └── report_generator.py    # Report generation
├── migrations/
│   ├── postgresql/            # PostgreSQL migration scripts
│   │   └── V1.0.0__Create_initial_database_structure.sql
│   └── mongodb/               # MongoDB migration scripts
│       └── V1.0.0__Create_initial_collections.json
├── logs/
│   ├── database-migration/    # Execution logs
│   └── migration-reports/     # JSON reports
├── migrate.py                 # Main entry point
├── migrate.bat               # Windows script
├── migrate.sh                # Linux/macOS script
├── requirements.txt          # Python dependencies
└── .env                      # Environment configuration
```

## 🔧 Configuration

### Config Server Integration

The system loads configuration from Spring Cloud Config Server:

**URL Pattern:** `{CONFIG_SERVER_URL}/{APPLICATION_NAME}/{PROFILE}`

**Example:** `http://localhost:8888/database-migration/local`

### Database Configuration

PostgreSQL and MongoDB settings are loaded from Config Server or environment variables:

```yaml
# Config Server: database-migration.yml
migration:
  postgres:
    url: jdbc:postgresql://localhost:5432/financer_accounts
    username: financer_user
    password: financer123
    schema: public
  mongodb:
    url: mongodb://localhost:27017
    database: financer
```

## 📊 Migration Scripts

### PostgreSQL Migrations

Place SQL files in `migrations/postgresql/` following Flyway naming convention:

```
V{version}__{description}.sql
```

**Example:** `V1.0.0__Create_initial_database_structure.sql`

### MongoDB Migrations

Place JSON files in `migrations/mongodb/` with structured migration definitions:

```json
{
  "version": "1.0.0",
  "description": "Create initial collections",
  "collections": [...],
  "indexes": [...],
  "data": [...]
}
```

## 🔍 Commands

| Command | Description |
|---------|-------------|
| `migrate` | Execute all pending migrations |
| `info` | Show migration status and information |
| `validate` | Validate migration checksums |
| `history` | Show execution history |
| `clean` | Clean database schemas (DANGER!) |

## 📈 Execution Tracking

All executions are tracked in the `migration_execution_history` table:

- **Execution ID** - Unique identifier
- **Command & Environment** - What was executed
- **Status & Duration** - Success/failure and timing
- **Error Details** - Full error messages if failed
- **Migration Counts** - PostgreSQL and MongoDB migration counts
- **Report Path** - Link to detailed JSON report

## 📋 Reporting

### JSON Reports

Detailed execution reports saved in `logs/migration-reports/`:

```json
{
  "execution_id": "uuid",
  "command": "migrate",
  "environment": "local", 
  "status": "SUCCESS",
  "postgres_results": [...],
  "mongodb_results": [...],
  "system_info": {...}
}
```

### Console Output

Beautiful formatted console summaries with:
- ✅ Status indicators
- 📊 Migration statistics 
- 🎯 Error details
- 📋 Report file references

## 🔧 Development

### Adding New Migration Engines

1. Create new engine class following the pattern
2. Implement required methods
3. Add to main runner
4. Update configuration

### Custom Report Formats

Extend `ReportGenerator` class to add new output formats.

## 🚀 Deployment

### Serverless Deployment

The system is designed for serverless deployment:

```bash
# Package for deployment
zip -r migration-system.zip src/ migrations/ migrate.py requirements.txt

# Deploy to cloud functions/lambda
# Configure environment variables
# Execute via cloud scheduler
```

### Docker Integration

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY . .
RUN pip install -r requirements.txt
ENTRYPOINT ["python", "migrate.py"]
```

## 🔐 Security

- **Config Server Authentication** - Basic auth with credentials
- **Database Connections** - Secure connection strings
- **Environment Variables** - Sensitive data in env vars
- **Execution Tracking** - Complete audit trail

## 🤝 Integration

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Run Database Migrations
  run: |
    python migrate.py migrate --environment prod
```

### Monitoring Integration

- **Execution logs** in structured format
- **JSON reports** for monitoring systems
- **Status codes** for automation

## 📝 Migration from Spring Boot

This Python system maintains **100% compatibility** with the original Spring Boot system:

- ✅ Same Config Server integration
- ✅ Same database schemas
- ✅ Same execution tracking
- ✅ Same report formats
- ✅ Same logging patterns

**Migration Path:** Simply switch from Java execution to Python execution - all data and configurations remain compatible.

## 🎯 Key Advantages

1. **🚀 True Serverless** - No persistent JVM processes
2. **⚡ Fast Startup** - Python starts faster than Spring Boot
3. **💾 Lower Memory** - Reduced resource consumption
4. **🔄 Cloud Native** - Perfect for serverless platforms
5. **🛠️ Maintainable** - Clean Python code structure
6. **📊 Rich Output** - Beautiful console formatting

---

**🏗️ Built for the Financer project by the Development Team**