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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

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
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model(null)
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .build();

            assertThatThrownBy(() -> controller.chatCompletions(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model is required");
        }

        @Test
        @DisplayName("model 为空白字符串抛出异常")
        void validateRequest_blankModelString_throwsException() {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("   ")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .build();

            assertThatThrownBy(() -> controller.chatCompletions(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model is required");
        }

        @Test
        @DisplayName("messages 为空抛出异常")
        void validateRequest_emptyMessages_throwsException() {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(null)
                .build();

            assertThatThrownBy(() -> controller.chatCompletions(request, 1L, 1L, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages is required");
        }

        @Test
        @DisplayName("messages 为空列表抛出异常")
        void validateRequest_emptyMessagesList_throwsException() {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of())
                .build();

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

            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
            verify(proxyService).proxy(any(LLMRequest.class), any());
        }

        @Test
        @DisplayName("请求包含错误响应")
        void chatCompletions_errorResponse_returnsError() throws Exception {
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

            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(400);
        }

        @Test
        @DisplayName("带完整参数的请求")
        void chatCompletions_withFullParams_returnsResponse() throws Exception {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .temperature(0.7)
                .maxTokens(100)
                .frequencyPenalty(0.5)
                .presencePenalty(0.3)
                .seed(42)
                .toolChoice("auto")
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("带工具调用消息的请求")
        void chatCompletions_withToolCalls_returnsResponse() throws Exception {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build(),
                    OpenAIChatRequest.Message.builder()
                        .role("assistant")
                        .toolCalls(List.of(
                            OpenAIChatRequest.ToolCall.builder()
                                .id("call-123")
                                .type("function")
                                .function(OpenAIChatRequest.FunctionCall.builder()
                                    .name("get_weather")
                                    .arguments("{\"location\": \"Beijing\"}")
                                    .build())
                                .build()
                        ))
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("带工具响应消息的请求")
        void chatCompletions_withToolResponse_returnsResponse() throws Exception {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("What's the weather?")
                        .build(),
                    OpenAIChatRequest.Message.builder()
                        .role("tool")
                        .toolCallId("call-123")
                        .content("{\"temperature\": 25}")
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }

        @Test
        @DisplayName("响应包含工具调用")
        void chatCompletions_responseWithToolCalls_returnsResponse() throws Exception {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("What's the weather?")
                        .build()
                ))
                .stream(false)
                .build();

            LLMResponse llmResponse = LLMResponse.builder()
                .id("chatcmpl-123")
                .model("gpt-4")
                .content(LLMResponse.Content.builder()
                    .text("")
                    .role("assistant")
                    .toolCalls(List.of(
                        LLMResponse.ToolCall.builder()
                            .id("call-123")
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

            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("流式请求测试")
    class StreamTests {

        @Test
        @DisplayName("流式请求设置响应头")
        void chatCompletions_stream_setsResponseHeaders() throws Exception {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .stream(true)
                .build();

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(printWriter);

            // 模拟 proxyStream 立即完成
            doAnswer(invocation -> {
                Runnable onComplete = invocation.getArgument(3);
                onComplete.run();
                return null;
            }).when(proxyService).proxyStream(any(), any(), any(), any(), any());

            controller.chatCompletions(request, 1L, 1L, response);

            verify(response).setContentType("text/event-stream");
            verify(response).setCharacterEncoding("UTF-8");
            verify(response).setStatus(200);
            verify(response).setHeader("Cache-Control", "no-cache");
            verify(response).setHeader("X-Accel-Buffering", "no");
        }
    }

    @Nested
    @DisplayName("stop 参数测试")
    class StopParamTests {

        @Test
        @DisplayName("带 stop 参数的请求")
        void chatCompletions_withStop_returnsResponse() throws Exception {
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    OpenAIChatRequest.Message.builder()
                        .role("user")
                        .content("Hello")
                        .build()
                ))
                .stop(List.of("END"))
                .stream(false)
                .build();

            LLMResponse llmResponse = createTestResponse();
            when(proxyService.proxy(any(LLMRequest.class), any())).thenReturn(llmResponse);

            ResponseEntity<?> result = controller.chatCompletions(request, 1L, 1L, response);

            assertThat(result).isNotNull();
            assertThat(result.getStatusCodeValue()).isEqualTo(200);
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
