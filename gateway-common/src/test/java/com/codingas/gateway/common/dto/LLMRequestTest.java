package com.codingas.gateway.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LLMRequest 单元测试
 */
@DisplayName("LLMRequest 测试")
class LLMRequestTest {

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("能通过 Builder 创建完整请求")
        void build_completeRequest() {
            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .temperature(0.7)
                    .maxTokens(1000)
                    .stream(false)
                    .build();

            assertThat(request.getModel()).isEqualTo("gpt-4");
            assertThat(request.getTemperature()).isEqualTo(0.7);
            assertThat(request.getMaxTokens()).isEqualTo(1000);
            assertThat(request.isStream()).isFalse();
        }

        @Test
        @DisplayName("能创建带消息的请求")
        void build_withMessages() {
            LLMRequest.Message message = LLMRequest.Message.builder()
                    .role("user")
                    .content("Hello")
                    .build();

            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .messages(List.of(message))
                    .build();

            assertThat(request.getMessages()).hasSize(1);
            assertThat(request.getMessages().get(0).getRole()).isEqualTo("user");
            assertThat(request.getMessages().get(0).getContent()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("能创建带工具定义的请求")
        void build_withTools() {
            LLMRequest.ToolDefinition tool = LLMRequest.ToolDefinition.builder()
                    .type("function")
                    .function(LLMRequest.Function.builder()
                            .name("get_weather")
                            .description("Get weather info")
                            .parameters("{}")
                            .build())
                    .build();

            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .tools(List.of(tool))
                    .build();

            assertThat(request.getTools()).hasSize(1);
            assertThat(request.getTools().get(0).getFunction().getName()).isEqualTo("get_weather");
        }
    }

    @Nested
    @DisplayName("字段设置测试")
    class FieldTests {

        @Test
        @DisplayName("流式请求标记正确")
        void stream_flagSetCorrectly() {
            LLMRequest request = LLMRequest.builder().stream(true).build();
            assertThat(request.isStream()).isTrue();
        }

        @Test
        @DisplayName("系统提示设置正确")
        void systemPrompt_setCorrectly() {
            LLMRequest request = LLMRequest.builder()
                    .systemPrompt("You are a helpful assistant")
                    .build();
            assertThat(request.getSystemPrompt()).isEqualTo("You are a helpful assistant");
        }

        @Test
        @DisplayName("额外参数设置正确")
        void extraParams_setCorrectly() {
            Map<String, Object> extraParams = Map.of("key1", "value1", "key2", 123);
            LLMRequest request = LLMRequest.builder()
                    .extraParams(extraParams)
                    .build();
            assertThat(request.getExtraParams()).isEqualTo(extraParams);
        }

        @Test
        @DisplayName("超时时间设置正确")
        void timeoutSeconds_setCorrectly() {
            LLMRequest request = LLMRequest.builder()
                    .timeoutSeconds(30)
                    .build();
            assertThat(request.getTimeoutSeconds()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("嵌套类测试")
    class NestedClassTests {

        @Test
        @DisplayName("Message 嵌套类正常工作")
        void message_nestedClassWorks() {
            LLMRequest.Message message = LLMRequest.Message.builder()
                    .role("assistant")
                    .content("How can I help?")
                    .name("assistant_name")
                    .build();

            assertThat(message.getRole()).isEqualTo("assistant");
            assertThat(message.getContent()).isEqualTo("How can I help?");
            assertThat(message.getName()).isEqualTo("assistant_name");
        }

        @Test
        @DisplayName("ToolCall 嵌套类正常工作")
        void toolCall_nestedClassWorks() {
            LLMRequest.ToolCall toolCall = LLMRequest.ToolCall.builder()
                    .id("call_123")
                    .type("function")
                    .function(LLMRequest.FunctionCall.builder()
                            .name("get_weather")
                            .arguments("{\"city\":\"Beijing\"}")
                            .build())
                    .build();

            assertThat(toolCall.getId()).isEqualTo("call_123");
            assertThat(toolCall.getFunction().getName()).isEqualTo("get_weather");
        }

        @Test
        @DisplayName("Function 嵌套类正常工作")
        void function_nestedClassWorks() {
            LLMRequest.Function function = LLMRequest.Function.builder()
                    .name("get_weather")
                    .description("Get weather for a city")
                    .parameters("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}")
                    .build();

            assertThat(function.getName()).isEqualTo("get_weather");
            assertThat(function.getDescription()).isEqualTo("Get weather for a city");
        }
    }
}
