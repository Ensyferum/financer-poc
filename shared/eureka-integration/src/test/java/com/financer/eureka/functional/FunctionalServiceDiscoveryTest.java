package com.financer.eureka.functional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Testes para FunctionalServiceDiscovery
 */
@ExtendWith(MockitoExtension.class)
class FunctionalServiceDiscoveryTest {
    
    @Mock
    private DiscoveryClient discoveryClient;
    
    @Mock
    private ServiceInstance serviceInstance1;
    
    @Mock
    private ServiceInstance serviceInstance2;
    
    private FunctionalServiceDiscovery functionalServiceDiscovery;
    
    @BeforeEach
    void setUp() {
        functionalServiceDiscovery = new FunctionalServiceDiscovery(discoveryClient);
        
        // Setup mock service instances with lenient to avoid unnecessary stubbing errors
        lenient().when(serviceInstance1.getHost()).thenReturn("host1");
        lenient().when(serviceInstance1.getPort()).thenReturn(8080);
        lenient().when(serviceInstance1.getServiceId()).thenReturn("test-service");
        
        lenient().when(serviceInstance2.getHost()).thenReturn("host2");
        lenient().when(serviceInstance2.getPort()).thenReturn(8081);
        lenient().when(serviceInstance2.getServiceId()).thenReturn("test-service");
    }
    
    @Test
    void shouldFindServiceSuccessfully() {
        // Given
        List<ServiceInstance> instances = Arrays.asList(serviceInstance1, serviceInstance2);
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // When
        Optional<ServiceInstance> result = functionalServiceDiscovery.findService("test-service");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(serviceInstance1);
    }
    
    @Test
    void shouldReturnEmptyWhenServiceNotFound() {
        // Given
        when(discoveryClient.getInstances(anyString())).thenReturn(List.of());
        
        // When
        Optional<ServiceInstance> result = functionalServiceDiscovery.findService("non-existent-service");
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldApplyFallbackWhenServiceNotFound() {
        // Given
        when(discoveryClient.getInstances(anyString())).thenReturn(List.of());
        
        // When
        Optional<String> result = functionalServiceDiscovery.findServiceAndApply(
            "non-existent-service",
            instance -> "found: " + instance.getHost(),
            () -> "fallback-value"
        );
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("fallback-value");
    }
    
    @Test
    void shouldMapAllInstances() {
        // Given
        List<ServiceInstance> instances = Arrays.asList(serviceInstance1, serviceInstance2);
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // When
        List<String> urls = functionalServiceDiscovery.mapAllInstances(
            "test-service",
            instance -> instance.getHost() + ":" + instance.getPort()
        );
        
        // Then
        assertThat(urls).containsExactly("host1:8080", "host2:8081");
    }
    
    @Test
    void shouldFilterInstancesCorrectly() {
        // Given
        List<ServiceInstance> instances = Arrays.asList(serviceInstance1, serviceInstance2);
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // When
        Optional<ServiceInstance> result = functionalServiceDiscovery.findServiceWithFilter(
            "test-service",
            instance -> instance.getPort() == 8081
        );
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(serviceInstance2);
    }
    
    @Test
    void shouldCountInstancesCorrectly() {
        // Given
        List<ServiceInstance> instances = Arrays.asList(serviceInstance1, serviceInstance2);
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // When
        long count = functionalServiceDiscovery.countInstancesWhere(
            "test-service",
            instance -> instance.getPort() > 8080
        );
        
        // Then
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void shouldFindServiceAsync() throws Exception {
        // Given
        List<ServiceInstance> instances = Arrays.asList(serviceInstance1);
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // When
        CompletableFuture<Optional<ServiceInstance>> future = 
            functionalServiceDiscovery.findServiceAsync("test-service");
        
        // Then
        Optional<ServiceInstance> result = future.get();
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(serviceInstance1);
    }
    
    @Test
    void shouldUsePredicatesCorrectly() {
        // Given
        List<ServiceInstance> instances = Arrays.asList(serviceInstance1, serviceInstance2);
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // When - Test hasPort predicate
        Optional<ServiceInstance> result = functionalServiceDiscovery.findServiceWithFilter(
            "test-service",
            FunctionalServiceDiscovery.Predicates.hasPort(8080)
        );
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(serviceInstance1);
    }
    
    @Test
    void shouldUseMappersCorrectly() {
        // Given
        List<ServiceInstance> instances = Arrays.asList(serviceInstance1);
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // When
        List<String> urls = functionalServiceDiscovery.mapAllInstances(
            "test-service",
            FunctionalServiceDiscovery.Mappers.toUrl()
        );
        
        // Then
        assertThat(urls).containsExactly("http://host1:8080");
    }
}