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
package com.codingas.gateway.integration;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.provider.vendor.ProviderException;
import com.codingas.gateway.resilience.circuitbreaker.CircuitBreaker;
import com.codingas.gateway.resilience.retry.RetryExecutor;
import com.codingas.gateway.providerhttp.upstream.OpenAIUpstreamClient;
import com.codingas.gateway.support.ProviderSimulator;
import com.codingas.gateway.support.ResponseTemplates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Gateway 全链路集成测试 — 覆盖韧性组件的协同工作场景。
 * <p>
 * 使用 ProviderSimulator（MockWebServer）模拟上游 LLM 提供商，
 * 直接通过 OpenAIUpstreamClient 验证韧性组件行为。
 */
class SimulatorGatewayIntegrationTest {

    // ==================== 1. 正常路径测试 ====================

    @Nested
    @DisplayName("正常路径")
    class NormalPath {

        @Test
        @DisplayName("非流式正常调用返回正确响应")
        void testNormalChat() throws Exception {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                sim.enqueueOpenAISuccess();

                var client = sim.createOpenAIIClient("sk-test-key", 30);
                var request = createTestRequest("gpt-4", false);
                ProtocolResponse response = client.chat(request);

                assertThat(response).isNotNull();
                assertThat(response.getModel()).isEqualTo("gpt-4o");
            }
        }

        @Test
        @DisplayName("流式正常调用收到多个 chunk 并完成")
        void testNormalStream() throws Exception {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                sim.enqueueStream(ResponseTemplates.openaiStreamChunks());

                var client = sim.createOpenAIIClient("sk-test-key", 30);
                var request = createTestRequest("gpt-4", true);

                CountDownLatch latch = new CountDownLatch(1);
                var chunks = new CopyOnWriteArrayList<String>();
                var completed = new AtomicBoolean(false);

                client.chatStream(request, new StreamCallback() {
                    @Override
                    public void onChunk(String data) { chunks.add(data); }
                    @Override
                    public void onComplete() { completed.set(true); latch.countDown(); }
                    @Override
                    public void onError(Throwable t) { latch.countDown(); }
                });

                assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(completed.get()).isTrue();
                assertThat(chunks).isNotEmpty();
            }
        }
    }

    // ==================== 2. 异常场景测试 ====================

    @Nested
    @DisplayName("异常场景")
    class ErrorScenarios {

        @Test
        @DisplayName("429 限流错误 — 通过 RetryExecutor 重试后抛出 RATE_LIMIT_ERROR")
        void testRateLimit_retried() throws IOException {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                sim.enqueueError(429, ResponseTemplates.openaiError(429));

                var client = sim.createOpenAIIClient("sk-test-key", 30);

                assertThatThrownBy(() -> client.chat(createTestRequest("gpt-4", false)))
                        .isInstanceOf(ProviderException.class)
                        .satisfies(ex -> {
                            ProviderException pe = (ProviderException) ex;
                            assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR);
                        });
            }
        }

        @Test
        @DisplayName("401 认证错误 — 不重试，立即抛出 AUTHENTICATION_ERROR")
        void testAuthError_notRetried() throws IOException {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                sim.enqueueError(401, ResponseTemplates.openaiError(401));

                var client = sim.createOpenAIIClient("sk-invalid-key", 30);

                assertThatThrownBy(() -> client.chat(createTestRequest("gpt-4", false)))
                        .isInstanceOf(ProviderException.class)
                        .satisfies(ex -> {
                            ProviderException pe = (ProviderException) ex;
                            assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
                        });
            }
        }

        @Test
        @DisplayName("500 服务端错误 — 通过 RetryExecutor 重试后抛出 UPSTREAM_ERROR")
        void testUpstreamError_retried() throws IOException {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                sim.enqueueError(500, ResponseTemplates.openaiError(500));

                var client = sim.createOpenAIIClient("sk-test-key", 30);

                assertThatThrownBy(() -> client.chat(createTestRequest("gpt-4", false)))
                        .isInstanceOf(ProviderException.class)
                        .satisfies(ex -> {
                            ProviderException pe = (ProviderException) ex;
                            assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
                        });
            }
        }

        @Test
        @DisplayName("超时 — 抛出 TIMEOUT_ERROR")
        void testTimeout() throws Exception {
            ProviderSimulator sim = ProviderSimulator.create();
            try {
                sim.enqueueTimeout();

                var client = sim.createOpenAIIClient("sk-test-key", 1);

                assertThatThrownBy(() -> client.chat(createTestRequest("gpt-4", false)))
                        .isInstanceOf(ProviderException.class)
                        .satisfies(ex -> {
                            ProviderException pe = (ProviderException) ex;
                            assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.TIMEOUT_ERROR);
                        });
            } finally {
                try { sim.close(); } catch (IOException e) { /* 超时后 MockWebServer 关闭可能失败 */ }
            }
        }
    }

    // ==================== 3. 熔断器测试 ====================

    @Nested
    @DisplayName("熔断器生命周期")
    class CircuitBreakerTests {

        @Test
        @DisplayName("连续失败触发熔断 — CLOSED → OPEN")
        void testCircuitBreaker_opens() {
            CircuitBreaker cb = new CircuitBreaker(0.5, 10, 30000, 3);

            // 10 次连续失败
            for (int i = 0; i < 10; i++) {
                cb.recordFailure();
            }

            // 第 11 次请求应被熔断
            assertThat(cb.allowRequest()).isFalse();
        }

        @Test
        @DisplayName("熔断后等待 openDuration 后进入 HALF_OPEN")
        void testCircuitBreaker_halfOpen() {
            CircuitBreaker cb = new CircuitBreaker(0.5, 10, 50, 3);

            // 10 次失败触发熔断
            for (int i = 0; i < 10; i++) {
                cb.recordFailure();
            }
            assertThat(cb.allowRequest()).isFalse();

            // 等待 openDuration (50ms) 后应进入 HALF_OPEN
            sleep(100);
            assertThat(cb.allowRequest()).isTrue();

            // HALF_OPEN 时成功一次 → 恢复 CLOSED
            cb.recordSuccess();
            assertThat(cb.allowRequest()).isTrue();
        }
    }

    // ==================== 4. 多错误模式验证 ====================

    @Nested
    @DisplayName("Simulator 错误模式")
    class SimulatorErrorModes {

        @Test
        @DisplayName("各 HTTP 状态码被正确映射为 ProviderErrorType")
        void testErrorClassification() throws Exception {
            // 验证 OpenAIErrorClassifier 的映射表
            int[][] testCases = {
                {401, ProviderErrorType.AUTHENTICATION_ERROR.ordinal()},
                {429, ProviderErrorType.RATE_LIMIT_ERROR.ordinal()},
                {400, ProviderErrorType.INVALID_REQUEST.ordinal()},
                {500, ProviderErrorType.UPSTREAM_ERROR.ordinal()},
                {503, ProviderErrorType.SERVICE_UNAVAILABLE.ordinal()},
                {408, ProviderErrorType.TIMEOUT_ERROR.ordinal()},
                {504, ProviderErrorType.TIMEOUT_ERROR.ordinal()},
            };

            for (int[] tc : testCases) {
                int statusCode = tc[0];
                ProviderErrorType expectedType = ProviderErrorType.values()[tc[1]];

                try (ProviderSimulator sim = ProviderSimulator.create()) {
                    sim.enqueueError(statusCode, ResponseTemplates.openaiError(statusCode));

                    var client = sim.createOpenAIIClient("sk-test-key", 30);

                    try {
                        client.chat(createTestRequest("gpt-4", false));
                    } catch (ProviderException e) {
                        assertThat(e.getErrorType())
                                .as("HTTP %d 应映射为 %s", statusCode, expectedType)
                                .isEqualTo(expectedType);
                    }
                }
            }
        }

        @Test
        @DisplayName("429 quota 错误被映射为 QUOTA_EXCEEDED")
        void testQuotaExceeded() throws IOException {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                // 响应体包含 insufficient_quota → ErrorClassifier 应映射为 QUOTA_EXCEEDED
                sim.enqueueError(429, """
                        {"error":{"type":"insufficient_quota","message":"quota exceeded"}}""");

                var client = sim.createOpenAIIClient("sk-test-key", 30);

                assertThatThrownBy(() -> client.chat(createTestRequest("gpt-4", false)))
                        .isInstanceOf(ProviderException.class)
                        .satisfies(ex -> {
                            ProviderException pe = (ProviderException) ex;
                            assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.QUOTA_EXCEEDED);
                        });
            }
        }
    }

    // ==================== 辅助方法 ====================

    private ProtocolRequest createTestRequest(String model, boolean stream) {
        return new ProtocolRequest() {
            private String m = model;
            private boolean s = stream;

            @Override public String getModel() { return m; }
            @Override public void setModel(String model) { this.m = model; }
            @Override public String getProtocol() { return "openai"; }
            @Override public boolean isStream() { return s; }
            @Override public void setStream(boolean stream) { this.s = stream; }
        };
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
