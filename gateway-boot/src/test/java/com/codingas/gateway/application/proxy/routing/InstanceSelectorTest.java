package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * InstanceSelector 单元测试
 *
 * <p>验证权限锚点 {@code applicationId} 与入站协议 {@code protocol} 从
 * {@link InstanceSelector#select} 透传至 {@link RoutingRequest}，供下游
 * {@code PermissionRouter} 判定可见渠道、{@code HealthRouter} 派生 endpointId。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstanceSelector 单元测试")
class InstanceSelectorTest {

    @Mock
    private ModelInstanceGateway modelInstanceGateway;

    @Mock
    private RouterChain routerChain;

    @InjectMocks
    private InstanceSelector instanceSelector;

    private ModelInstance instance;

    @BeforeEach
    void setUp() {
        instance = new ModelInstance();
        instance.setId(10L);
        instance.setChannelId(100L);
        instance.setModelId(1L);
    }

    @Test
    @DisplayName("select 将 applicationId 与 protocol 透传至 RoutingRequest")
    void select_forwardsApplicationIdAndProtocolToRoutingRequest() {
        // given
        when(modelInstanceGateway.findActiveByModelIdOrderByPriority(1L)).thenReturn(List.of(instance));
        when(routerChain.filter(any(), any(RoutingRequest.class))).thenReturn(List.of(instance));

        // when
        instanceSelector.select(1L, 7L, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI);

        // then — 捕获透传给 RouterChain 的 RoutingRequest，断言 applicationId 与 protocol 已透传
        ArgumentCaptor<RoutingRequest> captor = ArgumentCaptor.forClass(RoutingRequest.class);
        org.mockito.Mockito.verify(routerChain).filter(any(), captor.capture());
        RoutingRequest captured = captor.getValue();
        assertThat(captured.getApplicationId()).isEqualTo(7L);
        assertThat(captured.getModelId()).isEqualTo(1L);
        assertThat(captured.getUserId()).isEqualTo(50L);
        assertThat(captured.getRole()).isEqualTo("user");
        assertThat(captured.getProtocol()).isEqualTo(Protocol.OPENAI);
    }

    @Test
    @DisplayName("无活跃实例时抛出 ResourceNotFoundException")
    void select_noInstances_throws() {
        when(modelInstanceGateway.findActiveByModelIdOrderByPriority(1L)).thenReturn(List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        instanceSelector.select(1L, 7L, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
