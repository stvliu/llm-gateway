package com.codingas.gateway.web.service;

import com.codingas.gateway.adapter.StreamCallback;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.core.domain.entity.RouteGroup;
import com.codingas.gateway.router.LLMDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LLMChatUseCase 单元测试
 *
 * <p>测试 Application 层用例编排器是否正确：
 * <ul>
 *   <li>调用 LLMDispatcher 执行 LLM 请求</li>
 *   <li>发布 TokenUsedEvent 事件</li>
 *   <li>传递路由策略</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LLMChatUseCase Tests")
class LLMChatUseCaseTest {

    @Mock
    private LLMDispatcher llmDispatcher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LLMChatUseCase llmChatUseCase;

    private LLMRequest testRequest;
    private LLMResponse testResponse;

    @BeforeEach
    void setUp() {
        testRequest = LLMRequest.builder()
                .model("gpt-4o")
                .messages(List.of(
                        LLMRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .temperature(0.7)
                .maxTokens(1000)
                .build();

        testResponse = LLMResponse.builder()
                .id("chatcmpl-123")
                .model("gpt-4o")
                .content(LLMResponse.Content.builder()
                        .text("Hello! How can I help you?")
                        .role("assistant")
                        .build())
                .usage(LLMResponse.Usage.builder()
                        .promptTokens(10)
                        .completionTokens(20)
                        .totalTokens(30)
                        .build())
                .finishReason("stop")
                .build();
    }

    @Nested
    @DisplayName("send")
    class SendTests {

        @Test
        @DisplayName("发送请求并返回响应")
        void send_success() {
            when(llmDispatcher.send(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class)))
                    .thenReturn(testResponse);

            LLMResponse result = llmChatUseCase.send(testRequest, RouteGroup.RoutingStrategy.PRIORITY);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("chatcmpl-123");
            assertThat(result.getContent().getText()).isEqualTo("Hello! How can I help you?");
        }

        @Test
        @DisplayName("发送请求时使用正确的路由策略")
        void send_usesCorrectStrategy() {
            when(llmDispatcher.send(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class)))
                    .thenReturn(testResponse);

            llmChatUseCase.send(testRequest, RouteGroup.RoutingStrategy.ROUND_ROBIN);

            ArgumentCaptor<RouteGroup.RoutingStrategy> strategyCaptor =
                    ArgumentCaptor.forClass(RouteGroup.RoutingStrategy.class);
            verify(llmDispatcher).send(any(LLMRequest.class), strategyCaptor.capture());
            assertThat(strategyCaptor.getValue()).isEqualTo(RouteGroup.RoutingStrategy.ROUND_ROBIN);
        }

        @Test
        @DisplayName("响应包含 usage 时发布 TokenUsedEvent")
        void send_publishesEventWhenUsagePresent() {
            when(llmDispatcher.send(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class)))
                    .thenReturn(testResponse);

            llmChatUseCase.send(testRequest, RouteGroup.RoutingStrategy.PRIORITY);

            ArgumentCaptor<LLMChatUseCase.TokenUsedEvent> eventCaptor =
                    ArgumentCaptor.forClass(LLMChatUseCase.TokenUsedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            LLMChatUseCase.TokenUsedEvent event = eventCaptor.getValue();
            assertThat(event.model()).isEqualTo("gpt-4o");
            assertThat(event.promptTokens()).isEqualTo(10);
            assertThat(event.completionTokens()).isEqualTo(20);
        }

        @Test
        @DisplayName("响应不包含 usage 时不发布事件")
        void send_doesNotPublishEventWhenNoUsage() {
            LLMResponse responseWithoutUsage = LLMResponse.builder()
                    .id("chatcmpl-124")
                    .model("gpt-4o")
                    .content(LLMResponse.Content.builder()
                            .text("Hello")
                            .role("assistant")
                            .build())
                    .finishReason("stop")
                    .build();

            when(llmDispatcher.send(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class)))
                    .thenReturn(responseWithoutUsage);

            llmChatUseCase.send(testRequest, RouteGroup.RoutingStrategy.PRIORITY);

            verify(eventPublisher, never()).publishEvent(any(LLMChatUseCase.TokenUsedEvent.class));
        }
    }

    @Nested
    @DisplayName("sendStream")
    class SendStreamTests {

        @Test
        @DisplayName("发送流式请求")
        void sendStream_success() {
            StreamCallback callback = mock(StreamCallback.class);

            llmChatUseCase.sendStream(testRequest, RouteGroup.RoutingStrategy.PRIORITY, callback);

            verify(llmDispatcher).sendStream(
                    eq(testRequest),
                    eq(RouteGroup.RoutingStrategy.PRIORITY),
                    eq(callback)
            );
        }
    }

    @Nested
    @DisplayName("sendAsync")
    class SendAsyncTests {

        @Test
        @DisplayName("异步发送请求")
        void sendAsync_success() throws Exception {
            when(llmDispatcher.send(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class)))
                    .thenReturn(testResponse);

            CompletableFuture<LLMResponse> future =
                    llmChatUseCase.sendAsync(testRequest, RouteGroup.RoutingStrategy.PRIORITY);

            LLMResponse result = future.get();
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("chatcmpl-123");
        }
    }
}
