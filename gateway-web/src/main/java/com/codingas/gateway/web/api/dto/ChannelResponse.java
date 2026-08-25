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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelEndpointRepository;
import com.codingas.gateway.provider.channel.ChannelHealthSource;
import com.codingas.gateway.provider.channel.ChannelHealthStatus;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 渠道响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(Channel, ProviderRepository, ChannelEndpointRepository)} 从 {@code Channel}
 * 实体展开提供商名称与端点列表后生成。</p>
 */
@Data
public class ChannelResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private String name;
    private String billingMode;
    private Long quotaLimit;
    private Integer timeout;
    private Integer maxRetries;
    private String state;
    private List<ChannelEndpointResponse> endpoints;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastHealthCheckAt;
    private ChannelHealthStatus lastHealthStatus;
    private ChannelHealthSource lastHealthSource;

    /**
     * 从渠道实体转换（展开提供商名称与端点列表）
     *
     * @param channel            渠道实体
     * @param providerRepository 提供商仓储（查名称，仅展示用）
     * @param endpointRepository 端点仓储（查端点列表）
     * @return 渠道响应 DTO
     */
    public static ChannelResponse from(Channel channel,
                                       ProviderRepository providerRepository,
                                       ChannelEndpointRepository endpointRepository) {
        ChannelResponse response = new ChannelResponse();
        response.setId(channel.getId());
        response.setProviderId(channel.getProviderId());
        // 从 Provider 查找名称（仅展示用）
        providerRepository.findById(channel.getProviderId())
            .ifPresent(p -> response.setProviderName(p.getName()));
        response.setName(channel.getName());
        response.setBillingMode(channel.getBillingMode().name().toLowerCase());
        response.setQuotaLimit(channel.getQuotaLimit());
        response.setTimeout(channel.getTimeout());
        response.setMaxRetries(channel.getMaxRetries());
        response.setState(channel.getState().name());
        // 查询端点列表
        response.setEndpoints(
            endpointRepository.findByChannelId(channel.getId()).stream()
                .map(ChannelEndpointResponse::from)
                .toList()
        );

        response.setCreatedAt(channel.getCreatedAt());
        response.setUpdatedAt(channel.getUpdatedAt());
        // 健康状态字段透传（last-write-wins，未测试过时为 null）
        response.setLastHealthCheckAt(channel.getLastHealthCheckAt());
        response.setLastHealthStatus(channel.getLastHealthStatus());
        response.setLastHealthSource(channel.getLastHealthSource());
        return response;
    }

    /**
     * 从渠道实体列表转换
     *
     * @param channels           渠道实体列表
     * @param providerRepository 提供商仓储（查名称，仅展示用）
     * @param endpointRepository 端点仓储（查端点列表）
     * @return 渠道响应 DTO 列表
     */
    public static List<ChannelResponse> from(List<Channel> channels,
                                             ProviderRepository providerRepository,
                                             ChannelEndpointRepository endpointRepository) {
        return channels.stream()
                .map(c -> from(c, providerRepository, endpointRepository))
                .toList();
    }
}
