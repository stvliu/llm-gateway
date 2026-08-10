/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.protocol.contract;

/**
 * 流式回调接口
 *
 * <p>用于 LLM 流式请求的 SSE 事件回调。</p>
 *
 * <p>此接口定义在 Domain 层协议契约包中，供 UpstreamClient 使用。
 * 具体实现在 Infrastructure 层提供。</p>
 */
public interface StreamCallback {

    /**
     * 收到 SSE data 行
     *
     * @param data SSE data 内容 (不包含 "data: " 前缀)
     */
    void onChunk(String data);

    /**
     * 流式响应完成
     */
    void onComplete();

    /**
     * 流式响应出错
     *
     * @param t 错误原因
     */
    void onError(Throwable t);
}
