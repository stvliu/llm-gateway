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
package com.codingas.gateway.application.protocol.conversion;

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalChatResponse;
import com.codingas.gateway.api.capability.protocol.ProtocolAdapter;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamChunkResult;
import com.codingas.gateway.infrastructure.protocol.ProtocolStreamConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 跨协议转换门面：编排各协议 Adapter，把"原生→规范→原生"收敛为对外简洁调用。
 *
 * <p>对外提供统一的 {@code convertRequest/convertResponse/convertStreamChunk/convertStreamDone}
 * 转换语义，底层基于 Canonical IR + {@link ProtocolAdapter}（normalize + denormalize），
 * 消除 N×N 两两转换。</p>
 *
 * <p><b>SPI 装配</b>：注入 {@code List<ProtocolAdapter<?>>} 收集全部已注册协议 Adapter Bean，
 * 按 {@code protocol()} 标识动态查找，不依赖任何具体协议 Adapter 类 —— 新增协议仅需
 * 注册一个 Adapter Bean（协议插件化），本门面无需改动。流式转换委托
 * {@link ProtocolStreamConverter}（保持原样）。</p>
 */
@Component
public class ProtocolConversionFacade {

    /** 协议名 → Adapter 映射（如 "openai" → OpenAIProtocolAdapter） */
    private final Map<String, ProtocolAdapter<?>> adapters;
    private final ProtocolStreamConverter streamConverter;

    public ProtocolConversionFacade(List<ProtocolAdapter<?>> adapterList,
                                    ProtocolStreamConverter streamConverter) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(ProtocolAdapter::protocol, Function.identity()));
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
        ProtocolAdapter<OpenAIChatRequest> src = adapterFor("openai");
        ProtocolAdapter<AnthropicMessagesRequest> dst = adapterFor("anthropic");
        CanonicalChatRequest canonical = src.normalizeRequest(openai);
        return dst.denormalizeRequest(canonical);
    }

    /** Anthropic 请求 → OpenAI 请求 */
    private OpenAIChatRequest toOpenAI(AnthropicMessagesRequest anthropic) {
        ProtocolAdapter<AnthropicMessagesRequest> src = adapterFor("anthropic");
        ProtocolAdapter<OpenAIChatRequest> dst = adapterFor("openai");
        CanonicalChatRequest canonical = src.normalizeRequest(anthropic);
        return dst.denormalizeRequest(canonical);
    }

    /**
     * 跨协议响应转换：目标协议 ≠ 源协议时 normalize→denormalize；否则原样返回。
     */
    public ProtocolResponse convertResponse(ProtocolResponse response, String sourceProtocol) {
        if (response instanceof AnthropicMessagesResponse anthropic && "anthropic".equals(sourceProtocol)) {
            ProtocolAdapter<AnthropicMessagesResponse> src = adapterFor("anthropic");
            ProtocolAdapter<OpenAIChatResponse> dst = adapterFor("openai");
            CanonicalChatResponse canonical = src.normalizeResponse(anthropic);
            return (OpenAIChatResponse) dst.denormalizeResponse(canonical);
        }
        if (response instanceof OpenAIChatResponse openai && "openai".equals(sourceProtocol)) {
            ProtocolAdapter<OpenAIChatResponse> src = adapterFor("openai");
            ProtocolAdapter<AnthropicMessagesResponse> dst = adapterFor("anthropic");
            CanonicalChatResponse canonical = src.normalizeResponse(openai);
            return (AnthropicMessagesResponse) dst.denormalizeResponse(canonical);
        }
        return response;
    }

    /** 流式 chunk 转换（委托 ProtocolStreamConverter，方向 from→to） */
    public StreamChunkResult convertStreamChunk(String rawChunk, String fromProtocol, String toProtocol) {
        return streamConverter.convertStreamChunk(rawChunk, fromProtocol, toProtocol);
    }

    /** 流式结束标记转换（委托 ProtocolStreamConverter） */
    public StreamChunkResult convertStreamDone(String fromProtocol, String toProtocol) {
        return streamConverter.convertStreamDone(fromProtocol, toProtocol);
    }

    /**
     * 按协议标识获取 Adapter（泛型桥接，规避通配符捕获限制）。
     *
     * @param protocol 协议标识（如 "openai"）
     */
    @SuppressWarnings("unchecked")
    private <T> ProtocolAdapter<T> adapterFor(String protocol) {
        return (ProtocolAdapter<T>) adapters.get(protocol);
    }
}
