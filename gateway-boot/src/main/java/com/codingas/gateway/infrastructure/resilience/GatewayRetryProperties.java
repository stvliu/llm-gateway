/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 网关重试配置
 */
@Component
@ConfigurationProperties(prefix = "gateway.retry")
public class GatewayRetryProperties {

    /** 最大重试次数 */
    private int maxAttempts = 3;

    /** 初始退避时间（毫秒） */
    private long backoffInitial = 1000;

    /** 退避倍数 */
    private double backoffMultiplier = 2.0;

    /** 可重试的 HTTP 状态码 */
    private Set<Integer> retryableStatusCodes = Set.of(429, 500, 502, 503);

    /** 限流重试配置 */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /** 快速重试配置 */
    private FastRetryConfig fastRetry = new FastRetryConfig();

    /** 服务不可用重试配置 */
    private ServiceUnavailableConfig serviceUnavailable = new ServiceUnavailableConfig();

    // ---- 内部配置类 ----

    /**
     * 限流重试配置（429）
     */
    public static class RateLimitConfig {
        private int maxAttempts = 5;
        private long backoffInitial = 2000;
        private double backoffMultiplier = 2.0;
        private long maxBackoff = 60000;
        private double jitterRate = 0.25;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public long getBackoffInitial() { return backoffInitial; }
        public void setBackoffInitial(long backoffInitial) { this.backoffInitial = backoffInitial; }

        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }

        public long getMaxBackoff() { return maxBackoff; }
        public void setMaxBackoff(long maxBackoff) { this.maxBackoff = maxBackoff; }

        public double getJitterRate() { return jitterRate; }
        public void setJitterRate(double jitterRate) { this.jitterRate = jitterRate; }
    }

    /**
     * 快速重试配置（504）
     */
    public static class FastRetryConfig {
        private int maxAttempts = 2;
        private long backoffFixed = 500;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public long getBackoffFixed() { return backoffFixed; }
        public void setBackoffFixed(long backoffFixed) { this.backoffFixed = backoffFixed; }
    }

    /**
     * 服务不可用重试配置（503）
     */
    public static class ServiceUnavailableConfig {
        private int maxAttempts = 3;
        private long backoffFixed = 5000;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public long getBackoffFixed() { return backoffFixed; }
        public void setBackoffFixed(long backoffFixed) { this.backoffFixed = backoffFixed; }
    }

    // ---- 原有 getter/setter ----

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public long getBackoffInitial() { return backoffInitial; }
    public void setBackoffInitial(long backoffInitial) { this.backoffInitial = backoffInitial; }

    public double getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }

    public Set<Integer> getRetryableStatusCodes() { return retryableStatusCodes; }
    public void setRetryableStatusCodes(Set<Integer> retryableStatusCodes) { this.retryableStatusCodes = retryableStatusCodes; }

    // ---- 新增 getter ----

    public RateLimitConfig getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimitConfig rateLimit) { this.rateLimit = rateLimit; }

    public FastRetryConfig getFastRetry() { return fastRetry; }
    public void setFastRetry(FastRetryConfig fastRetry) { this.fastRetry = fastRetry; }

    public ServiceUnavailableConfig getServiceUnavailable() { return serviceUnavailable; }
    public void setServiceUnavailable(ServiceUnavailableConfig serviceUnavailable) { this.serviceUnavailable = serviceUnavailable; }
}
