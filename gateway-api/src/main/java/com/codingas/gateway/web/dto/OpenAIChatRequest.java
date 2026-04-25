package com.codingas.gateway.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions 请求格式
 *
 * <p>对应 OpenAI /v1/chat/completions 端点的请求格式。</p>
 *
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/create">OpenAI Chat API</a>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIChatRequest {

    /**
     * 模型名称或 ID
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 最大 token 数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数 (0-2)
     */
    private Double temperature;

    /**
     * 停止序列
     */
    private List<String> stop;

    /**
     * 频率惩罚
     */
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    /**
     * 存在惩罚
     */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    /**
     * 返回格式
     */
    @JsonProperty("response_format")
    private Map<String, Object> responseFormat;

    /**
     * 种子 (确定性采样)
     */
    private Integer seed;

    /**
     * 工具定义 (Function Calling)
     */
    private List<Map<String, Object>> tools;

    /**
     * 工具选择
     */
    @JsonProperty("tool_choice")
    private String toolChoice;

    /**
     * 是否流式响应
     */
    private Boolean stream;

    /**
     * 消息类型
     */
    @Data
    @Builder
    public static class Message {
        /**
         * 角色 (system, user, assistant, tool)
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

        /**
         * 工具调用 ID
         */
        @JsonProperty("tool_call_id")
        private String toolCallId;

        /**
         * 名称 (用于 function 角色)
         */
        private String name;
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
         * 函数参数 (JSON 字符串或对象)
         */
        private Object arguments;
    }
}
