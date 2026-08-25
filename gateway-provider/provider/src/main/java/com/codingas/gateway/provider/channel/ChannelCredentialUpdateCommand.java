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
 * 渠道凭证更新用例入参
 *
 * <p>仅非 null 字段参与更新；apiKey 非空时替换密钥。</p>
 *
 * @param channelId   渠道 ID
 * @param id          凭证 ID
 * @param priority    优先级（可选）
 * @param weight      权重（可选）
 * @param description 描述（可选）
 * @param apiKey      API Key（可选，传值则替换）
 */
public record ChannelCredentialUpdateCommand(
        Long channelId,
        Long id,
        Integer priority,
        Integer weight,
        String description,
        String apiKey
) {
}
