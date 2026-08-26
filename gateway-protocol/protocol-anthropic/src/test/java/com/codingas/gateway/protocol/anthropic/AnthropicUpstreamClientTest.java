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

import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.protocol.raw.AnthropicMessagesRequest;
import com.codingas.gateway.protocol.raw.AnthropicMessagesResponse;
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
 * AnthropicUpstreamClient 测试 — 覆盖 8 个核心场景（协议插件自包含）
 */
class AnthropicUpstreamClientTest {

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

    private AnthropicUpstreamClient createClient(String apiKey, int timeout) {
        return new AnthropicUpstreamClient(httpClient, baseUrl, apiKey, timeout,
                objectMapper, new AnthropicErrorClassifier());
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

    private static String anthropicSuccessBody() {
        return """
                {
                  "id": "msg_test_001",
                  "type": "message",
                  "role": "assistant",
                  "model": "claude-sonnet-4-20250514",
                  "content": [
                    {
                      "type": "text",
                      "text": "Hello! How can I help you today?"
                    }
                  ],
                  "stop_reason": "end_turn",
                  "stop_sequence": null,
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 8
                  }
                }""";
    }

    private static String anthropicStreamBody() {
        return """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_test_002","type":"message","role":"assistant","model":"claude-sonnet-4-20250514","content":[],"stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":0}}}

                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"!"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":0}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":8}}

                event: message_stop
                data: {"type":"message_stop"}
                """;
    }

    private static String anthropicErrorBody(int statusCode) {
        return "{\"error\":{\"type\":\"error\",\"message\":\"Simulated error with status code " + statusCode + "\"}}";
    }

    /**
     * 创建标准 Anthropic Messages 请求
     */
    private AnthropicMessagesRequest createTestRequest() {
        return AnthropicMessagesRequest.builder()
                .model("claude-sonnet-4-20250514")
                .messages(List.of(
                        AnthropicMessagesRequest.Message.builder()
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
        enqueueJson(200, anthropicSuccessBody());

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);
        AnthropicMessagesResponse response = (AnthropicMessagesResponse) client.chat(createTestRequest());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("msg_test_001");
        assertThat(response.getModel()).isEqualTo("claude-sonnet-4-20250514");
        assertThat(response.getContent()).isNotEmpty();
        assertThat(response.getContent().get(0).getText())
                .isEqualTo("Hello! How can I help you today?");
        assertThat(response.getStopReason()).isEqualTo("end_turn");
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getInputTokens()).isEqualTo(10);
        assertThat(response.getUsage().getOutputTokens()).isEqualTo(8);
    }

    // ==================== 场景 2：请求路径和头部验证 ====================

    @Test
    void chat_请求发送到正确路径并携带x_api_key和anthropic_version头() throws Exception {
        enqueueJson(200, anthropicSuccessBody());

        AnthropicUpstreamClient client = createClient("sk-ant-secret-key", 30);
        client.chat(createTestRequest());

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/v1/messages");
        assertThat(recorded.getHeader("x-api-key")).isEqualTo("sk-ant-secret-key");
        assertThat(recorded.getHeader("anthropic-version")).isEqualTo("2023-06-01");
        assertThat(recorded.getHeader("Content-Type")).contains("application/json");
        assertThat(recorded.getMethod()).isEqualTo("POST");
    }

    // ==================== 场景 3：流式调用 ====================

    @Test
    void chatStream_流式调用_收到多个chunk并以message_stop正常完成() throws Exception {
        enqueueStream(anthropicStreamBody());

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);

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
        enqueueJson(429, anthropicErrorBody(429));

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);

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
        enqueueJson(401, anthropicErrorBody(401));

        AnthropicUpstreamClient client = createClient("sk-ant-invalid-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
                });
    }

    // ==================== 场景 6：500 服务端错误 ====================

    @Test
    void chat_500服务端错误_抛出UPSTREAM_ERROR() throws IOException {
        enqueueJson(500, anthropicErrorBody(500));

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);

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
        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 1);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.TIMEOUT_ERROR);
                });
    }

    // ==================== 场景 8：连通性测试 ====================

    @Test
    void testConnectivity_非5xx响应视为成功() throws Exception {
        // Anthropic 连通性测试发送 POST /v1/messages，
        // 只要 HTTP 状态码 < 500 即视为连通成功
        enqueueJson(400, anthropicErrorBody(400));

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isTrue();
    }

    // ==================== 场景 9：supportedProvider ====================

    @Test
    void supportedProvider_返回anthropic() {
        assertThat(createClient("sk-ant-test-key", 30).supportedProvider()).isEqualTo("anthropic");
    }

    // ==================== 场景 10：流式错误与边界 ====================

    @Test
    void chatStream_HTTP错误_触发onError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody(anthropicErrorBody(429)));

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);
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

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);
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
    void chatStream_无message_stop_循环结束触发onComplete() throws Exception {
        // 流不含 message_stop 事件时，读到 EOF 后应 onComplete
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        event: content_block_delta
                        data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"hi"}}

                        event: content_block_delta
                        data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"!"}}

                        """));

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);
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
    void testConnectivity_5xx_返回失败结果() throws Exception {
        enqueueJson(500, anthropicErrorBody(500));

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("HTTP 500");
    }

    @Test
    void testConnectivity_服务不可达_返回失败结果() throws Exception {
        server.shutdown();
        server = null;

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    void chat_服务不可达_抛出NETWORK_ERROR() throws Exception {
        server.shutdown();
        server = null;

        AnthropicUpstreamClient client = createClient("sk-ant-test-key", 30);
        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.NETWORK_ERROR);
                });
    }
}
