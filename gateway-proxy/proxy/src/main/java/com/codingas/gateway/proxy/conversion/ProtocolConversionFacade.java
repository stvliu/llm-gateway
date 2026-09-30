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
package com.codingas.gateway.proxy.conversion;

import com.codingas.gateway.protocol.canonical.CanonicalChatRequest;
import com.codingas.gateway.protocol.canonical.CanonicalChatResponse;
import com.codingas.gateway.protocol.ProtocolAdapter;
import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamChunkResult;
import com.codingas.gateway.proxy.conversion.ProtocolStreamConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ProtocolConversionFacade.class);

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
     *
     * <p><b>SPI 通用路由</b>：源协议取自 {@code request.getProtocol()}，目标协议由参数指定，
     * 从 {@link #adapters} 映射取对应 Adapter 做 normalize→denormalize。新增协议仅需注册
     * Adapter（且请求 DTO 的 {@code getProtocol()} 返回其协议名），本方法无需改动。</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ProtocolRequest convertRequest(ProtocolRequest request, String targetProtocol) {
        String sourceProtocol = request.getProtocol();
        if (sourceProtocol == null || sourceProtocol.equals(targetProtocol)) {
            return request;
        }
        ProtocolAdapter src = adapters.get(sourceProtocol);
        ProtocolAdapter dst = adapters.get(targetProtocol);
        if (src == null || dst == null) {
            // 源或目标协议未注册 Adapter，无法转换，原样返回
            return request;
        }
        CanonicalChatRequest canonical = src.normalizeRequest(request);
        return (ProtocolRequest) dst.denormalizeRequest(canonical);
    }

    /**
     * 跨协议响应转换：源协议 ≠ 目标协议时 normalize→denormalize；否则原样返回。
     *
     * <p>源协议由参数指定（上游响应来源协议），目标协议为入站协议，从 {@link #adapters}
     * 取对应 Adapter 做 normalize→denormalize。任一协议未注册 Adapter，或响应类型与
     * 源协议标识不匹配时原样返回。通用路由消除 N×N 两两硬编码——新增协议（如 gemini）
     * 仅需注册 Adapter，本方法无需再改。</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ProtocolResponse convertResponse(ProtocolResponse response, String sourceProtocol, String targetProtocol) {
        if (response == null || sourceProtocol == null || targetProtocol == null || sourceProtocol.equals(targetProtocol)) {
            return response;
        }
        ProtocolAdapter src = adapters.get(sourceProtocol);
        ProtocolAdapter dst = adapters.get(targetProtocol);
        if (src == null || dst == null) {
            return response;
        }
        try {
            CanonicalChatResponse canonical = src.normalizeResponse(response);
            return (ProtocolResponse) dst.denormalizeResponse(canonical);
        } catch (ClassCastException e) {
            // 响应类型与源协议标识不匹配（防御性兜底）：告警后原样返回，避免静默吞错掩盖 Adapter 内部 bug
            log.warn("响应类型与源协议 {} 不匹配，跳过转换: {}", sourceProtocol, e.getMessage());
            return response;
        }
    }

    /** 流式 chunk 转换（委托 ProtocolStreamConverter，方向 from→to） */
    public StreamChunkResult convertStreamChunk(String rawChunk, String fromProtocol, String toProtocol) {
        return streamConverter.convertStreamChunk(rawChunk, fromProtocol, toProtocol);
    }

    /** 流式结束标记转换（委托 ProtocolStreamConverter） */
    public StreamChunkResult convertStreamDone(String fromProtocol, String toProtocol) {
        return streamConverter.convertStreamDone(fromProtocol, toProtocol);
    }
}
