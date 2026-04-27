package com.codingas.gateway.application.chat;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.service.ModelRouterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatApplication 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChatApplicationTest {

    @Mock
    private ModelRouterService modelRouterService;

    @Mock
    private LLMChatUseCase llmChatUseCase;

    @InjectMocks
    private ChatApplication chatApplication;

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

    @Test
    @DisplayName("chat() 成功场景 - 验证正确调用 modelRouterService 和 llmChatUseCase")
    void chat_success() {
        // Given
        String modelCode = "gpt-4";
        List<LLMRequest.Message> messages = List.of(
                LLMRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
        );
        ChatApplication.ChatRequest request = new ChatApplication.ChatRequest(modelCode, messages);

        when(modelRouterService.selectModel(modelCode)).thenReturn(testModel);
        when(llmChatUseCase.send(any(LLMRequest.class), eq(RouteGroup.RoutingStrategy.COST_OPTIMIZED)))
                .thenReturn(Mono.just(testLLMResponse));

        // When
        ChatApplication.ChatResponse response = chatApplication.chat(request).block();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.model()).isEqualTo("gpt-4");
        assertThat(response.content()).isEqualTo("Hello, how can I help you?");

        // 验证调用了 modelRouterService
        verify(modelRouterService).selectModel(modelCode);

        // 验证调用了 llmChatUseCase
        verify(llmChatUseCase).send(any(LLMRequest.class), eq(RouteGroup.RoutingStrategy.COST_OPTIMIZED));
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
        ChatApplication.ChatRequest request = new ChatApplication.ChatRequest(modelCode, messages, strategy);

        when(modelRouterService.selectModel(modelCode)).thenReturn(testModel);
        when(llmChatUseCase.send(any(LLMRequest.class), eq(strategy)))
                .thenReturn(Mono.just(testLLMResponse));

        // When
        ChatApplication.ChatResponse response = chatApplication.chat(request).block();

        // Then
        assertThat(response).isNotNull();
        verify(llmChatUseCase).send(any(LLMRequest.class), eq(strategy));
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
        ChatApplication.ChatRequest request = new ChatApplication.ChatRequest(modelCode, messages);

        LLMResponse responseWithNullContent = LLMResponse.builder()
                .model("gpt-4")
                .content(null)
                .build();

        when(modelRouterService.selectModel(modelCode)).thenReturn(testModel);
        when(llmChatUseCase.send(any(LLMRequest.class), any()))
                .thenReturn(Mono.just(responseWithNullContent));

        // When
        ChatApplication.ChatResponse response = chatApplication.chat(request).block();

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
        ChatApplication.ChatRequest request = new ChatApplication.ChatRequest(modelCode, messages);

        when(modelRouterService.selectModel(modelCode)).thenReturn(testModel);
        when(llmChatUseCase.send(any(LLMRequest.class), any())).thenReturn(Mono.empty());

        // When
        ChatApplication.ChatResponse response = chatApplication.chat(request).block();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNull();
    }
}
