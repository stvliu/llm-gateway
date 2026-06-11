package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 健康路由器 — 过滤熔断中的端点
 */
@Component
@Order(300)
@RequiredArgsConstructor
public class HealthRouter implements Router {

    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        return instances.stream()
                .filter(mi -> circuitBreakerManager.isAvailable(mi.getChannelId()))
                .toList();
    }

    @Override
    public boolean isForce() { return true; }
}
