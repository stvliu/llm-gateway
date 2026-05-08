package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.domain.security.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.security.exception.ForbiddenException;
import com.codingas.gateway.domain.security.exception.IpBlockedException;
import com.codingas.gateway.domain.security.exception.RateLimitExceededException;
import com.codingas.gateway.domain.security.exception.SecurityException;
import com.codingas.gateway.domain.security.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
            UnauthorizedException ex = new UnauthorizedException("Missing API Key");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleUnauthorized(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("处理 ForbiddenException")
        void handleForbidden() {
            // Given
            ForbiddenException ex = new ForbiddenException("No permission");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleForbidden(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("FORBIDDEN");
        }

        @Test
        @DisplayName("处理 AuthenticationFailedException")
        void handleAuthenticationFailed() {
            // Given
            AuthenticationFailedException ex = new AuthenticationFailedException("Invalid credentials");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleAuthenticationFailed(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("AUTHENTICATION_FAILED");
        }

        @Test
        @DisplayName("处理 RateLimitExceededException")
        void handleRateLimitExceeded() {
            // Given
            RateLimitExceededException ex = new RateLimitExceededException("Too many requests");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleRateLimitExceeded(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("处理 IpBlockedException")
        void handleIpBlocked() {
            // Given
            IpBlockedException ex = new IpBlockedException("IP blocked");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleIpBlocked(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("IP_BLOCKED");
        }

        @Test
        @DisplayName("处理通用 SecurityException")
        void handleSecurityException() {
            // Given
            SecurityException ex = new SecurityException("CUSTOM_SECURITY", "Custom security error");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleSecurityException(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("CUSTOM_SECURITY");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("Custom security error");
        }
    }

    @Nested
    @DisplayName("异常类测试")
    class ExceptionClassTests {

        @Test
        @DisplayName("异常类可以正确创建")
        void exceptionClasses_creation() {
            // Given & When
            UnauthorizedException unauthorized = new UnauthorizedException("test");
            ForbiddenException forbidden = new ForbiddenException("test");
            AuthenticationFailedException authFailed = new AuthenticationFailedException("test");
            RateLimitExceededException rateLimit = new RateLimitExceededException("test");
            IpBlockedException ipBlocked = new IpBlockedException("test");

            // Then
            assertThat(unauthorized.getMessage()).isEqualTo("test");
            assertThat(forbidden.getMessage()).isEqualTo("test");
            assertThat(authFailed.getMessage()).isEqualTo("test");
            assertThat(rateLimit.getMessage()).isEqualTo("test");
            assertThat(ipBlocked.getMessage()).isEqualTo("test");
        }

        @Test
        @DisplayName("异常类支持无参构造器")
        void exceptionClasses_defaultConstructor() {
            // Given & When
            UnauthorizedException unauthorized = new UnauthorizedException();
            ForbiddenException forbidden = new ForbiddenException();
            AuthenticationFailedException authFailed = new AuthenticationFailedException();
            RateLimitExceededException rateLimit = new RateLimitExceededException();
            IpBlockedException ipBlocked = new IpBlockedException();

            // Then
            assertThat(unauthorized.getCode()).isEqualTo("UNAUTHORIZED");
            assertThat(forbidden.getCode()).isEqualTo("FORBIDDEN");
            assertThat(authFailed.getCode()).isEqualTo("AUTHENTICATION_FAILED");
            assertThat(rateLimit.getCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
            assertThat(ipBlocked.getCode()).isEqualTo("IP_BLOCKED");
        }

        @Test
        @DisplayName("异常类支持 cause 构造器")
        void exceptionClasses_causeConstructor() {
            // Given
            Throwable cause = new RuntimeException("root cause");

            // When
            UnauthorizedException unauthorized = new UnauthorizedException("test", cause);
            ForbiddenException forbidden = new ForbiddenException("test", cause);
            AuthenticationFailedException authFailed = new AuthenticationFailedException("test", cause);
            RateLimitExceededException rateLimit = new RateLimitExceededException("test", cause);
            IpBlockedException ipBlocked = new IpBlockedException("test", cause);

            // Then
            assertThat(unauthorized.getCause()).isEqualTo(cause);
            assertThat(forbidden.getCause()).isEqualTo(cause);
            assertThat(authFailed.getCause()).isEqualTo(cause);
            assertThat(rateLimit.getCause()).isEqualTo(cause);
            assertThat(ipBlocked.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("异常类继承 SecurityException")
        void exceptionClasses_extendSecurityException() {
            // Given & When
            UnauthorizedException unauthorized = new UnauthorizedException();
            ForbiddenException forbidden = new ForbiddenException();
            AuthenticationFailedException authFailed = new AuthenticationFailedException();
            RateLimitExceededException rateLimit = new RateLimitExceededException();
            IpBlockedException ipBlocked = new IpBlockedException();

            // Then
            assertThat(unauthorized).isInstanceOf(SecurityException.class);
            assertThat(forbidden).isInstanceOf(SecurityException.class);
            assertThat(authFailed).isInstanceOf(SecurityException.class);
            assertThat(rateLimit).isInstanceOf(SecurityException.class);
            assertThat(ipBlocked).isInstanceOf(SecurityException.class);
        }
    }
}