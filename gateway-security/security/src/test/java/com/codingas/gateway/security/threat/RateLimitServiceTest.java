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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * RateLimitService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService")
class RateLimitServiceTest {

    @Mock
    private TokenBucketRateLimiter rateLimiter;

    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties(100, 10, 1000);
    }

    @Test
    @DisplayName("isAllowed 应使用配置的 bucketSize 和 refillRate")
    void isAllowed_usesConfiguredValues() {
        RateLimitService service = new RateLimitService(rateLimiter, properties);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(true);

        boolean result = service.isAllowed(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAllowed apiKeyId 为 null 应返回 true")
    void isAllowed_nullApiKey_returnsTrue() {
        RateLimitService service = new RateLimitService(rateLimiter, properties);

        boolean result = service.isAllowed(null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("shouldFailClose 当前 QPS 超过阈值应返回 true")
    void shouldFailClose_exceedsThreshold_returnsTrue() {
        RateLimitService service = new RateLimitService(rateLimiter, properties);

        boolean result = service.shouldFailClose(1001);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("shouldFailClose 当前 QPS 未超过阈值应返回 false")
    void shouldFailClose_belowThreshold_returnsFalse() {
        RateLimitService service = new RateLimitService(rateLimiter, properties);

        boolean result = service.shouldFailClose(999);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAllowed 限流器拒绝时应返回 false")
    void isAllowed_rateLimiterRejects_returnsFalse() {
        RateLimitService service = new RateLimitService(rateLimiter, properties);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(false);

        boolean result = service.isAllowed(1L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getStatus 有 apiKeyId 时应委托限流器查询")
    void getStatus_withApiKey_delegatesToRateLimiter() {
        RateLimitService service = new RateLimitService(rateLimiter, properties);
        TokenBucketStatus expected = new TokenBucketStatus(50, 100, 10);
        when(rateLimiter.getStatus("api_key:1", 100, 10)).thenReturn(expected);

        TokenBucketStatus result = service.getStatus(1L);

        assertThat(result.currentTokens()).isEqualTo(50);
        assertThat(result.capacity()).isEqualTo(100);
        assertThat(result.refillRate()).isEqualTo(10);
    }

    @Test
    @DisplayName("getStatus apiKeyId 为 null 应返回满桶状态")
    void getStatus_nullApiKey_returnsFullBucket() {
        RateLimitService service = new RateLimitService(rateLimiter, properties);

        TokenBucketStatus result = service.getStatus(null);

        assertThat(result.currentTokens()).isEqualTo(100);
        assertThat(result.capacity()).isEqualTo(100);
        assertThat(result.refillRate()).isEqualTo(10);
    }
}
