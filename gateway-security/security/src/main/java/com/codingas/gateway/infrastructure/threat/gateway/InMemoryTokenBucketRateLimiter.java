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
package com.codingas.gateway.infrastructure.threat.gateway;

import com.codingas.gateway.domain.threat.gateway.TokenBucketRateLimiter;
import com.codingas.gateway.domain.threat.service.TokenBucketStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的令牌桶限流器（开发环境使用）
 *
 * <p>生产环境应使用 Redis 版本的 TokenBucketRateLimiter。</p>
 */
@Slf4j
@Component
public class InMemoryTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    /**
     * 尝试获取令牌
     */
    public boolean tryAcquire(String key, int capacity, int refillRate, int requested) {
        String bucketKey = "ratelimit:" + key;
        long now = System.currentTimeMillis() / 1000;

        BucketState state = buckets.compute(bucketKey, (k, existing) -> {
            if (existing == null) {
                return new BucketState(capacity, now);
            }
            // 补充令牌
            long elapsed = now - existing.lastRefill;
            int newTokens = Math.min(capacity, existing.tokens + (int) (elapsed * refillRate));
            return new BucketState(newTokens, now);
        });

        if (state.tokens >= requested) {
            buckets.put(bucketKey, new BucketState(state.tokens - requested, state.lastRefill));
            return true;
        } else {
            log.warn("Rate limit exceeded for key: {}", key);
            return false;
        }
    }

    /**
     * 获取当前令牌桶状态
     */
    public TokenBucketStatus getStatus(String key, int capacity, int refillRate) {
        String bucketKey = "ratelimit:" + key;
        BucketState state = buckets.get(bucketKey);

        if (state == null) {
            return new TokenBucketStatus(capacity, capacity, refillRate);
        }

        // 计算当前令牌数（考虑时间补充）
        long now = System.currentTimeMillis() / 1000;
        long elapsed = now - state.lastRefill;
        int currentTokens = Math.min(capacity, state.tokens + (int) (elapsed * refillRate));

        return new TokenBucketStatus(currentTokens, capacity, refillRate);
    }

    /**
     * 重置限流桶
     */
    public void reset(String key) {
        String bucketKey = "ratelimit:" + key;
        buckets.remove(bucketKey);
    }

    private record BucketState(int tokens, long lastRefill) {}
}
