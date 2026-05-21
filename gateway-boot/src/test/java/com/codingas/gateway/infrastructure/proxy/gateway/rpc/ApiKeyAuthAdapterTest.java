package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.security.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.security.service.AuthenticationDomainService;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ApiKeyAuthAdapter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyAuthAdapter 测试")
class ApiKeyAuthAdapterTest {

    @Mock
    private AuthenticationDomainService authenticationService;

    private ApiKeyAuthAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ApiKeyAuthAdapter(authenticationService);
    }

    @Nested
    @DisplayName("authenticate 方法测试")
    class AuthenticateTests {

        @Test
        @DisplayName("有效 API Key 返回用户 ID")
        void authenticate_validKey_returnsUserId() {
            String apiKey = "sk-valid-key";
            UserAuthResult authResult = UserAuthResult.newArch(1L, "USER", 10L, 1L);
            when(authenticationService.authenticateUser(apiKey)).thenReturn(authResult);

            Long result = adapter.authenticate(apiKey);

            assertThat(result).isEqualTo(1L);
            verify(authenticationService).authenticateUser(apiKey);
        }

        @Test
        @DisplayName("null API Key 返回 null")
        void authenticate_nullKey_returnsNull() {
            Long result = adapter.authenticate(null);

            assertThat(result).isNull();
            verify(authenticationService, never()).authenticateUser(anyString());
        }

        @Test
        @DisplayName("空白 API Key 返回 null")
        void authenticate_blankKey_returnsNull() {
            Long result = adapter.authenticate("   ");

            assertThat(result).isNull();
            verify(authenticationService, never()).authenticateUser(anyString());
        }

        @Test
        @DisplayName("空字符串 API Key 返回 null")
        void authenticate_emptyKey_returnsNull() {
            Long result = adapter.authenticate("");

            assertThat(result).isNull();
            verify(authenticationService, never()).authenticateUser(anyString());
        }

        @Test
        @DisplayName("认证服务返回 null 时返回 null")
        void authenticate_serviceReturnsNull_returnsNull() {
            String apiKey = "sk-invalid-key";
            when(authenticationService.authenticateUser(apiKey)).thenReturn(null);

            Long result = adapter.authenticate(apiKey);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("认证服务抛出异常时返回 null")
        void authenticate_serviceThrowsException_returnsNull() {
            String apiKey = "sk-error-key";
            when(authenticationService.authenticateUser(apiKey))
                    .thenThrow(new RuntimeException("Authentication error"));

            Long result = adapter.authenticate(apiKey);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("认证服务抛出安全异常时返回 null")
        void authenticate_serviceThrowsSecurityException_returnsNull() {
            String apiKey = "sk-security-error";
            when(authenticationService.authenticateUser(apiKey))
                    .thenThrow(new AuthenticationFailedException("Invalid credentials"));

            Long result = adapter.authenticate(apiKey);

            assertThat(result).isNull();
        }
    }
}
