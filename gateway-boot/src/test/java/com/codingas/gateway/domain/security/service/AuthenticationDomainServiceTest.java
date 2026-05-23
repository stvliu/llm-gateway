package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
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
import static org.mockito.Mockito.*;

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
        @DisplayName("认证成功 — UserApiKey 活跃")
        void authenticateUser_success() {
            String apiKey = "sk-test1234567890abcdef";

            UserApiKey userApiKey = new UserApiKey();
            userApiKey.setId(101L);
            userApiKey.setUserId(1L);
            userApiKey.setTeamId(300L);
            userApiKey.setKeyPlain(apiKey);
            userApiKey.setKeyPrefix("sk-test1");
            userApiKey.setKeyHash("hashed-test-key");
            userApiKey.setState(UserApiKeyState.ACTIVE);

            when(userApiKeyGateway.findByKeyPrefix("sk-test1")).thenReturn(Optional.of(userApiKey));
            when(encryptionService.hashKey(apiKey)).thenReturn("hashed-test-key");

            UserAuthResult result = service.authenticateUser(apiKey);

            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.role()).isEqualTo("user");
            assertThat(result.userApiKeyId()).isEqualTo(101L);
            assertThat(result.teamId()).isEqualTo(300L);
        }

        @Test
        @DisplayName("认证失败 — UserApiKey 不存在")
        void authenticateUser_keyNotFound_throwsException() {
            String apiKey = "sk-unknown12345678";

            when(userApiKeyGateway.findByKeyPrefix("sk-unkno")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticateUser(apiKey))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("无效的 API Key");
        }

        @Test
        @DisplayName("认证失败 — UserApiKey 已禁用")
        void authenticateUser_keyDisabled_throwsException() {
            String apiKey = "sk-disabled1234567";

            UserApiKey userApiKey = new UserApiKey();
            userApiKey.setId(101L);
            userApiKey.setUserId(1L);
            userApiKey.setTeamId(300L);
            userApiKey.setKeyPlain(apiKey);
            userApiKey.setKeyPrefix("sk-disab");
            userApiKey.setKeyHash("hashed-disabled-key");
            userApiKey.setState(UserApiKeyState.INACTIVE);

            when(userApiKeyGateway.findByKeyPrefix("sk-disab")).thenReturn(Optional.of(userApiKey));
            when(encryptionService.hashKey(apiKey)).thenReturn("hashed-disabled-key");

            assertThatThrownBy(() -> service.authenticateUser(apiKey))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("API Key 已禁用");
        }

        @Test
        @DisplayName("空 API Key 抛出异常")
        void authenticateUser_blankKey_throwsException() {
            assertThatThrownBy(() -> service.authenticateUser(""))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("API Key 不能为空");
        }

        @Test
        @DisplayName("null API Key 抛出异常")
        void authenticateUser_nullKey_throwsException() {
            assertThatThrownBy(() -> service.authenticateUser(null))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("API Key 不能为空");
        }
    }
}
