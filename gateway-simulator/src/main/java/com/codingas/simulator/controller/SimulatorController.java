package com.codingas.simulator.controller;

import com.codingas.simulator.service.SimulatorModeService;
import com.codingas.simulator.template.SimulatorResponseTemplates;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 模拟端点 Controller，模拟 OpenAI 和 Anthropic 的 API 行为。
 * <p>
 * 根据当前模式返回不同类型的响应：
 * <ul>
 *   <li>NORMAL — 返回成功响应</li>
 *   <li>RATE_LIMITED — 返回 429 限流错误</li>
 *   <li>FAULT — 返回 500 服务器错误</li>
 * </ul>
 * 支持 stream=true 时的 SSE 流式响应。
 */
@RestController
public class SimulatorController {

    private final SimulatorModeService modeService;

    /** 用于异步发送 SSE 事件的线程池（daemon 线程，应用关闭时自动退出） */
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public SimulatorController(SimulatorModeService modeService) {
        this.modeService = modeService;
    }

    /**
     * 模拟 OpenAI Chat Completion 端点。
     * <p>
     * 支持 stream=true 时的 SSE 流式响应，发送 3 个 chunk + [DONE] 结束标记。
     *
     * @param body 请求体 JSON 字符串
     * @return 根据模式返回正常、限流或故障响应
     */
    @PostMapping("/v1/chat/completions")
    public ResponseEntity<?> openaiChatCompletions(@RequestBody String body) {
        modeService.recordRequest("POST", "/v1/chat/completions");

        return switch (modeService.getMode()) {
            case RATE_LIMITED -> ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiRateLimitError());
            case FAULT -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiServerError());
            case NORMAL -> handleOpenAINormal(body);
        };
    }

    /**
     * 模拟 Anthropic Messages 端点。
     * <p>
     * 支持 stream=true 时的 SSE 流式响应，发送 3 个 delta + message_stop 结束标记。
     *
     * @param body 请求体 JSON 字符串
     * @return 根据模式返回正常、限流或故障响应
     */
    @PostMapping("/v1/messages")
    public ResponseEntity<?> anthropicMessages(@RequestBody String body) {
        modeService.recordRequest("POST", "/v1/messages");

        return switch (modeService.getMode()) {
            case RATE_LIMITED -> ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicRateLimitError());
            case FAULT -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicServerError());
            case NORMAL -> handleAnthropicNormal(body);
        };
    }

    /**
     * 处理 OpenAI 正常模式响应。
     * <p>
     * 如果请求体包含 stream=true，返回 SSE 流式响应；
     * 否则返回标准 Chat Completion JSON。
     */
    private ResponseEntity<?> handleOpenAINormal(String body) {
        if (isStreamRequest(body)) {
            SseEmitter emitter = createSseEmitter();
            sseExecutor.execute(() -> sendOpenAIStream(emitter));
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.openaiChatCompletion());
    }

    /**
     * 处理 Anthropic 正常模式响应。
     * <p>
     * 如果请求体包含 stream=true，返回 SSE 流式响应；
     * 否则返回标准 Messages JSON。
     */
    private ResponseEntity<?> handleAnthropicNormal(String body) {
        if (isStreamRequest(body)) {
            SseEmitter emitter = createSseEmitter();
            sseExecutor.execute(() -> sendAnthropicStream(emitter));
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.anthropicMessages());
    }

    /**
     * 创建 SseEmitter 实例，设置超时为 30 秒。
     */
    private SseEmitter createSseEmitter() {
        SseEmitter emitter = new SseEmitter(30_000L);
        emitter.onCompletion(() -> {});
        emitter.onTimeout(() -> {});
        return emitter;
    }

    /**
     * 异步发送 OpenAI 流式响应。
     * <p>
     * 发送 3 个 data chunk + data: [DONE] 结束标记。
     */
    private void sendOpenAIStream(SseEmitter emitter) {
        try {
            // 发送 3 个内容 chunk
            String[] contents = {"Hello", " from", " simulator"};
            for (String content : contents) {
                emitter.send(SseEmitter.event()
                        .data(SimulatorResponseTemplates.openaiStreamChunk(content)));
                Thread.sleep(50);
            }
            // 发送结束标记
            emitter.send(SseEmitter.event()
                    .data(SimulatorResponseTemplates.openaiStreamDone()));
            emitter.complete();
        } catch (IOException | InterruptedException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * 异步发送 Anthropic 流式响应。
     * <p>
     * 发送 3 个 content_block_delta 事件 + message_stop 结束事件。
     */
    private void sendAnthropicStream(SseEmitter emitter) {
        try {
            // 发送 3 个 delta 事件
            String[] texts = {"Hello", " from", " simulator"};
            for (String text : texts) {
                emitter.send(SseEmitter.event()
                        .data(SimulatorResponseTemplates.anthropicStreamDelta(text)));
                Thread.sleep(50);
            }
            // 发送结束事件
            emitter.send(SseEmitter.event()
                    .data(SimulatorResponseTemplates.anthropicStreamStop()));
            emitter.complete();
        } catch (IOException | InterruptedException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * 检测请求体是否包含 stream=true。
     * <p>
     * 使用精确匹配避免误判（如 content 中含 "true" 字符串）。
     *
     * @param body 请求体 JSON 字符串
     * @return 如果包含 stream:true 则返回 true
     */
    private boolean isStreamRequest(String body) {
        return body != null
                && (body.contains("\"stream\":true") || body.contains("\"stream\": true"));
    }
}
