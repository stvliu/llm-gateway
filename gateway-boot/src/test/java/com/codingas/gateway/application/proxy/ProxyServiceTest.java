package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ProxyServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProxyServiceImpl 测试")
class ProxyServiceTest {

    @Mock
    private ChannelRoutingService channelRoutingService;

    @Mock
    private ProtocolGatewayRegistry protocolGatewayRegistry;

    @Mock
    private ProtocolGateway protocolGateway;

    private ProxyServiceImpl proxyService;

    private LLMRequest testRequest;
    private LLMResponse testResponse;
    private UserAuthResult testAuthResult;
    private RoutingContext testContext;

    @BeforeEach
    void setUp() {
        proxyService = new ProxyServiceImpl(channelRoutingService, protocolGatewayRegistry);

        testAuthResult = mock(UserAuthResult.class);

        testContext = RoutingContext.builder()
                .providerId(1L)
                .providerName("openai")
                .productId(10L)
                .model("gpt-4")
                .protocol("openai")
                .providerApiKey("sk-test-key")
                .endpoint("https://api.openai.com")
                .build();

        testRequest = LLMRequest.builder()
                .model("gpt-4")
                .messages(java.util.List.of())
                .build();

        testResponse = LLMResponse.builder()
                .id("chatcmpl-123")
                .model("gpt-4")
                .content(LLMResponse.Content.builder()
                        .text("Hello, world!")
                        .build())
                .usage(LLMResponse.Usage.builder()
                        .promptTokens(10)
                        .completionTokens(20)
                        .totalTokens(30)
                        .build())
                .build();
    }

    @Nested
    @DisplayName("chat 方法测试")
    class ChatTests {

        @Test
        @DisplayName("成功代理非流式请求")
        void chat_success() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayRegistry.getGateway("openai")).thenReturn(Optional.of(protocolGateway));
            when(protocolGateway.chat(any(), anyString(), anyString(), anyInt())).thenReturn(testResponse);

            LLMResponse response = proxyService.chat(testAuthResult, testRequest);

            assertThat(response).isNotNull();
            assertThat(response.getModel()).isEqualTo("gpt-4");
            verify(protocolGateway).chat(testRequest, "https://api.openai.com", "sk-test-key", 60);
        }

        @Test
        @DisplayName("协议网关不存在时抛出异常")
        void chat_protocolGatewayNotFound_throwsException() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayRegistry.getGateway("openai")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> proxyService.chat(testAuthResult, testRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No protocol gateway found");
        }
    }

    @Nested
    @DisplayName("chatStream 方法测试")
    class ChatStreamTests {

        @Test
        @DisplayName("成功代理流式请求")
        void chatStream_success() {
            StreamCallback callback = mock(StreamCallback.class);

            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayRegistry.getGateway("openai")).thenReturn(Optional.of(protocolGateway));

            proxyService.chatStream(testAuthResult, testRequest, callback);

            verify(protocolGateway).chatStream(testRequest, "https://api.openai.com", "sk-test-key", 60, callback);
        }

        @Test
        @DisplayName("协议网关不存在时抛出异常")
        void chatStream_protocolGatewayNotFound_throwsException() {
            StreamCallback callback = mock(StreamCallback.class);

            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayRegistry.getGateway("openai")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> proxyService.chatStream(testAuthResult, testRequest, callback))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No protocol gateway found");
        }
    }
}
