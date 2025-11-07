# 📊 Monitoramento e Observabilidade - Financer

## Visão Geral

O sistema de monitoramento do Financer implementa uma stack completa de observabilidade com:

- **Métricas**: Prometheus + Grafana
- **Logs**: Loki + Promtail
- **Tracing**: Jaeger + Tempo
- **Alertas**: Alertmanager
- **Monitoring de Sistema**: Node Exporter + cAdvisor

## 🚀 Quick Start

### Iniciar Stack de Monitoramento

```bash
# Windows
scripts\start-monitoring.bat

# Linux/Mac
docker-compose -f docker-compose.monitoring.yml up -d
```

### Verificar Status

```bash
# Windows
scripts\monitoring-status.bat

# Verificar logs
docker-compose -f docker-compose.monitoring.yml logs -f [service_name]
```

### Parar Stack

```bash
# Windows
scripts\stop-monitoring.bat
```

## 🌐 URLs dos Serviços

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | admin/admin123 |
| **Prometheus** | http://localhost:9090 | - |
| **Alertmanager** | http://localhost:9093 | - |
| **Jaeger** | http://localhost:16686 | - |
| **Loki** | http://localhost:3100 | - |
| **Tempo** | http://localhost:3200 | - |
| **Node Exporter** | http://localhost:9100 | - |
| **cAdvisor** | http://localhost:8080 | - |

## 📈 Dashboards Grafana

### Dashboards Incluídos

1. **Financer Application Overview**
   - Status dos serviços
   - Tempo de resposta
   - Taxa de requisições
   - Taxa de erro
   - Uso de memória JVM
   - Pool de conexões do banco

2. **Infrastructure Monitoring**
   - CPU, Memória, Disco
   - Rede
   - Containers Docker

3. **Database Monitoring**
   - PostgreSQL metrics
   - MongoDB metrics
   - Pool de conexões

### Importar Dashboards Adicionais

```bash
# Acesse Grafana -> Import
# Use IDs populares:
# - Node Exporter: 1860
# - Spring Boot: 12900
# - PostgreSQL: 9628
# - Docker: 893
```

## 🔔 Configuração de Alertas

### Tipos de Alertas

#### Críticos
- Serviço indisponível
- Alta taxa de erro (>5%)
- Banco de dados indisponível

#### Warnings
- Alto tempo de resposta (>1s)
- Alto uso de memória JVM (>85%)
- Alto uso de sistema (CPU/Memória/Disco >85%)

### Canais de Notificação

#### Slack (Configurar)
```yaml
# Em monitoring/alertmanager/alertmanager.yml
slack_api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
```

#### Email (Configurar)
```yaml
# Em monitoring/alertmanager/alertmanager.yml
smtp_smarthost: 'smtp.gmail.com:587'
smtp_from: 'alerts@yourcompany.com'
```

#### Microsoft Teams
```yaml
# Adicionar webhook do Teams
webhook_url: 'https://outlook.office.com/webhook/...'
```

## 📊 Métricas Principais

### Application Metrics

| Métrica | Descrição | Tipo |
|---------|-----------|------|
| `up` | Status do serviço | Gauge |
| `http_requests_total` | Total de requisições HTTP | Counter |
| `http_request_duration_seconds` | Duração das requisições | Histogram |
| `jvm_memory_used_bytes` | Uso de memória JVM | Gauge |
| `hikaricp_connections_active` | Conexões ativas do banco | Gauge |

### Infrastructure Metrics

| Métrica | Descrição | Tipo |
|---------|-----------|------|
| `node_cpu_seconds_total` | Uso de CPU | Counter |
| `node_memory_MemAvailable_bytes` | Memória disponível | Gauge |
| `node_filesystem_free_bytes` | Espaço livre em disco | Gauge |
| `container_memory_usage_bytes` | Uso de memória do container | Gauge |

## 🔍 Tracing Distribuído

### Configuração Spring Boot

```properties
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://tempo:9411/api/v2/spans
```

### Visualizar Traces

1. **Jaeger UI**: http://localhost:16686
2. **Grafana**: Explore -> Tempo datasource

### Trace Context

- **Service**: Nome do serviço
- **Operation**: Nome da operação/endpoint
- **Duration**: Tempo total da operação
- **Tags**: Metadados adicionais

## 📝 Logs Estruturados

### Configuração Logback

```xml
<!-- logback-spring.xml -->
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <timestamp/>
            <logLevel/>
            <loggerName/>
            <message/>
            <mdc/>
            <stackTrace/>
        </providers>
    </encoder>
</appender>
```

### Pesquisar Logs no Grafana

```
# Logs de erro
{job="financer-apps"} |= "ERROR"

# Logs por serviço
{container_name="user-service"}

# Logs por período
{job="financer-apps"} |= "ERROR" | json | level="ERROR"
```

## 🔧 Troubleshooting

### Serviços não Iniciam

```bash
# Verificar logs
docker-compose -f docker-compose.monitoring.yml logs prometheus

# Verificar configuração
docker exec -it financer-prometheus promtool check config /etc/prometheus/prometheus.yml
```

### Métricas não Aparecem

1. Verificar se o endpoint `/actuator/prometheus` está acessível
2. Verificar configuração do Prometheus
3. Verificar labels e targets

### Alertas não Funcionam

1. Verificar regras de alerta: http://localhost:9090/rules
2. Verificar Alertmanager: http://localhost:9093
3. Verificar configuração de canais

### Performance Issues

```bash
# Verificar uso de recursos
docker stats

# Verificar logs de performance
docker-compose -f docker-compose.monitoring.yml logs grafana | grep -i "slow"
```

## 🔐 Segurança

### Grafana Security

```yaml
# Mudar senha padrão
environment:
  - GF_SECURITY_ADMIN_PASSWORD=SUA_SENHA_FORTE
  - GF_USERS_ALLOW_SIGN_UP=false
```

### Prometheus Security

```yaml
# Adicionar autenticação básica
basic_auth:
  username: prometheus
  password: senha_forte
```

## 📋 Backup e Restore

### Backup Grafana

```bash
# Backup dashboards
docker exec financer-grafana grafana-cli admin export-dashboard > dashboards-backup.json

# Backup volume
docker run --rm -v grafana_data:/data -v $(pwd):/backup alpine tar czf /backup/grafana-backup.tar.gz /data
```

### Backup Prometheus

```bash
# Backup dados
docker run --rm -v prometheus_data:/data -v $(pwd):/backup alpine tar czf /backup/prometheus-backup.tar.gz /data
```

## 🚀 Próximos Passos

1. **Configurar Alertas Personalizados**
   - Definir SLIs/SLOs específicos
   - Criar alertas de negócio

2. **Implementar Dashboards Avançados**
   - Business metrics
   - User journey tracking

3. **Configurar Retention Policies**
   - Definir período de retenção
   - Implementar compactação

4. **Service Level Objectives (SLO)**
   - Definir SLOs por serviço
   - Implementar error budgets

5. **Monitoring as Code**
   - Versionar dashboards
   - Automatizar deployment