package com.codingas.gateway.domain.proxy.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions 请求格式
 *
 * <p>对应 OpenAI /v1/chat/completions 端点的请求格式。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIChatRequest implements ProtocolRequest {

    private String model;
    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;
    private List<String> stop;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("response_format")
    private Map<String, Object> responseFormat;

    private Integer seed;

    private List<Map<String, Object>> tools;

    @JsonProperty("tool_choice")
    private String toolChoice;

    private Boolean stream;

    @Override
    public String getProtocol() {
        return "openai";
    }

    @Override
    public boolean isStream() {
        return stream != null && stream;
    }

    @Override
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String role;
        private String content;

        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        @JsonProperty("tool_call_id")
        private String toolCallId;

        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolCall {
        private String id;
        private String type;
        private FunctionCall function;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FunctionCall {
        private String name;
        private Object arguments;
    }
}
