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
package com.codingas.gateway.web.api.assembler;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelManager;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.web.api.dto.ChannelEndpointResponse;
import com.codingas.gateway.web.api.dto.ChannelRequest;
import com.codingas.gateway.web.api.dto.ChannelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 渠道应用门面（管理服务层）
 *
 * <p>位于 Controller 与领域 Service 之间：负责组装对象（DTO 纯映射 + 跨实体展示数据）与跨域访问（协调核心服务），
 * 只依赖核心 Service，不访问持久化仓储。</p>
 */
@Component
@RequiredArgsConstructor
public class ChannelFacade {

    private final ChannelManager channelManager;

    /**
     * 创建渠道
     *
     * @param request 创建请求 DTO
     * @return 渠道响应 DTO
     */
    public ChannelResponse create(ChannelRequest request) {
        return toResponse(channelManager.create(request.toEntity()));
    }

    /**
     * 更新渠道
     *
     * @param id      渠道 ID
     * @param request 更新请求 DTO
     * @return 渠道响应 DTO
     */
    public ChannelResponse update(Long id, ChannelRequest request) {
        return toResponse(channelManager.update(id, request.toEntity()));
    }

    /**
     * 按 ID 获取渠道
     *
     * @param id 渠道 ID
     * @return 渠道响应 DTO
     */
    public ChannelResponse getById(Long id) {
        return toResponse(channelManager.getById(id));
    }

    /**
     * 查询渠道列表（按提供商/计费模式过滤）
     *
     * @param providerId  提供商 ID（可选）
     * @param billingMode 计费模式编码（可选）
     * @return 渠道响应 DTO 列表
     */
    public List<ChannelResponse> list(Long providerId, String billingMode) {
        List<Channel> channels;
        if (providerId == null) {
            channels = channelManager.getAll();
        } else if (billingMode != null) {
            channels = channelManager.getByProviderIdAndBillingMode(providerId, BillingMode.fromCode(billingMode));
        } else {
            channels = channelManager.getByProviderId(providerId);
        }
        return channels.stream().map(this::toResponse).toList();
    }

    /**
     * 组装渠道响应 DTO（纯映射 + 经核心 Service 获取的跨实体展示字段）
     */
    private ChannelResponse toResponse(Channel channel) {
        ChannelResponse response = ChannelResponse.from(channel);
        // 提供商名称（经核心 Service 查询，仅展示用）
        Provider provider = channelManager.getProvider(channel.getProviderId());
        if (provider != null) {
            response.setProviderName(provider.getName());
        }
        // 端点列表（经核心 Service 查询）
        response.setEndpoints(
            channelManager.getEndpoints(channel.getId()).stream()
                .map(ChannelEndpointResponse::from)
                .toList()
        );
        return response;
    }
}
