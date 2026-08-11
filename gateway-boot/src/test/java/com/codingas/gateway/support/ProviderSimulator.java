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
package com.codingas.gateway.support;

import com.codingas.gateway.infrastructure.supply.upstream.AnthropicUpstreamClient;
import com.codingas.gateway.infrastructure.supply.upstream.OpenAIUpstreamClient;
import com.codingas.gateway.infrastructure.upstream.AnthropicErrorClassifier;
import com.codingas.gateway.infrastructure.upstream.OpenAIErrorClassifier;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;

/**
 * MockWebServer 封装，模拟上游 LLM 提供商（OpenAI / Anthropic）的 HTTP 行为。
 * <p>
 * 提供便利方法入队成功/错误/超时响应，以及工厂方法创建已配置好端点地址的 UpstreamClient。
 * <p>
 * 使用方式：
 * <pre>
 * try (ProviderSimulator sim = ProviderSimulator.create()) {
 *     sim.enqueueOpenAISuccess();
 *     OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test", 30);
 *     ProtocolResponse response = client.chat(request);
 * }
 * </pre>
 */
public class ProviderSimulator implements AutoCloseable {

    private final MockWebServer server;
    private final OkHttpClient sharedHttpClient;
    private final ObjectMapper objectMapper;

    private ProviderSimulator(MockWebServer server, OkHttpClient sharedHttpClient, ObjectMapper objectMapper) {
        this.server = server;
        this.sharedHttpClient = sharedHttpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建并启动模拟服务器
     *
     * @return 已启动的 ProviderSimulator 实例
     * @throws IOException 启动失败时抛出
     */
    public static ProviderSimulator create() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        OkHttpClient httpClient = new OkHttpClient.Builder().build();
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return new ProviderSimulator(server, httpClient, objectMapper);
    }

    /**
     * 获取模拟服务器的基础 URL
     *
     * @return 格式为 http://host:port 的 URL
     */
    public String getUrl() {
        String url = server.url("").toString();
        // 去除末尾斜杠，避免客户端拼接路径时出现 //v1/...
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    // ==================== 响应入队 ====================

    /**
     * 入队 OpenAI 成功响应（200），使用 ResponseTemplates.openaiChatCompletion()
     */
    public void enqueueOpenAISuccess() {
        enqueueSuccess(ResponseTemplates.openaiChatCompletion());
    }

    /**
     * 入队 Anthropic 成功响应（200），使用 ResponseTemplates.anthropicMessages()
     */
    public void enqueueAnthropicSuccess() {
        enqueueSuccess(ResponseTemplates.anthropicMessages());
    }

    /**
     * 入队 SSE 流式响应（200），Content-Type 为 text/event-stream
     *
     * @param sseBody SSE 格式的流式响应字符串
     */
    public void enqueueStream(String sseBody) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody));
    }

    /**
     * 入队错误响应
     *
     * @param statusCode HTTP 状态码
     * @param errorBody  错误响应体
     */
    public void enqueueError(int statusCode, String errorBody) {
        server.enqueue(new MockResponse()
                .setResponseCode(statusCode)
                .setHeader("Content-Type", "application/json")
                .setBody(errorBody));
    }

    /**
     * 入队超时响应（30 秒 body 延迟），用于模拟上游超时场景
     */
    public void enqueueTimeout() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(30, java.util.concurrent.TimeUnit.SECONDS)
                .setBody("timeout simulation"));
    }

    // ==================== 客户端工厂 ====================

    /**
     * 创建已配置好模拟服务器地址的 OpenAI UpstreamClient
     *
     * @param apiKey   API 密钥
     * @param timeout  超时时间（秒）
     * @return OpenAIUpstreamClient 实例
     */
    public OpenAIUpstreamClient createOpenAIIClient(String apiKey, int timeout) {
        return new OpenAIUpstreamClient(
                sharedHttpClient,
                getUrl(),
                apiKey,
                timeout,
                objectMapper,
                new OpenAIErrorClassifier()
        );
    }

    /**
     * 创建已配置好模拟服务器地址的 Anthropic UpstreamClient
     *
     * @param apiKey   API 密钥
     * @param timeout  超时时间（秒）
     * @return AnthropicUpstreamClient 实例
     */
    public AnthropicUpstreamClient createAnthropicClient(String apiKey, int timeout) {
        return new AnthropicUpstreamClient(
                sharedHttpClient,
                getUrl(),
                apiKey,
                timeout,
                objectMapper,
                new AnthropicErrorClassifier()
        );
    }

    // ==================== 请求录制 ====================

    /**
     * 取出下一个录制的请求（代理 MockWebServer.takeRequest()）
     *
     * @return 录制的请求
     * @throws InterruptedException 等待被中断时抛出
     */
    public RecordedRequest takeRequest() throws InterruptedException {
        return server.takeRequest();
    }

    // ==================== 生命周期 ====================

    /**
     * 关闭模拟服务器
     *
     * @throws IOException 关闭失败时抛出
     */
    @Override
    public void close() throws IOException {
        server.close();
    }

    // ==================== 内部辅助 ====================

    private void enqueueSuccess(String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }
}
