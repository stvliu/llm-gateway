package com.codingas.gateway.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages 请求格式
 *
 * <p>对应 Anthropic /v1/messages 端点的请求格式。</p>
 *
 * @see <a href="https://docs.anthropic.com/en/api/reference/messages">Anthropic Messages API</a>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicMessagesRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 最大 token 数 (必需)
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 系统提示
     */
    private String system;

    /**
     * 温度参数 (0-1)
     */
    private Double temperature;

    /**
     * 停止序列
     */
    @JsonProperty("stop_sequences")
    private List<String> stopSequences;

    /**
     * 工具定义
     */
    private List<Map<String, Object>> tools;

    /**
     * 工具选择策略
     */
    @JsonProperty("tool_choice")
    private Map<String, Object> toolChoice;

    /**
     * 流式响应
     */
    private Boolean stream;

    /**
     * 消息类型
     */
    @Data
    @Builder
    public static class Message {
        /**
         * 角色 (user, assistant)
         */
        private String role;

        /**
         * 内容 (文本或 content blocks)
         */
        private Object content;
    }
}
