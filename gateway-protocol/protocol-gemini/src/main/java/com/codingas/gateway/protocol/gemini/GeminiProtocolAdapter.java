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
package com.codingas.gateway.protocol.gemini;

import com.codingas.gateway.protocol.canonical.CanonicalChatRequest;
import com.codingas.gateway.protocol.canonical.CanonicalChatResponse;
import com.codingas.gateway.protocol.canonical.CanonicalContentBlock;
import com.codingas.gateway.protocol.canonical.CanonicalMessage;
import com.codingas.gateway.protocol.canonical.CanonicalUsage;
import com.codingas.gateway.protocol.ProtocolAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini 协议适配器（示例插件）。
 *
 * <p><b>插件化验证金标准</b>：新增 Gemini 协议仅需新建本插件模块（契约类型
 * {@link GeminiChatRequest} + Adapter + AutoConfiguration + DB 供应商数据），
 * 不改 gateway-proxy / gateway-protocol 核心。请求转换经
 * {@code ProtocolConversionFacade} 按协议名（getProtocol()="gemini"）通用路由。</p>
 */
public class GeminiProtocolAdapter implements ProtocolAdapter<GeminiChatRequest> {

    @Override
    public String protocol() {
        return "gemini";
    }

    @Override
    public CanonicalChatRequest normalizeRequest(GeminiChatRequest req) {
        List<CanonicalMessage> messages = new ArrayList<>();
        for (GeminiChatRequest.Message m : req.getMessages()) {
            messages.add(CanonicalMessage.builder().role(toCanonicalRole(m.role())).content(m.content()).build());
        }
        return CanonicalChatRequest.builder()
                .model(req.getModel())
                .system(req.getSystem())
                .messages(messages)
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .stream(req.isStream())
                .build();
    }

    @Override
    public GeminiChatRequest denormalizeRequest(CanonicalChatRequest c) {
        GeminiChatRequest out = new GeminiChatRequest();
        out.setModel(c.getModel());
        out.setSystem(c.getSystem());
        out.setMaxTokens(c.getMaxTokens());
        out.setTemperature(c.getTemperature());
        out.setStream(c.isStream());
        if (c.getMessages() != null) {
            for (CanonicalMessage cm : c.getMessages()) {
                out.addMessage(new GeminiChatRequest.Message(toGeminiRole(cm.getRole()), cm.getContent()));
            }
        }
        return out;
    }

    /**
     * 规范角色 → Gemini 角色（出站方向）。
     *
     * <p>Gemini generateContent 仅接受 user/model/function 角色；OpenAI/Anthropic
     * 入站的 assistant/tool 必须归一，否则多轮对话（含 assistant 历史）会被 Gemini
     * 以 400 INVALID_ARGUMENT 拒绝。</p>
     */
    private String toGeminiRole(String role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case "assistant" -> "model";
            case "tool" -> "function";
            default -> role;
        };
    }

    /**
     * Gemini 角色 → 规范角色（入站方向，对称映射）。
     */
    private String toCanonicalRole(String role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case "model" -> "assistant";
            case "function" -> "tool";
            default -> role;
        };
    }

    @Override
    public CanonicalChatResponse normalizeResponse(Object nativeResponse) {
        GeminiChatResponse resp = (GeminiChatResponse) nativeResponse;
        return CanonicalChatResponse.builder()
                .id(resp.id())
                .model(resp.model())
                .content(resp.text() == null ? List.of() : List.of(CanonicalContentBlock.builder()
                        .type("text").text(resp.text()).build()))
                .usage(resp.inputTokens() == null ? null : CanonicalUsage.builder()
                        .inputTokens(resp.inputTokens())
                        .outputTokens(resp.outputTokens())
                        .build())
                .stopReason(toCanonicalStopReason(resp.finishReason()))
                .build();
    }

    @Override
    public Object denormalizeResponse(CanonicalChatResponse c) {
        StringBuilder text = new StringBuilder();
        if (c.getContent() != null) {
            for (CanonicalContentBlock b : c.getContent()) {
                if ("text".equals(b.getType()) && b.getText() != null) {
                    text.append(b.getText());
                }
            }
        }
        return new GeminiChatResponse(c.getId(), c.getModel(), text.toString(),
                c.getUsage() == null ? null : c.getUsage().getInputTokens(),
                c.getUsage() == null ? null : c.getUsage().getOutputTokens(),
                c.getStopReason() == null ? null : toGeminiFinishReason(c.getStopReason()));
    }

    /**
     * Gemini finishReason → 规范 stopReason（end_turn/max_tokens/tool_use）
     */
    private String toCanonicalStopReason(String finishReason) {
        if (finishReason == null) {
            return null;
        }
        return switch (finishReason) {
            case "STOP" -> "end_turn";
            case "MAX_TOKENS" -> "max_tokens";
            case "TOOL_CALLS" -> "tool_use";
            default -> null;
        };
    }

    /**
     * 规范 stopReason → Gemini finishReason（反方向映射）
     */
    private String toGeminiFinishReason(String stopReason) {
        if (stopReason == null) {
            return null;
        }
        return switch (stopReason) {
            case "end_turn" -> "STOP";
            case "max_tokens" -> "MAX_TOKENS";
            case "tool_use" -> "TOOL_CALLS";
            default -> null;
        };
    }
}
