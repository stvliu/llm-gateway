package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
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
 * HealthRouter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthRouter 单元测试")
class HealthRouterTest {

    @Mock
    private ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    @InjectMocks
    private HealthRouter router;

    @Test
    @DisplayName("过滤熔断中的端点")
    void filtersCircuitBreakerOpen() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);

        when(circuitBreakerManager.isAvailable(100L)).thenReturn(true);
        when(circuitBreakerManager.isAvailable(200L)).thenReturn(false);

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("全部熔断时返回空列表")
    void allCircuitBreakerOpen_returnsEmpty() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setChannelId(100L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setChannelId(200L);

        when(circuitBreakerManager.isAvailable(100L)).thenReturn(false);
        when(circuitBreakerManager.isAvailable(200L)).thenReturn(false);

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("空列表返回空")
    void emptyInput_returnsEmpty() {
        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isForce 返回 true")
    void isForce_returnsTrue() {
        assertThat(router.isForce()).isTrue();
    }
}
