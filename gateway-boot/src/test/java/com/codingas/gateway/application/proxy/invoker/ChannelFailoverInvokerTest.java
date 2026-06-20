package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChannelFailoverInvoker 单元测试
 *
 * <p>覆盖 L1 候选内逐个试、L2 模型降级分流、NONE/INVALID_REQUEST 不转移、
 * 流式首字节前转移等核心语义（D3/D5/深化点5）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelFailoverInvoker 单元测试")
class ChannelFailoverInvokerTest {

    @Mock
    private KeyFailoverInvoker keyFailoverInvoker;

    @Mock
    private ErrorClassifier errorClassifier;

    @Mock
    private DegradationService degradationService;

    private ChannelFailoverInvoker invoker;

    private RoutingContext ctx1;
    private RoutingContext ctx2;
    private ProtocolRequest request;

    @BeforeEach
    void setUp() {
        invoker = new ChannelFailoverInvoker(keyFailoverInvoker, errorClassifier, degradationService);

        ctx1 = new RoutingContext(10L, 20L, "https://ch1.example.com/v1",
                Protocol.OPENAI, "sk-1", 60, false, "gpt-4o", null);
        ctx2 = new RoutingContext(11L, 21L, "https://ch2.example.com/v1",
                Protocol.OPENAI, "sk-2", 60, false, "gpt-4o", null);

        request = mock(ProtocolRequest.class);
        lenient().when(request.getModel()).thenReturn("gpt-4o");
    }

    @Test
    @DisplayName("第一个候选成功时直接返回")
    void firstCandidateSuccess_returns() {
        ProtocolResponse expectedResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx1, request)).thenReturn(expectedResponse);

        ProtocolResponse result = invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true);

        assertThat(result).isSameAs(expectedResponse);
        verify(keyFailoverInvoker, never()).invoke(ctx2, request);
        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("L1：ch1 AUTH 共因失败后转移到 ch2 成功")
    void l1_failoverToNextCandidate() {
        // ch1 抛 AUTH（共因故障），ErrorClassifier 判定 L1 → 换下一候选
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);

        ProtocolResponse successResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenReturn(successResponse);

        ProtocolResponse result = invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true);

        assertThat(result).isSameAs(successResponse);
        verify(keyFailoverInvoker).invoke(ctx1, request);
        verify(keyFailoverInvoker).invoke(ctx2, request);
        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("L1 候选全部 AUTH 耗尽后进入 L2 降级")
    void l1_exhausted_thenL2() {
        // 所有候选 AUTH 耗尽，degrade 返回 fallback，抛出携带 fallback 模型名的异常
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn("gpt-3.5-turbo");

        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true))
                .isInstanceOf(ProviderException.class)
                .extracting(e -> ((ProviderException) e).getModel())
                .isEqualTo("gpt-3.5-turbo");

        // 显式证明 ctx2 被试过（L1 全耗尽才进 L2）
        verify(keyFailoverInvoker).invoke(ctx1, request);
        verify(keyFailoverInvoker).invoke(ctx2, request);
        verify(degradationService).degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("INVALID_REQUEST 绝不转移：ch1 失败直接抛出不试 ch2")
    void invalidRequest_noFailover() {
        ProviderException invalidEx = new ProviderException(
                ProviderErrorType.INVALID_REQUEST, "bad request");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(invalidEx);
        when(errorClassifier.classify(ProviderErrorType.INVALID_REQUEST))
                .thenReturn(FailoverDecision.NONE);

        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true))
                .isSameAs(invalidEx);

        verify(keyFailoverInvoker, never()).invoke(ctx2, request);
        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("L2 门禁关闭时 L1 耗尽直接抛最后异常，不调 degrade")
    void l2GateDisabled_throwsOriginal() {
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);

        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, false))
                .isSameAs(authEx);

        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("L2 degrade 返回 null 时抛最后捕获的异常")
    void l2_degradeReturnsNull_throwsLast() {
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(null);

        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true))
                .isSameAs(authEx);
    }

    @Test
    @DisplayName("L2 degrade 抛异常时防御捕获，抛回 lastException 保留上下文（非 degrade 异常）")
    void l2_degradeThrowsException_throwsLast() {
        // DegradationServiceImpl 违背 DegradationService.degrade 接口契约（"无可用备选返回 null"），
        // 实际在"有链但所有备选不可用"时抛 ProviderException。tryL2Degradation 应防御捕获，
        // 让调用方抛 lastException 保留原始失败上下文，而非让 degrade 异常传播。
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        ProviderException degradeEx = new ProviderException(
                ProviderErrorType.UPSTREAM_ERROR, "ALL_MODELS_DEGRADED: 所有备选均不可用");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR))
                .thenThrow(degradeEx);

        // 防御后应抛 lastException（authEx），而非 degrade 抛出的 degradeEx
        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true))
                .isSameAs(authEx);

        verify(keyFailoverInvoker).invoke(ctx1, request);
        verify(keyFailoverInvoker).invoke(ctx2, request);
        verify(degradationService).degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("流式：第一个候选成功直接返回不试下一候选")
    void stream_firstCandidateSuccess() {
        StreamCallback callback = mock(StreamCallback.class);

        invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true, callback);

        verify(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        verify(keyFailoverInvoker, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("流式：首字节前失败（启动失败）换下一候选")
    void streamOnlyBeforeFirstByte() {
        // ch1 流式启动失败（首字节前），L1 决策 → 换 ch2
        ProviderException startupEx = new ProviderException(
                ProviderErrorType.UPSTREAM_ERROR, "启动失败");
        doThrow(startupEx).when(keyFailoverInvoker)
                .invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        when(errorClassifier.classify(ProviderErrorType.UPSTREAM_ERROR))
                .thenReturn(FailoverDecision.L1);

        StreamCallback callback = mock(StreamCallback.class);

        // ch2 启动成功（invokeStream 默认 doNothing），流建立后 return，不再换候选
        invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true, callback);

        verify(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        verify(keyFailoverInvoker).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("流式：INVALID_REQUEST 启动失败直接抛不试下一候选")
    void stream_invalidRequest_noFailover() {
        ProviderException invalidEx = new ProviderException(
                ProviderErrorType.INVALID_REQUEST, "bad request");
        doThrow(invalidEx).when(keyFailoverInvoker)
                .invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        when(errorClassifier.classify(ProviderErrorType.INVALID_REQUEST))
                .thenReturn(FailoverDecision.NONE);

        StreamCallback callback = mock(StreamCallback.class);

        assertThatThrownBy(() -> invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true, callback))
                .isSameAs(invalidEx);

        verify(keyFailoverInvoker, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService, never()).degrade(anyString(), any());
    }

    @Test
    @DisplayName("流式：L1 候选全部启动失败后进入 L2 降级")
    void stream_l1Exhausted_thenL2() {
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        doThrow(authEx).when(keyFailoverInvoker)
                .invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        doThrow(authEx).when(keyFailoverInvoker)
                .invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn("gpt-3.5-turbo");

        StreamCallback callback = mock(StreamCallback.class);

        assertThatThrownBy(() -> invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true, callback))
                .isInstanceOf(ProviderException.class)
                .extracting(e -> ((ProviderException) e).getModel())
                .isEqualTo("gpt-3.5-turbo");

        // 显式证明 ctx2 被试过（L1 全耗尽才进 L2）
        verify(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        verify(keyFailoverInvoker).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService).degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("流式：首字节已发送后失败不换候选（首字节后转移边界）")
    void stream_afterFirstByte_noFailover() {
        // 场景：ch1 首字节已发送后同步失败（doAnswer 模拟先 onChunk 再抛异常）
        // 语义：首字节后失败不换渠道，直接抛传播给调用方，不试 ch2
        ProviderException afterFirstByteEx = new ProviderException(
                ProviderErrorType.UPSTREAM_ERROR, "首字节后失败");
        doAnswer(invocation -> {
            StreamCallback wrappedCallback = invocation.getArgument(2, StreamCallback.class);
            wrappedCallback.onChunk("first-byte-data");  // 首字节已发
            throw afterFirstByteEx;  // 首字节后同步失败
        }).when(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));

        StreamCallback callback = mock(StreamCallback.class);

        // 断言：不试 ch2，直接抛 afterFirstByteEx（首字节后转移边界）
        assertThatThrownBy(() -> invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, true, callback))
                .isSameAs(afterFirstByteEx);

        verify(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        verify(keyFailoverInvoker, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService, never()).degrade(anyString(), any());
        // 首字节已转发给原 callback（包装 callback 透传）
        verify(callback).onChunk("first-byte-data");
    }
}
