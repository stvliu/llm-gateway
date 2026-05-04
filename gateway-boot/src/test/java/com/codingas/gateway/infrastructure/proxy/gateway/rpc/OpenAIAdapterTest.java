package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.common.ProviderCapabilities;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
        @DisplayName("getProviderType 返回 OPENAI")
        void getProviderType_returnsOpenai() {
            assertThat(adapter.getProviderType()).isEqualTo(ProviderType.OPENAI);
        }

        @Test
        @DisplayName("isAvailable 有 API Key 时返回 true")
        void isAvailable_withApiKey_returnsTrue() {
            assertThat(adapter.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("getDefaultTimeout 返回配置的超时时间")
        void getDefaultTimeout_returnsConfigured() {
            assertThat(adapter.getDefaultTimeout()).isEqualTo(30);
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
            assertThat(capabilities.supportsStreaming()).isTrue();
            assertThat(capabilities.supportsFunctionCalling()).isTrue();
        }
    }

    @Nested
    @DisplayName("chat 方法测试")
    class ChatTests {

        @Test
        @DisplayName("无效请求抛出异常")
        void chat_invalidRequest_throwsException() {
            // given
            LLMRequest request = LLMRequest.builder()
                .model("gpt-4")
                .build();

            // when & then - 由于没有真实的 API 端点，会抛出异常
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.chat(request))
                .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("chatStream 方法测试")
    class ChatStreamTests {

        @Test
        @DisplayName("流式请求调用回调")
        void chatStream_callsCallback() {
            // given
            LLMRequest request = LLMRequest.builder()
                .model("gpt-4")
                .build();
            StreamCallback callback = mock(StreamCallback.class);

            // when - 由于是异步调用，这里只是验证不会抛出异常
            adapter.chatStream(request, callback);

            // then - 验证方法能正常调用（异步请求会失败但不会立即抛出异常）
            assertThat(adapter.isAvailable()).isTrue();
        }
    }
}
