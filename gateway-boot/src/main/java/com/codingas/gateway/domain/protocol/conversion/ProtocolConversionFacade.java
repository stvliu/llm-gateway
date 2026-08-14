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
package com.codingas.gateway.domain.protocol.conversion;

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalChatResponse;
import com.codingas.gateway.api.capability.protocol.ProtocolAdapter;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.infrastructure.protocol.AnthropicProtocolAdapter;
import com.codingas.gateway.infrastructure.protocol.OpenAIProtocolAdapter;
import com.codingas.gateway.infrastructure.protocol.ProtocolStreamConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 跨协议转换门面：编排各协议 Adapter，把"原生→规范→原生"收敛为对外简洁调用。
 *
 * <p>对外提供与旧 {@code ProtocolConverter} 相同语义的
 * {@code convertRequest/convertResponse/convertStreamChunk/convertStreamDone}，
 * 但底层基于 Canonical IR + ProtocolAdapter（normalize + denormalize），
 * 消除 N×N 两两转换。流式仍委托 {@link ProtocolStreamConverter}（本轮保持原样）。</p>
 */
@Component
public class ProtocolConversionFacade {

    private final OpenAIProtocolAdapter openaiAdapter;
    private final AnthropicProtocolAdapter anthropicAdapter;
    private final ProtocolStreamConverter streamConverter;

    public ProtocolConversionFacade(OpenAIProtocolAdapter openaiAdapter,
                                    AnthropicProtocolAdapter anthropicAdapter) {
        this.openaiAdapter = openaiAdapter;
        this.anthropicAdapter = anthropicAdapter;
        this.streamConverter = new ProtocolStreamConverter(new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Autowired
    public ProtocolConversionFacade(OpenAIProtocolAdapter openaiAdapter,
                                    AnthropicProtocolAdapter anthropicAdapter,
                                    ProtocolStreamConverter streamConverter) {
        this.openaiAdapter = openaiAdapter;
        this.anthropicAdapter = anthropicAdapter;
        this.streamConverter = streamConverter;
    }

    /**
     * 跨协议请求转换：目标协议 ≠ 源协议时 normalize→denormalize；否则原样返回。
     */
    public ProtocolRequest convertRequest(ProtocolRequest request, String targetProtocol) {
        if (request instanceof OpenAIChatRequest openai && !"openai".equals(targetProtocol)) {
            return toAnthropic(openai);
        }
        if (request instanceof AnthropicMessagesRequest anthropic && !"anthropic".equals(targetProtocol)) {
            return toOpenAI(anthropic);
        }
        return request;
    }

    /** OpenAI 请求 → Anthropic 请求（normalize + denormalize 两跳） */
    private AnthropicMessagesRequest toAnthropic(OpenAIChatRequest openai) {
        CanonicalChatRequest canonical = openaiAdapter.normalizeRequest(openai);
        return anthropicAdapter.denormalizeRequest(canonical);
    }

    /** Anthropic 请求 → OpenAI 请求 */
    private OpenAIChatRequest toOpenAI(AnthropicMessagesRequest anthropic) {
        CanonicalChatRequest canonical = anthropicAdapter.normalizeRequest(anthropic);
        return openaiAdapter.denormalizeRequest(canonical);
    }

    /**
     * 跨协议响应转换：目标协议 ≠ 源协议时 normalize→denormalize；否则原样返回。
     */
    public ProtocolResponse convertResponse(ProtocolResponse response, String sourceProtocol) {
        if (response instanceof AnthropicMessagesResponse anthropic && "anthropic".equals(sourceProtocol)) {
            CanonicalChatResponse canonical = anthropicAdapter.normalizeResponse(anthropic);
            return (OpenAIChatResponse) openaiAdapter.denormalizeResponse(canonical);
        }
        if (response instanceof OpenAIChatResponse openai && "openai".equals(sourceProtocol)) {
            CanonicalChatResponse canonical = openaiAdapter.normalizeResponse(openai);
            return (AnthropicMessagesResponse) anthropicAdapter.denormalizeResponse(canonical);
        }
        return response;
    }

    /** 流式 chunk 转换（委托 ProtocolStreamConverter，方向 from→to） */
    public com.codingas.gateway.domain.protocol.contract.StreamChunkResult convertStreamChunk(
            String rawChunk, String fromProtocol, String toProtocol) {
        return streamConverter.convertStreamChunk(rawChunk, fromProtocol, toProtocol);
    }

    /** 流式结束标记转换（委托 ProtocolStreamConverter） */
    public com.codingas.gateway.domain.protocol.contract.StreamChunkResult convertStreamDone(
            String fromProtocol, String toProtocol) {
        return streamConverter.convertStreamDone(fromProtocol, toProtocol);
    }
}
