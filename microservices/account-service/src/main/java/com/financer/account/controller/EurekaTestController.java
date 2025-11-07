package com.financer.account.controller;

import com.financer.eureka.discovery.ServiceDiscoveryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/eureka")
@RequiredArgsConstructor
@ConditionalOnBean(ServiceDiscoveryUtil.class)
public class EurekaTestController {

    private final ServiceDiscoveryUtil serviceDiscovery;

    @GetMapping("/test")
    public Map<String, Object> testEurekaIntegration() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("availableServices", serviceDiscovery.getAvailableServices());
        result.put("accountServiceInfo", serviceDiscovery.getServiceInfo("ACCOUNT-SERVICE"));
        result.put("apiGatewayAvailable", serviceDiscovery.isServiceAvailable("API-GATEWAY"));
        
        return result;
    }
}