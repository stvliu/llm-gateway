package com.codingas.gateway.support;

/**
 * 响应模板工厂，提供 OpenAI 和 Anthropic 协议的预制 JSON 响应模板。
 * <p>
 * 用于模拟上游提供商的响应格式，支持测试和开发场景。
 */
public final class ResponseTemplates {

    private ResponseTemplates() {
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
                  "id": "chatcmpl-test-001",
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
     * 返回 OpenAI Chat Completion 流式 SSE 响应模板。
     * <p>
     * 包含多个 data 行和最终的 data: [DONE] 结束标记。
     *
     * @return SSE 格式的流式响应字符串
     */
    public static String openaiStreamChunks() {
        return """
                data: {"id":"chatcmpl-test-002","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

                data: {"id":"chatcmpl-test-002","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

                data: {"id":"chatcmpl-test-002","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}

                data: {"id":"chatcmpl-test-002","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4o","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

                data: [DONE]
                """;
    }

    /**
     * 返回 OpenAI 错误响应模板。
     *
     * @param statusCode HTTP 状态码
     * @return 包含 error.type 和 error.message 的 JSON 字符串
     */
    public static String openaiError(int statusCode) {
        String type = switch (statusCode) {
            case 400 -> "invalid_request_error";
            case 401 -> "authentication_error";
            case 403 -> "permission_error";
            case 404 -> "not_found_error";
            case 429 -> "rate_limit_error";
            case 500 -> "server_error";
            case 503 -> "service_unavailable_error";
            default -> "unknown_error";
        };
        return """
                {
                  "error": {
                    "type": "%s",
                    "message": "Simulated error with status code %d"
                  }
                }""".formatted(type, statusCode);
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
                  "id": "msg_test_001",
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
     * 返回 Anthropic Messages 流式 SSE 响应模板。
     * <p>
     * 包含 event 行、data 行以及最终的 event: message_stop 结束标记。
     *
     * @return SSE 格式的流式响应字符串
     */
    public static String anthropicStreamChunks() {
        return """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_test_002","type":"message","role":"assistant","model":"claude-sonnet-4-20250514","content":[],"stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":0}}}

                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"!"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":0}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":8}}

                event: message_stop
                data: {"type":"message_stop"}
                """;
    }

    /**
     * 返回 Anthropic 错误响应模板。
     *
     * @param statusCode HTTP 状态码
     * @return 包含 error.type 的 JSON 字符串
     */
    public static String anthropicError(int statusCode) {
        String type = switch (statusCode) {
            case 400 -> "invalid_request_error";
            case 401 -> "authentication_error";
            case 403 -> "permission_error";
            case 404 -> "not_found_error";
            case 429 -> "rate_limit_error";
            case 500 -> "api_error";
            case 503 -> "overloaded_error";
            default -> "unknown_error";
        };
        return """
                {
                  "error": {
                    "type": "%s",
                    "message": "Simulated error with status code %d"
                  }
                }""".formatted(type, statusCode);
    }
}
