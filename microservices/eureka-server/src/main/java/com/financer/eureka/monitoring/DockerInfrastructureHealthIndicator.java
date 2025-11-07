package com.financer.eureka.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom health indicator for monitoring Docker containers and infrastructure components
 */
@Component("dockerInfrastructure")
public class DockerInfrastructureHealthIndicator implements HealthIndicator {

    private static final Map<String, ContainerInfo> CONTAINERS = new HashMap<>();
    
    static {
        CONTAINERS.put("postgres", new ContainerInfo("financer-postgres", "localhost", 5432, "PostgreSQL Database"));
        CONTAINERS.put("mongodb", new ContainerInfo("financer-mongodb", "localhost", 27017, "MongoDB Database"));
        CONTAINERS.put("kafka", new ContainerInfo("financer-kafka", "localhost", 9092, "Apache Kafka"));
        CONTAINERS.put("schema-registry", new ContainerInfo("financer-schema-registry", "localhost", 8081, "Schema Registry"));
        CONTAINERS.put("kafka-ui", new ContainerInfo("financer-kafka-ui", "localhost", 8080, "Kafka UI"));
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        Map<String, Object> details = new HashMap<>();
        
        boolean allHealthy = true;
        
        for (Map.Entry<String, ContainerInfo> entry : CONTAINERS.entrySet()) {
            String serviceName = entry.getKey();
            ContainerInfo container = entry.getValue();
            
            boolean isHealthy = checkContainerHealth(container);
            allHealthy = allHealthy && isHealthy;
            
            Map<String, Object> containerStatus = new HashMap<>();
            containerStatus.put("name", container.getName());
            containerStatus.put("description", container.getDescription());
            containerStatus.put("host", container.getHost());
            containerStatus.put("port", container.getPort());
            containerStatus.put("status", isHealthy ? "UP" : "DOWN");
            containerStatus.put("lastChecked", System.currentTimeMillis());
            
            details.put(serviceName, containerStatus);
        }
        
        if (allHealthy) {
            builder.up().withDetails(details);
        } else {
            builder.down().withDetails(details);
        }
        
        return builder.build();
    }
    
    private boolean checkContainerHealth(ContainerInfo container) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(2000); // 2 second timeout
            socket.connect(new java.net.InetSocketAddress(container.getHost(), container.getPort()), 2000);
            return true;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    
    private static class ContainerInfo {
        private final String name;
        private final String host;
        private final int port;
        private final String description;
        
        public ContainerInfo(String name, String host, int port, String description) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDescription() { return description; }
    }
}