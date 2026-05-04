package com.codingas.gateway.application.proxy;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.common.event.TokenUsedEvent;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.service.LLMDispatcher;
import com.codingas.gateway.domain.proxy.service.ModelRouterDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChatServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatServiceImpl 测试")
class ChatServiceImplTest {

    @Mock
    private ModelRouterDomainService modelRouterService;

    @Mock
    private LLMDispatcher llmDispatcher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatServiceImpl service;

    @Nested
    @DisplayName("send 方法测试")
    class SendTests {

        @Test
        @DisplayName("发送非流式请求成功")
        void send_validRequest_returnsResponse() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .messages(List.of())
                    .build();
            LLMResponse response = createTestResponse();

            when(llmDispatcher.send(any(), any())).thenReturn(response);

            // when
            service.send(request, RouteGroup.RoutingStrategy.WEIGHTED);

            // then
            verify(llmDispatcher).send(any(), any());
            verify(eventPublisher).publishEvent(any(TokenUsedEvent.class));
        }

        @Test
        @DisplayName("响应为 null 不发布事件")
        void send_nullResponse_noEvent() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .messages(List.of())
                    .build();

            when(llmDispatcher.send(any(), any())).thenReturn(null);

            // when
            service.send(request, RouteGroup.RoutingStrategy.WEIGHTED);

            // then
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("sendStream 方法测试")
    class SendStreamTests {

        @Test
        @DisplayName("发送流式请求成功")
        void sendStream_validRequest_callsDispatcher() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("gpt-4")
                    .messages(List.of())
                    .build();

            doNothing().when(llmDispatcher).sendStream(any(), any(), any(), any(), any());

            // when
            service.sendStream(request, RouteGroup.RoutingStrategy.WEIGHTED, s -> {});

            // then
            verify(llmDispatcher).sendStream(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("chat 方法测试")
    class ChatTests {

        @Test
        @DisplayName("处理聊天请求成功")
        void chat_validRequest_returnsResponse() {
            // given
            Model model = createTestModel();
            when(modelRouterService.selectModel("gpt-4")).thenReturn(model);

            LLMResponse response = createTestResponse();
            when(llmDispatcher.send(any(), any())).thenReturn(response);

            ChatService.ChatRequest request = new ChatService.ChatRequest("gpt-4", List.of(), null);

            // when
            service.chat(request);

            // then
            verify(modelRouterService).selectModel("gpt-4");
            verify(llmDispatcher).send(any(), any());
        }
    }

    @Nested
    @DisplayName("chatStream 方法测试")
    class ChatStreamTests {

        @Test
        @DisplayName("处理流式聊天请求成功")
        void chatStream_validRequest_callsCorrectly() {
            // given
            Model model = createTestModel();
            when(modelRouterService.selectModel("gpt-4")).thenReturn(model);

            doNothing().when(llmDispatcher).sendStream(any(), any(), any(), any(), any());

            ChatService.ChatRequest request = new ChatService.ChatRequest("gpt-4", List.of(), null);

            // when
            service.chatStream(request, s -> {});

            // then
            verify(modelRouterService).selectModel("gpt-4");
            verify(llmDispatcher).sendStream(any(), any(), any(), any(), any());
        }
    }

    // Helper methods
    private Model createTestModel() {
        Model model = new Model();
        model.setId(1L);
        model.setModelCode("gpt-4");
        model.setDisplayName("GPT-4");
        model.setStatus(Model.ModelStatus.ACTIVE);
        return model;
    }

    private LLMResponse createTestResponse() {
        return LLMResponse.builder()
                .model("gpt-4")
                .content(LLMResponse.Content.builder()
                        .text("Hello, world!")
                        .build())
                .usage(LLMResponse.Usage.builder()
                        .promptTokens(100)
                        .completionTokens(50)
                        .totalTokens(150)
                        .build())
                .build();
    }
}
