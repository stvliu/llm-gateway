package com.codingas.gateway.infrastructure.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CircuitBreaker 单元测试
 */
@DisplayName("熔断器测试")
class CircuitBreakerTest {

    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        // 失败率阈值 50%, 滑动窗口 10, OPEN 持续 1ms (测试用), HALF_OPEN 试探 3
        breaker = new CircuitBreaker(0.5, 10, 1, 3);
    }

    @Nested
    @DisplayName("初始状态")
    class InitialStateTests {

        @Test
        @DisplayName("初始状态为 CLOSED")
        void shouldBeClosedInitially() {
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
            assertThat(breaker.allowRequest()).isTrue();
        }
    }

    @Nested
    @DisplayName("CLOSED → OPEN 转换")
    class ClosedToOpenTests {

        @Test
        @DisplayName("失败率超阈值时触发 OPEN")
        void shouldOpenWhenFailureRateExceedsThreshold() {
            for (int i = 0; i < 10; i++) {
                breaker.recordFailure();
            }
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
            assertThat(breaker.allowRequest()).isFalse();
        }

        @Test
        @DisplayName("全部成功时保持 CLOSED")
        void shouldStayClosedOnSuccess() {
            for (int i = 0; i < 10; i++) {
                breaker.recordSuccess();
            }
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }
    }

    @Nested
    @DisplayName("OPEN → HALF_OPEN 转换")
    class OpenToHalfOpenTests {

        @Test
        @DisplayName("OPEN 超时后进入 HALF_OPEN")
        void shouldTransitionToHalfOpenAfterTimeout() throws InterruptedException {
            for (int i = 0; i < 10; i++) {
                breaker.recordFailure();
            }
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);

            Thread.sleep(10); // 等待 OPEN 持续时间过期
            assertThat(breaker.allowRequest()).isTrue();
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);
        }
    }

    @Nested
    @DisplayName("HALF_OPEN 状态行为")
    class HalfOpenTests {

        @Test
        @DisplayName("HALF_OPEN 中成功则恢复 CLOSED")
        void shouldCloseOnHalfOpenSuccess() throws InterruptedException {
            for (int i = 0; i < 10; i++) {
                breaker.recordFailure();
            }
            Thread.sleep(10);
            breaker.allowRequest(); // 触发进入 HALF_OPEN
            breaker.recordSuccess();
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }

        @Test
        @DisplayName("HALF_OPEN 中失败则重新 OPEN")
        void shouldReOpenOnHalfOpenFailure() throws InterruptedException {
            for (int i = 0; i < 10; i++) {
                breaker.recordFailure();
            }
            Thread.sleep(10);
            breaker.allowRequest(); // 触发进入 HALF_OPEN
            breaker.recordFailure();
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        }
    }
}