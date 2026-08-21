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
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.proxy.ChatDispatchService;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.provider.upstream.RoutingStrategy;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SSE 流式响应处理工具
 *
 * <p>提取公共的 SSE 流式写入逻辑，避免 Controller 代码重复。
 * 完成标记由调度服务根据协议类型注入，此处仅做 flush 和 latch 释放。</p>
 */
@Slf4j
public final class SseStreamHelper {

    private static final long STREAM_TIMEOUT_SECONDS = 120;

    private SseStreamHelper() {}

    /**
     * 执行流式调度请求并写入 SSE 响应
     *
     * @param dispatchService  调度服务
     * @param protocolRequest  协议请求
     * @param identity          认证结果
     * @param response         HTTP 响应
     */
    public static void executeStream(ChatDispatchService dispatchService, ProtocolRequest protocolRequest,
                                     Identity identity, HttpServletResponse response) throws IOException {
        setupSseResponse(response);

        PrintWriter writer = response.getWriter();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        dispatchService.dispatchStream(protocolRequest, identity, RoutingStrategy.WEIGHTED, new StreamCallback() {
            @Override
            public void onChunk(String data) {
                writeChunk(writer, data, errorRef, latch);
            }

            @Override
            public void onComplete() {
                completeStream(writer, latch);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Stream error: {}", t.getMessage());
                errorRef.set(t);
                latch.countDown();
            }
        });

        awaitCompletion(latch, errorRef);
    }

    private static void setupSseResponse(HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private static void writeChunk(PrintWriter writer, String data,
                                   AtomicReference<Throwable> errorRef, CountDownLatch latch) {
        try {
            writer.write("data: " + data + "\n\n");
            writer.flush();
        } catch (Exception e) {
            log.warn("Error writing SSE chunk: {}", e.getMessage());
            errorRef.set(e);
            latch.countDown();
        }
    }

    private static void completeStream(PrintWriter writer, CountDownLatch latch) {
        try {
            writer.flush();
        } catch (Exception e) {
            log.warn("Error flushing SSE stream: {}", e.getMessage());
        }
        latch.countDown();
    }

    private static void awaitCompletion(CountDownLatch latch, AtomicReference<Throwable> errorRef) {
        try {
            latch.await(STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Stream interrupted");
        }

        if (errorRef.get() != null) {
            log.error("Stream failed: {}", errorRef.get().getMessage());
        }
    }
}