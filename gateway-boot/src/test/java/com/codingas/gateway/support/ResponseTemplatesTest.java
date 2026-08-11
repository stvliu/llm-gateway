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
package com.codingas.gateway.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResponseTemplates 测试
 */
@DisplayName("ResponseTemplates 测试")
class ResponseTemplatesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    @DisplayName("OpenAI 模板")
    class OpenAITemplates {

        @Test
        @DisplayName("openaiChatCompletion 返回合法 JSON")
        void openaiChatCompletion_validJson() throws Exception {
            String json = ResponseTemplates.openaiChatCompletion();
            var node = mapper.readTree(json);
            assertThat(node.has("id")).isTrue();
            assertThat(node.has("model")).isTrue();
            assertThat(node.has("choices")).isTrue();
            assertThat(node.has("usage")).isTrue();
            assertThat(node.get("choices").size()).isGreaterThan(0);
        }

        @Test
        @DisplayName("openaiStreamChunks 包含 data 行和 [DONE]")
        void openaiStreamChunks_hasDataAndDone() {
            String sse = ResponseTemplates.openaiStreamChunks();
            assertThat(sse).contains("data: ");
            assertThat(sse).contains("data: [DONE]");
        }

        @Test
        @DisplayName("openaiError 返回含 error.type 的 JSON")
        void openaiError_validJson() throws Exception {
            String json = ResponseTemplates.openaiError(429);
            var node = mapper.readTree(json);
            assertThat(node.has("error")).isTrue();
            assertThat(node.get("error").has("type")).isTrue();
            assertThat(node.get("error").has("message")).isTrue();
        }
    }

    @Nested
    @DisplayName("Anthropic 模板")
    class AnthropicTemplates {

        @Test
        @DisplayName("anthropicMessages 返回合法 JSON")
        void anthropicMessages_validJson() throws Exception {
            String json = ResponseTemplates.anthropicMessages();
            var node = mapper.readTree(json);
            assertThat(node.has("id")).isTrue();
            assertThat(node.has("model")).isTrue();
            assertThat(node.has("content")).isTrue();
            assertThat(node.has("usage")).isTrue();
        }

        @Test
        @DisplayName("anthropicStreamChunks 包含 event 和 data 行及 message_stop")
        void anthropicStreamChunks_hasEventAndDataAndStop() {
            String sse = ResponseTemplates.anthropicStreamChunks();
            assertThat(sse).contains("event: ");
            assertThat(sse).contains("data: ");
            assertThat(sse).contains("event: message_stop");
        }

        @Test
        @DisplayName("anthropicError 返回含 error.type 的 JSON")
        void anthropicError_validJson() throws Exception {
            String json = ResponseTemplates.anthropicError(429);
            var node = mapper.readTree(json);
            assertThat(node.has("error")).isTrue();
            assertThat(node.get("error").has("type")).isTrue();
        }
    }
}
