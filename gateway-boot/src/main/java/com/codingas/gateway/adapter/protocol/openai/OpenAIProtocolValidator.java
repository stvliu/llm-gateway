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
package com.codingas.gateway.adapter.protocol.openai;

import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.validation.ProtocolValidationException;
import com.codingas.gateway.domain.protocol.validation.ProtocolValidator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * OpenAI Chat Completions 协议校验器
 */
@Component
public class OpenAIProtocolValidator implements ProtocolValidator<OpenAIChatRequest> {

    private static final Set<String> VALID_ROLES = Set.of("system", "user", "assistant", "tool");

    @Override
    public String getProtocol() {
        return "openai";
    }

    @Override
    public void validate(OpenAIChatRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new ProtocolValidationException("openai", "model", "不能为空");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new ProtocolValidationException("openai", "messages", "不能为空");
        }
        List<OpenAIChatRequest.Message> messages = request.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            OpenAIChatRequest.Message msg = messages.get(i);
            if (msg.getRole() == null || !VALID_ROLES.contains(msg.getRole())) {
                throw new ProtocolValidationException("openai",
                        "messages[" + i + "].role", "不合法: " + msg.getRole());
            }
            if ("tool".equals(msg.getRole()) && (msg.getToolCallId() == null || msg.getToolCallId().isBlank())) {
                throw new ProtocolValidationException("openai",
                        "messages[" + i + "].tool_call_id", "tool 角色消息必须提供 tool_call_id");
            }
        }
    }
}
