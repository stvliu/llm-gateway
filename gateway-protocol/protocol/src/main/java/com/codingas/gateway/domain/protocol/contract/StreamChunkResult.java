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
package com.codingas.gateway.domain.protocol.contract;

/**
 * 流式 chunk 转换结果
 *
 * @param eventType SSE event 类型（Anthropic 协议需要，如 "content_block_delta"、"message_delta"；
 *                  OpenAI 协议无 event 行，为 null）
 * @param data      SSE data 内容（JSON 字符串或 "[DONE]"）
 */
public record StreamChunkResult(String eventType, String data) {

    /**
     * 仅包含 data 的结果（无 event 类型，适用于 OpenAI 协议）
     */
    public static StreamChunkResult dataOnly(String data) {
        return new StreamChunkResult(null, data);
    }

    /**
     * 包含 event 类型和 data 的结果（适用于 Anthropic 协议）
     */
    public static StreamChunkResult of(String eventType, String data) {
        return new StreamChunkResult(eventType, data);
    }
}
