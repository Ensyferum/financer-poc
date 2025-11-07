# Eureka Integration Library

Biblioteca para integração automática com Eureka Server no projeto Financer.

## Funcionalidades

- **Configuração Automática**: Detecta automaticamente o ambiente (local/Docker) e configura o Eureka adequadamente
- **Health Check**: Monitor de conectividade com Eureka Server
- **Service Discovery**: Utilitários para descoberta e acesso a outros microserviços
- **Zero Configuration**: Funciona out-of-the-box com configurações inteligentes

## Como Usar

### 1. Adicione a dependência

```xml
<dependency>
    <groupId>com.financer</groupId>
    <artifactId>eureka-integration</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configuração Automática

A biblioteca detecta automaticamente o ambiente:
- **Local**: Usa `http://localhost:8761/eureka/`
- **Docker**: Usa `http://financer-eureka-server:8761/eureka/`

### 3. Configurações Customizadas (Opcional)

```yaml
financer:
  eureka:
    local-url: http://custom-eureka:8761/eureka/
    docker-url: http://docker-eureka:8761/eureka/
    lease-renewal-interval: 45
    lease-expiration-duration: 120
    enable-health-check: false
    
    docker:
      prefer-ip-address: true
      hostname: custom-hostname
```

### 4. Service Discovery

```java
@Autowired
private ServiceDiscoveryUtil serviceDiscovery;

// Buscar uma instância
Optional<ServiceInstance> instance = serviceDiscovery.getServiceInstance("account-service");

// URL do serviço
Optional<String> url = serviceDiscovery.getServiceUrl("account-service", "/api/accounts");

// Verificar disponibilidade
boolean available = serviceDiscovery.isServiceAvailable("account-service");

// Listar todos os serviços
List<String> services = serviceDiscovery.getAvailableServices();
```

### 5. Health Check

A biblioteca automaticamente adiciona um health indicator:

```bash
GET /actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "eurekaConnectivity": {
      "status": "UP",
      "details": {
        "eureka.server.status": "UP",
        "registered.applications.count": 4,
        "total.instances.count": 4,
        "registered.applications": "CONFIG-SERVER(1), EUREKA-SERVER(1), API-GATEWAY(1), ACCOUNT-SERVICE(1)",
        "self.registered": true
      }
    }
  }
}
```

## Detecção de Ambiente

A biblioteca detecta automaticamente o ambiente Docker através de:
- Profile `docker` ativo
- Variável de ambiente `DOCKER_ENVIRONMENT`
- Propriedade do sistema `java.net.preferIPv4Stack`
- Formato do hostname (contém hífen)

## Configurações Gerenciadas

A biblioteca gerencia automaticamente as seguintes propriedades:
- `eureka.client.service-url.defaultZone`
- `eureka.client.register-with-eureka`
- `eureka.client.fetch-registry`
- `eureka.instance.lease-renewal-interval-in-seconds`
- `eureka.instance.lease-expiration-duration-in-seconds`
- `eureka.instance.prefer-ip-address`
- `eureka.instance.hostname`
- `eureka.client.healthcheck.enabled`

## Logs

A biblioteca produz logs informativos sobre a configuração:

```
========================================
  Financer Eureka Integration Library
========================================
Eureka Server URL: http://financer-eureka-server:8761/eureka/
Lease Renewal Interval: 30s
Lease Expiration Duration: 90s
Register with Eureka: true
Fetch Registry: true
Health Check Enabled: true
Docker Environment Detected
Docker Hostname: account-service
Docker Prefer IP: false
========================================
```

## Exemplo de Uso Completo

### Antes (Configuração Manual)
```yaml
# application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    
---
spring:
  config:
    activate:
      on-profile: docker
eureka:
  client:
    service-url:
      defaultZone: http://financer-eureka-server:8761/eureka/
  instance:
    hostname: account-service
    prefer-ip-address: false
```

### Depois (Com a Biblioteca)
```yaml
# application.yml - arquivo vazio ou apenas customizações
financer:
  eureka:
    lease-renewal-interval: 45  # opcional, customização
```

A biblioteca cuida de tudo automaticamente! 🚀