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

import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
import com.codingas.gateway.protocol.transport.SessionStartContext;
import com.codingas.gateway.protocol.transport.SessionStartHook;
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
 * GeminiUpstreamClient 测试 — 覆盖核心场景（协议插件自包含：格式转换 + 传输调用）
 */
class GeminiUpstreamClientTest {

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

    private GeminiUpstreamClient createClient(String apiKey, int timeout) {
        return createClient(apiKey, timeout, null);
    }

    private GeminiUpstreamClient createClient(String apiKey, int timeout, SessionStartHook hook) {
        return new GeminiUpstreamClient(httpClient, baseUrl, apiKey, timeout,
                objectMapper, new GeminiErrorClassifier(), hook);
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

    private static String geminiSuccessBody() {
        return """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "Hello! How can I help?"}],
                        "role": "model"
                      },
                      "finishReason": "STOP",
                      "index": 0
                    }
                  ],
                  "usageMetadata": {
                    "promptTokenCount": 10,
                    "candidatesTokenCount": 8,
                    "totalTokenCount": 18
                  },
                  "modelVersion": "gemini-2.5-pro"
                }""";
    }

    private static String geminiStreamBody() {
        return """
                data: {"candidates":[{"content":{"parts":[{"text":"Hello"}],"role":"model"},"finishReason":null,"index":0}]}

                data: {"candidates":[{"content":{"parts":[{"text":"!"}],"role":"model"},"finishReason":null,"index":0}]}

                data: {"candidates":[{"content":{"parts":[],"role":"model"},"finishReason":"STOP","index":0}],"usageMetadata":{"promptTokenCount":10,"candidatesTokenCount":8,"totalTokenCount":18}}

                """;
    }

    private static String geminiErrorBody(int statusCode) {
        return "{\"error\":{\"code\":" + statusCode + ",\"status\":\"ERROR\",\"message\":\"Simulated error\"}}";
    }

    /**
     * 创建标准 Gemini Chat 请求
     */
    private GeminiChatRequest createTestRequest() {
        GeminiChatRequest req = new GeminiChatRequest();
        req.setModel("gemini-2.5-pro");
        req.setSystem("You are helpful.");
        req.addMessage(new GeminiChatRequest.Message("user", "Hello"));
        req.setMaxTokens(100);
        req.setTemperature(0.7);
        return req;
    }

    // ==================== 场景 1：非流式正常调用 ====================

    @Test
    void chat_非流式正常调用_返回正确响应() {
        enqueueJson(200, geminiSuccessBody());

        GeminiUpstreamClient client = createClient("gem-key", 30);
        GeminiChatResponse response = (GeminiChatResponse) client.chat(createTestRequest());

        assertThat(response).isNotNull();
        assertThat(response.text()).isEqualTo("Hello! How can I help?");
        assertThat(response.inputTokens()).isEqualTo(10);
        assertThat(response.outputTokens()).isEqualTo(8);
        // geminiSuccessBody 的 finishReason=STOP 应被解析
        assertThat(response.finishReason()).isEqualTo("STOP");
    }

    // ==================== 场景 2：请求路径和头部验证 ====================

    @Test
    void chat_请求发送到generateContent路径并携带x_goog_api_key头() throws Exception {
        enqueueJson(200, geminiSuccessBody());

        GeminiUpstreamClient client = createClient("gem-secret-key", 30);
        client.chat(createTestRequest());

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/v1beta/models/gemini-2.5-pro:generateContent");
        assertThat(recorded.getHeader("x-goog-api-key")).isEqualTo("gem-secret-key");
        assertThat(recorded.getHeader("Content-Type")).contains("application/json");
        assertThat(recorded.getMethod()).isEqualTo("POST");
    }

    @Test
    void chat_请求体序列化为Gemini原生格式() throws Exception {
        enqueueJson(200, geminiSuccessBody());

        GeminiUpstreamClient client = createClient("gem-secret-key", 30);
        client.chat(createTestRequest());

        RecordedRequest recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"contents\"");
        assertThat(body).contains("\"role\":\"user\"");
        assertThat(body).contains("\"text\":\"Hello\"");
        assertThat(body).contains("\"systemInstruction\"");
        assertThat(body).contains("\"maxOutputTokens\":100");
    }

    // ==================== 场景 2.5：SessionStart hook（请求发出前触发） ====================

    @Test
    void chat_请求发出前_触发SessionStartHook() {
        enqueueJson(200, geminiSuccessBody());

        List<SessionStartContext> contexts = new CopyOnWriteArrayList<>();
        GeminiUpstreamClient client = createClient("gem-key", 30, contexts::add);

        // traceId 经 copy() 透传：验证请求契约扩展 + hook 全链路关联
        GeminiChatRequest request = createTestRequest();
        request.setTraceId("trace-abc-123");
        client.chat((GeminiChatRequest) request.copy());

        assertThat(contexts).hasSize(1);
        SessionStartContext ctx = contexts.get(0);
        assertThat(ctx.traceId()).isEqualTo("trace-abc-123");
        assertThat(ctx.provider()).isEqualTo("gemini");
        assertThat(ctx.model()).isEqualTo("gemini-2.5-pro");
        assertThat(ctx.endpointUrl()).isEqualTo(baseUrl);
        // Gemini 模型名在 URL 路径而非请求体：断言请求体为 Gemini 原生格式
        assertThat(ctx.requestBody()).contains("\"contents\"");
        assertThat(ctx.requestBytes()).isGreaterThan(0);
        assertThat(ctx.stream()).isFalse();
    }

    @Test
    void chatStream_请求发出前_触发SessionStartHook() throws Exception {
        enqueueStream(geminiStreamBody());

        List<SessionStartContext> contexts = new CopyOnWriteArrayList<>();
        GeminiUpstreamClient client = createClient("gem-key", 30, contexts::add);
        CountDownLatch latch = new CountDownLatch(1);

        GeminiChatRequest request = createTestRequest();
        request.setTraceId("trace-abc-123");
        client.chatStream((GeminiChatRequest) request.copy(), new StreamCallback() {
            @Override public void onChunk(String data) { }
            @Override public void onComplete() { latch.countDown(); }
            @Override public void onError(Throwable t) { latch.countDown(); }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(contexts).hasSize(1);
        assertThat(contexts.get(0).traceId()).isEqualTo("trace-abc-123");
        assertThat(contexts.get(0).provider()).isEqualTo("gemini");
        assertThat(contexts.get(0).stream()).isTrue();
    }

    // ==================== 场景 3：流式调用 ====================

    @Test
    void chatStream_流式调用_收到多个chunk并正常完成() throws Exception {
        enqueueStream(geminiStreamBody());

        GeminiUpstreamClient client = createClient("gem-key", 30);

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
        assertThat(chunks).hasSize(3);
        assertThat(chunks).anyMatch(chunk -> chunk.contains("Hello"));
    }

    // ==================== 场景 4：429 限流 ====================

    @Test
    void chat_429限流_抛出RATE_LIMIT_ERROR() {
        enqueueJson(429, geminiErrorBody(429));

        GeminiUpstreamClient client = createClient("gem-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR);
                });
    }

    // ==================== 场景 5：401 鉴权失败 ====================

    @Test
    void chat_401鉴权失败_抛出AUTHENTICATION_ERROR() {
        enqueueJson(401, geminiErrorBody(401));

        GeminiUpstreamClient client = createClient("gem-invalid-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
                });
    }

    // ==================== 场景 5.5：404 模型不存在 ====================

    @Test
    void chat_404模型不存在_抛出MODEL_NOT_FOUND并透传httpStatus() {
        enqueueJson(404, "{\"error\":{\"code\":404,\"status\":\"NOT_FOUND\",\"message\":\"models/gemini-2.5-pro not found\"}}");

        GeminiUpstreamClient client = createClient("gem-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.MODEL_NOT_FOUND);
                    assertThat(pe.getHttpStatus()).isEqualTo(404);
                });
    }

    // ==================== 场景 6：500 服务端错误 ====================

    @Test
    void chat_500服务端错误_抛出UPSTREAM_ERROR() {
        enqueueJson(500, geminiErrorBody(500));

        GeminiUpstreamClient client = createClient("gem-key", 30);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
                });
    }

    // ==================== 场景 7：超时 ====================

    @Test
    void chat_超时_抛出TIMEOUT_ERROR() {
        enqueueTimeout();

        // 使用 1 秒短超时客户端
        GeminiUpstreamClient client = createClient("gem-key", 1);

        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.TIMEOUT_ERROR);
                });
    }

    // ==================== 场景 8：连通性测试 ====================

    @Test
    void testConnectivity_连通性测试成功() {
        // 连通性测试请求 GET /v1beta/models，入队一个 200 响应
        enqueueJson(200, "{\"models\":[]}");

        GeminiUpstreamClient client = createClient("gem-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void testConnectivity_HTTP失败_返回失败结果() {
        enqueueJson(500, geminiErrorBody(500));

        GeminiUpstreamClient client = createClient("gem-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("HTTP 500");
    }

    @Test
    void testConnectivity_服务不可达_返回失败结果() throws IOException {
        server.shutdown();
        server = null;

        GeminiUpstreamClient client = createClient("gem-key", 30);
        ConnectivityTestResult result = client.testConnectivity();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }

    // ==================== 场景 9：supportedProvider ====================

    @Test
    void supportedProvider_返回gemini() {
        assertThat(createClient("gem-key", 30).supportedProvider()).isEqualTo("gemini");
    }

    // ==================== 场景 10：流式错误与边界 ====================

    @Test
    void chatStream_HTTP错误_触发onError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody(geminiErrorBody(429)));

        GeminiUpstreamClient client = createClient("gem-key", 30);
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

        GeminiUpstreamClient client = createClient("gem-key", 30);
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
    void chatStream_无结束标记_EOF触发onComplete() throws Exception {
        // Gemini SSE 无 [DONE]/message_stop 标记，读到 EOF 后应 onComplete
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        data: {"candidates":[{"content":{"parts":[{"text":"hi"}],"role":"model"}}]}

                        data: {"candidates":[{"content":{"parts":[{"text":"!"}],"role":"model"}}]}

                        """));

        GeminiUpstreamClient client = createClient("gem-key", 30);
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
    void chat_服务不可达_抛出NETWORK_ERROR() throws IOException {
        server.shutdown();
        server = null;

        GeminiUpstreamClient client = createClient("gem-key", 30);
        assertThatThrownBy(() -> client.chat(createTestRequest()))
                .isInstanceOf(UpstreamException.class)
                .satisfies(ex -> {
                    UpstreamException pe = (UpstreamException) ex;
                    assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.NETWORK_ERROR);
                });
    }
}
