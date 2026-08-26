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

/**
 * 渠道凭证更新请求 DTO（HTTP 契约）
 *
 * <p>注意：channelId 和 id 由适配层（Controller）从路径参数中提取并填充，
 * 请求体本身不包含这些字段。</p>
 *
 * @param priority   优先级（可选）
 * @param weight     权重（可选）
 * @param description 描述（可选）
 * @param apiKey     API Key（可选，传值则替换）
 */
public record ChannelCredentialUpdateRequest(
        Integer priority,
        Integer weight,
        String description,
        String apiKey
) {
    /**
     * 转换为凭证实体（null 字段表示不更新）
     *
     * @param channelId 渠道 ID（适配层填充）
     * @param id        凭证 ID（适配层填充）
     * @return 凭证实体
     */
    public ChannelCredential toEntity(Long channelId, Long id) {
        ChannelCredential credential = new ChannelCredential();
        credential.setChannelId(channelId);
        credential.setId(id);
        credential.setWeight(weight);
        credential.setPriority(priority);
        credential.setApiKeyPlain(apiKey);
        return credential;
    }
}
