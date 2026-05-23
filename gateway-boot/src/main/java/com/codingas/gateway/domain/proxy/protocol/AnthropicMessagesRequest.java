package com.codingas.gateway.domain.proxy.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages 请求格式
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicMessagesRequest implements ProtocolRequest {

    private String model;
    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private String system;
    private Double temperature;

    @JsonProperty("stop_sequences")
    private List<String> stopSequences;

    private List<Map<String, Object>> tools;

    @JsonProperty("tool_choice")
    private Map<String, Object> toolChoice;

    private Boolean stream;

    @Override
    public String getProtocol() {
        return "anthropic";
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
    @Builder
    public static class Message {
        private String role;
        private Object content;
    }
}
