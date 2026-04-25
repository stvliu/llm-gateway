package com.codingas.gateway.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Anthropic Messages 响应格式
 *
 * <p>对应 Anthropic /v1/messages 端点的响应格式。</p>
 *
 * @see <a href="https://docs.anthropic.com/en/api/reference/messages">Anthropic Messages API</a>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicMessagesResponse {

    /**
     * 响应 ID
     */
    private String id;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 响应类型
     */
    private String type;

    /**
     * 角色
     */
    private String role;

    /**
     * 内容块列表
     */
    private List<ContentBlock> content;

    /**
     * 停止原因
     */
    @JsonProperty("stop_reason")
    private String stopReason;

    /**
     * 停止序列
     */
    @JsonProperty("stop_sequence")
    private Object stopSequence;

    /**
     * Token 使用量
     */
    private Usage usage;

    /**
     * 错误信息
     */
    private Error error;

    /**
     * 内容块
     */
    @Data
    @Builder
    public static class ContentBlock {
        /**
         * 内容类型 (text, tool_use)
         */
        private String type;

        /**
         * 文本内容
         */
        private String text;

        /**
         * 工具使用
         */
        @JsonProperty("tool_use")
        private ToolUse toolUse;
    }

    /**
     * 工具使用块
     */
    @Data
    @Builder
    public static class ToolUse {
        /**
         * 工具名称
         */
        private String name;

        /**
         * 工具输入 (JSON 对象)
         */
        private Object input;

        /**
         * 工具调用 ID
         */
        @JsonProperty("id")
        private String id;
    }

    /**
     * Token 使用量
     */
    @Data
    @Builder
    public static class Usage {
        /**
         * 输入 token 数
         */
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        /**
         * 输出 token 数
         */
        @JsonProperty("output_tokens")
        private Integer outputTokens;
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
    }
}
