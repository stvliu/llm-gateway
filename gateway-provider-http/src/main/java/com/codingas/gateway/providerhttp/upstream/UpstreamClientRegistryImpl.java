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
package com.codingas.gateway.providerhttp.upstream;

import com.codingas.gateway.provider.upstream.UpstreamClient;
import com.codingas.gateway.provider.upstream.UpstreamClientRegistry;
import com.codingas.gateway.providerhttp.upstream.ErrorClassificationStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 上游调用注册表实现
 */
@Component
public class UpstreamClientRegistryImpl implements UpstreamClientRegistry {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, ErrorClassificationStrategy> classifiers;

    public UpstreamClientRegistryImpl(OkHttpClient httpClient, ObjectMapper objectMapper,
                                      List<ErrorClassificationStrategy> classifierList) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.classifiers = classifierList.stream()
                .collect(Collectors.toMap(ErrorClassificationStrategy::supportedProvider, Function.identity()));
    }

    @Override
    public UpstreamClient getClient(String protocol, String endpointUrl, String apiKey, int timeoutSeconds) {
        ErrorClassificationStrategy classifier = classifiers.get(protocol);
        return switch (protocol) {
            case "openai" -> new OpenAIUpstreamClient(httpClient, endpointUrl, apiKey, timeoutSeconds,
                    objectMapper, classifier);
            case "anthropic" -> new AnthropicUpstreamClient(httpClient, endpointUrl, apiKey, timeoutSeconds,
                    objectMapper, classifier);
            default -> throw new IllegalArgumentException("不支持的协议: " + protocol);
        };
    }

    @Override
    public List<String> getSupportedProtocols() {
        return List.of("openai", "anthropic");
    }
}
