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

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalChatResponse;
import com.codingas.gateway.api.capability.protocol.CanonicalContentBlock;
import com.codingas.gateway.api.capability.protocol.CanonicalMessage;
import com.codingas.gateway.api.capability.protocol.CanonicalUsage;
import com.codingas.gateway.api.capability.protocol.ProtocolAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini 协议适配器（示例插件）。
 *
 * <p><b>插件化验证金标准</b>：新增 Gemini 协议仅需新建本插件模块（契约类型
 * {@link GeminiChatRequest} + Adapter + AutoConfiguration + DB 供应商数据），
 * 不改 gateway-proxy / gateway-protocol 核心。请求转换经
 * {@code ProtocolConversionFacade} 按协议名（getProtocol()="gemini"）通用路由。</p>
 */
public class GeminiProtocolAdapter implements ProtocolAdapter<GeminiChatRequest> {

    @Override
    public String protocol() {
        return "gemini";
    }

    @Override
    public CanonicalChatRequest normalizeRequest(GeminiChatRequest req) {
        List<CanonicalMessage> messages = new ArrayList<>();
        for (GeminiChatRequest.Message m : req.getMessages()) {
            messages.add(CanonicalMessage.builder().role(m.role()).content(m.content()).build());
        }
        return CanonicalChatRequest.builder()
                .model(req.getModel())
                .system(req.getSystem())
                .messages(messages)
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .stream(req.isStream())
                .build();
    }

    @Override
    public GeminiChatRequest denormalizeRequest(CanonicalChatRequest c) {
        GeminiChatRequest out = new GeminiChatRequest();
        out.setModel(c.getModel());
        out.setSystem(c.getSystem());
        out.setMaxTokens(c.getMaxTokens());
        out.setTemperature(c.getTemperature());
        out.setStream(c.isStream());
        if (c.getMessages() != null) {
            for (CanonicalMessage cm : c.getMessages()) {
                out.addMessage(new GeminiChatRequest.Message(cm.getRole(), cm.getContent()));
            }
        }
        return out;
    }

    @Override
    public CanonicalChatResponse normalizeResponse(Object nativeResponse) {
        GeminiChatResponse resp = (GeminiChatResponse) nativeResponse;
        return CanonicalChatResponse.builder()
                .id(resp.id())
                .model(resp.model())
                .content(resp.text() == null ? List.of() : List.of(CanonicalContentBlock.builder()
                        .type("text").text(resp.text()).build()))
                .usage(resp.inputTokens() == null ? null : CanonicalUsage.builder()
                        .inputTokens(resp.inputTokens())
                        .outputTokens(resp.outputTokens())
                        .build())
                .build();
    }

    @Override
    public Object denormalizeResponse(CanonicalChatResponse c) {
        StringBuilder text = new StringBuilder();
        if (c.getContent() != null) {
            for (CanonicalContentBlock b : c.getContent()) {
                if ("text".equals(b.getType()) && b.getText() != null) {
                    text.append(b.getText());
                }
            }
        }
        return new GeminiChatResponse(c.getId(), c.getModel(), text.toString(),
                c.getUsage() == null ? null : c.getUsage().getInputTokens(),
                c.getUsage() == null ? null : c.getUsage().getOutputTokens());
    }
}
