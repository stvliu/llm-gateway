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

import com.codingas.gateway.iam.auth.AuthorizationService;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.model.ModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link PermissionRouter} 权限路由测试（授权查询委托统一门面）
 *
 * <p>验证数据面权限路由经 {@link AuthorizationService#permittedChannelIds(Long)}
 * 获取应用可见渠道，且 ADMIN 角色不再跳过过滤（D9：ADMIN 退管理面，数据面无特权旁路）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionRouter（数据面权限路由）测试")
class PermissionRouterTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private AuthorizationService authorizationService;

    private PermissionRouter router;

    @BeforeEach
    void setUp() {
        router = new PermissionRouter(channelRepository, authorizationService);
    }

    private ModelInstance instance(long id, long channelId) {
        ModelInstance mi = new ModelInstance();
        mi.setId(id);
        mi.setChannelId(channelId);
        return mi;
    }

    private Channel activeChannel(long id) {
        Channel ch = new Channel();
        ch.setId(id);
        ch.setState(ChannelState.ACTIVE);
        return ch;
    }

    private RoutingRequest request(Long applicationId) {
        return new RoutingRequest(1L, applicationId, 1L, "USER", RoutingStrategy.WEIGHTED);
    }

    @Test
    @DisplayName("应用可见渠道内的实例通过过滤")
    void filter_permittedChannelInstances_passed() {
        ModelInstance mi1 = instance(1L, 10L);
        ModelInstance mi2 = instance(2L, 20L);
        when(authorizationService.permittedChannelIds(100L)).thenReturn(Set.of(10L, 20L));
        when(channelRepository.findByIds(List.of(10L, 20L)))
                .thenReturn(List.of(activeChannel(10L), activeChannel(20L)));

        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request(100L));

        assertThat(result).containsExactly(mi1, mi2);
    }

    @Test
    @DisplayName("非授权渠道实例被过滤")
    void filter_unauthorizedChannelInstance_filtered() {
        ModelInstance mi1 = instance(1L, 10L);
        ModelInstance mi2 = instance(2L, 30L);
        when(authorizationService.permittedChannelIds(100L)).thenReturn(Set.of(10L));
        when(channelRepository.findByIds(List.of(10L))).thenReturn(List.of(activeChannel(10L)));

        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request(100L));

        assertThat(result).containsExactly(mi1);
    }

    @Test
    @DisplayName("无权限锚点（applicationId null）返回空集，不查门面")
    void filter_nullApplicationId_empty() {
        assertThat(router.filter(List.of(instance(1L, 10L)), request(null))).isEmpty();
        verifyNoInteractions(authorizationService);
    }

    @Test
    @DisplayName("非活跃渠道的实例被过滤")
    void filter_inactiveChannelInstances_filtered() {
        ModelInstance mi1 = instance(1L, 10L);
        when(authorizationService.permittedChannelIds(100L)).thenReturn(Set.of(10L));
        Channel inactive = new Channel();
        inactive.setId(10L);
        inactive.setState(ChannelState.SUSPENDED);
        when(channelRepository.findByIds(List.of(10L))).thenReturn(List.of(inactive));

        assertThat(router.filter(List.of(mi1), request(100L))).isEmpty();
    }

    @Test
    @DisplayName("ADMIN 角色不跳过过滤：仍按统一门面授权过滤（D9 数据面无特权旁路）")
    void admin_doesNotSkip() {
        // role=ADMIN 且 applicationId=1：仍走统一门面授权过滤，授权集 {10} → 仅 10 保留
        ModelInstance mi1 = instance(1L, 10L);
        ModelInstance mi2 = instance(2L, 20L);

        when(authorizationService.permittedChannelIds(1L)).thenReturn(Set.of(10L));
        when(channelRepository.findByIds(List.of(10L))).thenReturn(List.of(activeChannel(10L)));

        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "ADMIN", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        // 关键断言：ADMIN 角色仍调用统一授权门面（无旁路）
        verify(authorizationService).permittedChannelIds(1L);
    }

    @Test
    @DisplayName("isForce 返回 true")
    void isForce_returnsTrue() {
        assertThat(router.isForce()).isTrue();
    }
}
