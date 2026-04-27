package com.codingas.gateway.adapter.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Anthropic Messages 响应格式
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicMessagesResponse {

    private String id;
    private String model;
    private String type;
    private String role;
    private List<ContentBlock> content;

    @JsonProperty("stop_reason")
    private String stopReason;

    @JsonProperty("stop_sequence")
    private Object stopSequence;

    private Usage usage;
    private Error error;

    @Data
    @Builder
    public static class ContentBlock {
        private String type;
        private String text;

        @JsonProperty("tool_use")
        private ToolUse toolUse;
    }

    @Data
    @Builder
    public static class ToolUse {
        private String name;
        private Object input;
        private String id;
    }

    @Data
    @Builder
    public static class Usage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;
    }

    @Data
    @Builder
    public static class Error {
        private String type;
        private String code;
        private String message;
    }
}
