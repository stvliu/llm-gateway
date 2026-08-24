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
package com.codingas.gateway.protocol.openai;

import com.codingas.gateway.protocol.canonical.CanonicalChatRequest;
import com.codingas.gateway.protocol.canonical.CanonicalChatResponse;
import com.codingas.gateway.protocol.canonical.CanonicalContentBlock;
import com.codingas.gateway.protocol.canonical.CanonicalMessage;
import com.codingas.gateway.protocol.canonical.CanonicalTool;
import com.codingas.gateway.protocol.canonical.CanonicalToolCall;
import com.codingas.gateway.protocol.canonical.CanonicalUsage;
import com.codingas.gateway.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.protocol.contract.OpenAIChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIProtocolAdapterTest {

    private OpenAIProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OpenAIProtocolAdapter(new ObjectMapper());
    }

    @Test
    void protocolReturnsOpenai() {
        assertThat(adapter.protocol()).isEqualTo("openai");
    }

    @Test
    void normalizeRequestExtractsSystemAndTool() {
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("type", "object");
        OpenAIChatRequest.Message sys = OpenAIChatRequest.Message.builder()
                .role("system").content("你是助手").build();
        OpenAIChatRequest.Message user = OpenAIChatRequest.Message.builder()
                .role("user").content("hi").build();
        OpenAIChatRequest req = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(sys, user))
                .tools(List.of(java.util.Map.of("type", "function",
                        "function", java.util.Map.of("name", "f1", "parameters", params))))
                .stream(true)
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getSystem()).isEqualTo("你是助手");
        assertThat(c.getMessages()).hasSize(1);
        assertThat(c.getMessages().get(0).getRole()).isEqualTo("user");
        assertThat(c.getTools()).hasSize(1);
        assertThat(c.getTools().get(0).getName()).isEqualTo("f1");
        assertThat(c.isStream()).isTrue();
    }

    @Test
    void denormalizeRequestRoundTripsMessages() {
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("gpt-4o")
                .system("sys")
                .messages(List.of(CanonicalMessage.builder().role("user").content("hi").build()))
                .build();

        OpenAIChatRequest nativeReq = adapter.denormalizeRequest(c);

        assertThat(nativeReq.getModel()).isEqualTo("gpt-4o");
        assertThat(nativeReq.getMessages()).hasSize(2); // system 角色 + user
        assertThat(nativeReq.getMessages().get(0).getRole()).isEqualTo("system");
    }

    @Test
    void denormalizeRequestToleratesToolWithoutParameters() {
        // 工具无入参 schema（parameters=null）时 denormalizeRequest 不得抛 NPE
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("gpt-4o")
                .tools(List.of(CanonicalTool.builder()
                        .name("f1")
                        .description(null)
                        .parameters(null)
                        .build()))
                .build();

        OpenAIChatRequest nativeReq = adapter.denormalizeRequest(c);

        assertThat(nativeReq.getTools()).hasSize(1);
        Map<String, Object> tool = nativeReq.getTools().get(0);
        assertThat(tool.get("type")).isEqualTo("function");
        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) tool.get("function");
        assertThat(function.get("name")).isEqualTo("f1");
        assertThat(function.containsKey("description")).isTrue(); // null 值保留，由序列化层省略
        assertThat(function.get("parameters")).isNull();
    }

    @Test
    void normalizeRequest_nullMessages_returnsEmptyMessages() {
        OpenAIChatRequest req = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(null)
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getMessages()).isEmpty();
        assertThat(c.getSystem()).isNull();
    }

    @Test
    void normalizeRequest_convertsMessageToolCalls() {
        // 携带 tool_calls 的 assistant 消息 → 规范消息 toolCalls 保留
        OpenAIChatRequest.ToolCall tc = OpenAIChatRequest.ToolCall.builder()
                .id("call_1")
                .type("function")
                .function(OpenAIChatRequest.FunctionCall.builder()
                        .name("weather")
                        .arguments("{\"city\":\"beijing\"}")
                        .build())
                .build();
        OpenAIChatRequest.Message assistant = OpenAIChatRequest.Message.builder()
                .role("assistant")
                .content("查询中")
                .toolCalls(List.of(tc))
                .build();
        OpenAIChatRequest req = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(assistant))
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getMessages()).hasSize(1);
        assertThat(c.getMessages().get(0).getToolCalls()).hasSize(1);
        CanonicalToolCall converted = c.getMessages().get(0).getToolCalls().get(0);
        assertThat(converted.getId()).isEqualTo("call_1");
        assertThat(converted.getName()).isEqualTo("weather");
        assertThat(converted.getArguments().asText()).contains("beijing");
    }

    @Test
    void normalizeRequest_toolCallWithoutFunction_tolerated() {
        // toolCall 无 function（null）时转换不抛 NPE
        OpenAIChatRequest.Message assistant = OpenAIChatRequest.Message.builder()
                .role("assistant")
                .content("x")
                .toolCalls(List.of(OpenAIChatRequest.ToolCall.builder()
                        .id("call_2")
                        .type("function")
                        .function(null)
                        .build()))
                .build();
        OpenAIChatRequest req = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(assistant))
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getMessages().get(0).getToolCalls().get(0).getName()).isNull();
        assertThat(c.getMessages().get(0).getToolCalls().get(0).getArguments()).isNull();
    }

    @Test
    void denormalizeRequest_convertsCanonicalToolCalls() {
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(CanonicalMessage.builder()
                        .role("assistant")
                        .content("查询中")
                        .toolCalls(List.of(CanonicalToolCall.builder()
                                .id("call_1")
                                .name("weather")
                                .arguments(new ObjectMapper().createObjectNode().put("city", "beijing"))
                                .build()))
                        .build()))
                .build();

        OpenAIChatRequest nativeReq = adapter.denormalizeRequest(c);

        OpenAIChatRequest.ToolCall tc = nativeReq.getMessages().get(0).getToolCalls().get(0);
        assertThat(tc.getId()).isEqualTo("call_1");
        assertThat(tc.getType()).isEqualTo("function");
        assertThat(tc.getFunction().getName()).isEqualTo("weather");
        // 规范 JsonNode arguments 直接透传为 Object
        assertThat(tc.getFunction().getArguments()).isInstanceOf(JsonNode.class);
        JsonNode args = (JsonNode) tc.getFunction().getArguments();
        assertThat(args.get("city").asText()).isEqualTo("beijing");
    }

    @Test
    void normalizeResponse_mapsTextToolUseAndUsage() {
        ObjectNode args = new ObjectMapper().createObjectNode();
        args.put("city", "beijing");
        OpenAIChatResponse resp = OpenAIChatResponse.builder()
                .id("chatcmpl-1")
                .model("gpt-4o")
                .choices(List.of(OpenAIChatResponse.Choice.builder()
                        .index(0)
                        .message(OpenAIChatResponse.Message.builder()
                                .role("assistant")
                                .content("正在查询")
                                .toolCalls(List.of(OpenAIChatResponse.ToolCall.builder()
                                        .id("call_1")
                                        .type("function")
                                        .function(OpenAIChatResponse.FunctionCall.builder()
                                                .name("weather")
                                                .arguments("{\"city\":\"beijing\"}")
                                                .build())
                                        .build()))
                                .build())
                        .finishReason("stop")
                        .build()))
                .usage(OpenAIChatResponse.Usage.builder()
                        .promptTokens(10).completionTokens(8).totalTokens(18).build())
                .build();

        CanonicalChatResponse canonical = adapter.normalizeResponse(resp);

        assertThat(canonical.getId()).isEqualTo("chatcmpl-1");
        assertThat(canonical.getContent()).hasSize(2);
        assertThat(canonical.getContent().get(0).getType()).isEqualTo("text");
        assertThat(canonical.getContent().get(0).getText()).isEqualTo("正在查询");
        CanonicalContentBlock toolBlock = canonical.getContent().get(1);
        assertThat(toolBlock.getType()).isEqualTo("toolUse");
        assertThat(toolBlock.getToolUse().getId()).isEqualTo("call_1");
        assertThat(toolBlock.getToolUse().getName()).isEqualTo("weather");
        assertThat(toolBlock.getToolUse().getArguments().asText()).contains("beijing");
        assertThat(canonical.getStopReason()).isEqualTo("end_turn");
        assertThat(canonical.getUsage().getInputTokens()).isEqualTo(10);
        assertThat(canonical.getUsage().getOutputTokens()).isEqualTo(8);
    }

    @Test
    void normalizeResponse_mapsLengthAndCustomFinishReasons() {
        // length → max_tokens；未知 finish_reason 原样透传
        OpenAIChatResponse lengthResp = OpenAIChatResponse.builder()
                .id("r1").model("gpt-4o")
                .choices(List.of(OpenAIChatResponse.Choice.builder()
                        .index(0)
                        .message(OpenAIChatResponse.Message.builder().role("assistant").content("c").build())
                        .finishReason("length")
                        .build()))
                .build();
        assertThat(adapter.normalizeResponse(lengthResp).getStopReason()).isEqualTo("max_tokens");

        OpenAIChatResponse customResp = OpenAIChatResponse.builder()
                .id("r2").model("gpt-4o")
                .choices(List.of(OpenAIChatResponse.Choice.builder()
                        .index(0)
                        .message(OpenAIChatResponse.Message.builder().role("assistant").content("c").build())
                        .finishReason("content_filter")
                        .build()))
                .build();
        assertThat(adapter.normalizeResponse(customResp).getStopReason()).isEqualTo("content_filter");
    }

    @Test
    void denormalizeResponse_roundTripsTextToolUseAndUsage() {
        ObjectNode args = new ObjectMapper().createObjectNode();
        args.put("city", "beijing");
        CanonicalChatResponse canonical = CanonicalChatResponse.builder()
                .id("chatcmpl-1")
                .model("gpt-4o")
                .content(List.of(
                        CanonicalContentBlock.builder().type("text").text("正在查询").build(),
                        CanonicalContentBlock.builder()
                                .type("toolUse")
                                .toolUse(CanonicalToolCall.builder()
                                        .id("call_1")
                                        .name("weather")
                                        .arguments(args)
                                        .build())
                                .build()))
                .stopReason("tool_use")
                .usage(CanonicalUsage.builder().inputTokens(10).outputTokens(8).build())
                .build();

        OpenAIChatResponse nativeResp = (OpenAIChatResponse) adapter.denormalizeResponse(canonical);

        assertThat(nativeResp.getId()).isEqualTo("chatcmpl-1");
        assertThat(nativeResp.getChoices()).hasSize(1);
        assertThat(nativeResp.getChoices().get(0).getFinishReason()).isEqualTo("tool_calls");
        OpenAIChatResponse.Message message = nativeResp.getChoices().get(0).getMessage();
        assertThat(message.getContent()).isEqualTo("正在查询");
        assertThat(message.getToolCalls()).hasSize(1);
        assertThat(message.getToolCalls().get(0).getFunction().getArguments()).contains("beijing");
        assertThat(nativeResp.getUsage().getPromptTokens()).isEqualTo(10);
        assertThat(nativeResp.getUsage().getCompletionTokens()).isEqualTo(8);
        assertThat(nativeResp.getUsage().getTotalTokens()).isEqualTo(18);
    }

    @Test
    void denormalizeResponse_mapsMaxTokensAndCustomStopReasons() {
        // max_tokens → length；未知 stop_reason 原样透传
        CanonicalChatResponse maxTokens = CanonicalChatResponse.builder()
                .id("r1").model("gpt-4o").stopReason("max_tokens").build();
        OpenAIChatResponse r1 = (OpenAIChatResponse) adapter.denormalizeResponse(maxTokens);
        assertThat(r1.getChoices().get(0).getFinishReason()).isEqualTo("length");
        assertThat(r1.getUsage()).isNull();

        CanonicalChatResponse custom = CanonicalChatResponse.builder()
                .id("r2").model("gpt-4o").stopReason("content_filter").build();
        OpenAIChatResponse r2 = (OpenAIChatResponse) adapter.denormalizeResponse(custom);
        assertThat(r2.getChoices().get(0).getFinishReason()).isEqualTo("content_filter");
    }

    @Test
    void normalizeResponse_nullChoices_returnsEmptyContent() {
        OpenAIChatResponse resp = OpenAIChatResponse.builder()
                .id("r1").model("gpt-4o")
                .choices(null)
                .usage(null)
                .build();

        CanonicalChatResponse canonical = adapter.normalizeResponse(resp);

        assertThat(canonical.getContent()).isEmpty();
        assertThat(canonical.getUsage()).isNull();
        assertThat(canonical.getStopReason()).isNull();
    }
}
