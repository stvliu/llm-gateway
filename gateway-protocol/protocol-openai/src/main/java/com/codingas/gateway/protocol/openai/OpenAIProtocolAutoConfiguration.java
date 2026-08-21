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
package com.codingas.gateway.protocol.openai;

import com.codingas.gateway.api.capability.protocol.ProtocolAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * OpenAI 协议能力插件自动装配。
 *
 * <p>注册 {@link OpenAIProtocolAdapter} 为 {@link ProtocolAdapter} Bean。
 * 通过 {@code gateway.protocol.openai.enabled=true}（默认启用）控制插件开关，
 * 未启用时该协议插件不参与装配。</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gateway.protocol.openai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenAIProtocolAutoConfiguration {

    /**
     * OpenAI 协议适配器
     */
    @Bean
    public OpenAIProtocolAdapter openAIProtocolAdapter(ObjectMapper objectMapper) {
        return new OpenAIProtocolAdapter(objectMapper);
    }
}
