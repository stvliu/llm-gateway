package com.codingas.gateway.web.controller;

import com.codingas.gateway.adapter.dto.LLMRequest;
import com.codingas.gateway.adapter.dto.LLMResponse;
import com.codingas.gateway.core.domain.entity.RouteGroup;
import com.codingas.gateway.dispatch.LLMDispatcher;
import com.codingas.gateway.web.dto.OpenAIChatRequest;
import com.codingas.gateway.web.dto.OpenAIChatResponse;
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
 * OpenAI 兼容 API 控制器
 *
 * <p>暴露 /v1/chat/completions 端点，兼容 OpenAI API 格式。</p>
 *
 * @see <a href="https://platform.openai.com/docs/api-reference/chat">OpenAI Chat API</a>
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAIController {

    private final LLMDispatcher dispatcher;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Chat Completions 端点
     *
     * <p>支持非流式和流式两种响应模式。</p>
     */
    @PostMapping("/chat/completions")
    public Object chatCompletions(
            @RequestBody OpenAIChatRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("apiKeyId") Long apiKeyId) {

        log.info("OpenAI chat request: model={}, userId={}, stream={}",
                request.getModel(), userId, request.getStream());

        // 验证请求
        validateRequest(request);

        // 判断流式或非流式
        if (Boolean.TRUE.equals(request.getStream())) {
            return chatCompletionsStream(request, userId, apiKeyId);
        } else {
            return chatCompletionsNonStream(request, userId, apiKeyId);
        }
    }

    /**
     * 非流式响应
     */
    private OpenAIChatResponse chatCompletionsNonStream(
            OpenAIChatRequest request, Long userId, Long apiKeyId) {

        // 转换为内部请求格式
        LLMRequest llmRequest = toLLMRequest(request);

        // 调用调度器
        LLMResponse response = dispatcher.send(llmRequest, RouteGroup.RoutingStrategy.PRIORITY);

        // 转换为 OpenAI 响应格式
        return toOpenAIChatResponse(response);
    }

    /**
     * 流式响应
     */
    private SseEmitter chatCompletionsStream(
            OpenAIChatRequest request, Long userId, Long apiKeyId) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.submit(() -> {
            try {
                // 转换为内部请求格式
                LLMRequest llmRequest = toLLMRequest(request);
                llmRequest.setStream(true);

                // 调用调度器 (流式)
                dispatcher.sendStream(llmRequest, RouteGroup.RoutingStrategy.PRIORITY,
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
    private void validateRequest(OpenAIChatRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new IllegalArgumentException("model is required");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("messages is required");
        }
    }

    /**
     * 转换为内部 LLMRequest
     */
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

    /**
     * 转换为 OpenAI 响应格式
     */
    private OpenAIChatResponse toOpenAIChatResponse(LLMResponse response) {
        if (response.getError() != null) {
            return OpenAIChatResponse.builder()
                    .error(OpenAIChatResponse.Error.builder()
                            .message(response.getError().getMessage())
                            .type(response.getError().getType())
                            .code(response.getError().getCode())
                            .build())
                    .build();
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
