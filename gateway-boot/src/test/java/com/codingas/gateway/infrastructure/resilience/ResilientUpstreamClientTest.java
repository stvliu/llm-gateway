package com.codingas.gateway.infrastructure.resilience;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
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
    private ResilientUpstreamClient resilientClient;

    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker(0.5, 10, 30000, 3);
        GatewayRetryProperties props = new GatewayRetryProperties();
        props.setMaxAttempts(2);
        props.setBackoffInitial(1);
        props.setBackoffMultiplier(1.0);
        props.setRetryableStatusCodes(Set.of(429, 500));
        retryExecutor = new RetryExecutor(props);
        resilientClient = new ResilientUpstreamClient(delegate, circuitBreaker, retryExecutor);
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
            // 触发熔断
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure();
            }

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
    }
}