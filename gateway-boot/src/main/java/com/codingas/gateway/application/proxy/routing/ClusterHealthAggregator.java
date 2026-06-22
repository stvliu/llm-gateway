package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus;
import com.codingas.gateway.infrastructure.resilience.CircuitBreaker;
import com.codingas.gateway.infrastructure.resilience.CircuitBreakerState;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cluster 域级健康聚合器
 *
 * <p>依据一个故障域内各端点的熔断状态，聚合出域级健康状态（{@link ClusterHealthStatus}）。
 * 聚合规则（与 {@link ClusterHealthStatus} Javadoc 一致）：</p>
 * <ul>
 *   <li>域内全部端点 CLOSED（正常）→ {@link ClusterHealthStatus#HEALTHY}</li>
 *   <li>域内全部端点 OPEN（熔断）→ {@link ClusterHealthStatus#DOWN}（共因故障，整域不可用）</li>
 *   <li>域内部分端点 OPEN → {@link ClusterHealthStatus#DEGRADED}（容量受损但仍可用）</li>
 *   <li>域内任一端点 HALF_OPEN（试探放行）→ 视为正在恢复，<b>不判定为 DOWN</b>
 *       （容灾方案设计.md 第七节恢复机制：域内任一渠道 half-open 成功 → 解除 DOWN）</li>
 * </ul>
 *
 * <p>只读查询：通过 {@link CircuitBreaker#getState()} 读取熔断状态，
 * <b>不调用</b> {@link ChannelEndpointCircuitBreakerManager#isAvailable}——后者会触发
 * OPEN→HALF_OPEN 的状态迁移副作用，聚合判断不应改变熔断器状态。</p>
 *
 * <p>纯计算组件，不持久化 {@code Cluster.healthStatus}。是否落库由上层决定，
 * 路由侧仅需实时聚合判断域是否 DOWN 以决定是否跨域转移。</p>
 */
@Component
@RequiredArgsConstructor
public class ClusterHealthAggregator {

    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    /**
     * 聚合域级健康状态
     *
     * @param endpointIds 域内端点 ID 集合
     * @return 聚合后的域级健康状态；空集合保守返回 HEALTHY（无故障证据，避免误杀空域）
     */
    public ClusterHealthStatus aggregate(List<Long> endpointIds) {
        if (endpointIds == null || endpointIds.isEmpty()) {
            // 无故障证据，保守判健康，避免空域被误判 DOWN 导致路由误杀
            return ClusterHealthStatus.HEALTHY;
        }

        boolean anyHalfOpen = false;
        boolean anyOpen = false;
        boolean anyClosed = false;

        for (Long endpointId : endpointIds) {
            CircuitBreakerState state = circuitBreakerManager.getBreaker(endpointId).getState();
            switch (state) {
                case HALF_OPEN -> anyHalfOpen = true;
                case OPEN -> anyOpen = true;
                case CLOSED -> anyClosed = true;
            }
        }

        // 任一端点 HALF_OPEN：正在试探恢复，域不应判 DOWN（解除 DOWN 语义）
        // 具体取 DEGRADED 或 HEALTHY：若仍有 OPEN 端点则 DEGRADED，否则 HEALTHY
        if (anyHalfOpen) {
            return anyOpen ? ClusterHealthStatus.DEGRADED : ClusterHealthStatus.HEALTHY;
        }

        // 无 HALF_OPEN：全 OPEN → DOWN；全 CLOSED → HEALTHY；混合 → DEGRADED
        if (anyClosed && anyOpen) {
            return ClusterHealthStatus.DEGRADED;
        }
        if (anyOpen) {
            return ClusterHealthStatus.DOWN;
        }
        return ClusterHealthStatus.HEALTHY;
    }
}
