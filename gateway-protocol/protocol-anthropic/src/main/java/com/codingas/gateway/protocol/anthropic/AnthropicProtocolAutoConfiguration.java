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
package com.codingas.gateway.protocol.anthropic;

import com.codingas.gateway.protocol.ProtocolAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Anthropic 协议能力插件自动装配。
 *
 * <p>注册 {@link AnthropicProtocolAdapter} 为 {@link ProtocolAdapter} Bean、
 * {@link AnthropicUpstreamClientFactory} 为 {@link com.codingas.gateway.protocol.transport.UpstreamClientFactory} Bean。
 * 通过 {@code gateway.protocol.anthropic.enabled=true}（默认启用）控制插件开关，
 * 未启用时该协议插件不参与装配。</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gateway.protocol.anthropic", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AnthropicProtocolAutoConfiguration {

    /**
     * 上游 HTTP 客户端（插件自包含提供；宿主已提供 OkHttpClient Bean 时复用）
     */
    @Bean
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Anthropic 协议适配器
     */
    @Bean
    public AnthropicProtocolAdapter anthropicProtocolAdapter() {
        return new AnthropicProtocolAdapter();
    }

    /**
     * Anthropic 上游客户端工厂（供协议域注册表收集）
     */
    @Bean
    public AnthropicUpstreamClientFactory anthropicUpstreamClientFactory(OkHttpClient httpClient, ObjectMapper objectMapper) {
        return new AnthropicUpstreamClientFactory(httpClient, objectMapper);
    }

    /**
     * Anthropic 协议校验器（插件自包含注册，无 @Component 防 boot 扫描误装配）
     */
    @Bean
    public AnthropicProtocolValidator anthropicProtocolValidator() {
        return new AnthropicProtocolValidator();
    }

    /**
     * Anthropic 协议出站调谐器（供 OutboundTuner 按 List&lt;ProtocolTuner&gt; 集合收集）
     */
    @Bean
    public AnthropicTuner anthropicTuner() {
        return new AnthropicTuner();
    }
}
