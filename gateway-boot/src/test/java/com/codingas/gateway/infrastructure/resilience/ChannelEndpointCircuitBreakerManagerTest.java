package com.codingas.gateway.infrastructure.resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChannelEndpointCircuitBreakerManager 单元测试
 */
@DisplayName("端点熔断器管理器测试")
class ChannelEndpointCircuitBreakerManagerTest {

    @Nested
    @DisplayName("getBreaker 获取熔断器")
    class GetBreakerTests {

        @Test
        @DisplayName("同一端点返回同一熔断器实例")
        void shouldReturnSameBreakerForSameEndpoint() {
            ChannelEndpointCircuitBreakerManager manager = new ChannelEndpointCircuitBreakerManager();
            CircuitBreaker b1 = manager.getBreaker(1L);
            CircuitBreaker b2 = manager.getBreaker(1L);
            assertThat(b1).isSameAs(b2);
        }

        @Test
        @DisplayName("不同端点返回不同熔断器实例")
        void shouldReturnDifferentBreakerForDifferentEndpoint() {
            ChannelEndpointCircuitBreakerManager manager = new ChannelEndpointCircuitBreakerManager();
            CircuitBreaker b1 = manager.getBreaker(1L);
            CircuitBreaker b2 = manager.getBreaker(2L);
            assertThat(b1).isNotSameAs(b2);
        }
    }

    @Nested
    @DisplayName("isAvailable 可用性判断")
    class IsAvailableTests {

        @Test
        @DisplayName("新端点默认可用")
        void shouldBeAvailableByDefault() {
            ChannelEndpointCircuitBreakerManager manager = new ChannelEndpointCircuitBreakerManager();
            assertThat(manager.isAvailable(1L)).isTrue();
        }
    }

    @Nested
    @DisplayName("应急强制操作转发")
    class ForceOperationTests {

        @Test
        @DisplayName("forceOpen 将端点熔断器置为 OPEN")
        void forceOpen_transitionsEndpointBreakerToOpen() {
            ChannelEndpointCircuitBreakerManager manager = new ChannelEndpointCircuitBreakerManager();
            assertThat(manager.getState(1L)).isEqualTo(CircuitBreakerState.CLOSED);

            manager.forceOpen(1L);

            assertThat(manager.getState(1L)).isEqualTo(CircuitBreakerState.OPEN);
            assertThat(manager.isAvailable(1L)).isFalse();
        }

        @Test
        @DisplayName("forceClose 将端点熔断器置为 CLOSED 并恢复可用")
        void forceClose_transitionsEndpointBreakerToClosed() {
            ChannelEndpointCircuitBreakerManager manager = new ChannelEndpointCircuitBreakerManager();
            manager.forceOpen(1L);
            assertThat(manager.isAvailable(1L)).isFalse();

            manager.forceClose(1L);

            assertThat(manager.getState(1L)).isEqualTo(CircuitBreakerState.CLOSED);
            assertThat(manager.isAvailable(1L)).isTrue();
        }
    }
}