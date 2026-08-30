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
package com.codingas.gateway.web.api.facade;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelService;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.web.api.dto.ChannelCopyRequest;
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

    private final ChannelService channelService;

    /**
     * 创建渠道
     *
     * @param request 创建请求 DTO
     * @return 渠道响应 DTO
     */
    public ChannelResponse create(ChannelRequest request) {
        return toResponse(channelService.create(request.toEntity()));
    }

    /**
     * 复制渠道（组装响应：本体 + 端点列表 + 提供商名称）
     *
     * @param id      源渠道 ID
     * @param request 复制请求 DTO（name 必填，copyCredentials 控制凭证复制）
     * @return 新渠道响应 DTO
     */
    public ChannelResponse copy(Long id, ChannelCopyRequest request) {
        return toResponse(channelService.copy(id, request.toEntity(), request.isCopyCredentials()));
    }

    /**
     * 更新渠道
     *
     * @param id      渠道 ID
     * @param request 更新请求 DTO
     * @return 渠道响应 DTO
     */
    public ChannelResponse update(Long id, ChannelRequest request) {
        return toResponse(channelService.update(id, request.toEntity()));
    }

    /**
     * 按 ID 获取渠道
     *
     * @param id 渠道 ID
     * @return 渠道响应 DTO
     */
    public ChannelResponse getById(Long id) {
        return toResponse(channelService.getById(id));
    }

    /**
     * 查询渠道列表（按提供商/计费模式过滤，支持字段排序）
     *
     * @param providerId  提供商 ID（可选）
     * @param billingMode 计费模式编码（可选）
     * @param sortBy      排序字段（name/providerId/state/id，默认 name）
     * @param sortOrder   排序方向（ASC/DESC）
     * @return 渠道响应 DTO 列表
     */
    public List<ChannelResponse> list(Long providerId, String billingMode,
                                      String sortBy, String sortOrder) {
        List<Channel> channels;
        if (providerId == null) {
            channels = channelService.getAll(sortBy, sortOrder);
        } else if (billingMode != null) {
            channels = channelService.getByProviderIdAndBillingMode(
                    providerId, BillingMode.fromCode(billingMode), sortBy, sortOrder);
        } else {
            channels = channelService.getByProviderId(providerId, sortBy, sortOrder);
        }
        return channels.stream().map(this::toResponse).toList();
    }

    /**
     * 组装渠道响应 DTO（纯映射 + 经核心 Service 获取的跨实体展示字段）
     */
    private ChannelResponse toResponse(Channel channel) {
        ChannelResponse response = ChannelResponse.from(channel);
        // 提供商名称（经核心 Service 查询，仅展示用）
        Provider provider = channelService.getProvider(channel.getProviderId());
        if (provider != null) {
            response.setProviderName(provider.getName());
        }
        // 端点列表（经核心 Service 查询）
        response.setEndpoints(
            channelService.getEndpoints(channel.getId()).stream()
                .map(ChannelEndpointResponse::from)
                .toList()
        );
        return response;
    }
}
