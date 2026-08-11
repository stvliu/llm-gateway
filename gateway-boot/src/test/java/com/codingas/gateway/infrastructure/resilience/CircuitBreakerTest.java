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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CircuitBreaker 单元测试
 */
@DisplayName("熔断器测试")
class CircuitBreakerTest {

    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        // 失败率 50%，窗口 5，OPEN 持续 1 秒，半开最大探测 3 次
        breaker = new CircuitBreaker(0.5, 5, 1000, 3);
    }

    @Nested
    @DisplayName("CLOSED 状态")
    class ClosedState {

        @Test
        @DisplayName("初始状态为 CLOSED")
        void initialState_isClosed() {
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }

        @Test
        @DisplayName("CLOSED 状态下允许请求通过")
        void closedState_allowsRequests() {
            assertThat(breaker.allowRequest()).isTrue();
        }

        @Test
        @DisplayName("全部成功时保持 CLOSED")
        void allSuccess_staysClosed() {
            for (int i = 0; i < 5; i++) {
                breaker.recordSuccess();
            }
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }
    }

    @Nested
    @DisplayName("CLOSED→OPEN 转换")
    class ClosedToOpen {

        @Test
        @DisplayName("失败率达到阈值时熔断")
        void failureRateExceedsThreshold_tripsOpen() {
            // 窗口 5，阈值 50%，3/5 失败 = 60% > 50% → OPEN
            breaker.recordSuccess();
            breaker.recordFailure();
            breaker.recordFailure();
            breaker.recordFailure();
            breaker.recordSuccess();

            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        }

        @Test
        @DisplayName("失败率未达阈值时保持 CLOSED")
        void failureRateBelowThreshold_staysClosed() {
            // 2/5 失败 = 40% < 50% → CLOSED
            breaker.recordSuccess();
            breaker.recordFailure();
            breaker.recordSuccess();
            breaker.recordFailure();
            breaker.recordSuccess();

            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }
    }

    @Nested
    @DisplayName("OPEN 状态")
    class OpenState {

        @BeforeEach
        void tripBreaker() {
            // 触发熔断
            for (int i = 0; i < 5; i++) {
                breaker.recordFailure();
            }
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        }

        @Test
        @DisplayName("OPEN 状态下拒绝请求")
        void openState_rejectsRequests() {
            assertThat(breaker.allowRequest()).isFalse();
        }
    }

    @Nested
    @DisplayName("OPEN→HALF_OPEN 转换")
    class OpenToHalfOpen {

        @Test
        @DisplayName("超时后转为 HALF_OPEN")
        void timeoutTransitions_toHalfOpen() throws InterruptedException {
            // 使用短超时
            CircuitBreaker shortBreaker = new CircuitBreaker(0.5, 5, 100, 3);
            for (int i = 0; i < 5; i++) {
                shortBreaker.recordFailure();
            }
            assertThat(shortBreaker.getState()).isEqualTo(CircuitBreakerState.OPEN);

            Thread.sleep(150);
            assertThat(shortBreaker.allowRequest()).isTrue();
            assertThat(shortBreaker.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);
        }
    }

    @Nested
    @DisplayName("HALF_OPEN 状态")
    class HalfOpen {

        private CircuitBreaker halfOpenBreaker;

        @BeforeEach
        void setUpHalfOpen() throws InterruptedException {
            halfOpenBreaker = new CircuitBreaker(0.5, 5, 100, 3);
            for (int i = 0; i < 5; i++) {
                halfOpenBreaker.recordFailure();
            }
            Thread.sleep(150);
            halfOpenBreaker.allowRequest(); // 触发 HALF_OPEN
        }

        @Test
        @DisplayName("HALF_OPEN 中成功探测 → CLOSED")
        void halfOpen_success_closesCircuit() {
            halfOpenBreaker.recordSuccess();
            assertThat(halfOpenBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }

        @Test
        @DisplayName("HALF_OPEN 中失败探测 → OPEN")
        void halfOpen_failure_reopensCircuit() {
            halfOpenBreaker.recordFailure();
            assertThat(halfOpenBreaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        }

        @Test
        @DisplayName("HALF_OPEN 限制探测次数")
        void halfOpen_limitedAttempts() {
            assertThat(halfOpenBreaker.allowRequest()).isTrue();  // 第 1 次
            assertThat(halfOpenBreaker.allowRequest()).isTrue();  // 第 2 次
            assertThat(halfOpenBreaker.allowRequest()).isTrue();  // 第 3 次
            assertThat(halfOpenBreaker.allowRequest()).isFalse(); // 超限
        }
    }

    @Nested
    @DisplayName("滑动窗口")
    class SlidingWindow {

        @Test
        @DisplayName("窗口满时淘汰旧记录")
        void windowEviction_oldRecordsRemoved() {
            // 窗口大小 5
            breaker.recordSuccess(); // 1
            breaker.recordSuccess(); // 2
            breaker.recordSuccess(); // 3
            breaker.recordSuccess(); // 4
            breaker.recordSuccess(); // 5 - 窗口满

            // 第 6 条记录进入，第 1 条被淘汰
            breaker.recordFailure(); // 淘汰 success，新增 failure → 1/5 = 20%

            // 再加 2 条 failure → 3/5 = 60% > 50% → OPEN
            breaker.recordFailure();
            breaker.recordFailure();

            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        }
    }

    @Nested
    @DisplayName("应急强制操作")
    class ForceOperations {

        @Test
        @DisplayName("forceOpen 直接将熔断器置为 OPEN，无需触发失败")
        void forceOpen_transitionsToOpenImmediately() {
            // 初始 CLOSED，无任何 recordFailure
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);

            breaker.forceOpen();

            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
            // OPEN 状态下拒绝请求
            assertThat(breaker.allowRequest()).isFalse();
        }

        @Test
        @DisplayName("forceClose 将熔断器置为 CLOSED 并重置统计窗口")
        void forceClose_transitionsToClosedAndResetsWindow() throws InterruptedException {
            // 先触发 OPEN
            for (int i = 0; i < 5; i++) {
                breaker.recordFailure();
            }
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);

            breaker.forceClose();

            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
            // 恢复后允许请求通过
            assertThat(breaker.allowRequest()).isTrue();
            // 重置后窗口清空，单次失败不应触发再次熔断（窗口未满）
            breaker.recordFailure();
            assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }

        @Test
        @DisplayName("forceClose 在 HALF_OPEN 状态下也能恢复为 CLOSED")
        void forceClose_fromHalfOpen_resetsToClosed() throws InterruptedException {
            CircuitBreaker halfOpenBreaker = new CircuitBreaker(0.5, 5, 100, 3);
            for (int i = 0; i < 5; i++) {
                halfOpenBreaker.recordFailure();
            }
            Thread.sleep(150);
            halfOpenBreaker.allowRequest(); // 触发 HALF_OPEN
            assertThat(halfOpenBreaker.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);

            halfOpenBreaker.forceClose();

            assertThat(halfOpenBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        }
    }

    @Nested
    @DisplayName("并发安全")
    class Concurrency {

        @Test
        @DisplayName("多线程并发记录不会崩溃")
        void concurrentRecords_noCrash() throws Exception {
            CircuitBreaker concurrentBreaker = new CircuitBreaker(0.5, 100, 30000, 3);
            int threadCount = 10;
            int recordsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final boolean success = t % 2 == 0;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < recordsPerThread; i++) {
                            if (success) {
                                concurrentBreaker.recordSuccess();
                            } else {
                                concurrentBreaker.recordFailure();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();
            assertThat(completed).isTrue();
            // 状态应该是 CLOSED 或 OPEN 之一（不会崩溃或进入非法状态）
            assertThat(concurrentBreaker.getState()).isNotNull();
        }

        @Test
        @DisplayName("多线程并发 allowRequest 不会崩溃")
        void concurrentAllowRequest_noCrash() throws Exception {
            // 先触发 OPEN
            for (int i = 0; i < 5; i++) {
                breaker.recordFailure();
            }

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        results.add(breaker.allowRequest());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();
            assertThat(completed).isTrue();
            // OPEN 状态下所有请求应被拒绝
            assertThat(results).allMatch(r -> !r);
        }
    }
}
