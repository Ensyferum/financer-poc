package com.financer.eureka.loadbalancer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Testes para FunctionalLoadBalancer
 */
@ExtendWith(MockitoExtension.class)
class FunctionalLoadBalancerTest {
    
    @Mock
    private ServiceInstance instance1;
    
    @Mock
    private ServiceInstance instance2;
    
    @Mock
    private ServiceInstance instance3;
    
    private List<ServiceInstance> instances;
    
    @BeforeEach
    void setUp() {
        // Setup mock instances with lenient to avoid unnecessary stubbing errors
        lenient().when(instance1.getHost()).thenReturn("host1");
        lenient().when(instance1.getPort()).thenReturn(8080);
        lenient().when(instance1.getServiceId()).thenReturn("test-service");
        lenient().when(instance1.getMetadata()).thenReturn(Map.of("weight", "1"));
        
        lenient().when(instance2.getHost()).thenReturn("host2");
        lenient().when(instance2.getPort()).thenReturn(8081);
        lenient().when(instance2.getServiceId()).thenReturn("test-service");
        lenient().when(instance2.getMetadata()).thenReturn(Map.of("weight", "2"));
        
        lenient().when(instance3.getHost()).thenReturn("host3");
        lenient().when(instance3.getPort()).thenReturn(8082);
        lenient().when(instance3.getServiceId()).thenReturn("test-service");
        lenient().when(instance3.getMetadata()).thenReturn(new HashMap<>());
        
        instances = Arrays.asList(instance1, instance2, instance3);
    }
    
    @Test
    void shouldReturnEmptyWhenNoInstances() {
        // Given
        FunctionalLoadBalancer loadBalancer = FunctionalLoadBalancer.roundRobin();
        
        // When
        Optional<ServiceInstance> result = loadBalancer.selectInstance(List.of());
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldSelectInstanceWithRoundRobin() {
        // Given
        FunctionalLoadBalancer loadBalancer = FunctionalLoadBalancer.roundRobin();
        
        // When - Multiple calls should cycle through instances
        Optional<ServiceInstance> result1 = loadBalancer.selectInstance(instances);
        Optional<ServiceInstance> result2 = loadBalancer.selectInstance(instances);
        Optional<ServiceInstance> result3 = loadBalancer.selectInstance(instances);
        Optional<ServiceInstance> result4 = loadBalancer.selectInstance(instances);
        
        // Then
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result3).isPresent();
        assertThat(result4).isPresent();
        
        // Should cycle back to first instance after going through all
        assertThat(result4.get()).isEqualTo(result1.get());
    }
    
    @Test
    void shouldSelectInstanceWithRandom() {
        // Given
        FunctionalLoadBalancer loadBalancer = FunctionalLoadBalancer.random();
        
        // When
        Optional<ServiceInstance> result = loadBalancer.selectInstance(instances);
        
        // Then
        assertThat(result).isPresent();
        assertThat(instances).contains(result.get());
    }
    
    @Test
    void shouldSelectInstanceWithWeighted() {
        // Given
        FunctionalLoadBalancer loadBalancer = FunctionalLoadBalancer.weighted();
        
        // When - Test multiple times to see distribution
        Map<ServiceInstance, Integer> selections = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            Optional<ServiceInstance> result = loadBalancer.selectInstance(instances);
            assertThat(result).isPresent();
            selections.merge(result.get(), 1, Integer::sum);
        }
        
        // Then - instance2 should be selected more often (weight 2 vs 1)
        assertThat(selections).containsKeys(instance1, instance2, instance3);
        // instance2 should have more selections due to higher weight
        assertThat(selections.get(instance2)).isGreaterThan(selections.get(instance1));
    }
    
    @Test
    void shouldBuildLoadBalancerWithBuilder() {
        // Given & When
        FunctionalLoadBalancer loadBalancer = FunctionalLoadBalancer.builder()
                .roundRobin()
                .build();
        
        Optional<ServiceInstance> result = loadBalancer.selectInstance(instances);
        
        // Then
        assertThat(result).isPresent();
        assertThat(instances).contains(result.get());
    }
    
    @Test
    void shouldUseCompositeStrategy() {
        // Given
        FunctionalLoadBalancer loadBalancer = FunctionalLoadBalancer.builder()
                .composite(
                    FunctionalLoadBalancer.Strategies.firstAvailable(),
                    FunctionalLoadBalancer.Strategies.random()
                )
                .build();
        
        // When
        Optional<ServiceInstance> result = loadBalancer.selectInstance(instances);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(instance1); // First available strategy should pick first
    }
    
    @Test
    void shouldUseFirstAvailableStrategy() {
        // Given
        FunctionalLoadBalancer loadBalancer = new FunctionalLoadBalancer(
                FunctionalLoadBalancer.Strategies.firstAvailable()
        );
        
        // When
        Optional<ServiceInstance> result = loadBalancer.selectInstance(instances);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(instance1);
    }
    
    @Test
    void shouldUseHealthiestStrategy() {
        // Given
        FunctionalLoadBalancer loadBalancer = new FunctionalLoadBalancer(
                FunctionalLoadBalancer.Strategies.healthiest()
        );
        
        // When
        Optional<ServiceInstance> result = loadBalancer.selectInstance(instances);
        
        // Then
        assertThat(result).isPresent();
        assertThat(instances).contains(result.get());
    }
    
    @Test
    void shouldHandleNullInstances() {
        // Given
        FunctionalLoadBalancer loadBalancer = FunctionalLoadBalancer.roundRobin();
        
        // When
        Optional<ServiceInstance> result = loadBalancer.selectInstance(null);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldCreateLoadBalancerWithFactoryMethods() {
        // Given & When
        FunctionalLoadBalancer roundRobin = FunctionalLoadBalancer.roundRobin();
        FunctionalLoadBalancer random = FunctionalLoadBalancer.random();
        FunctionalLoadBalancer weighted = FunctionalLoadBalancer.weighted();
        
        // Then
        assertThat(roundRobin).isNotNull();
        assertThat(random).isNotNull();
        assertThat(weighted).isNotNull();
        
        // All should be able to select from instances
        assertThat(roundRobin.selectInstance(instances)).isPresent();
        assertThat(random.selectInstance(instances)).isPresent();
        assertThat(weighted.selectInstance(instances)).isPresent();
    }
}