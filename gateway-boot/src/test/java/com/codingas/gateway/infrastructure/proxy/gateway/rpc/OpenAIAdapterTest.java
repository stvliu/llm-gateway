package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.model.entity.ProviderCapabilities;
import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * OpenAIAdapter 单元测试
 */
@DisplayName("OpenAIAdapter 测试")
class OpenAIAdapterTest {

    private OpenAIAdapter adapter;
    private OkHttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = new OkHttpClient.Builder().build();
        adapter = new OpenAIAdapter(
            httpClient,
            "https://api.openai.com",
            "test-api-key",
            30
        );
    }

    @Nested
    @DisplayName("基础属性测试")
    class BasicPropertiesTests {

        @Test
        @DisplayName("getProviderCode 返回 openai")
        void getProviderCode_returnsOpenai() {
            assertThat(adapter.getProviderCode()).isEqualTo("openai");
        }

        @Test
        @DisplayName("getProviderName 返回 openai")
        void getProviderName_returnsOpenai() {
            assertThat(adapter.getProviderName()).isEqualTo("openai");
        }

        @Test
        @DisplayName("isAvailable 有 API Key 时返回 true")
        void isAvailable_withApiKey_returnsTrue() {
            assertThat(adapter.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("isAvailable 无 API Key 时返回 false")
        void isAvailable_withoutApiKey_returnsFalse() {
            OpenAIAdapter noKeyAdapter = new OpenAIAdapter(httpClient, "https://api.openai.com", null, 30);
            assertThat(noKeyAdapter.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("isAvailable 空字符串 API Key 时返回 false")
        void isAvailable_emptyApiKey_returnsFalse() {
            OpenAIAdapter emptyKeyAdapter = new OpenAIAdapter(httpClient, "https://api.openai.com", "", 30);
            assertThat(emptyKeyAdapter.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("getDefaultTimeout 返回配置的超时时间")
        void getDefaultTimeout_returnsConfigured() {
            assertThat(adapter.getDefaultTimeout()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("isHealthy 测试")
    class IsHealthyTests {

        @Test
        @DisplayName("isAvailable 时 isHealthy 返回 true")
        void isHealthy_whenAvailable_returnsTrue() {
            assertThat(adapter.isHealthy()).isTrue();
        }

        @Test
        @DisplayName("无 API Key 时 isHealthy 返回 false")
        void isHealthy_whenNotAvailable_returnsFalse() {
            OpenAIAdapter noKeyAdapter = new OpenAIAdapter(httpClient, "https://api.openai.com", null, 30);
            assertThat(noKeyAdapter.isHealthy()).isFalse();
        }
    }

    @Nested
    @DisplayName("checkConnection 测试")
    class CheckConnectionTests {

        @Test
        @DisplayName("连接检查失败返回 false")
        void checkConnection_connectionFailed_returnsFalse() {
            OpenAIAdapter invalidAdapter = new OpenAIAdapter(
                httpClient, "https://invalid-url-that-does-not-exist.com", "test-key", 5
            );
            assertThat(invalidAdapter.checkConnection()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCapabilities 测试")
    class GetCapabilitiesTests {

        @Test
        @DisplayName("返回 OpenAI 能力描述")
        void getCapabilities_returnsCapabilities() {
            ProviderCapabilities capabilities = adapter.getCapabilities();

            assertThat(capabilities).isNotNull();
            assertThat(capabilities.isSupportsStreaming()).isTrue();
            assertThat(capabilities.isSupportsFunctionCalling()).isTrue();
        }

        @Test
        @DisplayName("支持特定模型列表")
        void getCapabilities_supportsModels() {
            ProviderCapabilities capabilities = adapter.getCapabilities();

            assertThat(capabilities.getSupportedModels()).contains("gpt-4o", "gpt-4o-mini", "gpt-4-turbo");
        }
    }

    @Nested
    @DisplayName("chat 方法测试")
    class ChatTests {

        @Test
        @DisplayName("无效请求抛出异常")
        void chat_invalidRequest_throwsException() {
            LLMRequest request = LLMRequest.builder()
                .model("gpt-4")
                .build();

            assertThatThrownBy(() -> adapter.chat(request))
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("带完整参数的请求")
        void chat_withFullParams_throwsException() {
            LLMRequest request = LLMRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    LLMRequest.Message.builder().role("user").content("Hello").build()
                ))
                .temperature(0.7)
                .maxTokens(100)
                .build();

            assertThatThrownBy(() -> adapter.chat(request))
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("带 tools 参数的请求")
        void chat_withTools_throwsException() {
            LLMRequest request = LLMRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    LLMRequest.Message.builder().role("user").content("Hello").build()
                ))
                .tools(List.of(
                    LLMRequest.ToolDefinition.builder()
                        .type("function")
                        .function(LLMRequest.Function.builder()
                            .name("get_weather")
                            .description("Get weather")
                            .build())
                        .build()
                ))
                .toolChoice("auto")
                .build();

            assertThatThrownBy(() -> adapter.chat(request))
                .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("chatStream 方法测试")
    class ChatStreamTests {

        @Test
        @DisplayName("流式请求调用回调")
        void chatStream_callsCallback() {
            LLMRequest request = LLMRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    LLMRequest.Message.builder().role("user").content("Hello").build()
                ))
                .build();
            StreamCallback callback = mock(StreamCallback.class);

            adapter.chatStream(request, callback);

            assertThat(adapter.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("流式请求带温度参数")
        void chatStream_withTemperature_callsCallback() {
            LLMRequest request = LLMRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                    LLMRequest.Message.builder().role("user").content("Hello").build()
                ))
                .temperature(0.5)
                .build();
            StreamCallback callback = mock(StreamCallback.class);

            adapter.chatStream(request, callback);

            assertThat(adapter.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("messages 方法测试")
    class MessagesTests {

        @Test
        @DisplayName("messages 抛出 UnsupportedOperationException")
        void messages_throwsUnsupportedOperationException() {
            LLMRequest request = LLMRequest.builder()
                .model("claude-3")
                .build();

            assertThatThrownBy(() -> adapter.messages(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support Anthropic messages format");
        }
    }

    @Nested
    @DisplayName("messagesStream 方法测试")
    class MessagesStreamTests {

        @Test
        @DisplayName("messagesStream 抛出 UnsupportedOperationException")
        void messagesStream_throwsUnsupportedOperationException() {
            LLMRequest request = LLMRequest.builder()
                .model("claude-3")
                .build();
            StreamCallback callback = mock(StreamCallback.class);

            assertThatThrownBy(() -> adapter.messagesStream(request, callback))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support Anthropic messages format");
        }
    }

    @Nested
    @DisplayName("getChatCompletionsUrl 测试")
    class GetChatCompletionsUrlTests {

        @Test
        @DisplayName("返回正确的 API URL")
        void getChatCompletionsUrl_returnsCorrectUrl() {
            TestableOpenAIAdapter testableAdapter = new TestableOpenAIAdapter(
                httpClient, "https://api.openai.com", "test-key", 30
            );

            assertThat(testableAdapter.exposedGetChatCompletionsUrl())
                .isEqualTo("https://api.openai.com/v1/chat/completions");
        }
    }

    /**
     * 可测试的 OpenAIAdapter 子类，暴露受保护的方法
     */
    static class TestableOpenAIAdapter extends OpenAIAdapter {
        public TestableOpenAIAdapter(OkHttpClient httpClient, String baseUrl, String apiKey, int timeoutSeconds) {
            super(httpClient, baseUrl, apiKey, timeoutSeconds);
        }

        public String exposedGetChatCompletionsUrl() {
            return getChatCompletionsUrl();
        }
    }
}
