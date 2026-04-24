package com.codingas.gateway.core.security.ratelimit;

import com.codingas.gateway.core.domain.entity.GatewayApiKey;
import com.codingas.gateway.core.domain.entity.RateLimitConfig;
import com.codingas.gateway.core.repository.GatewayApiKeyRepository;
import com.codingas.gateway.core.repository.RateLimitConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 流量限流服务
 *
 * <p>基于令牌桶算法，支持 fail-open/fail-close 策略。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final int DEFAULT_REQUESTS_PER_MINUTE = 1000;
    private static final int DEFAULT_BUCKET_SIZE = 100;
    private static final int DEFAULT_REFILL_RATE = 10;

    private final TokenBucketRateLimiter rateLimiter;
    private final GatewayApiKeyRepository apiKeyRepository;
    private final RateLimitConfigRepository configRepository;

    /**
     * 检查是否允许请求
     *
     * @param apiKeyId API Key ID
     * @return true 表示允许，false 表示被限流
     */
    public boolean isAllowed(Long apiKeyId) {
        if (apiKeyId == null) {
            return true;
        }

        Optional<GatewayApiKey> optKey = apiKeyRepository.findById(apiKeyId);
        if (optKey.isEmpty()) {
            return true; // key 不存在，放行（让认证层处理）
        }

        GatewayApiKey apiKey = optKey.get();

        // 获取限流配置
        RateLimitConfig config = getRateLimitConfig();

        String limitKey = "api_key:" + apiKeyId;
        int capacity = config.getBucketSize() != null ? config.getBucketSize() : DEFAULT_BUCKET_SIZE;
        int refillRate = config.getRefillRate() != null ? config.getRefillRate() : DEFAULT_REFILL_RATE;

        return rateLimiter.tryAcquire(limitKey, capacity, refillRate, 1);
    }

    /**
     * 获取限流配置（使用默认配置）
     */
    public RateLimitConfig getRateLimitConfig() {
        return configRepository.findByConfigCode("default")
            .orElseGet(() -> createDefaultConfig());
    }

    /**
     * 创建默认限流配置
     */
    private RateLimitConfig createDefaultConfig() {
        RateLimitConfig config = new RateLimitConfig();
        config.setConfigCode("default");
        config.setName("Default Rate Limit");
        config.setRequestsPerMinute(DEFAULT_REQUESTS_PER_MINUTE);
        config.setBucketSize(DEFAULT_BUCKET_SIZE);
        config.setRefillRate(DEFAULT_REFILL_RATE);
        config.setEnabled(true);
        return configRepository.save(config);
    }

    /**
     * 检查当前限流策略（fail-open 或 fail-close）
     *
     * <p>fail-close 策略：当 Redis 完全不可用且 QPS > 1000 时触发。</p>
     */
    public boolean shouldFailClose(int currentQps) {
        return currentQps > 1000;
    }

    /**
     * 获取 API Key 的限流状态
     */
    public TokenBucketStatus getStatus(Long apiKeyId) {
        if (apiKeyId == null) {
            return new TokenBucketStatus(
                DEFAULT_BUCKET_SIZE, DEFAULT_BUCKET_SIZE, DEFAULT_REFILL_RATE);
        }

        Optional<GatewayApiKey> optKey = apiKeyRepository.findById(apiKeyId);
        if (optKey.isEmpty()) {
            return new TokenBucketStatus(
                DEFAULT_BUCKET_SIZE, DEFAULT_BUCKET_SIZE, DEFAULT_REFILL_RATE);
        }

        RateLimitConfig config = getRateLimitConfig();

        int capacity = config.getBucketSize() != null ? config.getBucketSize() : DEFAULT_BUCKET_SIZE;
        int refillRate = config.getRefillRate() != null ? config.getRefillRate() : DEFAULT_REFILL_RATE;

        String limitKey = "api_key:" + apiKeyId;
        return rateLimiter.getStatus(limitKey, capacity, refillRate);
    }
}
