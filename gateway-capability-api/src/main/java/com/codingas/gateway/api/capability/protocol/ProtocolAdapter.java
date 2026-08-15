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
package com.codingas.gateway.api.capability.protocol;

/**
 * 协议适配器 SPI：原生协议 ↔ 规范内部模型（Canonical IR）的双向转换。
 *
 * <p>每个支持的协议实现一个 Adapter，只负责"原生↔规范"两跳。网关跨协议转换
 * 由上层编排 normalize + denormalize，避免 N×N 两两转换器。</p>
 *
 * @param <T> 该协议的原生请求类型（如 OpenAIChatRequest / AnthropicMessagesRequest）
 */
public interface ProtocolAdapter<T> {

    /**
     * 协议标识（小写，如 "openai" / "anthropic"）
     */
    String protocol();

    /**
     * 入站：原生请求 → 规范请求
     */
    CanonicalChatRequest normalizeRequest(T nativeRequest);

    /**
     * 出站：规范请求 → 原生请求
     */
    T denormalizeRequest(CanonicalChatRequest canonical);

    /**
     * 入站：原生响应 → 规范响应
     */
    CanonicalChatResponse normalizeResponse(Object nativeResponse);

    /**
     * 出站：规范响应 → 原生响应
     */
    Object denormalizeResponse(CanonicalChatResponse canonical);
}
