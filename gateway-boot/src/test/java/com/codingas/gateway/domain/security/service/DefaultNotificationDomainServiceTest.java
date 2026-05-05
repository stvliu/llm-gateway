package com.codingas.gateway.domain.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultNotificationDomainService 单元测试
 */
@DisplayName("DefaultNotificationDomainService 测试")
class DefaultNotificationDomainServiceTest {

    private DefaultNotificationDomainService service;

    @BeforeEach
    void setUp() {
        service = new DefaultNotificationDomainService();
    }

    @Test
    @DisplayName("发送过期警告成功")
    void sendExpirationWarning_validParams_returnsTrue() {
        // given
        String email = "test@example.com";
        String username = "testuser";
        String keyCode = "key-001";
        String keyName = "Test Key";
        Instant expiresAt = Instant.now().plusSeconds(86400 * 7);

        // when
        boolean result = service.sendExpirationWarning(email, username, keyCode, keyName, expiresAt);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("null 参数也能正常处理")
    void sendExpirationWarning_nullParams_returnsTrue() {
        // when
        boolean result = service.sendExpirationWarning(null, null, null, null, null);

        // then
        assertThat(result).isTrue();
    }
}
