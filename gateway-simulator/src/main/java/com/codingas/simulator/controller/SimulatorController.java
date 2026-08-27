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
package com.codingas.simulator.controller;

import com.codingas.simulator.service.BehaviorSequence;
import com.codingas.simulator.service.SimulatorModeService;
import com.codingas.simulator.template.SimulatorResponseTemplates;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 模拟端点 Controller，模拟 OpenAI 和 Anthropic 的 API 行为。
 * <p>
 * 根据当前模式返回不同类型的响应：
 * <ul>
 *   <li>NORMAL — 返回成功响应</li>
 *   <li>AUTH_ERROR — 返回 401 认证错误</li>
 *   <li>RATE_LIMITED — 返回 429 限流错误</li>
 *   <li>QUOTA_EXCEEDED — 返回 429 配额超限错误</li>
 *   <li>INVALID_REQUEST — 返回 400 非法请求错误</li>
 *   <li>UPSTREAM_ERROR — 返回 500 服务器错误</li>
 *   <li>SERVICE_DOWN — 返回 503 服务不可用</li>
 *   <li>TIMEOUT — 返回 408 超时错误</li>
 *   <li>INTERMITTENT — 委托给 BehaviorSequence</li>
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
     * 支持 stream=true 时的 SSE 流式响应，根据 streamConfig 控制发送行为。
     *
     * @param body       请求体 JSON 字符串
     * @param authHeader Authorization 请求头（可选，用于 API Key 覆盖）
     * @return 根据模式返回正常、错误或流式响应
     */
    @PostMapping("/v1/chat/completions")
    public ResponseEntity<?> openaiChatCompletions(@RequestBody String body,
                                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        modeService.recordRequest("POST", "/v1/chat/completions");
        SimulatorModeService.SimulatorMode mode = resolveMode(authHeader);
        modeService.getDelayConfig().applyDelay();

        return switch (mode) {
            case RATE_LIMITED -> ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiRateLimitError());
            case UPSTREAM_ERROR -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiServerError());
            case AUTH_ERROR -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiAuthError());
            case QUOTA_EXCEEDED -> ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiQuotaExceeded());
            case INVALID_REQUEST -> ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiInvalidRequest());
            case SERVICE_DOWN -> ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiServiceDown());
            case TIMEOUT -> ResponseEntity
                    .status(HttpStatus.REQUEST_TIMEOUT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiTimeoutError());
            default -> handleOpenAINormal(body);
        };
    }

    /**
     * 模拟 Anthropic Messages 端点。
     *
     * @param body       请求体 JSON 字符串
     * @param authHeader Authorization 请求头（可选）
     * @return 根据模式返回正常或错误响应
     */
    @PostMapping("/v1/messages")
    public ResponseEntity<?> anthropicMessages(@RequestBody String body,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        modeService.recordRequest("POST", "/v1/messages");
        SimulatorModeService.SimulatorMode mode = resolveMode(authHeader);
        modeService.getDelayConfig().applyDelay();

        return switch (mode) {
            case RATE_LIMITED -> ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicRateLimitError());
            case UPSTREAM_ERROR -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicServerError());
            case AUTH_ERROR -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicAuthError());
            case QUOTA_EXCEEDED -> ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicQuotaExceeded());
            case INVALID_REQUEST -> ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicInvalidRequest());
            case SERVICE_DOWN -> ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicServiceDown());
            case TIMEOUT -> ResponseEntity
                    .status(HttpStatus.REQUEST_TIMEOUT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicTimeoutError());
            default -> handleAnthropicNormal(body);
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
     * 根据优先级解析当前模式：行为序列 > API Key 覆盖 > 全局模式。
     *
     * @param authHeader Authorization 请求头
     * @return 解析后的 SimulatorMode
     */
    private SimulatorModeService.SimulatorMode resolveMode(String authHeader) {
        // 1. 行为序列优先
        BehaviorSequence seq = modeService.getBehaviorSequence();
        if (seq != null && seq.isActive()) {
            Optional<SimulatorModeService.SimulatorMode> seqMode = seq.consume();
            if (seqMode.isPresent()) {
                return seqMode.get();
            }
        }
        // 2. API Key 覆盖
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String apiKey = authHeader.substring(7);
            Optional<SimulatorModeService.SimulatorMode> overrideMode =
                    modeService.getApiKeyOverrideConfig().matchOverride(apiKey);
            if (overrideMode.isPresent()) {
                return overrideMode.get();
            }
        }
        // 3. 全局模式
        SimulatorModeService.SimulatorMode globalMode = modeService.getMode();
        if (globalMode == SimulatorModeService.SimulatorMode.INTERMITTENT) {
            return SimulatorModeService.SimulatorMode.NORMAL;
        }
        return globalMode;
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
     * 根据 streamConfig 控制发送行为：支持中断（interruptAfter）、自定义 chunk 数量等。
     */
    private void sendOpenAIStream(SseEmitter emitter) {
        try {
            var config = modeService.getStreamConfig();
            int chunkCount = config.getChunkCount();
            int interruptAfter = config.getInterruptAfter();
            long intervalMs = config.getChunkIntervalMs();
            String action = config.getAction();

            // 发送内容 chunk
            for (int i = 0; i < chunkCount; i++) {
                emitter.send(SseEmitter.event()
                        .data(SimulatorResponseTemplates.openaiStreamChunk("chunk-" + i)));
                Thread.sleep(intervalMs);

                // 检查是否需要中断
                if (interruptAfter > 0 && i >= interruptAfter - 1) {
                    emitter.completeWithError(new RuntimeException("simulator interrupt"));
                    return;
                }
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
     * 根据 streamConfig 控制发送行为：支持中断（interruptAfter）、自定义 chunk 数量等。
     */
    private void sendAnthropicStream(SseEmitter emitter) {
        try {
            var config = modeService.getStreamConfig();
            int chunkCount = config.getChunkCount();
            int interruptAfter = config.getInterruptAfter();
            long intervalMs = config.getChunkIntervalMs();

            // 发送 delta 事件
            for (int i = 0; i < chunkCount; i++) {
                emitter.send(SseEmitter.event()
                        .data(SimulatorResponseTemplates.anthropicStreamDelta("chunk-" + i)));
                Thread.sleep(intervalMs);

                // 检查是否需要中断
                if (interruptAfter > 0 && i >= interruptAfter - 1) {
                    emitter.completeWithError(new RuntimeException("simulator interrupt"));
                    return;
                }
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
