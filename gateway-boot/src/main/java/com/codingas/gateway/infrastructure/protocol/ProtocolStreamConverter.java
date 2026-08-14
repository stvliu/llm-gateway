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

import com.codingas.gateway.domain.protocol.contract.StreamChunkResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * 流式 chunk 转换器：OpenAI ↔ Anthropic 的 SSE chunk / 结束标记转换。
 *
 * <p>从旧 {@code ProtocolConverter} 平移而来，纯逻辑拷贝、行为不变。规范 IR 阶段未覆盖流式
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
        }
        return null;
    }

    // ==================== 私有方法 ====================

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
     * finish_reason → stop_reason 映射
     */
    private String mapFinishReasonToStopReason(String finishReason) {
        if (finishReason == null) return null;
        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            default -> finishReason;
        };
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
