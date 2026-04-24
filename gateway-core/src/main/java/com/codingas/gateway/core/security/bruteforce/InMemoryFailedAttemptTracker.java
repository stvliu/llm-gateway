package com.codingas.gateway.core.security.bruteforce;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于内存的失败尝试追踪器（开发环境使用）
 *
 * <p>生产环境应使用 Redis 版本的 FailedAttemptTracker。</p>
 */
@Slf4j
@Component
public class InMemoryFailedAttemptTracker implements FailedAttemptTracker {

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    @Override
    public long increment(String key) {
        AtomicInteger count = attempts.computeIfAbsent(key, k -> new AtomicInteger(0));
        int result = count.incrementAndGet();
        log.debug("In-memory attempt increment: key={}, count={}", key, result);
        return result;
    }

    @Override
    public void delete(String key) {
        attempts.remove(key);
        log.debug("In-memory attempt deleted: key={}", key);
    }

    @Override
    public int get(String key) {
        AtomicInteger count = attempts.get(key);
        return count == null ? 0 : count.get();
    }
}