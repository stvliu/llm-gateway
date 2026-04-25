package com.codingas.gateway.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于内存的令牌桶限流器（开发环境使用）
 *
 * <p>生产环境应使用 Redis 版本的 TokenBucketRateLimiter。</p>
 */
@Slf4j
@Component
public class InMemoryTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int capacity, int refillRate, int requested) {
        String redisKey = "ratelimit:" + key;
        long now = System.currentTimeMillis() / 1000;

        BucketState state = buckets.compute(redisKey, (k, existing) -> {
            if (existing == null) {
                return new BucketState(capacity, now);
            }
            // 补充令牌
            long elapsed = now - existing.lastRefill;
            int newTokens = Math.min(capacity, existing.tokens + (int) (elapsed * refillRate));
            return new BucketState(newTokens, now);
        });

        if (state.tokens >= requested) {
            buckets.put(redisKey, new BucketState(state.tokens - requested, state.lastRefill));
            return true;
        } else {
            log.warn("Rate limit exceeded for key: {}", key);
            return false;
        }
    }

    @Override
    public TokenBucketStatus getStatus(String key, int capacity, int refillRate) {
        String redisKey = "ratelimit:" + key;
        BucketState state = buckets.get(redisKey);

        if (state == null) {
            return new TokenBucketStatus(capacity, capacity, refillRate);
        }

        // 计算当前令牌数（考虑时间补充）
        long now = System.currentTimeMillis() / 1000;
        long elapsed = now - state.lastRefill;
        int currentTokens = Math.min(capacity, state.tokens + (int) (elapsed * refillRate));

        return new TokenBucketStatus(currentTokens, capacity, refillRate);
    }

    @Override
    public void reset(String key) {
        String redisKey = "ratelimit:" + key;
        buckets.remove(redisKey);
    }

    private record BucketState(int tokens, long lastRefill) {}
}