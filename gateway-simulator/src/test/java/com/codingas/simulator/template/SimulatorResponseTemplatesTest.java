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
package com.codingas.simulator.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimulatorResponseTemplates 单元测试。
 * <p>
 * 验证模拟响应模板工厂生成的 JSON 格式正确，包含必要的协议字段。
 */
class SimulatorResponseTemplatesTest {

    // ==================== OpenAI 模板测试 ====================

    @Nested
    @DisplayName("OpenAI 协议模板")
    class OpenAITemplates {

        @Test
        @DisplayName("openaiChatCompletion 返回合法的 Chat Completion JSON")
        void openaiChatCompletion_returnsValidJson() {
            String json = SimulatorResponseTemplates.openaiChatCompletion();

            assertAll(
                    () -> assertNotNull(json, "响应不应为 null"),
                    () -> assertTrue(json.contains("\"chat.completion\""), "应包含 object 字段"),
                    () -> assertTrue(json.contains("\"choices\""), "应包含 choices 字段"),
                    () -> assertTrue(json.contains("\"usage\""), "应包含 usage 字段"),
                    () -> assertTrue(json.contains("\"stop\""), "应包含 finish_reason: stop")
            );
        }

        @Test
        @DisplayName("openaiStreamChunk 返回包含指定内容的 SSE chunk")
        void openaiStreamChunk_containsContent() {
            String chunk = SimulatorResponseTemplates.openaiStreamChunk("Hello");

            assertAll(
                    () -> assertNotNull(chunk, "chunk 不应为 null"),
                    () -> assertTrue(chunk.contains("\"chat.completion.chunk\""), "应包含 chunk object 类型"),
                    () -> assertTrue(chunk.contains("Hello"), "应包含指定的内容")
            );
        }

        @Test
        @DisplayName("openaiStreamDone 返回 [DONE] 结束标记")
        void openaiStreamDone_returnsDoneMarker() {
            String done = SimulatorResponseTemplates.openaiStreamDone();

            assertEquals("data: [DONE]\n\n", done, "应返回 data: [DONE] 结束标记");
        }

        @Test
        @DisplayName("openaiRateLimitError 返回 429 错误 JSON")
        void openaiRateLimitError_returns429Error() {
            String error = SimulatorResponseTemplates.openaiRateLimitError();

            assertAll(
                    () -> assertTrue(error.contains("\"rate_limit_error\""), "应包含 rate_limit_error 类型"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("openaiServerError 返回 500 错误 JSON")
        void openaiServerError_returns500Error() {
            String error = SimulatorResponseTemplates.openaiServerError();

            assertAll(
                    () -> assertTrue(error.contains("\"server_error\""), "应包含 server_error 类型"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }
    }

    // ==================== Anthropic 模板测试 ====================

    @Nested
    @DisplayName("Anthropic 协议模板")
    class AnthropicTemplates {

        @Test
        @DisplayName("anthropicMessages 返回合法的 Messages JSON")
        void anthropicMessages_returnsValidJson() {
            String json = SimulatorResponseTemplates.anthropicMessages();

            assertAll(
                    () -> assertNotNull(json, "响应不应为 null"),
                    () -> assertTrue(json.contains("\"message\""), "应包含 type: message"),
                    () -> assertTrue(json.contains("\"assistant\""), "应包含 role: assistant"),
                    () -> assertTrue(json.contains("\"content\""), "应包含 content 字段"),
                    () -> assertTrue(json.contains("\"end_turn\""), "应包含 stop_reason: end_turn")
            );
        }

        @Test
        @DisplayName("anthropicStreamDelta 返回包含指定内容的 content_block_delta")
        void anthropicStreamDelta_containsText() {
            String delta = SimulatorResponseTemplates.anthropicStreamDelta("World");

            assertAll(
                    () -> assertNotNull(delta, "delta 不应为 null"),
                    () -> assertTrue(delta.contains("\"content_block_delta\""), "应包含 content_block_delta 类型"),
                    () -> assertTrue(delta.contains("World"), "应包含指定的文本内容")
            );
        }

        @Test
        @DisplayName("anthropicStreamStop 返回 message_stop 事件")
        void anthropicStreamStop_returnsMessageStop() {
            String stop = SimulatorResponseTemplates.anthropicStreamStop();

            assertAll(
                    () -> assertTrue(stop.contains("event: message_stop"), "应包含 event: message_stop"),
                    () -> assertTrue(stop.contains("\"message_stop\""), "应包含 type: message_stop")
            );
        }

        @Test
        @DisplayName("anthropicRateLimitError 返回 429 错误 JSON")
        void anthropicRateLimitError_returns429Error() {
            String error = SimulatorResponseTemplates.anthropicRateLimitError();

            assertAll(
                    () -> assertTrue(error.contains("\"rate_limit_error\""), "应包含 rate_limit_error 类型"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("anthropicServerError 返回 500 错误 JSON")
        void anthropicServerError_returns500Error() {
            String error = SimulatorResponseTemplates.anthropicServerError();

            assertAll(
                    () -> assertTrue(error.contains("\"api_error\""), "应包含 api_error 类型"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }
    }

    // ==================== 新增 OpenAI 错误模板测试 ====================

    @Nested
    @DisplayName("OpenAI 新增错误模板")
    class OpenAINewErrorTemplates {

        @Test
        @DisplayName("openaiAuthError 返回 401 错误 JSON")
        void openaiAuthError_returns401Error() {
            String error = SimulatorResponseTemplates.openaiAuthError();
            assertAll(
                    () -> assertTrue(error.contains("\"authentication_error\""), "应包含 authentication_error"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("openaiQuotaExceeded 返回 429 quota 错误 JSON")
        void openaiQuotaExceeded_returns429QuotaError() {
            String error = SimulatorResponseTemplates.openaiQuotaExceeded();
            assertAll(
                    () -> assertTrue(error.contains("\"insufficient_quota\""), "应包含 insufficient_quota"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("openaiInvalidRequest 返回 400 错误 JSON")
        void openaiInvalidRequest_returns400Error() {
            String error = SimulatorResponseTemplates.openaiInvalidRequest();
            assertAll(
                    () -> assertTrue(error.contains("\"invalid_request_error\""), "应包含 invalid_request_error"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("openaiServiceDown 返回 503 错误 JSON")
        void openaiServiceDown_returns503Error() {
            String error = SimulatorResponseTemplates.openaiServiceDown();
            assertAll(
                    () -> assertTrue(error.contains("\"service_unavailable\""), "应包含 service_unavailable"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("openaiTimeoutError 返回 408 错误 JSON")
        void openaiTimeoutError_returns408Error() {
            String error = SimulatorResponseTemplates.openaiTimeoutError();
            assertAll(
                    () -> assertTrue(error.contains("\"timeout\""), "应包含 timeout"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }
    }

    // ==================== 新增 Anthropic 错误模板测试 ====================

    @Nested
    @DisplayName("Anthropic 新增错误模板")
    class AnthropicNewErrorTemplates {

        @Test
        @DisplayName("anthropicAuthError 返回 401 错误 JSON")
        void anthropicAuthError_returns401Error() {
            String error = SimulatorResponseTemplates.anthropicAuthError();
            assertAll(
                    () -> assertTrue(error.contains("\"authentication_error\""), "应包含 authentication_error"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("anthropicQuotaExceeded 返回 429 quota 错误 JSON")
        void anthropicQuotaExceeded_returns429QuotaError() {
            String error = SimulatorResponseTemplates.anthropicQuotaExceeded();
            assertAll(
                    () -> assertTrue(error.contains("\"insufficient_quota\""), "应包含 insufficient_quota"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("anthropicInvalidRequest 返回 400 错误 JSON")
        void anthropicInvalidRequest_returns400Error() {
            String error = SimulatorResponseTemplates.anthropicInvalidRequest();
            assertAll(
                    () -> assertTrue(error.contains("\"invalid_request_error\""), "应包含 invalid_request_error"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("anthropicServiceDown 返回 503 错误 JSON")
        void anthropicServiceDown_returns503Error() {
            String error = SimulatorResponseTemplates.anthropicServiceDown();
            assertAll(
                    () -> assertTrue(error.contains("\"service_unavailable\""), "应包含 service_unavailable"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }

        @Test
        @DisplayName("anthropicTimeoutError 返回 408 错误 JSON")
        void anthropicTimeoutError_returns408Error() {
            String error = SimulatorResponseTemplates.anthropicTimeoutError();
            assertAll(
                    () -> assertTrue(error.contains("\"timeout\""), "应包含 timeout"),
                    () -> assertTrue(error.contains("\"error\""), "应包含 error 对象")
            );
        }
    }
}
