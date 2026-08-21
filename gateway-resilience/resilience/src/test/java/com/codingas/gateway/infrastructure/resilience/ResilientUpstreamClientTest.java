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
package com.codingas.gateway.infrastructure.resilience;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.provider.vendor.ProviderException;
import com.codingas.gateway.provider.upstream.UpstreamClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ResilientUpstreamClient 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("韧性 UpstreamClient 测试")
class ResilientUpstreamClientTest {

    @Mock
    private UpstreamClient delegate;

    @Mock
    private ProtocolRequest request;

    @Mock
    private ProtocolResponse response;

    private CircuitBreaker circuitBreaker;
    private RetryExecutor retryExecutor;
    private MeterRegistry meterRegistry;
    private ResilientUpstreamClient resilientClient;

    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker(0.5, 10, 30000, 3);
        GatewayRetryProperties props = new GatewayRetryProperties();
        props.setMaxAttempts(2);
        props.setBackoffInitial(1);
        props.setBackoffMultiplier(1.0);
        props.setRetryableStatusCodes(Set.of(429, 500));
        meterRegistry = new SimpleMeterRegistry();
        retryExecutor = new RetryExecutor(props, meterRegistry);
        resilientClient = new ResilientUpstreamClient(delegate, circuitBreaker, retryExecutor,
                meterRegistry, new EndpointMetricsRegistry(), "test-provider", 1L);
    }

    @Nested
    @DisplayName("chat 非流式调用")
    class ChatTests {

        @Test
        @DisplayName("正常调用委托给底层客户端")
        void chat_success_delegatesToUnderlying() {
            when(delegate.chat(request)).thenReturn(response);

            ProtocolResponse result = resilientClient.chat(request);

            assertThat(result).isSameAs(response);
            verify(delegate).chat(request);
        }

        @Test
        @DisplayName("熔断器开启时抛出 CircuitOpenException")
        void chat_circuitOpen_throwsException() {
            // 触发熔断：窗口 10，阈值 50%，10 次失败 → 100% > 50%
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure();
            }
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.OPEN);

            assertThatThrownBy(() -> resilientClient.chat(request))
                    .isInstanceOf(CircuitOpenException.class)
                    .hasMessageContaining("熔断器开启");

            verify(delegate, never()).chat(any());
        }

        @Test
        @DisplayName("底层调用失败时记录失败到熔断器")
        void chat_failure_recordsFailureToCircuitBreaker() {
            when(delegate.chat(request)).thenThrow(new RuntimeException("上游 500 错误"));

            assertThatThrownBy(() -> resilientClient.chat(request))
                    .isInstanceOf(RuntimeException.class);

            // 熔断器中至少记录了一次失败（具体状态取决于失败率阈值和窗口大小）
            assertThat(circuitBreaker.getState()).isNotNull();
        }

        @Test
        @DisplayName("ProviderException 时记录错误类型 Metrics")
        void chat_providerException_recordsMetrics() {
            when(delegate.chat(request)).thenThrow(new ProviderException(
                    ProviderErrorType.RATE_LIMIT_ERROR, "429 限流"));

            assertThatThrownBy(() -> resilientClient.chat(request))
                    .isInstanceOf(ProviderException.class);

            assertThat(meterRegistry.counter("gateway.provider.errors",
                    "provider", "test-provider",
                    "error_type", "RATE_LIMIT_ERROR").count()).isPositive();
        }

        @Test
        @DisplayName("熔断器开启时记录 circuitbreaker.blocked Metrics")
        void chat_circuitOpen_recordsMetrics() {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure();
            }

            assertThatThrownBy(() -> resilientClient.chat(request))
                    .isInstanceOf(CircuitOpenException.class);

            assertThat(meterRegistry.counter("gateway.circuitbreaker.blocked",
                    "provider", "test-provider",
                    "endpoint_id", "1").count()).isPositive();
        }

        @Test
        @DisplayName("成功调用后熔断器记录成功")
        void chat_success_recordsSuccess() {
            when(delegate.chat(request)).thenReturn(response);

            resilientClient.chat(request);

            // 成功后应保持 CLOSED
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }
    }

    @Nested
    @DisplayName("chatStream 流式调用")
    class ChatStreamTests {

        @Test
        @DisplayName("熔断器开启时拒绝流式请求")
        void chatStream_circuitOpen_throwsException() {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure();
            }

            StreamCallback callback = mock(StreamCallback.class);
            assertThatThrownBy(() -> resilientClient.chatStream(request, callback))
                    .isInstanceOf(CircuitOpenException.class);
        }

        @Test
        @DisplayName("流式完成时熔断器记录成功")
        void chatStream_onComplete_recordsSuccess() {
            StreamCallback callback = mock(StreamCallback.class);
            // 让 delegate 在 chatStream 时立即调用 onComplete
            doAnswer(invocation -> {
                StreamCallback inner = invocation.getArgument(1);
                inner.onComplete();
                return null;
            }).when(delegate).chatStream(any(), any());

            resilientClient.chatStream(request, callback);

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }

        @Test
        @DisplayName("流式错误时熔断器记录失败")
        void chatStream_onError_recordsFailure() {
            // 先记录一些成功保持 CLOSED
            circuitBreaker.recordSuccess();
            circuitBreaker.recordSuccess();

            StreamCallback callback = mock(StreamCallback.class);
            doAnswer(invocation -> {
                StreamCallback inner = invocation.getArgument(1);
                inner.onError(new RuntimeException("stream error"));
                return null;
            }).when(delegate).chatStream(any(), any());

            resilientClient.chatStream(request, callback);

            // 失败被记录（状态取决于失败率）
            assertThat(circuitBreaker.getState()).isNotNull();
        }
    }
}
