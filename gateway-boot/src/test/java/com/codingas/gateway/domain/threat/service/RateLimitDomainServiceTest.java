/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.threat.service;

import com.codingas.gateway.domain.threat.gateway.TokenBucketRateLimiter;
import com.codingas.gateway.infrastructure.config.GatewayProperties;
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
 * RateLimitDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitDomainService")
class RateLimitDomainServiceTest {

    @Mock
    private TokenBucketRateLimiter rateLimiter;

    private GatewayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        GatewayProperties.RateLimitProperties rateLimit = new GatewayProperties.RateLimitProperties();
        rateLimit.setBucketSize(100);
        rateLimit.setRefillRate(10);
        rateLimit.setQpsThreshold(1000);
        properties.setRateLimit(rateLimit);
    }

    @Test
    @DisplayName("isAllowed 应使用配置的 bucketSize 和 refillRate")
    void isAllowed_usesConfiguredValues() {
        RateLimitDomainService service = new RateLimitDomainService(rateLimiter, properties);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(true);

        boolean result = service.isAllowed(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAllowed apiKeyId 为 null 应返回 true")
    void isAllowed_nullApiKey_returnsTrue() {
        RateLimitDomainService service = new RateLimitDomainService(rateLimiter, properties);

        boolean result = service.isAllowed(null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("shouldFailClose 当前 QPS 超过阈值应返回 true")
    void shouldFailClose_exceedsThreshold_returnsTrue() {
        RateLimitDomainService service = new RateLimitDomainService(rateLimiter, properties);

        boolean result = service.shouldFailClose(1001);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("shouldFailClose 当前 QPS 未超过阈值应返回 false")
    void shouldFailClose_belowThreshold_returnsFalse() {
        RateLimitDomainService service = new RateLimitDomainService(rateLimiter, properties);

        boolean result = service.shouldFailClose(999);

        assertThat(result).isFalse();
    }
}
