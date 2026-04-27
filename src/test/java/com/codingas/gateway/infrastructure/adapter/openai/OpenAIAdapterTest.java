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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * OpenAIAdapter 单元测试 (WebFlux 版本)
 */
@ExtendWith(MockitoExtension.class)
class OpenAIAdapterTest {

    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String API_KEY = "test-api-key";
    private static final int TIMEOUT_SECONDS = 30;

    @Mock
    private WebClient webClient;

    private OpenAIAdapter adapter;

    @Nested
    @DisplayName("getProviderCode")
    class GetProviderCode {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(webClient, BASE_URL, API_KEY, TIMEOUT_SECONDS);
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
            adapter = new OpenAIAdapter(webClient, BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("返回 ProviderType.OPENAI")
        void returnsOpenAI() {
            assertThat(adapter.getProviderType()).isEqualTo(ProviderType.OPENAI);
        }
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailable {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(webClient, BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("当 API Key 非空时返回 true")
        void returnsTrueWhenApiKeyNotEmpty() {
            assertThat(adapter.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("当 API Key 为空时返回 false")
        void returnsFalseWhenApiKeyEmpty() {
            adapter = new OpenAIAdapter(webClient, BASE_URL, "", TIMEOUT_SECONDS);
            assertThat(adapter.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCapabilities")
    class GetCapabilities {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(webClient, BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("返回正确的提供商能力")
        void returnsCorrectCapabilities() {
            ProviderCapabilities caps = adapter.getCapabilities();

            assertThat(caps.providerType()).isEqualTo(ProviderType.OPENAI);
            assertThat(caps.supportsChatCompletion()).isTrue();
            assertThat(caps.supportsMessages()).isFalse();
            assertThat(caps.supportsStreaming()).isTrue();
            assertThat(caps.supportsFunctionCalling()).isTrue();
            assertThat(caps.supportedModels()).contains("gpt-4o", "gpt-4o-mini");
        }
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @BeforeEach
        void setUp() {
            adapter = new OpenAIAdapter(webClient, BASE_URL, API_KEY, TIMEOUT_SECONDS);
        }

        @Test
        @DisplayName("messages() 方法抛出 UnsupportedOperationException")
        void messagesThrowsUnsupportedOperationException() {
            LLMRequest request = LLMRequest.builder().model("gpt-4").build();

            assertThatThrownBy(() -> adapter.messages(request).block())
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support Anthropic messages format");
        }
    }
}
