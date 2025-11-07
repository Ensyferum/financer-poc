FROM eclipse-temurin:21-jdk-alpine

LABEL maintainer="financer-team"
LABEL description="API Gateway for Financer microservices"

# Instalar wget para health checks
RUN apk add --no-cache wget

WORKDIR /app

# Copiar o JAR da aplicação
COPY target/api-gateway-*.jar app.jar

# Expor a porta
EXPOSE 8080

# Configurar variáveis de ambiente
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=docker

# Comando para executar a aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=50s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1