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

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.transport.ErrorClassificationStrategy;
import com.codingas.gateway.protocol.transport.UpstreamClientFactory;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

/**
 * Anthropic 上游客户端工厂（协议插件自包含：格式转换 + 传输调用）
 */
public class AnthropicUpstreamClientFactory implements UpstreamClientFactory {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ErrorClassificationStrategy classifier;

    public AnthropicUpstreamClientFactory(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.classifier = new AnthropicErrorClassifier();
    }

    @Override
    public String supportedProtocol() {
        return "anthropic";
    }

    @Override
    public UpstreamClient<? extends ProtocolRequest> create(String endpointUrl, String apiKey, int timeoutSeconds) {
        return new AnthropicUpstreamClient(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper, classifier);
    }
}
