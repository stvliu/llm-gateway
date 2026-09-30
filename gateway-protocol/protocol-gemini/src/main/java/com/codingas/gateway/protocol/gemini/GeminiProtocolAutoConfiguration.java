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

import com.codingas.gateway.protocol.ProtocolAdapter;
import com.codingas.gateway.protocol.transport.SessionStartHook;
import com.codingas.gateway.protocol.transport.UpstreamClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Gemini 协议能力插件自动装配。
 *
 * <p>注册 {@link GeminiProtocolAdapter} 为 {@link ProtocolAdapter} Bean、
 * {@link GeminiUpstreamClientFactory} 为 {@link UpstreamClientFactory} Bean。
 * 通过 {@code gateway.protocol.gemini.enabled=true}（默认启用）控制插件开关，
 * 未启用时该协议插件不参与装配。</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gateway.protocol.gemini", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GeminiProtocolAutoConfiguration {

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
     * Gemini 协议适配器
     */
    @Bean
    public GeminiProtocolAdapter geminiProtocolAdapter() {
        return new GeminiProtocolAdapter();
    }

    /**
     * Gemini 上游客户端工厂（供协议域注册表收集）
     */
    @Bean
    public GeminiUpstreamClientFactory geminiUpstreamClientFactory(OkHttpClient httpClient, ObjectMapper objectMapper,
                                                                   List<SessionStartHook> sessionStartHooks) {
        return new GeminiUpstreamClientFactory(httpClient, objectMapper, sessionStartHooks);
    }

    /**
     * Gemini 协议出站调谐器（供 OutboundTuner 按 List&lt;ProtocolTuner&gt; 集合收集）
     */
    @Bean
    public GeminiTuner geminiTuner() {
        return new GeminiTuner();
    }
}
