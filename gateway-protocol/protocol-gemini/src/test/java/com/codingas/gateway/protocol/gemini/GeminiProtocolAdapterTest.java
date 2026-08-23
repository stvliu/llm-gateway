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
}
