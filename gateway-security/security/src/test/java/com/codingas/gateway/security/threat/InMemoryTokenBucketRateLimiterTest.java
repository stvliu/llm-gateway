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
package com.codingas.gateway.security.threat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryTokenBucketRateLimiter 单元测试
 *
 * <p>纯内存令牌桶：直接实例化，验证首次取令牌、消费、拒绝、时间补充与重置。</p>
 */
@DisplayName("InMemoryTokenBucketRateLimiter 测试")
class InMemoryTokenBucketRateLimiterTest {

    private final InMemoryTokenBucketRateLimiter limiter = new InMemoryTokenBucketRateLimiter();

    @Test
    @DisplayName("首次请求按容量填充并允许")
    void tryAcquire_firstRequest_fillsBucketAndAllows() {
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 3)).isTrue();
    }

    @Test
    @DisplayName("桶内令牌不足时拒绝")
    void tryAcquire_insufficientTokens_rejects() {
        // 容量 5，先取 5 个清空桶
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 5)).isTrue();
        // 再取 1 个应拒绝
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 1)).isFalse();
    }

    @Test
    @DisplayName("恰好消耗全部令牌后不再放行")
    void tryAcquire_exactConsumption_thenRejects() {
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 5)).isTrue();
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 1)).isFalse();
    }

    @Test
    @DisplayName("经过 1 秒后按补充速率补充令牌")
    void tryAcquire_afterOneSecond_refillsTokens() throws InterruptedException {
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 5)).isTrue();
        // 等待跨过秒边界，补充 10/秒 → 桶补满至 5
        Thread.sleep(1100);
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 1)).isTrue();
    }

    @Test
    @DisplayName("getStatus 无状态时返回满桶")
    void getStatus_noState_returnsFullBucket() {
        TokenBucketStatus status = limiter.getStatus("ip:2", 5, 10);

        assertThat(status.currentTokens()).isEqualTo(5);
        assertThat(status.capacity()).isEqualTo(5);
        assertThat(status.refillRate()).isEqualTo(10);
    }

    @Test
    @DisplayName("getStatus 反映消费后的当前令牌数")
    void getStatus_afterConsumption_returnsRemaining() {
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 2)).isTrue();

        TokenBucketStatus status = limiter.getStatus("ip:1", 5, 10);

        assertThat(status.currentTokens()).isEqualTo(3);
        assertThat(status.capacity()).isEqualTo(5);
    }

    @Test
    @DisplayName("getStatus 考虑时间补充")
    void getStatus_withElapsed_reflectsRefill() throws InterruptedException {
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 5)).isTrue();
        Thread.sleep(1100);

        TokenBucketStatus status = limiter.getStatus("ip:1", 5, 10);

        // 补充后恢复满桶
        assertThat(status.currentTokens()).isEqualTo(5);
    }

    @Test
    @DisplayName("reset 移除桶状态")
    void reset_removesBucket() {
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 5)).isTrue();
        limiter.reset("ip:1");

        // 重置后按新桶处理（满桶）
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 5)).isTrue();
    }

    @Test
    @DisplayName("不同 key 相互独立")
    void tryAcquire_distinctKeys_areIsolated() {
        assertThat(limiter.tryAcquire("ip:1", 5, 10, 5)).isTrue();
        // 另一 key 仍是满桶
        assertThat(limiter.tryAcquire("ip:2", 5, 10, 5)).isTrue();
    }
}
