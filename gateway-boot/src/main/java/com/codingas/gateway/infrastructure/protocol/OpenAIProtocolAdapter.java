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

import com.codingas.gateway.api.capability.protocol.*;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 协议适配器：OpenAI 原生请求/响应 ↔ 规范内部模型（Canonical IR）。
 *
 * <p>system 角色消息 → 规范顶层 system；tools(Map) → CanonicalTool；
 * 反向 denormalize 时 system 还原为 system 角色消息。</p>
 */
@Component
public class OpenAIProtocolAdapter implements ProtocolAdapter<OpenAIChatRequest> {

    private final ObjectMapper objectMapper;

    public OpenAIProtocolAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String protocol() {
        return "openai";
    }

    @Override
    public CanonicalChatRequest normalizeRequest(OpenAIChatRequest req) {
        String system = null;
        List<CanonicalMessage> messages = new ArrayList<>();
        for (OpenAIChatRequest.Message m : req.getMessages() == null ? List.<OpenAIChatRequest.Message>of() : req.getMessages()) {
            if ("system".equals(m.getRole())) {
                system = m.getContent();
            } else {
                messages.add(CanonicalMessage.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .toolCalls(convertToolCallsToCanonical(m.getToolCalls()))
                        .toolCallId(m.getToolCallId())
                        .name(m.getName())
                        .build());
            }
        }
        return CanonicalChatRequest.builder()
                .model(req.getModel())
                .system(system)
                .messages(messages)
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .stop(req.getStop())
                .tools(convertToolsToCanonical(req.getTools()))
                .toolChoice(req.getToolChoice())
                .stream(req.isStream())
                .build();
    }

    @Override
    public OpenAIChatRequest denormalizeRequest(CanonicalChatRequest c) {
        List<OpenAIChatRequest.Message> messages = new ArrayList<>();
        if (c.getSystem() != null && !c.getSystem().isBlank()) {
            messages.add(OpenAIChatRequest.Message.builder().role("system").content(c.getSystem()).build());
        }
        if (c.getMessages() != null) {
            for (CanonicalMessage cm : c.getMessages()) {
                messages.add(OpenAIChatRequest.Message.builder()
                        .role(cm.getRole())
                        .content(cm.getContent())
                        .toolCalls(convertToolCallsToOpenAI(cm.getToolCalls()))
                        .toolCallId(cm.getToolCallId())
                        .name(cm.getName())
                        .build());
            }
        }
        return OpenAIChatRequest.builder()
                .model(c.getModel())
                .messages(messages)
                .maxTokens(c.getMaxTokens())
                .temperature(c.getTemperature())
                .stop(c.getStop())
                .tools(convertToolsToOpenAI(c.getTools()))
                .toolChoice(c.getToolChoice())
                .stream(c.isStream())
                .build();
    }

    @Override
    public CanonicalChatResponse normalizeResponse(Object nativeResponse) {
        OpenAIChatResponse resp = (OpenAIChatResponse) nativeResponse;
        List<CanonicalContentBlock> blocks = new ArrayList<>();
        if (resp.getChoices() != null && !resp.getChoices().isEmpty()) {
            OpenAIChatResponse.Choice choice = resp.getChoices().get(0);
            if (choice.getMessage() != null) {
                if (choice.getMessage().getContent() != null) {
                    blocks.add(CanonicalContentBlock.builder().type("text").text(choice.getMessage().getContent()).build());
                }
                if (choice.getMessage().getToolCalls() != null) {
                    for (OpenAIChatResponse.ToolCall tc : choice.getMessage().getToolCalls()) {
                        blocks.add(CanonicalContentBlock.builder()
                                .type("toolUse")
                                .toolUse(CanonicalToolCall.builder()
                                        .id(tc.getId())
                                        .name(tc.getFunction() != null ? tc.getFunction().getName() : null)
                                        .arguments(tc.getFunction() != null ? toJsonNode(tc.getFunction().getArguments()) : null)
                                        .build())
                                .build());
                    }
                }
            }
        }
        CanonicalUsage usage = null;
        if (resp.getUsage() != null) {
            usage = CanonicalUsage.builder()
                    .inputTokens(resp.getUsage().getPromptTokens())
                    .outputTokens(resp.getUsage().getCompletionTokens())
                    .build();
        }
        return CanonicalChatResponse.builder()
                .id(resp.getId())
                .model(resp.getModel())
                .content(blocks)
                .stopReason(mapFinishToStop(resp.getFinishReason()))
                .usage(usage)
                .build();
    }

    @Override
    public Object denormalizeResponse(CanonicalChatResponse c) {
        StringBuilder text = new StringBuilder();
        List<OpenAIChatResponse.ToolCall> toolCalls = new ArrayList<>();
        if (c.getContent() != null) {
            for (CanonicalContentBlock b : c.getContent()) {
                if ("text".equals(b.getType()) && b.getText() != null) {
                    text.append(b.getText());
                } else if ("toolUse".equals(b.getType()) && b.getToolUse() != null) {
                    CanonicalToolCall tu = b.getToolUse();
                    toolCalls.add(OpenAIChatResponse.ToolCall.builder()
                            .id(tu.getId())
                            .type("function")
                            .function(OpenAIChatResponse.FunctionCall.builder()
                                    .name(tu.getName())
                                    .arguments(tu.getArguments() != null ? tu.getArguments().toString() : null)
                                    .build())
                            .build());
                }
            }
        }
        OpenAIChatResponse.Message message = OpenAIChatResponse.Message.builder()
                .role("assistant")
                .content(text.toString())
                .toolCalls(toolCalls.isEmpty() ? null : toolCalls)
                .build();
        OpenAIChatResponse.Usage usage = null;
        if (c.getUsage() != null) {
            int in = c.getUsage().getInputTokens() != null ? c.getUsage().getInputTokens() : 0;
            int out = c.getUsage().getOutputTokens() != null ? c.getUsage().getOutputTokens() : 0;
            usage = OpenAIChatResponse.Usage.builder()
                    .promptTokens(in).completionTokens(out).totalTokens(in + out).build();
        }
        return OpenAIChatResponse.builder()
                .id(c.getId())
                .model(c.getModel())
                .choices(List.of(OpenAIChatResponse.Choice.builder()
                        .index(0).message(message).finishReason(mapStopToFinish(c.getStopReason())).build()))
                .usage(usage)
                .build();
    }

    // ---- 工具/工具调用转换 ----

    @SuppressWarnings("unchecked")
    private List<CanonicalTool> convertToolsToCanonical(List<Map<String, Object>> tools) {
        if (tools == null) return null;
        List<CanonicalTool> out = new ArrayList<>();
        for (Map<String, Object> t : tools) {
            Map<String, Object> fn = (Map<String, Object>) t.get("function");
            if (fn != null) {
                out.add(CanonicalTool.builder()
                        .name((String) fn.get("name"))
                        .description((String) fn.get("description"))
                        .parameters(toJsonNode(fn.get("parameters")))
                        .build());
            }
        }
        return out;
    }

    private List<Map<String, Object>> convertToolsToOpenAI(List<CanonicalTool> tools) {
        if (tools == null) return null;
        List<Map<String, Object>> out = new ArrayList<>();
        for (CanonicalTool t : tools) {
            // 用 HashMap 容忍 name/description/parameters 为 null（工具无入参 schema 时合法），
            // 避免 Map.of 对 null 值抛 NPE；配合 @JsonInclude(NON_NULL) 省略 null 字段
            Map<String, Object> openaiTool = new HashMap<>();
            openaiTool.put("type", "function");
            Map<String, Object> function = new HashMap<>();
            function.put("name", t.getName());
            function.put("description", t.getDescription());
            function.put("parameters", t.getParameters());
            openaiTool.put("function", function);
            out.add(openaiTool);
        }
        return out;
    }

    private List<CanonicalToolCall> convertToolCallsToCanonical(List<OpenAIChatRequest.ToolCall> calls) {
        if (calls == null) return null;
        List<CanonicalToolCall> out = new ArrayList<>();
        for (OpenAIChatRequest.ToolCall c : calls) {
            out.add(CanonicalToolCall.builder()
                    .id(c.getId())
                    .name(c.getFunction() != null ? c.getFunction().getName() : null)
                    .arguments(toJsonNode(c.getFunction() != null ? c.getFunction().getArguments() : null))
                    .build());
        }
        return out;
    }

    private List<OpenAIChatRequest.ToolCall> convertToolCallsToOpenAI(List<CanonicalToolCall> calls) {
        if (calls == null) return null;
        List<OpenAIChatRequest.ToolCall> out = new ArrayList<>();
        for (CanonicalToolCall c : calls) {
            out.add(OpenAIChatRequest.ToolCall.builder()
                    .id(c.getId())
                    .type("function")
                    .function(OpenAIChatRequest.FunctionCall.builder()
                            .name(c.getName())
                            .arguments(c.getArguments())
                            .build())
                    .build());
        }
        return out;
    }

    private JsonNode toJsonNode(Object o) {
        return o == null ? null : objectMapper.valueToTree(o);
    }

    private String mapFinishToStop(String finishReason) {
        if (finishReason == null) return null;
        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            default -> finishReason;
        };
    }

    private String mapStopToFinish(String stopReason) {
        if (stopReason == null) return null;
        return switch (stopReason) {
            case "end_turn" -> "stop";
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            default -> stopReason;
        };
    }
}
