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
package com.codingas.gateway.protocol.gemini;

import com.codingas.gateway.protocol.canonical.CanonicalChatRequest;
import com.codingas.gateway.protocol.canonical.CanonicalChatResponse;
import com.codingas.gateway.protocol.canonical.CanonicalUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gemini 协议示例插件适配器测试。
 */
@DisplayName("GeminiProtocolAdapter")
class GeminiProtocolAdapterTest {

    private final GeminiProtocolAdapter adapter = new GeminiProtocolAdapter();

    @Test
    @DisplayName("protocol 标识应为 gemini")
    void protocol_isGemini() {
        assertThat(adapter.protocol()).isEqualTo("gemini");
    }

    @Test
    @DisplayName("GeminiChatRequest 契约标识协议为 gemini")
    void request_protocolIsGemini() {
        assertThat(new GeminiChatRequest().getProtocol()).isEqualTo("gemini");
    }

    @Test
    @DisplayName("normalizeRequest 应映射到规范模型")
    void normalizeRequest_mapsToCanonical() {
        GeminiChatRequest req = new GeminiChatRequest();
        req.setModel("gemini-1.5-pro");
        req.setSystem("s");
        req.addMessage(new GeminiChatRequest.Message("user", "hi"));

        CanonicalChatRequest canonical = adapter.normalizeRequest(req);

        assertThat(canonical.getModel()).isEqualTo("gemini-1.5-pro");
        assertThat(canonical.getSystem()).isEqualTo("s");
        assertThat(canonical.getMessages()).hasSize(1);
        assertThat(canonical.getMessages().get(0).getContent()).isEqualTo("hi");
    }

    @Test
    @DisplayName("denormalizeRequest 应还原 gemini 请求")
    void denormalizeRequest_restoresRequest() {
        GeminiChatRequest out = adapter.denormalizeRequest(CanonicalChatRequest.builder()
                .model("gemini-1.5-pro")
                .system("sys")
                .messages(java.util.List.of(com.codingas.gateway.protocol.canonical.CanonicalMessage.builder()
                        .role("user").content("q").build()))
                .build());

        assertThat(out.getProtocol()).isEqualTo("gemini");
        assertThat(out.getSystem()).isEqualTo("sys");
        assertThat(out.getMessages()).hasSize(1);
        assertThat(out.getMessages().get(0).content()).isEqualTo("q");
    }

    @Test
    @DisplayName("normalizeRequest 应映射 maxTokens/temperature/stream")
    void normalizeRequest_mapsTuningFields() {
        GeminiChatRequest req = new GeminiChatRequest();
        req.setModel("gemini-1.5-pro");
        req.setMaxTokens(2048);
        req.setTemperature(0.7);
        req.setStream(true);
        req.addMessage(new GeminiChatRequest.Message("user", "hi"));

        CanonicalChatRequest canonical = adapter.normalizeRequest(req);

        assertThat(canonical.getMaxTokens()).isEqualTo(2048);
        assertThat(canonical.getTemperature()).isEqualTo(0.7);
        assertThat(canonical.isStream()).isTrue();
    }

    @Test
    @DisplayName("denormalizeRequest 无 messages 时返回空列表")
    void denormalizeRequest_nullMessages_returnsEmpty() {
        GeminiChatRequest out = adapter.denormalizeRequest(CanonicalChatRequest.builder()
                .model("gemini-1.5-pro")
                .build());

        assertThat(out.getMessages()).isEmpty();
        assertThat(out.getModel()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    @DisplayName("normalizeResponse 应映射文本与 token 用量")
    void normalizeResponse_mapsTextAndUsage() {
        GeminiChatResponse resp = new GeminiChatResponse("r1", "gemini-1.5-pro", "你好", 10, 5);

        CanonicalChatResponse canonical = adapter.normalizeResponse(resp);

        assertThat(canonical.getId()).isEqualTo("r1");
        assertThat(canonical.getModel()).isEqualTo("gemini-1.5-pro");
        assertThat(canonical.getContent()).hasSize(1);
        assertThat(canonical.getContent().get(0).getType()).isEqualTo("text");
        assertThat(canonical.getContent().get(0).getText()).isEqualTo("你好");
        assertThat(canonical.getUsage().getInputTokens()).isEqualTo(10);
        assertThat(canonical.getUsage().getOutputTokens()).isEqualTo(5);
    }

    @Test
    @DisplayName("normalizeResponse 文本为空时返回空内容块")
    void normalizeResponse_nullText_returnsEmptyContent() {
        GeminiChatResponse resp = new GeminiChatResponse("r1", "gemini-1.5-pro", null, null, null);

        CanonicalChatResponse canonical = adapter.normalizeResponse(resp);

        assertThat(canonical.getContent()).isEmpty();
        assertThat(canonical.getUsage()).isNull();
    }

    @Test
    @DisplayName("denormalizeResponse 应拼接文本块与 token 用量")
    void denormalizeResponse_concatenatesTextAndUsage() {
        CanonicalChatResponse canonical = CanonicalChatResponse.builder()
                .id("r1")
                .model("gemini-1.5-pro")
                .content(java.util.List.of(
                        com.codingas.gateway.protocol.canonical.CanonicalContentBlock.builder()
                                .type("text").text("你好").build(),
                        com.codingas.gateway.protocol.canonical.CanonicalContentBlock.builder()
                                .type("toolUse")
                                .toolUse(com.codingas.gateway.protocol.canonical.CanonicalToolCall.builder()
                                        .id("t1").name("f").build())
                                .build()))
                .usage(CanonicalUsage.builder().inputTokens(10).outputTokens(5).build())
                .build();

        GeminiChatResponse out = (GeminiChatResponse) adapter.denormalizeResponse(canonical);

        assertThat(out.id()).isEqualTo("r1");
        assertThat(out.model()).isEqualTo("gemini-1.5-pro");
        // 仅拼接 text 类型块，toolUse 块忽略
        assertThat(out.text()).isEqualTo("你好");
        assertThat(out.inputTokens()).isEqualTo(10);
        assertThat(out.outputTokens()).isEqualTo(5);
    }

    @Test
    @DisplayName("denormalizeResponse 无内容无用量时返回空")
    void denormalizeResponse_empty_returnsBlank() {
        GeminiChatResponse out = (GeminiChatResponse) adapter.denormalizeResponse(
                CanonicalChatResponse.builder().id("r1").model("m").build());

        assertThat(out.text()).isEmpty();
        assertThat(out.inputTokens()).isNull();
        assertThat(out.outputTokens()).isNull();
    }
}
