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
package com.codingas.gateway.domain.protocol.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
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

    @Override
    public OpenAIChatRequest copy() {
        // 手写字段拷贝：避免 Jackson 深拷贝的性能开销与循环引用风险
        // 集合字段拷贝容器（浅拷贝元素），调谐只改 model，不修改集合元素内部
        OpenAIChatRequest c = new OpenAIChatRequest();
        c.model = this.model;
        c.messages = this.messages != null ? new ArrayList<>(this.messages) : null;
        c.maxTokens = this.maxTokens;
        c.temperature = this.temperature;
        c.stop = this.stop != null ? new ArrayList<>(this.stop) : null;
        c.frequencyPenalty = this.frequencyPenalty;
        c.presencePenalty = this.presencePenalty;
        c.responseFormat = this.responseFormat;
        c.seed = this.seed;
        c.tools = this.tools != null ? new ArrayList<>(this.tools) : null;
        c.toolChoice = this.toolChoice;
        c.stream = this.stream;
        return c;
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
