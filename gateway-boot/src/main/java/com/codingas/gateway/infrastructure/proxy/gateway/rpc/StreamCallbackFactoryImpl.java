package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.gateway.StreamCallbackFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * StreamCallback 工厂实现
 *
 * <p>Infrastructure 层的工厂实现，负责创建 StreamCallback 实例。</p>
 */
@Component
public class StreamCallbackFactoryImpl implements StreamCallbackFactory {

    @Override
    public StreamCallback create(Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        return new StreamCallbackImpl(onChunk, onComplete, onError);
    }
}
