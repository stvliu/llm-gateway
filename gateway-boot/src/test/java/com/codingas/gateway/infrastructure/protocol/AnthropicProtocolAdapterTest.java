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
package com.codingas.gateway.infrastructure.protocol;

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalChatResponse;
import com.codingas.gateway.api.capability.protocol.CanonicalContentBlock;
import com.codingas.gateway.api.capability.protocol.CanonicalMessage;
import com.codingas.gateway.api.capability.protocol.CanonicalTool;
import com.codingas.gateway.api.capability.protocol.CanonicalToolCall;
import com.codingas.gateway.api.capability.protocol.CanonicalUsage;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicProtocolAdapterTest {

    private AnthropicProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AnthropicProtocolAdapter();
    }

    @Test
    void protocolReturnsAnthropic() {
        assertThat(adapter.protocol()).isEqualTo("anthropic");
    }

    @Test
    void normalizeRequestKeepsTopLevelSystem() {
        AnthropicMessagesRequest req = AnthropicMessagesRequest.builder()
                .model("claude-4")
                .system("你是助手")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hi").build()))
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getSystem()).isEqualTo("你是助手");
        assertThat(c.getMessages()).hasSize(1);
        assertThat(c.getMessages().get(0).getRole()).isEqualTo("user");
    }

    @Test
    void denormalizeRequestRestoresSystem() {
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("claude-4")
                .system("sys")
                .messages(List.of(CanonicalMessage.builder().role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest req = adapter.denormalizeRequest(c);

        assertThat(req.getSystem()).isEqualTo("sys");
        assertThat(req.getMessages()).hasSize(1);
        assertThat(req.getStopSequences()).isNull();
    }

    @Test
    void denormalizeRequestToleratesToolWithoutInputSchema() {
        // 工具无 input_schema（parameters=null）时 denormalizeRequest 不得抛 NPE（null 容忍裁定）
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("claude-4")
                .tools(List.of(CanonicalTool.builder()
                        .name("f1")
                        .description(null)
                        .parameters(null)
                        .build()))
                .build();

        AnthropicMessagesRequest nativeReq = adapter.denormalizeRequest(c);

        assertThat(nativeReq.getTools()).hasSize(1);
        Map<String, Object> tool = nativeReq.getTools().get(0);
        assertThat(tool.get("name")).isEqualTo("f1");
        assertThat(tool.containsKey("description")).isTrue(); // null 值保留，由序列化层省略
        assertThat(tool.get("input_schema")).isNull();
    }

    @Test
    void normalizeDenormalizeResponseRoundTripsToolUse() {
        ObjectNode input = new ObjectMapper().createObjectNode();
        input.put("location", "beijing");
        AnthropicMessagesResponse resp = AnthropicMessagesResponse.builder()
                .id("msg_123")
                .model("claude-4")
                .stopReason("tool_use")
                .content(List.of(AnthropicMessagesResponse.ContentBlock.builder()
                        .type("text").text("正在查询").build(),
                        AnthropicMessagesResponse.ContentBlock.builder()
                                .type("tool_use")
                                .toolUse(AnthropicMessagesResponse.ToolUse.builder()
                                        .id("toolu_1")
                                        .name("weather")
                                        .input(input)
                                        .build())
                                .build()))
                .usage(AnthropicMessagesResponse.Usage.builder()
                        .inputTokens(10).outputTokens(5).build())
                .build();

        CanonicalChatResponse canonical = adapter.normalizeResponse(resp);

        assertThat(canonical.getId()).isEqualTo("msg_123");
        assertThat(canonical.getStopReason()).isEqualTo("tool_use");
        assertThat(canonical.getContent()).hasSize(2);
        CanonicalContentBlock textBlock = canonical.getContent().get(0);
        assertThat(textBlock.getType()).isEqualTo("text");
        assertThat(textBlock.getText()).isEqualTo("正在查询");
        CanonicalContentBlock toolBlock = canonical.getContent().get(1);
        assertThat(toolBlock.getType()).isEqualTo("toolUse");
        assertThat(toolBlock.getToolUse().getId()).isEqualTo("toolu_1");
        assertThat(toolBlock.getToolUse().getName()).isEqualTo("weather");
        assertThat(toolBlock.getToolUse().getArguments()).isEqualTo(input);
        assertThat(canonical.getUsage().getInputTokens()).isEqualTo(10);
        assertThat(canonical.getUsage().getOutputTokens()).isEqualTo(5);

        // denormalize 回原生响应，断言等价
        AnthropicMessagesResponse back = (AnthropicMessagesResponse) adapter.denormalizeResponse(canonical);
        assertThat(back.getType()).isEqualTo("message");
        assertThat(back.getRole()).isEqualTo("assistant");
        assertThat(back.getContent()).hasSize(2);
        AnthropicMessagesResponse.ToolUse tu = back.getContent().get(1).getToolUse();
        assertThat(tu.getId()).isEqualTo("toolu_1");
        assertThat(tu.getName()).isEqualTo("weather");
        assertThat(tu.getInput()).isEqualTo(input);
        assertThat(back.getUsage().getInputTokens()).isEqualTo(10);
        assertThat(back.getUsage().getOutputTokens()).isEqualTo(5);
    }
}
