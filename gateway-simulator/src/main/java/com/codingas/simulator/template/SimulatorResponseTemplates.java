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

/**
 * 模拟响应模板工厂，提供 OpenAI 和 Anthropic 协议的预制 JSON 响应模板。
 * <p>
 * 用于 gateway-simulator 独立运行服务，支持非流式、流式和错误响应模板。
 * 所有方法均为静态方法，纯工具类不可实例化。
 */
public final class SimulatorResponseTemplates {

    private SimulatorResponseTemplates() {
        // 工具类禁止实例化
    }

    // ==================== OpenAI 模板 ====================

    /**
     * 返回 OpenAI Chat Completion 非流式响应模板。
     *
     * @return 合法的 OpenAI Chat Completion JSON 字符串
     */
    public static String openaiChatCompletion() {
        return """
                {
                  "id": "chatcmpl-sim-001",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4o",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Hello! How can I help you today?"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 8,
                    "total_tokens": 18
                  }
                }""";
    }

    /**
     * 返回 OpenAI Chat Completion 流式 SSE chunk 模板。
     * <p>
     * 包含指定内容的 delta chunk，格式为 SSE data 行。
     *
     * @param content chunk 中的文本内容
     * @return SSE 格式的单个 chunk 字符串
     */
    public static String openaiStreamChunk(String content) {
        return "data: {\"id\":\"chatcmpl-sim-002\",\"object\":\"chat.completion.chunk\"," +
                "\"created\":1700000000,\"model\":\"gpt-4o\"," +
                "\"choices\":[{\"index\":0,\"delta\":{\"content\":\"" + content + "\"},\"finish_reason\":null}]}\n\n";
    }

    /**
     * 返回 OpenAI 流式响应结束标记。
     *
     * @return SSE 格式的 [DONE] 结束标记
     */
    public static String openaiStreamDone() {
        return "data: [DONE]\n\n";
    }

    /**
     * 返回 OpenAI 429 限流错误响应模板。
     *
     * @return 包含 rate_limit_error 类型的 JSON 字符串
     */
    public static String openaiRateLimitError() {
        return """
                {
                  "error": {
                    "type": "rate_limit_error",
                    "message": "Simulated rate limit error"
                  }
                }""";
    }

    /**
     * 返回 OpenAI 500 服务器错误响应模板。
     *
     * @return 包含 server_error 类型的 JSON 字符串
     */
    public static String openaiServerError() {
        return """
                {
                  "error": {
                    "type": "server_error",
                    "message": "Simulated server error"
                  }
                }""";
    }

    // ==================== Anthropic 模板 ====================

    /**
     * 返回 Anthropic Messages 非流式响应模板。
     *
     * @return 合法的 Anthropic Messages JSON 字符串
     */
    public static String anthropicMessages() {
        return """
                {
                  "id": "msg_sim_001",
                  "type": "message",
                  "role": "assistant",
                  "model": "claude-sonnet-4-20250514",
                  "content": [
                    {
                      "type": "text",
                      "text": "Hello! How can I help you today?"
                    }
                  ],
                  "stop_reason": "end_turn",
                  "stop_sequence": null,
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 8
                  }
                }""";
    }

    /**
     * 返回 Anthropic 流式 content_block_delta 事件模板。
     * <p>
     * 包含指定文本的 delta 事件。
     *
     * @param text delta 中的文本内容
     * @return SSE 格式的 content_block_delta 事件字符串
     */
    public static String anthropicStreamDelta(String text) {
        return "event: content_block_delta\n" +
                "data: {\"type\":\"content_block_delta\",\"index\":0," +
                "\"delta\":{\"type\":\"text_delta\",\"text\":\"" + text + "\"}}\n\n";
    }

    /**
     * 返回 Anthropic 流式响应结束事件模板。
     * <p>
     * 包含 message_stop 事件标记流结束。
     *
     * @return SSE 格式的 message_stop 事件字符串
     */
    public static String anthropicStreamStop() {
        return "event: message_stop\n" +
                "data: {\"type\":\"message_stop\"}\n\n";
    }

    /**
     * 返回 Anthropic 429 限流错误响应模板。
     *
     * @return 包含 rate_limit_error 类型的 JSON 字符串
     */
    public static String anthropicRateLimitError() {
        return """
                {
                  "error": {
                    "type": "rate_limit_error",
                    "message": "Simulated rate limit error"
                  }
                }""";
    }

    /**
     * 返回 Anthropic 500 服务器错误响应模板。
     *
     * @return 包含 api_error 类型的 JSON 字符串
     */
    public static String anthropicServerError() {
        return """
                {
                  "error": {
                    "type": "api_error",
                    "message": "Simulated server error"
                  }
                }""";
    }

    // ==================== 新增 OpenAI 错误模板 ====================

    /**
     * 返回 OpenAI 401 认证错误响应模板。
     *
     * @return 包含 authentication_error 类型的 JSON 字符串
     */
    public static String openaiAuthError() {
        return """
                {
                  "error": {
                    "type": "authentication_error",
                    "message": "Simulated authentication error"
                  }
                }""";
    }

    /**
     * 返回 OpenAI 429 配额超限错误响应模板。
     *
     * @return 包含 insufficient_quota 类型的 JSON 字符串
     */
    public static String openaiQuotaExceeded() {
        return """
                {
                  "error": {
                    "type": "insufficient_quota",
                    "message": "Simulated quota exceeded error"
                  }
                }""";
    }

    /**
     * 返回 OpenAI 400 非法请求错误响应模板。
     *
     * @return 包含 invalid_request_error 类型的 JSON 字符串
     */
    public static String openaiInvalidRequest() {
        return """
                {
                  "error": {
                    "type": "invalid_request_error",
                    "message": "Simulated invalid request error"
                  }
                }""";
    }

    /**
     * 返回 OpenAI 503 服务不可用错误响应模板。
     *
     * @return 包含 service_unavailable 类型的 JSON 字符串
     */
    public static String openaiServiceDown() {
        return """
                {
                  "error": {
                    "type": "service_unavailable",
                    "message": "Simulated service unavailable error"
                  }
                }""";
    }

    /**
     * 返回 OpenAI 408 超时错误响应模板。
     *
     * @return 包含 timeout 类型的 JSON 字符串
     */
    public static String openaiTimeoutError() {
        return """
                {
                  "error": {
                    "type": "timeout",
                    "message": "Simulated timeout error"
                  }
                }""";
    }

    // ==================== 新增 Anthropic 错误模板 ====================

    /**
     * 返回 Anthropic 401 认证错误响应模板。
     *
     * @return 包含 authentication_error 类型的 JSON 字符串
     */
    public static String anthropicAuthError() {
        return """
                {
                  "error": {
                    "type": "authentication_error",
                    "message": "Simulated authentication error"
                  }
                }""";
    }

    /**
     * 返回 Anthropic 429 配额超限错误响应模板。
     *
     * @return 包含 insufficient_quota 类型的 JSON 字符串
     */
    public static String anthropicQuotaExceeded() {
        return """
                {
                  "error": {
                    "type": "insufficient_quota",
                    "message": "Simulated quota exceeded error"
                  }
                }""";
    }

    /**
     * 返回 Anthropic 400 非法请求错误响应模板。
     *
     * @return 包含 invalid_request_error 类型的 JSON 字符串
     */
    public static String anthropicInvalidRequest() {
        return """
                {
                  "error": {
                    "type": "invalid_request_error",
                    "message": "Simulated invalid request error"
                  }
                }""";
    }

    /**
     * 返回 Anthropic 503 服务不可用错误响应模板。
     *
     * @return 包含 service_unavailable 类型的 JSON 字符串
     */
    public static String anthropicServiceDown() {
        return """
                {
                  "error": {
                    "type": "service_unavailable",
                    "message": "Simulated service unavailable error"
                  }
                }""";
    }

    /**
     * 返回 Anthropic 408 超时错误响应模板。
     *
     * @return 包含 timeout 类型的 JSON 字符串
     */
    public static String anthropicTimeoutError() {
        return """
                {
                  "error": {
                    "type": "timeout",
                    "message": "Simulated timeout error"
                  }
                }""";
    }
}
