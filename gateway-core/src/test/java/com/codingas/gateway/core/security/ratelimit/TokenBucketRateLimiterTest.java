package com.codingas.gateway.core.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryTokenBucketRateLimiter 单元测试
 */
@DisplayName("InMemoryTokenBucketRateLimiter 测试")
class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryTokenBucketRateLimiter();
    }

    @Test
    @DisplayName("tryAcquire 返回 true 当令牌充足时")
    void tryAcquire_whenTokensAvailable_returnsTrue() {
        // when
        boolean result = rateLimiter.tryAcquire("user:123", 10, 5, 1);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("tryAcquire 返回 false 当令牌不足时")
    void tryAcquire_whenNoTokens_returnsFalse() {
        // given: 消耗完所有令牌
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire("user:456", 10, 5, 1);
        }

        // when: 再请求一个令牌
        boolean result = rateLimiter.tryAcquire("user:456", 10, 5, 1);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("令牌随时间补充")
    void tryAcquire_tokensRefillOverTime() throws InterruptedException {
        // given: 消耗完所有令牌
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("user:789", 10, 10, 1);
        }

        // when: 等待 200ms，应该补充约2个令牌
        Thread.sleep(200);
        boolean result = rateLimiter.tryAcquire("user:789", 10, 10, 1);

        // then: 补充了令牌，应该能获取
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("reset 清除桶")
    void reset_clearsBucket() {
        // given: 消耗部分令牌
        rateLimiter.tryAcquire("user:reset", 10, 5, 1);
        rateLimiter.tryAcquire("user:reset", 10, 5, 1);

        // when
        rateLimiter.reset("user:reset");

        // then: 重置后应该能再次获取
        assertThat(rateLimiter.tryAcquire("user:reset", 10, 5, 1)).isTrue();
    }

    @Test
    @DisplayName("不同 key 独立计数")
    void tryAcquire_differentKeysIndependent() {
        // when
        boolean user1 = rateLimiter.tryAcquire("user:1", 5, 1, 1);
        boolean user2 = rateLimiter.tryAcquire("user:2", 5, 1, 1);

        // then: 两个独立桶
        assertThat(user1).isTrue();
        assertThat(user2).isTrue();

        // 消耗 user:1 的令牌
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("user:1", 5, 1, 1);
        }

        // user:1 应该被限流，user:2 不受影响
        assertThat(rateLimiter.tryAcquire("user:1", 5, 1, 1)).isFalse();
        assertThat(rateLimiter.tryAcquire("user:2", 5, 1, 1)).isTrue();
    }
}