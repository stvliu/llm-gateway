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
package com.codingas.gateway.proxy.experience;

import java.util.List;
import java.util.Map;

/**
 * 模型体验聊天用例入参
 *
 * <p>支持两种模式：使用已保存的渠道配置或临时配置。
 * 字段合法性校验由 web 层 DTO 承担（{@code web.api.dto.ExperienceChatRequest}）。</p>
 *
 * @param model        模型名称
 * @param protocolName 协议名称（如 openai、anthropic）
 * @param messages     消息列表（role/content）
 * @param temperature  温度
 * @param maxTokens    最大 Token 数
 * @param stream       是否流式
 * @param channelId    渠道 ID（使用已保存配置时必填）
 * @param credentialId 凭证 ID（使用已保存配置时可选）
 * @param apiKey       直接传入的 API Key（不使用已保存配置时）
 * @param baseUrl      直接传入的 Base URL（不使用已保存配置时，可选）
 * @param savedConfig 是否使用已保存的渠道配置
 */
public record ExperienceChatParams(
        String model,
        String protocolName,
        List<Map<String, String>> messages,
        Double temperature,
        Integer maxTokens,
        Boolean stream,
        Long channelId,
        Long credentialId,
        String apiKey,
        String baseUrl,
        Boolean savedConfig
) {
    /**
     * 判断是否使用已保存的渠道配置
     */
    public boolean useSavedConfig() {
        return savedConfig != null && savedConfig;
    }

    /**
     * 验证请求是否有效
     */
    public boolean isValid() {
        if (model == null || model.isBlank()) {
            return false;
        }
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        if (useSavedConfig()) {
            return channelId != null;
        } else {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
