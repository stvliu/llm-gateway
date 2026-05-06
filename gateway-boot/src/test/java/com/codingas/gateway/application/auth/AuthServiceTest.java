package com.codingas.gateway.application.auth;

import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.service.AuthenticationDomainService;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationDomainService authenticationService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Nested
    @DisplayName("authenticate(String apiKey, String clientIp) 测试")
    class AuthenticateTests {

        @Test
        @DisplayName("当 API Key 有效时，返回认证结果")
        void authenticate_validApiKey_returnsAuthResult() {
            // given
            String apiKey = "sk-test-12345";
            String clientIp = "192.168.1.100";
            UserAuthResult expectedResult = new UserAuthResult(
                1L, "USER", 100L
            );
            when(authenticationService.authenticate(apiKey)).thenReturn(expectedResult);

            // when
            UserAuthResult result = authService.authenticate(apiKey, clientIp);

            // then
            assertThat(result).isEqualTo(expectedResult);
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.apiKeyId()).isEqualTo(100L);
            verify(authenticationService).authenticate(apiKey);
        }

        @Test
        @DisplayName("当 API Key 无效时，返回 null")
        void authenticate_invalidApiKey_returnsNull() {
            // given
            String apiKey = "invalid-key";
            String clientIp = "192.168.1.100";
            when(authenticationService.authenticate(apiKey)).thenReturn(null);

            // when
            UserAuthResult result = authService.authenticate(apiKey, clientIp);

            // then
            assertThat(result).isNull();
            verify(authenticationService).authenticate(apiKey);
        }

        @Test
        @DisplayName("当 API Key 为空时，返回 null")
        void authenticate_emptyApiKey_returnsNull() {
            // given
            String apiKey = "";
            String clientIp = "10.0.0.1";
            when(authenticationService.authenticate(apiKey)).thenReturn(null);

            // when
            UserAuthResult result = authService.authenticate(apiKey, clientIp);

            // then
            assertThat(result).isNull();
            verify(authenticationService).authenticate(apiKey);
        }
    }

    @Nested
    @DisplayName("checkPermission(Long userId, String resource, String action) 测试")
    class CheckPermissionTests {

        @Test
        @DisplayName("当用户是管理员时，返回 true")
        void checkPermission_adminUser_returnsTrue() {
            // given
            Long userId = 1L;
            String resource = "any:resource";
            String action = "any:action";
            User adminUser = new User();
            adminUser.setId(userId);
            adminUser.setRole("ADMIN");

            when(authenticationService.getUserById(userId)).thenReturn(Optional.of(adminUser));

            // when
            boolean result = authService.checkPermission(userId, resource, action);

            // then
            assertThat(result).isTrue();
            verify(authenticationService).getUserById(userId);
        }

        @Test
        @DisplayName("当用户不存在时，返回 false")
        void checkPermission_userNotFound_returnsFalse() {
            // given
            Long userId = 999L;
            String resource = "chat:completion";
            String action = "create";
            when(authenticationService.getUserById(userId)).thenReturn(Optional.empty());

            // when
            boolean result = authService.checkPermission(userId, resource, action);

            // then
            assertThat(result).isFalse();
            verify(authenticationService).getUserById(userId);
        }

        @Test
        @DisplayName("当用户是普通用户时，返回 false（当前简化实现）")
        void checkPermission_regularUser_returnsFalse() {
            // given
            Long userId = 2L;
            String resource = "admin:users";
            String action = "delete";
            User regularUser = new User();
            regularUser.setId(userId);
            regularUser.setRole("USER");

            when(authenticationService.getUserById(userId)).thenReturn(Optional.of(regularUser));

            // when
            boolean result = authService.checkPermission(userId, resource, action);

            // then
            assertThat(result).isFalse();
            verify(authenticationService).getUserById(userId);
        }
    }
}
