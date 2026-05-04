package com.codingas.gateway.infrastructure.adapter;

import com.codingas.gateway.infrastructure.proxy.gateway.rpc.StreamCallbackImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * StreamCallbackImpl 单元测试
 *
 * @author Liu Ye
 */
@DisplayName("StreamCallbackImpl 测试")
class StreamCallbackImplTest {

    @Test
    @DisplayName("onChunk 应调用 Consumer")
    void onChunk_shouldCallConsumer() {
        AtomicReference<String> captured = new AtomicReference<>();
        Consumer<String> consumer = captured::set;

        StreamCallbackImpl callback = new StreamCallbackImpl(consumer);
        callback.onChunk("test data");

        assertThat(captured.get()).isEqualTo("test data");
    }

    @Test
    @DisplayName("onChunk 应支持多次调用")
    void onChunk_shouldSupportMultipleCalls() {
        StringBuilder concatenated = new StringBuilder();
        Consumer<String> consumer = data -> concatenated.append(data);

        StreamCallbackImpl callback = new StreamCallbackImpl(consumer);
        callback.onChunk("first");
        callback.onChunk("second");

        assertThat(concatenated.toString()).isEqualTo("firstsecond");
    }

    @ParameterizedTest
    @MethodSource("provideNullTestCases")
    @DisplayName("onChunk 应正确处理各种数据")
    void onChunk_shouldHandleVariousData(String data) {
        AtomicReference<String> captured = new AtomicReference<>();
        Consumer<String> consumer = captured::set;

        StreamCallbackImpl callback = new StreamCallbackImpl(consumer);
        callback.onChunk(data);

        assertThat(captured.get()).isEqualTo(data);
    }

    static java.util.stream.Stream<Arguments> provideNullTestCases() {
        return java.util.stream.Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of("data"),
                Arguments.of("data with spaces"),
                Arguments.of("newline\n"),
                Arguments.of("tabs\t")
        );
    }

    @Test
    @DisplayName("onComplete 不应抛出异常")
    void onComplete_shouldNotThrow() {
        Consumer<String> consumer = data -> {};

        StreamCallbackImpl callback = new StreamCallbackImpl(consumer);

        assertThatCode(callback::onComplete).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("onComplete 可多次调用")
    void onComplete_shouldBeCallableMultipleTimes() {
        Consumer<String> consumer = data -> {};

        StreamCallbackImpl callback = new StreamCallbackImpl(consumer);

        assertThatCode(() -> {
            callback.onComplete();
            callback.onComplete();
            callback.onComplete();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("onError 不应抛出异常")
    void onError_shouldNotThrow() {
        Consumer<String> consumer = data -> {};
        RuntimeException ex = new RuntimeException("test error");

        StreamCallbackImpl callback = new StreamCallbackImpl(consumer);

        assertThatCode(() -> callback.onError(ex)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("onError 应接收异常但不影响 Consumer")
    void onError_shouldReceiveExceptionButNotAffectConsumer() {
        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        Consumer<String> consumer = data -> consumerCalled.set(true);

        RuntimeException ex = new RuntimeException("test error");
        StreamCallbackImpl callback = new StreamCallbackImpl(consumer);

        callback.onError(ex);
        callback.onChunk("data");

        assertThat(consumerCalled.get()).isTrue();
    }

    @Test
    @DisplayName("onError 可接收各种异常类型")
    void onError_shouldReceiveVariousExceptionTypes() {
        Consumer<String> consumer = data -> {};

        StreamCallbackImpl callback = new StreamCallbackImpl(consumer);

        assertThatCode(() -> callback.onError(new RuntimeException("runtime"))).doesNotThrowAnyException();
        assertThatCode(() -> callback.onError(new IllegalArgumentException("illegal"))).doesNotThrowAnyException();
        assertThatCode(() -> callback.onError(new NullPointerException("null"))).doesNotThrowAnyException();
    }
}