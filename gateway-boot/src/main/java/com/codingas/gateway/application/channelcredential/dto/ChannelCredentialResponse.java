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
package com.codingas.gateway.application.channelcredential.dto;


import java.time.Instant;

/**
 * 渠道凭证响应（不含明文）
 *
 * @param id 主键
 * @param channelId 渠道 ID
 * @param apiKeyPrefix Key 前缀
 * @param apiKeyPlain 明文 API Key（前端脱敏显示）
 * @param name 密钥名称
 * @param description 描述
 * @param weight 权重
 * @param priority 优先级
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelCredentialResponse(
        Long id,
        Long channelId,
        String apiKeyPrefix,
        String apiKeyPlain,
        String name,
        String description,
        Integer weight,
        Integer priority,
        Instant createdAt,
        Instant updatedAt
) {
}
