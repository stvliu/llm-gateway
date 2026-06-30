package com.codingas.gateway.integration;

import com.codingas.gateway.application.proxy.OutboundTuner;
import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.application.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.application.proxy.invoker.KeyFailoverInvoker;
import com.codingas.gateway.application.proxy.routing.RouterChain;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
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
 * <p>Task 5 适配：DomainHealth 域级聚合路由器（ClusterHealthAggregator + ClusterAffinityRouter）
 * 已删除，RouterChain 收敛为端点级健康过滤。本测试移除域级聚合/亲和路由端到端用例，
 * 新增 RouterChain 组成断言验证域级聚合路由器已不在责任链中。</p>
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
        // ChannelGateway 用 no-op mock（集成测试不验证 clusterId 反查，由 ChannelFailoverInvokerTest 覆盖）
        // OutboundTuner 用 mock + returnsFirstArg（调谐下沉后每候选 tune，集成测试聚焦真实分流表非调谐）
        // ProtocolConverter 用 no-op mock（集成测试候选均为同协议，不触发跨协议转换）
        OutboundTuner tuner = mock(OutboundTuner.class);
        lenient().when(tuner.tune(any(ProtocolRequest.class), any(RoutingContext.class)))
                .thenAnswer(org.mockito.AdditionalAnswers.returnsFirstArg());
        return new ChannelFailoverInvoker(keyFailoverInvoker, realErrorClassifier,
                mock(DomainEventPublisher.class), mock(com.codingas.gateway.domain.supply.gateway.ChannelGateway.class),
                tuner, mock(ProtocolConverter.class));
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

    // ==================== RouterChain 组成（DomainHealth 域级聚合移除验证，Task 5） ====================

    /** 真实路由器责任链 bean（Spring 装配，按 @Order 自动收集所有 Router） */
    @Autowired
    private RouterChain realRouterChain;

    @Nested
    @DisplayName("RouterChain 组成（DomainHealth 域级聚合移除验证，Task 5）")
    class RouterChainCompositionTests {

        @Test
        @DisplayName("RouterChain 不含域级聚合路由器 ClusterAffinityRouter（DomainHealth 已移除）")
        void routerChain_excludesDomainHealthAggregator() throws Exception {
            // 读取 RouterChain 已按 @Order 排序的责任链路由器类名
            List<String> routerNames = readRouterClassNames(realRouterChain);

            // DomainHealth 域级聚合路由器已删除，责任链不应再包含它
            assertThat(routerNames).doesNotContain("ClusterAffinityRouter");
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
