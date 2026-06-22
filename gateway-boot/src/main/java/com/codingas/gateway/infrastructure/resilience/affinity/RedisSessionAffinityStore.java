package com.codingas.gateway.infrastructure.resilience.affinity;

import com.codingas.gateway.domain.resilience.gateway.SessionAffinityStore;
import com.codingas.gateway.infrastructure.config.SessionAffinityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 会话亲和存储（生产环境）
 *
 * <p>使用 {@link StringRedisTemplate} 存储 sessionId → channelId 映射，
 * key 形如 {@code session:affinity:{sessionId}}，value 为 channelId 字符串，
 * 写入时设置 TTL（默认 30 分钟）。</p>
 *
 * <p>仅生产环境显式启用 Redis 时装配（{@code spring.data.redis.enabled=true}），
 * 开发/测试环境（Redis 未启用）自动走 {@link InMemorySessionAffinityStore}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
public class RedisSessionAffinityStore implements SessionAffinityStore {

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "session:affinity:";

    private final StringRedisTemplate stringRedisTemplate;
    private final SessionAffinityConfig sessionAffinityConfig;

    @Override
    public Long get(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(key(sessionId));
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("Redis 中 session affinity 值格式异常，清除: key={}, value={}", key(sessionId), value);
            stringRedisTemplate.delete(key(sessionId));
            return null;
        }
    }

    @Override
    public void put(String sessionId, Long channelId) {
        if (sessionId == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key(sessionId), String.valueOf(channelId),
                sessionAffinityConfig.getTtlMinutes(), TimeUnit.MINUTES);
        log.debug("Redis 会话亲和已存储: sessionId={}, channelId={}, ttl={}min",
                sessionId, channelId, sessionAffinityConfig.getTtlMinutes());
    }

    @Override
    public void evict(String sessionId) {
        if (sessionId == null) {
            return;
        }
        stringRedisTemplate.delete(key(sessionId));
        log.debug("Redis 会话亲和已清除: sessionId={}", sessionId);
    }

    /**
     * 构建 Redis key
     *
     * @param sessionId 会话标识
     * @return 完整的 Redis key
     */
    private static String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
