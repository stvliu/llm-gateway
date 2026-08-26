/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.protocol.raw;

import com.codingas.gateway.protocol.ProtocolRequest;

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
