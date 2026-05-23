package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.iam.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.iam.service.ApiKeyEncryptionDomainService;
import com.codingas.gateway.domain.iam.service.AuthenticationDomainService;
import com.codingas.gateway.domain.iam.service.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * AuthenticationDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationDomainService 测试")
class AuthenticationDomainServiceTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @Mock
    private ApiKeyEncryptionDomainService encryptionService;

    private AuthenticationDomainService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationDomainService(userApiKeyGateway, encryptionService);
    }

    @Nested
    @DisplayName("authenticateUser 方法测试")
    class AuthenticateUserTests {

        @Test
        @DisplayName("认证成功")
        void authenticate_success() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findByKeyPrefix("sk-abc1x")).thenReturn(Optional.of(apiKey));
            when(encryptionService.hashKey("sk-abc1xxxxx")).thenReturn("hash123");

            Identity result = service.authenticateUser("sk-abc1xxxxx");

            assertThat(result.userId()).isEqualTo(50L);
            assertThat(result.credentialId()).isEqualTo(100L);
            assertThat(result.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("API Key 为空 — 抛异常")
        void authenticate_emptyKey() {
            assertThatThrownBy(() -> service.authenticateUser(""))
                    .isInstanceOf(AuthenticationFailedException.class);
            assertThatThrownBy(() -> service.authenticateUser(null))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        @Test
        @DisplayName("Key prefix 未找到 — 抛异常")
        void authenticate_prefixNotFound() {
            when(userApiKeyGateway.findByKeyPrefix("sk-unkno")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticateUser("sk-unknown-key"))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("无效的 API Key");
        }

        @Test
        @DisplayName("Key hash 不匹配 — 抛异常")
        void authenticate_hashMismatch() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findByKeyPrefix("sk-abc1x")).thenReturn(Optional.of(apiKey));
            when(encryptionService.hashKey("sk-abc1xxxxx")).thenReturn("wrong-hash");

            assertThatThrownBy(() -> service.authenticateUser("sk-abc1xxxxx"))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("无效的 API Key");
        }

        @Test
        @DisplayName("Key 已禁用 — 抛异常")
        void authenticate_keyDisabled() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setState(UserApiKeyState.INACTIVE);
            when(userApiKeyGateway.findByKeyPrefix("sk-abc1x")).thenReturn(Optional.of(apiKey));
            when(encryptionService.hashKey("sk-abc1xxxxx")).thenReturn("hash123");

            assertThatThrownBy(() -> service.authenticateUser("sk-abc1xxxxx"))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("已禁用");
        }
    }

    private UserApiKey createSampleApiKey() {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setId(100L);
        apiKey.setUserId(50L);
        apiKey.setKeyHash("hash123");
        apiKey.setKeyPrefix("sk-abc1x");
        apiKey.setState(UserApiKeyState.ACTIVE);
        return apiKey;
    }
}