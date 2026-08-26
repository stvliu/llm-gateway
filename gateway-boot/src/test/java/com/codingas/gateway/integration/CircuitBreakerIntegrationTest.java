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

import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.raw.OpenAIChatRequest;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.transport.UpstreamException;
import com.codingas.gateway.resilience.circuitbreaker.CircuitBreaker;
import com.codingas.gateway.resilience.circuitbreaker.CircuitBreakerState;
import com.codingas.gateway.protocol.openai.OpenAIUpstreamClient;
import com.codingas.gateway.support.ProviderSimulator;
import com.codingas.gateway.support.ResponseTemplates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 熔断器状态流转与间歇故障恢复集成测试（Task 2.6）。
 * <p>
 * 覆盖 {@link CircuitBreaker} 的 CLOSED→OPEN→HALF_OPEN→CLOSED 状态机，
 * 以及通过 {@link ProviderSimulator}（MockWebServer）模拟上游间歇故障后的恢复路径。
 * <p>
 * 说明：行为序列类 {@code BehaviorSequence} 位于 gateway-simulator 模块
 * （包 {@code com.codingas.simulator.service}），gateway-boot 测试无法直接访问，
 * 故间歇故障场景改用 ProviderSimulator 交替入队错误/成功响应来验证。
 *
 * @see SimulatorGatewayIntegrationTest 参考的测试基座
 */
class CircuitBreakerIntegrationTest {

    // ==================== 1. CircuitBreaker 状态流转 ====================

    @Nested
    @DisplayName("CircuitBreaker 状态流转")
    class CircuitBreakerStateTransition {

        @Test
        @DisplayName("连续失败超过阈值触发熔断 — CLOSED → OPEN")
        void testCircuitBreaker_opensAfterFailures() {
            // 失败率阈值 50%，滑动窗口 10，熔断开启后 30s 冷却，半开最大试探 3 次
            CircuitBreaker cb = new CircuitBreaker(0.5, 10, 30000, 3);
            assertThat(cb.getState()).isEqualTo(CircuitBreakerState.CLOSED);

            // 窗口未满但连续全部失败时快速熔断：10 次连续失败填满窗口且全部失败
            for (int i = 0; i < 10; i++) {
                cb.recordFailure();
            }

            // 失败率达阈值 → 触发熔断，状态转为 OPEN
            assertThat(cb.getState()).isEqualTo(CircuitBreakerState.OPEN);
            // 熔断开启后立即拒绝请求
            assertThat(cb.allowRequest()).isFalse();
        }

        @Test
        @DisplayName("熔断冷却后进入半开，成功后恢复 — OPEN → HALF_OPEN → CLOSED")
        void testCircuitBreaker_halfOpenToClosed() {
            // openDuration 设为 50ms，便于测试快速进入半开
            CircuitBreaker cb = new CircuitBreaker(0.5, 10, 50, 3);

            // 10 次失败触发熔断
            for (int i = 0; i < 10; i++) {
                cb.recordFailure();
            }
            assertThat(cb.getState()).isEqualTo(CircuitBreakerState.OPEN);
            // 冷却时间未到，请求被拒绝
            assertThat(cb.allowRequest()).isFalse();

            // 等待冷却时间（50ms）结束
            sleep(100);

            // 冷却后允许试探请求 → 状态转为 HALF_OPEN
            assertThat(cb.allowRequest()).isTrue();
            assertThat(cb.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);

            // 半开状态下记录一次成功 → 恢复 CLOSED
            cb.recordSuccess();
            assertThat(cb.getState()).isEqualTo(CircuitBreakerState.CLOSED);
            // 恢复后正常放行
            assertThat(cb.allowRequest()).isTrue();
        }
    }

    // ==================== 2. 间歇故障恢复 ====================

    @Nested
    @DisplayName("间歇故障恢复")
    class IntermittentFailure {

        @Test
        @DisplayName("上游交替返回 500/200 — 第一次失败、第二次成功恢复")
        void testIntermittentFailure_recovery() throws IOException {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                // 先入队一次 500 错误，再入队一次成功响应，模拟间歇故障
                sim.enqueueError(500, ResponseTemplates.openaiError(500));
                sim.enqueueOpenAISuccess();

                OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test-key", 30);

                // 第一次调用：消费 500 响应 → 抛 UPSTREAM_ERROR
                assertThatThrownBy(() -> client.chat(createTestRequest("gpt-4", false)))
                        .isInstanceOf(UpstreamException.class)
                        .satisfies(ex -> {
                            UpstreamException pe = (UpstreamException) ex;
                            assertThat(pe.getErrorType()).isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
                        });

                // 第二次调用：消费成功响应 → 恢复正常
                ProtocolResponse response = client.chat(createTestRequest("gpt-4", false));
                assertThat(response).isNotNull();
                assertThat(response.getModel()).isEqualTo("gpt-4o");
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 OpenAI 协议请求，与 SimulatorGatewayIntegrationTest 保持一致。
     *
     * @param model  模型名称
     * @param stream 是否流式
     * @return OpenAI 协议请求
     */
    private OpenAIChatRequest createTestRequest(String model, boolean stream) {
        return OpenAIChatRequest.builder()
                .model(model)
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("Hello").build()))
                .stream(stream)
                .build();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
