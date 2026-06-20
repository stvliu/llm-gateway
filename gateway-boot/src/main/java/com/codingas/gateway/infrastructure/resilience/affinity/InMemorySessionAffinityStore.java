package com.codingas.gateway.infrastructure.resilience.affinity;

import com.codingas.gateway.domain.resilience.gateway.SessionAffinityStore;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存会话亲和存储（开发/测试环境）
 *
 * <p>使用 {@link ConcurrentHashMap} 存储亲和映射，惰性过期判断：
 * get 时检查写入时间戳，超 TTL 则视为过期并清除。</p>
 *
 * <p>不依赖外部资源，保证测试稳定。</p>
 */
@Slf4j
public class InMemorySessionAffinityStore implements SessionAffinityStore {

    /** sessionId → 带时间戳的 channelId 包装 */
    private final ConcurrentMap<String, TimestampedValue> store = new ConcurrentHashMap<>();

    /** TTL（毫秒） */
    private final long ttlMillis;

    /**
     * 构造内存会话亲和存储
     *
     * @param ttlMinutes TTL（分钟），过期后 get 返回 null
     */
    public InMemorySessionAffinityStore(int ttlMinutes) {
        this.ttlMillis = (long) ttlMinutes * 60 * 1000;
    }

    /**
     * 构造内存会话亲和存储（包级可见，专供测试）
     *
     * @param ttlMillis TTL（毫秒），过期后 get 返回 null
     * @param isMillis  标记位（仅用于区分重载，传 true）
     */
    InMemorySessionAffinityStore(long ttlMillis, boolean isMillis) {
        this.ttlMillis = ttlMillis;
    }

    @Override
    public Long get(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        TimestampedValue tv = store.get(sessionId);
        if (tv == null) {
            return null;
        }
        // 惰性过期检查
        if (tv.isExpired(ttlMillis)) {
            store.remove(sessionId, tv);
            log.debug("会话亲和已过期，清除: sessionId={}", sessionId);
            return null;
        }
        return tv.channelId;
    }

    @Override
    public void put(String sessionId, Long channelId) {
        if (sessionId == null) {
            return;
        }
        store.put(sessionId, new TimestampedValue(channelId, System.currentTimeMillis()));
        log.debug("会话亲和已存储: sessionId={}, channelId={}", sessionId, channelId);
    }

    @Override
    public void evict(String sessionId) {
        if (sessionId == null) {
            return;
        }
        store.remove(sessionId);
        log.debug("会话亲和已清除: sessionId={}", sessionId);
    }

    /**
     * 带时间戳的值包装
     *
     * <p>记录写入时间戳，用于惰性过期判断。</p>
     */
    private static class TimestampedValue {
        private final Long channelId;
        private final long timestamp;

        TimestampedValue(Long channelId, long timestamp) {
            this.channelId = channelId;
            this.timestamp = timestamp;
        }

        /**
         * 判断是否已过期
         *
         * @param ttlMillis TTL（毫秒）
         * @return 超过 TTL 返回 true
         */
        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }
    }
}
