package com.codingas.gateway.adapter.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * OpenAI Chat Completions 响应格式
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIChatResponse {

    private String id;
    private String model;
    private Long created;

    @JsonProperty("model_select")
    private String modelSelect;

    private List<Choice> choices;
    private Usage usage;
    private Error error;

    @Data
    @Builder
    public static class Choice {
        private Integer index;
        private Message message;

        @JsonProperty("logprobs")
        private Object logprobs;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;

        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    @Data
    @Builder
    public static class ToolCall {
        private String id;
        private String type;
        private FunctionCall function;
    }

    @Data
    @Builder
    public static class FunctionCall {
        private String name;
        private String arguments;
    }

    @Data
    @Builder
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    @Data
    @Builder
    public static class Error {
        private String type;
        private String code;
        private String message;
        private String param;
    }
}
