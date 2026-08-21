/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.security.threat;

import com.codingas.gateway.security.threat.TokenBucketRateLimiter;
import com.codingas.gateway.security.threat.RateLimitProperties;
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

    private final TokenBucketRateLimiter rateLimiter;
    private final RateLimitProperties properties;

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
        int capacity = properties.bucketSize();
        int refillRate = properties.refillRate();

        return rateLimiter.tryAcquire(limitKey, capacity, refillRate, 1);
    }

    /**
     * 检查当前限流策略（fail-open 或 fail-close）
     *
     * <p>fail-close 策略：当 QPS > 配置阈值时触发。</p>
     */
    public boolean shouldFailClose(int currentQps) {
        return currentQps > properties.qpsThreshold();
    }

    /**
     * 获取限流状态
     */
    public TokenBucketStatus getStatus(Long apiKeyId) {
        if (apiKeyId == null) {
            return new TokenBucketStatus(
                    properties.bucketSize(),
                    properties.bucketSize(),
                    properties.refillRate());
        }

        String limitKey = "api_key:" + apiKeyId;
        return rateLimiter.getStatus(limitKey,
                properties.bucketSize(),
                properties.refillRate());
    }
}
