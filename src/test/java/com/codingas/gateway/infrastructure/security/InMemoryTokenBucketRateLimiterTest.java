package com.codingas.gateway.infrastructure.security;

import com.codingas.gateway.domain.security.service.TokenBucketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryTokenBucketRateLimiter 单元测试
 */
@DisplayName("InMemoryTokenBucketRateLimiter")
class InMemoryTokenBucketRateLimiterTest {

    private final InMemoryTokenBucketRateLimiter rateLimiter = new InMemoryTokenBucketRateLimiter();

    private static final int CAPACITY = 10;
    private static final int REFILL_RATE = 1; // 每秒补充 1 个令牌

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquireTests {

        @Test
        @DisplayName("首次请求成功获取令牌")
        void firstRequest_acquiresSuccessfully() {
            boolean result = rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("容量内请求成功")
        void requestsWithinCapacity_succeed() {
            for (int i = 0; i < CAPACITY; i++) {
                boolean result = rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);
                assertThat(result).isTrue();
            }
        }

        @Test
        @DisplayName("超过容量后请求失败")
        void exceededCapacity_requestFails() {
            // 耗尽所有令牌
            for (int i = 0; i < CAPACITY; i++) {
                rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);
            }

            // 再请求应该失败
            boolean result = rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("请求多个令牌")
        void requestMultipleTokens() {
            boolean result = rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 5);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不同 key 独立限流")
        void differentKeys_independentLimits() {
            // 耗尽 user:123 的令牌
            for (int i = 0; i < CAPACITY; i++) {
                rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);
            }

            // user:456 仍可获取
            boolean result = rateLimiter.tryAcquire("user:456", CAPACITY, REFILL_RATE, 1);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatusTests {

        @Test
        @DisplayName("未使用的桶返回满容量")
        void unusedBucket_returnsFullCapacity() {
            TokenBucketStatus status = rateLimiter.getStatus("user:123", CAPACITY, REFILL_RATE);

            assertThat(status.currentTokens()).isEqualTo(CAPACITY);
            assertThat(status.capacity()).isEqualTo(CAPACITY);
            assertThat(status.refillRate()).isEqualTo(REFILL_RATE);
        }

        @Test
        @DisplayName("部分使用后返回正确剩余")
        void partiallyUsed_returnsCorrectRemaining() {
            rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 3);

            TokenBucketStatus status = rateLimiter.getStatus("user:123", CAPACITY, REFILL_RATE);

            assertThat(status.currentTokens()).isEqualTo(CAPACITY - 3);
        }

        @Test
        @DisplayName("不存在的桶返回满容量")
        void nonExistentBucket_returnsFullCapacity() {
            TokenBucketStatus status = rateLimiter.getStatus("nonexistent", CAPACITY, REFILL_RATE);

            assertThat(status.currentTokens()).isEqualTo(CAPACITY);
            assertThat(status.capacity()).isEqualTo(CAPACITY);
        }
    }

    @Nested
    @DisplayName("reset")
    class ResetTests {

        @Test
        @DisplayName("重置后令牌桶恢复满容量")
        void afterReset_bucketRefills() {
            // 耗尽令牌
            for (int i = 0; i < CAPACITY; i++) {
                rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);
            }

            // 重置
            rateLimiter.reset("user:123");

            TokenBucketStatus status = rateLimiter.getStatus("user:123", CAPACITY, REFILL_RATE);
            assertThat(status.currentTokens()).isEqualTo(CAPACITY);
        }

        @Test
        @DisplayName("重置不存在的桶不抛异常")
        void resetNonExistent_doesNotThrow() {
            rateLimiter.reset("nonexistent");
        }

        @Test
        @DisplayName("重置后可以重新获取令牌")
        void afterReset_canAcquireAgain() {
            rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);
            rateLimiter.reset("user:123");

            boolean result = rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("令牌桶耗尽场景")
    class ExhaustedBucketTests {

        @Test
        @DisplayName("耗尽后需要等待补充")
        void exhaustedBucket_requiresWaitingForRefill() {
            // 耗尽所有令牌
            for (int i = 0; i < CAPACITY; i++) {
                rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);
            }

            // 此时请求应该被拒绝
            boolean result = rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);
            assertThat(result).isFalse();

            // 状态显示已耗尽
            TokenBucketStatus status = rateLimiter.getStatus("user:123", CAPACITY, REFILL_RATE);
            assertThat(status.currentTokens()).isEqualTo(0);
        }

        @Test
        @DisplayName("请求大于剩余令牌数被拒绝")
        void requestMoreThanAvailable_rejected() {
            // 只剩 3 个令牌
            for (int i = 0; i < CAPACITY - 3; i++) {
                rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 1);
            }

            // 请求 5 个令牌应该被拒绝
            boolean result = rateLimiter.tryAcquire("user:123", CAPACITY, REFILL_RATE, 5);

            assertThat(result).isFalse();
        }
    }
}