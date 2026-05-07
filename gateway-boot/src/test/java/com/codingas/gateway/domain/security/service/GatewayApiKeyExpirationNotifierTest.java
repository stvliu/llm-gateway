package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GatewayApiKeyExpirationNotifier 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GatewayApiKeyExpirationNotifier 测试")
class GatewayApiKeyExpirationNotifierTest {

    @Mock
    private ApiKeyGateway apiKeyGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private NotificationDomainService notificationService;

    @InjectMocks
    private GatewayApiKeyExpirationNotifier notifier;

    @Nested
    @DisplayName("manualScan 方法测试")
    class ManualScanTests {

        @Test
        @DisplayName("手动扫描成功")
        void manualScan_keysFound_returnsResult() {
            // given
            GatewayApiKey key = createTestApiKey();
            User user = new User();
            user.setId(1L);
            user.setEmail("test@example.com");
            user.setUsername("testuser");
            Page<GatewayApiKey> page = new PageImpl<>(List.of(key));
            when(apiKeyGateway.findExpiringKeys(any(), any(), any())).thenReturn(page);
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(notificationService.sendExpirationWarning(any(), any(), any(), any(), any())).thenReturn(true);

            // when
            var result = notifier.manualScan();

            // then
            assertThat(result.total()).isEqualTo(1);
            assertThat(result.sent()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(0);
        }

        @Test
        @DisplayName("无过期 Key")
        void manualScan_noKeys_returnsEmpty() {
            // given
            Page<GatewayApiKey> emptyPage = new PageImpl<>(List.of());
            when(apiKeyGateway.findExpiringKeys(any(), any(), any())).thenReturn(emptyPage);

            // when
            var result = notifier.manualScan();

            // then
            assertThat(result.total()).isEqualTo(0);
            assertThat(result.sent()).isEqualTo(0);
        }

        @Test
        @DisplayName("发送失败计入 failed")
        void manualScan_sendFails_countsFailed() {
            // given
            GatewayApiKey key = createTestApiKey();
            Page<GatewayApiKey> page = new PageImpl<>(List.of(key));
            when(apiKeyGateway.findExpiringKeys(any(), any(), any())).thenReturn(page);
            when(notificationService.sendExpirationWarning(any(), any(), any(), any(), any())).thenReturn(false);

            // when
            var result = notifier.manualScan();

            // then
            assertThat(result.total()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("manualNotify 方法测试")
    class ManualNotifyTests {

        @Test
        @DisplayName("手动通知成功")
        void manualNotify_keyFound_returnsTrue() {
            // given
            GatewayApiKey key = createTestApiKey();
            User user = new User();
            user.setId(1L);
            user.setEmail("test@example.com");
            user.setUsername("testuser");
            when(apiKeyGateway.findById(1L)).thenReturn(Optional.of(key));
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(notificationService.sendExpirationWarning(any(), any(), any(), any(), any())).thenReturn(true);

            // when
            boolean result = notifier.manualNotify(1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Key 不存在返回 false")
        void manualNotify_keyNotFound_returnsFalse() {
            // given
            when(apiKeyGateway.findById(999L)).thenReturn(Optional.empty());

            // when
            boolean result = notifier.manualNotify(999L);

            // then
            assertThat(result).isFalse();
        }
    }

    // Helper methods
    private GatewayApiKey createTestApiKey() {
        GatewayApiKey key = new GatewayApiKey();
        key.setId(1L);
        key.setName("Test Key");
        key.setExpiresAt(Instant.now().plusSeconds(86400 * 5));

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        key.setUserId(user.getId());
        key.setUsername(user.getUsername());

        return key;
    }
}
