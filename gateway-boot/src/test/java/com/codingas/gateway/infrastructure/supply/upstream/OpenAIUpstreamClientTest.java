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
package com.codingas.gateway.infrastructure.supply.upstream;

import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResult;
import com.codingas.gateway.support.ProviderSimulator;
import com.codingas.gateway.support.ResponseTemplates;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

/**
 * OpenAIUpstreamClient 测试 — 覆盖 8 个核心场景
 */
class OpenAIUpstreamClientTest {

    // ==================== 辅助方法 ====================

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
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueOpenAISuccess();

            OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test-key", 30);
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
    }

    // ==================== 场景 2：请求路径和头部验证 ====================

    @Test
    void chat_请求发送到正确路径并携带Authorization头() throws Exception {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueOpenAISuccess();

            OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-secret-key", 30);
            client.chat(createTestRequest());

            RecordedRequest recorded = sim.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
            assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-secret-key");
            assertThat(recorded.getHeader("Content-Type")).contains("application/json");
            assertThat(recorded.getMethod()).isEqualTo("POST");
        }
    }

    // ==================== 场景 3：流式调用 ====================

    @Test
    void chatStream_流式调用_收到多个chunk并正常完成() throws Exception {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueStream(ResponseTemplates.openaiStreamChunks());

            OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test-key", 30);

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
    }

    // ==================== 场景 4：429 限流 ====================

    @Test
    void chat_429限流_抛出RATE_LIMIT_ERROR() throws IOException {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueError(429, ResponseTemplates.openaiError(429));

            OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test-key", 30);

            assertThatThrownBy(() -> client.chat(createTestRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> {
                        ProviderException pe = (ProviderException) ex;
                        assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR);
                    });
        }
    }

    // ==================== 场景 5：401 鉴权失败 ====================

    @Test
    void chat_401鉴权失败_抛出AUTHENTICATION_ERROR() throws IOException {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueError(401, ResponseTemplates.openaiError(401));

            OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-invalid-key", 30);

            assertThatThrownBy(() -> client.chat(createTestRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> {
                        ProviderException pe = (ProviderException) ex;
                        assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
                    });
        }
    }

    // ==================== 场景 6：500 服务端错误 ====================

    @Test
    void chat_500服务端错误_抛出UPSTREAM_ERROR() throws IOException {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueError(500, ResponseTemplates.openaiError(500));

            OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test-key", 30);

            assertThatThrownBy(() -> client.chat(createTestRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> {
                        ProviderException pe = (ProviderException) ex;
                        assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
                    });
        }
    }

    // ==================== 场景 7：超时 ====================

    @Test
    void chat_超时_抛出TIMEOUT_ERROR() throws Exception {
        // 超时测试后 MockWebServer 因未消费的延迟响应可能关闭失败，
        // 因此使用 try-finally 并吞掉关闭异常
        ProviderSimulator sim = ProviderSimulator.create();
        try {
            sim.enqueueTimeout();

            // 使用 1 秒短超时客户端
            OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test-key", 1);

            assertThatThrownBy(() -> client.chat(createTestRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> {
                        ProviderException pe = (ProviderException) ex;
                        assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.TIMEOUT_ERROR);
                    });
        } finally {
            try {
                sim.close();
            } catch (IOException e) {
                // 超时后 MockWebServer 可能因未消费的延迟响应而关闭失败，忽略此异常
            }
        }
    }

    // ==================== 场景 8：连通性测试 ====================

    @Test
    void testConnectivity_连通性测试成功() throws Exception {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            // 连通性测试请求 /v1/models，入队一个 200 响应
            sim.enqueueOpenAISuccess();

            OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test-key", 30);
            ConnectivityTestResult result = client.testConnectivity();

            assertThat(result.success()).isTrue();
            assertThat(result.errorMessage()).isNull();
        }
    }
}
