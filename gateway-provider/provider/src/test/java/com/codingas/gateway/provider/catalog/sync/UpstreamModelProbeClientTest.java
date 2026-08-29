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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UpstreamModelProbeClient 单元测试
 *
 * <p>Mockito mock {@link HttpClient#send} 返回构造的 {@link HttpResponse}，
 * 验证 OpenAI/Anthropic 与 Gemini 两类响应格式的解析以及非 2xx 抛异常行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class UpstreamModelProbeClientTest {

    @Mock
    private HttpClient httpClient;

    private UpstreamModelProbeClient client;

    @BeforeEach
    void setUp() {
        client = new UpstreamModelProbeClient(httpClient);
    }

    @Test
    @DisplayName("OpenAI 格式响应解析为模型 ID 集合")
    void fetchModelIds_openAiFormat_parsesIds() throws Exception {
        // given：HttpClient mock 返回 200 + {"data":[{"id":"gpt-4"},{"id":"claude-3"}]}
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"data\":[{\"id\":\"gpt-4\"},{\"id\":\"claude-3\"}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        ChannelEndpoint endpoint = newEndpoint("https://api.example.com", Protocol.OPENAI);

        // when
        Set<String> ids = client.fetchModelIds(endpoint, "sk-test");

        // then
        assertThat(ids).containsExactlyInAnyOrder("gpt-4", "claude-3");
    }

    @Test
    @DisplayName("Anthropic 格式响应解析为模型 ID 集合")
    void fetchModelIds_anthropicFormat_parsesIds() throws Exception {
        // given：HttpClient mock 返回 200 + {"data":[{"id":"claude-3-5-sonnet"},{"id":"claude-3-haiku"}]}
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"data\":[{\"id\":\"claude-3-5-sonnet\"},{\"id\":\"claude-3-haiku\"}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        ChannelEndpoint endpoint = newEndpoint("https://api.anthropic.com", Protocol.ANTHROPIC);

        // when
        Set<String> ids = client.fetchModelIds(endpoint, "sk-test");

        // then
        assertThat(ids).containsExactlyInAnyOrder("claude-3-5-sonnet", "claude-3-haiku");
    }

    @Test
    @DisplayName("Gemini 格式响应解析 models[].name")
    void fetchModelIds_geminiFormat_parsesNames() throws Exception {
        // given：HttpClient mock 返回 200 + {"models":[{"name":"models/gemini-pro"},{"name":"models/gemini-1.5"}]}
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"models\":[{\"name\":\"models/gemini-pro\"},{\"name\":\"models/gemini-1.5\"}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        ChannelEndpoint endpoint = newEndpoint("https://generativelanguage.googleapis.com", Protocol.GEMINI);

        // when
        Set<String> ids = client.fetchModelIds(endpoint, "sk-test");

        // then
        assertThat(ids).containsExactlyInAnyOrder("models/gemini-pro", "models/gemini-1.5");
    }

    @Test
    @DisplayName("非 2xx 抛 CatalogSyncException")
    void fetchModelIds_errorStatus_throws() throws Exception {
        // given：HttpClient mock 返回 401
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        ChannelEndpoint endpoint = newEndpoint("https://api.example.com", Protocol.OPENAI);

        // when/then
        assertThatThrownBy(() -> client.fetchModelIds(endpoint, "sk-test"))
                .isInstanceOf(CatalogSyncException.class);
    }

    @Test
    @DisplayName("send 抛 InterruptedException 时恢复中断标志并抛 CatalogSyncException")
    void fetchModelIds_interrupted_restoresFlagAndThrows() throws Exception {
        // given：HttpClient send 被中断
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));
        ChannelEndpoint endpoint = newEndpoint("https://api.example.com", Protocol.OPENAI);

        // when/then — 抛 CatalogSyncException 且当前线程中断标志被恢复（供调用方感知中断）
        assertThatThrownBy(() -> client.fetchModelIds(endpoint, "sk-test"))
                .isInstanceOf(CatalogSyncException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        // 清除中断标志，避免污染后续测试执行
        Thread.interrupted();
    }

    /** 构造渠道端点测试对象 */
    private ChannelEndpoint newEndpoint(String endpointUrl, Protocol protocol) {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setEndpointUrl(endpointUrl);
        endpoint.setProtocol(protocol);
        return endpoint;
    }
}
