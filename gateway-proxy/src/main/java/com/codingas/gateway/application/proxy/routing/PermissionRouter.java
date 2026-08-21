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

import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.channel.ChannelGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限路由器 — 按应用-渠道授权（ApplicationChannel）过滤模型实例
 *
 * <p>数据面权限锚点为 {@link RoutingRequest#getApplicationId()}：
 * 通过 {@link ApplicationChannelGateway#findChannelIdsByApplicationId(Long)} 查询应用可见的渠道集合，
 * 仅保留该集合内的实例，再过滤出活跃（{@code state.isRoutable()}）渠道。</p>
 *
 * <p>D9 约束：ADMIN 退管理面，数据面权限路由无特权旁路 —— 任何角色都按应用授权过滤，
 * 不再保留 ADMIN 跳过分支。{@code applicationId} 为 null（无权限锚点）时直接返回空集。</p>
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class PermissionRouter implements Router {

    private final ChannelGateway channelGateway;
    private final ApplicationChannelGateway applicationChannelGateway;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        // 获取应用可见的渠道 ID 集合（权限锚点）
        Set<Long> permittedChannelIds = getPermittedChannelIds(request);

        if (permittedChannelIds.isEmpty()) {
            return List.of();
        }

        // 过滤：只保留应用授权渠道内的实例
        List<ModelInstance> permitted = instances.stream()
                .filter(mi -> permittedChannelIds.contains(mi.getChannelId()))
                .toList();

        if (permitted.isEmpty()) {
            return List.of();
        }

        // 再过滤活跃 Channel（state.isRoutable()）
        List<Long> channelIds = permitted.stream().map(ModelInstance::getChannelId).toList();
        List<Channel> activeChannels = channelGateway.findByIds(channelIds).stream()
                .filter(ch -> ch.getState() != null && ch.getState().isRoutable())
                .toList();
        Set<Long> activeChannelIds = activeChannels.stream().map(Channel::getId).collect(Collectors.toSet());

        return permitted.stream()
                .filter(mi -> activeChannelIds.contains(mi.getChannelId()))
                .toList();
    }

    @Override
    public boolean isForce() { return true; }

    /**
     * 计算应用可见的渠道 ID 集合
     *
     * <p>无权限锚点（applicationId 为 null）时返回空集；
     * 否则查询应用-渠道授权关联。ADMIN 角色不再跳过此过滤。</p>
     *
     * @param request 路由请求上下文
     * @return 应用可见的渠道 ID 集合
     */
    private Set<Long> getPermittedChannelIds(RoutingRequest request) {
        Long applicationId = request.getApplicationId();
        if (applicationId == null) {
            // 无权限锚点：不允许访问任何渠道
            return Set.of();
        }
        return applicationChannelGateway.findChannelIdsByApplicationId(applicationId);
    }
}
