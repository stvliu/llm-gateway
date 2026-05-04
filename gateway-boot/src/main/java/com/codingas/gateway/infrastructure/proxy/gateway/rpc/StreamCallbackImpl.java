package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

/**
 * 流式响应回调实现
 *
 * <p>将 Consumer 包装为 StreamCallback 接口。</p>
 */
@RequiredArgsConstructor
public class StreamCallbackImpl implements StreamCallback {

    private final Consumer<String> onChunk;
    private final Runnable onCompleteCallback;
    private final Consumer<Throwable> onErrorCallback;

    /**
     * 简化构造函数（使用空回调）
     */
    public StreamCallbackImpl(Consumer<String> onChunk) {
        this.onChunk = onChunk;
        this.onCompleteCallback = () -> {};
        this.onErrorCallback = t -> {};
    }

    @Override
    public void onChunk(String data) {
        onChunk.accept(data);
    }

    @Override
    public void onComplete() {
        if (onCompleteCallback != null) {
            onCompleteCallback.run();
        }
    }

    @Override
    public void onError(Throwable t) {
        if (onErrorCallback != null) {
            onErrorCallback.accept(t);
        }
    }
}
