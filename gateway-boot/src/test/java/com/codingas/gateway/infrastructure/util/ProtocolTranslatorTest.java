package com.codingas.gateway.infrastructure.util;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProtocolTranslator 单元测试
 *
 * <p>测试 OpenAI 和 Anthropic 格式之间的双向转换。</p>
 */
@DisplayName("ProtocolTranslator")
class ProtocolTranslatorTest {

    private ProtocolTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new ProtocolTranslator();
    }

    @Nested
    @DisplayName("toAnthropicFormat")
    class ToAnthropicFormat {

        @Test
        @DisplayName("基本字段转换正确")
        void basicFieldsConvertedCorrectly() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello")
                                    .build()
                    ))
                    .maxTokens(1024)
                    .temperature(0.7)
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicFormat(request);

            // then
            assertThat(result.get("model")).isEqualTo("claude-sonnet-4-20250514");
            assertThat(result.get("max_tokens")).isEqualTo(1024);
            assertThat(result.get("temperature")).isEqualTo(0.7);
        }

        @Test
        @DisplayName("system prompt 转换为 Anthropic 格式")
        void systemPromptConvertedToAnthropic() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .systemPrompt("You are a helpful assistant")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hi")
                                    .build()
                    ))
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicFormat(request);

            // then
            assertThat(result.get("system")).isEqualTo("You are a helpful assistant");
        }

        @Test
        @DisplayName("maxTokens 为 null 时默认为 1024")
        void maxTokensDefaultsTo1024WhenNull() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hi")
                                    .build()
                    ))
                    .maxTokens(null)
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicFormat(request);

            // then
            assertThat(result.get("max_tokens")).isEqualTo(1024);
        }

        @Test
        @DisplayName("temperature 为 null 时不包含在输出中")
        void temperatureNotIncludedWhenNull() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hi")
                                    .build()
                    ))
                    .temperature(null)
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicFormat(request);

            // then
            assertThat(result).doesNotContainKey("temperature");
        }

        @Test
        @DisplayName("消息角色转换 - system 转为 user")
        void systemRoleConvertedToUser() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("system")
                                    .content("You are a helpful assistant")
                                    .build(),
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello")
                                    .build()
                    ))
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicFormat(request);
            List<Map<String, Object>> messages = (List<Map<String, Object>>) result.get("messages");

            // then
            assertThat(messages).hasSize(2);
            assertThat(messages.get(0).get("role")).isEqualTo("user");
            assertThat(messages.get(1).get("role")).isEqualTo("user");
        }

        @Test
        @DisplayName("消息角色转换 - assistant 保持不变")
        void assistantRoleRemainsAssistant() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello")
                                    .build(),
                            LLMRequest.Message.builder()
                                    .role("assistant")
                                    .content("Hi there!")
                                    .build()
                    ))
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicFormat(request);
            List<Map<String, Object>> messages = (List<Map<String, Object>>) result.get("messages");

            // then
            assertThat(messages.get(1).get("role")).isEqualTo("assistant");
        }

        @Test
        @DisplayName("工具定义转换正确")
        void toolsConvertedCorrectly() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("What's the weather?")
                                    .build()
                    ))
                    .tools(List.of(
                            LLMRequest.ToolDefinition.builder()
                                    .type("function")
                                    .function(LLMRequest.Function.builder()
                                            .name("get_weather")
                                            .description("Get weather for a city")
                                            .parameters("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}")
                                            .build())
                                    .build()
                    ))
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicFormat(request);

            // then
            assertThat(result).containsKey("tools");
            List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
            assertThat(tools).hasSize(1);
            assertThat(tools.get(0).get("name")).isEqualTo("get_weather");
            assertThat(tools.get(0).get("description")).isEqualTo("Get weather for a city");
            assertThat(tools.get(0)).containsKey("input_schema");
        }

        @Test
        @DisplayName("工具调用转换正确")
        void toolCallsConvertedCorrectly() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Use the weather tool")
                                    .build(),
                            LLMRequest.Message.builder()
                                    .role("assistant")
                                    .content("")
                                    .toolCalls(List.of(
                                            LLMRequest.ToolCall.builder()
                                                    .id("tool_call_1")
                                                    .type("function")
                                                    .function(LLMRequest.FunctionCall.builder()
                                                            .name("get_weather")
                                                            .arguments("{\"city\":\"Beijing\"}")
                                                            .build())
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicFormat(request);
            List<Map<String, Object>> messages = (List<Map<String, Object>>) result.get("messages");

            // then
            Map<String, Object> assistantMsg = messages.get(1);
            List<Map<String, Object>> content = (List<Map<String, Object>>) assistantMsg.get("content");
            assertThat(content).hasSize(1);
            assertThat(content.get(0).get("type")).isEqualTo("tool_use");
            assertThat(content.get(0).get("id")).isEqualTo("tool_call_1");
            assertThat(content.get(0).get("name")).isEqualTo("get_weather");
        }
    }

    @Nested
    @DisplayName("fromAnthropicResponse")
    class FromAnthropicResponse {

        @Test
        @DisplayName("基本响应解析正确")
        void basicResponseParsedCorrectly() {
            // given
            Map<String, Object> anthropicResponse = Map.of(
                    "id", "msg_abc123",
                    "model", "claude-sonnet-4-20250514",
                    "content", List.of(
                            Map.of("type", "text", "text", "Hello, how can I help you?")
                    ),
                    "stop_reason", "end_turn",
                    "usage", Map.of(
                            "input_tokens", 100,
                            "output_tokens", 50
                    )
            );

            // when
            LLMResponse result = translator.fromAnthropicResponse(anthropicResponse);

            // then
            assertThat(result.getId()).isEqualTo("msg_abc123");
            assertThat(result.getModel()).isEqualTo("claude-sonnet-4-20250514");
            assertThat(result.getProvider()).isEqualTo("anthropic");
            assertThat(result.getFinishReason()).isEqualTo("end_turn");
            assertThat(result.getContent().getText()).isEqualTo("Hello, how can I help you?");
            assertThat(result.getUsage().getPromptTokens()).isEqualTo(100);
            assertThat(result.getUsage().getCompletionTokens()).isEqualTo(50);
            assertThat(result.isStream()).isFalse();
        }

        @Test
        @DisplayName("工具调用响应解析正确")
        void toolUseResponseParsedCorrectly() {
            // given
            Map<String, Object> anthropicResponse = Map.of(
                    "id", "msg_abc123",
                    "model", "claude-sonnet-4-20250514",
                    "content", List.of(
                            Map.of(
                                    "type", "tool_use",
                                    "id", "toolu_abc123",
                                    "name", "get_weather",
                                    "input", Map.of("city", "Beijing")
                            )
                    ),
                    "stop_reason", "tool_use"
            );

            // when
            LLMResponse result = translator.fromAnthropicResponse(anthropicResponse);

            // then
            assertThat(result.getContent().getToolCalls()).hasSize(1);
            LLMResponse.ToolCall toolCall = result.getContent().getToolCalls().get(0);
            assertThat(toolCall.getId()).isEqualTo("toolu_abc123");
            assertThat(toolCall.getFunction().getName()).isEqualTo("get_weather");
            assertThat(toolCall.getFunction().getArguments()).isEqualTo("{\"city\":\"Beijing\"}");
        }

        @Test
        @DisplayName("空 content 返回 null")
        void emptyContentReturnsNull() {
            // given
            Map<String, Object> anthropicResponse = Map.of(
                    "id", "msg_abc123",
                    "model", "claude-sonnet-4-20250514",
                    "content", List.of(),
                    "stop_reason", "end_turn"
            );

            // when
            LLMResponse result = translator.fromAnthropicResponse(anthropicResponse);

            // then
            assertThat(result.getContent()).isNull();
        }

        @Test
        @DisplayName("缺少 usage 时返回 null usage")
        void missingUsageReturnsNullUsage() {
            // given
            Map<String, Object> anthropicResponse = Map.of(
                    "id", "msg_abc123",
                    "model", "claude-sonnet-4-20250514",
                    "content", List.of(
                            Map.of("type", "text", "text", "Hello")
                    ),
                    "stop_reason", "end_turn"
            );

            // when
            LLMResponse result = translator.fromAnthropicResponse(anthropicResponse);

            // then
            assertThat(result.getUsage()).isNull();
        }
    }

    @Nested
    @DisplayName("fromAnthropicRequest")
    class FromAnthropicRequest {

        @Test
        @DisplayName("基本请求转换正确")
        void basicRequestConvertedCorrectly() {
            // given
            Map<String, Object> anthropicRequest = Map.of(
                    "model", "claude-sonnet-4-20250514",
                    "messages", List.of(
                            Map.of("role", "user", "content", "Hello")
                    ),
                    "max_tokens", 1024,
                    "temperature", 0.7
            );

            // when
            LLMRequest result = translator.fromAnthropicRequest(anthropicRequest);

            // then
            assertThat(result.getModel()).isEqualTo("claude-sonnet-4-20250514");
            assertThat(result.getMaxTokens()).isEqualTo(1024);
            assertThat(result.getTemperature()).isEqualTo(0.7);
            assertThat(result.getMessages()).hasSize(1);
            assertThat(result.getMessages().get(0).getRole()).isEqualTo("user");
            assertThat(result.getMessages().get(0).getContent()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("system 消息转换正确")
        void systemMessageConvertedCorrectly() {
            // given
            Map<String, Object> anthropicRequest = Map.of(
                    "model", "claude-sonnet-4-20250514",
                    "system", "You are a helpful assistant",
                    "messages", List.of(
                            Map.of("role", "user", "content", "Hello")
                    )
            );

            // when
            LLMRequest result = translator.fromAnthropicRequest(anthropicRequest);

            // then
            assertThat(result.getSystemPrompt()).isEqualTo("You are a helpful assistant");
        }

        @Test
        @DisplayName("user 角色保持不变")
        void userRoleRemainsUser() {
            // given
            Map<String, Object> anthropicRequest = Map.of(
                    "model", "claude-sonnet-4-20250514",
                    "messages", List.of(
                            Map.of("role", "user", "content", "Hello")
                    )
            );

            // when
            LLMRequest result = translator.fromAnthropicRequest(anthropicRequest);

            // then
            assertThat(result.getMessages().get(0).getRole()).isEqualTo("user");
        }

        @Test
        @DisplayName("assistant 角色保持不变")
        void assistantRoleRemainsAssistant() {
            // given
            Map<String, Object> anthropicRequest = Map.of(
                    "model", "claude-sonnet-4-20250514",
                    "messages", List.of(
                            Map.of("role", "user", "content", "Hello"),
                            Map.of("role", "assistant", "content", "Hi there!")
                    )
            );

            // when
            LLMRequest result = translator.fromAnthropicRequest(anthropicRequest);

            // then
            assertThat(result.getMessages().get(1).getRole()).isEqualTo("assistant");
        }

        @Test
        @DisplayName("工具定义转换正确")
        void toolsConvertedCorrectly() {
            // given
            // 实际实现：tool.get("name") 获取名称，tool.get("function") 获取函数定义
            Map<String, Object> function = Map.of(
                    "description", "Get weather for a city",
                    "parameters", "{\"type\":\"object\"}"
            );
            Map<String, Object> tool = Map.of(
                    "name", "get_weather",
                    "function", function
            );
            Map<String, Object> anthropicRequest = Map.of(
                    "model", "claude-sonnet-4-20250514",
                    "messages", List.of(
                            Map.of("role", "user", "content", "Use the tool")
                    ),
                    "tools", List.of(tool)
            );

            // when
            LLMRequest result = translator.fromAnthropicRequest(anthropicRequest);

            // then
            assertThat(result.getTools()).hasSize(1);
            LLMRequest.ToolDefinition toolResult = result.getTools().get(0);
            assertThat(toolResult.getType()).isEqualTo("function");
            assertThat(toolResult.getFunction().getName()).isEqualTo("get_weather");
            assertThat(toolResult.getFunction().getDescription()).isEqualTo("Get weather for a city");
        }

        @Test
        @DisplayName("tool_use_id 转换正确")
        void toolUseIdConvertedCorrectly() {
            // given
            Map<String, Object> anthropicRequest = Map.of(
                    "model", "claude-sonnet-4-20250514",
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", "The weather is sunny",
                                    "tool_use_id", "toolu_abc123"
                            )
                    )
            );

            // when
            LLMRequest result = translator.fromAnthropicRequest(anthropicRequest);

            // then
            assertThat(result.getMessages().get(0).getToolCallId()).isEqualTo("toolu_abc123");
        }

        @Test
        @DisplayName("maxTokens 和 temperature 为 null 时正确处理")
        void nullMaxTokensAndTemperatureHandledCorrectly() {
            // given
            Map<String, Object> anthropicRequest = Map.of(
                    "model", "claude-sonnet-4-20250514",
                    "messages", List.of(
                            Map.of("role", "user", "content", "Hello")
                    )
            );

            // when
            LLMRequest result = translator.fromAnthropicRequest(anthropicRequest);

            // then
            assertThat(result.getMaxTokens()).isNull();
            assertThat(result.getTemperature()).isNull();
        }
    }

    @Nested
    @DisplayName("toAnthropicResponse")
    class ToAnthropicResponse {

        @Test
        @DisplayName("基本响应转换正确")
        void basicResponseConvertedCorrectly() {
            // given
            LLMResponse response = LLMResponse.builder()
                    .id("msg_abc123")
                    .model("claude-sonnet-4-20250514")
                    .content(LLMResponse.Content.builder()
                            .text("Hello, how can I help you?")
                            .role("assistant")
                            .build())
                    .usage(LLMResponse.Usage.builder()
                            .promptTokens(100)
                            .completionTokens(50)
                            .build())
                    .finishReason("end_turn")
                    .stream(false)
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicResponse(response);

            // then
            assertThat(result.get("id")).isEqualTo("msg_abc123");
            assertThat(result.get("model")).isEqualTo("claude-sonnet-4-20250514");
            assertThat(result.get("type")).isEqualTo("message");
            assertThat(result.get("role")).isEqualTo("assistant");
            assertThat(result.get("stop_reason")).isEqualTo("end_turn");

            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            assertThat(content).hasSize(1);
            assertThat(content.get(0).get("type")).isEqualTo("text");
            assertThat(content.get(0).get("text")).isEqualTo("Hello, how can I help you?");

            Map<String, Object> usage = (Map<String, Object>) result.get("usage");
            assertThat(usage.get("input_tokens")).isEqualTo(100);
            assertThat(usage.get("output_tokens")).isEqualTo(50);
        }

        @Test
        @DisplayName("工具调用响应转换正确")
        void toolCallsResponseConvertedCorrectly() {
            // given
            LLMResponse response = LLMResponse.builder()
                    .id("msg_abc123")
                    .model("claude-sonnet-4-20250514")
                    .content(LLMResponse.Content.builder()
                            .role("assistant")
                            .toolCalls(List.of(
                                    LLMResponse.ToolCall.builder()
                                            .id("toolu_abc123")
                                            .type("function")
                                            .function(LLMResponse.FunctionCall.builder()
                                                    .name("get_weather")
                                                    .arguments("{\"city\":\"Beijing\"}")
                                                    .build())
                                            .build()
                            ))
                            .build())
                    .finishReason("tool_use")
                    .stream(false)
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicResponse(response);

            // then
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            assertThat(content).hasSize(1);
            assertThat(content.get(0).get("type")).isEqualTo("tool_use");
            assertThat(content.get(0).get("id")).isEqualTo("toolu_abc123");
            assertThat(content.get(0).get("name")).isEqualTo("get_weather");
            assertThat(content.get(0).get("input")).isEqualTo(Map.of("city", "Beijing"));
        }

        @Test
        @DisplayName("空文本内容时 content 为空列表")
        void emptyTextContentResultsInEmptyContentList() {
            // given
            LLMResponse response = LLMResponse.builder()
                    .id("msg_abc123")
                    .model("claude-sonnet-4-20250514")
                    .content(LLMResponse.Content.builder()
                            .text("")
                            .role("assistant")
                            .build())
                    .finishReason("end_turn")
                    .stream(false)
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicResponse(response);

            // then
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            assertThat(content).isEmpty();
        }

        @Test
        @DisplayName("缺少 usage 时不包含 usage 字段")
        void missingUsageNotIncluded() {
            // given
            LLMResponse response = LLMResponse.builder()
                    .id("msg_abc123")
                    .model("claude-sonnet-4-20250514")
                    .content(LLMResponse.Content.builder()
                            .text("Hello")
                            .role("assistant")
                            .build())
                    .finishReason("end_turn")
                    .stream(false)
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicResponse(response);

            // then
            assertThat(result).doesNotContainKey("usage");
        }

        @Test
        @DisplayName("stop_sequence 始终为 null")
        void stopSequenceAlwaysNull() {
            // given
            LLMResponse response = LLMResponse.builder()
                    .id("msg_abc123")
                    .model("claude-sonnet-4-20250514")
                    .content(LLMResponse.Content.builder()
                            .text("Hello")
                            .role("assistant")
                            .build())
                    .finishReason("end_turn")
                    .stream(false)
                    .build();

            // when
            Map<String, Object> result = translator.toAnthropicResponse(response);

            // then
            assertThat(result.get("stop_sequence")).isNull();
        }
    }
}