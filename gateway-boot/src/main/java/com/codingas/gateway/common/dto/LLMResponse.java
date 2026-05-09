package com.codingas.gateway.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * LLM 响应 DTO
 *
 * <p>统一的 LLM 响应格式，内部使用 OpenAI 格式作为中间表示。</p>
 */
@Data
@Builder
public class LLMResponse {

    /**
     * 提供商
     */
    private String provider;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 响应 ID
     */
    private String id;

    /**
     * 响应创建时间
     */
    private Long created;

    /**
     * 消息内容
     */
    private Content content;

    /**
     * Token 使用量
     */
    private Usage usage;

    /**
     * 停止原因
     */
    private String finishReason;

    /**
     * 是否为流式响应
     */
    private boolean stream;

    /**
     * 错误信息 (如果有)
     */
    private Error error;

    /**
     * 额外响应数据
     */
    private Map<String, Object> extraData;

    /**
     * 消息内容
     */
    @Data
    @Builder
    public static class Content {
        /**
         * 文本内容
         */
        private String text;

        /**
         * 工具调用列表
         */
        private List<ToolCall> toolCalls;

        /**
         * 角色
         */
        private String role;
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
        private Integer promptTokens;

        /**
         * 完成 Token 数
         */
        private Integer completionTokens;

        /**
         * 总 Token 数
         */
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
         * 参数名 (如果是参数错误)
         */
        private String param;
    }

    /**
     * 创建错误响应
     */
    public static LLMResponse error(String provider, String message) {
        return LLMResponse.builder()
                .provider(provider)
                .error(Error.builder()
                        .message(message)
                        .type("api_error")
                        .build())
                .build();
    }
}
