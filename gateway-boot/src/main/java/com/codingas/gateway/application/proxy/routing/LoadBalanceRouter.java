package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 负载均衡路由器 — Task 3.1 降级为透传（候选列表产出后不再收敛到单实例）
 *
 * <p>历史职责：作为链终结者调用 LoadBalance.select() 选一个实例。
 * Task 3.1 起 RouterChain 改为产出候选列表供 L1 故障转移逐个尝试，
 * 本路由器 filter 直接透传输入列表，isForce 降为 false。
 * LoadBalance 依赖与策略映射字段保留供向后兼容构造，降级后不再使用。</p>
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
        // Task 3.1 降级：RouterChain 改为产出候选列表，LoadBalanceRouter 不再收敛到单实例，
        // 直接透传候选列表供 L1 故障转移逐个尝试（负载均衡收敛职责已移除）
        return instances;
    }

    @Override
    public boolean isForce() {
        // 降级为非强制：透传语义下不会因空列表终止链（空输入返回空，等价于无候选）
        return false;
    }
}
