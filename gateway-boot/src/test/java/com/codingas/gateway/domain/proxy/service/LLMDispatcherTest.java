package com.codingas.gateway.domain.proxy.service;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.gateway.LLMProviderPort;
import com.codingas.gateway.domain.proxy.gateway.ModelRouter;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.gateway.StreamCallbackFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * LLMDispatcher 完整单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LLMDispatcher 测试")
class LLMDispatcherTest {

    @Mock
    private ModelRouter modelRouter;

    @Mock
    private StreamCallbackFactory streamCallbackFactory;

    @Mock
    private LLMProviderPort providerPort;

    @Mock
    private StreamCallback streamCallback;

    private LLMDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new LLMDispatcher(modelRouter, streamCallbackFactory);
    }

    @Nested
    @DisplayName("send 方法测试")
    class SendTests {

        @Test
        @DisplayName("发送非流式请求成功")
        void send_validRequest_returnsResponse() {
            // given
            LLMRequest request = createTestRequest();
            LLMResponse expectedResponse = createTestResponse();

            when(modelRouter.select(request, RouteGroup.RoutingStrategy.WEIGHTED)).thenReturn(providerPort);
            when(providerPort.getProviderCode()).thenReturn("openai");
            when(providerPort.chat(request)).thenReturn(expectedResponse);

            // when
            LLMResponse response = dispatcher.send(request, RouteGroup.RoutingStrategy.WEIGHTED);

            // then
            assertThat(response).isEqualTo(expectedResponse);
            verify(modelRouter).select(request, RouteGroup.RoutingStrategy.WEIGHTED);
            verify(providerPort).chat(request);
        }

        @Test
        @DisplayName("使用不同的路由策略")
        void send_differentStrategies_usesCorrectStrategy() {
            // given
            LLMRequest request = createTestRequest();
            LLMResponse response = createTestResponse();

            when(modelRouter.select(any(LLMRequest.class), any())).thenReturn(providerPort);
            when(providerPort.getProviderCode()).thenReturn("anthropic");
            when(providerPort.chat(any())).thenReturn(response);

            // when & then
            dispatcher.send(request, RouteGroup.RoutingStrategy.WEIGHTED);
            verify(modelRouter).select(request, RouteGroup.RoutingStrategy.WEIGHTED);

            dispatcher.send(request, RouteGroup.RoutingStrategy.RANDOM);
            verify(modelRouter).select(request, RouteGroup.RoutingStrategy.RANDOM);

            dispatcher.send(request, RouteGroup.RoutingStrategy.FAILOVER);
            verify(modelRouter).select(request, RouteGroup.RoutingStrategy.FAILOVER);
        }
    }

    @Nested
    @DisplayName("sendStream 方法测试")
    class SendStreamTests {

        @Test
        @DisplayName("发送流式请求成功")
        void sendStream_validRequest_callsAdapter() {
            // given
            LLMRequest request = createTestRequest();
            Consumer<String> onChunk = s -> {};
            Runnable onComplete = () -> {};
            Consumer<Throwable> onError = e -> {};

            when(modelRouter.select(request, RouteGroup.RoutingStrategy.WEIGHTED)).thenReturn(providerPort);
            when(providerPort.getProviderCode()).thenReturn("openai");
            when(streamCallbackFactory.create(onChunk, onComplete, onError)).thenReturn(streamCallback);
            doNothing().when(providerPort).chatStream(any(), any());

            // when
            dispatcher.sendStream(request, RouteGroup.RoutingStrategy.WEIGHTED, onChunk, onComplete, onError);

            // then
            verify(modelRouter).select(request, RouteGroup.RoutingStrategy.WEIGHTED);
            verify(streamCallbackFactory).create(onChunk, onComplete, onError);
            verify(providerPort).chatStream(eq(request), eq(streamCallback));
        }

        @Test
        @DisplayName("发送流式请求（简化版本）使用默认回调")
        void sendStream_simpleVersion_usesDefaultCallbacks() {
            // given
            LLMRequest request = createTestRequest();
            Consumer<String> onChunk = s -> {};

            when(modelRouter.select(any(LLMRequest.class), any())).thenReturn(providerPort);
            when(providerPort.getProviderCode()).thenReturn("openai");
            when(streamCallbackFactory.create(any(), any(), any())).thenReturn(streamCallback);
            doNothing().when(providerPort).chatStream(any(), any());

            // when
            dispatcher.sendStream(request, RouteGroup.RoutingStrategy.WEIGHTED, onChunk);

            // then
            verify(modelRouter).select(request, RouteGroup.RoutingStrategy.WEIGHTED);
            verify(streamCallbackFactory).create(eq(onChunk), any(Runnable.class), any(Consumer.class));
            verify(providerPort).chatStream(eq(request), eq(streamCallback));
        }
    }

    // Helper methods
    private LLMRequest createTestRequest() {
        return LLMRequest.builder()
            .model("gpt-4")
            .build();
    }

    private LLMResponse createTestResponse() {
        return LLMResponse.builder()
            .id("chatcmpl-123")
            .model("gpt-4")
            .build();
    }
}
