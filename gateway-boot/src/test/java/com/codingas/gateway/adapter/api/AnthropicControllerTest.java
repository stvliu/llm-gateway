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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

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

            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model is required");
        }

        @Test
        @DisplayName("model 为空白字符串抛出异常")
        void validateRequest_blankModelString_throwsException() {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("   ")
                .maxTokens(1024)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .build();

            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model is required");
        }

        @Test
        @DisplayName("messages 为空抛出异常")
        void validateRequest_emptyMessages_throwsException() {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(null)
                .build();

            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages is required");
        }

        @Test
        @DisplayName("messages 为空列表抛出异常")
        void validateRequest_emptyMessagesList_throwsException() {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(List.of())
                .build();

            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages is required");
        }

        @Test
        @DisplayName("maxTokens 为空抛出异常")
        void validateRequest_nullMaxTokens_throwsException() {
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

            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_tokens is required");
        }

        @Test
        @DisplayName("maxTokens 为 0 抛出异常")
        void validateRequest_zeroMaxTokens_throwsException() {
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

            assertThatThrownBy(() -> controller.messages(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_tokens is required");
        }

        @Test
        @DisplayName("maxTokens 为负数抛出异常")
        void validateRequest_negativeMaxTokens_throwsException() {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(-1)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .build();

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

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
            verify(proxyService).proxy(any(LLMRequest.class), any());
        }

        @Test
        @DisplayName("带系统提示的请求")
        void messages_withSystemPrompt_returnsResponse() throws Exception {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .system("You are a helpful assistant.")
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

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("带温度参数的请求")
        void messages_withTemperature_returnsResponse() throws Exception {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .temperature(0.7)
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

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("带 stop sequences 的请求")
        void messages_withStopSequences_returnsResponse() throws Exception {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .stopSequences(List.of("END", "STOP"))
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

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("响应包含错误")
        void messages_errorResponse_returnsError() throws Exception {
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

            LLMResponse llmResponse = LLMResponse.builder()
                .error(LLMResponse.Error.builder()
                    .message("Model not found")
                    .type("invalid_request_error")
                    .code("model_not_found")
                    .build())
                .build();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(400);
        }

        @Test
        @DisplayName("响应包含工具调用")
        void messages_responseWithToolCalls_returnsResponse() throws Exception {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("What's the weather?")
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = LLMResponse.builder()
                .id("msg-123")
                .model("claude-3-opus")
                .content(LLMResponse.Content.builder()
                    .text("")
                    .role("assistant")
                    .toolCalls(List.of(
                        LLMResponse.ToolCall.builder()
                            .id("toolu-123")
                            .type("function")
                            .function(LLMResponse.FunctionCall.builder()
                                .name("get_weather")
                                .arguments("{\"location\": \"Beijing\"}")
                                .build())
                            .build()
                    ))
                    .build())
                .build();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("消息内容为列表格式")
        void messages_withListContent_returnsResponse() throws Exception {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content(List.of(
                            Map.of("type", "text", "text", "Hello")
                        ))
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("消息包含 tool_use 内容块")
        void messages_withToolUseContent_returnsResponse() throws Exception {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("assistant")
                        .content(List.of(
                            Map.of("type", "text", "text", "Let me check the weather."),
                            Map.of(
                                "type", "tool_use",
                                "id", "toolu-123",
                                "name", "get_weather",
                                "input", Map.of("location", "Beijing")
                            )
                        ))
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("带 toolChoice 的请求")
        void messages_withToolChoice_returnsResponse() throws Exception {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .toolChoice(Map.of("type", "auto"))
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.messages(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("流式请求测试")
    class StreamTests {

        @Test
        @DisplayName("流式请求设置响应头")
        void messages_stream_setsResponseHeaders() throws Exception {
            AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-opus")
                .maxTokens(1024)
                .messages(List.of(
                    AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .stream(true)
                .build();

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(printWriter);

            doAnswer(invocation -> {
                Runnable onComplete = invocation.getArgument(3);
                onComplete.run();
                return null;
            }).when(proxyService).proxyStream(any(), any(), any(), any(), any());

            controller.messages(request, 1L, 1L, response);

            verify(response).setContentType("text/event-stream");
            verify(response).setCharacterEncoding("UTF-8");
            verify(response).setStatus(200);
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
