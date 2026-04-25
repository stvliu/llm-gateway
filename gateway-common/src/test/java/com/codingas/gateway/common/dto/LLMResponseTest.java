package com.codingas.gateway.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LLMResponse 单元测试
 */
@DisplayName("LLMResponse 测试")
class LLMResponseTest {

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("能通过 Builder 创建完整响应")
        void build_completeResponse() {
            LLMResponse response = LLMResponse.builder()
                    .providerCode("openai")
                    .model("gpt-4")
                    .id("resp_123")
                    .created(1234567890L)
                    .stream(false)
                    .finishReason("stop")
                    .build();

            assertThat(response.getProviderCode()).isEqualTo("openai");
            assertThat(response.getModel()).isEqualTo("gpt-4");
            assertThat(response.getId()).isEqualTo("resp_123");
            assertThat(response.getCreated()).isEqualTo(1234567890L);
            assertThat(response.isStream()).isFalse();
            assertThat(response.getFinishReason()).isEqualTo("stop");
        }

        @Test
        @DisplayName("能创建带 Usage 的响应")
        void build_withUsage() {
            LLMResponse.Usage usage = LLMResponse.Usage.builder()
                    .promptTokens(100)
                    .completionTokens(50)
                    .totalTokens(150)
                    .build();

            LLMResponse response = LLMResponse.builder()
                    .providerCode("openai")
                    .usage(usage)
                    .build();

            assertThat(response.getUsage().getPromptTokens()).isEqualTo(100);
            assertThat(response.getUsage().getCompletionTokens()).isEqualTo(50);
            assertThat(response.getUsage().getTotalTokens()).isEqualTo(150);
        }

        @Test
        @DisplayName("能创建带 Content 的响应")
        void build_withContent() {
            LLMResponse.Content content = LLMResponse.Content.builder()
                    .text("Hello, how can I help you?")
                    .role("assistant")
                    .build();

            LLMResponse response = LLMResponse.builder()
                    .providerCode("openai")
                    .content(content)
                    .build();

            assertThat(response.getContent().getText()).isEqualTo("Hello, how can I help you?");
            assertThat(response.getContent().getRole()).isEqualTo("assistant");
        }
    }

    @Nested
    @DisplayName("error 静态工厂方法测试")
    class ErrorFactoryTests {

        @Test
        @DisplayName("error 方法正确创建错误响应")
        void error_createsCorrectErrorResponse() {
            LLMResponse response = LLMResponse.error("openai", "API key is invalid");

            assertThat(response.getProviderCode()).isEqualTo("openai");
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getMessage()).isEqualTo("API key is invalid");
            assertThat(response.getError().getType()).isEqualTo("api_error");
        }
    }

    @Nested
    @DisplayName("嵌套类测试")
    class NestedClassTests {

        @Test
        @DisplayName("Content 嵌套类正常工作")
        void content_nestedClassWorks() {
            LLMResponse.Content content = LLMResponse.Content.builder()
                    .text("Test response")
                    .role("assistant")
                    .build();

            assertThat(content.getText()).isEqualTo("Test response");
            assertThat(content.getRole()).isEqualTo("assistant");
        }

        @Test
        @DisplayName("ToolCall 嵌套类正常工作")
        void toolCall_nestedClassWorks() {
            LLMResponse.ToolCall toolCall = LLMResponse.ToolCall.builder()
                    .id("call_abc")
                    .type("function")
                    .function(LLMResponse.FunctionCall.builder()
                            .name("search")
                            .arguments("{}")
                            .build())
                    .build();

            assertThat(toolCall.getId()).isEqualTo("call_abc");
            assertThat(toolCall.getFunction().getName()).isEqualTo("search");
        }

        @Test
        @DisplayName("FunctionCall 嵌套类正常工作")
        void functionCall_nestedClassWorks() {
            LLMResponse.FunctionCall functionCall = LLMResponse.FunctionCall.builder()
                    .name("get_weather")
                    .arguments("{\"city\":\"Shanghai\"}")
                    .build();

            assertThat(functionCall.getName()).isEqualTo("get_weather");
            assertThat(functionCall.getArguments()).isEqualTo("{\"city\":\"Shanghai\"}");
        }

        @Test
        @DisplayName("Usage 嵌套类正常工作")
        void usage_nestedClassWorks() {
            LLMResponse.Usage usage = LLMResponse.Usage.builder()
                    .promptTokens(200)
                    .completionTokens(100)
                    .totalTokens(300)
                    .build();

            assertThat(usage.getPromptTokens()).isEqualTo(200);
            assertThat(usage.getCompletionTokens()).isEqualTo(100);
            assertThat(usage.getTotalTokens()).isEqualTo(300);
        }

        @Test
        @DisplayName("Error 嵌套类正常工作")
        void error_nestedClassWorks() {
            LLMResponse.Error error = LLMResponse.Error.builder()
                    .type("rate_limit_error")
                    .code("429")
                    .message("Rate limit exceeded")
                    .param("requests")
                    .build();

            assertThat(error.getType()).isEqualTo("rate_limit_error");
            assertThat(error.getCode()).isEqualTo("429");
            assertThat(error.getMessage()).isEqualTo("Rate limit exceeded");
            assertThat(error.getParam()).isEqualTo("requests");
        }
    }

    @Nested
    @DisplayName("额外数据测试")
    class ExtraDataTests {

        @Test
        @DisplayName("额外数据设置正确")
        void extraData_setCorrectly() {
            Map<String, Object> extraData = Map.of("customField", "value123");
            LLMResponse response = LLMResponse.builder()
                    .extraData(extraData)
                    .build();

            assertThat(response.getExtraData()).isEqualTo(extraData);
        }
    }
}
