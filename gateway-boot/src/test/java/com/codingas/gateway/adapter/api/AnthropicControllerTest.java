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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AnthropicController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnthropicController 测试")
class AnthropicControllerTest {

    @Mock
    private ProxyService proxyService;

    private AnthropicController controller;

    @BeforeEach
    void setUp() {
        controller = new AnthropicController(proxyService);
    }

    private AnthropicMessagesRequest createRequest(String model, Integer maxTokens, boolean stream) {
        return AnthropicMessagesRequest.builder()
            .model(model)
            .maxTokens(maxTokens)
            .stream(stream)
            .messages(List.of(
                AnthropicMessagesRequest.Message.builder().role("user").content("Hello").build()
            ))
            .build();
    }

    @Nested
    @DisplayName("messages 方法测试")
    class MessagesTests {

        @Test
        @DisplayName("非流式请求 — 有 authResult 时走新架构代理")
        void messages_nonStream_withAuthResult() throws IOException {
            // Arrange
            AnthropicMessagesRequest request = createRequest("claude-3-5-sonnet-20241022", 1024, false);
            UserAuthResult authResult = UserAuthResult.legacy(1L, "USER", 10L);

            LLMResponse response = LLMResponse.builder()
                .id("msg-123")
                .model("claude-3-5-sonnet-20241022")
                .content(LLMResponse.Content.builder().text("Hello!").build())
                .usage(LLMResponse.Usage.builder()
                    .promptTokens(10)
                    .completionTokens(5)
                    .build())
                .build();

            when(proxyService.proxy(any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class)))
                .thenReturn(response);

            // Act
            ResponseEntity<?> result = controller.messages(request, 1L, 10L, authResult, null);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(proxyService).proxy(any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class));
        }

        @Test
        @DisplayName("非流式请求 — 无 authResult 时走旧架构代理")
        void messages_nonStream_withoutAuthResult() throws IOException {
            // Arrange
            AnthropicMessagesRequest request = createRequest("claude-3-5-sonnet-20241022", 1024, false);

            LLMResponse response = LLMResponse.builder()
                .id("msg-123")
                .model("claude-3-5-sonnet-20241022")
                .content(LLMResponse.Content.builder().text("Hello!").build())
                .build();

            when(proxyService.proxy(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class)))
                .thenReturn(response);

            // Act
            ResponseEntity<?> result = controller.messages(request, 1L, 10L, null, null);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(proxyService).proxy(any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class));
        }

        @Test
        @DisplayName("非流式请求 — 代理返回错误时返回 400")
        void messages_nonStream_errorResponse() throws IOException {
            // Arrange
            AnthropicMessagesRequest request = createRequest("claude-3-5-sonnet-20241022", 1024, false);
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
            ResponseEntity<?> result = controller.messages(request, 1L, 10L, authResult, null);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("流式请求 — 有 authResult 时走新架构流式代理")
        void messages_stream_withAuthResult() throws IOException {
            // Arrange
            AnthropicMessagesRequest request = createRequest("claude-3-5-sonnet-20241022", 1024, true);
            UserAuthResult authResult = UserAuthResult.legacy(1L, "USER", 10L);

            doNothing().when(proxyService).proxyStream(
                any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class),
                any(), any(), any());

            // Act
            controller.messages(request, 1L, 10L, authResult, mock(jakarta.servlet.http.HttpServletResponse.class));

            // Assert
            verify(proxyService).proxyStream(
                any(LLMRequest.class), eq(authResult), any(RouteGroup.RoutingStrategy.class),
                any(), any(), any());
        }

        @Test
        @DisplayName("流式请求 — 无 authResult 时走旧架构流式代理")
        void messages_stream_withoutAuthResult() throws IOException {
            // Arrange
            AnthropicMessagesRequest request = createRequest("claude-3-5-sonnet-20241022", 1024, true);

            doNothing().when(proxyService).proxyStream(
                any(LLMRequest.class), any(RouteGroup.RoutingStrategy.class),
                any(), any(), any());

            // Act
            controller.messages(request, 1L, 10L, null, mock(jakarta.servlet.http.HttpServletResponse.class));

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
        void messages_nullModel_throwsException() {
            AnthropicMessagesRequest request = createRequest(null, 1024, false);

            assertThatThrownBy(() -> controller.messages(request, 1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model is required");
        }

        @Test
        @DisplayName("model 为空字符串时抛出 IllegalArgumentException")
        void messages_blankModel_throwsException() {
            AnthropicMessagesRequest request = createRequest("  ", 1024, false);

            assertThatThrownBy(() -> controller.messages(request, 1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model is required");
        }

        @Test
        @DisplayName("maxTokens 为 null 时抛出 IllegalArgumentException")
        void messages_nullMaxTokens_throwsException() {
            AnthropicMessagesRequest request = createRequest("claude-3-5-sonnet-20241022", null, false);

            assertThatThrownBy(() -> controller.messages(request, 1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_tokens is required");
        }

        @Test
        @DisplayName("maxTokens 为 0 时抛出 IllegalArgumentException")
        void messages_zeroMaxTokens_throwsException() {
            AnthropicMessagesRequest request = createRequest("claude-3-5-sonnet-20241022", 0, false);

            assertThatThrownBy(() -> controller.messages(request, 1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_tokens is required");
        }

        @Test
        @DisplayName("messages 为 null 时抛出 IllegalArgumentException")
        void messages_nullMessages_throwsException() {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .maxTokens(1024)
                .messages(null)
                .build();

            assertThatThrownBy(() -> controller.messages(request, 1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages is required");
        }

        @Test
        @DisplayName("messages 为空列表时抛出 IllegalArgumentException")
        void messages_emptyMessages_throwsException() {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .maxTokens(1024)
                .messages(List.of())
                .build();

            assertThatThrownBy(() -> controller.messages(request, 1L, 10L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages is required");
        }
    }
}