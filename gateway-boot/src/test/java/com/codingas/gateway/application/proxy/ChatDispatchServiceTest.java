package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.application.proxy.invoker.L2DegradationRequiredException;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.application.resilience.ResilienceResolver;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.common.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatDispatchService 单元测试
 *
 * <p>Task 3.6 适配：DegradationInvoker 退场后，ChatDispatchServiceImpl 改用
 * {@link ChannelFailoverInvoker}。测试 mock ChannelFailoverInvoker + RoutingResolver.resolveCandidates，
 * 覆盖 L2 降级重路由循环（L2DegradationRequiredException 捕获 → fallback 重新路由）与防递归守卫。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatDispatchService 单元测试")
class ChatDispatchServiceTest {

    @Mock
    private RoutingResolver routingResolver;

    @Mock
    private OutboundTuner outboundTuner;

    @Mock
    private ProtocolConverter protocolConverter;

    @Mock
    private AuditGateway auditGateway;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ChannelFailoverInvoker channelFailoverInvoker;

    @Mock
    private ResilienceResolver resilienceResolver;

    private ChatDispatchServiceImpl dispatchService;

    private Identity testIdentity;
    private RoutingContext openAIContext;

    @BeforeEach
    void setUp() {
        dispatchService = new ChatDispatchServiceImpl(routingResolver, outboundTuner,
                protocolConverter, auditGateway, eventPublisher, channelFailoverInvoker, resilienceResolver);

        testIdentity = Identity.of(1L, "USER", 1L, 7L);
        openAIContext = new RoutingContext(10L, 20L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-test", 60, false, "test-model", null);

        lenient().when(auditGateway.saveCallLog(any())).thenReturn(null);
        // 容灾画像解析返回启用 L2 的测试画像（Task 4.9 profile 贯穿 Invoker 链）
        lenient().when(resilienceResolver.resolve(any())).thenReturn(testProfile());
    }

    /** 构造启用 L2 降级的测试画像（供 Invoker 链门禁） */
    private ResilienceProfile testProfile() {
        ResilienceProfile p = new ResilienceProfile();
        p.setEnableL2ModelDegradation(true);
        p.setDegradationMaxDepth(5);
        return p;
    }

    @Nested
    @DisplayName("dispatch 非流式调度")
    class DispatchTests {

        @Test
        @DisplayName("同协议调度：OpenAI→OpenAI，委托 ChannelFailoverInvoker")
        void dispatch_sameProtocol_noConversion() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            OpenAIChatResponse response = OpenAIChatResponse.builder().id("chatcmpl-123").model("gpt-4o").build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            lenient().when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(request);
            lenient().when(channelFailoverInvoker.invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class))).thenReturn(response);

            // when
            ProtocolResponse result = dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            assertThat(result).isInstanceOf(OpenAIChatResponse.class);
            verify(protocolConverter, never()).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter, never()).toOpenAI(any(AnthropicMessagesResponse.class));
            verify(channelFailoverInvoker).invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class));
        }

        @Test
        @DisplayName("跨协议调度：OpenAI→Anthropic")
        void dispatch_crossProtocol_withConversion() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            RoutingContext anthropicContext = new RoutingContext(10L, 21L, "https://api.anthropic.com",
                    Protocol.ANTHROPIC, "sk-ant-key", 60, true, "test-model", null);

            AnthropicMessagesRequest convertedRequest = AnthropicMessagesRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .messages(List.of(AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()))
                    .maxTokens(1024)
                    .build();

            AnthropicMessagesResponse upstreamResponse = AnthropicMessagesResponse.builder()
                    .id("msg-123").model("claude-3-5-sonnet-20241022").build();

            OpenAIChatResponse finalResponse = OpenAIChatResponse.builder().id("chatcmpl-123").model("gpt-4o").build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(anthropicContext));
            lenient().when(protocolConverter.toAnthropic(any(OpenAIChatRequest.class))).thenReturn(convertedRequest);
            lenient().when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(convertedRequest);
            lenient().when(channelFailoverInvoker.invoke(eq(anthropicContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class))).thenReturn(upstreamResponse);
            lenient().when(protocolConverter.toOpenAI(any(AnthropicMessagesResponse.class))).thenReturn(finalResponse);

            // when
            ProtocolResponse result = dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            assertThat(result).isInstanceOf(OpenAIChatResponse.class);
            verify(protocolConverter).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter).toOpenAI(any(AnthropicMessagesResponse.class));
        }

        @Test
        @DisplayName("不支持的请求类型抛出异常")
        void dispatch_unsupportedRequestType_throwsException() {
            // given — 使用不属于任何已知协议类型的 mock
            ProtocolRequest unsupportedRequest = mock(ProtocolRequest.class);

            // when & then
            assertThatThrownBy(() -> dispatchService.dispatch(unsupportedRequest, testIdentity, RoutingStrategy.WEIGHTED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不支持的协议类型");
        }

        @Test
        @DisplayName("L2 降级：ChannelFailoverInvoker 抛 L2DegradationRequiredException 后用 fallback 重新路由成功")
        void dispatch_l2Degradation_reroutesWithFallback() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            RoutingContext fallbackCtx = new RoutingContext(11L, 21L, "https://api.openai.com/v1",
                    Protocol.OPENAI, "sk-fb", 60, false, "gpt-3.5-turbo", null);
            OpenAIChatResponse fallbackResponse = OpenAIChatResponse.builder().id("chatcmpl-fb").model("gpt-3.5-turbo").build();

            // 主模型候选列表 + fallback 候选列表
            when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            when(routingResolver.resolveCandidates("gpt-3.5-turbo", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(fallbackCtx));
            when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(request);

            // 首次 invoke(primaryCtx) 抛 L2 降级信号（fallback=gpt-3.5-turbo），第二次 invoke(fallbackCtx) 返回成功
            ProviderException originalEx = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "上游错误");
            L2DegradationRequiredException l2Ex = new L2DegradationRequiredException(
                    "gpt-3.5-turbo", "gpt-4o", ProviderErrorType.UPSTREAM_ERROR, originalEx);
            when(channelFailoverInvoker.invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class))).thenThrow(l2Ex);
            when(channelFailoverInvoker.invoke(eq(fallbackCtx), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class))).thenReturn(fallbackResponse);

            // when
            ProtocolResponse result = dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            assertThat(result).isSameAs(fallbackResponse);
            // resolveCandidates 调两次：主模型 + fallback
            verify(routingResolver).resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);
            verify(routingResolver).resolveCandidates("gpt-3.5-turbo", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);
            // ChannelFailoverInvoker.invoke 调两次
            verify(channelFailoverInvoker, times(2)).invoke(any(RoutingContext.class), anyList(),
                    any(ProtocolRequest.class), eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class));
        }

        @Test
        @DisplayName("L2 降级防递归：fallback 已降级过时抛出原始上游异常")
        void dispatch_l2Degradation_loopGuard_throwsOriginal() {
            // given — fallback 指回原始模型（自环），应被防递归守卫拦截
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            ProviderException originalEx = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "原始上游错误");
            // fallback=gpt-4o 与原始模型相同 → 自环，防递归应拦截
            L2DegradationRequiredException l2Ex = new L2DegradationRequiredException(
                    "gpt-4o", "gpt-4o", ProviderErrorType.UPSTREAM_ERROR, originalEx);

            when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(request);
            when(channelFailoverInvoker.invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class))).thenThrow(l2Ex);

            // when & then
            assertThatThrownBy(() -> dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED))
                    .isSameAs(originalEx);

            // 防递归拦截：只调一次 resolveCandidates（不重路由）
            verify(routingResolver, times(1))
                    .resolveCandidates(anyString(), any(Protocol.class), anyLong(), anyLong(), anyString(), any(RoutingStrategy.class));
            verify(channelFailoverInvoker, times(1)).invoke(any(RoutingContext.class), anyList(),
                    any(ProtocolRequest.class), eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class));
        }

        @Test
        @DisplayName("L2 降级深度超限：持续降级超过上限后抛出 ProviderException")
        void dispatch_l2Degradation_depthLimit_throwsOriginal() {
            // given — invoke 每次抛不同 fallback 的 L2（永不重复，不走防递归），触发深度上限兜底
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            ProviderException originalEx = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "原始上游错误");
            // 用 thenAnswer 生成递增 fallback，确保不触发去重守卫
            int[] counter = {0};
            when(channelFailoverInvoker.invoke(any(RoutingContext.class), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class))).thenAnswer(inv -> {
                throw new L2DegradationRequiredException(
                        "fb-" + (counter[0]++), "gpt-4o", ProviderErrorType.UPSTREAM_ERROR, originalEx);
            });
            // 任意 fallback 均返回非空候选列表，避免因候选空提前终止
            when(routingResolver.resolveCandidates(anyString(), eq(Protocol.OPENAI), eq(7L), eq(1L), eq("USER"), eq(RoutingStrategy.WEIGHTED)))
                    .thenReturn(List.of(openAIContext));
            when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(request);

            // when & then — 深度超限后抛出异常（保留原始失败上下文 cause）
            assertThatThrownBy(() -> dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED))
                    .isInstanceOf(ProviderException.class);

            // 验证多次降级尝试（深度上限兜底生效，非无限循环）
            verify(channelFailoverInvoker, atLeast(2)).invoke(any(RoutingContext.class), anyList(),
                    any(ProtocolRequest.class), eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class));
        }
    }

    @Nested
    @DisplayName("dispatchStream 流式调度")
    class DispatchStreamTests {

        @Test
        @DisplayName("同协议流式调度：委托给 ChannelFailoverInvoker")
        void dispatchStream_sameProtocol_delegatesToChannelFailoverInvoker() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            lenient().when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(request);

            StreamCallback callback = mock(StreamCallback.class);

            // when
            dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, callback);

            // then
            verify(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class), any(StreamCallback.class));
            verify(protocolConverter, never()).convertStreamChunk(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("ChannelFailoverInvoker 抛出异常时传递到调用方")
        void dispatchStream_invokerError_propagatesException() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            lenient().when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(request);

            doThrow(new RuntimeException("上游调用失败"))
                    .when(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                            eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class), any(StreamCallback.class));

            StreamCallback callback = mock(StreamCallback.class);

            // when & then
            assertThatThrownBy(() -> dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, callback))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("上游调用失败");
        }

        @Test
        @DisplayName("流式 L2 降级：invokeStream 抛 L2DegradationRequiredException 后用 fallback 重新路由")
        void dispatchStream_l2Degradation_reroutesWithFallback() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            RoutingContext fallbackCtx = new RoutingContext(11L, 21L, "https://api.openai.com/v1",
                    Protocol.OPENAI, "sk-fb", 60, false, "gpt-3.5-turbo", null);

            when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            when(routingResolver.resolveCandidates("gpt-3.5-turbo", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(fallbackCtx));
            when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(request);

            // 首次 invokeStream(openAIContext) 抛 L2 降级信号，第二次 invokeStream(fallbackCtx) 正常返回（doNothing）
            ProviderException originalEx = new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "上游错误");
            L2DegradationRequiredException l2Ex = new L2DegradationRequiredException(
                    "gpt-3.5-turbo", "gpt-4o", ProviderErrorType.UPSTREAM_ERROR, originalEx);
            doThrow(l2Ex).when(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class), any(StreamCallback.class));

            StreamCallback callback = mock(StreamCallback.class);

            // when — 重路由后流建立成功，不抛异常
            dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, callback);

            // then — 两次 invokeStream：主模型 + fallback
            verify(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class), any(StreamCallback.class));
            verify(channelFailoverInvoker).invokeStream(eq(fallbackCtx), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), any(ResilienceProfile.class), any(StreamCallback.class));
        }
    }
}
