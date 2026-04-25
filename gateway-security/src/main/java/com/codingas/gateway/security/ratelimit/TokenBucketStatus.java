package com.codingas.gateway.security.ratelimit;

/**
 * 令牌桶状态
 */
public record TokenBucketStatus(int currentTokens, int capacity, int refillRate) {
    public double usagePercent() {
        return ((double) (capacity - currentTokens) / capacity) * 100;
    }
}