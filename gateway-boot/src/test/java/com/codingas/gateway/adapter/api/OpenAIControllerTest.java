package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.proxy.ProxyService;
import com.codingas.gateway.application.proxy.dto.*;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OpenAIController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAIController 测试")
class OpenAIControllerTest {

    @Mock
    private ProxyService proxyService;

    private OpenAIController controller;

    private OpenAIChatRequest createRequest(String model, boolean stream) {
        OpenAIChatRequest request = new OpenAIChatRequest();
        request.setModel(model);
        request.setStream(stream);
        request.setMessages(List.of(
            OpenAIChatRequest.Message.builder().role("user").content("Hello").build()
        ));
        return request;
    }

    @BeforeEach
    void setUp() {
        controller = new OpenAIController(proxyService);
    }

    @Nested
    @DisplayName("chatCompletions 方法测试")
    class ChatCompletionsTests {

        @Test
        @DisplayName("非流式请求 — 有 authResult 时走新架构代理")
        void chatCompletions_nonStream_withAuthResult() throws IOException {
            // Arrange
            OpenAIChatRequest request = createRequest("gpt-4o", false);
            UserAuthResult authResult = UserAuthResult.legacy(1L, "USER", 10L);

            LLMResponse response = LLMResponse.builder()
                .id("chatcmpl-123")
                .model("gpt-4o")
                .content(LLMResponse.Content.builder().text("Hello!").build())
                .usage(LLMResponse.Usage.builder()
                    .promptTokens(10)
                    .completionTokens(5)
                    .build())
                .build();

            when(proxyService.proxy(any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class)))
                .thenReturn(response);

            // Act
            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 10L, authResult, null);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(proxyService).proxy(any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class));
        }

        @Test
        @DisplayName("非流式请求 — 无 authResult 时走旧架构代理")
        void chatCompletions_nonStream_withoutAuthResult() throws IOException {
            // Arrange
            OpenAIChatRequest request = createRequest("gpt-4o", false);

            LLMResponse response = LLMResponse.builder()
                .id("chatcmpl-123")
                .model("gpt-4o")
                .content(LLMResponse.Content.builder().text("Hello!").build())
                .build();

            when(proxyService.proxy(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class)))
                .thenReturn(response);

            // Act
            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 10L, null, null);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(proxyService).proxy(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class));
        }

        @Test
        @DisplayName("非流式请求 — 代理返回错误时返回 400")
        void chatCompletions_nonStream_errorResponse() throws IOException {
            // Arrange
            OpenAIChatRequest request = createRequest("gpt-4o", false);
            UserAuthResult authResult = UserAuthResult.legacy(1L, "USER", 10L);

            LLMResponse response = LLMResponse.builder()
                .error(LLMResponse.Error.builder()
                    .message("Model not found")
                    .type("invalid_request_error")
                    .code("model_not_found")
                    .build())
                .build();

            when(proxyService.proxy(any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class)))
                .thenReturn(response);

            // Act
            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 10L, authResult, null);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("流式请求 — 有 authResult 时走新架构流式代理")
        void chatCompletions_stream_withAuthResult() throws IOException {
            // Arrange
            OpenAIChatRequest request = createRequest("gpt-4o", true);
            UserAuthResult authResult = UserAuthResult.legacy(1L, "USER", 10L);

            // Mock stream - just verify the method is called
            doNothing().when(proxyService).proxyStream(
                any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class),
                any(), any(), any());

            // Act
            controller.chatCompletions(request, 1L, 10L, authResult, mock(jakarta.servlet.http.HttpServletResponse.class));

            // Assert
            verify(proxyService).proxyStream(
                any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class),
                any(), any(), any());
        }

        @Test
        @DisplayName("流式请求 — 无 authResult 时走旧架构流式代理")
        void chatCompletions_stream_withoutAuthResult() throws IOException {
            // Arrange
            OpenAIChatRequest request = createRequest("gpt-4o", true);

            doNothing().when(proxyService).proxyStream(
                any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class),
                any(), any(), any());

            // Act
            controller.chatCompletions(request, 1L, 10L, null, mock(jakarta.servlet.http.HttpServletResponse.class));

            // Assert
            verify(proxyService).proxyStream(
                any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class),
                any(), any(), any());
        }
    }

    @Nested
    @DisplayName("请求验证测试")
    class ValidationTests {

        @Test
        @DisplayName("model 为 null 时抛出 IllegalArgumentException")
        void chatCompletions_nullModel_throwsException() {
            OpenAIChatRequest request = createRequest(null, false);

            assertThat(org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> controller.chatCompletions(request, 1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class));
        }

        @Test
        @DisplayName("model 为空字符串时抛出 IllegalArgumentException")
        void chatCompletions_blankModel_throwsException() {
            OpenAIChatRequest request = createRequest("  ", false);

            assertThat(org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> controller.chatCompletions(request, 1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class));
        }
    }
}