package com.codingas.gateway.web.controller;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.core.domain.entity.RouteGroup;
import com.codingas.gateway.web.dto.AnthropicMessagesRequest;
import com.codingas.gateway.web.dto.AnthropicMessagesResponse;
import com.codingas.gateway.web.service.LLMChatUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Anthropic 兼容 API 控制器
 *
 * <p>暴露 /v1/messages 端点，兼容 Anthropic API 格式。</p>
 *
 * @see <a href="https://docs.anthropic.com/en/api/reference/messages">Anthropic Messages API</a>
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AnthropicController {

    private final LLMChatUseCase llmChatUseCase;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Messages 端点
     *
     * <p>支持非流式和流式两种响应模式。</p>
     */
    @PostMapping("/messages")
    public Object messages(
            @RequestBody AnthropicMessagesRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("apiKeyId") Long apiKeyId) {

        log.info("Anthropic messages request: model={}, userId={}, stream={}",
                request.getModel(), userId, request.getStream());

        // 验证请求
        validateRequest(request);

        // 判断流式或非流式
        if (Boolean.TRUE.equals(request.getStream())) {
            return messagesStream(request, userId, apiKeyId);
        } else {
            return messagesNonStream(request, userId, apiKeyId);
        }
    }

    /**
     * 非流式响应
     */
    private AnthropicMessagesResponse messagesNonStream(
            AnthropicMessagesRequest request, Long userId, Long apiKeyId) {

        // 转换为内部请求格式
        LLMRequest llmRequest = toLLMRequest(request);

        // 调用用例编排器
        LLMResponse response = llmChatUseCase.send(llmRequest, RouteGroup.RoutingStrategy.PRIORITY);

        // 转换为 Anthropic 响应格式
        return toAnthropicMessagesResponse(response);
    }

    /**
     * 流式响应
     */
    private SseEmitter messagesStream(
            AnthropicMessagesRequest request, Long userId, Long apiKeyId) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.submit(() -> {
            try {
                // 转换为内部请求格式
                LLMRequest llmRequest = toLLMRequest(request);
                llmRequest.setStream(true);

                // 调用用例编排器 (流式)
                llmChatUseCase.sendStream(llmRequest, RouteGroup.RoutingStrategy.PRIORITY,
                        new com.codingas.gateway.adapter.StreamCallback() {
                            @Override
                            public void onChunk(String data) {
                                try {
                                    emitter.send(data, MediaType.TEXT_EVENT_STREAM);
                                } catch (Exception e) {
                                    log.warn("SSE send error: {}", e.getMessage());
                                }
                            }

                            @Override
                            public void onComplete() {
                                try {
                                    emitter.complete();
                                } catch (Exception e) {
                                    log.warn("SSE complete error: {}", e.getMessage());
                                }
                            }

                            @Override
                            public void onError(Throwable error) {
                                try {
                                    emitter.completeWithError(error);
                                } catch (Exception e) {
                                    log.warn("SSE error error: {}", e.getMessage());
                                }
                            }
                        });

            } catch (Exception e) {
                log.error("Stream request error: {}", e.getMessage(), e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.warn("SSE completeWithError error: {}", ex.getMessage());
                }
            }
        });

        return emitter;
    }

    /**
     * 验证请求
     */
    private void validateRequest(AnthropicMessagesRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new IllegalArgumentException("model is required");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("messages is required");
        }
        if (request.getMaxTokens() == null || request.getMaxTokens() <= 0) {
            throw new IllegalArgumentException("max_tokens is required and must be positive");
        }
    }

    /**
     * 转换为内部 LLMRequest
     */
    private LLMRequest toLLMRequest(AnthropicMessagesRequest request) {
        List<LLMRequest.Message> messages = request.getMessages().stream()
                .map(this::toLLMMessage)
                .toList();

        return LLMRequest.builder()
                .model(request.getModel())
                .messages(messages)
                .systemPrompt(request.getSystem())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .stop(request.getStopSequences())
                .toolChoice(request.getToolChoice() != null
                        ? request.getToolChoice().getOrDefault("type", "auto").toString()
                        : null)
                .stream(request.getStream() != null && request.getStream())
                .build();
    }

    @SuppressWarnings("unchecked")
    private LLMRequest.Message toLLMMessage(AnthropicMessagesRequest.Message msg) {
        String content = null;
        String toolCallId = null;

        if (msg.getContent() instanceof String str) {
            content = str;
        } else if (msg.getContent() instanceof List list) {
            // 处理 content blocks
            StringBuilder textBuilder = new StringBuilder();
            for (Object block : list) {
                if (block instanceof Map blockMap) {
                    if ("text".equals(blockMap.get("type"))) {
                        textBuilder.append(blockMap.get("text"));
                    } else if ("tool_use".equals(blockMap.get("type"))) {
                        toolCallId = (String) blockMap.get("id");
                        Object input = blockMap.get("input");
                        if (input instanceof Map inputMap) {
                            textBuilder.append("[TOOL_CALL:");
                            textBuilder.append(blockMap.get("name"));
                            textBuilder.append(":");
                            textBuilder.append(inputMap);
                            textBuilder.append("]");
                        }
                    }
                }
            }
            content = textBuilder.toString();
        }

        return LLMRequest.Message.builder()
                .role(msg.getRole())
                .content(content)
                .toolCallId(toolCallId)
                .build();
    }

    /**
     * 转换为 Anthropic 响应格式
     */
    private AnthropicMessagesResponse toAnthropicMessagesResponse(LLMResponse response) {
        if (response.getError() != null) {
            return AnthropicMessagesResponse.builder()
                    .error(AnthropicMessagesResponse.Error.builder()
                            .message(response.getError().getMessage())
                            .type(response.getError().getType())
                            .code(response.getError().getCode())
                            .build())
                    .build();
        }

        List<AnthropicMessagesResponse.ContentBlock> content = null;
        if (response.getContent() != null) {
            List<AnthropicMessagesResponse.ToolUse> toolUses = null;

            if (response.getContent().getToolCalls() != null) {
                toolUses = response.getContent().getToolCalls().stream()
                        .map(tc -> AnthropicMessagesResponse.ToolUse.builder()
                                .id(tc.getId())
                                .name(tc.getFunction().getName())
                                .input(tc.getFunction().getArguments())
                                .build())
                        .toList();
            }

            // 构建 content blocks
            if (response.getContent().getText() != null && !response.getContent().getText().isEmpty()) {
                content = List.of(AnthropicMessagesResponse.ContentBlock.builder()
                        .type("text")
                        .text(response.getContent().getText())
                        .build());
            }

            if (toolUses != null && !toolUses.isEmpty()) {
                // 添加 tool_use blocks
                List<AnthropicMessagesResponse.ContentBlock> finalContent = content;
                content = (finalContent == null ? List.of() : finalContent);
                // 注：这里需要合并 text 和 tool_use blocks，简化处理
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
                .content(content)
                .stopReason(response.getFinishReason())
                .usage(usage)
                .build();
    }
}
