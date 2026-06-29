package com.codingas.gateway.application.auth;

import com.codingas.gateway.domain.iam.service.AuthenticationDomainService;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
            String apiKey = "sk-test-12345";
            String clientIp = "192.168.1.100";
            Identity expectedResult = Identity.of(1L, "user", 100L, null);
            when(authenticationService.authenticateUser(apiKey)).thenReturn(expectedResult);

            Identity result = authService.authenticate(apiKey, clientIp);

            assertThat(result).isEqualTo(expectedResult);
            assertThat(result.userId()).isEqualTo(1L);
            verify(authenticationService).authenticateUser(apiKey);
        }

        @Test
        @DisplayName("当 API Key 无效时，返回 null")
        void authenticate_invalidApiKey_returnsNull() {
            String apiKey = "invalid-key";
            String clientIp = "192.168.1.100";
            when(authenticationService.authenticateUser(apiKey)).thenThrow(
                    new com.codingas.gateway.domain.iam.exception.AuthenticationFailedException("无效的 API Key"));

            Identity result = authService.authenticate(apiKey, clientIp);

            assertThat(result).isNull();
        }
    }

}
