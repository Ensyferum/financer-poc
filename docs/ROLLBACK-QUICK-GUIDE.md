🎯 ROLLBACK RÁPIDO - Instruções de Emergência
===================================================

📌 SE ALGO DEU ERRADO, USE ESTA TAG ESTÁVEL: v1.0.0-stable

🚨 ROLLBACK EM 3 PASSOS:

1️⃣ PARAR TUDO
   docker-compose down

2️⃣ VOLTAR AO ESTADO ESTÁVEL  
   git checkout v1.0.0-stable

3️⃣ SUBIR AMBIENTE NOVAMENTE
   docker-compose up -d

✅ VERIFICAR SAÚDE (após 2-3 minutos):

🔗 URLs de Verificação:
- Eureka Server:    http://localhost:8761
- Kafka UI:         http://localhost:8080  
- Account Service:  http://localhost:8081/actuator/health
- API Gateway:      http://localhost:8090/actuator/health
- Config Server:    http://localhost:8888/actuator/health

📊 Status dos Containers:
   docker-compose ps

🎯 O QUE ESTE ROLLBACK GARANTE:
✅ Todos os 10 containers funcionando
✅ Health checks configurados e operacionais
✅ Portas sem conflitos (Schema Registry na 8082)
✅ Eureka registrando todos os serviços
✅ Arquitetura modular Docker Compose
✅ Sistema de migração Python configurado
✅ Comandos usando ';' em vez de '&&'

🔄 ESTADO VERIFICADO EM: 2024-12-28 (v1.0.0-stable)

📋 CONTAINERS ESPERADOS (10 total):
1. financer-postgres (5432)
2. financer-mongodb (27017)  
3. financer-zookeeper (2181)
4. financer-kafka (9092)
5. financer-schema-registry (8082)
6. financer-kafka-ui (8080)
7. financer-config-server (8888)
8. financer-eureka-server (8761)
9. financer-api-gateway (8090)
10. financer-account-service (8081)

⚠️  SE O ROLLBACK NÃO RESOLVER:
1. Limpar tudo: docker-compose down -v
2. Limpar Docker: docker system prune -f  
3. Rollback: git checkout v1.0.0-stable
4. Restart: docker-compose up -d

💡 DICA: Aguarde 2-3 minutos após 'docker-compose up -d' 
    para que todos os health checks se estabilizem.