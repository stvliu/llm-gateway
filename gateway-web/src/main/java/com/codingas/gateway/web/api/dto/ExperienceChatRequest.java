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

import com.codingas.gateway.proxy.experience.ExperienceChatCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 体验聊天请求 DTO（HTTP 契约）
 *
 * <p>支持两种模式：使用已保存的渠道配置或临时配置。</p>
 */
@Data
public class ExperienceChatRequest {

    @NotBlank(message = "Model is required")
    private String model;

    /** 协议名称（如 openai、anthropic），必填 */
    @NotBlank(message = "Protocol name is required")
    private String protocolName;

    private List<Map<String, String>> messages;

    private Double temperature;

    private Integer maxTokens;

    private Boolean stream;

    /** 渠道 ID（使用已保存配置时必填） */
    private Long channelId;

    /** 凭证 ID（使用已保存配置时可选，默认使用渠道的默认 Key） */
    private Long credentialId;

    /** 直接传入的 API Key（不使用已保存配置时） */
    private String apiKey;

    /** 直接传入的 Base URL（不使用已保存配置时，可选，默认使用协议默认 URL） */
    private String baseUrl;

    /** 是否使用已保存的渠道配置 */
    private Boolean useSavedConfig;

    /**
     * 转换为核心体验聊天用例入参
     *
     * @return 体验聊天用例入参
     */
    public ExperienceChatCommand toCommand() {
        return new ExperienceChatCommand(
                model, protocolName, messages, temperature, maxTokens, stream,
                channelId, credentialId, apiKey, baseUrl, getUseSavedConfig());
    }
}
