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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InstanceSelector 单元测试
 *
 * <p>验证 {@link InstanceSelector#select} 返回候选 {@link ModelInstance} 列表（顺序由
 * {@link PriorityRouter} 保证按 priority 升序），并将 {@code applicationId} 与 {@code protocol}
 * 透传至 {@link RoutingRequest}，供下游 {@code PermissionRouter}/{@code HealthRouter} 使用。</p>
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
        instance = buildInstance(10L, 100L, 1);
    }

    @Test
    @DisplayName("select 返回候选列表（多实例，按 priority 升序透传 RouterChain 结果）")
    void select_returnsCandidateList() {
        // given — 三个实例 priority 分别 1/2/3；PriorityRouter 已按 priority 升序，filter 返回升序列表
        ModelInstance mi1 = buildInstance(11L, 100L, 1);
        ModelInstance mi2 = buildInstance(12L, 200L, 2);
        ModelInstance mi3 = buildInstance(13L, 300L, 3);
        when(modelInstanceGateway.findActiveByModelIdOrderByPriority(1L)).thenReturn(List.of(mi1, mi2, mi3));
        when(routerChain.filter(any(), any(RoutingRequest.class))).thenReturn(List.of(mi1, mi2, mi3));

        // when
        List<ModelInstance> result = instanceSelector.select(
                1L, 7L, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI);

        // then — 返回 List 且顺序与 RouterChain 输出一致（即 priority 升序，由 PriorityRouter 保证）
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ModelInstance::getPriority).containsExactly(1, 2, 3);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(11L, 12L, 13L);
    }

    @Test
    @DisplayName("无活跃实例时抛出 ResourceNotFoundException")
    void select_empty_throws() {
        when(modelInstanceGateway.findActiveByModelIdOrderByPriority(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> instanceSelector.select(
                1L, 7L, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RouterChain 过滤后无候选时抛出 ResourceNotFoundException")
    void select_filterReturnsEmpty_throws() {
        when(modelInstanceGateway.findActiveByModelIdOrderByPriority(1L)).thenReturn(List.of(instance));
        when(routerChain.filter(any(), any(RoutingRequest.class))).thenReturn(List.of());

        assertThatThrownBy(() -> instanceSelector.select(
                1L, 7L, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                .isInstanceOf(ResourceNotFoundException.class);
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
        verify(routerChain).filter(any(), captor.capture());
        RoutingRequest captured = captor.getValue();
        assertThat(captured.getApplicationId()).isEqualTo(7L);
        assertThat(captured.getModelId()).isEqualTo(1L);
        assertThat(captured.getUserId()).isEqualTo(50L);
        assertThat(captured.getRole()).isEqualTo("user");
        assertThat(captured.getProtocol()).isEqualTo(Protocol.OPENAI);
    }

    /** 构造测试用 ModelInstance */
    private ModelInstance buildInstance(Long id, Long channelId, int priority) {
        ModelInstance mi = new ModelInstance();
        mi.setId(id);
        mi.setChannelId(channelId);
        mi.setModelId(1L);
        mi.setPriority(priority);
        return mi;
    }
}
