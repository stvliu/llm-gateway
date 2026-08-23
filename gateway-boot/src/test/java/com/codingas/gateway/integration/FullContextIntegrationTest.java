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

import com.codingas.gateway.proxy.invoker.KeyFailoverInvoker;
import com.codingas.gateway.proxy.routing.CredentialResolver;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.vendor.ProviderException;
import com.codingas.gateway.provider.upstream.ResilientClientFactory;
import com.codingas.gateway.provider.upstream.UpstreamClient;
import com.codingas.gateway.provider.upstream.UpstreamClientRegistry;
import com.codingas.gateway.provider.upstream.RoutingContext;
import com.codingas.gateway.common.enums.FailureStrategy;
import com.codingas.gateway.resilience.circuitbreaker.ChannelEndpointCircuitBreakerManager;
import com.codingas.gateway.support.ProviderSimulator;
import com.codingas.gateway.support.ResponseTemplates;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 FullContextIntegrationTestBase 能正常加载 Spring 上下文，
 * 并通过真实 {@link KeyFailoverInvoker} + {@link ProviderSimulator} 验证 Key 级故障转移链路。
 */
class FullContextIntegrationTest extends FullContextIntegrationTestBase {

    @Test
    void contextLoads() {
        assertThat(chatDispatchService).isNotNull();
    }

    // ==================== Key 故障转移测试 ====================

    /**
     * Key 级故障转移集成测试
     *
     * <p>不依赖基类对 {@code KeyFailoverInvoker} 的 {@code @MockBean}（它把真实 invoker mock 了，无法测真实链路），
     * 而是手动 {@code new KeyFailoverInvoker(...)} 构造真实 invoker，配合 {@link ProviderSimulator}（MockWebServer）
     * 验证 Key 失败 → 切换 → 成功的真实 HTTP 链路。参考 {@code SimulatorGatewayIntegrationTest} 模式。</p>
     */
    @Nested
    @DisplayName("Key 故障转移")
    class KeyFailoverTests {

        @Test
        @DisplayName("Key1 返回 401 失败 → 自动切换 Key2 返回 200 成功")
        void testKeyFailover_key1Fails_key2Succeeds() throws IOException {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                // 入队响应：Key1 → 401，Key2 → 200 成功（MockWebServer 按 FIFO 顺序派发）
                sim.enqueueError(401, ResponseTemplates.openaiError(401));
                sim.enqueueOpenAISuccess();

                // 构造两个凭证（不同 Key，按优先级升序）
                ChannelCredential key1 = newCredential(101L, "sk-key-1", 1);
                ChannelCredential key2 = newCredential(102L, "sk-key-2", 2);

                SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
                KeyFailoverInvoker invoker = buildInvoker(sim, meterRegistry, key1, key2);

                RoutingContext ctx = new RoutingContext(
                        1L, 1L, sim.getUrl(), Protocol.OPENAI,
                        "sk-test-key", 30, false, "gpt-4", null,
                        FailureStrategy.FAIL_RETRY);

                ProtocolResponse response = invoker.invoke(ctx, createTestRequest("gpt-4", false));

                // 验证故障转移成功：返回的是 Key2 的成功响应
                assertThat(response).isNotNull();
                assertThat(response.getModel()).isEqualTo("gpt-4o");

                // 验证故障转移计数器：Key1(101) 失败应触发一次
                assertThat(meterRegistry.counter("gateway.failover.triggered",
                        "provider", "openai",
                        "from_key", "101",
                        "error_type", "AUTHENTICATION_ERROR").count())
                        .as("Key1 失败应触发一次故障转移计数")
                        .isEqualTo(1.0);
                // 未触发耗尽计数
                assertThat(meterRegistry.counter("gateway.failover.exhausted",
                        "provider", "openai",
                        "channel_id", "1").count())
                        .as("存在可用 Key 时不应触发耗尽计数")
                        .isZero();
            }
        }

        @Test
        @DisplayName("所有 Key 均 401 失败 → 抛出 ProviderException")
        void testKeyFailover_allKeysFail() throws IOException {
            try (ProviderSimulator sim = ProviderSimulator.create()) {
                // 入队响应：两个 Key 均 401
                sim.enqueueError(401, ResponseTemplates.openaiError(401));
                sim.enqueueError(401, ResponseTemplates.openaiError(401));

                ChannelCredential key1 = newCredential(101L, "sk-key-1", 1);
                ChannelCredential key2 = newCredential(102L, "sk-key-2", 2);

                SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
                KeyFailoverInvoker invoker = buildInvoker(sim, meterRegistry, key1, key2);

                RoutingContext ctx = new RoutingContext(
                        1L, 1L, sim.getUrl(), Protocol.OPENAI,
                        "sk-test-key", 30, false, "gpt-4", null,
                        FailureStrategy.FAIL_RETRY);

                assertThatThrownBy(() -> invoker.invoke(ctx, createTestRequest("gpt-4", false)))
                        .isInstanceOf(ProviderException.class)
                        .hasMessageContaining("所有 Key 均失败");

                // 验证故障转移计数器：Key1(101) 失败后尝试 Key2，应触发一次
                assertThat(meterRegistry.counter("gateway.failover.triggered",
                        "provider", "openai",
                        "from_key", "101",
                        "error_type", "AUTHENTICATION_ERROR").count())
                        .as("Key1 失败应触发一次故障转移计数")
                        .isEqualTo(1.0);
                // 验证耗尽计数器：所有 Key 失败应触发一次
                assertThat(meterRegistry.counter("gateway.failover.exhausted",
                        "provider", "openai",
                        "channel_id", "1").count())
                        .as("所有 Key 失败应触发一次耗尽计数")
                        .isEqualTo(1.0);
            }
        }

        // ---------- 辅助方法 ----------

        /**
         * 构造测试凭证
         *
         * @param id      凭证 ID
         * @param apiKey  明文 API Key
         * @param priority 故障转移优先级（数值越小优先级越高）
         * @return 已填充字段的 ChannelCredential
         */
        private ChannelCredential newCredential(Long id, String apiKey, int priority) {
            ChannelCredential cred = new ChannelCredential();
            cred.setId(id);
            cred.setApiKeyPlain(apiKey);
            cred.setChannelId(1L);
            cred.setPriority(priority);
            return cred;
        }

        /**
         * 构造真实 KeyFailoverInvoker，注入 Mock 依赖 + 真实 MeterRegistry
         *
         * <p>Mock 策略：
         * <ul>
         *   <li>{@link CredentialResolver#resolveAll(Long)} 返回传入的凭证列表</li>
         *   <li>{@link UpstreamClientRegistry#getClient} 返回由 ProviderSimulator 创建的真实 OpenAIUpstreamClient</li>
         *   <li>{@link ResilientClientFactory#wrap} 直接返回原始 client（不包装重试/熔断，以便验证真实 HTTP 行为）</li>
         *   <li>{@link ChannelEndpointCircuitBreakerManager#isAvailable} 始终返回 true（不触发端点熔断跳过）</li>
         * </ul></p>
         *
         * @param sim           ProviderSimulator（MockWebServer）
         * @param meterRegistry 真实 MeterRegistry（用于断言计数器）
         * @param creds         可用凭证列表（按优先级排序）
         * @return 真实 KeyFailoverInvoker 实例
         */
        private KeyFailoverInvoker buildInvoker(ProviderSimulator sim,
                                                SimpleMeterRegistry meterRegistry,
                                                ChannelCredential... creds) {
            // 凭证解析：返回传入的凭证列表
            CredentialResolver credentialResolver = mock(CredentialResolver.class);
            when(credentialResolver.resolveAll(anyLong())).thenReturn(List.of(creds));

            // 客户端注册表：用 ProviderSimulator 创建指向 MockWebServer 的真实 OpenAIUpstreamClient
            UpstreamClientRegistry clientRegistry = mock(UpstreamClientRegistry.class);
            when(clientRegistry.getClient(anyString(), anyString(), anyString(), anyInt()))
                    .thenAnswer(inv -> {
                        String apiKey = inv.getArgument(2);
                        int timeout = inv.getArgument(3);
                        return sim.createOpenAIIClient(apiKey, timeout);
                    });

            // 韧性工厂：直接返回原始 client，不包装重试/熔断（隔离验证 KeyFailoverInvoker 自身逻辑）
            ResilientClientFactory resilientClientFactory = mock(ResilientClientFactory.class);
            when(resilientClientFactory.wrap(any(UpstreamClient.class), anyLong()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // 熔断管理器：端点始终可用，避免触发熔断跳过逻辑
            ChannelEndpointCircuitBreakerManager circuitBreakerManager =
                    mock(ChannelEndpointCircuitBreakerManager.class);
            when(circuitBreakerManager.isAvailable(anyLong())).thenReturn(true);

            return new KeyFailoverInvoker(credentialResolver, clientRegistry,
                    resilientClientFactory, circuitBreakerManager, meterRegistry);
        }

        /**
         * 构造简单的 OpenAI 协议请求（参考 SimulatorGatewayIntegrationTest.createTestRequest）
         *
         * @param model  模型名
         * @param stream 是否流式
         * @return 匿名 ProtocolRequest 实现
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
}
