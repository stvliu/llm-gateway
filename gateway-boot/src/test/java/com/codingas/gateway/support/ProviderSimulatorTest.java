/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.support;

import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.infrastructure.supply.upstream.AnthropicUpstreamClient;
import com.codingas.gateway.infrastructure.supply.upstream.OpenAIUpstreamClient;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * ProviderSimulator 测试 — 验证 MockWebServer 封装行为
 */
class ProviderSimulatorTest {

    // ==================== 基础生命周期 ====================

    @Test
    void create_启动模拟服务器_getUrl返回有效URL() throws IOException {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            String url = sim.getUrl();
            assertThat(url).isNotBlank();
            assertThat(url).startsWith("http://");
        }
    }

    @Test
    void close_关闭后不再接受连接() throws IOException {
        ProviderSimulator sim = ProviderSimulator.create();
        String url = sim.getUrl();
        sim.close();

        // 关闭后再创建客户端连接应该失败
        OpenAIUpstreamClient client = new OpenAIUpstreamClient(
                new OkHttpClient(), url, "key", 2,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules(),
                new com.codingas.gateway.infrastructure.upstream.OpenAIErrorClassifier()
        );

        OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(java.util.List.of(
                        OpenAIChatRequest.Message.builder().role("user").content("hello").build()
                ))
                .build();

        assertThatThrownBy(() -> client.chat(request))
                .isInstanceOf(ProviderException.class);
    }

    // ==================== OpenAI 客户端 ====================

    @Test
    void enqueueOpenAISuccess_createOpenAIIClient_客户端请求得到200响应() throws Exception {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueOpenAISuccess();

            OpenAIUpstreamClient client = sim.createOpenAIIClient("test-key", 10);
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(java.util.List.of(
                            OpenAIChatRequest.Message.builder().role("user").content("hello").build()
                    ))
                    .build();

            OpenAIChatResponse response = (OpenAIChatResponse) client.chat(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo("chatcmpl-test-001");

            // 验证请求被正确录制
            RecordedRequest recorded = sim.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
            assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer test-key");
        }
    }

    // ==================== Anthropic 客户端 ====================

    @Test
    void enqueueAnthropicSuccess_createAnthropicClient_客户端请求得到200响应() throws Exception {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueAnthropicSuccess();

            AnthropicUpstreamClient client = sim.createAnthropicClient("test-key", 10);
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                    .model("claude-sonnet-4-20250514")
                    .maxTokens(100)
                    .messages(java.util.List.of(
                            AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()
                    ))
                    .build();

            AnthropicMessagesResponse response = (AnthropicMessagesResponse) client.chat(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo("msg_test_001");

            // 验证请求被正确录制
            RecordedRequest recorded = sim.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/v1/messages");
            assertThat(recorded.getHeader("x-api-key")).isEqualTo("test-key");
        }
    }

    // ==================== 错误响应 ====================

    @Test
    void enqueueError_客户端请求得到指定错误码() throws IOException {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueError(429, ResponseTemplates.openaiError(429));

            OpenAIUpstreamClient client = sim.createOpenAIIClient("test-key", 10);
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(java.util.List.of(
                            OpenAIChatRequest.Message.builder().role("user").content("hello").build()
                    ))
                    .build();

            assertThatThrownBy(() -> client.chat(request))
                    .isInstanceOf(ProviderException.class);
        }
    }

    // ==================== 请求录制 ====================

    @Test
    void takeRequest_能录制并返回请求() throws Exception {
        try (ProviderSimulator sim = ProviderSimulator.create()) {
            sim.enqueueOpenAISuccess();

            OpenAIUpstreamClient client = sim.createOpenAIIClient("my-api-key", 10);
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(java.util.List.of(
                            OpenAIChatRequest.Message.builder().role("user").content("test").build()
                    ))
                    .build();

            client.chat(request);

            RecordedRequest recorded = sim.takeRequest();
            assertThat(recorded).isNotNull();
            assertThat(recorded.getMethod()).isEqualTo("POST");
            assertThat(recorded.getHeader("Content-Type")).contains("application/json");
            assertThat(recorded.getBody().readUtf8()).contains("gpt-4o");
        }
    }
}
