package com.codingas.gateway.application.chat;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.common.event.TokenUsedEvent;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.service.LLMDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * LLMChatUseCase 单元测试
 */
@ExtendWith(MockitoExtension.class)
class LLMChatUseCaseTest {

    @Mock
    private LLMDispatcher llmDispatcher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LLMChatUseCase llmChatUseCase;

    @Captor
    private ArgumentCaptor<TokenUsedEvent> tokenUsedEventCaptor;

    private LLMRequest testRequest;
    private LLMResponse testResponse;

    @BeforeEach
    void setUp() {
        // 准备测试请求
        testRequest = LLMRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                        LLMRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .build();

        // 准备测试响应
        testResponse = LLMResponse.builder()
                .model("gpt-4")
                .content(LLMResponse.Content.builder()
                        .text("Hello, how can I help you?")
                        .build())
                .usage(LLMResponse.Usage.builder()
                        .promptTokens(10)
                        .completionTokens(20)
                        .totalTokens(30)
                        .build())
                .build();
    }

    @Test
    @DisplayName("send() 成功场景 - 验证调用 llmDispatcher 并发布事件")
    void send_success() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.COST_OPTIMIZED;
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy))).thenReturn(testResponse);

        // When
        LLMResponse result = llmChatUseCase.send(testRequest, strategy);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getModel()).isEqualTo("gpt-4");
        assertThat(result.getContent().getText()).isEqualTo("Hello, how can I help you?");

        // 验证调用了 llmDispatcher
        verify(llmDispatcher).send(testRequest, strategy);

        // 验证发布了 TokenUsedEvent
        verify(eventPublisher).publishEvent(tokenUsedEventCaptor.capture());
        TokenUsedEvent capturedEvent = tokenUsedEventCaptor.getValue();
        assertThat(capturedEvent.model()).isEqualTo("gpt-4");
        assertThat(capturedEvent.promptTokens()).isEqualTo(10);
        assertThat(capturedEvent.completionTokens()).isEqualTo(20);
    }

    @Test
    @DisplayName("send() 响应为 null 时不发布事件")
    void send_responseIsNull_noEventPublished() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.COST_OPTIMIZED;
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy))).thenReturn(null);

        // When
        LLMResponse result = llmChatUseCase.send(testRequest, strategy);

        // Then
        assertThat(result).isNull();

        // 验证调用了 llmDispatcher
        verify(llmDispatcher).send(testRequest, strategy);

        // 验证没有发布事件
        verify(eventPublisher, never()).publishEvent(any(TokenUsedEvent.class));
    }

    @Test
    @DisplayName("send() 响应 usage 为 null 时不发布事件")
    void send_responseUsageIsNull_noEventPublished() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.COST_OPTIMIZED;
        LLMResponse responseWithNullUsage = LLMResponse.builder()
                .model("gpt-4")
                .content(LLMResponse.Content.builder()
                        .text("Hello")
                        .build())
                .usage(null)
                .build();
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy))).thenReturn(responseWithNullUsage);

        // When
        LLMResponse result = llmChatUseCase.send(testRequest, strategy);

        // Then
        assertThat(result).isNotNull();

        // 验证没有发布事件
        verify(eventPublisher, never()).publishEvent(any(TokenUsedEvent.class));
    }

    @Test
    @DisplayName("sendStream() 成功场景 - 验证调用 llmDispatcher")
    void sendStream_success() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.COST_OPTIMIZED;
        Consumer<String> mockCallback = mock(Consumer.class);
        doNothing().when(llmDispatcher).sendStream(any(LLMRequest.class), eq(strategy), any());

        // When
        llmChatUseCase.sendStream(testRequest, strategy, mockCallback);

        // Then
        verify(llmDispatcher).sendStream(eq(testRequest), eq(strategy), eq(mockCallback));
    }

    @Test
    @DisplayName("send() 使用不同路由策略")
    void send_withDifferentStrategy() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.LATENCY_OPTIMIZED;
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy))).thenReturn(testResponse);

        // When
        LLMResponse result = llmChatUseCase.send(testRequest, strategy);

        // Then
        assertThat(result).isNotNull();
        verify(llmDispatcher).send(testRequest, strategy);
        verify(eventPublisher).publishEvent(any(TokenUsedEvent.class));
    }
}
