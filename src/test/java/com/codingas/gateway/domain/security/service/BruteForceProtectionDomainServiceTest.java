package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.gateway.IpBlockGateway;
import com.codingas.gateway.infrastructure.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * BruteForceProtectionDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BruteForceProtectionDomainService")
class BruteForceProtectionDomainServiceTest {

    @Mock
    private IpBlockGateway ipBlockGateway;

    private GatewayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        GatewayProperties.SecurityProperties security = new GatewayProperties.SecurityProperties();
        security.setMaxFailedAttempts(3);
        security.setBlockDurationMinutes(15);
        properties.setSecurity(security);
    }

    @Test
    @DisplayName("recordFailedAttempt 未达上限应记录但不封禁")
    void recordFailedAttempt_belowLimit_recordsOnly() {
        BruteForceProtectionDomainService service = new BruteForceProtectionDomainService(ipBlockGateway, properties);

        service.recordFailedAttempt("192.168.1.1");
        service.recordFailedAttempt("192.168.1.1");

        // 未达到 maxFailedAttempts=3，不应封禁
        verify(ipBlockGateway, times(0)).block(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("recordFailedAttempt 达到上限应封禁 IP")
    void recordFailedAttempt_reachesLimit_blocksIp() {
        BruteForceProtectionDomainService service = new BruteForceProtectionDomainService(ipBlockGateway, properties);

        service.recordFailedAttempt("192.168.1.100");
        service.recordFailedAttempt("192.168.1.100");
        service.recordFailedAttempt("192.168.1.100");

        verify(ipBlockGateway).block(
                eq("192.168.1.100"),
                eq("Brute force protection"),
                eq(null),
                any(Instant.class));
    }

    @Test
    @DisplayName("clearFailedAttempts 应清除记录后重新计数")
    void clearFailedAttempts_clearsRecordAndResetsCount() {
        BruteForceProtectionDomainService service = new BruteForceProtectionDomainService(ipBlockGateway, properties);

        service.recordFailedAttempt("192.168.1.1");
        service.recordFailedAttempt("192.168.1.1");
        service.clearFailedAttempts("192.168.1.1");
        // 清除后重新计数，需要 3 次才能封禁
        service.recordFailedAttempt("192.168.1.1");
        service.recordFailedAttempt("192.168.1.1");
        service.recordFailedAttempt("192.168.1.1");

        verify(ipBlockGateway).block(
                eq("192.168.1.1"),
                eq("Brute force protection"),
                eq(null),
                any(Instant.class));
    }
}
