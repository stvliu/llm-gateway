package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.infrastructure.resilience.CircuitBreaker;
import com.codingas.gateway.infrastructure.resilience.CircuitBreakerState;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ClusterHealthAggregator 单元测试
 *
 * <p>验证域级健康聚合规则：域内全部端点熔断（OPEN）→ DOWN；
 * 全部正常（CLOSED）→ HEALTHY；部分熔断 → DEGRADED；
 * 任一端点处于 HALF_OPEN（试探放行）→ 视为解除 DOWN（不返回 DOWN）。</p>
 *
 * <p>聚合器通过 {@link CircuitBreaker#getState()} 只读查询熔断状态，
 * 不调用 {@code isAvailable}（避免 OPEN→HALF_OPEN 的状态迁移副作用）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterHealthAggregator 单元测试")
class ClusterHealthAggregatorTest {

    @Mock
    private ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    @InjectMocks
    private ClusterHealthAggregator aggregator;

    @Test
    @DisplayName("域内全部端点 CLOSED → HEALTHY")
    void allClosed_returnsHealthy() {
        stubBreakers(List.of(50L, 60L), CircuitBreakerState.CLOSED, CircuitBreakerState.CLOSED);

        assertThat(aggregator.aggregate(List.of(50L, 60L)))
                .as("域内全部端点健康，聚合为 HEALTHY");
    }

    @Test
    @DisplayName("域内全部端点 OPEN → DOWN")
    void allOpen_returnsDown() {
        stubBreakers(List.of(50L, 60L), CircuitBreakerState.OPEN, CircuitBreakerState.OPEN);

        assertThat(aggregator.aggregate(List.of(50L, 60L)))
                .as("域内全部端点熔断，聚合为 DOWN");
    }

    @Test
    @DisplayName("域内部分端点 OPEN → DEGRADED")
    void partialOpen_returnsDegraded() {
        stubBreakers(List.of(50L, 60L), CircuitBreakerState.OPEN, CircuitBreakerState.CLOSED);

        assertThat(aggregator.aggregate(List.of(50L, 60L)))
                .as("域内部分端点熔断，聚合为 DEGRADED");
    }

    @Test
    @DisplayName("域内任一端点 HALF_OPEN → 解除 DOWN（不为 DOWN）")
    void anyHalfOpen_notDown() {
        // 一个 OPEN、一个 HALF_OPEN：任一 half-open 成功探测 → 域不应判定为 DOWN
        stubBreakers(List.of(50L, 60L), CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN);

        assertThat(aggregator.aggregate(List.of(50L, 60L)))
                .as("任一端点 HALF_OPEN 表示正在试探恢复，域不应判定为 DOWN")
                .isNotEqualTo(com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus.DOWN);
    }

    @Test
    @DisplayName("单个端点 OPEN → DOWN")
    void singleOpen_returnsDown() {
        stubBreakers(List.of(50L), CircuitBreakerState.OPEN);

        assertThat(aggregator.aggregate(List.of(50L)))
                .as("单端点域全熔断 → DOWN");
    }

    @Test
    @DisplayName("空端点列表 → HEALTHY（无故障证据，保守判健康避免误杀空域）")
    void emptyEndpoints_returnsHealthy() {
        assertThat(aggregator.aggregate(List.of()))
                .as("空域无故障证据，保守判 HEALTHY");
    }

    /** 批量 stub 熔断器状态：按 endpointId 顺序对应给定状态 */
    private void stubBreakers(List<Long> endpointIds, CircuitBreakerState... states) {
        for (int i = 0; i < endpointIds.size(); i++) {
            Long endpointId = endpointIds.get(i);
            CircuitBreaker breaker = mockBreaker(states[i]);
            when(circuitBreakerManager.getBreaker(endpointId)).thenReturn(breaker);
        }
    }

    /** 构造指定状态的熔断器桩 */
    private CircuitBreaker mockBreaker(CircuitBreakerState state) {
        CircuitBreaker breaker = org.mockito.Mockito.mock(CircuitBreaker.class);
        when(breaker.getState()).thenReturn(state);
        return breaker;
    }
}
