package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.entity.TokenLimit;
import com.codingas.gateway.domain.security.gateway.TokenBucketRateLimiter;
import com.codingas.gateway.domain.security.gateway.TokenLimitGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 流量限流服务
 *
 * <p>基于令牌桶算法。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final int DEFAULT_BUCKET_SIZE = 100;
    private static final int DEFAULT_REFILL_RATE = 10;

    private final TokenLimitGateway tokenLimitGateway;
    private final TokenBucketRateLimiter rateLimiter;

    /**
     * 检查是否允许请求
     *
     * @param apiKeyId API Key ID
     * @return 是否允许
     */
    public boolean isAllowed(Long apiKeyId) {
        if (apiKeyId == null) {
            return true;
        }

        String limitKey = "api_key:" + apiKeyId;
        // TODO: 从 RateLimitConfig 获取配置，当前使用默认值
        int capacity = DEFAULT_BUCKET_SIZE;
        int refillRate = DEFAULT_REFILL_RATE;

        return rateLimiter.tryAcquire(limitKey, capacity, refillRate, 1);
    }

    /**
     * 获取用户的 Token 限额
     */
    public TokenLimit getTokenLimit(Long userId) {
        return tokenLimitGateway.findByUserId(userId);
    }

    /**
     * 检查当前限流策略（fail-open 或 fail-close）
     *
     * <p>fail-close 策略：当 QPS > 1000 时触发。</p>
     */
    public boolean shouldFailClose(int currentQps) {
        return currentQps > 1000;
    }

    /**
     * 获取限流状态
     */
    public TokenBucketStatus getStatus(Long apiKeyId) {
        if (apiKeyId == null) {
            return new TokenBucketStatus(DEFAULT_BUCKET_SIZE, DEFAULT_BUCKET_SIZE, DEFAULT_REFILL_RATE);
        }

        String limitKey = "api_key:" + apiKeyId;
        return rateLimiter.getStatus(limitKey, DEFAULT_BUCKET_SIZE, DEFAULT_REFILL_RATE);
    }
}
