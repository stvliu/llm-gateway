package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * StreamCallbackFactoryImpl 单元测试
 */
@DisplayName("StreamCallbackFactoryImpl 测试")
class StreamCallbackFactoryImplTest {

    private StreamCallbackFactoryImpl factory;

    @BeforeEach
    void setUp() {
        factory = new StreamCallbackFactoryImpl();
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建 StreamCallback 实例")
        void create_returnsStreamCallback() {
            // Given
            @SuppressWarnings("unchecked")
            Consumer<String> onChunk = mock(Consumer.class);
            Runnable onComplete = mock(Runnable.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = mock(Consumer.class);

            // When
            StreamCallback callback = factory.create(onChunk, onComplete, onError);

            // Then
            assertThat(callback).isNotNull();
            assertThat(callback).isInstanceOf(StreamCallbackImpl.class);
        }

        @Test
        @DisplayName("创建的回调可以调用 onChunk")
        void create_callbackOnChunk() {
            // Given
            @SuppressWarnings("unchecked")
            Consumer<String> onChunk = mock(Consumer.class);
            Runnable onComplete = mock(Runnable.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = mock(Consumer.class);

            // When
            StreamCallback callback = factory.create(onChunk, onComplete, onError);
            callback.onChunk("test chunk");

            // Then
            verify(onChunk).accept("test chunk");
        }

        @Test
        @DisplayName("创建的回调可以调用 onComplete")
        void create_callbackOnComplete() {
            // Given
            @SuppressWarnings("unchecked")
            Consumer<String> onChunk = mock(Consumer.class);
            Runnable onComplete = mock(Runnable.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = mock(Consumer.class);

            // When
            StreamCallback callback = factory.create(onChunk, onComplete, onError);
            callback.onComplete();

            // Then
            verify(onComplete).run();
        }

        @Test
        @DisplayName("创建的回调可以调用 onError")
        void create_callbackOnError() {
            // Given
            @SuppressWarnings("unchecked")
            Consumer<String> onChunk = mock(Consumer.class);
            Runnable onComplete = mock(Runnable.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = mock(Consumer.class);
            RuntimeException error = new RuntimeException("test error");

            // When
            StreamCallback callback = factory.create(onChunk, onComplete, onError);
            callback.onError(error);

            // Then
            verify(onError).accept(error);
        }

        @Test
        @DisplayName("多次调用返回不同实例")
        void create_multipleCalls_returnsDifferentInstances() {
            // Given
            @SuppressWarnings("unchecked")
            Consumer<String> onChunk = mock(Consumer.class);
            Runnable onComplete = mock(Runnable.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = mock(Consumer.class);

            // When
            StreamCallback callback1 = factory.create(onChunk, onComplete, onError);
            StreamCallback callback2 = factory.create(onChunk, onComplete, onError);

            // Then
            assertThat(callback1).isNotSameAs(callback2);
        }
    }
}
