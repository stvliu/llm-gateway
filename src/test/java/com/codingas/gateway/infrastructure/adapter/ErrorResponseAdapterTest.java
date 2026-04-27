package com.codingas.gateway.infrastructure.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorResponseAdapter 单元测试
 *
 * @author Liu Ye
 */
@DisplayName("ErrorResponseAdapter 测试")
class ErrorResponseAdapterTest {

    private final ErrorResponseAdapter adapter = new ErrorResponseAdapter();

    @Nested
    @DisplayName("toOpenAIError 测试")
    class ToOpenAIErrorTest {

        @Test
        @DisplayName("应返回正确的 OpenAI 错误格式")
        void shouldReturnCorrectOpenAIErrorFormat() {
            Map<String, Object> result = adapter.toOpenAIError("test message", "test_code", 400);

            assertThat(result).containsKey("error");
            Map<?, ?> error = (Map<?, ?>) result.get("error");
            assertThat(error.get("message")).isEqualTo("test message");
            assertThat(error.get("type")).isEqualTo("invalid_request_error");
            assertThat(error.get("code")).isEqualTo("test_code");
            assertThat(error.get("status")).isEqualTo(400);
        }

        @Test
        @DisplayName("null errorMessage 应使用默认值")
        void shouldUseDefaultMessageWhenNull() {
            Map<String, Object> result = adapter.toOpenAIError(null, null, 400);

            Map<?, ?> error = (Map<?, ?>) result.get("error");
            assertThat(error.get("message")).isEqualTo("Unknown error");
            assertThat(error.get("code")).isEqualTo("invalid_request");
        }

        @ParameterizedTest
        @CsvSource({
                "400, invalid_request_error",
                "401, invalid_request_error",
                "403, invalid_request_error",
                "404, invalid_request_error",
                "429, rate_limit_error",
                "500, server_error",
                "502, server_error",
                "503, server_error"
        })
        @DisplayName("应正确映射 HTTP 状态码到 OpenAI 错误类型")
        void shouldMapHttpStatusToOpenAIErrorType(int httpStatus, String expectedType) {
            Map<String, Object> result = adapter.toOpenAIError("msg", null, httpStatus);

            Map<?, ?> error = (Map<?, ?>) result.get("error");
            assertThat(error.get("type")).isEqualTo(expectedType);
        }

        @ParameterizedTest
        @CsvSource({
                "401, invalid_api_key",
                "403, permission_not_found",
                "404, not_found",
                "429, rate_limit_exceeded",
                "500, internal_server_error",
                "400, invalid_request"
        })
        @DisplayName("应正确映射 HTTP 状态码到 OpenAI 错误代码")
        void shouldMapHttpStatusToOpenAIErrorCode(int httpStatus, String expectedCode) {
            Map<String, Object> result = adapter.toOpenAIError("msg", null, httpStatus);

            Map<?, ?> error = (Map<?, ?>) result.get("error");
            assertThat(error.get("code")).isEqualTo(expectedCode);
        }
    }

    @Nested
    @DisplayName("toAnthropicError 测试")
    class ToAnthropicErrorTest {

        @Test
        @DisplayName("应返回正确的 Anthropic 错误格式")
        void shouldReturnCorrectAnthropicErrorFormat() {
            Map<String, Object> result = adapter.toAnthropicError("test message", "test_code", 429);

            assertThat(result).containsKey("type");
            assertThat(result.get("type")).isEqualTo("rate_limit_error");
            assertThat(result.get("message")).isEqualTo("test message");
            assertThat(result.get("code")).isEqualTo("test_code");
            assertThat(result.get("status")).isEqualTo(429);
        }

        @Test
        @DisplayName("null errorMessage 应使用默认值")
        void shouldUseDefaultMessageWhenNull() {
            Map<String, Object> result = adapter.toAnthropicError(null, null, 500);

            assertThat(result.get("message")).isEqualTo("Unknown error");
            assertThat(result.get("code")).isEqualTo("internal_server_error");
        }

        @ParameterizedTest
        @CsvSource({
                "400, invalid_request_error",
                "401, invalid_request_error",
                "403, invalid_request_error",
                "404, invalid_request_error",
                "429, rate_limit_error",
                "500, error",
                "502, error",
                "503, error"
        })
        @DisplayName("应正确映射 HTTP 状态码到 Anthropic 错误类型")
        void shouldMapHttpStatusToAnthropicErrorType(int httpStatus, String expectedType) {
            Map<String, Object> result = adapter.toAnthropicError("msg", null, httpStatus);

            assertThat(result.get("type")).isEqualTo(expectedType);
        }

        @ParameterizedTest
        @CsvSource({
                "401, authentication_error",
                "403, permission_denied",
                "404, not_found_error",
                "429, rate_limit_exceeded",
                "500, internal_server_error",
                "400, invalid_request_error"
        })
        @DisplayName("应正确映射 HTTP 状态码到 Anthropic 错误代码")
        void shouldMapHttpStatusToAnthropicErrorCode(int httpStatus, String expectedCode) {
            Map<String, Object> result = adapter.toAnthropicError("msg", null, httpStatus);

            assertThat(result.get("code")).isEqualTo(expectedCode);
        }
    }

    @Nested
    @DisplayName("fromProviderError 测试")
    class FromProviderErrorTest {

        @Test
        @DisplayName("providerCode 为 anthropic 时应返回 Anthropic 格式")
        void shouldReturnAnthropicFormatWhenProviderIsAnthropic() {
            Map<String, Object> result = adapter.fromProviderError("anthropic", "rate limit", 429);

            assertThat(result).containsKey("type");
            assertThat(result.get("type")).isEqualTo("rate_limit_error");
            assertThat(result.get("message")).isEqualTo("rate limit");
        }

        @Test
        @DisplayName("providerCode 为其他值时应返回 OpenAI 格式")
        void shouldReturnOpenAIFormatWhenProviderIsOther() {
            Map<String, Object> result = adapter.fromProviderError("openai", "server error", 500);

            assertThat(result).containsKey("error");
            Map<?, ?> error = (Map<?, ?>) result.get("error");
            assertThat(error.get("type")).isEqualTo("server_error");
            assertThat(error.get("message")).isEqualTo("server error");
        }

        @Test
        @DisplayName("未知 providerCode 应默认使用 OpenAI 格式")
        void shouldDefaultToOpenAIFormatForUnknownProvider() {
            Map<String, Object> result = adapter.fromProviderError("unknown", "some error", 400);

            assertThat(result).containsKey("error");
            assertThat(result).doesNotContainKey("type");
        }
    }
}