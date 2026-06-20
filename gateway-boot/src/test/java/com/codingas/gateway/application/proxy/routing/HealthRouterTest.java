package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.Protocol;
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
 *
 * <p>验证熔断 key 已统一为 endpointId：HealthRouter 通过 {@link EndpointResolver}
 * 按入站协议从 channelId 派生 endpointId，再查询 {@link ChannelEndpointCircuitBreakerManager}
 * 的端点级熔断状态——与 {@code KeyFailoverInvoker} 共享同一熔断器 bean，确保路由阶段
 * 过滤掉的熔断端点与调用阶段跳过的熔断端点完全一致。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthRouter 单元测试")
class HealthRouterTest {

    @Mock
    private ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    @Mock
    private EndpointResolver endpointResolver;

    @InjectMocks
    private HealthRouter router;

    @Test
    @DisplayName("过滤熔断中的端点（按 endpointId 派生）")
    void filtersCircuitBreakerOpen() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);

        // 按 OPENAI 协议派生 endpointId：channel 100→endpoint 50（健康）；channel 200→endpoint 60（熔断）
        ChannelEndpoint ep1 = endpoint(50L, 100L, Protocol.OPENAI);
        ChannelEndpoint ep2 = endpoint(60L, 200L, Protocol.OPENAI);
        when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(ep1);
        when(endpointResolver.resolve(200L, Protocol.OPENAI)).thenReturn(ep2);
        when(circuitBreakerManager.isAvailable(50L)).thenReturn(true);
        when(circuitBreakerManager.isAvailable(60L)).thenReturn(false);

        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
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

        ChannelEndpoint ep1 = endpoint(50L, 100L, Protocol.OPENAI);
        ChannelEndpoint ep2 = endpoint(60L, 200L, Protocol.OPENAI);
        when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(ep1);
        when(endpointResolver.resolve(200L, Protocol.OPENAI)).thenReturn(ep2);
        when(circuitBreakerManager.isAvailable(50L)).thenReturn(false);
        when(circuitBreakerManager.isAvailable(60L)).thenReturn(false);

        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("空列表返回空")
    void emptyInput_returnsEmpty() {
        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
        List<ModelInstance> result = router.filter(List.of(), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isForce 返回 true")
    void isForce_returnsTrue() {
        assertThat(router.isForce()).isTrue();
    }

    @Test
    @DisplayName("同 channel 两 endpoint：OPENAI 端点熔断时按 endpointId 过滤掉该 channel 的实例")
    void sameChannelTwoEndpoints_openaiBroken_filtersInstance() {
        // 同一 channel 100L 上有两个端点：50L(OPENAI) 熔断、60L(ANTHROPIC) 健康
        ModelInstance mi = new ModelInstance();
        mi.setId(1L);
        mi.setChannelId(100L);

        ChannelEndpoint openaiEp = endpoint(50L, 100L, Protocol.OPENAI);
        when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(openaiEp);
        when(circuitBreakerManager.isAvailable(50L)).thenReturn(false);

        // 入站协议 OPENAI → 派生 endpointId=50L → 熔断 → 实例被过滤
        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
        List<ModelInstance> result = router.filter(List.of(mi), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("同 channel 两 endpoint：ANTHROPIC 端点健康时按 endpointId 保留该 channel 的实例")
    void sameChannelTwoEndpoints_anthropicHealthy_keepsInstance() {
        ModelInstance mi = new ModelInstance();
        mi.setId(1L);
        mi.setChannelId(100L);

        ChannelEndpoint anthropicEp = endpoint(60L, 100L, Protocol.ANTHROPIC);
        when(endpointResolver.resolve(100L, Protocol.ANTHROPIC)).thenReturn(anthropicEp);
        when(circuitBreakerManager.isAvailable(60L)).thenReturn(true);

        // 入站协议 ANTHROPIC → 派生 endpointId=60L → 健康 → 实例保留
        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.ANTHROPIC);
        List<ModelInstance> result = router.filter(List.of(mi), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("endpoint 派生失败（无可用端点）时视为不可用，过滤掉实例")
    void endpointUnresolvable_filtersInstance() {
        ModelInstance mi = new ModelInstance();
        mi.setId(1L);
        mi.setChannelId(100L);

        // EndpointResolver 未找到端点时抛 ResourceNotFoundException
        when(endpointResolver.resolve(100L, Protocol.GEMINI))
                .thenThrow(new ResourceNotFoundException("ChannelEndpoint", 100L));

        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.GEMINI);
        List<ModelInstance> result = router.filter(List.of(mi), request);

        assertThat(result).isEmpty();
    }

    /** 构造测试用 ChannelEndpoint */
    private ChannelEndpoint endpoint(long id, long channelId, Protocol protocol) {
        ChannelEndpoint ep = new ChannelEndpoint();
        ep.setId(id);
        ep.setChannelId(channelId);
        ep.setProtocol(protocol);
        return ep;
    }
}
