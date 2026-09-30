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

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.transport.ErrorClassificationStrategy;
import com.codingas.gateway.protocol.transport.SessionStartHook;
import com.codingas.gateway.protocol.transport.UpstreamClientFactory;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

import java.util.List;

/**
 * OpenAI 上游客户端工厂（协议插件自包含：格式转换 + 传输调用）
 */
public class OpenAIUpstreamClientFactory implements UpstreamClientFactory {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ErrorClassificationStrategy classifier;
    private final SessionStartHook sessionStartHook;

    public OpenAIUpstreamClientFactory(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this(httpClient, objectMapper, List.of());
    }

    /**
     * 创建 OpenAI 上游客户端工厂
     *
     * @param sessionStartHooks 出站会话开始钩子集合（Spring 收集全部 Bean），空列表时使用空实现
     */
    public OpenAIUpstreamClientFactory(OkHttpClient httpClient, ObjectMapper objectMapper,
                                       List<SessionStartHook> sessionStartHooks) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.classifier = new OpenAIErrorClassifier();
        this.sessionStartHook = SessionStartHook.composite(sessionStartHooks);
    }

    @Override
    public String supportedProtocol() {
        return "openai";
    }

    @Override
    public UpstreamClient<? extends ProtocolRequest> create(String endpointUrl, String apiKey, int timeoutSeconds) {
        return new OpenAIUpstreamClient(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper, classifier,
                sessionStartHook);
    }
}
