/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.integration;

import com.codingas.gateway.application.protocol.conversion.ProtocolConversionFacade;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.protocol.anthropic.AnthropicProtocolAdapter;
import com.codingas.gateway.protocol.openai.OpenAIProtocolAdapter;
import com.codingas.gateway.infrastructure.protocol.ProtocolStreamConverter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨协议转换集成测试 — 验证 {@link ProtocolConversionFacade} 的 OpenAI ↔ Anthropic 双向转换逻辑。
 * <p>
 * 直接构造真实的 {@link ProtocolConversionFacade}（生产 {@code @Component}，内部使用两个真实
 * ProtocolAdapter 走 Canonical IR 两跳 normalize→denormalize），覆盖请求与响应四个方向的转换语义：
 * <ul>
 *     <li>OpenAI 请求 → Anthropic 请求：system 角色提取到顶层、tools/tool_choice 格式转换</li>
 *     <li>Anthropic 请求 → OpenAI 请求：顶层 system 合并为首条消息、content 拼接、tools/tool_choice 反向转换</li>
 *     <li>Anthropic 响应 → OpenAI 响应：content blocks 拆分为 content + tool_calls、stop_reason 映射、补 total_tokens</li>
 *     <li>OpenAI 响应 → Anthropic 响应：choices 转 content blocks、finish_reason 映射、usage 字段名映射</li>
 * </ul>
 * <p>
 * ObjectMapper 配置与 {@code ProviderSimulator} 保持一致，确保转换行为贴近生产环境。
 * 说明：max_tokens 缺省补全已从转换器下沉到出站调谐（AnthropicTuner），转换层不再补 1024；
 * stream 在规范 IR 中为布尔值，转换后为 false（非 null）。
 */
class ProtocolConversionIntegrationTest {

    private ProtocolConversionFacade facade;

    @BeforeEach
    void setUp() {
        // 与 ProviderSimulator 保持一致的 ObjectMapper 配置，确保转换行为贴近生产
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        facade = new ProtocolConversionFacade(
                List.of(
                    new OpenAIProtocolAdapter(objectMapper),
                    new AnthropicProtocolAdapter()),
                new ProtocolStreamConverter(objectMapper));
    }

    // ==================== 请求转换 ====================

    @Nested
    @DisplayName("请求转换")
    class RequestConversion {

        @Test
        @DisplayName("OpenAIChatRequest → AnthropicMessagesRequest：system 提取、tools/tool_choice 转换")
        void testConvert_openaiRequestToAnthropic() {
            // 构造 OpenAI 请求：含 system 消息、tools、tool_choice
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(
                            OpenAIChatRequest.Message.builder()
                                    .role("system").content("You are a helpful assistant.").build(),
                            OpenAIChatRequest.Message.builder()
                                    .role("user").content("Hello").build()
                    ))
                    .temperature(0.7)
                    .stop(List.of("END"))
                    .tools(List.of(Map.of(
                            "type", "function",
                            "function", Map.of(
                                    "name", "get_weather",
                                    "description", "Get weather",
                                    "parameters", Map.of("type", "object", "properties", Map.of())
                            )
                    )))
                    .toolChoice("auto")
                    .stream(false)
                    .build();

            AnthropicMessagesRequest result = (AnthropicMessagesRequest) facade.convertRequest(request, "anthropic");

            // 基本字段透传
            assertThat(result.getModel()).isEqualTo("gpt-4o");
            assertThat(result.getTemperature()).isEqualTo(0.7);
            // system 角色消息应被提取到顶层 system 字段
            assertThat(result.getSystem()).isEqualTo("You are a helpful assistant.");
            // 非 system 消息保留
            assertThat(result.getMessages()).hasSize(1);
            assertThat(result.getMessages().get(0).getRole()).isEqualTo("user");
            assertThat(result.getMessages().get(0).getContent()).isEqualTo("Hello");
            // max_tokens 缺省补全已下沉出站调谐（AnthropicTuner），转换层原样透传（null）
            assertThat(result.getMaxTokens()).isNull();
            // stop → stop_sequences
            assertThat(result.getStopSequences()).containsExactly("END");
            // tools 格式转换：function.{name,description,parameters} → {name,description,input_schema}
            assertThat(result.getTools()).hasSize(1);
            Map<String, Object> anthropicTool = result.getTools().get(0);
            assertThat(anthropicTool).containsEntry("name", "get_weather");
            assertThat(anthropicTool).containsEntry("description", "Get weather");
            assertThat(anthropicTool).containsKey("input_schema");
            // tool_choice: String "auto" → Map{type:"auto"}
            assertThat(result.getToolChoice()).containsEntry("type", "auto");
            // stream 规范 IR 为布尔值，转换后为 false
            assertThat(result.getStream()).isFalse();
        }

        @Test
        @DisplayName("AnthropicMessagesRequest → OpenAIChatRequest：system 合并、tools/tool_choice 反向转换")
        void testConvert_anthropicRequestToOpenAI() {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                    .model("claude-sonnet-4")
                    .system("Be concise.")
                    .messages(List.of(
                            AnthropicMessagesRequest.Message.builder()
                                    .role("user").content("Hi").build()
                    ))
                    .maxTokens(512)
                    .temperature(0.5)
                    .stopSequences(List.of("STOP"))
                    .tools(List.of(Map.of(
                            "name", "calc",
                            "description", "Calculator",
                            "input_schema", Map.of("type", "object")
                    )))
                    .toolChoice(Map.of("type", "auto"))
                    .stream(false)
                    .build();

            OpenAIChatRequest result = (OpenAIChatRequest) facade.convertRequest(request, "openai");

            assertThat(result.getModel()).isEqualTo("claude-sonnet-4");
            assertThat(result.getMaxTokens()).isEqualTo(512);
            assertThat(result.getTemperature()).isEqualTo(0.5);
            // system 应合并为首条 system 角色消息
            assertThat(result.getMessages()).hasSize(2);
            assertThat(result.getMessages().get(0).getRole()).isEqualTo("system");
            assertThat(result.getMessages().get(0).getContent()).isEqualTo("Be concise.");
            assertThat(result.getMessages().get(1).getRole()).isEqualTo("user");
            assertThat(result.getMessages().get(1).getContent()).isEqualTo("Hi");
            // stop_sequences → stop
            assertThat(result.getStop()).containsExactly("STOP");
            // tools 反向转换：{name,description,input_schema} → function.{name,description,parameters}
            assertThat(result.getTools()).hasSize(1);
            Map<String, Object> openaiTool = result.getTools().get(0);
            assertThat(openaiTool).containsEntry("type", "function");
            @SuppressWarnings("unchecked")
            Map<String, Object> function = (Map<String, Object>) openaiTool.get("function");
            assertThat(function).containsEntry("name", "calc");
            assertThat(function).containsEntry("description", "Calculator");
            assertThat(function).containsKey("parameters");
            // tool_choice: Map{type:"auto"} → String "auto"
            assertThat(result.getToolChoice()).isEqualTo("auto");
            // stream 规范 IR 为布尔值，转换后为 false
            assertThat(result.getStream()).isFalse();
        }
    }

    // ==================== 响应转换 ====================

    @Nested
    @DisplayName("响应转换")
    class ResponseConversion {

        @Test
        @DisplayName("AnthropicMessagesResponse → OpenAIChatResponse：content blocks 拆分、stop_reason 映射、total_tokens 补全")
        void testConvert_anthropicResponseToOpenAI() {
            AnthropicMessagesResponse response = AnthropicMessagesResponse.builder()
                    .id("msg_001")
                    .model("claude-sonnet-4")
                    .content(List.of(
                            AnthropicMessagesResponse.ContentBlock.builder()
                                    .type("text").text("Hello!").build(),
                            AnthropicMessagesResponse.ContentBlock.builder()
                                    .type("tool_use")
                                    .toolUse(AnthropicMessagesResponse.ToolUse.builder()
                                            .id("tool_1")
                                            .name("get_weather")
                                            .input(Map.of("city", "SF"))
                                            .build())
                                    .build()
                    ))
                    .stopReason("end_turn")
                    .usage(AnthropicMessagesResponse.Usage.builder()
                            .inputTokens(10)
                            .outputTokens(8)
                            .build())
                    .build();

            OpenAIChatResponse result = (OpenAIChatResponse) facade.convertResponse(response, "anthropic");

            assertThat(result.getId()).isEqualTo("msg_001");
            assertThat(result.getModel()).isEqualTo("claude-sonnet-4");
            // choices[0].message：text 拼接为 content，tool_use 转为 tool_calls
            assertThat(result.getChoices()).hasSize(1);
            OpenAIChatResponse.Choice choice = result.getChoices().get(0);
            assertThat(choice.getMessage().getRole()).isEqualTo("assistant");
            assertThat(choice.getMessage().getContent()).isEqualTo("Hello!");
            assertThat(choice.getMessage().getToolCalls()).hasSize(1);
            OpenAIChatResponse.ToolCall toolCall = choice.getMessage().getToolCalls().get(0);
            assertThat(toolCall.getId()).isEqualTo("tool_1");
            assertThat(toolCall.getType()).isEqualTo("function");
            assertThat(toolCall.getFunction().getName()).isEqualTo("get_weather");
            // tool_use.input (Map) → function.arguments（JSON 字符串，含 city）
            assertThat(toolCall.getFunction().getArguments()).contains("city");
            // stop_reason "end_turn" → finish_reason "stop"
            assertThat(choice.getFinishReason()).isEqualTo("stop");
            // usage 映射：input_tokens→prompt_tokens, output_tokens→completion_tokens, total=输入+输出
            assertThat(result.getUsage().getPromptTokens()).isEqualTo(10);
            assertThat(result.getUsage().getCompletionTokens()).isEqualTo(8);
            assertThat(result.getUsage().getTotalTokens()).isEqualTo(18);
        }

        @Test
        @DisplayName("OpenAIChatResponse → AnthropicMessagesResponse：choices 转 content blocks、finish_reason 映射")
        void testConvert_openaiResponseToAnthropic() {
            OpenAIChatResponse response = OpenAIChatResponse.builder()
                    .id("chatcmpl-001")
                    .model("gpt-4o")
                    .choices(List.of(
                            OpenAIChatResponse.Choice.builder()
                                    .index(0)
                                    .message(OpenAIChatResponse.Message.builder()
                                            .role("assistant")
                                            .content("Hi there")
                                            .toolCalls(List.of(
                                                    OpenAIChatResponse.ToolCall.builder()
                                                            .id("call_1")
                                                            .type("function")
                                                            .function(OpenAIChatResponse.FunctionCall.builder()
                                                                    .name("search")
                                                                    .arguments("{\"q\":\"test\"}")
                                                                    .build())
                                                            .build()
                                            ))
                                            .build())
                                    .finishReason("tool_calls")
                                    .build()
                    ))
                    .usage(OpenAIChatResponse.Usage.builder()
                            .promptTokens(5)
                            .completionTokens(3)
                            .totalTokens(8)
                            .build())
                    .build();

            AnthropicMessagesResponse result = (AnthropicMessagesResponse) facade.convertResponse(response, "openai");

            assertThat(result.getId()).isEqualTo("chatcmpl-001");
            assertThat(result.getModel()).isEqualTo("gpt-4o");
            // type/role 固定
            assertThat(result.getType()).isEqualTo("message");
            assertThat(result.getRole()).isEqualTo("assistant");
            // content blocks：text + tool_use
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getType()).isEqualTo("text");
            assertThat(result.getContent().get(0).getText()).isEqualTo("Hi there");
            assertThat(result.getContent().get(1).getType()).isEqualTo("tool_use");
            AnthropicMessagesResponse.ToolUse toolUse = result.getContent().get(1).getToolUse();
            assertThat(toolUse.getId()).isEqualTo("call_1");
            assertThat(toolUse.getName()).isEqualTo("search");
            // tool_calls.function.arguments (String) → tool_use.input（规范 IR 中为 JsonNode，文本值不变）
            assertThat(toolUse.getInput()).isInstanceOf(JsonNode.class);
            assertThat(((JsonNode) toolUse.getInput()).asText()).isEqualTo("{\"q\":\"test\"}");
            // finish_reason "tool_calls" → stop_reason "tool_use"
            assertThat(result.getStopReason()).isEqualTo("tool_use");
            // usage 映射：prompt_tokens→input_tokens, completion_tokens→output_tokens
            assertThat(result.getUsage().getInputTokens()).isEqualTo(5);
            assertThat(result.getUsage().getOutputTokens()).isEqualTo(3);
        }
    }
}
