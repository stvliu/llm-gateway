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
package com.codingas.gateway.proxy.chat;

import com.codingas.gateway.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.proxy.routing.RoutingResolver;
import com.codingas.gateway.proxy.conversion.ProtocolConversionFacade;
import com.codingas.gateway.protocol.*;
import com.codingas.gateway.protocol.contract.*;
import com.codingas.gateway.protocol.transport.ProviderException;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.upstream.RoutingStrategy;
import com.codingas.gateway.provider.upstream.RoutingContext;
import com.codingas.gateway.common.enums.FailureStrategy;
import com.codingas.gateway.audit.AuditLogRepository;
import com.codingas.gateway.iam.auth.Identity;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.usage.event.TokenUsedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatDispatchManager 单元测试
 *
 * <p>Task 4 适配：L2 模型降级层已删除，ChatDispatchManagerImpl 不再解析容灾画像、不再做 L2 重路由循环，
 * 直接委托 {@link ChannelFailoverInvoker}（候选内逐个试，耗尽抛最后异常）。invoke/invokeStream 签名
 * 移除 profile 参数。测试覆盖同协议/跨协议调度委托、异常传播等核心语义。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatDispatchManager 单元测试")
class ChatDispatchManagerTest {

    @Mock
    private RoutingResolver routingResolver;

    @Mock
    private ProtocolConversionFacade protocolConversionFacade;

    @Mock
    private AuditLogRepository auditRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ChannelFailoverInvoker channelFailoverInvoker;

    private ChatDispatchManagerImpl dispatchService;

    private Identity testIdentity;
    private RoutingContext openAIContext;

    @BeforeEach
    void setUp() {
        dispatchService = new ChatDispatchManagerImpl(routingResolver,
                auditRepository, eventPublisher, channelFailoverInvoker);

        testIdentity = Identity.of(1L, "USER", 1L, 7L);
        openAIContext = new RoutingContext(10L, 20L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-test", 60, false, "test-model", null,
                FailureStrategy.FAIL_RETRY);

        lenient().when(auditRepository.saveCallLog(any())).thenReturn(null);
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
            verify(protocolConversionFacade, never()).convertRequest(any(ProtocolRequest.class), anyString());
            verify(protocolConversionFacade, never()).convertResponse(any(ProtocolResponse.class), anyString());
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
            verify(protocolConversionFacade, never()).convertRequest(any(ProtocolRequest.class), anyString());
            verify(protocolConversionFacade, never()).convertResponse(any(ProtocolResponse.class), anyString());
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

        @Test
        @DisplayName("调度成功：审计成功日志并发布 OpenAI Token 使用事件")
        void dispatch_success_publishesOpenAiUsage() {
            // given — OpenAI 响应带 usage，触发 Token 计量
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            OpenAIChatResponse response = OpenAIChatResponse.builder()
                    .id("chatcmpl-1").model("gpt-4o")
                    .usage(OpenAIChatResponse.Usage.builder().promptTokens(10).completionTokens(5).build())
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            lenient().when(channelFailoverInvoker.invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString())).thenReturn(response);

            // when
            ProtocolResponse result = dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then — 审计成功 + Token 事件（OpenAI promptTokens/completionTokens 映射）
            assertThat(result).isSameAs(response);
            verify(auditRepository).saveCallLog(argThat(log ->
                    Boolean.TRUE.equals(log.getSuccess()) && "gpt-4o".equals(log.getModel())));
            ArgumentCaptor<TokenUsedEvent> eventCaptor =
                    ArgumentCaptor.forClass(TokenUsedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            TokenUsedEvent event = eventCaptor.getValue();
            assertThat(event.userId()).isEqualTo(1L);
            assertThat(event.apiKeyId()).isEqualTo(1L);
            assertThat(event.provider()).isEqualTo("openai");
            assertThat(event.promptTokens()).isEqualTo(10);
            assertThat(event.completionTokens()).isEqualTo(5);
        }

        @Test
        @DisplayName("调度成功（Anthropic 响应）发布 Anthropic Token 使用事件")
        void dispatch_success_publishesAnthropicUsage() {
            // given — Anthropic 响应带 usage（inputTokens/outputTokens）
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            RoutingContext anthropicContext = new RoutingContext(10L, 21L, "https://api.anthropic.com",
                    Protocol.ANTHROPIC, "sk-ant-key", 60, true, "test-model", null,
                    FailureStrategy.FAIL_RETRY);

            AnthropicMessagesResponse response = AnthropicMessagesResponse.builder()
                    .id("msg-1").model("claude-3-5-sonnet")
                    .usage(AnthropicMessagesResponse.Usage.builder().inputTokens(100).outputTokens(50).build())
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(anthropicContext));
            lenient().when(channelFailoverInvoker.invoke(eq(anthropicContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString())).thenReturn(response);

            // when
            dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            ArgumentCaptor<TokenUsedEvent> eventCaptor =
                    ArgumentCaptor.forClass(TokenUsedEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            TokenUsedEvent event = eventCaptor.getValue();
            assertThat(event.provider()).isEqualTo("anthropic");
            assertThat(event.promptTokens()).isEqualTo(100);
            assertThat(event.completionTokens()).isEqualTo(50);
        }

        @Test
        @DisplayName("响应无 usage 时不发布 Token 事件")
        void dispatch_success_noUsage_noEvent() {
            // given — usage 为 null → publishTokenUsedEvent 分支不触发
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            OpenAIChatResponse response = OpenAIChatResponse.builder().id("chatcmpl-1").model("gpt-4o").build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            lenient().when(channelFailoverInvoker.invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString())).thenReturn(response);

            // when
            dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            verify(eventPublisher, never()).publish(any());
            verify(auditRepository).saveCallLog(argThat(log -> Boolean.TRUE.equals(log.getSuccess())));
        }

        @Test
        @DisplayName("调度失败：审计失败日志（success=false + errorMessage）")
        void dispatch_failure_auditsFailureLog() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));
            lenient().when(channelFailoverInvoker.invoke(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString()))
                    .thenThrow(new RuntimeException("上游调用失败"));

            // when & then
            assertThatThrownBy(() -> dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED))
                    .isInstanceOf(RuntimeException.class);
            verify(auditRepository).saveCallLog(argThat(log ->
                    Boolean.FALSE.equals(log.getSuccess())
                            && "上游调用失败".equals(log.getErrorMessage())
                            && log.getDurationMs() != null));
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
            verify(protocolConversionFacade, never()).convertStreamChunk(anyString(), anyString(), anyString());
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

        @Test
        @DisplayName("流式回调 onChunk 透传给用户回调")
        void dispatchStream_callback_onChunk_passthrough() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));

            StreamCallback userCallback = mock(StreamCallback.class);
            ArgumentCaptor<StreamCallback> auditCaptor = ArgumentCaptor.forClass(StreamCallback.class);
            dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, userCallback);

            // when — 捕获审计包装回调并驱动 onChunk
            verify(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString(), auditCaptor.capture());
            auditCaptor.getValue().onChunk("data chunk");

            // then — 原样透传
            verify(userCallback).onChunk("data chunk");
        }

        @Test
        @DisplayName("流式回调 onComplete 审计成功并回调用户")
        void dispatchStream_callback_onComplete_auditsAndCompletes() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));

            StreamCallback userCallback = mock(StreamCallback.class);
            ArgumentCaptor<StreamCallback> auditCaptor = ArgumentCaptor.forClass(StreamCallback.class);
            dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, userCallback);
            verify(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString(), auditCaptor.capture());

            // when
            auditCaptor.getValue().onComplete();

            // then — 审计成功日志 + 通知用户完成
            verify(auditRepository).saveCallLog(argThat(log -> Boolean.TRUE.equals(log.getSuccess())));
            verify(userCallback).onComplete();
        }

        @Test
        @DisplayName("流式回调 onError（ProviderException）格式化为 SSE 错误并审计失败")
        void dispatchStream_callback_onError_providerException() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));

            StreamCallback userCallback = mock(StreamCallback.class);
            ArgumentCaptor<StreamCallback> auditCaptor = ArgumentCaptor.forClass(StreamCallback.class);
            dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, userCallback);
            verify(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString(), auditCaptor.capture());

            // when — ProviderException 触发 SseErrorFormatter 格式化
            ProviderException pe = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR, "限流",
                    "trace-1", "gpt-4o", "openai", 20L, 30);
            auditCaptor.getValue().onError(pe);

            // then — 审计失败日志记录格式化后的 SSE 错误 JSON
            verify(auditRepository).saveCallLog(argThat(log ->
                    Boolean.FALSE.equals(log.getSuccess())
                            && log.getErrorMessage() != null
                            && log.getErrorMessage().contains("rate_limit")
                            && log.getErrorMessage().contains("30")));
            verify(userCallback).onError(argThat(t ->
                    t instanceof RuntimeException && t.getMessage().contains("rate_limit")));
        }

        @Test
        @DisplayName("流式回调 onError（非 ProviderException）使用 unknown_error 兜底")
        void dispatchStream_callback_onError_unknownError() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            lenient().when(routingResolver.resolveCandidates("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .thenReturn(List.of(openAIContext));

            StreamCallback userCallback = mock(StreamCallback.class);
            ArgumentCaptor<StreamCallback> auditCaptor = ArgumentCaptor.forClass(StreamCallback.class);
            dispatchService.dispatchStream(request, testIdentity, RoutingStrategy.WEIGHTED, userCallback);
            verify(channelFailoverInvoker).invokeStream(eq(openAIContext), anyList(), any(ProtocolRequest.class),
                    eq(Protocol.OPENAI), eq(7L), anyString(), auditCaptor.capture());

            // when — 非 ProviderException 走 unknown_error 兜底分支
            auditCaptor.getValue().onError(new IllegalStateException("boom"));

            // then
            verify(auditRepository).saveCallLog(argThat(log ->
                    Boolean.FALSE.equals(log.getSuccess())
                            && log.getErrorMessage() != null
                            && log.getErrorMessage().contains("unknown_error")));
            verify(userCallback).onError(argThat(t ->
                    t instanceof RuntimeException && t.getMessage().contains("unknown_error")));
        }
    }
}
