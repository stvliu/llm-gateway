/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.threat.service;

/**
 * 令牌桶状态
 */
public record TokenBucketStatus(int currentTokens, int capacity, int refillRate) {
    public double usagePercent() {
        return ((double) (capacity - currentTokens) / capacity) * 100;
    }
}
