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
package com.codingas.gateway.integration;

import com.codingas.gateway.proxy.conversion.OutboundTuner;
import com.codingas.gateway.proxy.chat.ErrorClassifier;
import com.codingas.gateway.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.proxy.invoker.KeyFailoverInvoker;
import com.codingas.gateway.proxy.routing.RouterChain;
import com.codingas.gateway.common.event.BizEventPublisher;
import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.proxy.conversion.ProtocolConversionFacade;
import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.transport.ProviderException;
import com.codingas.gateway.proxy.routing.RoutingContext;
import com.codingas.gateway.common.enums.FailureStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 渠道级故障转移端到端集成测试（Task 3.7）
 *
 * <p>验证 {@link ChannelFailoverInvoker} 在真实 Spring 上下文装配下的 L1 转移、错误分流、
 * 流式首字节边界场景。与 {@code ChannelFailoverInvokerTest}（单元测试）的区别：</p>
 * <ul>
 *   <li>单元测试 mock {@link ErrorClassifier} 手动注入决策（L1/NONE），聚焦 Invoker 内部分支逻辑；</li>
 *   <li>本集成测试 {@link Autowired} 真实 {@link ErrorClassifier} bean（Spring 装配的静态分流表），
 *       验证"AUTHENTICATION_ERROR 真实被分到 L1 触发转移、INVALID_REQUEST 真实被分到 NONE 不转移"
 *       这类端到端分流语义，而非手动注入决策。</li>
 * </ul>
 *
 * <p><b>真实/mock 边界划分</b>：继承 {@link FullContextIntegrationTestBase} 借用 Spring 上下文加载，
 * 但基类把 {@code ChannelFailoverInvoker} 与 {@code KeyFailoverInvoker} 都 {@code @MockBean} 了
 * （@Autowired 拿到的是 mock，无法测真实链路）。因此本测试在每个用例内手动
 * {@code new ChannelFailoverInvoker(...)}，注入：</p>
 * <ul>
 *   <li>真实 {@link ErrorClassifier}（{@link Autowired}，验证真实分流表装配）；</li>
 *   <li>mock {@link KeyFailoverInvoker}（上游 HTTP 边界，控制成功/失败/流式行为）。</li>
 * </ul>
 *
 * <p>Task 4 适配：L2 模型降级层已删除，invoke/invokeStream 签名移除 profile 参数，候选耗尽直接抛
 * 最后异常，不再进入 L2 降级。</p>
 *
 * <p>Task 5 适配：DomainHealth 域级聚合路由器已删除，RouterChain 收敛为端点级健康过滤。
 * 本测试移除域级聚合端到端用例，新增 RouterChain 组成断言验证责任链顺序。</p>
 *
 * <p>参考 {@link FullContextIntegrationTest} 的 KeyFailoverTests 模式：继承基类借用上下文，
 * 测试内手动构造真实 invoker。</p>
 */
class ChannelFailoverIntegrationTest extends FullContextIntegrationTestBase {

    /** 真实错误分流器（Spring 装配，按静态分流表决策，非 mock） */
    @Autowired
    private ErrorClassifier realErrorClassifier;

    /** 候选渠道 1 上下文（channelId=10, endpointId=20） */
    private RoutingContext ctx1;
    /** 候选渠道 2 上下文（channelId=11, endpointId=21） */
    private RoutingContext ctx2;
    /** 测试协议请求（model=gpt-4o） */
    private ProtocolRequest request;

    @BeforeEach
    void setUpFailoverFixture() {
        // 构造两个候选渠道上下文（按 priority 升序，ctx1 优先）
        // Task 7：期望换渠道的场景用 FAIL_OVER（FAIL_RETRY 不换渠道；NONE 决策在三种策略下都直接抛）
        ctx1 = new RoutingContext(10L, 20L, "https://ch1.example.com/v1",
                Protocol.OPENAI, "sk-1", 60, false, "gpt-4o", null,
                FailureStrategy.FAIL_OVER);
        ctx2 = new RoutingContext(11L, 21L, "https://ch2.example.com/v1",
                Protocol.OPENAI, "sk-2", 60, false, "gpt-4o", null,
                FailureStrategy.FAIL_OVER);

        // mock 协议请求：仅需要 getModel 返回固定模型名（L2 降级读取）
        request = mock(ProtocolRequest.class);
        lenient().when(request.getModel()).thenReturn("gpt-4o");
        // 调谐下沉：invoker 每候选对原始 request 副本做 convert+tune，copy 桩返回自身保持既有匹配
        lenient().when(request.copy()).thenReturn(request);
    }

    /**
     * 构造真实 ChannelFailoverInvoker，注入真实 ErrorClassifier + mock 上游/事件发布边界
     *
     * @param keyFailoverInvoker   mock 的 Key 级 Invoker（控制候选成功/失败）
     * @return 真实 ChannelFailoverInvoker 实例（使用 Spring 装配的真实分流表）
     */
    private ChannelFailoverInvoker newRealInvoker(KeyFailoverInvoker keyFailoverInvoker) {
        // 事件发布器用 no-op mock（集成测试不验证转移事件持久化，由 ChannelFailoverInvokerTest 覆盖）
        // OutboundTuner 用 mock + returnsFirstArg（调谐下沉后每候选 tune，集成测试聚焦真实分流表非调谐）
        // ProtocolConversionFacade 用 no-op mock（集成测试候选均为同协议，不触发跨协议转换）
        OutboundTuner tuner = mock(OutboundTuner.class);
        lenient().when(tuner.tune(any(ProtocolRequest.class), any(RoutingContext.class)))
                .thenAnswer(org.mockito.AdditionalAnswers.returnsFirstArg());
        return new ChannelFailoverInvoker(keyFailoverInvoker, realErrorClassifier,
                mock(BizEventPublisher.class),
                tuner, mock(ProtocolConversionFacade.class));
    }

    // ==================== L1 转移与错误分流（非流式） ====================

    @Nested
    @DisplayName("L1 转移与错误分流（非流式）")
    class NonStreamFailoverTests {

        @Test
        @DisplayName("L1 转移：ch1 AUTHENTICATION_ERROR 共因故障 → ch2 成功")
        void l1Failover_authError_transfersToNextCandidate() {
            // ch1 抛 AUTH 共因故障 → 真实 ErrorClassifier 分流为 L1 → 换 ch2
            ProviderException authEx = new ProviderException(
                    ProviderErrorType.AUTHENTICATION_ERROR, "ch1 认证失败");
            KeyFailoverInvoker keyFailover = mock(KeyFailoverInvoker.class);
            when(keyFailover.invoke(ctx1, request)).thenThrow(authEx);

            // ch2 返回成功响应
            ProtocolResponse successResponse = mock(ProtocolResponse.class);
            when(keyFailover.invoke(ctx2, request)).thenReturn(successResponse);

            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover);

            ProtocolResponse result = invoker.invoke(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, "test-trace-id");

            // 断言：转移成功，返回 ch2 的响应
            assertThat(result).isSameAs(successResponse);
            verify(keyFailover).invoke(ctx1, request);
            verify(keyFailover).invoke(ctx2, request);
        }

        @Test
        @DisplayName("错误分流：INVALID_REQUEST 不转移，ch1 失败直接抛出不试 ch2")
        void invalidRequest_noFailover_throwsDirectly() {
            // ch1 抛 INVALID_REQUEST → 真实 ErrorClassifier 分流为 NONE → 直接抛不转移
            ProviderException invalidEx = new ProviderException(
                    ProviderErrorType.INVALID_REQUEST, "请求格式错误");
            KeyFailoverInvoker keyFailover = mock(KeyFailoverInvoker.class);
            when(keyFailover.invoke(ctx1, request)).thenThrow(invalidEx);

            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover);

            // 断言：直接抛出原始 INVALID_REQUEST 异常，不试 ch2
            assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, "test-trace-id"))
                    .isSameAs(invalidEx);

            verify(keyFailover).invoke(ctx1, request);
            verify(keyFailover, never()).invoke(ctx2, request);
        }

        @Test
        @DisplayName("9.7 端到端：共因故障不再跳过同域 → 按顺序试所有候选 → 全部耗尽抛最后异常")
        void e2e_commonCauseFailure_triesAllCandidatesInOrder_exhaustsAndThrowsLast() {
            // 构造：ch1 + ch2 + ch3 三个候选
            // Task 2 变更：删除共因跳过，所有候选按 priority 顺序逐个试，全部耗尽抛最后异常
            // Task 7：期望逐个试所有候选，用 FAIL_OVER 策略（FAIL_RETRY 不换渠道只试首候选）
            RoutingContext ch1 = new RoutingContext(10L, 20L, "https://ch1.example.com/v1",
                    Protocol.OPENAI, "sk-1", 60, false, "gpt-4o", null,
                    FailureStrategy.FAIL_OVER);
            RoutingContext ch2 = new RoutingContext(11L, 21L, "https://ch2.example.com/v1",
                    Protocol.OPENAI, "sk-2", 60, false, "gpt-4o", null,
                    FailureStrategy.FAIL_OVER);
            RoutingContext ch3 = new RoutingContext(12L, 22L, "https://ch3.example.com/v1",
                    Protocol.OPENAI, "sk-3", 60, false, "gpt-4o", null,
                    FailureStrategy.FAIL_OVER);

            // 三个候选均 AUTH 共因失败 → 真实 ErrorClassifier 分流 L1 → 逐个试不跳过
            ProviderException ch1Ex = new ProviderException(
                    ProviderErrorType.AUTHENTICATION_ERROR, "ch1 认证失败");
            ProviderException ch2Ex = new ProviderException(
                    ProviderErrorType.AUTHENTICATION_ERROR, "ch2 认证失败");
            ProviderException ch3Ex = new ProviderException(
                    ProviderErrorType.AUTHENTICATION_ERROR, "ch3 认证失败");
            KeyFailoverInvoker keyFailover = mock(KeyFailoverInvoker.class);
            when(keyFailover.invoke(ch1, request)).thenThrow(ch1Ex);
            when(keyFailover.invoke(ch2, request)).thenThrow(ch2Ex);
            when(keyFailover.invoke(ch3, request)).thenThrow(ch3Ex);

            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover);

            // 断言：所有候选按顺序逐个试，全部耗尽后抛最后捕获的异常（ch3 的异常）
            assertThatThrownBy(() -> invoker.invoke(ch1, List.of(ch1, ch2, ch3),
                    request, Protocol.OPENAI, 7L, "test-trace-id"))
                    .isSameAs(ch3Ex);

            // 断言：三个候选都被试过（Task 2 删除共因跳过，同域候选 ch2 也被试）
            verify(keyFailover).invoke(ch1, request);
            verify(keyFailover).invoke(ch2, request);  // 同域候选不再跳过
            verify(keyFailover).invoke(ch3, request);  // 异域候选也被试
        }
    }

    // ==================== 流式首字节边界 ====================

    @Nested
    @DisplayName("流式首字节边界")
    class StreamBoundaryTests {

        @Test
        @DisplayName("流式：首字节前启动失败（UPSTREAM_ERROR 共因）→ 转移到 ch2")
        void stream_beforeFirstByte_transfersToNextCandidate() {
            // ch1 流式启动失败（首字节前，未触发 onChunk）→ 真实分流 L1 → 换 ch2
            ProviderException startupEx = new ProviderException(
                    ProviderErrorType.UPSTREAM_ERROR, "ch1 流式启动失败");
            KeyFailoverInvoker keyFailover = mock(KeyFailoverInvoker.class);
            doThrow(startupEx).when(keyFailover)
                    .invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
            // ch2 启动成功（invokeStream 默认 doNothing，表示流建立后 return）

            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover);
            StreamCallback callback = mock(StreamCallback.class);

            // 执行：ch1 启动失败 → 换 ch2 建立流
            invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, "test-trace-id", callback);

            // 断言：两个候选都被试过（ch1 失败后转移到 ch2）
            verify(keyFailover).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
            verify(keyFailover).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
        }

        @Test
        @DisplayName("流式：首字节已发送后失败不换候选（首字节后转移边界）")
        void stream_afterFirstByte_noFailover() {
            // 场景：ch1 首字节已发送后同步失败（先 onChunk 再抛异常）
            // 语义：首字节后失败不换渠道，直接抛传播给调用方，不试 ch2
            ProviderException afterFirstByteEx = new ProviderException(
                    ProviderErrorType.UPSTREAM_ERROR, "ch1 首字节后失败");
            KeyFailoverInvoker keyFailover = mock(KeyFailoverInvoker.class);
            doAnswer(invocation -> {
                // 模拟 KeyFailoverInvoker 在流建立过程中先发首字节，再同步抛异常
                StreamCallback wrappedCallback = invocation.getArgument(2, StreamCallback.class);
                wrappedCallback.onChunk("first-byte-data");  // 首字节已发 → firstByteSent=true
                throw afterFirstByteEx;  // 首字节后同步失败
            }).when(keyFailover).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));

            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover);
            StreamCallback callback = mock(StreamCallback.class);

            // 断言：首字节已发，不换 ch2，直接抛 afterFirstByteEx
            assertThatThrownBy(() -> invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, "test-trace-id", callback))
                    .isSameAs(afterFirstByteEx);

            verify(keyFailover).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
            verify(keyFailover, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
            // 首字节已透传给原 callback（包装 callback 透传语义）
            verify(callback).onChunk("first-byte-data");
        }
    }

    // ==================== RouterChain 组成验证 ====================

    /** 真实路由器责任链 bean（Spring 装配，按 @Order 自动收集所有 Router） */
    @Autowired
    private RouterChain realRouterChain;

    @Nested
    @DisplayName("RouterChain 组成验证")
    class RouterChainCompositionTests {

        @Test
        @DisplayName("RouterChain 按预期顺序组成：Permission → Health → Priority → LoadBalance")
        void routerChain_composedByExpectedOrder() throws Exception {
            // 读取 RouterChain 已按 @Order 排序的责任链路由器类名
            List<String> routerNames = readRouterClassNames(realRouterChain);

            // 保留路由器按 @Order 升序保持：Permission → EndpointHealth(Health) → Priority → LoadBalance
            assertThat(routerNames)
                    .containsSubsequence("PermissionRouter", "HealthRouter", "PriorityRouter", "LoadBalanceRouter");
        }
    }

    /** 读取 RouterChain 私有 routers 字段（已按 @Order 排序）的类名列表 */
    @SuppressWarnings("unchecked")
    private List<String> readRouterClassNames(RouterChain chain) throws Exception {
        java.lang.reflect.Field field = RouterChain.class.getDeclaredField("routers");
        field.setAccessible(true);
        List<?> routers = (List<?>) field.get(chain);
        return routers.stream().map(r -> r.getClass().getSimpleName()).toList();
    }
}
