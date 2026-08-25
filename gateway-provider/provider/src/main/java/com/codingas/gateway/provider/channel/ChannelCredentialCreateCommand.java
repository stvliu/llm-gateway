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
package com.codingas.gateway.provider.channel;

/**
 * 渠道凭证创建用例入参
 *
 * @param channelId   渠道 ID
 * @param apiKey      API Key 明文（创建后加密存储）
 * @param priority    优先级（数值越小优先级越高）
 * @param weight      权重（同优先级下按权重分配流量）
 * @param description 描述
 */
public record ChannelCredentialCreateCommand(
        Long channelId,
        String apiKey,
        Integer priority,
        Integer weight,
        String description
) {
}
