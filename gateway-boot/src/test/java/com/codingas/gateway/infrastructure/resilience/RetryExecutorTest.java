package com.codingas.gateway.infrastructure.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RetryExecutor 单元测试
 */
@DisplayName("重试执行器测试")
class RetryExecutorTest {

    private GatewayRetryProperties properties;
    private RetryExecutor executor;

    @BeforeEach
    void setUp() {
        properties = new GatewayRetryProperties();
        properties.setMaxAttempts(3);
        properties.setBackoffInitial(1); // 测试用最小退避
        properties.setBackoffMultiplier(1.0);
        properties.setRetryableStatusCodes(Set.of(429, 500, 502, 503));
        executor = new RetryExecutor(properties);
    }

    @Nested
    @DisplayName("execute 执行")
    class ExecuteTests {

        @Test
        @DisplayName("首次成功时直接返回结果")
        void execute_firstSuccess_returnsResult() {
            String result = executor.execute(() -> "ok");
            assertThat(result).isEqualTo("ok");
        }

        @Test
        @DisplayName("重试后成功时返回结果")
        void execute_retryThenSuccess_returnsResult() {
            AtomicInteger counter = new AtomicInteger(0);
            String result = executor.execute(() -> {
                if (counter.incrementAndGet() < 3) {
                    throw new RetryableException("上游 429 限流");
                }
                return "ok";
            });
            assertThat(result).isEqualTo("ok");
            assertThat(counter.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("不可重试异常立即抛出")
        void execute_nonRetryable_throwsImmediately() {
            AtomicInteger counter = new AtomicInteger(0);
            assertThatThrownBy(() -> executor.execute(() -> {
                counter.incrementAndGet();
                throw new IllegalArgumentException("参数错误");
            })).isInstanceOf(IllegalArgumentException.class);

            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("重试耗尽后抛出最后一次异常")
        void execute_retryExhausted_throwsLastException() {
            assertThatThrownBy(() -> executor.execute(() -> {
                throw new RetryableException("上游持续 500");
            })).isInstanceOf(RetryableException.class)
              .hasMessageContaining("500");
        }
    }

    @Nested
    @DisplayName("isRetryable 判断")
    class IsRetryableTests {

        @Test
        @DisplayName("RetryableException 是可重试的")
        void isRetryable_retryableException_returnsTrue() {
            assertThat(executor.isRetryable(new RetryableException("429"))).isTrue();
        }

        @Test
        @DisplayName("普通异常不可重试")
        void isRetryable_plainException_returnsFalse() {
            assertThat(executor.isRetryable(new IllegalArgumentException("bad"))).isFalse();
        }
    }

    @Nested
    @DisplayName("calculateDelay 退避计算")
    class CalculateDelayTests {

        @Test
        @DisplayName("第一次重试延迟为 backoffInitial")
        void calculateDelay_firstAttempt_returnsInitial() {
            assertThat(executor.calculateDelay(1)).isEqualTo(1);
        }

        @Test
        @DisplayName("退避按倍数增长")
        void calculateDelay_withMultiplier_grows() {
            properties.setBackoffInitial(1000);
            properties.setBackoffMultiplier(2.0);
            RetryExecutor exec = new RetryExecutor(properties);
            assertThat(exec.calculateDelay(1)).isEqualTo(1000);
            assertThat(exec.calculateDelay(2)).isEqualTo(2000);
            assertThat(exec.calculateDelay(3)).isEqualTo(4000);
        }
    }
}