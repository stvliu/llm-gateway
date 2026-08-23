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
package com.codingas.gateway.protocol.gemini;

import com.codingas.gateway.protocol.ProtocolRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini 原生请求契约（示例插件提供）。
 *
 * <p>由 gemini 插件模块定义（实现核心 {@link ProtocolRequest} 契约），
 * 携带协议标识 {@code getProtocol()="gemini"}，供 {@code ProtocolConversionFacade}
 * 按协议名通用路由。新增协议不改核心即插即用。</p>
 */
public class GeminiChatRequest implements ProtocolRequest {

    private String model;
    private String system;
    private final List<Message> messages = new ArrayList<>();
    private Integer maxTokens;
    private Double temperature;
    private boolean stream;

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getProtocol() {
        return "gemini";
    }

    @Override
    public boolean isStream() {
        return stream;
    }

    @Override
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    @Override
    public ProtocolRequest copy() {
        GeminiChatRequest copy = new GeminiChatRequest();
        copy.model = this.model;
        copy.system = this.system;
        copy.messages.addAll(this.messages);
        copy.maxTokens = this.maxTokens;
        copy.temperature = this.temperature;
        copy.stream = this.stream;
        return copy;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    /** Gemini 消息 */
    public record Message(String role, String content) {
    }
}
