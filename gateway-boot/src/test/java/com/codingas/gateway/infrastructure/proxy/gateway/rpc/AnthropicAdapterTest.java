package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.model.entity.ProviderCapabilities;
import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AnthropicAdapter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnthropicAdapter 测试")
class AnthropicAdapterTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call call;

    @Mock
    private StreamCallback streamCallback;

    private AnthropicAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new AnthropicAdapter(
                httpClient,
                "https://api.anthropic.com",
                "test-api-key",
                "2023-06-01",
                60
        );
    }

    @Nested
    @DisplayName("基础属性测试")
    class BasicPropertiesTests {

        @Test
        @DisplayName("getProviderCode 返回正确值")
        void getProviderCode_returnsAnthropic() {
            assertThat(adapter.getProviderCode()).isEqualTo("anthropic");
        }

        @Test
        @DisplayName("getProviderType 返回正确值")
        void getProviderType_returnsAnthropic() {
            assertThat(adapter.getProviderType()).isEqualTo(ProviderType.ANTHROPIC);
        }

        @Test
        @DisplayName("getDefaultTimeout 返回正确值")
        void getDefaultTimeout_returnsConfigured() {
            assertThat(adapter.getDefaultTimeout()).isEqualTo(60);
        }

        @Test
        @DisplayName("getCapabilities 返回正确的配置")
        void getCapabilities_returnsCorrectConfig() {
            ProviderCapabilities capabilities = adapter.getCapabilities();

            assertThat(capabilities.providerType()).isEqualTo(ProviderType.ANTHROPIC);
            assertThat(capabilities.supportsChatCompletion()).isFalse();
            assertThat(capabilities.supportsMessages()).isTrue();
            assertThat(capabilities.supportsEmbeddings()).isFalse();
            assertThat(capabilities.supportsStreaming()).isTrue();
            assertThat(capabilities.supportsFunctionCalling()).isTrue();
            assertThat(capabilities.supportedModels()).contains("claude-sonnet-4-6");
        }
    }

    @Nested
    @DisplayName("可用性测试")
    class AvailabilityTests {

        @Test
        @DisplayName("isAvailable 有 API Key 时返回 true")
        void isAvailable_withApiKey_returnsTrue() {
            assertThat(adapter.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("isAvailable 无 API Key 时返回 false")
        void isAvailable_withoutApiKey_returnsFalse() {
            AnthropicAdapter noKeyAdapter = new AnthropicAdapter(
                    httpClient, "https://api.anthropic.com", null, "2023-06-01", 60);
            assertThat(noKeyAdapter.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("isAvailable 空字符串 API Key 时返回 false")
        void isAvailable_emptyApiKey_returnsFalse() {
            AnthropicAdapter emptyKeyAdapter = new AnthropicAdapter(
                    httpClient, "https://api.anthropic.com", "", "2023-06-01", 60);
            assertThat(emptyKeyAdapter.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("isHealthy 返回与 isAvailable 相同结果")
        void isHealthy_returnsSameAsAvailable() {
            assertThat(adapter.isHealthy()).isTrue();
        }
    }

    @Nested
    @DisplayName("chat 方法测试")
    class ChatMethodTests {

        @Test
        @DisplayName("chat 抛出 UnsupportedOperationException")
        void chat_throwsUnsupportedOperationException() {
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-6")
                    .messages(List.of(LLMRequest.Message.builder()
                            .role("user")
                            .content("Hello")
                            .build()))
                    .build();

            assertThatThrownBy(() -> adapter.chat(request))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support OpenAI chat format");
        }

        @Test
        @DisplayName("chatStream 抛出 UnsupportedOperationException")
        void chatStream_throwsUnsupportedOperationException() {
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-6")
                    .messages(List.of(LLMRequest.Message.builder()
                            .role("user")
                            .content("Hello")
                            .build()))
                    .build();

            assertThatThrownBy(() -> adapter.chatStream(request, streamCallback))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support OpenAI chat format");
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用默认版本号")
        void constructor_withNullVersion_usesDefault() {
            AnthropicAdapter defaultVersionAdapter = new AnthropicAdapter(
                    httpClient, "https://api.anthropic.com", "test-key", null, 60);

            assertThat(defaultVersionAdapter.getProviderCode()).isEqualTo("anthropic");
        }
    }

    @Nested
    @DisplayName("请求构建测试")
    class RequestBuildingTests {

        @Test
        @DisplayName("messages 方法构建正确请求")
        void messages_buildsCorrectRequest() throws Exception {
            // Given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-6")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello")
                                    .build()
                    ))
                    .maxTokens(100)
                    .temperature(0.7)
                    .systemPrompt("You are a helpful assistant")
                    .build();

            // Mock HTTP 响应
            String mockResponse = """
                {
                    "id": "msg-123",
                    "model": "claude-sonnet-4-6",
                    "content": [{"type": "text", "text": "Hello!"}],
                    "usage": {"input_tokens": 10, "output_tokens": 5},
                    "stop_reason": "end_turn"
                }
                """;

            mockSuccessfulResponse(mockResponse);

            // When
            LLMResponse response = adapter.messages(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo("msg-123");
            assertThat(response.getModel()).isEqualTo("claude-sonnet-4-6");
            assertThat(response.getContent()).isNotNull();
            assertThat(response.getContent().getText()).isEqualTo("Hello!");
        }

        @Test
        @DisplayName("messages 使用默认 maxTokens")
        void messages_withNullMaxTokens_usesDefault() throws Exception {
            // Given
            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-6")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello")
                                    .build()
                    ))
                    .build();

            String mockResponse = """
                {
                    "id": "msg-123",
                    "model": "claude-sonnet-4-6",
                    "content": [{"type": "text", "text": "Hi!"}],
                    "usage": {}
                }
                """;

            mockSuccessfulResponse(mockResponse);

            // When
            LLMResponse response = adapter.messages(request);

            // Then
            assertThat(response).isNotNull();
        }

        private void mockSuccessfulResponse(String responseBody) throws IOException {
            Response mockResponse = mock(Response.class);
            ResponseBody mockResponseBody = mock(ResponseBody.class);

            when(httpClient.newCall(any(Request.class))).thenReturn(call);
            when(call.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);
            when(mockResponse.body()).thenReturn(mockResponseBody);
            when(mockResponseBody.string()).thenReturn(responseBody);
        }
    }

    @Nested
    @DisplayName("响应解析测试")
    class ResponseParsingTests {

        @Test
        @DisplayName("解析包含工具调用的响应")
        void parseResponse_withToolCalls() throws Exception {
            // Given
            String mockResponse = """
                {
                    "id": "msg-123",
                    "model": "claude-sonnet-4-6",
                    "content": [
                        {"type": "text", "text": "Let me check that."},
                        {"type": "tool_use", "id": "tool-1", "name": "get_weather", "input": {"city": "Beijing"}}
                    ],
                    "usage": {"input_tokens": 20, "output_tokens": 30},
                    "stop_reason": "tool_use"
                }
                """;

            mockSuccessfulResponse(mockResponse);
            LLMRequest request = createBasicRequest();

            // When
            LLMResponse response = adapter.messages(request);

            // Then
            assertThat(response.getContent()).isNotNull();
            assertThat(response.getContent().getText()).isEqualTo("Let me check that.");
            assertThat(response.getContent().getToolCalls()).isNotNull();
            assertThat(response.getContent().getToolCalls()).hasSize(1);
            assertThat(response.getContent().getToolCalls().get(0).getId()).isEqualTo("tool-1");
            assertThat(response.getContent().getToolCalls().get(0).getFunction().getName()).isEqualTo("get_weather");
        }

        @Test
        @DisplayName("解析包含 usage 的响应")
        void parseResponse_withUsage() throws Exception {
            // Given
            String mockResponse = """
                {
                    "id": "msg-123",
                    "model": "claude-sonnet-4-6",
                    "content": [{"type": "text", "text": "Response"}],
                    "usage": {"input_tokens": 100, "output_tokens": 200}
                }
                """;

            mockSuccessfulResponse(mockResponse);
            LLMRequest request = createBasicRequest();

            // When
            LLMResponse response = adapter.messages(request);

            // Then
            assertThat(response.getUsage()).isNotNull();
            assertThat(response.getUsage().getPromptTokens()).isEqualTo(100);
            assertThat(response.getUsage().getCompletionTokens()).isEqualTo(200);
        }

        @Test
        @DisplayName("解析空 content 响应")
        void parseResponse_emptyContent() throws Exception {
            // Given
            String mockResponse = """
                {
                    "id": "msg-123",
                    "model": "claude-sonnet-4-6",
                    "content": [],
                    "usage": {}
                }
                """;

            mockSuccessfulResponse(mockResponse);
            LLMRequest request = createBasicRequest();

            // When
            LLMResponse response = adapter.messages(request);

            // Then
            assertThat(response.getContent()).isNull();
        }

        private void mockSuccessfulResponse(String responseBody) throws IOException {
            Response mockResponse = mock(Response.class);
            ResponseBody mockResponseBody = mock(ResponseBody.class);

            when(httpClient.newCall(any(Request.class))).thenReturn(call);
            when(call.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);
            when(mockResponse.body()).thenReturn(mockResponseBody);
            when(mockResponseBody.string()).thenReturn(responseBody);
        }

        private LLMRequest createBasicRequest() {
            return LLMRequest.builder()
                    .model("claude-sonnet-4-6")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello")
                                    .build()
                    ))
                    .maxTokens(100)
                    .build();
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("HTTP 错误响应抛出异常")
        void messages_httpError_throwsException() throws Exception {
            // Given
            Response mockResponse = mock(Response.class);
            when(httpClient.newCall(any(Request.class))).thenReturn(call);
            when(call.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(false);
            when(mockResponse.toString()).thenReturn("404 Not Found");

            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-6")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello")
                                    .build()
                    ))
                    .maxTokens(100)
                    .build();

            // When & Then
            assertThatThrownBy(() -> adapter.messages(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Anthropic messages request failed");
        }

        @Test
        @DisplayName("IO 异常抛出 RuntimeException")
        void messages_ioException_throwsRuntimeException() throws Exception {
            // Given
            when(httpClient.newCall(any(Request.class))).thenReturn(call);
            when(call.execute()).thenThrow(new IOException("Connection refused"));

            LLMRequest request = LLMRequest.builder()
                    .model("claude-sonnet-4-6")
                    .messages(List.of(
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content("Hello")
                                    .build()
                    ))
                    .maxTokens(100)
                    .build();

            // When & Then
            assertThatThrownBy(() -> adapter.messages(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Anthropic messages request failed");
        }
    }
}
