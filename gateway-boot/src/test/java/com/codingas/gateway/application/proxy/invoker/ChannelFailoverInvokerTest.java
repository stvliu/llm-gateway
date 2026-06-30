package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.OutboundTuner;
import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.common.event.FailoverOccurredEvent;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

    /** 测试用画像：启用 L2 降级（替换原硬编码 true 占位，Task 4.9 profile 贯穿） */
    private static final ResilienceProfile L2_PROFILE = profile(true);

    @Mock
    private KeyFailoverInvoker keyFailoverInvoker;

    @Mock
    private ErrorClassifier errorClassifier;

    @Mock
    private DegradationService degradationService;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private OutboundTuner outboundTuner;

    @Mock
    private ProtocolConverter protocolConverter;

    private ChannelFailoverInvoker invoker;

    private RoutingContext ctx1;
    private RoutingContext ctx2;
    private ProtocolRequest request;

    @BeforeEach
    void setUp() {
        invoker = new ChannelFailoverInvoker(keyFailoverInvoker, errorClassifier, degradationService,
                eventPublisher, channelGateway, outboundTuner, protocolConverter);

        ctx1 = new RoutingContext(10L, 20L, "https://ch1.example.com/v1",
                Protocol.OPENAI, "sk-1", 60, false, "gpt-4o", null);
        ctx2 = new RoutingContext(11L, 21L, "https://ch2.example.com/v1",
                Protocol.OPENAI, "sk-2", 60, false, "gpt-4o", null);

        request = mock(ProtocolRequest.class);
        lenient().when(request.getModel()).thenReturn("gpt-4o");
        // 调谐下沉：invoker 每候选对原始 request 副本做 convert+tune。
        // 单元测试聚焦转移决策，copy/tune 用「返回自身」桩保持 invoke(ctx, request) 匹配既有断言。
        lenient().when(request.copy()).thenReturn(request);
        lenient().when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class)))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    @DisplayName("第一个候选成功时直接返回")
    void firstCandidateSuccess_returns() {
        ProtocolResponse expectedResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx1, request)).thenReturn(expectedResponse);

        ProtocolResponse result = invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id");

        assertThat(result).isSameAs(expectedResponse);
        verify(keyFailoverInvoker, never()).invoke(ctx2, request);
        verify(degradationService, never()).degrade(anyString(), any(), any());
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
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id");

        assertThat(result).isSameAs(successResponse);
        verify(keyFailoverInvoker).invoke(ctx1, request);
        verify(keyFailoverInvoker).invoke(ctx2, request);
        verify(degradationService, never()).degrade(anyString(), any(), any());
    }

    @Test
    @DisplayName("L1 候选全部 UNKNOWN_ERROR 耗尽后进入 L2 降级")
    void l1_exhausted_thenL2() {
        // 所有候选 UNKNOWN_ERROR（L2 类，模型能力问题）耗尽，degrade 返回 fallback，
        // 抛出携带 fallback 模型名的异常。reason 用 UNKNOWN_ERROR 符合 4.8 分流表（L2 类才触发模型降级）
        ProviderException modelEx = new ProviderException(
                ProviderErrorType.UNKNOWN_ERROR, "model fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(modelEx);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenThrow(modelEx);
        when(errorClassifier.classify(ProviderErrorType.UNKNOWN_ERROR))
                .thenReturn(FailoverDecision.L2);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, L2_PROFILE))
                .thenReturn("gpt-3.5-turbo");

        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id"))
                .isInstanceOf(L2DegradationRequiredException.class)
                .extracting(e -> ((L2DegradationRequiredException) e).getFallbackModel())
                .isEqualTo("gpt-3.5-turbo");

        // 显式证明 ctx2 被试过（L1 全耗尽才进 L2）
        verify(keyFailoverInvoker).invoke(ctx1, request);
        verify(keyFailoverInvoker).invoke(ctx2, request);
        verify(degradationService).degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, L2_PROFILE);
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
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id"))
                .isSameAs(invalidEx);

        verify(keyFailoverInvoker, never()).invoke(ctx2, request);
        verify(degradationService, never()).degrade(anyString(), any(), any());
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

        // 画像关闭 L2 门禁（enableL2ModelDegradation=false），L1 耗尽不进 L2
        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, profile(false), "test-trace-id"))
                .isSameAs(authEx);

        verify(degradationService, never()).degrade(anyString(), any(), any());
    }

    @Test
    @DisplayName("L2 degrade 返回 null 时抛最后捕获的异常")
    void l2_degradeReturnsNull_throwsLast() {
        ProviderException modelEx = new ProviderException(
                ProviderErrorType.UNKNOWN_ERROR, "model fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(modelEx);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenThrow(modelEx);
        when(errorClassifier.classify(ProviderErrorType.UNKNOWN_ERROR))
                .thenReturn(FailoverDecision.L2);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, L2_PROFILE))
                .thenReturn(null);

        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id"))
                .isSameAs(modelEx);
    }

    @Test
    @DisplayName("L2 degrade 抛异常时防御捕获，抛回 lastException 保留上下文（非 degrade 异常）")
    void l2_degradeThrowsException_throwsLast() {
        // DegradationServiceImpl 违背 DegradationService.degrade 接口契约（"无可用备选返回 null"），
        // 实际在"有链但所有备选不可用"时抛 ProviderException。tryL2Degradation 应防御捕获，
        // 让调用方抛 lastException 保留原始失败上下文，而非让 degrade 异常传播。
        ProviderException modelEx = new ProviderException(
                ProviderErrorType.UNKNOWN_ERROR, "model fail");
        ProviderException degradeEx = new ProviderException(
                ProviderErrorType.UPSTREAM_ERROR, "ALL_MODELS_DEGRADED: 所有备选均不可用");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(modelEx);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenThrow(modelEx);
        when(errorClassifier.classify(ProviderErrorType.UNKNOWN_ERROR))
                .thenReturn(FailoverDecision.L2);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, L2_PROFILE))
                .thenThrow(degradeEx);

        // 防御后应抛 lastException（modelEx），而非 degrade 抛出的 degradeEx
        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id"))
                .isSameAs(modelEx);

        verify(keyFailoverInvoker).invoke(ctx1, request);
        verify(keyFailoverInvoker).invoke(ctx2, request);
        verify(degradationService).degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, L2_PROFILE);
    }

    @Test
    @DisplayName("流式：第一个候选成功直接返回不试下一候选")
    void stream_firstCandidateSuccess() {
        StreamCallback callback = mock(StreamCallback.class);

        invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback);

        verify(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        verify(keyFailoverInvoker, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService, never()).degrade(anyString(), any(), any());
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
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback);

        verify(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        verify(keyFailoverInvoker).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService, never()).degrade(anyString(), any(), any());
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
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback))
                .isSameAs(invalidEx);

        verify(keyFailoverInvoker, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService, never()).degrade(anyString(), any(), any());
    }

    @Test
    @DisplayName("流式：L1 候选全部启动失败后进入 L2 降级")
    void stream_l1Exhausted_thenL2() {
        ProviderException modelEx = new ProviderException(
                ProviderErrorType.UNKNOWN_ERROR, "model fail");
        doThrow(modelEx).when(keyFailoverInvoker)
                .invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        doThrow(modelEx).when(keyFailoverInvoker)
                .invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        when(errorClassifier.classify(ProviderErrorType.UNKNOWN_ERROR))
                .thenReturn(FailoverDecision.L2);
        when(degradationService.degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, L2_PROFILE))
                .thenReturn("gpt-3.5-turbo");

        StreamCallback callback = mock(StreamCallback.class);

        assertThatThrownBy(() -> invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback))
                .isInstanceOf(L2DegradationRequiredException.class)
                .extracting(e -> ((L2DegradationRequiredException) e).getFallbackModel())
                .isEqualTo("gpt-3.5-turbo");

        // 显式证明 ctx2 被试过（L1 全耗尽才进 L2）
        verify(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        verify(keyFailoverInvoker).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService).degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, L2_PROFILE);
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
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback))
                .isSameAs(afterFirstByteEx);

        verify(keyFailoverInvoker).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        verify(keyFailoverInvoker, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        verify(degradationService, never()).degrade(anyString(), any(), any());
        // 首字节已转发给原 callback（包装 callback 透传）
        verify(callback).onChunk("first-byte-data");
    }

    // ==================== clusterId 过滤生效（Task 4.11c 修复） ====================

    @Test
    @DisplayName("转移事件 clusterId 反查填充：L1 转移发布事件的 fromClusterId/toClusterId 由 ChannelGateway 反查填充（非 null）")
    void failover_eventClusterId_populatedFromChannelGateway() {
        // 场景：ch1(channelId=10, clusterId=100) AUTH 共因失败 → L1 → 换 ch2(channelId=11, clusterId=200) 成功
        // 修复前：publishFailoverEvent 硬编码 fromClusterId/toClusterId = null → clusterId 过滤失效
        // 修复后：Invoker 通过 ChannelGateway.findById 反查 channelId→clusterId 填充事件字段
        Channel ch1 = channelWithCluster(10L, 100L);
        Channel ch2 = channelWithCluster(11L, 200L);
        when(channelGateway.findById(10L)).thenReturn(Optional.of(ch1));
        when(channelGateway.findById(11L)).thenReturn(Optional.of(ch2));

        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);
        ProtocolResponse successResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenReturn(successResponse);

        invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id");

        // 断言：事件 fromClusterId/toClusterId 由 ChannelGateway 反查填充，非 null
        ArgumentCaptor<FailoverOccurredEvent> captor = ArgumentCaptor.forClass(FailoverOccurredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        FailoverOccurredEvent event = captor.getValue();
        assertThat(event.fromClusterId())
                .as("fromClusterId 应由 ChannelGateway 反查填充（clusterId=100），修复前恒为 null 导致过滤失效")
                .isEqualTo(100L);
        assertThat(event.toClusterId())
                .as("toClusterId 应由 ChannelGateway 反查填充（clusterId=200）")
                .isEqualTo(200L);
    }

    @Test
    @DisplayName("转移事件 clusterId 反查填充：耗尽场景 toChannelId 为 null 时 toClusterId 也为 null")
    void failover_exhaustedEvent_toClusterIdNullWhenNoTarget() {
        // 场景：ch1(channelId=10, clusterId=100) 失败 → L1 → 无下一候选（exhausted=true, toChannelId=null）
        // 期望：fromClusterId=100（反查填充），toClusterId=null（无目标渠道，不反查）
        Channel ch1 = channelWithCluster(10L, 100L);
        when(channelGateway.findById(10L)).thenReturn(Optional.of(ch1));

        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);

        // 单候选耗尽 → exhausted=true, toChannelId=null
        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1),
                request, Protocol.OPENAI, 7L, profile(false), "test-trace-id"))
                .isSameAs(authEx);

        ArgumentCaptor<FailoverOccurredEvent> captor = ArgumentCaptor.forClass(FailoverOccurredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        FailoverOccurredEvent event = captor.getValue();
        assertThat(event.fromClusterId()).isEqualTo(100L);
        assertThat(event.toChannelId()).isNull();
        assertThat(event.toClusterId())
                .as("无目标渠道时 toClusterId 应为 null，不触发反查")
                .isNull();
        assertThat(event.exhausted()).isTrue();
    }

    @Test
    @DisplayName("转移事件 clusterId 容错：channelId 查不到 channel 时 clusterId 填 null（不阻塞发布）")
    void failover_channelNotFound_clusterIdNull_doesNotBlock() {
        // 场景：ch1(channelId=10) 失败 → L1 → 换 ch2 成功，但 ChannelGateway.findById 返回空（channel 不存在）
        // 期望：clusterId 填 null（容错），事件仍正常发布
        when(channelGateway.findById(10L)).thenReturn(Optional.empty());
        when(channelGateway.findById(11L)).thenReturn(Optional.empty());

        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);
        ProtocolResponse successResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenReturn(successResponse);

        invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id");

        ArgumentCaptor<FailoverOccurredEvent> captor = ArgumentCaptor.forClass(FailoverOccurredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        FailoverOccurredEvent event = captor.getValue();
        // 容错：查不到 channel 时 clusterId 填 null，事件仍发布
        assertThat(event.fromClusterId()).isNull();
        assertThat(event.toClusterId()).isNull();
    }

    /** 构造测试用 Channel（带 clusterId） */
    private Channel channelWithCluster(Long channelId, Long clusterId) {
        Channel ch = new Channel();
        ch.setId(channelId);
        ch.setClusterId(clusterId);
        return ch;
    }

    // ==================== 转移事件发布（Task 4.11c） ====================

    @Test
    @DisplayName("转移事件发布：L1 换候选时发布 FailoverOccurredEvent（from=失败候选, to=下一候选）")
    void failover_l1Decision_publishesFailoverEvent() {
        // ch1 AUTH 共因失败 → L1 决策 → 换 ch2 成功 → 应发布 1 条转移事件
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);
        ProtocolResponse successResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenReturn(successResponse);

        invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id");

        // 断言：发布 1 条转移事件，from=ctx1, to=ctx2, decision=L1, exhausted=false
        ArgumentCaptor<FailoverOccurredEvent> captor = ArgumentCaptor.forClass(FailoverOccurredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        FailoverOccurredEvent event = captor.getValue();
        assertThat(event.fromChannelId()).isEqualTo(10L);
        assertThat(event.fromEndpointId()).isEqualTo(20L);
        assertThat(event.toChannelId()).isEqualTo(11L);
        assertThat(event.toEndpointId()).isEqualTo(21L);
        assertThat(event.errorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
        assertThat(event.decision()).isEqualTo(FailoverDecision.L1);
        assertThat(event.exhausted()).isFalse();
        assertThat(event.applicationId()).isEqualTo(7L);
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    @DisplayName("转移事件发布：NONE 决策不发布事件（请求级错误不转移）")
    void failover_noneDecision_doesNotPublishEvent() {
        // ch1 INVALID_REQUEST → NONE 决策 → 直接抛不转移 → 不应发布事件
        ProviderException invalidEx = new ProviderException(
                ProviderErrorType.INVALID_REQUEST, "bad request");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(invalidEx);
        when(errorClassifier.classify(ProviderErrorType.INVALID_REQUEST))
                .thenReturn(FailoverDecision.NONE);

        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id"))
                .isSameAs(invalidEx);

        // 断言：NONE 不发布任何转移事件
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("转移事件发布：首候选成功不发布事件（无转移发生）")
    void failover_firstCandidateSuccess_doesNotPublishEvent() {
        // ch1 首选成功 → 无转移 → 不应发布事件
        ProtocolResponse successResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx1, request)).thenReturn(successResponse);

        invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id");

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("转移事件发布：全部候选耗尽且未降级时发布 exhausted=true 事件（to 为 null）")
    void failover_allExhaustedNoL2_publishesExhaustedEvent() {
        // 两候选均 AUTH 失败耗尽，L2 门禁关闭 → 抛最后异常，应发布 exhausted 事件（to=null）
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);

        assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, profile(false), "test-trace-id"))
                .isSameAs(authEx);

        // 断言：发布 2 条事件（ctx1→ctx2 转移 + ctx2 耗尽），最后一条 exhausted=true, to=null
        ArgumentCaptor<FailoverOccurredEvent> captor = ArgumentCaptor.forClass(FailoverOccurredEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        java.util.List<FailoverOccurredEvent> events = captor.getAllValues();
        // 第 1 条：ctx1 → ctx2 转移
        assertThat(events.get(0).fromChannelId()).isEqualTo(10L);
        assertThat(events.get(0).toChannelId()).isEqualTo(11L);
        assertThat(events.get(0).exhausted()).isFalse();
        // 第 2 条：ctx2 耗尽（已是最后候选），to=null
        assertThat(events.get(1).fromChannelId()).isEqualTo(11L);
        assertThat(events.get(1).toChannelId()).isNull();
        assertThat(events.get(1).toEndpointId()).isNull();
        assertThat(events.get(1).exhausted()).isTrue();
    }

    @Test
    @DisplayName("转移事件发布：流式首字节前转移发布事件")
    void failover_streamBeforeFirstByte_publishesEvent() {
        // ch1 流式启动失败（首字节前）→ L1 决策 → 换 ch2 成功 → 应发布 1 条事件
        ProviderException startupEx = new ProviderException(
                ProviderErrorType.UPSTREAM_ERROR, "启动失败");
        doThrow(startupEx).when(keyFailoverInvoker)
                .invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        when(errorClassifier.classify(ProviderErrorType.UPSTREAM_ERROR))
                .thenReturn(FailoverDecision.L1);

        StreamCallback callback = mock(StreamCallback.class);
        invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback);

        ArgumentCaptor<FailoverOccurredEvent> captor = ArgumentCaptor.forClass(FailoverOccurredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().fromChannelId()).isEqualTo(10L);
        assertThat(captor.getValue().toChannelId()).isEqualTo(11L);
        assertThat(captor.getValue().decision()).isEqualTo(FailoverDecision.L1);
    }

    // ==================== traceId 透传（4.11c 技术债偿还） ====================

    @Test
    @DisplayName("转移事件 traceId 透传：invoke 传入的 traceId 填充到发布的事件（修复前恒为 null）")
    void failover_eventTraceId_propagatedFromInvoke() {
        // 修复前：publishFailoverEvent 硬编码 traceId=null（调用链暂未透传），事件 traceId 恒为 null
        // 修复后：invoke 接收 traceId 参数并透传给 publishFailoverEvent → 事件 traceId 等于传入值
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(ctx1, request)).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);
        ProtocolResponse successResponse = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(ctx2, request)).thenReturn(successResponse);

        invoker.invoke(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "trace-id-from-dispatch");

        ArgumentCaptor<FailoverOccurredEvent> captor = ArgumentCaptor.forClass(FailoverOccurredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().traceId())
                .as("traceId 应由 invoke 参数透传，修复前恒为 null 导致调用链关联断裂")
                .isEqualTo("trace-id-from-dispatch");
    }

    @Test
    @DisplayName("转移事件 traceId 透传：流式 invokeStream 传入的 traceId 同样填充到事件")
    void failover_eventTraceId_propagatedFromInvokeStream() {
        // ch1 流式启动失败（首字节前）→ L1 决策 → 换 ch2 成功 → 发布 1 条事件，traceId 应透传
        ProviderException startupEx = new ProviderException(
                ProviderErrorType.UPSTREAM_ERROR, "启动失败");
        doThrow(startupEx).when(keyFailoverInvoker)
                .invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
        when(errorClassifier.classify(ProviderErrorType.UPSTREAM_ERROR))
                .thenReturn(FailoverDecision.L1);

        StreamCallback callback = mock(StreamCallback.class);
        invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                request, Protocol.OPENAI, 7L, L2_PROFILE, "stream-trace-id", callback);

        ArgumentCaptor<FailoverOccurredEvent> captor = ArgumentCaptor.forClass(FailoverOccurredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().traceId())
                .as("流式调用链 traceId 应同样透传到事件")
                .isEqualTo("stream-trace-id");
    }

    // ==================== 调谐下沉：copy 隔离（Task 2.2） ====================

    @Test
    @DisplayName("OpenAIChatRequest.copy 返回等值独立副本（手写字段拷贝）")
    void openaiRequest_copy_returnsIndependentCopy() {
        OpenAIChatRequest original = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .maxTokens(128)
                .temperature(0.7)
                .stop(List.of("END"))
                .tools(List.of(java.util.Map.of("function", java.util.Map.of("name", "fn"))))
                .toolChoice("auto")
                .stream(true)
                .build();

        OpenAIChatRequest copy = original.copy();

        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getModel()).isEqualTo("gpt-4o");
        assertThat(copy.getMaxTokens()).isEqualTo(128);
        assertThat(copy.getTemperature()).isEqualTo(0.7);
        assertThat(copy.getStop()).containsExactly("END");
        assertThat(copy.getToolChoice()).isEqualTo("auto");
        assertThat(copy.isStream()).isTrue();
        // 副本独立：修改 copy 的 model 不影响 original
        copy.setModel("claude-3");
        assertThat(original.getModel()).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("AnthropicMessagesRequest.copy 返回等值独立副本（手写字段拷贝）")
    void anthropicRequest_copy_returnsIndependentCopy() {
        AnthropicMessagesRequest original = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet")
                .messages(List.of(AnthropicMessagesRequest.Message.builder().role("user").content("hi").build()))
                .maxTokens(256)
                .system("sys")
                .temperature(0.5)
                .stopSequences(List.of("X"))
                .stream(true)
                .build();

        AnthropicMessagesRequest copy = original.copy();

        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getModel()).isEqualTo("claude-3-5-sonnet");
        assertThat(copy.getMaxTokens()).isEqualTo(256);
        assertThat(copy.getSystem()).isEqualTo("sys");
        assertThat(copy.getStopSequences()).containsExactly("X");
        assertThat(copy.isStream()).isTrue();
        copy.setModel("gpt-4o");
        assertThat(original.getModel()).isEqualTo("claude-3-5-sonnet");
    }

    // ==================== 调谐下沉：每候选独立 convert+tune（Task 2.1 / 2.3） ====================

    @Test
    @DisplayName("L1 换渠道后每候选独立调谐：备候选收到 request.model==备候选 upstreamModelName，原始请求不污染")
    void l1Failover_eachCandidateTunedIndependently() {
        // 真实 OutboundTuner（无协议调谐器，仅做模型名替换）验证真实调谐行为
        OutboundTuner realTuner = new OutboundTuner(List.of());
        ChannelFailoverInvoker invokerWithRealTuner = new ChannelFailoverInvoker(
                keyFailoverInvoker, errorClassifier, degradationService,
                eventPublisher, channelGateway, realTuner, protocolConverter);

        // 两候选 upstreamModelName 不同；主候选失败，备候选成功（同协议，聚焦模型名替换）
        RoutingContext primaryCtx = new RoutingContext(10L, 20L, "https://ch1/v1",
                Protocol.OPENAI, "sk-1", 60, false, "gpt-4o", "ch1-upstream-model");
        RoutingContext backupCtx = new RoutingContext(11L, 21L, "https://ch2/v1",
                Protocol.OPENAI, "sk-2", 60, false, "gpt-4o", "ch2-upstream-model");

        OpenAIChatRequest original = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .build();

        // 主候选 AUTH 共因失败 → L1 → 换备候选
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(eq(primaryCtx), any(ProtocolRequest.class))).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);

        ProtocolResponse success = mock(ProtocolResponse.class);
        when(keyFailoverInvoker.invoke(eq(backupCtx), any(ProtocolRequest.class))).thenReturn(success);

        invokerWithRealTuner.invoke(primaryCtx, List.of(primaryCtx, backupCtx),
                original, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id");

        // 断言：备候选收到的 request.model==备候选 upstreamModelName（每候选独立调谐）
        ArgumentCaptor<ProtocolRequest> captor = ArgumentCaptor.forClass(ProtocolRequest.class);
        verify(keyFailoverInvoker).invoke(eq(backupCtx), captor.capture());
        assertThat(captor.getValue().getModel())
                .as("L1 换渠道后应基于备候选 upstreamModelName 独立调谐，修复前恒为首候选调谐结果导致 model 错误")
                .isEqualTo("ch2-upstream-model");
        // 原始请求未被污染（copy 隔离）
        assertThat(original.getModel())
                .as("调谐应基于原始请求副本，原始请求 model 不被候选调谐覆盖")
                .isEqualTo("gpt-4o");
    }

    // ==================== 调谐下沉：非流式响应转换（与流式 buildStreamCallback 对称） ====================

    @Test
    @DisplayName("非流式跨协议换候选：基于实际成功候选(非主候选) 转换响应为入站协议格式")
    void nonStream_failoverCrossProtocol_convertsResponsePerSuccessfulCandidate() {
        // 真实 ProtocolConverter（验证真实响应转换）+ 真实 OutboundTuner（仅模型名替换）
        ProtocolConverter realConverter = new ProtocolConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        OutboundTuner realTuner = new OutboundTuner(List.of());
        ChannelFailoverInvoker invokerReal = new ChannelFailoverInvoker(
                keyFailoverInvoker, errorClassifier, degradationService,
                eventPublisher, channelGateway, realTuner, realConverter);

        // inbound=OPENAI；主候选 OpenAI 上游（同协议）失败，备候选 Anthropic 上游（跨协议）成功
        RoutingContext openaiCtx = new RoutingContext(10L, 20L, "https://ch1/v1",
                Protocol.OPENAI, "sk-1", 60, false, "gpt-4o", "ch1-model");
        RoutingContext anthropicCtx = new RoutingContext(11L, 21L, "https://ch2/v1",
                Protocol.ANTHROPIC, "sk-2", 60, true, "gpt-4o", "ch2-model");

        OpenAIChatRequest original = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .build();

        // 主候选(OpenAI 同协议) AUTH 共因失败 → L1 → 换备候选(Anthropic 跨协议)
        ProviderException authEx = new ProviderException(
                ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
        when(keyFailoverInvoker.invoke(eq(openaiCtx), any(ProtocolRequest.class))).thenThrow(authEx);
        when(errorClassifier.classify(ProviderErrorType.AUTHENTICATION_ERROR))
                .thenReturn(FailoverDecision.L1);

        // 备候选(Anthropic 上游)成功：返回 Anthropic 格式响应（content blocks）
        AnthropicMessagesResponse upstreamResponse = AnthropicMessagesResponse.builder()
                .id("msg-123").model("claude-3-5-sonnet")
                .content(List.of(AnthropicMessagesResponse.ContentBlock.builder()
                        .type("text").text("hi").build()))
                .build();
        when(keyFailoverInvoker.invoke(eq(anthropicCtx), any(ProtocolRequest.class))).thenReturn(upstreamResponse);

        ProtocolResponse result = invokerReal.invoke(openaiCtx, List.of(openaiCtx, anthropicCtx),
                original, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id");

        // 断言：备候选 Anthropic 响应被转换为入站 OpenAI 格式（choices 非空，含 message.content）
        // 修复前 invoker 不做响应转换，返回原始 AnthropicMessagesResponse（content blocks，无 choices）
        assertThat(result)
                .as("跨协议换候选应基于成功候选(Anthropic)→inbound(OpenAI) 转换响应，修复前返回未转换的 Anthropic 响应")
                .isInstanceOf(OpenAIChatResponse.class);
        OpenAIChatResponse openaiResult = (OpenAIChatResponse) result;
        assertThat(openaiResult.getChoices()).isNotNull().isNotEmpty();
        assertThat(openaiResult.getChoices().get(0).getMessage().getContent()).isEqualTo("hi");
    }

    // ==================== 调谐下沉：流式 chunk 转换方向重建（Task 2.5） ====================

    @Test
    @DisplayName("流式跨协议候选：基于实际候选 upstreamProtocol 转换 chunk（修复前 invoker 不转换）")
    void stream_crossProtocolCandidate_convertsChunkPerCandidate() {
        // 真实 ProtocolConverter（验证真实 chunk 转换）+ 真实 OutboundTuner（仅模型名替换）
        ProtocolConverter realConverter = new ProtocolConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        OutboundTuner realTuner = new OutboundTuner(List.of());
        ChannelFailoverInvoker invokerReal = new ChannelFailoverInvoker(
                keyFailoverInvoker, errorClassifier, degradationService,
                eventPublisher, channelGateway, realTuner, realConverter);

        // inbound=OPENAI；候选为 Anthropic 上游（跨协议）
        RoutingContext anthropicCtx = new RoutingContext(10L, 20L, "https://ch1/v1",
                Protocol.ANTHROPIC, "sk-1", 60, true, "gpt-4o", "ch1-model");

        OpenAIChatRequest original = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .build();

        // 候选成功：发一个 Anthropic 格式 chunk（content_block_delta）
        String anthropicChunk = "{\"type\":\"content_block_delta\",\"delta\":{\"text\":\"hi\"}}";
        doAnswer(inv -> {
            StreamCallback cb = inv.getArgument(2, StreamCallback.class);
            cb.onChunk(anthropicChunk);
            return null;
        }).when(keyFailoverInvoker).invokeStream(eq(anthropicCtx), any(ProtocolRequest.class), any(StreamCallback.class));

        StreamCallback callback = mock(StreamCallback.class);
        invokerReal.invokeStream(anthropicCtx, List.of(anthropicCtx),
                original, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback);

        // 断言：Anthropic 上游 chunk 被转换为 OpenAI 格式（含 chat.completion.chunk）
        // 修复前 invoker 不做 chunk 转换，callback 收到原始 Anthropic chunk（含 content_block_delta）
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(callback).onChunk(captor.capture());
        assertThat(captor.getValue())
                .as("跨协议候选应基于候选 upstreamProtocol(anthropic)→inbound(openai) 转换 chunk")
                .contains("chat.completion.chunk")
                .doesNotContain("content_block_delta");
    }

    @Test
    @DisplayName("流式跨协议换候选：基于实际成功候选(非主候选) upstreamProtocol 重建转换方向")
    void stream_failoverDifferentProtocol_usesSuccessfulCandidateDirection() {
        ProtocolConverter realConverter = new ProtocolConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        OutboundTuner realTuner = new OutboundTuner(List.of());
        ChannelFailoverInvoker invokerReal = new ChannelFailoverInvoker(
                keyFailoverInvoker, errorClassifier, degradationService,
                eventPublisher, channelGateway, realTuner, realConverter);

        // inbound=OPENAI；主候选 Anthropic 上游（跨协议）失败，备候选 OpenAI 上游（同协议）成功
        RoutingContext anthropicCtx = new RoutingContext(10L, 20L, "https://ch1/v1",
                Protocol.ANTHROPIC, "sk-1", 60, true, "gpt-4o", "ch1-model");
        RoutingContext openaiCtx = new RoutingContext(11L, 21L, "https://ch2/v1",
                Protocol.OPENAI, "sk-2", 60, false, "gpt-4o", "ch2-model");

        OpenAIChatRequest original = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .build();

        // 主候选(Anthropic)启动失败（首字节前）→ L1 → 换备候选(OpenAI)
        ProviderException startupEx = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "启动失败");
        doThrow(startupEx).when(keyFailoverInvoker)
                .invokeStream(eq(anthropicCtx), any(ProtocolRequest.class), any(StreamCallback.class));
        when(errorClassifier.classify(ProviderErrorType.UPSTREAM_ERROR))
                .thenReturn(FailoverDecision.L1);

        // 备候选(OpenAI 上游 == inbound)成功：发 OpenAI 格式 chunk（同协议，不应转换）
        String openaiChunk = "{\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}";
        doAnswer(inv -> {
            StreamCallback cb = inv.getArgument(2, StreamCallback.class);
            cb.onChunk(openaiChunk);
            return null;
        }).when(keyFailoverInvoker).invokeStream(eq(openaiCtx), any(ProtocolRequest.class), any(StreamCallback.class));

        StreamCallback callback = mock(StreamCallback.class);
        invokerReal.invokeStream(anthropicCtx, List.of(anthropicCtx, openaiCtx),
                original, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback);

        // 断言：备候选 OpenAI 上游==inbound（同协议），chunk 透传不转换
        // 若错误按主候选(Anthropic)方向转换，会把 OpenAI chunk 当 Anthropic 解析 → 返回 null → chunk 丢失
        verify(callback).onChunk(openaiChunk);
    }

    @Test
    @DisplayName("流式跨协议换候选：主同协议失败→备跨协议成功，chunk 被转换为入站协议格式（与 2.5b 对称方向）")
    void stream_failoverFromSameToCrossProtocol_convertsChunkToInbound() {
        // 真实 ProtocolConverter + 真实 OutboundTuner
        ProtocolConverter realConverter = new ProtocolConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        OutboundTuner realTuner = new OutboundTuner(List.of());
        ChannelFailoverInvoker invokerReal = new ChannelFailoverInvoker(
                keyFailoverInvoker, errorClassifier, degradationService,
                eventPublisher, channelGateway, realTuner, realConverter);

        // inbound=OPENAI；主候选 OpenAI 上游（同协议）失败，备候选 Anthropic 上游（跨协议）成功
        RoutingContext openaiCtx = new RoutingContext(10L, 20L, "https://ch1/v1",
                Protocol.OPENAI, "sk-1", 60, false, "gpt-4o", "ch1-model");
        RoutingContext anthropicCtx = new RoutingContext(11L, 21L, "https://ch2/v1",
                Protocol.ANTHROPIC, "sk-2", 60, true, "gpt-4o", "ch2-model");

        OpenAIChatRequest original = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .build();

        // 主候选(OpenAI 同协议)启动失败（首字节前）→ L1 → 换备候选(Anthropic 跨协议)
        ProviderException startupEx = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "启动失败");
        doThrow(startupEx).when(keyFailoverInvoker)
                .invokeStream(eq(openaiCtx), any(ProtocolRequest.class), any(StreamCallback.class));
        when(errorClassifier.classify(ProviderErrorType.UPSTREAM_ERROR))
                .thenReturn(FailoverDecision.L1);

        // 备候选(Anthropic 上游)成功：发 Anthropic 格式 chunk（content_block_delta）
        String anthropicChunk = "{\"type\":\"content_block_delta\",\"delta\":{\"text\":\"hi\"}}";
        doAnswer(inv -> {
            StreamCallback cb = inv.getArgument(2, StreamCallback.class);
            cb.onChunk(anthropicChunk);
            return null;
        }).when(keyFailoverInvoker).invokeStream(eq(anthropicCtx), any(ProtocolRequest.class), any(StreamCallback.class));

        StreamCallback callback = mock(StreamCallback.class);
        invokerReal.invokeStream(openaiCtx, List.of(openaiCtx, anthropicCtx),
                original, Protocol.OPENAI, 7L, L2_PROFILE, "test-trace-id", callback);

        // 断言：备候选 Anthropic chunk 被转换为入站 OpenAI 格式（含 chat.completion.chunk）
        // 与 2.5b 对称：2.5b 是主跨协议失败→备同协议成功（chunk 透传）；
        // 本测试是主同协议失败→备跨协议成功（chunk 转换为入站协议）
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(callback).onChunk(captor.capture());
        assertThat(captor.getValue())
                .as("主同协议失败→备跨协议成功，chunk 应按成功候选(Anthropic)→inbound(OpenAI) 转换")
                .contains("chat.completion.chunk")
                .doesNotContain("content_block_delta");
    }

    /** 构造测试用画像：enableL2 控制是否启用 L2 模型降级门禁（Task 4.9 profile 贯穿） */
    private static ResilienceProfile profile(boolean enableL2) {
        ResilienceProfile p = new ResilienceProfile();
        p.setEnableL2ModelDegradation(enableL2);
        p.setDegradationMaxDepth(5);
        return p;
    }
}
