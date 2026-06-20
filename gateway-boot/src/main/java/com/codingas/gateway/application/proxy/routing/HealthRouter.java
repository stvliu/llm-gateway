package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 健康路由器 — 过滤熔断中的端点
 *
 * <p>顺序语义：排在 {@link PriorityRouter} 之前（@Order 200 < 300），
 * 先剔除熔断渠道，再由优先级路由在健康候选中择优，
 * 确保高优先级渠道熔断时可回退到次优先级健康渠道。</p>
 */
@Component
@Order(200)
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
