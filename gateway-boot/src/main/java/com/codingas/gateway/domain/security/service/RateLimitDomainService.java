package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.usage.entity.TokenLimit;
import com.codingas.gateway.domain.security.gateway.TokenBucketRateLimiter;
import com.codingas.gateway.domain.security.gateway.TokenLimitGateway;
import com.codingas.gateway.infrastructure.config.GatewayProperties;
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
public class RateLimitDomainService {

    private final TokenLimitGateway tokenLimitGateway;
    private final TokenBucketRateLimiter rateLimiter;
    private final GatewayProperties properties;

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
        int capacity = properties.getRateLimit().getBucketSize();
        int refillRate = properties.getRateLimit().getRefillRate();

        return rateLimiter.tryAcquire(limitKey, capacity, refillRate, 1);
    }

    /**
     * 获取用户的 Token 限额列表
     */
    public java.util.List<TokenLimit> getTokenLimits(Long userId) {
        return tokenLimitGateway.findByUserId(userId);
    }

    /**
     * 检查当前限流策略（fail-open 或 fail-close）
     *
     * <p>fail-close 策略：当 QPS > 配置阈值时触发。</p>
     */
    public boolean shouldFailClose(int currentQps) {
        return currentQps > properties.getRateLimit().getQpsThreshold();
    }

    /**
     * 获取限流状态
     */
    public TokenBucketStatus getStatus(Long apiKeyId) {
        if (apiKeyId == null) {
            return new TokenBucketStatus(
                    properties.getRateLimit().getBucketSize(),
                    properties.getRateLimit().getBucketSize(),
                    properties.getRateLimit().getRefillRate());
        }

        String limitKey = "api_key:" + apiKeyId;
        return rateLimiter.getStatus(limitKey,
                properties.getRateLimit().getBucketSize(),
                properties.getRateLimit().getRefillRate());
    }
}
