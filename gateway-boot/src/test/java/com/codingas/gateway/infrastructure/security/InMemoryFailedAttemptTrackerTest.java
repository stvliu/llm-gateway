package com.codingas.gateway.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryFailedAttemptTracker 单元测试
 */
@DisplayName("InMemoryFailedAttemptTracker")
class InMemoryFailedAttemptTrackerTest {

    private final InMemoryFailedAttemptTracker tracker = new InMemoryFailedAttemptTracker();

    @Nested
    @DisplayName("increment")
    class IncrementTests {

        @Test
        @DisplayName("首次调用返回 1")
        void firstIncrement_returnsOne() {
            long result = tracker.increment("user:123");

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("多次调用递增计数")
        void multipleIncrements_incrementsCount() {
            tracker.increment("user:123");
            tracker.increment("user:123");
            long result = tracker.increment("user:123");

            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("不同 key 独立计数")
        void differentKeys_independentCounts() {
            tracker.increment("user:123");
            tracker.increment("user:123");
            tracker.increment("user:456");

            assertThat(tracker.get("user:123")).isEqualTo(2);
            assertThat(tracker.get("user:456")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("get")
    class GetTests {

        @Test
        @DisplayName("不存在的 key 返回 0")
        void nonExistentKey_returnsZero() {
            int result = tracker.get("nonexistent");

            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("存在的 key 返回正确计数")
        void existingKey_returnsCorrectCount() {
            tracker.increment("user:123");
            tracker.increment("user:123");

            int result = tracker.get("user:123");

            assertThat(result).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除后计数归零")
        void afterDelete_countIsZero() {
            tracker.increment("user:123");
            tracker.increment("user:123");
            tracker.delete("user:123");

            assertThat(tracker.get("user:123")).isEqualTo(0);
        }

        @Test
        @DisplayName("删除不存在的 key 不抛异常")
        void deleteNonExistent_doesNotThrow() {
            tracker.delete("nonexistent");
        }

        @Test
        @DisplayName("删除后可以重新计数")
        void afterDelete_canIncrementAgain() {
            tracker.increment("user:123");
            tracker.delete("user:123");
            long result = tracker.increment("user:123");

            assertThat(result).isEqualTo(1);
        }
    }
}