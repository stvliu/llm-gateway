package com.codingas.gateway.adapter.protocol.anthropic;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.tuning.ProtocolTuner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic 协议出站调谐器
 *
 * <p>补全 Anthropic 请求的默认值：</p>
 * <ul>
 *   <li>max_tokens 缺省补 1024</li>
 *   <li>system 角色消息提取到顶层 system 字段</li>
 * </ul>
 */
@Component
public class AnthropicTuner implements ProtocolTuner<AnthropicMessagesRequest> {

    private static final int DEFAULT_MAX_TOKENS = 1024;

    @Override
    public String getProtocol() {
        return "anthropic";
    }

    @Override
    public AnthropicMessagesRequest tune(AnthropicMessagesRequest request) {
        // 补全 max_tokens 默认值
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(DEFAULT_MAX_TOKENS);
        }

        // 提取 system 角色消息到顶层 system 字段
        if (request.getMessages() != null && request.getSystem() == null) {
            List<AnthropicMessagesRequest.Message> nonSystemMessages = new ArrayList<>();
            String systemContent = null;

            for (AnthropicMessagesRequest.Message msg : request.getMessages()) {
                if ("system".equals(msg.getRole())) {
                    systemContent = msg.getContent() instanceof String s ? s : String.valueOf(msg.getContent());
                } else {
                    nonSystemMessages.add(msg);
                }
            }

            if (systemContent != null) {
                request.setSystem(systemContent);
                request.setMessages(nonSystemMessages);
            }
        }

        return request;
    }
}