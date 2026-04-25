package com.codingas.gateway.dispatch;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProtocolTranslator 单元测试
 */
class ProtocolTranslatorTest {

    private ProtocolTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new ProtocolTranslator();
    }

    @Nested
    @DisplayName("OpenAI → Anthropic 转换")
    class ToAnthropicFormat {

        @Test
        @DisplayName("基本消息转换")
        void basicMessageConversion() {
            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello, world!")
                                    .build()
                    ))
                    .maxTokens(100)
                    .temperature(0.7)
                    .build();

            Map<String, Object> anthropicRequest = translator.toAnthropicFormat(request);

            assertThat(anthropicRequest.get("model")).isEqualTo("gpt-4");
            assertThat(anthropicRequest.get("max_tokens")).isEqualTo(100);
            assertThat(anthropicRequest.get("temperature")).isEqualTo(0.7);
            assertThat(anthropicRequest.get("messages")).isInstanceOf(List.class);
        }

        @Test
        @DisplayName("系统提示转换")
        void systemPromptConversion() {
            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .messages(List.of())
                    .systemPrompt("You are a helpful assistant.")
                    .build();

            Map<String, Object> anthropicRequest = translator.toAnthropicFormat(request);

            assertThat(anthropicRequest.get("system")).isEqualTo("You are a helpful assistant.");
        }

        @Test
        @DisplayName("工具定义转换")
        void toolsConversion() {
            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .messages(List.of())
                    .tools(List.of(
                            LLMRequest.ToolDefinition.builder()
                                    .type("function")
                                    .function(LLMRequest.Function.builder()
                                            .name("get_weather")
                                            .description("Get weather for a location")
                                            .parameters("{\"type\":\"object\",\"properties\":{\"location\":{\"type\":\"string\"}}}")
                                            .build())
                                    .build()
                    ))
                    .build();

            Map<String, Object> anthropicRequest = translator.toAnthropicFormat(request);

            assertThat(anthropicRequest.get("tools")).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) anthropicRequest.get("tools");
            assertThat(tools).hasSize(1);
            assertThat(tools.get(0).get("name")).isEqualTo("get_weather");
        }
    }

    @Nested
    @DisplayName("Anthropic → OpenAI 转换")
    class FromAnthropicFormat {

        @Test
        @DisplayName("Anthropic 响应转 OpenAI 格式")
        void anthropicResponseToOpenAI() {
            Map<String, Object> anthropicResponse = Map.of(
                    "id", "msg_123",
                    "model", "claude-3-sonnet",
                    "content", List.of(
                            Map.of("type", "text", "text", "Hello!"),
                            Map.of("type", "tool_use", "id", "tool_1", "name", "get_weather",
                                    "input", Map.of("location", "Beijing"))
                    ),
                    "usage", Map.of("input_tokens", 100, "output_tokens", 50),
                    "stop_reason", "end_turn"
            );

            LLMResponse response = translator.fromAnthropicResponse(anthropicResponse);

            assertThat(response.getProviderCode()).isEqualTo("anthropic");
            assertThat(response.getId()).isEqualTo("msg_123");
            assertThat(response.getModel()).isEqualTo("claude-3-sonnet");
            assertThat(response.getContent().getText()).isEqualTo("Hello!");
            assertThat(response.getContent().getToolCalls()).hasSize(1);
            assertThat(response.getContent().getToolCalls().get(0).getFunction().getName()).isEqualTo("get_weather");
            assertThat(response.getUsage().getPromptTokens()).isEqualTo(100);
            assertThat(response.getUsage().getCompletionTokens()).isEqualTo(50);
        }

        @Test
        @DisplayName("Anthropic 请求转 OpenAI 格式")
        void anthropicRequestToOpenAI() {
            Map<String, Object> anthropicRequest = Map.of(
                    "model", "claude-3-sonnet",
                    "messages", List.of(
                            Map.of("role", "user", "content", "Hello!")
                    ),
                    "system", "You are helpful.",
                    "max_tokens", 1024,
                    "temperature", 0.5
            );

            LLMRequest request = translator.fromAnthropicRequest(anthropicRequest);

            assertThat(request.getModel()).isEqualTo("claude-3-sonnet");
            assertThat(request.getMessages()).hasSize(1);
            assertThat(request.getSystemPrompt()).isEqualTo("You are helpful.");
            assertThat(request.getMaxTokens()).isEqualTo(1024);
            assertThat(request.getTemperature()).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("OpenAI → Anthropic 响应转换")
    class ToAnthropicResponse {

        @Test
        @DisplayName("基本响应转换")
        void basicResponseConversion() {
            LLMResponse openAIResponse = LLMResponse.builder()
                    .id("chatcmpl_123")
                    .model("gpt-4")
                    .content(LLMResponse.Content.builder()
                            .text("Hello!")
                            .role("assistant")
                            .build())
                    .usage(LLMResponse.Usage.builder()
                            .promptTokens(100)
                            .completionTokens(50)
                            .totalTokens(150)
                            .build())
                    .finishReason("stop")
                    .build();

            Map<String, Object> anthropicResponse = translator.toAnthropicResponse(openAIResponse);

            assertThat(anthropicResponse.get("id")).isEqualTo("chatcmpl_123");
            assertThat(anthropicResponse.get("model")).isEqualTo("gpt-4");
            assertThat(anthropicResponse.get("type")).isEqualTo("message");
            assertThat(anthropicResponse.get("role")).isEqualTo("assistant");
            assertThat(anthropicResponse.get("stop_reason")).isEqualTo("stop");
        }

        @Test
        @DisplayName("带工具调用的响应转换")
        void toolCallResponseConversion() {
            LLMResponse openAIResponse = LLMResponse.builder()
                    .id("chatcmpl_123")
                    .model("gpt-4")
                    .content(LLMResponse.Content.builder()
                            .text("")
                            .role("assistant")
                            .toolCalls(List.of(
                                    LLMResponse.ToolCall.builder()
                                            .id("call_123")
                                            .type("function")
                                            .function(LLMResponse.FunctionCall.builder()
                                                    .name("get_weather")
                                                    .arguments("{\"location\":\"Beijing\"}")
                                                    .build())
                                            .build()
                            ))
                            .build())
                    .finishReason("tool_calls")
                    .build();

            Map<String, Object> anthropicResponse = translator.toAnthropicResponse(openAIResponse);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) anthropicResponse.get("content");
            assertThat(content).hasSize(1);
            assertThat(content.get(0).get("type")).isEqualTo("tool_use");
            assertThat(content.get(0).get("name")).isEqualTo("get_weather");
        }
    }
}
