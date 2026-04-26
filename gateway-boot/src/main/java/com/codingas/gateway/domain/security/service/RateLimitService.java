package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.entity.TokenLimit;
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

        return TokenBucketRateLimiter.tryAcquire(limitKey, capacity, refillRate, 1);
    }

    public TokenLimit getTokenLimit(Long userId) {
        return tokenLimitGateway.findByUserId(userId);
    }

    public boolean shouldFailClose(int currentQps) {
        return currentQps > 1000;
    }
}

/**
 * 令牌桶限流器
 */
class TokenBucketRateLimiter {
    public static boolean tryAcquire(String key, int capacity, int refillRate, int tokens) {
        // TODO: 实现令牌桶算法
        return true;
    }
}
