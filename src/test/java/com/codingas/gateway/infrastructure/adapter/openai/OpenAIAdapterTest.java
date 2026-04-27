package com.codingas.gateway.infrastructure.adapter.openai;

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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenAIAdapter 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OpenAIAdapterTest {

    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String API_KEY = "test-api-key";
    private static final int TIMEOUT_SECONDS = 30;

    private OpenAIAdapter adapter;

    @Nested
    @DisplayName("getProviderCode")
    class GetProviderCode {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("返回 openai")
        void returnsOpenai() {
            assertThat(adapter.getProviderCode()).isEqualTo("openai");
        }
    }

    @Nested
    @DisplayName("getProviderType")
    class GetProviderType {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("返回 ProviderType.OPENAI")
        void returnsOpenAiProviderType() {
            assertThat(adapter.getProviderType()).isEqualTo(ProviderType.OPENAI);
        }
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailable {

        @Test
        @DisplayName("apiKey 为空时返回 false")
        void emptyApiKey_returnsFalse() {
            adapter = new OpenAIAdapter(BASE_URL, "", TIMEOUT_SECONDS);
            assertThat(adapter.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("apiKey 为 null 时返回 false")
        void nullApiKey_returnsFalse() {
            adapter = new OpenAIAdapter(BASE_URL, null, TIMEOUT_SECONDS);
            assertThat(adapter.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("apiKey 有效时返回 true")
        void validApiKey_returnsTrue() {
            adapter = new OpenAIAdapter(BASE_URL, API_KEY, TIMEOUT_SECONDS);
            assertThat(adapter.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("isHealthy")
    class IsHealthy {

        @Test
        @DisplayName("与 isAvailable 行为一致")
        void consistentWithIsAvailable() {
            adapter = new OpenAIAdapter(BASE_URL, "", TIMEOUT_SECONDS);
            assertThat(adapter.isHealthy()).isEqualTo(adapter.isAvailable());

            adapter = new OpenAIAdapter(BASE_URL, API_KEY, TIMEOUT_SECONDS);
            assertThat(adapter.isHealthy()).isEqualTo(adapter.isAvailable());
        }
    }

    @Nested
    @DisplayName("messages")
    class Messages {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("抛出 UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            LLMRequest request = LLMRequest.builder().model("gpt-4").build();

            assertThatThrownBy(() -> adapter.messages(request))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Anthropic messages format");
        }
    }

    @Nested
    @DisplayName("messagesStream")
    class MessagesStream {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("抛出 UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            LLMRequest request = LLMRequest.builder().model("gpt-4").build();
            StreamCallback callback = new TestStreamCallback();

            assertThatThrownBy(() -> adapter.messagesStream(request, callback))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Anthropic messages format");
        }
    }

    @Nested
    @DisplayName("getCapabilities")
    class GetCapabilities {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("返回正确的 ProviderCapabilities")
        void returnsCorrectCapabilities() {
            ProviderCapabilities capabilities = adapter.getCapabilities();

            assertThat(capabilities.providerType()).isEqualTo(ProviderType.OPENAI);
            assertThat(capabilities.supportsChatCompletion()).isTrue();
            assertThat(capabilities.supportsMessages()).isFalse();
            assertThat(capabilities.supportsEmbeddings()).isTrue();
            assertThat(capabilities.supportsStreaming()).isTrue();
            assertThat(capabilities.supportsFunctionCalling()).isTrue();
            assertThat(capabilities.supportedModels())
                    .contains("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo");
        }
    }

    @Nested
    @DisplayName("getDefaultTimeout")
    class GetDefaultTimeout {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("返回配置的 timeout")
        void returnsConfiguredTimeout() {
            assertThat(adapter.getDefaultTimeout()).isEqualTo(TIMEOUT_SECONDS);
        }
    }

    
    /**
     * 测试用 StreamCallback 实现
     */
    private static class TestStreamCallback implements StreamCallback {
        @Override
        public void onChunk(String data) {}

        @Override
        public void onComplete() {}

        @Override
        public void onError(Throwable t) {}
    }
}