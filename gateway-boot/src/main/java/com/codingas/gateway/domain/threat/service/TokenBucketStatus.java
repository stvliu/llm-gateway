package com.codingas.gateway.domain.threat.service;

/**
 * 令牌桶状态
 */
public record TokenBucketStatus(int currentTokens, int capacity, int refillRate) {
    public double usagePercent() {
        return ((double) (capacity - currentTokens) / capacity) * 100;
    }
}
