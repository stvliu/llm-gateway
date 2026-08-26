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
import com.codingas.gateway.provider.channel.ChannelHealthSource;
import com.codingas.gateway.provider.channel.ChannelHealthStatus;
import com.codingas.gateway.provider.channel.ChannelView;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 渠道响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(ChannelView)} 从核心组装好的渠道视图对象纯映射生成
 * （提供商名称与端点列表已由核心 Service 组装，转换层不依赖持久化仓储）。</p>
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
     * 从渠道视图对象纯映射转换
     *
     * @param view 渠道视图对象（含渠道实体、提供商名称与端点列表）
     * @return 渠道响应 DTO
     */
    public static ChannelResponse from(ChannelView view) {
        Channel channel = view.getChannel();
        ChannelResponse response = new ChannelResponse();
        response.setId(channel.getId());
        response.setProviderId(channel.getProviderId());
        response.setProviderName(view.getProviderName());
        response.setName(channel.getName());
        response.setBillingMode(channel.getBillingMode().name().toLowerCase());
        response.setQuotaLimit(channel.getQuotaLimit());
        response.setTimeout(channel.getTimeout());
        response.setMaxRetries(channel.getMaxRetries());
        response.setState(channel.getState().name());
        response.setEndpoints(
            view.getEndpoints().stream()
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
     * 从渠道视图对象列表纯映射转换
     *
     * @param views 渠道视图对象列表
     * @return 渠道响应 DTO 列表
     */
    public static List<ChannelResponse> from(List<ChannelView> views) {
        return views.stream().map(ChannelResponse::from).toList();
    }
}
