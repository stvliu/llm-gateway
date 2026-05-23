package com.codingas.gateway.application.proxy.dto.chat;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 对话补全请求 DTO
 *
 * <p>统一的对话请求格式，内部使用 OpenAI 格式作为中间表示。</p>
 * <p>用于 Chat Completions API（对话补全能力）的请求处理。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 协议类型 (openai, anthropic)
     *
     * <p>用于路由层选择正确的端点和适配器。</p>
     */
    private String protocol;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 温度参数 (0-2)
     */
    private Double temperature;

    /**
     * 最大 token 数
     */
    private Integer maxTokens;

    /**
     * 停止序列
     */
    private List<String> stop;

    /**
     * 频率惩罚
     */
    private Double frequencyPenalty;

    /**
     * 存在惩罚
     */
    private Double presencePenalty;

    /**
     * 响应格式
     */
    private String responseFormat;

    /**
     * 种子 (用于确定性采样)
     */
    private Integer seed;

    /**
     * 工具定义 (Function Calling)
     */
    private List<ToolDefinition> tools;

    /**
     * 工具选择
     */
    private String toolChoice;

    /**
     * 是否流式响应
     */
    private boolean stream;

    /**
     * 系统提示 (用于 Anthropic 等)
     */
    private String systemPrompt;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;

    /**
     * 超时时间 (秒)
     *
     * <p>覆盖默认超时时间。如果为 null，使用适配器的默认超时。</p>
     */
    private Integer timeoutSeconds;

    /**
     * 消息类型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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
        private List<ToolCall> toolCalls;

        /**
         * 工具调用 ID
         */
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
    @NoArgsConstructor
    @AllArgsConstructor
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
    @NoArgsConstructor
    @AllArgsConstructor
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
     * 工具定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolDefinition {
        /**
         * 工具类型
         */
        private String type;

        /**
         * 函数定义
         */
        private Function function;
    }

    /**
     * 函数定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Function {
        /**
         * 函数名
         */
        private String name;

        /**
         * 函数描述
         */
        private String description;

        /**
         * 参数定义 (JSON Schema)
         */
        private String parameters;
    }
}
