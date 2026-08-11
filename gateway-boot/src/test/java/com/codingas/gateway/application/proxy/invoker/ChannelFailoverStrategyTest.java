/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.proxy.OutboundTuner;
import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.common.event.FailoverOccurredEvent;
import com.codingas.gateway.domain.application.enums.FailureStrategy;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChannelFailoverInvoker 应用级 failureStrategy 分流测试（Task 7）
 *
 * <p>验证三种失败处理策略（FAIL_FAST/FAIL_RETRY/FAIL_OVER）与 null 回退 FAIL_RETRY 的
 * L0（同渠道换 Key）/L1（换渠道）行为：</p>
 * <ul>
 *   <li>FAIL_FAST：调 {@link KeyFailoverInvoker#invokeSingleKey} 首个 Key 失败立即抛，
 *       不换 Key 不换渠道</li>
 *   <li>FAIL_RETRY（默认）：调 {@link KeyFailoverInvoker#invoke} 试完同渠道所有 Key，
 *       Key 耗尽后不换渠道直接抛错</li>
 *   <li>FAIL_OVER：试完同渠道 Key 后换下一候选渠道，全耗尽抛错</li>
 *   <li>null：回退 FAIL_RETRY 行为（不换渠道）</li>
 * </ul>
 *
 * <p>策略递进关系：FAIL_FAST ⊂ FAIL_RETRY ⊂ FAIL_OVER。</p>
 */
@DisplayName("ChannelFailoverInvoker 应用级 failureStrategy 分流测试（Task 7）")
class ChannelFailoverStrategyTest {

    /**
     * 构造真实 ChannelFailoverInvoker，注入 mock KeyFailoverInvoker + 真实 ErrorClassifier
     *
     * <p>其他依赖（OutboundTuner/ProtocolConverter/DomainEventPublisher）用 mock/stub，
     * 聚焦策略分流语义非调谐/转换/事件发布。OutboundTuner.tune 用 returnsFirstArg 桩，
     * 匹配 invoker 调谐下沉后 keyFailoverInvoker.invoke/invokeSingleKey 收到的 request
     * 仍是同一个 mock 对象（断言匹配 any(ProtocolRequest.class)）。</p>
     *
     * <p>eventPublisher 用匿名 mock（不持有引用），适用于不需验证事件发布的行为测试。
     * 需验证事件发布时改用 {@link #newRealInvoker(KeyFailoverInvoker, DomainEventPublisher)}。</p>
     *
     * @param keyInvoker mock Key 级 Invoker（控制 invoke/invokeSingleKey 成功/失败）
     * @return 真实 ChannelFailoverInvoker 实例
     */
    private ChannelFailoverInvoker newRealInvoker(KeyFailoverInvoker keyInvoker) {
        return newRealInvoker(keyInvoker, mock(DomainEventPublisher.class));
    }

    /**
     * 构造真实 ChannelFailoverInvoker，注入 mock KeyFailoverInvoker + 真实 ErrorClassifier + 指定 eventPublisher
     *
     * <p>重载版本：用于验证转移事件发布行为（FAIL_RETRY 不发 / FAIL_OVER 发）。
     * 调用方持有 eventPublisher mock 引用，可对其 verify。其他依赖（OutboundTuner/ProtocolConverter）
     * 用 mock/stub。ErrorClassifier 用真实实例，其 classify 逻辑确定性映射
     * （RATE_LIMIT_ERROR→L1），无需 stub。</p>
     *
     * @param keyInvoker     mock Key 级 Invoker（控制 invoke/invokeStream 成功/失败）
     * @param eventPublisher mock 领域事件发布器（调用方持有引用以 verify publish 调用）
     * @return 真实 ChannelFailoverInvoker 实例
     */
    private ChannelFailoverInvoker newRealInvoker(KeyFailoverInvoker keyInvoker,
                                                   DomainEventPublisher eventPublisher) {
        ErrorClassifier errorClassifier = new ErrorClassifier();
        OutboundTuner tuner = mock(OutboundTuner.class);
        lenient().when(tuner.tune(any(ProtocolRequest.class), any(RoutingContext.class)))
                .thenAnswer(returnsFirstArg());
        return new ChannelFailoverInvoker(keyInvoker, errorClassifier,
                eventPublisher, tuner, mock(ProtocolConverter.class));
    }

    /**
     * 构造 RoutingContext（10 参数 record，failureStrategy 设为指定值）
     *
     * @param channelId 渠道 ID（endpointId = channelId + 10L）
     * @param strategy  应用级失败处理策略
     * @return 路由上下文
     */
    private RoutingContext ctx(long channelId, FailureStrategy strategy) {
        long endpointId = channelId + 10L;
        return new RoutingContext(channelId, endpointId, "https://ch" + channelId + ".example.com/v1",
                Protocol.OPENAI, "sk-" + channelId, 60, false, "gpt-4o", null, strategy);
    }

    /**
     * 构造 failureStrategy 为 null 的 RoutingContext（验证默认回退 FAIL_RETRY）
     *
     * @param channelId 渠道 ID
     * @return failureStrategy 为 null 的路由上下文
     */
    private RoutingContext ctxWithNullStrategy(long channelId) {
        long endpointId = channelId + 10L;
        return new RoutingContext(channelId, endpointId, "https://ch" + channelId + ".example.com/v1",
                Protocol.OPENAI, "sk-" + channelId, 60, false, "gpt-4o", null, null);
    }

    /**
     * 构造 mock ProtocolRequest（copy 返回自身，匹配 invoker 调谐下沉桩）
     *
     * @return mock 协议请求
     */
    private ProtocolRequest req() {
        ProtocolRequest request = mock(ProtocolRequest.class);
        lenient().when(request.getModel()).thenReturn("gpt-4o");
        lenient().when(request.copy()).thenReturn(request);
        return request;
    }

    /**
     * 构造 mock ProtocolResponse（成功响应占位）
     *
     * @return mock 协议响应
     */
    private ProtocolResponse successResponse() {
        return mock(ProtocolResponse.class);
    }

    @Test
    @DisplayName("FAIL_FAST：首个 Key 失败立即抛错，不调 invoke（换 Key）、不换渠道")
    void failFast_firstKeyFailure_throwsImmediately_noChannelSwitch() {
        // 两个候选，第一个 Key 失败应立即抛错，不试第二个候选
        KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
        ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR,
                "限流", null, "gpt-4o", "openai", 20L, null);
        when(keyInvoker.invokeSingleKey(any(), any())).thenThrow(failure);

        ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker);
        RoutingContext c1 = ctx(10L, FailureStrategy.FAIL_FAST);
        RoutingContext c2 = ctx(11L, FailureStrategy.FAIL_FAST);

        assertThatThrownBy(() -> invoker.invoke(c1, List.of(c1, c2), req(),
                Protocol.OPENAI, 1L, "t"))
                .isSameAs(failure);
        // 验证未换渠道：c2 的 invokeSingleKey 从未被调用
        verify(keyInvoker, never()).invokeSingleKey(eq(c2), any());
        // 验证未调 invoke（换 Key）：FAIL_FAST 只调 invokeSingleKey
        verify(keyInvoker, never()).invoke(any(), any());
    }

    @Test
    @DisplayName("FAIL_RETRY：同渠道 Key 耗尽后不换渠道，直接抛错")
    void failRetry_sameChannelKeyExhausted_noChannelSwitch_throws() {
        KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
        ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR,
                "Key 耗尽", null, "gpt-4o", "openai", 20L, null);
        when(keyInvoker.invoke(any(), any())).thenThrow(failure);

        ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker);
        RoutingContext c1 = ctx(10L, FailureStrategy.FAIL_RETRY);
        RoutingContext c2 = ctx(11L, FailureStrategy.FAIL_RETRY);

        assertThatThrownBy(() -> invoker.invoke(c1, List.of(c1, c2), req(),
                Protocol.OPENAI, 1L, "t"))
                .isSameAs(failure);
        // 验证未换渠道：c2 从未被调用
        verify(keyInvoker, never()).invoke(eq(c2), any());
    }

    @Test
    @DisplayName("FAIL_OVER：候选渠道 Key 耗尽后换下一候选")
    void failOver_channelExhausted_switchesToNextCandidate() {
        KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
        ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR,
                "限流", null, "gpt-4o", "openai", 20L, null);
        RoutingContext c1 = ctx(10L, FailureStrategy.FAIL_OVER);
        RoutingContext c2 = ctx(11L, FailureStrategy.FAIL_OVER);
        when(keyInvoker.invoke(eq(c1), any())).thenThrow(failure);
        when(keyInvoker.invoke(eq(c2), any())).thenReturn(successResponse());

        ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker);

        ProtocolResponse resp = invoker.invoke(c1, List.of(c1, c2), req(),
                Protocol.OPENAI, 1L, "t");
        assertThat(resp).isNotNull();
        // 验证换到了第二个候选
        verify(keyInvoker).invoke(eq(c2), any());
    }

    @Test
    @DisplayName("默认策略：failureStrategy 为 null 时回退 FAIL_RETRY（不换渠道）")
    void defaultStrategy_whenNull_failRetry() {
        // failureStrategy 为 null 时回退 FAIL_RETRY
        KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
        ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR,
                "限流", null, "gpt-4o", "openai", 20L, null);
        when(keyInvoker.invoke(any(), any())).thenThrow(failure);

        ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker);
        RoutingContext c1 = ctxWithNullStrategy(10L);
        RoutingContext c2 = ctxWithNullStrategy(11L);

        assertThatThrownBy(() -> invoker.invoke(c1, List.of(c1, c2), req(),
                Protocol.OPENAI, 1L, "t"))
                .isSameAs(failure);
        // 验证未换渠道
        verify(keyInvoker, never()).invoke(eq(c2), any());
    }

    // ==================== 转移事件发布语义（FAIL_RETRY 不发 / FAIL_OVER 发） ====================

    @Test
    @DisplayName("FAIL_RETRY：同渠道 Key 耗尽后不发转移事件（不换渠道，事件 from→to 误导可观测性）")
    void failRetry_sameChannelKeyExhausted_noFailoverEventPublished() {
        // FAIL_RETRY 下 L1 共因失败 break 不换渠道，publishFailoverEvent 内部按
        // nextIndex<size 计算 to=下一候选 + exhausted=false，会画出"从 A 转移到 B"
        // 但实际未转移，误导容灾诊断。修复后 FAIL_RETRY 不发转移事件。
        KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR,
                "Key 耗尽", null, "gpt-4o", "openai", 20L, null);
        when(keyInvoker.invoke(any(), any())).thenThrow(failure);

        ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker, eventPublisher);
        RoutingContext c1 = ctx(10L, FailureStrategy.FAIL_RETRY);
        RoutingContext c2 = ctx(11L, FailureStrategy.FAIL_RETRY);

        assertThatThrownBy(() -> invoker.invoke(c1, List.of(c1, c2), req(),
                Protocol.OPENAI, 1L, "t"))
                .isSameAs(failure);
        // FAIL_RETRY 不换渠道，不应发布任何转移事件
        verify(eventPublisher, never()).publish(any(FailoverOccurredEvent.class));
    }

    @Test
    @DisplayName("流式 FAIL_RETRY：同渠道 Key 耗尽后不发转移事件（首字节前失败也不发）")
    void failRetry_streamSameChannelKeyExhausted_noFailoverEventPublished() {
        // 流式 FAIL_RETRY 首字节前启动失败 break 不换渠道，同样不应发转移事件
        KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR,
                "Key 耗尽", null, "gpt-4o", "openai", 20L, null);
        // 流式启动失败（首字节前同步抛错，未调 onChunk）
        doThrow(failure).when(keyInvoker).invokeStream(any(), any(), any(StreamCallback.class));

        ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker, eventPublisher);
        RoutingContext c1 = ctx(10L, FailureStrategy.FAIL_RETRY);
        RoutingContext c2 = ctx(11L, FailureStrategy.FAIL_RETRY);

        StreamCallback callback = mock(StreamCallback.class);
        assertThatThrownBy(() -> invoker.invokeStream(c1, List.of(c1, c2), req(),
                Protocol.OPENAI, 1L, "t", callback))
                .isSameAs(failure);
        // 流式 FAIL_RETRY 不换渠道，不应发布任何转移事件
        verify(eventPublisher, never()).publish(any(FailoverOccurredEvent.class));
    }

    @Test
    @DisplayName("FAIL_OVER：候选渠道 Key 耗尽后仍发转移事件（对照，确保修复未误删事件发布）")
    void failOver_channelExhausted_publishesFailoverEvent() {
        // 对照测试：FAIL_OVER 换候选前应发转移事件，确保 FAIL_RETRY 收窄修复未误伤 FAIL_OVER 路径
        KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR,
                "限流", null, "gpt-4o", "openai", 20L, null);
        when(keyInvoker.invoke(any(), any())).thenThrow(failure);

        ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker, eventPublisher);
        RoutingContext c1 = ctx(10L, FailureStrategy.FAIL_OVER);
        RoutingContext c2 = ctx(11L, FailureStrategy.FAIL_OVER);

        assertThatThrownBy(() -> invoker.invoke(c1, List.of(c1, c2), req(),
                Protocol.OPENAI, 1L, "t"))
                .isSameAs(failure);
        // FAIL_OVER 换候选前应发布转移事件（修复仅收窄 FAIL_RETRY，FAIL_OVER 行为不变）
        verify(eventPublisher, atLeastOnce()).publish(any(FailoverOccurredEvent.class));
    }
}
