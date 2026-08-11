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
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 超时与流中断集成测试 — 覆盖上游调用的超时和流式错误中断场景。
 * <p>
 * 使用 ProviderSimulator（MockWebServer）模拟上游 LLM 提供商，
 * 直接通过 OpenAIUpstreamClient 验证：
 * <ul>
 *   <li>读超时被映射为 {@link ProviderErrorType#TIMEOUT_ERROR}</li>
 *   <li>正常 SSE 流式响应能被完整接收并触发 onComplete</li>
 *   <li>流式请求遇到上游错误（HTTP 500）时触发 onError 而非 onComplete</li>
 * </ul>
 */
class TimeoutAndStreamIntegrationTest {

    // ==================== 1. 超时场景 ====================

    @Nested
    @DisplayName("超时场景")
    class TimeoutScenarios {

        @Test
        @DisplayName("读超时 — 抛出 ProviderException 且 errorType=TIMEOUT_ERROR")
        void testTimeout_throwsTimeoutError() throws Exception {
            // enqueueTimeout 入队 30 秒 body 延迟；client 读超时设为 1 秒，应触发 SocketTimeoutException
            // 超时后 MockWebServer 关闭可能失败，故使用 try/finally 而非 try-with-resources
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

    // ==================== 2. 流式场景 ====================

    @Nested
    @DisplayName("流式场景")
    class StreamScenarios {

        @Test
        @DisplayName("正常流式调用 — 收到多个 chunk 并完成")
        void testStreamNormal_completes() throws Exception {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                sim.enqueueStream(ResponseTemplates.openaiStreamChunks());

                var client = sim.createOpenAIIClient("sk-test-key", 30);
                var request = createTestRequest("gpt-4", true);

                CountDownLatch latch = new CountDownLatch(1);
                var chunks = new CopyOnWriteArrayList<String>();
                var completed = new AtomicBoolean(false);
                var errorRef = new AtomicReference<Throwable>();

                client.chatStream(request, new StreamCallback() {
                    @Override
                    public void onChunk(String data) { chunks.add(data); }

                    @Override
                    public void onComplete() { completed.set(true); latch.countDown(); }

                    @Override
                    public void onError(Throwable t) { errorRef.set(t); latch.countDown(); }
                });

                assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(errorRef.get()).as("正常流不应触发 onError").isNull();
                assertThat(completed.get()).as("正常流应触发 onComplete").isTrue();
                assertThat(chunks).as("应收到非空 chunk 列表").isNotEmpty();
            }
        }

        @Test
        @DisplayName("流式调用上游错误 — 触发 onError 而非 onComplete")
        void testStreamInterrupt_providerError() throws Exception {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                // 入队 HTTP 500 错误响应，stream=true
                sim.enqueueError(500, ResponseTemplates.openaiError(500));

                var client = sim.createOpenAIIClient("sk-test-key", 30);
                var request = createTestRequest("gpt-4", true);

                CountDownLatch latch = new CountDownLatch(1);
                var completed = new AtomicBoolean(false);
                var errorRef = new AtomicReference<Throwable>();

                client.chatStream(request, new StreamCallback() {
                    @Override
                    public void onChunk(String data) { /* 错误响应不应产生 chunk */ }

                    @Override
                    public void onComplete() { completed.set(true); latch.countDown(); }

                    @Override
                    public void onError(Throwable t) { errorRef.set(t); latch.countDown(); }
                });

                assertThat(latch.await(5, TimeUnit.SECONDS)).as("onError/onComplete 应在超时内触发").isTrue();
                assertThat(completed.get()).as("上游错误不应触发 onComplete").isFalse();
                assertThat(errorRef.get()).as("应触发 onError").isNotNull();
                assertThat(errorRef.get()).isInstanceOf(ProviderException.class);
                assertThat(((ProviderException) errorRef.get()).getErrorType())
                        .as("HTTP 500 流式错误应映射为 UPSTREAM_ERROR")
                        .isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建匿名 ProtocolRequest 测试桩，仅携带 model/protocol/stream 三个最小字段。
     *
     * @param model  模型名
     * @param stream 是否流式
     * @return ProtocolRequest 测试桩
     */
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
}
