package com.codingas.gateway.adapter.protocol.anthropic;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.validation.ProtocolValidationException;
import com.codingas.gateway.domain.protocol.validation.ProtocolValidator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Anthropic Messages 协议校验器
 */
@Component
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
