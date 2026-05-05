package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.proxy.ProxyService;
import com.codingas.gateway.application.proxy.dto.AnthropicMessagesRequest;
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
 * AnthropicController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnthropicController 测试")
class AnthropicControllerTest {

    @Mock
    private ProxyService proxyService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AnthropicController controller;

    @Nested
    @DisplayName("validateRequest 测试")
    class ValidateRequestTests {

        @Test
        @DisplayName("model 为空抛出异常")
        void validateRequest_blankModel_throwsException() {
            // given
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model(null)
                .maxTokens(1024)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .build();

            // when & then
            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model is required");
        }

        @Test
        @DisplayName("messages 为空抛出异常")
        void validateRequest_emptyMessages_throwsException() {
            // given
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(null)
                .build();

            // when & then
            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages is required");
        }

        @Test
        @DisplayName("maxTokens 为空抛出异常")
        void validateRequest_nullMaxTokens_throwsException() {
            // given
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(null)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .build();

            // when & then
            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_tokens is required");
        }

        @Test
        @DisplayName("maxTokens 为 0 抛出异常")
        void validateRequest_zeroMaxTokens_throwsException() {
            // given
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(0)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .build();

            // when & then
            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_tokens is required");
        }
    }

    @Nested
    @DisplayName("非流式请求测试")
    class NonStreamTests {

        @Test
        @DisplayName("成功处理非流式请求")
        void messages_nonStream_returnsResponse() throws Exception {
            // given
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            // when
            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
            verify(proxyService).proxy(any(LLMRequest.class), any());
        }
    }

    // Helper methods
    private LLMResponse createTestResponse() {
        return LLMResponse.builder()
            .id("msg-123")
            .model("claude-3-opus")
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
