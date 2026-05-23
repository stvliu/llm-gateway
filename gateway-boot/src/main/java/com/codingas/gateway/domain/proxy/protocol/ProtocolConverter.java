package com.codingas.gateway.domain.proxy.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 跨协议转换器，处理 OpenAI ↔ Anthropic 的请求/响应/流式 chunk 转换
 */
@Component
public class ProtocolConverter {

    private static final int DEFAULT_MAX_TOKENS = 1024;
    private final ObjectMapper objectMapper;

    public ProtocolConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== 请求转换 ====================

    /**
     * OpenAI 请求 → Anthropic 请求
     *
     * <p>system 角色消息提取到顶层 system 字段；max_tokens 缺省补 1024</p>
     */
    public AnthropicMessagesRequest toAnthropic(OpenAIChatRequest request) {
        String system = null;
        List<AnthropicMessagesRequest.Message> anthropicMessages = new ArrayList<>();

        for (OpenAIChatRequest.Message msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) {
                system = msg.getContent();
            } else {
                anthropicMessages.add(AnthropicMessagesRequest.Message.builder()
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .build());
            }
        }

        return AnthropicMessagesRequest.builder()
                .model(request.getModel())
                .messages(anthropicMessages)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : DEFAULT_MAX_TOKENS)
                .temperature(request.getTemperature())
                .stopSequences(request.getStop())
                .tools(request.getTools())
                .toolChoice(request.getToolChoice() != null ? Map.of("type", request.getToolChoice()) : null)
                .stream(request.isStream() ? true : null)
                .system(system)
                .build();
    }

    /**
     * Anthropic 请求 → OpenAI 请求
     *
     * <p>顶层 system 字段合并为 system 角色消息；content blocks 拼接为 string</p>
     */
    public OpenAIChatRequest toOpenAI(AnthropicMessagesRequest request) {
        List<OpenAIChatRequest.Message> openaiMessages = new ArrayList<>();

        if (request.getSystem() != null && !request.getSystem().isBlank()) {
            openaiMessages.add(OpenAIChatRequest.Message.builder()
                    .role("system")
                    .content(request.getSystem())
                    .build());
        }

        if (request.getMessages() != null) {
            for (AnthropicMessagesRequest.Message msg : request.getMessages()) {
                String content = msg.getContent() instanceof String s ? s : String.valueOf(msg.getContent());
                openaiMessages.add(OpenAIChatRequest.Message.builder()
                        .role(msg.getRole())
                        .content(content)
                        .build());
            }
        }

        return OpenAIChatRequest.builder()
                .model(request.getModel())
                .messages(openaiMessages)
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .stop(request.getStopSequences())
                .tools(request.getTools())
                .toolChoice(convertAnthropicToolChoice(request.getToolChoice()))
                .stream(request.isStream() ? true : null)
                .build();
    }

    // ==================== 响应转换 ====================

    /**
     * OpenAI 响应 → Anthropic 响应
     *
     * <p>choices[0] → content blocks；finish_reason → stop_reason；usage 字段名映射</p>
     */
    public AnthropicMessagesResponse toAnthropic(OpenAIChatResponse response) {
        List<AnthropicMessagesResponse.ContentBlock> contentBlocks = new ArrayList<>();

        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            OpenAIChatResponse.Choice choice = response.getChoices().get(0);
            if (choice.getMessage() != null) {
                if (choice.getMessage().getContent() != null) {
                    contentBlocks.add(AnthropicMessagesResponse.ContentBlock.builder()
                            .type("text")
                            .text(choice.getMessage().getContent())
                            .build());
                }
            }
        }

        AnthropicMessagesResponse.Usage usage = null;
        if (response.getUsage() != null) {
            usage = AnthropicMessagesResponse.Usage.builder()
                    .inputTokens(response.getUsage().getPromptTokens())
                    .outputTokens(response.getUsage().getCompletionTokens())
                    .build();
        }

        return AnthropicMessagesResponse.builder()
                .id(response.getId())
                .model(response.getModel())
                .type("message")
                .role("assistant")
                .content(contentBlocks)
                .stopReason(mapFinishReasonToStopReason(response.getFinishReason()))
                .usage(usage)
                .build();
    }

    /**
     * Anthropic 响应 → OpenAI 响应
     *
     * <p>content blocks → choices[0].message；stop_reason → finish_reason；补 total_tokens</p>
     */
    public OpenAIChatResponse toOpenAI(AnthropicMessagesResponse response) {
        StringBuilder contentText = new StringBuilder();
        if (response.getContent() != null) {
            for (AnthropicMessagesResponse.ContentBlock block : response.getContent()) {
                if ("text".equals(block.getType()) && block.getText() != null) {
                    contentText.append(block.getText());
                }
            }
        }

        OpenAIChatResponse.Message message = OpenAIChatResponse.Message.builder()
                .role("assistant")
                .content(contentText.toString())
                .build();

        List<OpenAIChatResponse.Choice> choices = List.of(
                OpenAIChatResponse.Choice.builder()
                        .index(0)
                        .message(message)
                        .finishReason(mapStopReasonToFinishReason(response.getStopReason()))
                        .build()
        );

        OpenAIChatResponse.Usage usage = null;
        if (response.getUsage() != null) {
            int inputTokens = response.getUsage().getInputTokens() != null ? response.getUsage().getInputTokens() : 0;
            int outputTokens = response.getUsage().getOutputTokens() != null ? response.getUsage().getOutputTokens() : 0;
            usage = OpenAIChatResponse.Usage.builder()
                    .promptTokens(inputTokens)
                    .completionTokens(outputTokens)
                    .totalTokens(inputTokens + outputTokens)
                    .build();
        }

        return OpenAIChatResponse.builder()
                .id(response.getId())
                .model(response.getModel())
                .choices(choices)
                .usage(usage)
                .build();
    }

    // ==================== 流式 chunk 转换 ====================

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

    /**
     * Anthropic tool_choice (Map) → OpenAI tool_choice (String)
     */
    private String convertAnthropicToolChoice(Map<String, Object> toolChoice) {
        if (toolChoice == null) return null;
        Object type = toolChoice.get("type");
        return type != null ? type.toString() : null;
    }
}