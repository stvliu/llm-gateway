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
package com.codingas.gateway.resilience.retry;

import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.transport.UpstreamException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        properties = new GatewayRetryProperties();
        properties.setMaxAttempts(3);
        properties.setRetryableStatusCodes(Set.of(429, 500, 502, 503));
        meterRegistry = new SimpleMeterRegistry();
        executor = new RetryExecutor(properties, meterRegistry);
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

        @Test
        @DisplayName("UpstreamException RATE_LIMIT_ERROR 重试")
        void rateLimit_retriesWithStrategy() {
            properties.getRateLimit().setMaxAttempts(3);
            properties.getRateLimit().setBackoffInitial(1);
            executor = new RetryExecutor(properties, new SimpleMeterRegistry());
            AtomicInteger counter = new AtomicInteger(0);
            String result = executor.execute(() -> {
                if (counter.incrementAndGet() < 3) {
                    throw new UpstreamException(ProviderErrorType.RATE_LIMIT_ERROR, "429 限流");
                }
                return "ok";
            });
            assertThat(result).isEqualTo("ok");
            assertThat(counter.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("QUOTA_EXCEEDED 不可重试")
        void quotaExceeded_notRetryable() {
            AtomicInteger counter = new AtomicInteger(0);
            assertThatThrownBy(() -> executor.execute(() -> {
                counter.incrementAndGet();
                throw new UpstreamException(ProviderErrorType.QUOTA_EXCEEDED, "配额超限");
            })).isInstanceOf(UpstreamException.class);
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("AUTHENTICATION_ERROR 不可重试")
        void authenticationError_notRetryable() {
            AtomicInteger counter = new AtomicInteger(0);
            assertThatThrownBy(() -> executor.execute(() -> {
                counter.incrementAndGet();
                throw new UpstreamException(ProviderErrorType.AUTHENTICATION_ERROR, "401");
            })).isInstanceOf(UpstreamException.class);
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("INVALID_REQUEST 不可重试")
        void invalidRequest_notRetryable() {
            AtomicInteger counter = new AtomicInteger(0);
            assertThatThrownBy(() -> executor.execute(() -> {
                counter.incrementAndGet();
                throw new UpstreamException(ProviderErrorType.INVALID_REQUEST, "400");
            })).isInstanceOf(UpstreamException.class);
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("TIMEOUT_ERROR 走快速重试策略重试后成功")
        void timeoutError_retriesWithFastRetry() {
            properties.getFastRetry().setBackoffFixed(1);
            executor = new RetryExecutor(properties, new SimpleMeterRegistry());
            AtomicInteger counter = new AtomicInteger(0);
            String result = executor.execute(() -> {
                if (counter.incrementAndGet() < 3) {
                    throw new UpstreamException(ProviderErrorType.TIMEOUT_ERROR, "504 超时");
                }
                return "ok";
            });
            assertThat(result).isEqualTo("ok");
            assertThat(counter.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("UPSTREAM_ERROR 走服务不可用策略重试后成功")
        void upstreamError_retriesWithServiceUnavailable() {
            properties.getServiceUnavailable().setBackoffFixed(1);
            executor = new RetryExecutor(properties, new SimpleMeterRegistry());
            AtomicInteger counter = new AtomicInteger(0);
            String result = executor.execute(() -> {
                if (counter.incrementAndGet() < 3) {
                    throw new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "502 上游错误");
                }
                return "ok";
            });
            assertThat(result).isEqualTo("ok");
            assertThat(counter.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("UNKNOWN_ERROR 走指数退避策略重试后成功")
        void unknownError_retriesWithExponentialBackoff() {
            properties.setBackoffInitial(1);
            properties.setBackoffMultiplier(1.0);
            executor = new RetryExecutor(properties, new SimpleMeterRegistry());
            AtomicInteger counter = new AtomicInteger(0);
            String result = executor.execute(() -> {
                if (counter.incrementAndGet() < 3) {
                    throw new UpstreamException(ProviderErrorType.UNKNOWN_ERROR, "未知错误");
                }
                return "ok";
            });
            assertThat(result).isEqualTo("ok");
            assertThat(counter.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("消息包含可重试状态码的普通异常可重试")
        void messageContainsRetryableStatusCode_retries() {
            properties.setBackoffInitial(1);
            properties.setBackoffMultiplier(1.0);
            executor = new RetryExecutor(properties, new SimpleMeterRegistry());
            AtomicInteger counter = new AtomicInteger(0);
            String result = executor.execute(() -> {
                if (counter.incrementAndGet() < 3) {
                    throw new RuntimeException("上游返回 HTTP 500");
                }
                return "ok";
            });
            assertThat(result).isEqualTo("ok");
            assertThat(counter.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("消息为 null 的普通异常不可重试")
        void messageNull_notRetryable() {
            AtomicInteger counter = new AtomicInteger(0);
            assertThatThrownBy(() -> executor.execute(() -> {
                counter.incrementAndGet();
                throw new RuntimeException();
            })).isInstanceOf(RuntimeException.class);
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("重试耗尽后记录 exhausted Metrics")
        void execute_exhausted_recordsExhaustedMetric() {
            properties.getServiceUnavailable().setBackoffFixed(1);
            MeterRegistry freshRegistry = new SimpleMeterRegistry();
            executor = new RetryExecutor(properties, freshRegistry);

            assertThatThrownBy(() -> executor.execute(() -> {
                throw new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "持续 502");
            })).isInstanceOf(UpstreamException.class);

            assertThat(freshRegistry.counter("gateway.retry.exhausted",
                    "error_type", "UPSTREAM_ERROR").count()).isPositive();
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

        @Test
        @DisplayName("UpstreamException RATE_LIMIT_ERROR 可重试")
        void isRetryable_rateLimitError_returnsTrue() {
            assertThat(executor.isRetryable(
                new UpstreamException(ProviderErrorType.RATE_LIMIT_ERROR, "429"))).isTrue();
        }

        @Test
        @DisplayName("UpstreamException QUOTA_EXCEEDED 不可重试")
        void isRetryable_quotaExceeded_returnsFalse() {
            assertThat(executor.isRetryable(
                new UpstreamException(ProviderErrorType.QUOTA_EXCEEDED, "配额超限"))).isFalse();
        }

        @Test
        @DisplayName("UpstreamException INVALID_REQUEST 不可重试")
        void isRetryable_invalidRequest_returnsFalse() {
            assertThat(executor.isRetryable(
                new UpstreamException(ProviderErrorType.INVALID_REQUEST, "400"))).isFalse();
        }

        @Test
        @DisplayName("消息包含可重试状态码的普通异常可重试")
        void isRetryable_messageContainsStatusCode_returnsTrue() {
            assertThat(executor.isRetryable(new RuntimeException("上游 500 错误"))).isTrue();
        }
    }
}
