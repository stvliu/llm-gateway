package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** DegradationInvoker 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DegradationInvoker 单元测试")
class DegradationInvokerTest {

    @Mock
    private KeyFailoverInvoker keyFailoverInvoker;

    @Mock
    private DegradationService degradationService;

    @Mock
    private RoutingResolver routingResolver;

    private DegradationInvoker invoker;

    private RoutingContext ctx;
    private ProtocolRequest request;

    @BeforeEach
    void setUp() {
        invoker = new DegradationInvoker(keyFailoverInvoker, degradationService, routingResolver);

        ctx = new RoutingContext(10L, 20L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-test", 60, false, "test-model", null);

        request = mock(ProtocolRequest.class);
        lenient().when(request.getModel()).thenReturn("gpt-4o");
    }

    @Test
    @DisplayName("KeyFailoverInvoker 成功时直接返回")
    void keyFailoverSucceeds_returns() {
        ProtocolResponse expectedResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx, request)).thenReturn(expectedResponse);

        ProtocolResponse result = invoker.invoke(ctx, request, Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

        assertThat(result).isSameAs(expectedResponse);
        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("Key 全部失败时触发降级")
    void allKeysFailed_triggersDegradation() {
        ProviderException ex = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "上游错误");
        when(keyFailoverInvoker.invoke(ctx, request)).thenThrow(ex);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR)).thenReturn("gpt-3.5-turbo");

        RoutingContext fallbackCtx = new RoutingContext(11L, 21L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-fallback", 60, false, "gpt-3.5-turbo", null);

        when(routingResolver.resolve("gpt-3.5-turbo", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                .thenReturn(fallbackCtx);

        ProtocolResponse expectedResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(fallbackCtx, request)).thenReturn(expectedResponse);

        ProtocolResponse result = invoker.invoke(ctx, request, Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

        assertThat(result).isSameAs(expectedResponse);
        verify(degradationService).degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);
    }

    @Test
    @DisplayName("降级耗尽时抛出原异常")
    void degradationExhausted_throwsOriginal() {
        ProviderException ex = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "上游错误");
        when(keyFailoverInvoker.invoke(ctx, request)).thenThrow(ex);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR)).thenReturn(null);

        assertThatThrownBy(() -> invoker.invoke(ctx, request, Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                .isSameAs(ex);
    }

    @Test
    @DisplayName("流式调用：KeyFailoverInvoker 成功时直接返回")
    void invokeStream_succeeds() {
        StreamCallback callback = mock(StreamCallback.class);

        invoker.invokeStream(ctx, request, callback, Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

        verify(keyFailoverInvoker).invokeStream(ctx, request, callback);
        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("流式调用：失败时触发降级")
    void invokeStream_triggersDegradation() {
        ProviderException ex = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "上游错误");
        doThrow(ex).when(keyFailoverInvoker).invokeStream(eq(ctx), eq(request), any(StreamCallback.class));

        when(degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR)).thenReturn("gpt-3.5-turbo");

        RoutingContext fallbackCtx = new RoutingContext(11L, 21L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-fallback", 60, false, "gpt-3.5-turbo", null);

        when(routingResolver.resolve("gpt-3.5-turbo", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                .thenReturn(fallbackCtx);

        StreamCallback callback = mock(StreamCallback.class);

        invoker.invokeStream(ctx, request, callback, Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

        verify(degradationService).degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);
        verify(keyFailoverInvoker).invokeStream(fallbackCtx, request, callback);
    }
}
