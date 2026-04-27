package com.codingas.gateway.infrastructure.adapter.anthropic;

import com.codingas.gateway.common.ProviderCapabilities;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AnthropicAdapter 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AnthropicAdapterTest {

    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String API_KEY = "test-api-key";
    private static final String VERSION = "2023-06-01";
    private static final int TIMEOUT_SECONDS = 30;

    @Mock
    private StreamCallback streamCallback;

    private AnthropicAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AnthropicAdapter(BASE_URL, API_KEY, VERSION, TIMEOUT_SECONDS);
    }

    @Nested
    @DisplayName("getProviderCode")
    class GetProviderCode {

        @Test
        @DisplayName("返回 anthropic")
        void returnsAnthropic() {
            assertThat(adapter.getProviderCode()).isEqualTo("anthropic");
        }
    }

    @Nested
    @DisplayName("getProviderType")
    class GetProviderType {

        @Test
        @DisplayName("返回 ProviderType.ANTHROPIC")
        void returnsAnthropicType() {
            assertThat(adapter.getProviderType()).isEqualTo(ProviderType.ANTHROPIC);
        }
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailable {

        @Test
        @DisplayName("apiKey 不为空时返回 true")
        void apiKeyNotEmpty_returnsTrue() {
            assertThat(adapter.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("apiKey 为空字符串时返回 false")
        void apiKeyEmpty_returnsFalse() {
            var adapterWithEmptyKey = new AnthropicAdapter(BASE_URL, "", VERSION, TIMEOUT_SECONDS);
            assertThat(adapterWithEmptyKey.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("apiKey 为 null 时返回 false")
        void apiKeyNull_returnsFalse() {
            var adapterWithNullKey = new AnthropicAdapter(BASE_URL, null, VERSION, TIMEOUT_SECONDS);
            assertThat(adapterWithNullKey.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isHealthy")
    class IsHealthy {

        @Test
        @DisplayName("与 isAvailable 行为一致")
        void consistentWithIsAvailable() {
            assertThat(adapter.isHealthy()).isEqualTo(adapter.isAvailable());

            var adapterWithEmptyKey = new AnthropicAdapter(BASE_URL, "", VERSION, TIMEOUT_SECONDS);
            assertThat(adapterWithEmptyKey.isHealthy()).isEqualTo(adapterWithEmptyKey.isAvailable());

            var adapterWithNullKey = new AnthropicAdapter(BASE_URL, null, VERSION, TIMEOUT_SECONDS);
            assertThat(adapterWithNullKey.isHealthy()).isEqualTo(adapterWithNullKey.isAvailable());
        }
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @Test
        @DisplayName("抛出 UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            var request = LLMRequest.builder().model("claude-3-5-sonnet-20241022").build();

            assertThatThrownBy(() -> adapter.chat(request))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("OpenAI chat format");
        }
    }

    @Nested
    @DisplayName("chatStream")
    class ChatStream {

        @Test
        @DisplayName("抛出 UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            var request = LLMRequest.builder().model("claude-3-5-sonnet-20241022").build();

            assertThatThrownBy(() -> adapter.chatStream(request, streamCallback))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("OpenAI chat format");
        }
    }

    @Nested
    @DisplayName("getCapabilities")
    class GetCapabilities {

        @Test
        @DisplayName("返回正确的 ProviderCapabilities")
        void returnsCorrectCapabilities() {
            ProviderCapabilities capabilities = adapter.getCapabilities();

            assertThat(capabilities.providerType()).isEqualTo(ProviderType.ANTHROPIC);
            assertThat(capabilities.supportsChatCompletion()).isFalse();
            assertThat(capabilities.supportsMessages()).isTrue();
            assertThat(capabilities.supportsEmbeddings()).isFalse();
            assertThat(capabilities.supportsStreaming()).isTrue();
            assertThat(capabilities.supportsFunctionCalling()).isTrue();
            assertThat(capabilities.supportedModels()).contains("claude-opus-4-5", "claude-sonnet-4-6", "claude-haiku-4-5");
        }
    }

    @Nested
    @DisplayName("buildMessagesRequestBody")
    class BuildMessagesRequestBody {

        @Test
        @DisplayName("正确构建请求体，包含必填字段")
        void buildsRequestBodyWithRequiredFields() {
            var request = LLMRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .messages(List.of(
                            LLMRequest.Message.builder().role("user").content("Hello").build()
                    ))
                    .build();

            // 通过反射调用私有方法
            Map<String, Object> body = invokeBuildMessagesRequestBody(request);

            assertThat(body).containsEntry("model", "claude-3-5-sonnet-20241022");
            assertThat(body).containsEntry("messages", request.getMessages());
            assertThat(body).containsEntry("max_tokens", 1024); // 默认值
        }

        @Test
        @DisplayName("使用指定的 maxTokens")
        void usesSpecifiedMaxTokens() {
            var request = LLMRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .maxTokens(2048)
                    .messages(List.of())
                    .build();

            Map<String, Object> body = invokeBuildMessagesRequestBody(request);

            assertThat(body).containsEntry("max_tokens", 2048);
        }

        @Test
        @DisplayName("包含 temperature")
        void includesTemperature() {
            var request = LLMRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .temperature(0.7)
                    .messages(List.of())
                    .build();

            Map<String, Object> body = invokeBuildMessagesRequestBody(request);

            assertThat(body).containsEntry("temperature", 0.7);
        }

        @Test
        @DisplayName("包含 systemPrompt")
        void includesSystemPrompt() {
            var request = LLMRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .systemPrompt("You are a helpful assistant.")
                    .messages(List.of())
                    .build();

            Map<String, Object> body = invokeBuildMessagesRequestBody(request);

            assertThat(body).containsEntry("system", "You are a helpful assistant.");
        }
    }

    @Nested
    @DisplayName("messages")
    class Messages {

        @Test
        @DisplayName("解析响应并返回 LLMResponse")
        void parsesResponseCorrectly() {
            // 使用反射调用 parseResponse 来验证解析逻辑
            Map<String, Object> responseMap = Map.of(
                    "id", "msg_123",
                    "model", "claude-3-5-sonnet-20241022",
                    "content", List.of(Map.of("type", "text", "text", "Hello! How can I help you?")),
                    "stop_reason", "end_turn",
                    "usage", Map.of("input_tokens", 10, "output_tokens", 20)
            );

            LLMResponse response = invokeParseResponse(responseMap);

            assertThat(response.getProviderCode()).isEqualTo("anthropic");
            assertThat(response.getId()).isEqualTo("msg_123");
            assertThat(response.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
            assertThat(response.getContent().getText()).isEqualTo("Hello! How can I help you?");
            assertThat(response.getFinishReason()).isEqualTo("end_turn");
            assertThat(response.getUsage().getPromptTokens()).isEqualTo(10);
            assertThat(response.getUsage().getCompletionTokens()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("getDefaultTimeout")
    class GetDefaultTimeout {

        @Test
        @DisplayName("返回配置的超时时间")
        void returnsConfiguredTimeout() {
            assertThat(adapter.getDefaultTimeout()).isEqualTo(TIMEOUT_SECONDS);
        }
    }

    // 反射辅助方法
    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeBuildMessagesRequestBody(LLMRequest request) {
        try {
            var method = AnthropicAdapter.class.getDeclaredMethod("buildMessagesRequestBody", LLMRequest.class);
            method.setAccessible(true);
            return (Map<String, Object>) method.invoke(adapter, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private LLMResponse invokeParseResponse(Map<String, Object> response) {
        try {
            var method = AnthropicAdapter.class.getDeclaredMethod("parseResponse", Map.class);
            method.setAccessible(true);
            return (LLMResponse) method.invoke(adapter, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
