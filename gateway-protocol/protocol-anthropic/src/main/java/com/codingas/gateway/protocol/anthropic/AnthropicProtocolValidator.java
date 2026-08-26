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
package com.codingas.gateway.protocol.anthropic;

import com.codingas.gateway.protocol.raw.AnthropicMessagesRequest;
import com.codingas.gateway.protocol.validation.ProtocolValidationException;
import com.codingas.gateway.protocol.validation.ProtocolValidator;

import java.util.List;

/**
 * Anthropic Messages 协议校验器
 */
public class AnthropicProtocolValidator implements ProtocolValidator<AnthropicMessagesRequest> {

    @Override
    public String getProtocol() {
        return "anthropic";
    }

    @Override
    public void validate(AnthropicMessagesRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new ProtocolValidationException("anthropic", "model", "不能为空");
        }
        if (request.getMaxTokens() == null) {
            throw new ProtocolValidationException("anthropic", "max_tokens", "必填");
        }
        if (request.getMaxTokens() <= 0) {
            throw new ProtocolValidationException("anthropic", "max_tokens", "必须大于0");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new ProtocolValidationException("anthropic", "messages", "不能为空");
        }
        List<AnthropicMessagesRequest.Message> messages = request.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            AnthropicMessagesRequest.Message msg = messages.get(i);
            if ("system".equals(msg.getRole())) {
                throw new ProtocolValidationException("anthropic",
                        "messages[" + i + "].role", "system 角色应使用顶层 system 字段，不应出现在 messages 中");
            }
        }
        if (!"user".equals(messages.get(0).getRole())) {
            throw new ProtocolValidationException("anthropic", "messages[0].role", "首条消息必须是 user 角色");
        }
    }
}
