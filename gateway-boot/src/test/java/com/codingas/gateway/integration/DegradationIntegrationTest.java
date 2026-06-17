package com.codingas.gateway.integration;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.invoker.DegradationInvoker;
import com.codingas.gateway.application.proxy.invoker.KeyFailoverInvoker;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 模型降级集成测试
 *
 * <p>测试真实的 {@link DegradationInvoker}，Mock 其下游依赖
 * （{@link KeyFailoverInvoker}、{@link DegradationService}、{@link RoutingResolver}），
 * 验证主模型失败后降级到备选模型、以及降级链耗尽时抛出原异常的完整流程。</p>
 *
 * <p>本测试使用真实的 {@link ProtocolRequest}（匿名实现），以验证
 * {@code setModel} 在降级过程中的真实副作用，更贴近集成语义。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("模型降级集成测试")
class DegradationIntegrationTest {

    @Mock
    private KeyFailoverInvoker keyFailoverInvoker;

    @Mock
    private DegradationService degradationService;

    @Mock
    private RoutingResolver routingResolver;

    /** 被测真实对象 */
    private DegradationInvoker degradationInvoker;

    @BeforeEach
    void setUp() {
        // 手动构造真实 DegradationInvoker，注入 Mock 依赖
        degradationInvoker = new DegradationInvoker(keyFailoverInvoker, degradationService, routingResolver);
    }

    /**
     * 创建真实的匿名 ProtocolRequest，支持 setModel 副作用。
     *
     * @param model  初始模型名称
     * @param stream 是否流式
     * @return ProtocolRequest 实例
     */
    private ProtocolRequest createTestRequest(String model, boolean stream) {
        return new ProtocolRequest() {
            private String m = model;
            private boolean s = stream;

            @Override public String getModel() { return m; }
            @Override public void setModel(String model) { this.m = model; }
            @Override public String getProtocol() { return "openai"; }
            @Override public boolean isStream() { return s; }
            @Override public void setStream(boolean stream) { this.s = stream; }
        };
    }

    /**
     * 主模型失败时，degrade 返回备选模型，重新路由 + 递归调用成功。
     */
    @Test
    @DisplayName("主模型失败时降级到备选模型并重新调用成功")
    void testDegradation_primaryFails_fallbackSucceeds() {
        // 主模型路由上下文
        RoutingContext primaryCtx = new RoutingContext(10L, 20L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-primary", 60, false, "gpt-4o", null);

        // 备选模型路由上下文（降级后重新路由的结果）
        RoutingContext fallbackCtx = new RoutingContext(11L, 21L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-fallback", 60, false, "gpt-3.5-turbo", null);

        // 真实请求对象：主模型 gpt-4o
        ProtocolRequest request = createTestRequest("gpt-4o", false);

        // 第一次调用抛 ProviderException，第二次返回成功响应
        ProviderException upstreamError =
                new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "上游错误");
        ProtocolResponse successResponse = new ProtocolResponse() {
            @Override public String getModel() { return "gpt-3.5-turbo"; }
            @Override public String getFinishReason() { return "stop"; }
        };
        when(keyFailoverInvoker.invoke(any(RoutingContext.class), any(ProtocolRequest.class)))
                .thenThrow(upstreamError)
                .thenReturn(successResponse);

        // 降级服务返回备选模型
        when(degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR))
                .thenReturn("gpt-3.5-turbo");

        // 重新路由解析返回备选上下文
        when(routingResolver.resolve("gpt-3.5-turbo", Protocol.OPENAI, 1L, "USER", RoutingStrategy.WEIGHTED))
                .thenReturn(fallbackCtx);

        // 执行降级调用
        ProtocolResponse result = degradationInvoker.invoke(
                primaryCtx, request, Protocol.OPENAI, 1L, "USER", RoutingStrategy.WEIGHTED);

        // 验证返回成功响应（非 null）
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(successResponse);

        // 验证请求模型已被降级覆盖为备选模型
        assertThat(request.getModel()).isEqualTo("gpt-3.5-turbo");

        // 验证降级链路调用轨迹
        verify(degradationService).degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);
        verify(routingResolver).resolve("gpt-3.5-turbo", Protocol.OPENAI, 1L, "USER", RoutingStrategy.WEIGHTED);
        // KeyFailoverInvoker 被调用两次：第一次失败，第二次成功
        verify(keyFailoverInvoker, times(2)).invoke(any(RoutingContext.class), any(ProtocolRequest.class));
    }

    /**
     * degrade 返回 null（降级链耗尽），验证抛出原 ProviderException。
     */
    @Test
    @DisplayName("降级链耗尽时抛出原 ProviderException")
    void testDegradation_chainExhausted() {
        // 主模型路由上下文
        RoutingContext primaryCtx = new RoutingContext(10L, 20L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-primary", 60, false, "gpt-4o", null);

        // 真实请求对象：主模型 gpt-4o
        ProtocolRequest request = createTestRequest("gpt-4o", false);

        // 主模型调用抛 ProviderException
        ProviderException upstreamError =
                new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "上游错误");
        when(keyFailoverInvoker.invoke(primaryCtx, request)).thenThrow(upstreamError);

        // 降级链耗尽，无可用备选
        when(degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR)).thenReturn(null);

        // 验证抛出原异常（同一引用）
        assertThatThrownBy(() -> degradationInvoker.invoke(
                primaryCtx, request, Protocol.OPENAI, 1L, "USER", RoutingStrategy.WEIGHTED))
                .isSameAs(upstreamError)
                .isInstanceOf(ProviderException.class);

        // 验证请求模型未被修改（降级未生效）
        assertThat(request.getModel()).isEqualTo("gpt-4o");

        // 降级链耗尽时不应重新路由
        verify(routingResolver, org.mockito.Mockito.never())
                .resolve(any(), any(), any(), any(), any());
    }
}
