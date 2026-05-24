package com.codingas.gateway.domain.supply.protocol;

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
