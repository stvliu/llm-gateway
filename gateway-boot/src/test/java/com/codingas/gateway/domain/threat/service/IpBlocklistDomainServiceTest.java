package com.codingas.gateway.domain.threat.service;

import com.codingas.gateway.domain.threat.gateway.IpBlockGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IpBlocklistDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IpBlocklistDomainService")
class IpBlocklistDomainServiceTest {

    @Mock
    private IpBlockGateway ipBlockGateway;

    @InjectMocks
    private IpBlocklistDomainService ipBlocklistService;

    @Test
    @DisplayName("isBlocked null IP 应返回 false")
    void isBlocked_nullIp_returnsFalse() {
        boolean result = ipBlocklistService.isBlocked(null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isBlocked blank IP 应返回 false")
    void isBlocked_blankIp_returnsFalse() {
        boolean result = ipBlocklistService.isBlocked("");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isBlocked 在黑名单中的 IP 应返回 true")
    void isBlocked_blockedIp_returnsTrue() {
        when(ipBlockGateway.isBlocked("192.168.1.100")).thenReturn(true);

        boolean result = ipBlocklistService.isBlocked("192.168.1.100");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("blockIp 永久封禁应调用 ipBlockGateway.block 带 null expiresAt")
    void blockIp_permanent_callsBlockWithNullExpires() {
        ipBlocklistService.blockIp("192.168.1.100", "Brute force", 1L);

        verify(ipBlockGateway).block(eq("192.168.1.100"), eq("Brute force"), eq(1L), eq(null));
    }

    @Test
    @DisplayName("blockIp 临时封禁应计算正确的过期时间")
    void blockIp_temporary_calculatesExpiresAt() {
        ipBlocklistService.blockIp("192.168.1.100", "Testing", 1L, 30);

        verify(ipBlockGateway).block(eq("192.168.1.100"), eq("Testing"), eq(1L), any(Instant.class));
    }

    @Test
    @DisplayName("unblockIp 应调用 ipBlockGateway.unblock")
    void unblockIp_callsUnblock() {
        ipBlocklistService.unblockIp("192.168.1.100");

        verify(ipBlockGateway).unblock("192.168.1.100");
    }
}
