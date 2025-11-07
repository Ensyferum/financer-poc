# 🏷️ Sistema de Versionamento Docker - Financer

## 📋 Visão Geral

O projeto Financer implementa um sistema completo de versionamento para imagens Docker, permitindo controle granular sobre versões de cada microserviço.

## 🔧 Componentes

### 1. `update-version.bat` - Script Principal
Script centralizado para gerenciar versões de todos os componentes.

#### Uso:
```cmd
update-version.bat [component] [new-version] [version-type] [options]
```

#### Exemplos:
```cmd
# Atualizar apenas a versão
update-version.bat account-service 1.0.2 patch

# Atualizar versão + build Docker + criar Git tag
update-version.bat account-service 1.0.2 patch --build --tag

# Atualizar todos os serviços para mesma versão
update-version.bat all 1.1.0 minor --build --tag --push
```

#### Componentes Disponíveis:
- `config-server`
- `eureka-server` 
- `api-gateway`
- `account-service`
- `transaction-service`
- `orchestration-service`
- `common-lib`
- `all` (todos os serviços)

#### Tipos de Versão:
- `major` - Mudanças que quebram compatibilidade
- `minor` - Novas funcionalidades compatíveis
- `patch` - Correções de bugs

#### Opções:
- `--build` - Constrói imagens Docker com nova versão
- `--tag` - Cria tag Git e commit das mudanças  
- `--push` - Envia imagens para registry Docker

### 2. `docker-images.bat` - Gerenciamento de Imagens
Script para listar, limpar e gerenciar imagens Docker do projeto.

#### Comandos:
```cmd
docker-images.bat list        # Lista todas as imagens Financer
docker-images.bat clean       # Remove versões antigas (mantém latest e atual)
docker-images.bat prune       # Remove todas as imagens não utilizadas
docker-images.bat build-all   # Reconstrói todas as imagens com versões atuais
```

### 3. `build-and-deploy.bat` - Deploy Versionado
Script de build e deploy que usa automaticamente as versões definidas.

```cmd
build-and-deploy.bat
```

**Funcionalidades:**
- Lê versões do `VERSION.properties`
- Compila cada serviço com Maven
- Constrói imagens Docker com tags específicas
- Atualiza docker-compose.yml com versões corretas
- Deploy da stack completa

## 📄 Arquivos de Configuração

### `VERSION.properties`
Arquivo central que define as versões de todos os componentes:

```properties
PROJECT_VERSION=1.0.0
CONFIG_SERVER_VERSION=1.0.0
EUREKA_SERVER_VERSION=1.0.1
API_GATEWAY_VERSION=1.0.0
ACCOUNT_SERVICE_VERSION=1.0.1
TRANSACTION_SERVICE_VERSION=0.1.0
ORCHESTRATION_SERVICE_VERSION=0.1.0
COMMON_LIB_VERSION=1.0.0
```

### `.env`
Variáveis de ambiente para Docker Compose:

```env
CONFIG_SERVER_VERSION=1.0.0
EUREKA_SERVER_VERSION=1.0.1
API_GATEWAY_VERSION=1.0.0
ACCOUNT_SERVICE_VERSION=1.0.1
# ... outras configurações
```

### `docker-compose.services.yml`
Arquivo Docker Compose que usa variáveis de ambiente:

```yaml
services:
  config-server:
    image: financer/config-server:${CONFIG_SERVER_VERSION:-1.0.0}
    build:
      args:
        - VERSION=${CONFIG_SERVER_VERSION:-1.0.0}
```

## 🚀 Fluxo de Trabalho

### 1. Atualização de Versão Simples
```cmd
# Atualizar versão do account-service
update-version.bat account-service 1.0.2 patch
```

**O que acontece:**
- ✅ Atualiza `VERSION.properties`
- ✅ Atualiza `.env`
- ✅ Atualiza arquivos `docker-compose*.yml`

### 2. Release Completo
```cmd
# Release com build e tag
update-version.bat account-service 1.0.2 patch --build --tag
```

**O que acontece:**
- ✅ Atualiza arquivos de versão
- ✅ Constrói imagem Docker: `financer/account-service:1.0.2`
- ✅ Cria tag: `financer/account-service:latest`
- ✅ Faz commit Git com changelog
- ✅ Cria tag Git: `account-service-v1.0.2`

### 3. Deploy com Nova Versão
```cmd
# Depois do update-version
build-and-deploy.bat
```

**O que acontece:**
- ✅ Compila JAR com Maven
- ✅ Constrói imagem versionada
- ✅ Para containers antigos
- ✅ Sobe nova stack com versões corretas

## 🐳 Tags de Imagem Docker

Cada serviço terá duas tags:
- `financer/service:1.0.2` - Versão específica
- `financer/service:latest` - Sempre aponta para a versão mais recente

### Exemplo de Imagens:
```
REPOSITORY                 TAG       IMAGE ID       CREATED
financer/eureka-server     1.0.1     5db9f4b27d7a   2 hours ago
financer/eureka-server     latest    5db9f4b27d7a   2 hours ago
financer/account-service   1.0.1     740985748e7b   2 hours ago
financer/account-service   latest    740985748e7b   2 hours ago
```

## 📊 Git Tags e Commits

### Estrutura de Tags:
- `v1.0.0-stable` - Tag de rollback estável
- `account-service-v1.0.1` - Tag específica do serviço
- `v1.1.0` - Tag de release completo (quando usar `all`)

### Commits Automáticos:
```
🏷️ Release account-service 1.0.1

📦 VERSION UPDATE:
- account-service: 1.0.1
- Type: patch update
- Docker images tagged with 1.0.1

🐳 DOCKER CHANGES:
- Updated docker-compose files with new version tags
- Images built and tagged: financer/account-service:1.0.1
```

## 🔄 Comandos de Manutenção

### Listar Imagens
```cmd
docker-images.bat list
```

### Limpar Versões Antigas
```cmd
docker-images.bat clean
```

### Reconstruir Tudo
```cmd
docker-images.bat build-all
```

### Verificar Versões Atuais
```cmd
findstr "_VERSION=" VERSION.properties
```

### Status dos Containers
```cmd
docker-compose ps
```

## 🚨 Solução de Problemas

### Problema: Imagem não encontrada
```cmd
# Verificar se imagem foi construída
docker images financer/*

# Reconstruir se necessário
docker-images.bat build-all
```

### Problema: Versões inconsistentes
```cmd
# Sincronizar .env com VERSION.properties
update-version.bat all [current-version] patch
```

### Problema: Container não inicia
```cmd
# Verificar logs do container
docker-compose logs -f [service-name]

# Reconstruir e reiniciar
docker-compose down
build-and-deploy.bat
```

## 📈 Vantagens do Sistema

✅ **Controle Granular**: Versiona cada serviço independentemente  
✅ **Automatização**: Scripts automatizam todo o processo  
✅ **Rastreabilidade**: Git tags e commits para cada mudança  
✅ **Rollback**: Fácil rollback para versões específicas  
✅ **CI/CD Ready**: Preparado para pipelines de integração  
✅ **Registry Support**: Suporte a push para registries Docker  
✅ **Desenvolvimento**: Facilita testes com versões específicas  

## 🎯 Próximos Passos

1. **Registry Integration**: Configure push automático para registry
2. **CI/CD Pipeline**: Integre com GitHub Actions ou Jenkins
3. **Health Checks**: Adicione validação de saúde pós-deploy
4. **Rollback Automation**: Scripts de rollback automático
5. **Version Validation**: Validação de compatibilidade entre versões