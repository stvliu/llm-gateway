package com.codingas.gateway.adapter.chat.controller;

import com.codingas.gateway.adapter.chat.dto.AnthropicMessagesRequest;
import com.codingas.gateway.adapter.chat.dto.AnthropicMessagesResponse;
import com.codingas.gateway.application.chat.LLMChatUseCase;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Anthropic 兼容 API 控制器 (WebFlux 版本)
 *
 * <p>暴露 /anthropic/v1/messages 端点，兼容 Anthropic API 格式。</p>
 */
@Slf4j
@RestController
@RequestMapping("/anthropic/v1")
@RequiredArgsConstructor
public class AnthropicController {

    private final LLMChatUseCase llmChatUseCase;

    /**
     * Messages 端点
     */
    @PostMapping("/messages")
    public Mono<Object> messages(
            @RequestBody AnthropicMessagesRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("apiKeyId") Long apiKeyId,
            ServerWebExchange exchange) {

        log.info("Anthropic messages request: model={}, userId={}, stream={}",
                request.getModel(), userId, request.getStream());

        validateRequest(request);

        if (Boolean.TRUE.equals(request.getStream())) {
            return messagesStream(request, userId, apiKeyId, exchange);
        } else {
            return messagesNonStream(request, userId, apiKeyId);
        }
    }

    /**
     * 非流式响应
     */
    private Mono<Object> messagesNonStream(
            AnthropicMessagesRequest request, Long userId, Long apiKeyId) {

        LLMRequest llmRequest = toLLMRequest(request);

        return llmChatUseCase.send(llmRequest, RouteGroup.RoutingStrategy.WEIGHTED)
                .map(this::toAnthropicMessagesResponse)
                .map(ResponseEntity::ok);
    }

    /**
     * 流式响应
     */
    private Mono<Object> messagesStream(
            AnthropicMessagesRequest request, Long userId, Long apiKeyId, ServerWebExchange exchange) {

        LLMRequest llmRequest = toLLMRequest(request);
        llmRequest.setStream(true);

        return llmChatUseCase.sendStream(llmRequest, RouteGroup.RoutingStrategy.WEIGHTED,
                        data -> {
                            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
                            exchange.getResponse().writeWith(
                                    Mono.just(getDataBuffer(exchange, data))
                            ).subscribe();
                        })
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }

    private org.springframework.core.io.buffer.DataBuffer getDataBuffer(ServerWebExchange exchange, String data) {
        try {
            byte[] bytes = data.getBytes("UTF-8");
            var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return buffer;
        } catch (Exception e) {
            log.warn("Error creating data buffer: {}", e.getMessage());
            return exchange.getResponse().bufferFactory().wrap(new byte[0]);
        }
    }

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

    private Object toAnthropicMessagesResponse(LLMResponse response) {
        if (response.getError() != null) {
            return ResponseEntity.badRequest().body(
                    AnthropicMessagesResponse.builder()
                            .error(AnthropicMessagesResponse.Error.builder()
                                    .message(response.getError().getMessage())
                                    .type(response.getError().getType())
                                    .code(response.getError().getCode())
                                    .build())
                            .build());
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

            if (response.getContent().getText() != null && !response.getContent().getText().isEmpty()) {
                content = List.of(AnthropicMessagesResponse.ContentBlock.builder()
                        .type("text")
                        .text(response.getContent().getText())
                        .build());
            }

            if (toolUses != null && !toolUses.isEmpty()) {
                List<AnthropicMessagesResponse.ContentBlock> finalContent = content;
                content = (finalContent == null ? List.of() : finalContent);
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
