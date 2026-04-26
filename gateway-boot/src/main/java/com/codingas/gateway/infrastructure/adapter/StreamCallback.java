package com.codingas.gateway.infrastructure.adapter;

/**
 * 流式响应回调接口
 *
 * <p>用于 OkHttp 流式请求的 SSE 事件回调。</p>
 */
public interface StreamCallback {

    /**
     * 接收到 SSE data 行
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
