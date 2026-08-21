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
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.iam.application.ApplicationChannelGateway;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.upstream.RoutingStrategy;
import com.codingas.gateway.provider.channel.ChannelGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PermissionRouter 单元测试
 *
 * <p>验证数据面权限路由按应用-渠道授权（ApplicationChannel）过滤，且 ADMIN 角色不再跳过过滤
 * （D9：ADMIN 退管理面，数据面无特权旁路）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionRouter 单元测试")
class PermissionRouterTest {

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private ApplicationChannelGateway applicationChannelGateway;

    @InjectMocks
    private PermissionRouter router;

    @Test
    @DisplayName("应用授权渠道过滤：仅保留 ApplicationChannel 授权集与活跃渠道交集内的实例")
    void normalApplication_filtersByApplicationChannel() {
        // applicationId=1 授权渠道 {100, 200}，候选实例分布在 100 与 300 → 仅 100 保留
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        ModelInstance mi3 = new ModelInstance();
        mi3.setId(3L);
        mi3.setChannelId(300L);

        Channel ch1 = new Channel();
        ch1.setId(100L);
        ch1.setState(ChannelState.ACTIVE);

        when(applicationChannelGateway.findChannelIdsByApplicationId(1L)).thenReturn(Set.of(100L, 200L));
        when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(ch1));

        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi3), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("applicationId 为 null 时无权限锚点，返回空集")
    void noApplication_returnsEmpty() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);

        RoutingRequest request = new RoutingRequest(1L, null, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1), request);

        assertThat(result).isEmpty();
        // 无权限锚点时不应触碰应用-渠道授权网关
        verify(applicationChannelGateway, org.mockito.Mockito.never())
                .findChannelIdsByApplicationId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("ADMIN 角色不跳过过滤：仍按 ApplicationChannel 授权过滤（D9 数据面无特权旁路）")
    void admin_doesNotSkip() {
        // role=ADMIN 且 applicationId=1：仍走 ApplicationChannel 过滤，授权集 {100} → 仅 100 保留
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);

        Channel ch1 = new Channel();
        ch1.setId(100L);
        ch1.setState(ChannelState.ACTIVE);

        when(applicationChannelGateway.findChannelIdsByApplicationId(1L)).thenReturn(Set.of(100L));
        when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(ch1));

        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "ADMIN", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        // 关键断言：ADMIN 角色仍调用应用-渠道授权网关（无旁路）
        verify(applicationChannelGateway).findChannelIdsByApplicationId(1L);
    }

    @Test
    @DisplayName("非活跃渠道实例被过滤掉")
    void inactiveChannel_filtered() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);

        Channel ch1 = new Channel();
        ch1.setId(100L);
        ch1.setState(ChannelState.ACTIVE);
        Channel ch2 = new Channel();
        ch2.setId(200L);
        ch2.setState(ChannelState.SUSPENDED);

        when(applicationChannelGateway.findChannelIdsByApplicationId(1L)).thenReturn(Set.of(100L, 200L));
        when(channelGateway.findByIds(List.of(100L, 200L))).thenReturn(List.of(ch1, ch2));

        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        // ch2 非 routable，mi2 被过滤
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("isForce 返回 true")
    void isForce_returnsTrue() {
        assertThat(router.isForce()).isTrue();
    }
}
