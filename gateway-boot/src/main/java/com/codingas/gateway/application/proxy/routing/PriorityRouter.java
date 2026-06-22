package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 优先级路由器 — 按 priority 分组，只保留 priority 最小的组
 *
 * <p>顺序语义：排在 {@link HealthRouter} 之后（@Order 300 > 200），
 * 仅在健康候选（已剔除熔断渠道）中按优先级择优，
 * 避免先选定高优先级渠道、再被健康路由过滤导致链终止返回空。</p>
 */
@Component
@Order(300)
public class PriorityRouter implements Router {

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        if (instances.isEmpty()) {
            return List.of();
        }

        // 找最小 priority
        int minPriority = instances.stream()
                .min(Comparator.comparingInt(mi -> mi.getPriority() != null ? mi.getPriority() : 100))
                .map(mi -> mi.getPriority() != null ? mi.getPriority() : 100)
                .orElse(100);

        // 只保留 priority 最小的组
        return instances.stream()
                .filter(mi -> (mi.getPriority() != null ? mi.getPriority() : 100) == minPriority)
                .toList();
    }

    @Override
    public boolean isForce() { return true; }
}
