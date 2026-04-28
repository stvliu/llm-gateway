package com.codingas.gateway.application.auth;

import com.codingas.gateway.common.enums.UserRole;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.service.AuthenticationService;
import com.codingas.gateway.domain.security.service.RbacService;
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
    private AuthenticationService authenticationService;

    @Mock
    private RbacService rbacService;

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
                1L, "user-001", UserRole.DEVELOPER, 100L, "key-code-001"
            );
            when(authenticationService.authenticate(apiKey)).thenReturn(expectedResult);

            // when
            UserAuthResult result = authService.authenticate(apiKey, clientIp);

            // then
            assertThat(result).isEqualTo(expectedResult);
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.apiKeyCode()).isEqualTo("key-code-001");
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
        @DisplayName("当用户存在且有权限时，返回 true")
        void checkPermission_userHasPermission_returnsTrue() {
            // given
            Long userId = 1L;
            String resource = "chat:completion";
            String action = "create";
            User user = new User();
            user.setId(userId);
            user.setUserCode("user-001");

            when(authenticationService.getUserById(userId)).thenReturn(Optional.of(user));
            when(rbacService.hasPermission(user, resource, action)).thenReturn(true);

            // when
            boolean result = authService.checkPermission(userId, resource, action);

            // then
            assertThat(result).isTrue();
            verify(authenticationService).getUserById(userId);
            verify(rbacService).hasPermission(user, resource, action);
        }

        @Test
        @DisplayName("当用户存在但无权限时，返回 false")
        void checkPermission_userHasNoPermission_returnsFalse() {
            // given
            Long userId = 2L;
            String resource = "admin:users";
            String action = "delete";
            User user = new User();
            user.setId(userId);
            user.setUserCode("user-002");

            when(authenticationService.getUserById(userId)).thenReturn(Optional.of(user));
            when(rbacService.hasPermission(user, resource, action)).thenReturn(false);

            // when
            boolean result = authService.checkPermission(userId, resource, action);

            // then
            assertThat(result).isFalse();
            verify(authenticationService).getUserById(userId);
            verify(rbacService).hasPermission(user, resource, action);
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
            verifyNoInteractions(rbacService);
        }

        @Test
        @DisplayName("当用户是管理员时，直接返回 true")
        void checkPermission_adminUser_returnsTrue() {
            // given
            Long userId = 1L;
            String resource = "any:resource";
            String action = "any:action";
            User adminUser = new User();
            adminUser.setId(userId);
            adminUser.setUserCode("admin-001");

            when(authenticationService.getUserById(userId)).thenReturn(Optional.of(adminUser));
            when(rbacService.hasPermission(adminUser, resource, action)).thenReturn(true);

            // when
            boolean result = authService.checkPermission(userId, resource, action);

            // then
            assertThat(result).isTrue();
            verify(authenticationService).getUserById(userId);
            verify(rbacService).hasPermission(adminUser, resource, action);
        }
    }
}
