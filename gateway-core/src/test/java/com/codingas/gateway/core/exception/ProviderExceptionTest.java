package com.codingas.gateway.core.exception;

import com.codingas.gateway.core.domain.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ProviderException 单元测试
 */
@DisplayName("ProviderException Tests")
class ProviderExceptionTest {

    @Nested
    @DisplayName("构造")
    class ConstructorTests {

        @Test
        @DisplayName("基本构造设置所有字段")
        void basicConstructor_setsAllFields() {
            ProviderException ex = new ProviderException(
                    "openai",
                    "API rate limit exceeded",
                    ProviderErrorType.RATE_LIMIT_ERROR,
                    true
            );

            assertThat(ex.getProviderCode()).isEqualTo("openai");
            assertThat(ex.getModelId()).isNull();
            assertThat(ex.getErrorType()).isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR);
            assertThat(ex.isRetryable()).isTrue();
            assertThat(ex.getMessage()).isEqualTo("API rate limit exceeded");
            assertThat(ex.getErrorCode()).isEqualTo("PROVIDER_ERROR");
        }

        @Test
        @DisplayName("带 modelId 构造设置所有字段")
        void withModelId_setsAllFields() {
            ProviderException ex = new ProviderException(
                    "openai",
                    "gpt-4",
                    "Model not found",
                    ProviderErrorType.INVALID_REQUEST,
                    false
            );

            assertThat(ex.getProviderCode()).isEqualTo("openai");
            assertThat(ex.getModelId()).isEqualTo("gpt-4");
            assertThat(ex.getErrorType()).isEqualTo(ProviderErrorType.INVALID_REQUEST);
            assertThat(ex.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("带根因构造正确设置")
        void withCause_setsCorrectCause() {
            RuntimeException cause = new RuntimeException("Connection timeout");
            ProviderException ex = new ProviderException(
                    "anthropic",
                    "Request timeout",
                    ProviderErrorType.TIMEOUT_ERROR,
                    true,
                    cause
            );

            assertThat(ex.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("重试行为")
    class RetryBehaviorTests {

        @Test
        @DisplayName("限流错误应可重试")
        void rateLimitError_isRetryable() {
            ProviderException ex = new ProviderException(
                    "openai",
                    "Rate limit",
                    ProviderErrorType.RATE_LIMIT_ERROR,
                    true
            );

            assertThat(ex.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("认证错误不应重试")
        void authError_isNotRetryable() {
            ProviderException ex = new ProviderException(
                    "openai",
                    "Invalid API key",
                    ProviderErrorType.AUTHENTICATION_ERROR,
                    false
            );

            assertThat(ex.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("配额超限不应重试")
        void quotaExceeded_isNotRetryable() {
            ProviderException ex = new ProviderException(
                    "openai",
                    "Quota exceeded",
                    ProviderErrorType.QUOTA_EXCEEDED,
                    false
            );

            assertThat(ex.isRetryable()).isFalse();
        }
    }

    @Nested
    @DisplayName("继承关系")
    class InheritanceTests {

        @Test
        @DisplayName("继承自 GatewayException")
        void extendsGatewayException() {
            ProviderException ex = new ProviderException(
                    "openai",
                    "Error",
                    ProviderErrorType.UNKNOWN_ERROR,
                    false
            );

            assertThat(ex).isInstanceOf(GatewayException.class);
        }
    }
}