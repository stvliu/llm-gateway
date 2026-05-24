package com.codingas.gateway.domain.supply.gateway;

/**
 * 流式响应回调接口
 *
 * <p>用于 LLM 流式请求的 SSE 事件回调。</p>
 *
 * <p>此接口定义在 Domain 层，供 ProtocolGateway 使用。
 * 具体实现在 Infrastructure 层提供。</p>
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