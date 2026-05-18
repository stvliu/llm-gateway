package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.proxy.dto.OpenAIChatRequest;
import com.codingas.gateway.application.proxy.dto.OpenAIChatResponse;
import com.codingas.gateway.application.proxy.ProxyService;
import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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

    private final ProxyService proxyService;

    /**
     * Chat Completions 端点
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(
            @RequestBody OpenAIChatRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("apiKeyId") Long apiKeyId,
            @RequestAttribute(value = "authResult", required = false) UserAuthResult authResult,
            HttpServletResponse response) throws IOException {

        log.info("OpenAI chat request: model={}, userId={}, stream={}",
                request.getModel(), userId, request.getStream());

        validateRequest(request);

        if (Boolean.TRUE.equals(request.getStream())) {
            chatCompletionsStream(request, authResult, response);
            return null;
        } else {
            return chatCompletionsNonStream(request, authResult);
        }
    }

    /**
     * 非流式响应
     */
    private ResponseEntity<?> chatCompletionsNonStream(
            OpenAIChatRequest request, UserAuthResult authResult) {

        LLMRequest llmRequest = toLLMRequest(request);

        LLMResponse llmResponse;
        if (authResult != null) {
            llmResponse = proxyService.proxy(llmRequest, authResult, RouteGroup.RoutingStrategy.WEIGHTED);
        } else {
            llmResponse = proxyService.proxy(llmRequest, RouteGroup.RoutingStrategy.WEIGHTED);
        }

        // 错误响应直接返回 400
        if (llmResponse.getError() != null) {
            return ResponseEntity.badRequest().body(toOpenAIChatResponse(llmResponse));
        }

        return ResponseEntity.ok(toOpenAIChatResponse(llmResponse));
    }

    /**
     * 流式响应
     *
     * <p>使用同步方式写入 SSE 流，避免异步回调时连接已关闭的问题。</p>
     */
    private void chatCompletionsStream(
            OpenAIChatRequest request, UserAuthResult authResult,
            HttpServletResponse response) throws IOException {

        LLMRequest llmRequest = toLLMRequest(request);
        llmRequest.setStream(true);

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = response.getWriter();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        if (authResult != null) {
            proxyService.proxyStream(llmRequest, authResult, RouteGroup.RoutingStrategy.WEIGHTED, data -> {
                try {
                    writer.write("data: " + data + "\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.error("Error writing stream data: {}", e.getMessage());
                    errorRef.set(e);
                    latch.countDown();
                }
            }, () -> {
                try {
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.error("Error writing stream done: {}", e.getMessage());
                }
                latch.countDown();
            }, error -> {
                log.error("Stream error: {}", error.getMessage());
                errorRef.set(error);
                latch.countDown();
            });
        } else {
            proxyService.proxyStream(llmRequest, RouteGroup.RoutingStrategy.WEIGHTED, data -> {
                try {
                    writer.write("data: " + data + "\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.error("Error writing stream data: {}", e.getMessage());
                    errorRef.set(e);
                    latch.countDown();
                }
            }, () -> {
                try {
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.error("Error writing stream done: {}", e.getMessage());
                }
                latch.countDown();
            }, error -> {
                log.error("Stream error: {}", error.getMessage());
                errorRef.set(error);
                latch.countDown();
            });
        }

        try {
            // 等待流式响应完成（最多等待 120 秒）
            latch.await(120, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Stream interrupted");
        }

        if (errorRef.get() != null) {
            log.error("Stream failed: {}", errorRef.get().getMessage());
        }
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
