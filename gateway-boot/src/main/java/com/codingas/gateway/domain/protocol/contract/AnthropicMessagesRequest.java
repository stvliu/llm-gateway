/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.protocol.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
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

    @Override
    public AnthropicMessagesRequest copy() {
        // 手写字段拷贝：避免 Jackson 深拷贝的性能开销与循环引用风险
        // 集合字段拷贝容器（浅拷贝元素），调谐只改 model，不修改集合元素内部
        return AnthropicMessagesRequest.builder()
                .model(this.model)
                .messages(this.messages != null ? new ArrayList<>(this.messages) : null)
                .maxTokens(this.maxTokens)
                .system(this.system)
                .temperature(this.temperature)
                .stopSequences(this.stopSequences != null ? new ArrayList<>(this.stopSequences) : null)
                .tools(this.tools != null ? new ArrayList<>(this.tools) : null)
                .toolChoice(this.toolChoice)
                .stream(this.stream)
                .build();
    }

    @Data
    @Builder
    public static class Message {
        private String role;
        private Object content;
    }
}
