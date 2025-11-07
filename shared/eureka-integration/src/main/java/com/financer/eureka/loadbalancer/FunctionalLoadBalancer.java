package com.financer.eureka.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Functional Load Balancer
 * 
 * Implementa diferentes estratégias de load balancing usando programação funcional.
 * Suporta round-robin, random, weighted e custom strategies.
 */
@Slf4j
public final class FunctionalLoadBalancer {
    
    /**
     * Estratégia de load balancing
     */
    @FunctionalInterface
    public interface LoadBalancingStrategy extends Function<List<ServiceInstance>, Optional<ServiceInstance>> {
    }
    
    /**
     * Estratégias pré-definidas de load balancing
     */
    public static final class Strategies {
        
        private static final AtomicInteger roundRobinCounter = new AtomicInteger(0);
        
        /**
         * Estratégia Round Robin
         */
        public static LoadBalancingStrategy roundRobin() {
            return instances -> {
                if (instances.isEmpty()) {
                    return Optional.empty();
                }
                
                int index = roundRobinCounter.getAndIncrement() % instances.size();
                ServiceInstance selected = instances.get(index);
                
                log.debug("Round-robin selected instance {}/{}: {}:{}", 
                         index + 1, instances.size(), selected.getHost(), selected.getPort());
                
                return Optional.of(selected);
            };
        }
        
        /**
         * Estratégia Random
         */
        public static LoadBalancingStrategy random() {
            return instances -> {
                if (instances.isEmpty()) {
                    return Optional.empty();
                }
                
                int index = ThreadLocalRandom.current().nextInt(instances.size());
                ServiceInstance selected = instances.get(index);
                
                log.debug("Random selected instance {}/{}: {}:{}", 
                         index + 1, instances.size(), selected.getHost(), selected.getPort());
                
                return Optional.of(selected);
            };
        }
        
        /**
         * Estratégia First Available
         */
        public static LoadBalancingStrategy firstAvailable() {
            return instances -> {
                Optional<ServiceInstance> first = instances.stream().findFirst();
                first.ifPresent(instance -> 
                    log.debug("First available selected: {}:{}", instance.getHost(), instance.getPort()));
                return first;
            };
        }
        
        /**
         * Estratégia baseada em peso (usando metadata 'weight')
         */
        public static LoadBalancingStrategy weighted() {
            return instances -> {
                if (instances.isEmpty()) {
                    return Optional.empty();
                }
                
                // Calcula peso total
                int totalWeight = instances.stream()
                        .mapToInt(instance -> getWeight(instance))
                        .sum();
                
                if (totalWeight <= 0) {
                    // Se não há pesos, usa random
                    return random().apply(instances);
                }
                
                // Seleciona baseado no peso
                int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
                int currentWeight = 0;
                
                for (ServiceInstance instance : instances) {
                    currentWeight += getWeight(instance);
                    if (randomWeight < currentWeight) {
                        log.debug("Weighted selected instance: {}:{} (weight: {})", 
                                 instance.getHost(), instance.getPort(), getWeight(instance));
                        return Optional.of(instance);
                    }
                }
                
                // Fallback para o último
                return Optional.of(instances.get(instances.size() - 1));
            };
        }
        
        /**
         * Estratégia baseada em saúde (healthiest first)
         */
        public static LoadBalancingStrategy healthiest() {
            return instances -> {
                if (instances.isEmpty()) {
                    return Optional.empty();
                }
                
                // Por enquanto, seleciona o primeiro
                // Em uma implementação real, consultaria health checks
                return instances.stream()
                        .filter(FunctionalLoadBalancer::isHealthy)
                        .findFirst()
                        .or(() -> instances.stream().findFirst());
            };
        }
        
        /**
         * Estratégia que combina multiple critérios
         */
        public static LoadBalancingStrategy composite(LoadBalancingStrategy... strategies) {
            return instances -> {
                for (LoadBalancingStrategy strategy : strategies) {
                    Optional<ServiceInstance> result = strategy.apply(instances);
                    if (result.isPresent()) {
                        return result;
                    }
                }
                return Optional.empty();
            };
        }
        
        private static int getWeight(ServiceInstance instance) {
            try {
                String weightStr = instance.getMetadata().get("weight");
                return weightStr != null ? Integer.parseInt(weightStr) : 1;
            } catch (NumberFormatException e) {
                return 1; // Peso padrão
            }
        }
        
        private Strategies() {
            // Utility class
        }
    }
    
    private final LoadBalancingStrategy strategy;
    
    public FunctionalLoadBalancer(LoadBalancingStrategy strategy) {
        this.strategy = strategy;
    }
    
    /**
     * Cria load balancer com estratégia round-robin
     */
    public static FunctionalLoadBalancer roundRobin() {
        return new FunctionalLoadBalancer(Strategies.roundRobin());
    }
    
    /**
     * Cria load balancer com estratégia random
     */
    public static FunctionalLoadBalancer random() {
        return new FunctionalLoadBalancer(Strategies.random());
    }
    
    /**
     * Cria load balancer com estratégia weighted
     */
    public static FunctionalLoadBalancer weighted() {
        return new FunctionalLoadBalancer(Strategies.weighted());
    }
    
    /**
     * Cria load balancer com estratégia customizada
     */
    public static FunctionalLoadBalancer custom(LoadBalancingStrategy strategy) {
        return new FunctionalLoadBalancer(strategy);
    }
    
    /**
     * Seleciona uma instância da lista usando a estratégia configurada
     */
    public Optional<ServiceInstance> selectInstance(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            log.warn("No instances available for load balancing");
            return Optional.empty();
        }
        
        // Filtra apenas instâncias saudáveis
        List<ServiceInstance> healthyInstances = instances.stream()
                .filter(FunctionalLoadBalancer::isHealthy)
                .toList();
        
        if (healthyInstances.isEmpty()) {
            log.warn("No healthy instances available, falling back to all instances");
            healthyInstances = instances;
        }
        
        return strategy.apply(healthyInstances);
    }
    
    /**
     * Verifica se uma instância está saudável
     * Em uma implementação real, isso consultaria health checks reais
     */
    private static boolean isHealthy(ServiceInstance instance) {
        // Por enquanto, assume que todas as instâncias registradas estão saudáveis
        // Em produção, isso deveria verificar endpoints de health check
        return true;
    }
    
    /**
     * Builder para configuração avançada
     */
    public static class Builder {
        private LoadBalancingStrategy strategy = Strategies.roundRobin();
        
        public Builder strategy(LoadBalancingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public Builder roundRobin() {
            this.strategy = Strategies.roundRobin();
            return this;
        }
        
        public Builder random() {
            this.strategy = Strategies.random();
            return this;
        }
        
        public Builder weighted() {
            this.strategy = Strategies.weighted();
            return this;
        }
        
        public Builder composite(LoadBalancingStrategy... strategies) {
            this.strategy = Strategies.composite(strategies);
            return this;
        }
        
        public FunctionalLoadBalancer build() {
            return new FunctionalLoadBalancer(strategy);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}