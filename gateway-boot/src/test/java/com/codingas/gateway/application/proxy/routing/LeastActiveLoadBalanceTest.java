package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.infrastructure.resilience.EndpointMetrics;
import com.codingas.gateway.infrastructure.resilience.EndpointMetricsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * LeastActiveLoadBalance 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeastActiveLoadBalance 单元测试")
class LeastActiveLoadBalanceTest {

    @Mock
    private EndpointMetricsRegistry metricsRegistry;

    private LeastActiveLoadBalance loadBalance;

    @BeforeEach
    void setUp() {
        loadBalance = new LeastActiveLoadBalance(metricsRegistry);
    }

    @Test
    @DisplayName("选活跃数最少的实例")
    void selectsLeastActive() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        mi1.setWeight(100);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);
        mi2.setWeight(100);

        EndpointMetrics metrics1 = new EndpointMetrics();
        metrics1.beginCall(); metrics1.endCall(100, true); // active=0
        EndpointMetrics metrics2 = new EndpointMetrics();
        metrics2.beginCall(); // active=1

        when(metricsRegistry.get(100L)).thenReturn(metrics1);
        when(metricsRegistry.get(200L)).thenReturn(metrics2);

        ModelInstance result = loadBalance.select(List.of(mi1, mi2));

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("同活跃度内按权重选择")
    void sameActive_weightedSelection() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        mi1.setWeight(100);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);
        mi2.setWeight(100);

        EndpointMetrics metrics1 = new EndpointMetrics();
        EndpointMetrics metrics2 = new EndpointMetrics();

        when(metricsRegistry.get(100L)).thenReturn(metrics1);
        when(metricsRegistry.get(200L)).thenReturn(metrics2);

        // 同活跃度（都是 0），不应抛异常
        ModelInstance result = loadBalance.select(List.of(mi1, mi2));
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("单元素列表直接返回")
    void singleInstance_returnsDirectly() {
        ModelInstance mi = new ModelInstance();
        mi.setId(1L);

        ModelInstance result = loadBalance.select(List.of(mi));

        assertThat(result).isSameAs(mi);
    }

    @Test
    @DisplayName("null 或空列表返回 null")
    void nullOrEmpty_returnsNull() {
        assertThat(loadBalance.select(null)).isNull();
        assertThat(loadBalance.select(List.of())).isNull();
    }
}
