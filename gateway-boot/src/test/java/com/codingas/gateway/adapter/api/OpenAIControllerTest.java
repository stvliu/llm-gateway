package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.proxy.ProxyService;
import com.codingas.gateway.application.proxy.dto.OpenAIChatRequest;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OpenAIController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAIController 测试")
class OpenAIControllerTest {

    @Mock
    private ProxyService proxyService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private OpenAIController controller;

    @Nested
    @DisplayName("validateRequest 测试")
    class ValidateRequestTests {

        @Test
        @DisplayName("model 为空抛出异常")
        void validateRequest_blankModel_throwsException() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model(null)
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .build();

            // when & then
            assertThatThrownBy(() -> controller.chatCompletions(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model is required");
        }

        @Test
        @DisplayName("messages 为空抛出异常")
        void validateRequest_emptyMessages_throwsException() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(null)
                .build();

            // when & then
            assertThatThrownBy(() -> controller.chatCompletions(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages is required");
        }

        @Test
        @DisplayName("messages 为空列表抛出异常")
        void validateRequest_emptyMessagesList_throwsException() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of())
                .build();

            // when & then
            assertThatThrownBy(() -> controller.chatCompletions(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages is required");
        }
    }

    @Nested
    @DisplayName("非流式请求测试")
    class NonStreamTests {

        @Test
        @DisplayName("成功处理非流式请求")
        void chatCompletions_nonStream_returnsResponse() throws Exception {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            // when
            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
            verify(proxyService).proxy(any(LLMRequest.class), any());
        }

        @Test
        @DisplayName("请求包含错误响应")
        void chatCompletions_errorResponse_returnsError() throws Exception {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = LLMResponse.builder()
                .error(LLMResponse.Error.builder()
                    .message("Model not found")
                    .type("invalid_request_error")
                    .code("model_not_found")
                    .build())
                .build();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            // when
            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            // then
            assertThat(result).isNotNull();
            // 错误响应被包装在 ResponseEntity.badRequest() 中返回
            assertThat(result.getStatusCodeValue()).isEqualTo(400);
        }
    }

    // Helper methods
    private LLMResponse createTestResponse() {
        return LLMResponse.builder()
            .id("chatcmpl-123")
            .model("gpt-4")
            .content(LLMResponse.Content.builder()
                .text("Hello, how can I help you?")
                .role("assistant")
                .build())
            .usage(LLMResponse.Usage.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .build())
            .build();
    }
}
