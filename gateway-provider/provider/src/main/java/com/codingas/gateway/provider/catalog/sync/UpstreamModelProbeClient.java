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
package com.codingas.gateway.provider.catalog.sync;

import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * 上游模型列表探测客户端
 *
 * <p>按渠道协议类型调用上游模型列表 API（OpenAI 兼容 / Anthropic / Gemini），
 * 归一化为模型 ID 集合返回，供 {@link CatalogProbeService} 对比本地下线情况。</p>
 */
@Component
@RequiredArgsConstructor
public class UpstreamModelProbeClient {

    private final HttpClient httpClient;

    /**
     * 拉取上游可用模型 ID 集合
     *
     * @param endpoint 渠道端点（protocol 决定列表 API 与响应格式）
     * @param apiKey   渠道凭证明文
     * @return 模型 ID 集合（OpenAI/Anthropic 取 data[].id；Gemini 取 models[].name）
     * @throws CatalogSyncException 请求失败或响应解析失败
     */
    public Set<String> fetchModelIds(ChannelEndpoint endpoint, String apiKey) {
        String url = endpoint.getEndpointUrl() + "/v1/models";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // 若被中断则恢复中断标志，避免异常传播吞掉中断状态（调用方依赖该标志判断退出）
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CatalogSyncException("上游模型列表请求失败: " + url + " - " + e.getMessage());
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new CatalogSyncException("上游模型列表请求失败, status=" + response.statusCode());
        }
        return parse(response.body(), endpoint.getProtocol());
    }

    /** 按协议解析响应：OpenAI/Anthropic 取 data[].id，Gemini 取 models[].name */
    private Set<String> parse(String body, Protocol protocol) {
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            Set<String> ids = new HashSet<>();
            if (protocol == Protocol.GEMINI) {
                for (JsonNode node : root.path("models")) {
                    ids.add(node.path("name").asText());
                }
            } else {
                for (JsonNode node : root.path("data")) {
                    ids.add(node.path("id").asText());
                }
            }
            return ids;
        } catch (Exception e) {
            throw new CatalogSyncException("上游模型列表响应解析失败: " + e.getMessage());
        }
    }
}
