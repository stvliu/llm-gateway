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

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public long getBackoffInitial() { return backoffInitial; }
    public void setBackoffInitial(long backoffInitial) { this.backoffInitial = backoffInitial; }

    public double getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }

    public Set<Integer> getRetryableStatusCodes() { return retryableStatusCodes; }
    public void setRetryableStatusCodes(Set<Integer> retryableStatusCodes) { this.retryableStatusCodes = retryableStatusCodes; }
}