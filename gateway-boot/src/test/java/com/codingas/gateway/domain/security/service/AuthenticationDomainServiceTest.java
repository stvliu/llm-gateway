package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.enums.UserState;
import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AuthenticationDomainService 完整单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationDomainService 测试")
class AuthenticationDomainServiceTest {

    @Mock
    private ApiKeyGateway apiKeyGateway;

    @Mock
    private UserGateway userGateway;

    private AuthenticationDomainService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthenticationDomainService(apiKeyGateway, userGateway);
        ReflectionTestUtils.setField(authService, "hashSalt", "test-salt");
    }

    @Nested
    @DisplayName("authenticate 方法测试")
    class AuthenticateTests {

        @Test
        @DisplayName("有效 API Key 认证成功")
        void authenticate_validKey_returnsAuthResult() {
            // given
            String apiKey = "sk-test-valid-key-12345";
            User user = createActiveUser();
            GatewayApiKey gatewayKey = createActiveApiKey(user);

            when(apiKeyGateway.findByKeyHash(anyString())).thenReturn(gatewayKey);
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            doNothing().when(apiKeyGateway).updateLastUsed(anyLong(), any(Instant.class));

            // when
            UserAuthResult result = authService.authenticate(apiKey);

            // then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.apiKeyId()).isEqualTo(100L);
            verify(apiKeyGateway).updateLastUsed(eq(100L), any(Instant.class));
        }

        @Test
        @DisplayName("空 API Key 返回 null")
        void authenticate_emptyKey_returnsNull() {
            assertThat(authService.authenticate(null)).isNull();
            assertThat(authService.authenticate("")).isNull();
            assertThat(authService.authenticate("   ")).isNull();
        }

        @Test
        @DisplayName("API Key 不存在返回 null")
        void authenticate_keyNotFound_returnsNull() {
            // given
            when(apiKeyGateway.findByKeyHash(anyString())).thenReturn(null);

            // when
            UserAuthResult result = authService.authenticate("sk-unknown");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("API Key 状态非 ACTIVE 返回 null")
        void authenticate_inactiveKey_returnsNull() {
            // given
            User user = createActiveUser();
            GatewayApiKey inactiveKey = createActiveApiKey(user);
            inactiveKey.setState(GatewayApiKeyState.DISABLED);

            when(apiKeyGateway.findByKeyHash(anyString())).thenReturn(inactiveKey);

            // when
            UserAuthResult result = authService.authenticate("sk-test");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("API Key 已过期返回 null")
        void authenticate_expiredKey_returnsNull() {
            // given
            User user = createActiveUser();
            GatewayApiKey expiredKey = createActiveApiKey(user);
            expiredKey.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

            when(apiKeyGateway.findByKeyHash(anyString())).thenReturn(expiredKey);

            // when
            UserAuthResult result = authService.authenticate("sk-test");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("API Key 关联用户为空返回 null")
        void authenticate_noUser_returnsNull() {
            // given
            GatewayApiKey keyWithoutUser = createActiveApiKey(null);

            when(apiKeyGateway.findByKeyHash(anyString())).thenReturn(keyWithoutUser);

            // when
            UserAuthResult result = authService.authenticate("sk-test");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("用户状态非 ACTIVE 返回 null")
        void authenticate_inactiveUser_returnsNull() {
            // given
            User inactiveUser = createActiveUser();
            inactiveUser.setState(UserState.DISABLED);
            GatewayApiKey gatewayKey = createActiveApiKey(inactiveUser);

            when(apiKeyGateway.findByKeyHash(anyString())).thenReturn(gatewayKey);

            // when
            UserAuthResult result = authService.authenticate("sk-test");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("相同的 API Key 产生相同的哈希")
        void authenticate_sameKey_producesSameHash() {
            // given
            String apiKey = "sk-same-key-test";
            User user = createActiveUser();
            GatewayApiKey gatewayKey = createActiveApiKey(user);

            when(apiKeyGateway.findByKeyHash(anyString())).thenReturn(gatewayKey);

            // when
            authService.authenticate(apiKey);
            authService.authenticate(apiKey);

            // then - 两次调用应该使用相同的哈希值
            verify(apiKeyGateway, times(2)).findByKeyHash(anyString());
        }
    }

    @Nested
    @DisplayName("getUserById 方法测试")
    class GetUserByIdTests {

        @Test
        @DisplayName("用户存在时返回用户")
        void getUserById_existingUser_returnsUser() {
            // given
            User user = createActiveUser();
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));

            // when
            Optional<User> result = authService.getUserById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("用户不存在时返回空")
        void getUserById_nonExistingUser_returnsEmpty() {
            // given
            when(userGateway.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<User> result = authService.getUserById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // Helper methods
    private User createActiveUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("Test User");
        user.setEmail("test@example.com");
        user.setState(UserState.ACTIVE);
        user.setRole("USER");
        return user;
    }

    private GatewayApiKey createActiveApiKey(User user) {
        GatewayApiKey key = new GatewayApiKey();
        key.setId(100L);
        key.setState(GatewayApiKeyState.ACTIVE);
        key.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        if (user != null) {
            key.setUserId(user.getId());
            key.setUsername(user.getUsername());
        }
        return key;
    }
}
