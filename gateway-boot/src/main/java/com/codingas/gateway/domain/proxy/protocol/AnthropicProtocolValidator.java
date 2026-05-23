package com.codingas.gateway.domain.proxy.protocol;

import com.codingas.gateway.domain.proxy.exception.ProtocolValidationException;
import org.springframework.stereotype.Component;

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
    }
}