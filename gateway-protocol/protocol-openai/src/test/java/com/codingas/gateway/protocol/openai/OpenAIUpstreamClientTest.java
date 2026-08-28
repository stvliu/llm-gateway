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

import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.protocol.raw.OpenAIChatRequest;
import com.codingas.gateway.protocol.raw.OpenAIChatResponse;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
import com.codingas.gateway.protocol.transport.UpstreamException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenAIUpstreamClient 测试 — 覆盖 8 个核心场景（协议插件自包含）
 */
class OpenAIUpstreamClientTest {

    private MockWebServer server;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        httpClient = new OkHttpClient();
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        baseUrl = server.url("").toString().replaceAll("/$", "");
    }

    @AfterEach
    void tearDown() {
        if (server == null) {
            return;
        }
        try {
            server.shutdown();
        } catch (IOException e) {
            // 超时后 MockWebServer 可能因未消费的延迟响应而关闭失败，忽略此异常
        }
    }

    private OpenAIUpstreamClient createClient(String apiKey, int timeout) {
        return new OpenAIUpstreamClient(httpClient, baseUrl, apiKey, timeout,
                objectMapper, new OpenAIErrorClassifier());
    }

    private void enqueueJson(int code, String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }

    private void enqueueStream(String sseBody) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody));
    }

    private void enqueueTimeout() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(30, TimeUnit.SECONDS)
                .setBody("timeout simulation"));
    }

    private static String openaiSuccessBody() {
        return """
                {
                  "id": "chatcmpl-test-001",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4o",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Hello! How can I help you today?"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 8,
                    "total_tokens": 18
                  }
                }""";
    }

    private static String openaiStreamBody() {
        return """
                data: {"id":"chatcmpl-test-002","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

                data: {"id":"chatcmpl-test-002","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

                data: {"id":"chatcmpl-test-002","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}

                data: {"id":"chatcmpl-test-002","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

                data: [DONE]
                """;
    }

    private static String openaiErrorBody(int statusCode) {
        return "{\"error\":{\"type\":\"error\",\"message\":\"Simulated error with status code " + statusCode + "\"}}";
    }

    /**
     * 创建标准 OpenAI Chat 请求
     */
    private OpenAIChatRequest createTestRequest() {
        return OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(
                        OpenAIChatRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .maxTokens(100)
                .build();
    }

    // ==================== 场景 1：非流式正常调用 ====================

    @Test
    void chat_非流式正常调用_返回正确响应() throws Exception {
        enqueueJson(200, openaiSuccessBody());

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        OpenAIChatResponse response = (OpenAIChatResponse) client.chat(createTestRequest());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("chatcmpl-test-001");
        assertThat(response.getModel()).isEqualTo("gpt-4o");
        assertThat(response.getChoices()).isNotEmpty();
        assertThat(response.getChoices().get(0).getMessage().getContent())
                .isEqualTo("Hello! How can I help you today?");
        assertThat(response.getChoices().get(0).getFinishReason()).isEqualTo("stop");
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(10);
        assertThat(response.getUsage().getCompletionTokens()).isEqualTo(8);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(18);
    }

    // ==================== 场景 2：请求路径和头部验证 ====================

    @Test
    void chat_请求发送到正确路径并携带Authorization头() throws Exception {
        enqueueJson(200, openaiSuccessBody());

        OpenAIUpstreamClient client = createClient("sk-secret-key", 30);
        client.chat(createTestRequest());

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-secret-key");
        assertThat(recorded.getHeader("Content-Type")).contains("application/json");
        assertThat(recorded.getMethod()).isEqualTo("POST");
    }

    // ==================== 场景 3：流式调用 ====================

    @Test
    void chatStream_流式调用_收到多个chunk并正常完成() throws Exception {
        enqueueStream(openaiStreamBody());

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);

        CountDownLatch latch = new CountDownLatch(1);
        List<String> chunks = new CopyOnWriteArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        StreamCallback callback = new StreamCallback() {
            @Override
            public void onChunk(String data) {
                chunks.add(data);
            }

            @Override
            public void onComplete() {
                completed.set(true);
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }
        };

        client.chatStream(createTestRequest(), callback);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(completed.get()).isTrue();
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks).anyMatch(chunk -> chunk.contains("Hello"));
    }

    // ==================== 场景 4：429 限流 ====================

    @Test
    void chat_429限流_抛出RATE_LIMIT_ERROR() throws IOException {
        enqueueJson(429, openaiErrorBody(429));

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR);
                });
    }

    // ==================== 场景 5：401 鉴权失败 ====================

    @Test
    void chat_401鉴权失败_抛出AUTHENTICATION_ERROR() throws IOException {
        enqueueJson(401, openaiErrorBody(401));

        OpenAIUpstreamClient client = createClient("sk-invalid-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
                });
    }

    // ==================== 场景 5.5：404 模型不存在 ====================

    @Test
    void chat_404模型不存在_抛出MODEL_NOT_FOUND并透传httpStatus() throws IOException {
        enqueueJson(404, "{\"error\":{\"message\":\"The model 'gpt-4o' does not exist\",\"code\":\"model_not_found\"}}");

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.MODEL_NOT_FOUND);
                    assertThat(pe.getHttpStatus()).isEqualTo(404);
                });
    }

    @Test
    void chatStream_HTTP404_触发onError并透传httpStatus() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"The model 'gpt-4o' does not exist\",\"code\":\"model_not_found\"}}"));

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.chatStream(createTestRequest(), new StreamCallback() {
            @Override public void onChunk(String data) { }
            @Override public void onComplete() { latch.countDown(); }
            @Override public void onError(Throwable t) { error.set(t); latch.countDown(); }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(UpstreamException.class);
        UpstreamException pe = (UpstreamException) error.get();
        assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.MODEL_NOT_FOUND);
        assertThat(pe.getHttpStatus()).isEqualTo(404);
    }

    // ==================== 场景 6：500 服务端错误 ====================

    @Test
    void chat_500服务端错误_抛出UPSTREAM_ERROR() throws IOException {
        enqueueJson(500, openaiErrorBody(500));

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
                });
    }

    // ==================== 场景 7：超时 ====================

    @Test
    void chat_超时_抛出TIMEOUT_ERROR() throws Exception {
        enqueueTimeout();

        // 使用 1 秒短超时客户端
        OpenAIUpstreamClient client = createClient("sk-test-key", 1);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.TIMEOUT_ERROR);
                });
    }

    // ==================== 场景 8：连通性测试 ====================

    @Test
    void testConnectivity_连通性测试成功() throws Exception {
        // 连通性测试请求 /v1/models，入队一个 200 响应
        enqueueJson(200, openaiSuccessBody());

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void testConnectivity_HTTP失败_返回失败结果() throws Exception {
        enqueueJson(500, openaiErrorBody(500));

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("HTTP 500");
    }

    @Test
    void testConnectivity_服务不可达_返回失败结果() throws Exception {
        // 先关闭服务端，请求必然连接失败 → 捕获异常返回失败结果
        server.shutdown();
        server = null;

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }

    // ==================== 场景 9：supportedProvider ====================

    @Test
    void supportedProvider_返回openai() {
        assertThat(createClient("sk-test-key", 30).supportedProvider()).isEqualTo("openai");
    }

    // ==================== 场景 10：流式错误与边界 ====================

    @Test
    void chatStream_HTTP错误_触发onError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody(openaiErrorBody(429)));

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.chatStream(createTestRequest(), new StreamCallback() {
            @Override public void onChunk(String data) { }
            @Override public void onComplete() { latch.countDown(); }
            @Override public void onError(Throwable t) { error.set(t); latch.countDown(); }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(UpstreamException.class);
        UpstreamException pe = (UpstreamException) error.get();
        assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR);
    }

    @Test
    void chatStream_网络断开_触发onError() throws Exception {
        // 服务端立即断开连接 → okhttp onFailure → 回调 onError(NETWORK_ERROR)
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.chatStream(createTestRequest(), new StreamCallback() {
            @Override public void onChunk(String data) { }
            @Override public void onComplete() { latch.countDown(); }
            @Override public void onError(Throwable t) { error.set(t); latch.countDown(); }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(UpstreamException.class);
        assertThat(((UpstreamException) error.get()).getErrorType())
                .isEqualTo(ProviderErrorType.NETWORK_ERROR);
    }

    @Test
    void chatStream_无DONE标记_循环结束触发onComplete() throws Exception {
        // SSE 流不含 [DONE] 标记时，读到 EOF 后应 onComplete
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        data: {"id":"c1","choices":[{"delta":{"content":"hi"}}]}

                        data: {"id":"c1","choices":[{"delta":{"content":"!"}}]}

                        """));

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        CountDownLatch latch = new CountDownLatch(1);
        List<String> chunks = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.chatStream(createTestRequest(), new StreamCallback() {
            @Override public void onChunk(String data) { chunks.add(data); }
            @Override public void onComplete() { latch.countDown(); }
            @Override public void onError(Throwable t) { error.set(t); latch.countDown(); }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isNull();
        assertThat(chunks).hasSize(2);
    }

    @Test
    void chatStream_空data行_不触发onChunk() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        data:

                        data: {"id":"c1","choices":[{"delta":{"content":"x"}}]}

                        data: [DONE]
                        """));

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        CountDownLatch latch = new CountDownLatch(1);
        List<String> chunks = new CopyOnWriteArrayList<>();

        client.chatStream(createTestRequest(), new StreamCallback() {
            @Override public void onChunk(String data) { chunks.add(data); }
            @Override public void onComplete() { latch.countDown(); }
            @Override public void onError(Throwable t) { latch.countDown(); }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        // 空 data 行被跳过，仅 1 个 chunk
        assertThat(chunks).hasSize(1);
    }

    @Test
    void chat_服务不可达_抛出NETWORK_ERROR() throws Exception {
        server.shutdown();
        server = null;

        OpenAIUpstreamClient client = createClient("sk-test-key", 30);
        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.NETWORK_ERROR);
                });
    }
}
