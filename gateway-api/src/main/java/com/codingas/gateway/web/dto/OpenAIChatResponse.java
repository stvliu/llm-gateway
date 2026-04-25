package com.codingas.gateway.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * OpenAI Chat Completions 响应格式
 *
 * <p>对应 OpenAI /v1/chat/completions 端点的响应格式。</p>
 *
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/create">OpenAI Chat API</a>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIChatResponse {

    /**
     * 响应 ID
     */
    private String id;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 创建时间戳
     */
    private Long created;

    /**
     * 模型选择
     */
    @JsonProperty("model_select")
    private String modelSelect;

    /**
     * 响应选项
     */
    private List<Choice> choices;

    /**
     * Token 使用量
     */
    private Usage usage;

    /**
     * 错误信息
     */
    private Error error;

    /**
     * 响应选项
     */
    @Data
    @Builder
    public static class Choice {
        /**
         * 索引
         */
        private Integer index;

        /**
         * 消息
         */
        private Message message;

        /**
         * 日志概率
         */
        @JsonProperty("logprobs")
        private Object logprobs;

        /**
         * 停止原因
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 消息
     */
    @Data
    @Builder
    public static class Message {
        /**
         * 角色
         */
        private String role;

        /**
         * 内容
         */
        private String content;

        /**
         * 工具调用
         */
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    /**
     * 工具调用
     */
    @Data
    @Builder
    public static class ToolCall {
        /**
         * 工具调用 ID
         */
        private String id;

        /**
         * 工具类型 (function)
         */
        private String type;

        /**
         * 函数调用
         */
        private FunctionCall function;
    }

    /**
     * 函数调用
     */
    @Data
    @Builder
    public static class FunctionCall {
        /**
         * 函数名
         */
        private String name;

        /**
         * 函数参数 (JSON 字符串)
         */
        private String arguments;
    }

    /**
     * Token 使用量
     */
    @Data
    @Builder
    public static class Usage {
        /**
         * 提示 Token 数
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 完成 Token 数
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * 总 Token 数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * 错误信息
     */
    @Data
    @Builder
    public static class Error {
        /**
         * 错误类型
         */
        private String type;

        /**
         * 错误代码
         */
        private String code;

        /**
         * 错误消息
         */
        private String message;

        /**
         * 参数名
         */
        private String param;
    }
}
