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
package com.codingas.gateway.protocol.anthropic;

import com.codingas.gateway.protocol.canonical.CanonicalChatRequest;
import com.codingas.gateway.protocol.canonical.CanonicalChatResponse;
import com.codingas.gateway.protocol.canonical.CanonicalContentBlock;
import com.codingas.gateway.protocol.canonical.CanonicalMessage;
import com.codingas.gateway.protocol.canonical.CanonicalTool;
import com.codingas.gateway.protocol.canonical.CanonicalToolCall;
import com.codingas.gateway.protocol.canonical.CanonicalUsage;
import com.codingas.gateway.protocol.ProtocolAdapter;
import com.codingas.gateway.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.protocol.contract.AnthropicMessagesResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic 协议适配器：Anthropic Messages API 原生请求/响应 ↔ 规范内部模型（Canonical IR）。
 *
 * <p>顶层 system 字段 ↔ 规范顶层 system；content blocks ↔ 规范 content。
 * 规范 tools 转换：Anthropic {"name","description","input_schema"} 与规范 CanonicalTool 对齐。</p>
 */
public class AnthropicProtocolAdapter implements ProtocolAdapter<AnthropicMessagesRequest> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicProtocolAdapter() {
    }

    @Override
    public String protocol() {
        return "anthropic";
    }

    @Override
    public CanonicalChatRequest normalizeRequest(AnthropicMessagesRequest req) {
        List<CanonicalMessage> messages = new ArrayList<>();
        if (req.getMessages() != null) {
            for (AnthropicMessagesRequest.Message m : req.getMessages()) {
                // content 可能是 String 或 content blocks 列表（Object），规范统一为文本表示
                messages.add(CanonicalMessage.builder()
                        .role(m.getRole())
                        .content(m.getContent() instanceof String s ? s : String.valueOf(m.getContent()))
                        .build());
            }
        }
        return CanonicalChatRequest.builder()
                .model(req.getModel())
                .system(req.getSystem())
                .messages(messages)
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .stop(req.getStopSequences())
                .tools(convertToolsToCanonical(req.getTools()))
                // toolChoice 存在但 type 键缺失/null 时返回 null（对齐旧版 convertAnthropicToolChoice 语义），
                // 避免 String.valueOf 产出字面量 "null"
                .toolChoice(req.getToolChoice() != null && req.getToolChoice().get("type") != null
                        ? req.getToolChoice().get("type").toString() : null)
                .stream(req.isStream())
                .build();
    }

    @Override
    public AnthropicMessagesRequest denormalizeRequest(CanonicalChatRequest c) {
        List<AnthropicMessagesRequest.Message> messages = new ArrayList<>();
        if (c.getMessages() != null) {
            for (CanonicalMessage cm : c.getMessages()) {
                messages.add(AnthropicMessagesRequest.Message.builder()
                        .role(cm.getRole())
                        .content(cm.getContent())
                        .build());
            }
        }
        return AnthropicMessagesRequest.builder()
                .model(c.getModel())
                .system(c.getSystem())
                .messages(messages)
                .maxTokens(c.getMaxTokens())
                .temperature(c.getTemperature())
                .stopSequences(c.getStop())
                .tools(convertToolsToAnthropic(c.getTools()))
                .toolChoice(c.getToolChoice() != null ? Map.of("type", c.getToolChoice()) : null)
                .stream(c.isStream())
                .build();
    }

    @Override
    public CanonicalChatResponse normalizeResponse(Object nativeResponse) {
        AnthropicMessagesResponse resp = (AnthropicMessagesResponse) nativeResponse;
        List<CanonicalContentBlock> blocks = new ArrayList<>();
        if (resp.getContent() != null) {
            for (AnthropicMessagesResponse.ContentBlock b : resp.getContent()) {
                if ("text".equals(b.getType())) {
                    blocks.add(CanonicalContentBlock.builder().type("text").text(b.getText()).build());
                } else if ("tool_use".equals(b.getType()) && b.getToolUse() != null) {
                    blocks.add(CanonicalContentBlock.builder()
                            .type("toolUse")
                            .toolUse(CanonicalToolCall.builder()
                                    .id(b.getToolUse().getId())
                                    .name(b.getToolUse().getName())
                                    .arguments(toJsonNode(b.getToolUse().getInput()))
                                    .build())
                            .build());
                }
            }
        }
        CanonicalUsage usage = null;
        if (resp.getUsage() != null) {
            usage = CanonicalUsage.builder()
                    .inputTokens(resp.getUsage().getInputTokens())
                    .outputTokens(resp.getUsage().getOutputTokens())
                    .build();
        }
        return CanonicalChatResponse.builder()
                .id(resp.getId())
                .model(resp.getModel())
                .content(blocks)
                .stopReason(resp.getStopReason())
                .usage(usage)
                .build();
    }

    @Override
    public Object denormalizeResponse(CanonicalChatResponse c) {
        List<AnthropicMessagesResponse.ContentBlock> blocks = new ArrayList<>();
        if (c.getContent() != null) {
            for (CanonicalContentBlock b : c.getContent()) {
                if ("text".equals(b.getType())) {
                    blocks.add(AnthropicMessagesResponse.ContentBlock.builder().type("text").text(b.getText()).build());
                } else if ("toolUse".equals(b.getType()) && b.getToolUse() != null) {
                    CanonicalToolCall tu = b.getToolUse();
                    blocks.add(AnthropicMessagesResponse.ContentBlock.builder()
                            .type("tool_use")
                            .toolUse(AnthropicMessagesResponse.ToolUse.builder()
                                    .id(tu.getId())
                                    .name(tu.getName())
                                    .input(tu.getArguments())
                                    .build())
                            .build());
                }
            }
        }
        return AnthropicMessagesResponse.builder()
                .id(c.getId())
                .model(c.getModel())
                .type("message")
                .role("assistant")
                .content(blocks)
                .stopReason(c.getStopReason())
                .usage(c.getUsage() != null ? AnthropicMessagesResponse.Usage.builder()
                        .inputTokens(c.getUsage().getInputTokens())
                        .outputTokens(c.getUsage().getOutputTokens())
                        .build() : null)
                .build();
    }

    // ---- 工具转换 ----

    private List<CanonicalTool> convertToolsToCanonical(List<Map<String, Object>> tools) {
        if (tools == null) return null;
        List<CanonicalTool> out = new ArrayList<>();
        for (Map<String, Object> t : tools) {
            out.add(CanonicalTool.builder()
                    .name((String) t.get("name"))
                    .description((String) t.get("description"))
                    .parameters(toJsonNode(t.get("input_schema")))
                    .build());
        }
        return out;
    }

    private List<Map<String, Object>> convertToolsToAnthropic(List<CanonicalTool> tools) {
        if (tools == null) return null;
        List<Map<String, Object>> out = new ArrayList<>();
        for (CanonicalTool t : tools) {
            // 用 HashMap 容忍 name/description/input_schema 为 null（工具无入参 schema 时合法），
            // 避免 Map.of 对 null 值抛 NPE；description 缺省为 null，由 @JsonInclude(NON_NULL) 序列化省略
            Map<String, Object> anthropicTool = new HashMap<>();
            anthropicTool.put("name", t.getName());
            anthropicTool.put("description", t.getDescription());
            anthropicTool.put("input_schema", t.getParameters());
            out.add(anthropicTool);
        }
        return out;
    }

    private JsonNode toJsonNode(Object o) {
        return o == null ? null : objectMapper.valueToTree(o);
    }
}
