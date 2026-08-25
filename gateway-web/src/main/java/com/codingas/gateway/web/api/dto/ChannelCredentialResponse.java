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

import com.codingas.gateway.provider.channel.ChannelCredential;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 渠道凭证响应 DTO（HTTP 契约）
 *
 * <p>兼容原 ChannelCredentialResponse 与 ChannelCredentialDetailResponse（字段一致），
 * 详情端点沿用本 DTO，JSON 契约不变。</p>
 */
@Data
public class ChannelCredentialResponse {
    private Long id;
    private Long channelId;
    private String apiKeyPrefix;
    private String apiKeyPlain;
    private String name;
    private String description;
    private Integer weight;
    private Integer priority;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 从凭证实体转换
     *
     * @param credential 凭证实体
     * @return 凭证响应 DTO
     */
    public static ChannelCredentialResponse from(ChannelCredential credential) {
        ChannelCredentialResponse response = new ChannelCredentialResponse();
        response.setId(credential.getId());
        response.setChannelId(credential.getChannelId());
        response.setApiKeyPrefix(credential.getApiKeyPrefix());
        response.setApiKeyPlain(credential.getApiKeyPlain());
        response.setName(credential.getName());
        response.setDescription(null); // description not in ChannelCredential
        response.setWeight(credential.getWeight());
        response.setPriority(credential.getPriority());
        response.setCreatedAt(credential.getCreatedAt());
        response.setUpdatedAt(credential.getUpdatedAt());
        return response;
    }

    /**
     * 从凭证实体列表转换
     *
     * @param credentials 凭证实体列表
     * @return 凭证响应 DTO 列表
     */
    public static List<ChannelCredentialResponse> from(List<ChannelCredential> credentials) {
        return credentials.stream().map(ChannelCredentialResponse::from).toList();
    }
}
