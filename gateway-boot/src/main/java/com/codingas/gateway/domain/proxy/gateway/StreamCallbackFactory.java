package com.codingas.gateway.domain.proxy.gateway;

/**
 * StreamCallback 工厂接口
 *
 * <p>用于创建 StreamCallback 实例，避免 Domain 层直接依赖 Infrastructure 层的实现。</p>
 */
public interface StreamCallbackFactory {

    /**
     * 创建 StreamCallback 实例
     *
     * @param onChunk 接收到数据块的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     * @return StreamCallback 实例
     */
    StreamCallback create(java.util.function.Consumer<String> onChunk,
                          Runnable onComplete,
                          java.util.function.Consumer<Throwable> onError);
}
