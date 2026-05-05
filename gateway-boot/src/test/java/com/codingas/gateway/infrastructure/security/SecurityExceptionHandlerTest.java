package com.codingas.gateway.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityExceptionHandler 单元测试
 */
@DisplayName("SecurityExceptionHandler 测试")
class SecurityExceptionHandlerTest {

    private SecurityExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SecurityExceptionHandler();
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("处理 UnauthorizedException")
        void handleUnauthorized() {
            // Given
            SecurityExceptionHandler.UnauthorizedException ex =
                    new SecurityExceptionHandler.UnauthorizedException("Missing API Key");

            // When
            ResponseEntity<Map<String, Object>> response = handler.handleUnauthorized(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("处理 ForbiddenException")
        void handleForbidden() {
            // Given
            SecurityExceptionHandler.ForbiddenException ex =
                    new SecurityExceptionHandler.ForbiddenException("No permission");

            // When
            ResponseEntity<Map<String, Object>> response = handler.handleForbidden(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("FORBIDDEN");
        }

        @Test
        @DisplayName("处理 AuthenticationFailedException")
        void handleAuthenticationFailed() {
            // Given
            SecurityExceptionHandler.AuthenticationFailedException ex =
                    new SecurityExceptionHandler.AuthenticationFailedException("Invalid credentials");

            // When
            ResponseEntity<Map<String, Object>> response = handler.handleAuthenticationFailed(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("AUTHENTICATION_FAILED");
        }

        @Test
        @DisplayName("处理 RateLimitExceededException")
        void handleRateLimitExceeded() {
            // Given
            SecurityExceptionHandler.RateLimitExceededException ex =
                    new SecurityExceptionHandler.RateLimitExceededException("Too many requests");

            // When
            ResponseEntity<Map<String, Object>> response = handler.handleRateLimitExceeded(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("RATE_LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("处理 IpBlockedException")
        void handleIpBlocked() {
            // Given
            SecurityExceptionHandler.IpBlockedException ex =
                    new SecurityExceptionHandler.IpBlockedException("IP blocked");

            // When
            ResponseEntity<Map<String, Object>> response = handler.handleIpBlocked(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("IP_BLOCKED");
        }

        @Test
        @DisplayName("处理通用 Exception")
        void handleGenericException() {
            // Given
            Exception ex = new RuntimeException("Unexpected error");

            // When
            ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("INTERNAL_ERROR");
        }
    }

    @Nested
    @DisplayName("异常类测试")
    class ExceptionClassTests {

        @Test
        @DisplayName("异常类可以正确创建")
        void exceptionClasses_creation() {
            // Given & When
            SecurityExceptionHandler.UnauthorizedException unauthorized =
                    new SecurityExceptionHandler.UnauthorizedException("test");
            SecurityExceptionHandler.ForbiddenException forbidden =
                    new SecurityExceptionHandler.ForbiddenException("test");
            SecurityExceptionHandler.AuthenticationFailedException authFailed =
                    new SecurityExceptionHandler.AuthenticationFailedException("test");
            SecurityExceptionHandler.RateLimitExceededException rateLimit =
                    new SecurityExceptionHandler.RateLimitExceededException("test");
            SecurityExceptionHandler.IpBlockedException ipBlocked =
                    new SecurityExceptionHandler.IpBlockedException("test");

            // Then
            assertThat(unauthorized.getMessage()).isEqualTo("test");
            assertThat(forbidden.getMessage()).isEqualTo("test");
            assertThat(authFailed.getMessage()).isEqualTo("test");
            assertThat(rateLimit.getMessage()).isEqualTo("test");
            assertThat(ipBlocked.getMessage()).isEqualTo("test");
        }
    }
}
