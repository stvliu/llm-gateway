package com.codingas.gateway.application.chat;

import com.codingas.gateway.application.proxy.ChatService;
import com.codingas.gateway.application.proxy.ChatServiceImpl;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.common.event.TokenUsedEvent;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.service.LLMDispatcher;
import com.codingas.gateway.domain.proxy.service.ModelRouterDomainService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ModelRouterDomainService modelRouterService;

    @Mock
    private LLMDispatcher llmDispatcher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Captor
    private ArgumentCaptor<TokenUsedEvent> tokenUsedEventCaptor;

    private Model testModel;
    private LLMRequest testLLMRequest;
    private LLMResponse testLLMResponse;

    @BeforeEach
    void setUp() {
        // 准备测试模型
        testModel = new Model();
        testModel.setModelCode("gpt-4");
        testModel.setDisplayName("GPT-4");
        testModel.setStatus(Model.ModelStatus.ACTIVE);

        // 准备测试请求消息
        List<LLMRequest.Message> messages = List.of(
                LLMRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
        );

        // 准备 LLM 请求
        testLLMRequest = LLMRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .build();

        // 准备 LLM 响应
        testLLMResponse = LLMResponse.builder()
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

    // ==================== send() 方法测试 ====================

    @Test
    @DisplayName("send() 成功场景 - 验证调用 llmDispatcher 并发布事件")
    void send_success() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.WEIGHTED;
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy))).thenReturn(testLLMResponse);

        // When
        LLMResponse result = chatService.send(testLLMRequest, strategy);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getModel()).isEqualTo("gpt-4");
        assertThat(result.getContent().getText()).isEqualTo("Hello, how can I help you?");

        // 验证调用了 llmDispatcher
        verify(llmDispatcher).send(testLLMRequest, strategy);

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
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.WEIGHTED;
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy))).thenReturn(null);

        // When
        LLMResponse result = chatService.send(testLLMRequest, strategy);

        // Then
        assertThat(result).isNull();

        // 验证调用了 llmDispatcher
        verify(llmDispatcher).send(testLLMRequest, strategy);

        // 验证没有发布事件
        verify(eventPublisher, never()).publishEvent(any(TokenUsedEvent.class));
    }

    @Test
    @DisplayName("send() 响应 usage 为 null 时不发布事件")
    void send_responseUsageIsNull_noEventPublished() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.WEIGHTED;
        LLMResponse responseWithNullUsage = LLMResponse.builder()
                .model("gpt-4")
                .content(LLMResponse.Content.builder()
                        .text("Hello")
                        .build())
                .usage(null)
                .build();
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy))).thenReturn(responseWithNullUsage);

        // When
        LLMResponse result = chatService.send(testLLMRequest, strategy);

        // Then
        assertThat(result).isNotNull();

        // 验证没有发布事件
        verify(eventPublisher, never()).publishEvent(any(TokenUsedEvent.class));
    }

    @Test
    @DisplayName("send() 使用不同路由策略")
    void send_withDifferentStrategy() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.LATENCY_OPTIMIZED;
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy))).thenReturn(testLLMResponse);

        // When
        LLMResponse result = chatService.send(testLLMRequest, strategy);

        // Then
        assertThat(result).isNotNull();
        verify(llmDispatcher).send(testLLMRequest, strategy);
        verify(eventPublisher).publishEvent(any(TokenUsedEvent.class));
    }

    // ==================== chat() 方法测试（简化版接口）====================

    @Test
    @DisplayName("chat() 成功场景 - 验证正确调用 modelRouterService 和 llmDispatcher")
    void chat_success() {
        // Given
        String modelCode = "gpt-4";
        List<LLMRequest.Message> messages = List.of(
                LLMRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
        );
        ChatService.ChatRequest request = new ChatService.ChatRequest(modelCode, messages);

        when(modelRouterService.selectModel(modelCode)).thenReturn(testModel);
        when(llmDispatcher.send(any(LLMRequest.class), eq(RouteGroup.RoutingStrategy.WEIGHTED)))
                .thenReturn(testLLMResponse);

        // When
        ChatService.ChatResponse response = chatService.chat(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.model()).isEqualTo("gpt-4");
        assertThat(response.content()).isEqualTo("Hello, how can I help you?");

        // 验证调用了 modelRouterService
        verify(modelRouterService).selectModel(modelCode);

        // 验证调用了 llmDispatcher
        verify(llmDispatcher).send(any(LLMRequest.class), eq(RouteGroup.RoutingStrategy.WEIGHTED));
    }

    @Test
    @DisplayName("chat() 使用指定路由策略")
    void chat_withSpecifiedStrategy() {
        // Given
        String modelCode = "gpt-4";
        List<LLMRequest.Message> messages = List.of(
                LLMRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
        );
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.LATENCY_OPTIMIZED;
        ChatService.ChatRequest request = new ChatService.ChatRequest(modelCode, messages, strategy);

        when(modelRouterService.selectModel(modelCode)).thenReturn(testModel);
        when(llmDispatcher.send(any(LLMRequest.class), eq(strategy)))
                .thenReturn(testLLMResponse);

        // When
        ChatService.ChatResponse response = chatService.chat(request);

        // Then
        assertThat(response).isNotNull();
        verify(llmDispatcher).send(any(LLMRequest.class), eq(strategy));
    }

    @Test
    @DisplayName("chat() 响应 content 为 null 时返回 null")
    void chat_responseContentIsNull() {
        // Given
        String modelCode = "gpt-4";
        List<LLMRequest.Message> messages = List.of(
                LLMRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
        );
        ChatService.ChatRequest request = new ChatService.ChatRequest(modelCode, messages);

        LLMResponse responseWithNullContent = LLMResponse.builder()
                .model("gpt-4")
                .content(null)
                .build();

        when(modelRouterService.selectModel(modelCode)).thenReturn(testModel);
        when(llmDispatcher.send(any(LLMRequest.class), any()))
                .thenReturn(responseWithNullContent);

        // When
        ChatService.ChatResponse response = chatService.chat(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNull();
    }

    @Test
    @DisplayName("chat() 响应为 null 时返回 null")
    void chat_responseIsNull() {
        // Given
        String modelCode = "gpt-4";
        List<LLMRequest.Message> messages = List.of(
                LLMRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
        );
        ChatService.ChatRequest request = new ChatService.ChatRequest(modelCode, messages);

        when(modelRouterService.selectModel(modelCode)).thenReturn(testModel);
        when(llmDispatcher.send(any(LLMRequest.class), any())).thenReturn(null);

        // When
        ChatService.ChatResponse response = chatService.chat(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNull();
    }

    // ==================== sendStream() 方法测试 ====================

    @Test
    @DisplayName("sendStream() 成功场景 - 验证调用 llmDispatcher")
    void sendStream_success() {
        // Given
        RouteGroup.RoutingStrategy strategy = RouteGroup.RoutingStrategy.WEIGHTED;
        java.util.function.Consumer<String> mockCallback = mock(java.util.function.Consumer.class);
        doNothing().when(llmDispatcher).sendStream(any(LLMRequest.class), eq(strategy), any(), any(), any());

        // When
        chatService.sendStream(testLLMRequest, strategy, mockCallback);

        // Then
        verify(llmDispatcher).sendStream(eq(testLLMRequest), eq(strategy), any(), any(), any());
    }
}
