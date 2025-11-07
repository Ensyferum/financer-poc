package com.financer.eureka.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for infrastructure monitoring and service discovery status
 */
@RestController
@RequestMapping("/infrastructure")
public class InfrastructureMonitoringController {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getInfrastructureStatus() {
        Map<String, Object> response = new HashMap<>();
        
        // Get overall health
        HealthComponent health = healthEndpoint.health();
        response.put("overallHealth", health);
        
        // Get current timestamp
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Infrastructure monitoring status for Financer microservices");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/docker")
    public ResponseEntity<Map<String, Object>> getDockerContainersStatus() {
        Map<String, Object> response = new HashMap<>();
        
        // Get Docker infrastructure health
        try {
            HealthComponent health = healthEndpoint.health();
            response.put("dockerContainers", health);
        } catch (Exception e) {
            response.put("dockerContainers", Map.of("status", "ERROR", "message", "Failed to get Docker health check: " + e.getMessage()));
        }
        
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getRegisteredServices() {
        Map<String, Object> response = new HashMap<>();
        
        // This would typically integrate with Eureka's ApplicationManager
        // For now, we'll return basic info
        response.put("message", "Use /eureka/apps for detailed service registry information");
        response.put("eurekaDashboard", "http://localhost:8761/");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}