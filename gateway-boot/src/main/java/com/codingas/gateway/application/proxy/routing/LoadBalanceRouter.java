package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 负载均衡路由器 — 链终结者，内部调用 LoadBalance.select() 选一个实例
 *
 * <p>根据 RoutingStrategy 映射到对应的 LoadBalance 实现：</p>
 * <ul>
 *   <li>WEIGHTED → weightedRandomLoadBalance（默认）</li>
 *   <li>RANDOM → weightedRandomLoadBalance</li>
 *   <li>FAILOVER → weightedRandomLoadBalance</li>
 *   <li>COST_OPTIMIZED → weightedRandomLoadBalance</li>
 *   <li>LATENCY_OPTIMIZED → weightedRandomLoadBalance</li>
 * </ul>
 */
@Component
@Order(9999)
public class LoadBalanceRouter implements Router {

    private static final String DEFAULT_LOAD_BALANCE = "weightedRandomLoadBalance";

    private final Map<String, LoadBalance> loadBalanceMap;

    private static final Map<RoutingStrategy, String> STRATEGY_MAPPING = Map.of(
            RoutingStrategy.WEIGHTED, "weightedRandomLoadBalance",
            RoutingStrategy.RANDOM, "weightedRandomLoadBalance",
            RoutingStrategy.FAILOVER, "weightedRandomLoadBalance",
            RoutingStrategy.COST_OPTIMIZED, "weightedRandomLoadBalance",
            RoutingStrategy.LATENCY_OPTIMIZED, "weightedRandomLoadBalance"
    );

    public LoadBalanceRouter(Map<String, LoadBalance> loadBalanceMap) {
        this.loadBalanceMap = loadBalanceMap;
    }

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        if (instances.isEmpty()) {
            return List.of();
        }

        String beanName = STRATEGY_MAPPING.getOrDefault(request.getStrategy(), DEFAULT_LOAD_BALANCE);
        LoadBalance loadBalance = loadBalanceMap.get(beanName);

        if (loadBalance == null) {
            loadBalance = loadBalanceMap.get(DEFAULT_LOAD_BALANCE);
        }

        ModelInstance selected = loadBalance != null ? loadBalance.select(instances) : instances.getFirst();
        return selected != null ? List.of(selected) : List.of();
    }

    @Override
    public boolean isForce() {
        return true;
    }
}
