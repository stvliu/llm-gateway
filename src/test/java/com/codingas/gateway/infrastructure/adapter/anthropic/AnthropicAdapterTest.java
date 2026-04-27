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
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * AnthropicAdapter 单元测试 (WebFlux 版本)
 */
@ExtendWith(MockitoExtension.class)
class AnthropicAdapterTest {

    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String API_KEY = "test-api-key";
    private static final String VERSION = "2023-06-01";
    private static final int TIMEOUT_SECONDS = 30;

    @Mock
    private WebClient webClient;

    @Mock
    private StreamCallback streamCallback;

    private AnthropicAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AnthropicAdapter(webClient, BASE_URL, API_KEY, VERSION, TIMEOUT_SECONDS);
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
            var adapterWithEmptyKey = new AnthropicAdapter(webClient, BASE_URL, "", VERSION, TIMEOUT_SECONDS);
            assertThat(adapterWithEmptyKey.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("apiKey 为 null 时返回 false")
        void apiKeyNull_returnsFalse() {
            var adapterWithNullKey = new AnthropicAdapter(webClient, BASE_URL, null, VERSION, TIMEOUT_SECONDS);
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

            var adapterWithEmptyKey = new AnthropicAdapter(webClient, BASE_URL, "", VERSION, TIMEOUT_SECONDS);
            assertThat(adapterWithEmptyKey.isHealthy()).isEqualTo(adapterWithEmptyKey.isAvailable());

            var adapterWithNullKey = new AnthropicAdapter(webClient, BASE_URL, null, VERSION, TIMEOUT_SECONDS);
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

            assertThatThrownBy(() -> adapter.chat(request).block())
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support OpenAI chat format");
        }
    }

    @Nested
    @DisplayName("chatStream")
    class ChatStream {

        @Test
        @DisplayName("抛出 UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            var request = LLMRequest.builder().model("claude-3-5-sonnet-20241022").build();

            assertThatThrownBy(() -> adapter.chatStream(request, streamCallback).block())
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support OpenAI chat format");
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
    @DisplayName("getDefaultTimeout")
    class GetDefaultTimeout {

        @Test
        @DisplayName("返回配置的超时时间")
        void returnsConfiguredTimeout() {
            assertThat(adapter.getDefaultTimeout()).isEqualTo(TIMEOUT_SECONDS);
        }
    }
}
