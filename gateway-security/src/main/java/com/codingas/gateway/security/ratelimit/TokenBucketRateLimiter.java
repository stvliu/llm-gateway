package com.codingas.gateway.security.ratelimit;

/**
 * 令牌桶限流器接口
 *
 * <p>实现可以是 Redis 版本（生产）或内存版本（开发）。</p>
 */
public interface TokenBucketRateLimiter {

    /**
     * 尝试获取令牌
     *
     * @param key        限流 key（如 user:123 或 api_key:xxx）
     * @param capacity   桶容量
     * @param refillRate 每秒补充令牌数
     * @param requested  请求令牌数（通常为1）
     * @return true 表示允许，false 表示被限流
     */
    boolean tryAcquire(String key, int capacity, int refillRate, int requested);

    /**
     * 获取当前令牌桶状态
     */
    TokenBucketStatus getStatus(String key, int capacity, int refillRate);

    /**
     * 重置限流桶
     */
    void reset(String key);
}