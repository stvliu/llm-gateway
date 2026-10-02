/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.iam.application.ApplicationChannel;
import com.codingas.gateway.iam.application.ApplicationChannelRepository;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.protocol.Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InstanceSelector 单元测试
 *
 * <p>验证 {@link InstanceSelector#select} 数据面「认证即授权」语义：读取应用渠道配置
 * （配置而非授权）、过滤活跃渠道（ABAC 属性）、带条件查询实例（DB 层过滤），再经
 * {@link RouterChain} 纯路由（优先级/健康）返回候选 {@link ModelInstance} 列表。
 * 授权相关断言已从 PermissionRouterTest 迁移至此（PermissionRouter 已删除）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstanceSelector 单元测试")
class InstanceSelectorTest {

    @Mock
    private ModelInstanceRepository modelInstanceRepository;

    @Mock
    private RouterChain routerChain;

    @Mock
    private ApplicationChannelRepository applicationChannelRepository;

    @Mock
    private ChannelRepository channelRepository;

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
        when(applicationChannelRepository.findChannelIdsByApplicationId(7L))
                .thenReturn(Set.of(100L, 200L, 300L));
        when(channelRepository.findByIds(anyList()))
                .thenReturn(List.of(activeChannel(100L), activeChannel(200L), activeChannel(300L)));
        when(modelInstanceRepository.findActiveByModelIdAndChannelIds(1L, Set.of(100L, 200L, 300L)))
                .thenReturn(List.of(mi1, mi2, mi3));
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
    @DisplayName("select：按应用渠道配置过滤实例（认证即授权，配置读取前移）")
    void select_filtersByApplicationChannels() {
        // given — 应用 100 配置渠道 {1}，渠道 1 活跃，实例查询带条件返回 mi1
        ModelInstance mi1 = buildInstance(10L, 1L, 1);
        when(applicationChannelRepository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L));
        when(channelRepository.findByIds(List.of(1L))).thenReturn(List.of(activeChannel(1L)));
        when(modelInstanceRepository.findActiveByModelIdAndChannelIds(10L, Set.of(1L))).thenReturn(List.of(mi1));
        when(routerChain.filter(any(), any(RoutingRequest.class))).thenReturn(List.of(mi1));

        // when
        List<ModelInstance> result = instanceSelector.select(
                10L, 100L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);

        // then — 仅返回配置渠道内的活跃实例（DB 层按渠道集合过滤）
        assertThat(result).containsExactly(mi1);
        verify(modelInstanceRepository).findActiveByModelIdAndChannelIds(10L, Set.of(1L));
    }

    @Test
    @DisplayName("select：应用无渠道配置（空集合）→ ResourceNotFoundException")
    void select_emptyChannelConfig_throws() {
        when(applicationChannelRepository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of());

        assertThatThrownBy(() -> instanceSelector.select(
                10L, 100L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("select：无活跃实例（查询空）→ ResourceNotFoundException")
    void select_noActiveInstances_throws() {
        // given — 渠道配置 {1}，渠道 1 活跃，但按模型+渠道查询实例为空
        when(applicationChannelRepository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L));
        when(channelRepository.findByIds(List.of(1L))).thenReturn(List.of(activeChannel(1L)));
        when(modelInstanceRepository.findActiveByModelIdAndChannelIds(10L, Set.of(1L))).thenReturn(List.of());

        assertThatThrownBy(() -> instanceSelector.select(
                10L, 100L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RouterChain 过滤后无候选时抛出 ResourceNotFoundException")
    void select_filterReturnsEmpty_throws() {
        when(applicationChannelRepository.findChannelIdsByApplicationId(7L)).thenReturn(Set.of(100L));
        when(channelRepository.findByIds(List.of(100L))).thenReturn(List.of(activeChannel(100L)));
        when(modelInstanceRepository.findActiveByModelIdAndChannelIds(1L, Set.of(100L))).thenReturn(List.of(instance));
        when(routerChain.filter(any(), any(RoutingRequest.class))).thenReturn(List.of());

        assertThatThrownBy(() -> instanceSelector.select(
                1L, 7L, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("select 将 applicationId 与 protocol 透传至 RoutingRequest")
    void select_forwardsApplicationIdAndProtocolToRoutingRequest() {
        // given
        when(applicationChannelRepository.findChannelIdsByApplicationId(7L)).thenReturn(Set.of(100L));
        when(channelRepository.findByIds(List.of(100L))).thenReturn(List.of(activeChannel(100L)));
        when(modelInstanceRepository.findActiveByModelIdAndChannelIds(1L, Set.of(100L))).thenReturn(List.of(instance));
        when(routerChain.filter(any(), any(RoutingRequest.class))).thenReturn(List.of(instance));

        // when
        instanceSelector.select(1L, 7L, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI);

        // then — 捕获透传给 RouterChain 的 RoutingRequest，断言 applicationId 与 protocol 已透传
        ArgumentCaptor<RoutingRequest> captor = ArgumentCaptor.forClass(RoutingRequest.class);
        verify(routerChain).filter(any(), captor.capture());
        RoutingRequest captured = captor.getValue();
        assertThat(captured.getApplicationId()).isEqualTo(7L);
        assertThat(captured.getModelId()).isEqualTo(1L);
        assertThat(captured.getProtocol()).isEqualTo(Protocol.OPENAI);
    }

    @Test
    @DisplayName("select 查应用授权渠道 priority 构建 channelPriorityMap 填入 RoutingRequest")
    void select_buildsChannelPriorityMapFromApplicationChannels() {
        // given — 应用 7 授权渠道 100(priority=1)、200(priority=2)、300(priority=null)
        ApplicationChannel rel1 = new ApplicationChannel(7L, 100L);
        rel1.setPriority(1);
        ApplicationChannel rel2 = new ApplicationChannel(7L, 200L);
        rel2.setPriority(2);
        ApplicationChannel rel3 = new ApplicationChannel(7L, 300L);
        when(applicationChannelRepository.findChannelIdsByApplicationId(7L))
                .thenReturn(Set.of(100L, 200L, 300L));
        when(channelRepository.findByIds(anyList()))
                .thenReturn(List.of(activeChannel(100L), activeChannel(200L), activeChannel(300L)));
        when(modelInstanceRepository.findActiveByModelIdAndChannelIds(1L, Set.of(100L, 200L, 300L)))
                .thenReturn(List.of(instance));
        when(applicationChannelRepository.findByApplicationId(7L)).thenReturn(List.of(rel1, rel2, rel3));
        when(routerChain.filter(any(), any(RoutingRequest.class))).thenReturn(List.of(instance));

        // when
        instanceSelector.select(1L, 7L, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI);

        // then — channelPriorityMap 应包含渠道 100->1, 200->2；null priority 不放入映射（PriorityRouter 回退默认值）
        ArgumentCaptor<RoutingRequest> captor = ArgumentCaptor.forClass(RoutingRequest.class);
        verify(routerChain).filter(any(), captor.capture());
        Map<Long, Integer> map = captor.getValue().getChannelPriorityMap();
        assertThat(map).containsEntry(100L, 1).containsEntry(200L, 2);
        assertThat(map).doesNotContainKey(300L);
    }

    @Test
    @DisplayName("applicationId 为 null 时抛 ResourceNotFoundException 且不查询渠道/应用渠道仓储")
    void nullApplicationId_throws_noRepoCall() {
        assertThatThrownBy(() -> instanceSelector.select(
                1L, null, 50L, "user", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                .isInstanceOf(ResourceNotFoundException.class);

        // applicationId 为 null 时配置读取返回空集直接抛异常，不查任何仓储
        verify(applicationChannelRepository, never()).findChannelIdsByApplicationId(any());
        verify(channelRepository, never()).findByIds(anyList());
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

    /** 构造活跃渠道（state=ACTIVE，isRoutable 为 true） */
    private Channel activeChannel(Long id) {
        Channel ch = new Channel();
        ch.setId(id);
        ch.setState(ChannelState.ACTIVE);
        return ch;
    }
}
