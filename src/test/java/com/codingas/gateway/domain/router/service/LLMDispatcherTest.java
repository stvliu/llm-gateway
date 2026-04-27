package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.gateway.LLMProviderPort;
import com.codingas.gateway.domain.router.gateway.ModelRouter;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

/**
 * LLMDispatcher 单元测试 (响应式版本)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LLMDispatcher")
class LLMDispatcherTest {

    @Mock
    private ModelRouter modelRouter;

    @Mock
    private LLMProviderPort providerPort;

    @Mock
    private LLMResponse llmResponse;

    private LLMDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new LLMDispatcher(modelRouter);
    }

    @Test
    @DisplayName("send 应调用 ModelRouter.select 并返回 Mono")
    void send_delegatesToRouter() {
        LLMRequest request = LLMRequest.builder()
                .model("openai/gpt-4o")
                .build();

        when(modelRouter.select(request, RouteGroup.RoutingStrategy.RANDOM)).thenReturn(providerPort);
        when(providerPort.chat(request)).thenReturn(Mono.just(llmResponse));

        Mono<LLMResponse> result = dispatcher.send(request, RouteGroup.RoutingStrategy.RANDOM);

        assertThat(result.block()).isSameAs(llmResponse);
        verify(modelRouter).select(request, RouteGroup.RoutingStrategy.RANDOM);
        verify(providerPort).chat(request);
    }

    @Test
    @DisplayName("sendStream 应调用 ModelRouter.select 并传递回调")
    @SuppressWarnings("unchecked")
    void sendStream_delegatesToRouter() {
        LLMRequest request = LLMRequest.builder()
                .model("openai/gpt-4o")
                .build();
        Consumer<String> onChunk = mock(Consumer.class);

        when(modelRouter.select(request, RouteGroup.RoutingStrategy.RANDOM)).thenReturn(providerPort);
        when(providerPort.chatStream(eq(request), any(StreamCallback.class))).thenReturn(Mono.empty());

        dispatcher.sendStream(request, RouteGroup.RoutingStrategy.RANDOM, onChunk);

        // 验证调用了 router 选择 provider
        verify(modelRouter).select(request, RouteGroup.RoutingStrategy.RANDOM);
    }
}
