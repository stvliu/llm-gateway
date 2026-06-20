package com.codingas.gateway.integration;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.application.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.application.proxy.invoker.KeyFailoverInvoker;
import com.codingas.gateway.application.proxy.invoker.L2DegradationRequiredException;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
 * 渠道级故障转移端到端集成测试（Task 3.7）
 *
 * <p>验证 {@link ChannelFailoverInvoker} 在真实 Spring 上下文装配下的 L1 转移、错误分流、
 * 流式首字节边界与 L2 降级门禁两对照场景。与 {@code ChannelFailoverInvokerTest}（单元测试）的区别：</p>
 * <ul>
 *   <li>单元测试 mock {@link ErrorClassifier} 手动注入决策（L1/L2/NONE），聚焦 Invoker 内部分支逻辑；</li>
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
 *   <li>mock {@link KeyFailoverInvoker}（上游 HTTP 边界，控制成功/失败/流式行为）；</li>
 *   <li>mock {@link DegradationService}（L2 降级边界，控制 degrade 返回值）。</li>
 * </ul>
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
        ctx1 = new RoutingContext(10L, 20L, "https://ch1.example.com/v1",
                Protocol.OPENAI, "sk-1", 60, false, "gpt-4o", null);
        ctx2 = new RoutingContext(11L, 21L, "https://ch2.example.com/v1",
                Protocol.OPENAI, "sk-2", 60, false, "gpt-4o", null);

        // mock 协议请求：仅需要 getModel 返回固定模型名（L2 降级读取）
        request = mock(ProtocolRequest.class);
        lenient().when(request.getModel()).thenReturn("gpt-4o");
    }

    /**
     * 构造真实 ChannelFailoverInvoker，注入真实 ErrorClassifier + mock 上游/降级边界
     *
     * @param keyFailoverInvoker   mock 的 Key 级 Invoker（控制候选成功/失败）
     * @param degradationService   mock 的降级服务（控制 degrade 返回）
     * @return 真实 ChannelFailoverInvoker 实例（使用 Spring 装配的真实分流表）
     */
    private ChannelFailoverInvoker newRealInvoker(KeyFailoverInvoker keyFailoverInvoker,
                                                  DegradationService degradationService) {
        return new ChannelFailoverInvoker(keyFailoverInvoker, realErrorClassifier, degradationService);
    }

    /**
     * 构造 mock DegradationService（L2 门禁关闭场景默认不期望被调用）
     *
     * @return mock DegradationService
     */
    private DegradationService mockDegradation() {
        return mock(DegradationService.class);
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

            DegradationService degradation = mockDegradation();
            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover, degradation);

            ProtocolResponse result = invoker.invoke(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, true);

            // 断言：转移成功，返回 ch2 的响应
            assertThat(result).isSameAs(successResponse);
            verify(keyFailover).invoke(ctx1, request);
            verify(keyFailover).invoke(ctx2, request);
            // L1 转移成功不应触发 L2 降级
            verify(degradation, never()).degrade(anyString(), any());
        }

        @Test
        @DisplayName("错误分流：INVALID_REQUEST 不转移，ch1 失败直接抛出不试 ch2")
        void invalidRequest_noFailover_throwsDirectly() {
            // ch1 抛 INVALID_REQUEST → 真实 ErrorClassifier 分流为 NONE → 直接抛不转移
            ProviderException invalidEx = new ProviderException(
                    ProviderErrorType.INVALID_REQUEST, "请求格式错误");
            KeyFailoverInvoker keyFailover = mock(KeyFailoverInvoker.class);
            when(keyFailover.invoke(ctx1, request)).thenThrow(invalidEx);

            DegradationService degradation = mockDegradation();
            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover, degradation);

            // 断言：直接抛出原始 INVALID_REQUEST 异常，不试 ch2，不降级
            assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, true))
                    .isSameAs(invalidEx);

            verify(keyFailover).invoke(ctx1, request);
            verify(keyFailover, never()).invoke(ctx2, request);
            verify(degradation, never()).degrade(anyString(), any());
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

            DegradationService degradation = mockDegradation();
            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover, degradation);
            StreamCallback callback = mock(StreamCallback.class);

            // 执行：ch1 启动失败 → 换 ch2 建立流
            invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, true, callback);

            // 断言：两个候选都被试过（ch1 失败后转移到 ch2）
            verify(keyFailover).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
            verify(keyFailover).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
            verify(degradation, never()).degrade(anyString(), any());
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

            DegradationService degradation = mockDegradation();
            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover, degradation);
            StreamCallback callback = mock(StreamCallback.class);

            // 断言：首字节已发，不换 ch2，直接抛 afterFirstByteEx
            assertThatThrownBy(() -> invoker.invokeStream(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, true, callback))
                    .isSameAs(afterFirstByteEx);

            verify(keyFailover).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
            verify(keyFailover, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
            verify(degradation, never()).degrade(anyString(), any());
            // 首字节已透传给原 callback（包装 callback 透传语义）
            verify(callback).onChunk("first-byte-data");
        }
    }

    // ==================== L2 降级门禁两对照场景 ====================

    @Nested
    @DisplayName("L2 降级门禁两对照（Claude Code 禁降级 / 客服全开）")
    class L2GateProfileTests {

        @Test
        @DisplayName("Claude Code 禁降级（enableL2=false）：L1 耗尽直接抛最后异常，不调 degrade")
        void claudeCodeProfile_l2Disabled_throwsOriginal() {
            // 两候选均 AUTH 共因失败耗尽；L2 门禁关闭 → tryL2Degradation 直接返回 null，抛 lastException
            ProviderException authEx = new ProviderException(
                    ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
            KeyFailoverInvoker keyFailover = mock(KeyFailoverInvoker.class);
            when(keyFailover.invoke(ctx1, request)).thenThrow(authEx);
            when(keyFailover.invoke(ctx2, request)).thenThrow(authEx);

            DegradationService degradation = mockDegradation();
            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover, degradation);

            // 断言：禁降级场景抛最后捕获的 authEx，不触发 L2 降级信号
            assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, false))
                    .isSameAs(authEx);

            // 显式验证门禁关闭时不调 degrade
            verify(degradation, never()).degrade(anyString(), any());
        }

        @Test
        @DisplayName("客服全开（enableL2=true）：L1 耗尽后 degrade 返回 fallback → 抛 L2 降级信号")
        void customerServiceProfile_l2Enabled_degrades() {
            // 两候选均 AUTH 共因失败耗尽；L2 门禁开启 → degrade 返回 fallback → 抛 L2DegradationRequiredException
            ProviderException authEx = new ProviderException(
                    ProviderErrorType.AUTHENTICATION_ERROR, "auth fail");
            KeyFailoverInvoker keyFailover = mock(KeyFailoverInvoker.class);
            when(keyFailover.invoke(ctx1, request)).thenThrow(authEx);
            when(keyFailover.invoke(ctx2, request)).thenThrow(authEx);

            DegradationService degradation = mockDegradation();
            when(degradation.degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR))
                    .thenReturn("gpt-3.5-turbo");

            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover, degradation);

            // 断言：全开场景触发 L2 降级信号，携带 fallback 模型名
            assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, true))
                    .isInstanceOf(L2DegradationRequiredException.class)
                    .extracting(e -> ((L2DegradationRequiredException) e).getFallbackModel())
                    .isEqualTo("gpt-3.5-turbo");

            // 显式验证两候选都被试过（L1 全耗尽才进 L2）且 degrade 被调用
            verify(keyFailover).invoke(ctx1, request);
            verify(keyFailover).invoke(ctx2, request);
            verify(degradation).degrade("gpt-4o", ProviderErrorType.AUTHENTICATION_ERROR);
        }
    }

    // ==================== 跨 Cluster 不越权（P2 占位） ====================

    @Test
    @DisplayName("跨 Cluster 不越权：P2 Cluster 落地后补充")
    @Disabled("P2 Cluster 体系落地后补充：验证 L1 转移不跨越 Cluster 边界，避免越权访问其他租户/集群的渠道")
    void crossCluster_isolation_placeholder() {
        // TODO P2：Cluster 体系落地后，构造同 Cluster 多候选 + 跨 Cluster 候选，
        // 验证 L1 转移仅在同 Cluster 内进行，不越权访问其他 Cluster 的渠道。
        // 当前 P1 阶段无 Cluster 概念，无法验证此场景，先占位。
    }
}
