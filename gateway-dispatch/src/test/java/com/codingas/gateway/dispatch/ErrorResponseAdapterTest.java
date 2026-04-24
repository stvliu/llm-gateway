package com.codingas.gateway.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorResponseAdapter 单元测试
 */
class ErrorResponseAdapterTest {

    private ErrorResponseAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ErrorResponseAdapter();
    }

    @Nested
    @DisplayName("OpenAI 错误格式")
    class OpenAIErrorFormat {

        @Test
        @DisplayName("转换 401 错误")
        void map401Error() {
            Map<String, Object> error = adapter.toOpenAIError("Invalid API key", null, 401);

            assertThat(error).containsKey("error");
            @SuppressWarnings("unchecked")
            Map<String, Object> errorDetails = (Map<String, Object>) error.get("error");
            assertThat(errorDetails.get("message")).isEqualTo("Invalid API key");
            assertThat(errorDetails.get("code")).isEqualTo("invalid_api_key");
            assertThat(errorDetails.get("type")).isEqualTo("invalid_request_error");
            assertThat(errorDetails.get("status")).isEqualTo(401);
        }

        @Test
        @DisplayName("转换 429 速率限制错误")
        void map429RateLimitError() {
            Map<String, Object> error = adapter.toOpenAIError("Rate limit exceeded", null, 429);

            @SuppressWarnings("unchecked")
            Map<String, Object> errorDetails = (Map<String, Object>) error.get("error");
            assertThat(errorDetails.get("code")).isEqualTo("rate_limit_exceeded");
            assertThat(errorDetails.get("type")).isEqualTo("rate_limit_error");
        }

        @Test
        @DisplayName("转换 500 服务器错误")
        void map500ServerError() {
            Map<String, Object> error = adapter.toOpenAIError("Internal server error", null, 500);

            @SuppressWarnings("unchecked")
            Map<String, Object> errorDetails = (Map<String, Object>) error.get("error");
            assertThat(errorDetails.get("type")).isEqualTo("server_error");
            assertThat(errorDetails.get("code")).isEqualTo("internal_server_error");
        }

        @Test
        @DisplayName("自定义错误码")
        void customErrorCode() {
            Map<String, Object> error = adapter.toOpenAIError("Custom error", "custom_code", 400);

            @SuppressWarnings("unchecked")
            Map<String, Object> errorDetails = (Map<String, Object>) error.get("error");
            assertThat(errorDetails.get("code")).isEqualTo("custom_code");
        }
    }

    @Nested
    @DisplayName("Anthropic 错误格式")
    class AnthropicErrorFormat {

        @Test
        @DisplayName("转换 Anthropic 错误")
        void mapAnthropicError() {
            Map<String, Object> error = adapter.toAnthropicError("Invalid API key", null, 401);

            assertThat(error.get("type")).isEqualTo("invalid_request_error");
            assertThat(error.get("message")).isEqualTo("Invalid API key");
            assertThat(error.get("code")).isEqualTo("authentication_error");
            assertThat(error.get("status")).isEqualTo(401);
        }

        @Test
        @DisplayName("转换 Anthropic 速率限制错误")
        void mapAnthropicRateLimitError() {
            Map<String, Object> error = adapter.toAnthropicError("Rate limit exceeded", null, 429);

            assertThat(error.get("type")).isEqualTo("rate_limit_error");
            assertThat(error.get("code")).isEqualTo("rate_limit_exceeded");
        }
    }

    @Nested
    @DisplayName("提供商错误转换")
    class ProviderErrorConversion {

        @Test
        @DisplayName("从 Anthropic 提供商错误转换")
        void fromAnthropicProviderError() {
            Map<String, Object> error = adapter.fromProviderError("anthropic", "Test error", 400);

            assertThat(error.get("type")).isEqualTo("invalid_request_error");
        }

        @Test
        @DisplayName("从 OpenAI 提供商错误转换")
        void fromOpenAIProviderError() {
            Map<String, Object> error = adapter.fromProviderError("openai", "Test error", 400);

            assertThat(error).containsKey("error");
        }

        @Test
        @DisplayName("未知提供商默认使用 OpenAI 格式")
        void unknownProviderDefaultsToOpenAI() {
            Map<String, Object> error = adapter.fromProviderError("unknown", "Test error", 400);

            assertThat(error).containsKey("error");
        }
    }
}
