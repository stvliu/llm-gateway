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

    @Override
    public void onChunk(String data) {
        onChunk.accept(data);
    }

    @Override
    public void onComplete() {
        // 默认实现为空
    }

    @Override
    public void onError(Throwable t) {
        // 默认实现为空
    }
}
