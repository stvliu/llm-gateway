package com.codingas.gateway.integration;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.application.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.application.proxy.invoker.KeyFailoverInvoker;
import com.codingas.gateway.application.proxy.invoker.L2DegradationRequiredException;
import com.codingas.gateway.application.proxy.routing.ClusterAffinityRouter;
import com.codingas.gateway.application.proxy.routing.EndpointResolver;
import com.codingas.gateway.application.proxy.routing.RoutingRequest;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
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
     * 构造真实 ChannelFailoverInvoker，注入真实 ErrorClassifier + mock 上游/降级/事件发布边界
     *
     * @param keyFailoverInvoker   mock 的 Key 级 Invoker（控制候选成功/失败）
     * @param degradationService   mock 的降级服务（控制 degrade 返回）
     * @return 真实 ChannelFailoverInvoker 实例（使用 Spring 装配的真实分流表）
     */
    private ChannelFailoverInvoker newRealInvoker(KeyFailoverInvoker keyFailoverInvoker,
                                                  DegradationService degradationService) {
        // 事件发布器用 no-op mock（集成测试不验证转移事件持久化，由 ChannelFailoverInvokerTest 覆盖）
        // ChannelGateway 用 no-op mock（集成测试不验证 clusterId 反查，由 ChannelFailoverInvokerTest 覆盖）
        return new ChannelFailoverInvoker(keyFailoverInvoker, realErrorClassifier, degradationService,
                mock(DomainEventPublisher.class), mock(com.codingas.gateway.domain.supply.gateway.ChannelGateway.class));
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
                    request, Protocol.OPENAI, 7L, profile(true), "test-trace-id");

            // 断言：转移成功，返回 ch2 的响应
            assertThat(result).isSameAs(successResponse);
            verify(keyFailover).invoke(ctx1, request);
            verify(keyFailover).invoke(ctx2, request);
            // L1 转移成功不应触发 L2 降级
            verify(degradation, never()).degrade(anyString(), any(), any());
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
                    request, Protocol.OPENAI, 7L, profile(true), "test-trace-id"))
                    .isSameAs(invalidEx);

            verify(keyFailover).invoke(ctx1, request);
            verify(keyFailover, never()).invoke(ctx2, request);
            verify(degradation, never()).degrade(anyString(), any(), any());
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
                    request, Protocol.OPENAI, 7L, profile(true), "test-trace-id", callback);

            // 断言：两个候选都被试过（ch1 失败后转移到 ch2）
            verify(keyFailover).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
            verify(keyFailover).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
            verify(degradation, never()).degrade(anyString(), any(), any());
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
                    request, Protocol.OPENAI, 7L, profile(true), "test-trace-id", callback))
                    .isSameAs(afterFirstByteEx);

            verify(keyFailover).invokeStream(eq(ctx1), eq(request), any(StreamCallback.class));
            verify(keyFailover, never()).invokeStream(eq(ctx2), eq(request), any(StreamCallback.class));
            verify(degradation, never()).degrade(anyString(), any(), any());
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
                    request, Protocol.OPENAI, 7L, profile(false), "test-trace-id"))
                    .isSameAs(authEx);

            // 显式验证门禁关闭时不调 degrade
            verify(degradation, never()).degrade(anyString(), any(), any());
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
            when(degradation.degrade(eq("gpt-4o"), eq(ProviderErrorType.AUTHENTICATION_ERROR), any(ResilienceProfile.class)))
                    .thenReturn("gpt-3.5-turbo");

            ChannelFailoverInvoker invoker = newRealInvoker(keyFailover, degradation);

            // 断言：全开场景触发 L2 降级信号，携带 fallback 模型名
            assertThatThrownBy(() -> invoker.invoke(ctx1, List.of(ctx1, ctx2),
                    request, Protocol.OPENAI, 7L, profile(true), "test-trace-id"))
                    .isInstanceOf(L2DegradationRequiredException.class)
                    .extracting(e -> ((L2DegradationRequiredException) e).getFallbackModel())
                    .isEqualTo("gpt-3.5-turbo");

            // 显式验证两候选都被试过（L1 全耗尽才进 L2）且 degrade 被调用
            verify(keyFailover).invoke(ctx1, request);
            verify(keyFailover).invoke(ctx2, request);
            verify(degradation).degrade(eq("gpt-4o"), eq(ProviderErrorType.AUTHENTICATION_ERROR), any(ResilienceProfile.class));
        }
    }

    /**
     * 构造测试用画像：enableL2 控制是否启用 L2 模型降级门禁（Task 4.9 profile 贯穿 Invoker 链）
     */
    private static ResilienceProfile profile(boolean enableL2) {
        ResilienceProfile p = new ResilienceProfile();
        p.setEnableL2ModelDegradation(enableL2);
        p.setDegradationMaxDepth(5);
        return p;
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

    // ==================== 会话亲和端到端（Task 4.10） ====================

    // 真实会话亲和存储 bean（integration-test profile 下 SessionAffinityConfig 装配 InMemory 实现）
    @Autowired
    private com.codingas.gateway.domain.resilience.gateway.SessionAffinityStore realSessionAffinityStore;

    @Nested
    @DisplayName("会话亲和端到端：标识缺失不亲和、亲和粘滞、熔断转移更新")
    class SessionAffinityTests {

        @Test
        @DisplayName("标识缺失（null sessionId）不亲和：get 返回 null，put 不存储")
        void nullSessionId_noAffinity() {
            // 标识缺失时 get 返回 null（不亲和路径）
            assertThat(realSessionAffinityStore.get(null)).isNull();
            // put(null) 不存储：即便后续 get 仍是 null
            realSessionAffinityStore.put(null, 10L);
            assertThat(realSessionAffinityStore.get(null)).isNull();
        }

        @Test
        @DisplayName("亲和粘滞：put 绑定后 get 返回同渠道，粘滞同会话")
        void affinityPutThenGet_stickySameChannel() {
            // 亲和命中：put 绑定 sessionId→channelId，get 返回同 channelId
            realSessionAffinityStore.put("session-001", 10L);
            assertThat(realSessionAffinityStore.get("session-001")).isEqualTo(10L);
            // 清理：避免污染其他测试
            realSessionAffinityStore.evict("session-001");
        }

        @Test
        @DisplayName("熔断转移更新：亲和渠道熔断后 evict 旧绑定 + put 新绑定，亲和转移到新渠道")
        void circuitBreakerFailover_evictOldPutNew_updatesAffinity() {
            // 场景：会话亲和绑定 ch1（channelId=10），ch1 熔断后 L1 转移到 ch2（channelId=11）
            // 转移成功后 Invoker 应执行：evict 旧绑定 + put 新绑定（亲和转移更新协议）
            // 注：当前调度链未接线会话亲和（后续 task 接入），此处验证存储契约：
            //     手动执行转移更新协议序列，确认存储在真实 Spring 装配下行为正确
            String sessionId = "session-failover";
            realSessionAffinityStore.put(sessionId, 10L);
            assertThat(realSessionAffinityStore.get(sessionId)).isEqualTo(10L);

            // ch1 熔断 → L1 转移到 ch2 成功 → 转移更新协议
            realSessionAffinityStore.evict(sessionId);
            realSessionAffinityStore.put(sessionId, 11L);

            // 断言：亲和已转移到新渠道 ch2
            assertThat(realSessionAffinityStore.get(sessionId)).isEqualTo(11L);
            // 清理
            realSessionAffinityStore.evict(sessionId);
        }

        @Test
        @DisplayName("evict 不存在的 sessionId 不抛异常（幂等清除）")
        void evictNonExistent_noException() {
            // 幂等语义：不存在的 sessionId evict 不抛异常
            realSessionAffinityStore.evict("non-existent-session");
            assertThat(realSessionAffinityStore.get("non-existent-session")).isNull();
        }
    }

    // ==================== Cluster 健康聚合 + 共因隔离 + 亲和路由端到端（Task 4.10） ====================

    // 真实 Cluster 健康聚合器 bean（Spring 装配，读真实熔断器状态，纯计算不写库）
    @Autowired
    private com.codingas.gateway.application.proxy.routing.ClusterHealthAggregator realClusterHealthAggregator;

    // 真实端点熔断器管理器 bean（Spring 装配，每 endpointId 独立熔断器）
    @Autowired
    private com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager realCircuitBreakerManager;

    // 真实 Cluster 亲和路由器 bean（Spring 装配，@Order 250，DOWN 域过滤）
    @Autowired
    private com.codingas.gateway.application.proxy.routing.ClusterAffinityRouter realClusterAffinityRouter;

    // 真实端点解析器 bean（Spring 装配）
    @Autowired
    private com.codingas.gateway.application.proxy.routing.EndpointResolver realEndpointResolver;

    @Nested
    @DisplayName("Cluster 健康聚合 + 共因隔离端到端：真实熔断器状态聚合")
    class ClusterHealthAggregationTests {

        @Test
        @DisplayName("域内全部端点 CLOSED → HEALTHY（正常承接流量）")
        void allClosed_returnsHealthy() {
            // 全新 endpoint 熔断器默认 CLOSED
            long ep = 9001L;
            // 确保初始 CLOSED（新熔断器默认 CLOSED）
            assertThat(realCircuitBreakerManager.getBreaker(ep).getState())
                    .isEqualTo(com.codingas.gateway.infrastructure.resilience.CircuitBreakerState.CLOSED);

            com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus status =
                    realClusterHealthAggregator.aggregate(java.util.List.of(ep));

            assertThat(status)
                    .isEqualTo(com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("域内全部端点 OPEN → DOWN（共因故障，整域不可用，触发跨域转移）")
        void allOpen_returnsDown_commonCauseIsolated() {
            // 构造两个端点均 OPEN：通过 recordFailure 触发熔断（窗口 10，连续 10 次失败快速熔断）
            long ep1 = 9101L;
            long ep2 = 9102L;
            forceOpen(ep1);
            forceOpen(ep2);
            assertThat(realCircuitBreakerManager.getBreaker(ep1).getState())
                    .isEqualTo(com.codingas.gateway.infrastructure.resilience.CircuitBreakerState.OPEN);
            assertThat(realCircuitBreakerManager.getBreaker(ep2).getState())
                    .isEqualTo(com.codingas.gateway.infrastructure.resilience.CircuitBreakerState.OPEN);

            com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus status =
                    realClusterHealthAggregator.aggregate(java.util.List.of(ep1, ep2));

            // 共因隔离：全 OPEN → DOWN，触发跨域转移
            assertThat(status)
                    .isEqualTo(com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus.DOWN);
        }

        @Test
        @DisplayName("域内部分端点 OPEN → DEGRADED（容量受损但仍可用，不跨域转移）")
        void partialOpen_returnsDegraded() {
            // 一个端点 OPEN，一个 CLOSED → DEGRADED
            long epOpen = 9201L;
            long epClosed = 9202L;
            forceOpen(epOpen);
            // epClosed 默认 CLOSED

            com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus status =
                    realClusterHealthAggregator.aggregate(java.util.List.of(epOpen, epClosed));

            assertThat(status)
                    .isEqualTo(com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus.DEGRADED);
        }

        @Test
        @DisplayName("域内任一端点 HALF_OPEN → 不判 DOWN（正在试探恢复，解除 DOWN 语义）")
        void anyHalfOpen_notDown() {
            // 一个 OPEN，一个 HALF_OPEN → 不应判 DOWN（恢复机制：任一 half-open → 解除 DOWN）
            long epOpen = 9301L;
            long epHalfOpen = 9302L;
            forceOpen(epOpen);
            forceHalfOpen(epHalfOpen);

            com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus status =
                    realClusterHealthAggregator.aggregate(java.util.List.of(epOpen, epHalfOpen));

            // 任一 HALF_OPEN 且仍有 OPEN → DEGRADED（不判 DOWN）
            assertThat(status)
                    .isEqualTo(com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus.DEGRADED);
        }

        /**
         * 强制端点熔断器进入 OPEN 状态
         *
         * <p>默认熔断器窗口 10、失败率阈值 0.5；窗口未满但连续全部失败时快速熔断
         * （{@code failures >= slidingWindowSize}）。连续 recordFailure 10 次即可触发 OPEN。</p>
         *
         * @param endpointId 端点 ID
         */
        private void forceOpen(long endpointId) {
            com.codingas.gateway.infrastructure.resilience.CircuitBreaker breaker =
                    realCircuitBreakerManager.getBreaker(endpointId);
            // 连续失败直到 OPEN（窗口 10，连续 10 次失败快速熔断）
            for (int i = 0; i < 10; i++) {
                breaker.recordFailure();
                if (breaker.getState() == com.codingas.gateway.infrastructure.resilience.CircuitBreakerState.OPEN) {
                    return;
                }
            }
        }

        /**
         * 强制端点熔断器进入 HALF_OPEN 状态
         *
         * <p>先 forceOpen，再通过反射重置 openSince 为过去时间，调用 allowRequest 触发 OPEN→HALF_OPEN 迁移。
         * allowRequest 在 OPEN 且超时后迁移到 HALF_OPEN 并返回 true。</p>
         *
         * @param endpointId 端点 ID
         */
        private void forceHalfOpen(long endpointId) {
            forceOpen(endpointId);
            com.codingas.gateway.infrastructure.resilience.CircuitBreaker breaker =
                    realCircuitBreakerManager.getBreaker(endpointId);
            // 反射重置 openSince 为 0（远古时间），使 allowRequest 判定已超时 → 迁移 HALF_OPEN
            try {
                java.lang.reflect.Field openSinceField =
                        com.codingas.gateway.infrastructure.resilience.CircuitBreaker.class
                                .getDeclaredField("openSince");
                openSinceField.setAccessible(true);
                openSinceField.setLong(breaker, 0L);
                // 调用 allowRequest 触发 OPEN→HALF_OPEN 迁移
                breaker.allowRequest();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("反射重置 CircuitBreaker.openSince 失败", e);
            }
            assertThat(breaker.getState())
                    .isEqualTo(com.codingas.gateway.infrastructure.resilience.CircuitBreakerState.HALF_OPEN);
        }
    }

    // ==================== 亲和路由串联端到端（Task 4.10） ====================

    @Nested
    @DisplayName("亲和路由串联：熔断器 OPEN → 聚合器判 DOWN → 路由器过滤整域触发跨域转移")
    class ClusterAffinityRoutingChainTests {

        /**
         * 端到端串联：真实熔断器（OPEN）→ 真实聚合器（判 DOWN）→ 真实路由器（过滤整域实例）
         *
         * <p>与 {@code ClusterAffinityRouterTest}（单元测试，mock {@link ClusterHealthAggregator}）的区别：
         * 本测试 {@link Autowired} 真实 {@link ClusterHealthAggregator} 与真实
         * {@link ChannelEndpointCircuitBreakerManager}，仅 mock 边界 Gateway
         * （{@link ChannelGateway} / {@link EndpointResolver}，控制 Channel 与 Endpoint 返回），
         * 验证「熔断器状态 → 聚合判断 → 路由过滤」完整串联在真实 Spring 装配下的端到端语义。</p>
         *
         * <p>手动 {@code new ClusterAffinityRouter(...)} 注入真实聚合器 + mock 边界 Gateway，
         * 避免对 Spring 上下文注入额外 {@code @MockBean} 造成上下文重建（参考 ChannelFailover
         * 集成测试手动 new Invoker 的既有模式）。</p>
         */
        @Test
        @DisplayName("域内全部端点熔断 OPEN → 路由器过滤整域实例，保留健康域实例（跨域转移）")
        void downClusterFiltered_healthyClusterKept_endToEnd() {
            // 构造两域候选：
            //   clusterA(10) channel=100 endpoint=9401（全熔断 → DOWN）
            //   clusterB(20) channel=200 endpoint=9402（健康 → 保留）
            com.codingas.gateway.domain.supply.entity.ModelInstance miA =
                    instance(1L, 100L);
            com.codingas.gateway.domain.supply.entity.ModelInstance miB =
                    instance(2L, 200L);

            // mock 边界：ChannelGateway 返回 channel→cluster 映射
            com.codingas.gateway.domain.supply.gateway.ChannelGateway mockChannelGateway =
                    mock(com.codingas.gateway.domain.supply.gateway.ChannelGateway.class);
            com.codingas.gateway.domain.supply.entity.Channel chA = channel(100L, 10L);
            com.codingas.gateway.domain.supply.entity.Channel chB = channel(200L, 20L);
            when(mockChannelGateway.findByIds(java.util.List.of(100L, 200L)))
                    .thenReturn(java.util.List.of(chA, chB));

            // mock 边界：EndpointResolver 返回 channel→endpoint 映射
            EndpointResolver mockEndpointResolver = mock(EndpointResolver.class);
            com.codingas.gateway.domain.supply.entity.ChannelEndpoint epA =
                    endpoint(9401L, 100L);
            com.codingas.gateway.domain.supply.entity.ChannelEndpoint epB =
                    endpoint(9402L, 200L);
            when(mockEndpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(epA);
            when(mockEndpointResolver.resolve(200L, Protocol.OPENAI)).thenReturn(epB);

            // 真实熔断器：clusterA 域端点 9401 全熔断 OPEN；clusterB 域端点 9402 健康 CLOSED
            forceCircuitOpen(9401L);

            // 手动构造真实路由器：注入真实聚合器（读真实熔断器）+ mock 边界 Gateway
            ClusterAffinityRouter router = new ClusterAffinityRouter(
                    mockChannelGateway, realClusterHealthAggregator, mockEndpointResolver);

            RoutingRequest request = new RoutingRequest(
                    1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI, null);
            java.util.List<com.codingas.gateway.domain.supply.entity.ModelInstance> result =
                    router.filter(java.util.List.of(miA, miB), request);

            // 断言：DOWN 域（clusterA）实例被过滤，HEALTHY 域（clusterB）实例保留 → 触发跨域转移
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("域内部分端点熔断 → DEGRADED 域实例保留（容量受损但不跨域转移）")
        void degradedClusterKept_endToEnd() {
            // 构造同域两实例：clusterA(10) channel=100/101 endpoint=9501/9502
            //   endpoint 9501 OPEN，endpoint 9502 CLOSED → DEGRADED → 保留整域
            com.codingas.gateway.domain.supply.entity.ModelInstance mi1 =
                    instance(1L, 100L);
            com.codingas.gateway.domain.supply.entity.ModelInstance mi2 =
                    instance(2L, 101L);

            com.codingas.gateway.domain.supply.gateway.ChannelGateway mockChannelGateway =
                    mock(com.codingas.gateway.domain.supply.gateway.ChannelGateway.class);
            when(mockChannelGateway.findByIds(java.util.List.of(100L, 101L)))
                    .thenReturn(java.util.List.of(channel(100L, 10L), channel(101L, 10L)));

            EndpointResolver mockEndpointResolver = mock(EndpointResolver.class);
            when(mockEndpointResolver.resolve(100L, Protocol.OPENAI))
                    .thenReturn(endpoint(9501L, 100L));
            when(mockEndpointResolver.resolve(101L, Protocol.OPENAI))
                    .thenReturn(endpoint(9502L, 101L));

            // 真实熔断器：9501 OPEN，9502 CLOSED → DEGRADED
            forceCircuitOpen(9501L);

            ClusterAffinityRouter router = new ClusterAffinityRouter(
                    mockChannelGateway, realClusterHealthAggregator, mockEndpointResolver);

            RoutingRequest request = new RoutingRequest(
                    1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI, null);
            java.util.List<com.codingas.gateway.domain.supply.entity.ModelInstance> result =
                    router.filter(java.util.List.of(mi1, mi2), request);

            // 断言：DEGRADED 域实例全部保留（不跨域转移）
            assertThat(result).hasSize(2);
        }

        /** 构造测试用 ModelInstance */
        private com.codingas.gateway.domain.supply.entity.ModelInstance instance(
                long id, long channelId) {
            com.codingas.gateway.domain.supply.entity.ModelInstance mi =
                    new com.codingas.gateway.domain.supply.entity.ModelInstance();
            mi.setId(id);
            mi.setChannelId(channelId);
            return mi;
        }

        /** 构造测试用 Channel */
        private com.codingas.gateway.domain.supply.entity.Channel channel(long id, Long clusterId) {
            com.codingas.gateway.domain.supply.entity.Channel ch =
                    new com.codingas.gateway.domain.supply.entity.Channel();
            ch.setId(id);
            ch.setClusterId(clusterId);
            return ch;
        }

        /** 构造测试用 ChannelEndpoint */
        private com.codingas.gateway.domain.supply.entity.ChannelEndpoint endpoint(
                long id, long channelId) {
            com.codingas.gateway.domain.supply.entity.ChannelEndpoint ep =
                    new com.codingas.gateway.domain.supply.entity.ChannelEndpoint();
            ep.setId(id);
            ep.setChannelId(channelId);
            return ep;
        }

        /** 强制端点熔断器进入 OPEN 状态（连续 10 次失败快速熔断） */
        private void forceCircuitOpen(long endpointId) {
            com.codingas.gateway.infrastructure.resilience.CircuitBreaker breaker =
                    realCircuitBreakerManager.getBreaker(endpointId);
            for (int i = 0; i < 10; i++) {
                breaker.recordFailure();
                if (breaker.getState() == com.codingas.gateway.infrastructure.resilience.CircuitBreakerState.OPEN) {
                    return;
                }
            }
        }
    }
}
