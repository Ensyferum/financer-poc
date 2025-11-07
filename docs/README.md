# 📚 Documentação do Financer

## 📋 Índice da Documentação

### 🎯 Documentos Principais
| Arquivo | Descrição | Uso |
|---------|-----------|-----|
| [prompt.md](prompt.md) | **Prompt para LLMs** | Contexto completo do projeto para assistentes IA |
| [tasks.md](tasks.md) | **Gestão de Tarefas** | Tracking completo de tasks com status e prioridades |
| [DOCKER-VERSIONING.md](DOCKER-VERSIONING.md) | **Sistema de Versionamento** | Guia completo do versionamento Docker |
| [ROLLBACK-QUICK-GUIDE.md](ROLLBACK-QUICK-GUIDE.md) | **Guia de Rollback** | Instruções de emergência para rollback |

### 📊 Histórico e Logs
| Arquivo | Descrição | Uso |
|---------|-----------|-----|
| [CHANGELOG.md](CHANGELOG.md) | **Log de Mudanças** | Histórico detalhado de releases |
| [CHANGELOG-ROLLBACK.md](CHANGELOG-ROLLBACK.md) | **Rollback Log** | Documentação técnica do ponto de rollback |

## 🚀 Como Usar Esta Documentação

### 👨‍💻 Para Desenvolvedores
1. **Começar aqui**: [prompt.md](prompt.md) - Entenda o contexto completo
2. **Tarefas**: [tasks.md](tasks.md) - Veja o que precisa ser feito
3. **Versionamento**: [DOCKER-VERSIONING.md](DOCKER-VERSIONING.md) - Aprenda o sistema

### 🤖 Para LLMs/Assistentes IA
1. **Prompt Principal**: [prompt.md](prompt.md) - Contexto completo e instruções
2. **Estado Atual**: [tasks.md](tasks.md) - Tasks implementadas e pendentes
3. **Referência Técnica**: [DOCKER-VERSIONING.md](DOCKER-VERSIONING.md) - Detalhes técnicos

### 🚨 Para Troubleshooting
1. **Rollback Rápido**: [ROLLBACK-QUICK-GUIDE.md](ROLLBACK-QUICK-GUIDE.md) - Em caso de problemas
2. **Log de Mudanças**: [CHANGELOG-ROLLBACK.md](CHANGELOG-ROLLBACK.md) - Estado estável documentado

## 📁 Estrutura de Arquivos

```
docs/
├── 🎯 prompt.md                    # Prompt otimizado para LLMs
├── 📋 tasks.md                     # Gestão completa de tarefas  
├── 🐳 DOCKER-VERSIONING.md        # Sistema de versionamento
├── 🚨 ROLLBACK-QUICK-GUIDE.md     # Guia de rollback de emergência
├── 📊 CHANGELOG.md                # Log de mudanças do projeto
├── 🔄 CHANGELOG-ROLLBACK.md       # Documentação do rollback
└── 📚 README.md                   # Este índice
```

## 🔗 Links Rápidos

### 🛠️ Comandos Essenciais
- **Build e Deploy**: `scripts/build-and-deploy.bat`
- **Versionamento**: `scripts/update-version.bat [service] [version] [type]`
- **Rollback**: `git checkout v1.0.0-stable && docker-compose up -d`

### 🌐 URLs do Sistema
- **Eureka Server**: http://localhost:8761
- **Kafka UI**: http://localhost:8080
- **API Gateway**: http://localhost:8090
- **Account Service**: http://localhost:8081

### 📊 Monitoramento
- **Container Status**: `docker-compose ps`
- **Logs**: `docker-compose logs -f [service]`
- **Images**: `scripts/docker-images.bat list`

---

💡 **Dica**: Para contribuir com a documentação, mantenha este índice atualizado quando adicionar novos arquivos!

---

⬅️ **Voltar para**: [README principal](../README.md)