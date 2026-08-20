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
package com.codingas.gateway.protocol.gemini;

import com.codingas.gateway.api.capability.protocol.ProtocolAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Gemini 协议能力插件自动装配（示例）。
 *
 * <p>注册 {@link GeminiProtocolAdapter} 为 {@link ProtocolAdapter} Bean。
 * 通过 {@code gateway.protocol.gemini.enabled=true} 控制插件开关（示例默认启用）。</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gateway.protocol.gemini", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GeminiProtocolAutoConfiguration {

    /**
     * Gemini 协议适配器（示例插件）
     */
    @Bean
    public GeminiProtocolAdapter geminiProtocolAdapter() {
        return new GeminiProtocolAdapter();
    }
}
