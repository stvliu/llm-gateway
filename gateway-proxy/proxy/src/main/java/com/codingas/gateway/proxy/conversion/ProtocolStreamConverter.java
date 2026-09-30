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
package com.codingas.gateway.proxy.conversion;

import com.codingas.gateway.protocol.StreamChunkResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * 流式 chunk 转换器：OpenAI ↔ Anthropic 的 SSE chunk / 结束标记转换。
 *
 * <p>从旧协议转换逻辑平移而来，纯逻辑拷贝、行为不变。规范 IR 阶段未覆盖流式
 * （YAGNI，见 spec §D7 落地顺序），故本轮保持原样。</p>
 */
@Component
public class ProtocolStreamConverter {

    private final ObjectMapper objectMapper;

    public ProtocolStreamConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 流式 chunk 转换
     *
     * @param rawChunk     原始 SSE data 行的 JSON 字符串
     * @param fromProtocol 源协议
     * @param toProtocol   目标协议
     * @return 转换结果，null 表示无效/空 chunk
     */
    public StreamChunkResult convertStreamChunk(String rawChunk, String fromProtocol, String toProtocol) {
        if (rawChunk == null || rawChunk.isBlank()) {
            return null;
        }
        if (fromProtocol.equals(toProtocol)) {
            return StreamChunkResult.dataOnly(rawChunk);
        }

        try {
            JsonNode node = objectMapper.readTree(rawChunk);
            if (fromProtocol.equals("openai") && toProtocol.equals("anthropic")) {
                return convertOpenAIChunkToAnthropic(node);
            } else if (fromProtocol.equals("anthropic") && toProtocol.equals("openai")) {
                return convertAnthropicChunkToOpenAI(node);
            } else if (fromProtocol.equals("gemini") && toProtocol.equals("openai")) {
                return convertGeminiChunkToOpenAI(node);
            } else if (fromProtocol.equals("gemini") && toProtocol.equals("anthropic")) {
                return convertGeminiChunkToAnthropic(node);
            }
            return StreamChunkResult.dataOnly(rawChunk);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 流式结束标记转换
     */
    public StreamChunkResult convertStreamDone(String fromProtocol, String toProtocol) {
        if (fromProtocol.equals("openai") && toProtocol.equals("anthropic")) {
            return StreamChunkResult.of("message_delta",
                    "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"}}");
        } else if (fromProtocol.equals("anthropic") && toProtocol.equals("openai")) {
            return StreamChunkResult.dataOnly("[DONE]");
        } else if (fromProtocol.equals("gemini") && toProtocol.equals("openai")) {
            return StreamChunkResult.dataOnly("[DONE]");
        } else if (fromProtocol.equals("gemini") && toProtocol.equals("anthropic")) {
            return StreamChunkResult.of("message_delta",
                    "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"}}");
        }
        return null;
    }

    // ==================== 私有方法 ====================

    /**
     * Gemini chunk → OpenAI：candidates[0] 文本 → delta.content，finishReason → finish_reason
     *
     * <p>无候选（纯 usageMetadata / 空 chunk）返回 null；文本与结束原因可同时出现，
     * 分别填充 delta 与 finish_reason（OpenAI 客户端兼容）。</p>
     */
    private StreamChunkResult convertGeminiChunkToOpenAI(JsonNode node) {
        JsonNode candidates = node.path("candidates");
        if (candidates.isEmpty()) {
            return null;
        }
        JsonNode first = candidates.get(0);
        String text = extractGeminiText(first);
        String finishReason = first.path("finishReason").asText(null);
        if (text == null && finishReason == null) {
            return null;
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("id", "chatcmpl-gemini");
        result.put("object", "chat.completion.chunk");
        ObjectNode choiceNode = result.putArray("choices").addObject();
        choiceNode.put("index", 0);
        ObjectNode deltaNode = choiceNode.putObject("delta");
        if (text != null) {
            deltaNode.put("content", text);
        }
        choiceNode.put("finish_reason", mapGeminiFinishReason(finishReason));
        return StreamChunkResult.dataOnly(result.toString());
    }

    /**
     * Gemini chunk → Anthropic：文本 → content_block_delta（text_delta）。
     *
     * <p>仅结束原因（无文本）的末 chunk 返回 null 跳过——结束标记统一由
     * {@link #convertStreamDone} 补发 message_delta，避免与 EOF 结束标记重复发送
     * 两条 message_delta（超出 Anthropic 协议规范）。</p>
     */
    private StreamChunkResult convertGeminiChunkToAnthropic(JsonNode node) {
        JsonNode candidates = node.path("candidates");
        if (candidates.isEmpty()) {
            return null;
        }
        JsonNode first = candidates.get(0);
        String text = extractGeminiText(first);
        if (text == null) {
            return null;
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.put("type", "content_block_delta");
        result.put("index", 0);
        ObjectNode delta = result.putObject("delta");
        delta.put("type", "text_delta");
        delta.put("text", text);
        return StreamChunkResult.of("content_block_delta", result.toString());
    }

    /**
     * 提取 Gemini 候选的文本内容（content.parts[].text 拼接）；无文本返回 null
     */
    private String extractGeminiText(JsonNode candidate) {
        JsonNode parts = candidate.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            String t = part.path("text").asText(null);
            if (t != null) {
                sb.append(t);
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Gemini finishReason → OpenAI finish_reason 映射
     */
    private String mapGeminiFinishReason(String finishReason) {
        if (finishReason == null) {
            return null;
        }
        return switch (finishReason) {
            case "STOP" -> "stop";
            case "MAX_TOKENS" -> "length";
            case "SAFETY", "RECITATION", "PROHIBITED_CONTENT" -> "content_filter";
            case "TOOL_CALLS" -> "tool_calls";
            default -> "stop";
        };
    }

    private StreamChunkResult convertOpenAIChunkToAnthropic(JsonNode node) {
        JsonNode choices = node.path("choices");
        if (choices.isEmpty()) return null;

        JsonNode delta = choices.get(0).path("delta");
        JsonNode content = delta.path("content");
        if (content.isMissingNode() || content.isNull()) return null;

        ObjectNode result = objectMapper.createObjectNode();
        result.put("type", "content_block_delta");
        result.put("index", 0);
        ObjectNode deltaNode = objectMapper.createObjectNode();
        deltaNode.put("type", "text_delta");
        deltaNode.set("text", content);
        result.set("delta", deltaNode);

        return StreamChunkResult.of("content_block_delta", result.toString());
    }

    private StreamChunkResult convertAnthropicChunkToOpenAI(JsonNode node) {
        String type = node.path("type").asText("");

        if ("content_block_delta".equals(type)) {
            JsonNode delta = node.path("delta");
            String text = delta.path("text").asText(null);
            if (text == null) return null;

            ObjectNode result = objectMapper.createObjectNode();
            result.put("id", "chatcmpl-anthropic");
            result.put("object", "chat.completion.chunk");
            ObjectNode choiceNode = objectMapper.createObjectNode();
            choiceNode.put("index", 0);
            ObjectNode deltaNode = objectMapper.createObjectNode();
            deltaNode.put("content", text);
            choiceNode.set("delta", deltaNode);
            choiceNode.putNull("finish_reason");
            result.putArray("choices").add(choiceNode);

            return StreamChunkResult.dataOnly(result.toString());
        }

        if ("message_delta".equals(type)) {
            String stopReason = node.path("delta").path("stop_reason").asText(null);
            String finishReason = mapStopReasonToFinishReason(stopReason);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("id", "chatcmpl-anthropic");
            result.put("object", "chat.completion.chunk");
            ObjectNode choiceNode = objectMapper.createObjectNode();
            choiceNode.put("index", 0);
            choiceNode.putObject("delta");
            choiceNode.put("finish_reason", finishReason);
            result.putArray("choices").add(choiceNode);

            return StreamChunkResult.dataOnly(result.toString());
        }

        return null;
    }

    /**
     * stop_reason → finish_reason 映射
     */
    private String mapStopReasonToFinishReason(String stopReason) {
        if (stopReason == null) return null;
        return switch (stopReason) {
            case "end_turn" -> "stop";
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            default -> stopReason;
        };
    }
}
