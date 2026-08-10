/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.application.enums.FailureStrategy;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatDispatchService 单元测试
 *
 * <p>Task 4 适配：L2 模型降级层已删除，ChatDispatchServiceImpl 不再解析容灾画像、不再做 L2 重路由循环，
 * 直接委托 {@link ChannelFailoverInvoker}（候选内逐个试，耗尽抛最后异常）。invoke/invokeStream 签名
 * 移除 profile 参数。测试覆盖同协议/跨协议调度委托、异常传播等核心语义。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatDispatchService 单元测试")
class ChatDispatchServiceTest {

    @Mock
    private RoutingResolver routingResolver;

    @Mock
    private ProtocolConverter protocolConverter;

    @Mock
    private AuditGateway auditGateway;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ChannelFailoverInvoker channelFailoverInvoker;

    private ChatDispatchServiceImpl dispatchService;

    private Identity testIdentity;
    private RoutingContext openAIContext;

    @BeforeEach
    void setUp() {
        dispatchService = new ChatDispatchServiceImpl(routingResolver,
                protocolConverter, auditGateway, eventPublisher, channelFailoverInvoker);

        testIdentity = Identity.of(1L, "USER", 1L, 7L);
        openAIContext = new RoutingContext(10L, 20L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-test", 60, false, "test-model", null,
                FailureStrategy.FAIL_RETRY);

        lenient().when(auditGateway.saveCallLog(any())).thenReturn(null);
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
            lenient().when(channelFailoverInvoker.invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString())).thenReturn(response);

            // when
            ProtocolResponse result = dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            assertThat(result).isInstanceOf(OpenAIChatResponse.class);
            // 阶段3/4 下沉 Invoker：dispatch 不再做请求转换/调谐，protocolConverter 不被 dispatch 调用
            verify(protocolConverter, never()).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter, never()).toOpenAI(any(AnthropicMessagesResponse.class));
            verify(channelFailoverInvoker).invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString());
        }

        @Test
        @DisplayName("跨协议调度：请求/响应转换+调谐均下沉 Invoker，dispatch 传原始 request 并透传响应")
        void dispatch_crossProtocol_withConversion() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            RoutingContext anthropicContext = new RoutingContext(10L, 21L, "https://api.anthropic.com",
                    Protocol.ANTHROPIC, "sk-ant-key", 60, true, "test-model", null,
                    FailureStrategy.FAIL_RETRY);

            // Invoker 已下沉响应转换，返回入站协议格式的最终响应；dispatch 应透传不转换
            AnthropicMessagesResponse invokerResponse = AnthropicMessagesResponse.builder()
                    .id("msg-123").model("claude-3-5-sonnet-20241022").build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(anthropicContext));
            // 阶段3/4/6 均下沉 Invoker：dispatch 传原始 request，Invoker 内部按候选独立 convert+tune+响应转换
            lenient().when(channelFailoverInvoker.invoke(eq(anthropicContext), anyList(), eq(request),
                    eq(Protocol.OPENAI), eq(7L), anyString())).thenReturn(invokerResponse);

            // when
            ProtocolResponse result = dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            assertThat(result).isSameAs(invokerResponse);
            // dispatch 传原始 request（非转换后请求）给 Invoker
            verify(channelFailoverInvoker).invoke(eq(anthropicContext), anyList(), eq(request),
                    eq(Protocol.OPENAI), eq(7L), anyString());
            // 请求转换(toAnthropic) + 响应转换(toOpenAI) 均下沉 Invoker，dispatch 不再调用 protocolConverter
            verify(protocolConverter, never()).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter, never()).toOpenAI(any(AnthropicMessagesResponse.class));
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
        @DisplayName("Invoker 抛出异常时传递到调用方（候选耗尽抛最后异常）")
        void dispatch_invokerError_propagatesException() {
            // given — Invoker 候选耗尽抛出最后异常，dispatch 应透传
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            RuntimeException upstreamEx = new RuntimeException("上游调用失败");
            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            lenient().when(channelFailoverInvoker.invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString())).thenThrow(upstreamEx);

            // when & then
            assertThatThrownBy(() -> dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED))
                    .isSameAs(upstreamEx);
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
            StreamCallback callback = mock(StreamCallback.class);

            // when
            dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, callback);

            // then
            verify(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString(), any(StreamCallback.class));
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
            doThrow(new RuntimeException("上游调用失败"))
                    .when(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                            eq(Protocol.OPENAI), eq(7L), anyString(), any(StreamCallback.class));

            StreamCallback callback = mock(StreamCallback.class);

            // when & then
            assertThatThrownBy(() -> dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, callback))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("上游调用失败");
        }
    }
}
