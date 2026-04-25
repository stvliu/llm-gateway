package com.codingas.gateway.dispatch;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 协议转换器 - OpenAI ↔ Anthropic 双向转换
 *
 * <p>实现 OpenAI 和 Anthropic API 格式之间的双向转换。</p>
 * <p>用于：当请求方使用一种协议，但目标提供商使用另一种协议时进行转换。</p>
 */
@Slf4j
public class ProtocolTranslator {

    private final ObjectMapper objectMapper;

    public ProtocolTranslator() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将 OpenAI 格式请求转换为 Anthropic 格式
     *
     * @param openAIRequest OpenAI 格式请求
     * @return Anthropic 格式请求 Map
     */
    public Map<String, Object> toAnthropicFormat(LLMRequest openAIRequest) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", openAIRequest.getModel());

        // 转换消息格式
        body.put("messages", convertMessagesToAnthropic(openAIRequest.getMessages()));

        // 系统提示转换
        if (openAIRequest.getSystemPrompt() != null) {
            body.put("system", openAIRequest.getSystemPrompt());
        }

        // 最大 token
        if (openAIRequest.getMaxTokens() != null) {
            body.put("max_tokens", openAIRequest.getMaxTokens());
        } else {
            body.put("max_tokens", 1024);
        }

        // 温度
        if (openAIRequest.getTemperature() != null) {
            body.put("temperature", openAIRequest.getTemperature());
        }

        // 工具转换 (OpenAI tools → Anthropic tools)
        if (openAIRequest.getTools() != null && !openAIRequest.getTools().isEmpty()) {
            body.put("tools", convertToolsToAnthropic(openAIRequest.getTools()));
        }

        return body;
    }

    /**
     * 将 Anthropic 格式响应转换为 OpenAI 格式
     *
     * @param anthropicResponse Anthropic 格式响应 Map
     * @return OpenAI 格式 LLMResponse
     */
    public LLMResponse fromAnthropicResponse(Map<String, Object> anthropicResponse) {
        return LLMResponse.builder()
                .providerCode("anthropic")
                .id((String) anthropicResponse.get("id"))
                .model((String) anthropicResponse.get("model"))
                .content(parseAnthropicContent(anthropicResponse))
                .usage(parseAnthropicUsage(anthropicResponse))
                .finishReason((String) anthropicResponse.get("stop_reason"))
                .stream(false)
                .build();
    }

    /**
     * 将 Anthropic 格式请求转换为 OpenAI 格式
     *
     * @param anthropicRequest Anthropic 格式请求 Map
     * @return LLMRequest
     */
    @SuppressWarnings("unchecked")
    public LLMRequest fromAnthropicRequest(Map<String, Object> anthropicRequest) {
        List<Map<String, Object>> anthropicMessages = (List<Map<String, Object>>) anthropicRequest.get("messages");

        return LLMRequest.builder()
                .model((String) anthropicRequest.get("model"))
                .messages(convertMessagesFromAnthropic(anthropicMessages))
                .systemPrompt((String) anthropicRequest.get("system"))
                .maxTokens(anthropicRequest.get("max_tokens") != null ?
                        ((Number) anthropicRequest.get("max_tokens")).intValue() : null)
                .temperature(anthropicRequest.get("temperature") != null ?
                        ((Number) anthropicRequest.get("temperature")).doubleValue() : null)
                .tools(anthropicRequest.get("tools") != null ?
                        convertToolsFromAnthropic((List<Map<String, Object>>) anthropicRequest.get("tools")) : null)
                .build();
    }

    /**
     * 将 OpenAI 格式响应转换为 Anthropic 格式响应字符串
     *
     * @param openAIResponse OpenAI 格式 LLMResponse
     * @return Anthropic 格式响应 Map
     */
    public Map<String, Object> toAnthropicResponse(LLMResponse openAIResponse) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", openAIResponse.getId());
        response.put("model", openAIResponse.getModel());
        response.put("type", "message");
        response.put("role", "assistant");

        // 转换 content
        List<Map<String, Object>> content = new ArrayList<>();
        if (openAIResponse.getContent() != null) {
            if (openAIResponse.getContent().getText() != null && !openAIResponse.getContent().getText().isEmpty()) {
                content.add(Map.of("type", "text", "text", openAIResponse.getContent().getText()));
            }
            if (openAIResponse.getContent().getToolCalls() != null) {
                for (LLMResponse.ToolCall tc : openAIResponse.getContent().getToolCalls()) {
                    content.add(Map.of(
                            "type", "tool_use",
                            "id", tc.getId(),
                            "name", tc.getFunction().getName(),
                            "input", parseJsonString(tc.getFunction().getArguments()))
                    );
                }
            }
        }
        response.put("content", content);

        // 转换 usage
        if (openAIResponse.getUsage() != null) {
            Map<String, Object> usage = new HashMap<>();
            usage.put("input_tokens", openAIResponse.getUsage().getPromptTokens());
            usage.put("output_tokens", openAIResponse.getUsage().getCompletionTokens());
            response.put("usage", usage);
        }

        response.put("stop_reason", openAIResponse.getFinishReason());
        response.put("stop_sequence", null);

        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertMessagesToAnthropic(List<LLMRequest.Message> messages) {
        if (messages == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LLMRequest.Message msg : messages) {
            Map<String, Object> anthropicMsg = new HashMap<>();
            String role = convertRoleToAnthropic(msg.getRole());
            anthropicMsg.put("role", role);

            // 构建 content
            List<Map<String, Object>> contentBlocks = new ArrayList<>();

            // 添加文本内容
            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                contentBlocks.add(Map.of("type", "text", "text", msg.getContent()));
            }

            // 处理 tool_calls
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                for (LLMRequest.ToolCall tc : msg.getToolCalls()) {
                    Map<String, Object> toolUse = new HashMap<>();
                    toolUse.put("type", "tool_use");
                    toolUse.put("id", tc.getId());
                    toolUse.put("name", tc.getFunction().getName());
                    toolUse.put("input", parseJsonString(tc.getFunction().getArguments()));
                    contentBlocks.add(toolUse);
                }
            }

            // 处理 tool 结果
            if ("tool".equals(msg.getRole()) && msg.getToolCallId() != null) {
                // Anthropic 中 tool 结果作为 user 角色的 text block
                anthropicMsg.put("role", "user");
                if (msg.getContent() != null) {
                    contentBlocks.add(Map.of("type", "text", "text", msg.getContent()));
                }
            }

            if (contentBlocks.size() == 1 && contentBlocks.get(0).get("type").equals("text")) {
                anthropicMsg.put("content", contentBlocks.get(0).get("text"));
            } else if (!contentBlocks.isEmpty()) {
                anthropicMsg.put("content", contentBlocks);
            } else {
                anthropicMsg.put("content", "");
            }

            result.add(anthropicMsg);
        }
        return result;
    }

    private String convertRoleToAnthropic(String role) {
        if (role == null) return "user";
        return switch (role) {
            case "system" -> "user";
            case "assistant" -> "assistant";
            case "tool" -> "user";
            default -> "user";
        };
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertToolsToAnthropic(List<LLMRequest.ToolDefinition> tools) {
        List<Map<String, Object>> anthropicTools = new ArrayList<>();
        for (LLMRequest.ToolDefinition tool : tools) {
            Map<String, Object> anthropicTool = new HashMap<>();
            anthropicTool.put("name", tool.getFunction().getName());
            anthropicTool.put("description", tool.getFunction().getDescription());

            // 将 JSON Schema 字符串转换为 Map
            if (tool.getFunction().getParameters() != null) {
                anthropicTool.put("input_schema", parseJsonString(tool.getFunction().getParameters()));
            } else {
                anthropicTool.put("input_schema", Map.of());
            }

            anthropicTools.add(anthropicTool);
        }
        return anthropicTools;
    }

    @SuppressWarnings("unchecked")
    private List<LLMRequest.Message> convertMessagesFromAnthropic(List<Map<String, Object>> messages) {
        if (messages == null) {
            return Collections.emptyList();
        }

        List<LLMRequest.Message> result = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            LLMRequest.Message message = LLMRequest.Message.builder()
                    .role(convertRoleFromAnthropic((String) msg.get("role")))
                    .content(extractContentFromAnthropic(msg))
                    .toolCallId(msg.get("tool_use_id") != null ? (String) msg.get("tool_use_id") : null)
                    .build();
            result.add(message);
        }
        return result;
    }

    private String convertRoleFromAnthropic(String role) {
        if (role == null) return "user";
        return switch (role) {
            case "user" -> "user";
            case "assistant" -> "assistant";
            default -> "user";
        };
    }

    @SuppressWarnings("unchecked")
    private String extractContentFromAnthropic(Map<String, Object> msg) {
        Object content = msg.get("content");
        if (content == null) {
            return null;
        }

        if (content instanceof String) {
            return (String) content;
        }

        if (content instanceof List) {
            List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) content;
            StringBuilder text = new StringBuilder();
            for (Map<String, Object> block : contentBlocks) {
                if ("text".equals(block.get("type"))) {
                    Object textObj = block.get("text");
                    if (textObj != null) {
                        text.append(textObj.toString());
                    }
                }
            }
            return text.toString();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<LLMRequest.ToolDefinition> convertToolsFromAnthropic(List<Map<String, Object>> tools) {
        List<LLMRequest.ToolDefinition> openAITools = new ArrayList<>();
        for (Map<String, Object> tool : tools) {
            Map<String, Object> function = (Map<String, Object>) tool.get("function");
            openAITools.add(LLMRequest.ToolDefinition.builder()
                    .type("function")
                    .function(LLMRequest.Function.builder()
                            .name((String) tool.get("name"))
                            .description(function != null && function.get("description") != null ?
                                    (String) function.get("description") : "")
                            .parameters(function != null && function.get("parameters") != null ?
                                    toJsonString(function.get("parameters")) : "{}")
                            .build())
                    .build());
        }
        return openAITools;
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Content parseAnthropicContent(Map<String, Object> response) {
        var content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            return null;
        }

        StringBuilder textBuilder = new StringBuilder();
        List<LLMResponse.ToolCall> toolCalls = null;

        for (Map<String, Object> block : content) {
            String blockType = (String) block.get("type");
            if ("text".equals(blockType)) {
                String text = (String) block.get("text");
                if (text != null) {
                    textBuilder.append(text);
                }
            } else if ("tool_use".equals(blockType)) {
                if (toolCalls == null) {
                    toolCalls = new ArrayList<>();
                }
                String toolName = (String) block.get("name");
                Object inputObj = block.get("input");
                String toolInputJson = inputObj != null ? toJsonString(inputObj) : "{}";
                String toolId = (String) block.get("id");

                toolCalls.add(LLMResponse.ToolCall.builder()
                        .id(toolId)
                        .type("function")
                        .function(LLMResponse.FunctionCall.builder()
                                .name(toolName)
                                .arguments(toolInputJson)
                                .build())
                        .build());
            }
        }

        return LLMResponse.Content.builder()
                .role("assistant")
                .text(textBuilder.toString())
                .toolCalls(toolCalls)
                .build();
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Usage parseAnthropicUsage(Map<String, Object> response) {
        var usage = (Map<String, Object>) response.get("usage");
        if (usage == null) {
            return null;
        }
        return LLMResponse.Usage.builder()
                .promptTokens(usage.get("input_tokens") != null ? ((Number) usage.get("input_tokens")).intValue() : null)
                .completionTokens(usage.get("output_tokens") != null ? ((Number) usage.get("output_tokens")).intValue() : null)
                .totalTokens(null)
                .build();
    }

    private Map<String, Object> parseJsonString(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON string: {}", json);
            return new HashMap<>();
        }
    }

    private String toJsonString(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON: {}", obj);
            return "{}";
        }
    }
}
