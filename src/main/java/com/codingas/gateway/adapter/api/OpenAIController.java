package com.codingas.gateway.adapter.api;

import com.codingas.gateway.adapter.chat.dto.OpenAIChatRequest;
import com.codingas.gateway.adapter.chat.dto.OpenAIChatResponse;
import com.codingas.gateway.application.chat.LLMChatUseCase;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * OpenAI 兼容 API 控制器
 *
 * <p>暴露 /v1/chat/completions 端点，兼容 OpenAI API 格式。</p>
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAIController {

    private final LLMChatUseCase llmChatUseCase;

    /**
     * Chat Completions 端点
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(
            @RequestBody OpenAIChatRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("apiKeyId") Long apiKeyId,
            HttpServletResponse response) throws IOException {

        log.info("OpenAI chat request: model={}, userId={}, stream={}",
                request.getModel(), userId, request.getStream());

        validateRequest(request);

        if (Boolean.TRUE.equals(request.getStream())) {
            return chatCompletionsStream(request, userId, apiKeyId, response);
        } else {
            return chatCompletionsNonStream(request, userId, apiKeyId);
        }
    }

    /**
     * 非流式响应
     */
    private ResponseEntity<?> chatCompletionsNonStream(
            OpenAIChatRequest request, Long userId, Long apiKeyId) {

        LLMRequest llmRequest = toLLMRequest(request);

        LLMResponse llmResponse = llmChatUseCase.send(llmRequest, RouteGroup.RoutingStrategy.WEIGHTED);

        return ResponseEntity.ok(toOpenAIChatResponse(llmResponse));
    }

    /**
     * 流式响应
     */
    private ResponseEntity<?> chatCompletionsStream(
            OpenAIChatRequest request, Long userId, Long apiKeyId, HttpServletResponse response) throws IOException {

        LLMRequest llmRequest = toLLMRequest(request);
        llmRequest.setStream(true);

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        SseEmitter emitter = new SseEmitter();
        try {
            llmChatUseCase.sendStream(llmRequest, RouteGroup.RoutingStrategy.WEIGHTED, data -> {
                try {
                    emitter.send(SseEmitter.event().data(data));
                } catch (IOException e) {
                    log.warn("Error sending SSE data: {}", e.getMessage());
                }
            });
            emitter.complete();
        } catch (Exception e) {
            log.error("Error in stream: {}", e.getMessage());
            emitter.completeWithError(e);
        }

        return ResponseEntity.ok().body(emitter);
    }

    private void validateRequest(OpenAIChatRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new IllegalArgumentException("model is required");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("messages is required");
        }
    }

    private LLMRequest toLLMRequest(OpenAIChatRequest request) {
        List<LLMRequest.Message> messages = request.getMessages().stream()
                .map(this::toLLMMessage)
                .toList();

        return LLMRequest.builder()
                .model(request.getModel())
                .messages(messages)
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .stop(request.getStop())
                .frequencyPenalty(request.getFrequencyPenalty())
                .presencePenalty(request.getPresencePenalty())
                .seed(request.getSeed())
                .toolChoice(request.getToolChoice())
                .stream(request.getStream() != null && request.getStream())
                .build();
    }

    private LLMRequest.Message toLLMMessage(OpenAIChatRequest.Message msg) {
        List<LLMRequest.ToolCall> toolCalls = null;
        if (msg.getToolCalls() != null) {
            toolCalls = msg.getToolCalls().stream()
                    .map(tc -> LLMRequest.ToolCall.builder()
                            .id(tc.getId())
                            .type(tc.getType())
                            .function(LLMRequest.FunctionCall.builder()
                                    .name(tc.getFunction().getName())
                                    .arguments(tc.getFunction().getArguments() instanceof String
                                            ? (String) tc.getFunction().getArguments()
                                            : tc.getFunction().getArguments().toString())
                                    .build())
                            .build())
                    .toList();
        }

        return LLMRequest.Message.builder()
                .role(msg.getRole())
                .content(msg.getContent())
                .toolCalls(toolCalls)
                .toolCallId(msg.getToolCallId())
                .name(msg.getName())
                .build();
    }

    private Object toOpenAIChatResponse(LLMResponse response) {
        if (response.getError() != null) {
            return ResponseEntity.badRequest().body(
                    OpenAIChatResponse.builder()
                            .error(OpenAIChatResponse.Error.builder()
                                    .message(response.getError().getMessage())
                                    .type(response.getError().getType())
                                    .code(response.getError().getCode())
                                    .build())
                            .build());
        }

        List<OpenAIChatResponse.Choice> choices = null;
        if (response.getContent() != null) {
            List<OpenAIChatResponse.ToolCall> toolCalls = null;
            if (response.getContent().getToolCalls() != null) {
                toolCalls = response.getContent().getToolCalls().stream()
                        .map(tc -> OpenAIChatResponse.ToolCall.builder()
                                .id(tc.getId())
                                .type(tc.getType())
                                .function(OpenAIChatResponse.FunctionCall.builder()
                                        .name(tc.getFunction().getName())
                                        .arguments(tc.getFunction().getArguments())
                                        .build())
                                .build())
                        .toList();
            }

            choices = List.of(OpenAIChatResponse.Choice.builder()
                    .index(0)
                    .message(OpenAIChatResponse.Message.builder()
                            .role(response.getContent().getRole())
                            .content(response.getContent().getText())
                            .toolCalls(toolCalls)
                            .build())
                    .finishReason(response.getFinishReason())
                    .build());
        }

        OpenAIChatResponse.Usage usage = null;
        if (response.getUsage() != null) {
            usage = OpenAIChatResponse.Usage.builder()
                    .promptTokens(response.getUsage().getPromptTokens())
                    .completionTokens(response.getUsage().getCompletionTokens())
                    .totalTokens(response.getUsage().getTotalTokens())
                    .build();
        }

        return OpenAIChatResponse.builder()
                .id(response.getId())
                .model(response.getModel())
                .created(response.getCreated())
                .choices(choices)
                .usage(usage)
                .build();
    }
}
