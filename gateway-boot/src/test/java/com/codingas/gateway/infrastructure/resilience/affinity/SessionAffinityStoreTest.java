package com.codingas.gateway.infrastructure.resilience.affinity;

import com.codingas.gateway.domain.resilience.gateway.SessionAffinityStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SessionAffinityStore 单元测试（针对 InMemory 实现）
 *
 * <p>验证会话亲和存储的 put/get/evict 语义以及 TTL 过期行为。
 * 测试直接实例化 {@link InMemorySessionAffinityStore}，不依赖外部资源。</p>
 */
@DisplayName("SessionAffinityStore 测试")
class SessionAffinityStoreTest {

    private InMemorySessionAffinityStore store;

    /** TTL 设为 1 秒（1000ms），用于快速验证过期语义 */
    private static final long SHORT_TTL_MILLIS = 1000L;

    @BeforeEach
    void setUp() {
        store = new InMemorySessionAffinityStore(60); // 默认 TTL 60 分钟
    }

    @Nested
    @DisplayName("put / get 基本语义")
    class PutGetTests {

        @Test
        @DisplayName("put 后 get 返回正确的 channelId")
        void putThenGet_returnsCorrectChannelId() {
            store.put("session-1", 42L);

            Long channelId = store.get("session-1");

            assertThat(channelId).isEqualTo(42L);
        }

        @Test
        @DisplayName("多个 sessionId 各自独立存储")
        void multipleSessions_areIndependent() {
            store.put("session-a", 10L);
            store.put("session-b", 20L);

            assertThat(store.get("session-a")).isEqualTo(10L);
            assertThat(store.get("session-b")).isEqualTo(20L);
        }

        @Test
        @DisplayName("put 覆盖已有 sessionId 更新 channelId")
        void put_overwritesExistingChannelId() {
            store.put("session-1", 42L);
            store.put("session-1", 99L);

            assertThat(store.get("session-1")).isEqualTo(99L);
        }
    }

    @Nested
    @DisplayName("evict 语义")
    class EvictTests {

        @Test
        @DisplayName("evict 后 get 返回 null")
        void evictThenGet_returnsNull() {
            store.put("session-1", 42L);
            store.evict("session-1");

            assertThat(store.get("session-1")).isNull();
        }

        @Test
        @DisplayName("evict 不存在的 sessionId 不抛异常")
        void evictNonExistent_doesNotThrow() {
            store.evict("non-existent");
            // 无异常即通过
        }
    }

    @Nested
    @DisplayName("标识缺失（不亲和）语义")
    class MissingIdentifierTests {

        @Test
        @DisplayName("未 put 的 sessionId 返回 null（不亲和）")
        void neverPut_returnsNull() {
            Long channelId = store.get("never-put");

            assertThat(channelId).isNull();
        }

        @Test
        @DisplayName("null sessionId 调用 get 返回 null（不亲和）")
        void nullSessionId_getReturnsNull() {
            Long channelId = store.get(null);

            assertThat(channelId).isNull();
        }

        @Test
        @DisplayName("null sessionId 调用 put 不存储")
        void nullSessionId_putDoesNotStore() {
            store.put(null, 42L);

            // 用已知 sessionId 验证 map 未受影响（null key 不应被存储）
            store.put("valid-session", 99L);
            assertThat(store.get("valid-session")).isEqualTo(99L);
            // 此处我们只验证 null put 不抛异常，且不影响正常存储
        }
    }

    @Nested
    @DisplayName("TTL 过期语义")
    class TtlTests {

        @Test
        @DisplayName("TTL 过期后 get 返回 null")
        void expiredTtl_returnsNull() throws InterruptedException {
            // 使用短 TTL（1 秒）的 store
            InMemorySessionAffinityStore shortTtlStore = new InMemorySessionAffinityStore(SHORT_TTL_MILLIS, true);

            shortTtlStore.put("session-ttl", 42L);
            // 刚 put 完应能获取
            assertThat(shortTtlStore.get("session-ttl")).isEqualTo(42L);

            // 等待 TTL 过期
            Thread.sleep(1100);

            // 过期后应返回 null
            assertThat(shortTtlStore.get("session-ttl")).isNull();
        }

        @Test
        @DisplayName("未过期的 session 仍可正常获取")
        void notExpired_returnsChannelId() throws InterruptedException {
            InMemorySessionAffinityStore shortTtlStore = new InMemorySessionAffinityStore(SHORT_TTL_MILLIS, true);

            shortTtlStore.put("session-active", 42L);
            // 在过期前获取，应能正常返回
            Thread.sleep(500); // 未到 1 秒
            assertThat(shortTtlStore.get("session-active")).isEqualTo(42L);
        }

        @Test
        @DisplayName("put 更新已有 session 重置 TTL")
        void put_refreshesTtl() throws InterruptedException {
            InMemorySessionAffinityStore shortTtlStore = new InMemorySessionAffinityStore(SHORT_TTL_MILLIS, true);

            shortTtlStore.put("session-refresh", 42L);
            Thread.sleep(600); // 过半 TTL
            shortTtlStore.put("session-refresh", 99L); // 刷新 TTL

            Thread.sleep(600); // 距刷新 600ms，距首次 1200ms > 1s，但刷新后 TTL 重置
            assertThat(shortTtlStore.get("session-refresh")).isEqualTo(99L);
        }
    }
}
