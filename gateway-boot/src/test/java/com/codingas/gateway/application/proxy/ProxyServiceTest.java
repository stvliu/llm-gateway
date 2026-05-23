package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.entity.RoutingStrategy;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("proxy 方法测试")
    class ProxyTests {

        @Test
        @DisplayName("成功代理非流式请求")
        void proxy_success() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayRegistry.getGateway("openai")).thenReturn(Optional.of(protocolGateway));
            when(protocolGateway.chat(any(), anyString(), anyString(), anyInt())).thenReturn(testResponse);

            LLMResponse response = proxyService.proxy(testRequest, testAuthResult, RoutingStrategy.WEIGHTED);

            assertThat(response).isNotNull();
            assertThat(response.getModel()).isEqualTo("gpt-4");
            verify(protocolGateway).chat(testRequest, "https://api.openai.com", "sk-test-key", 60);
        }

        @Test
        @DisplayName("协议网关不存在时抛出异常")
        void proxy_protocolGatewayNotFound_throwsException() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayRegistry.getGateway("openai")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> proxyService.proxy(testRequest, testAuthResult, RoutingStrategy.WEIGHTED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No protocol gateway found");
        }
    }

    @Nested
    @DisplayName("proxyStream 方法测试")
    class ProxyStreamTests {

        @Test
        @DisplayName("成功代理流式请求（带回调）")
        void proxyStream_success() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayRegistry.getGateway("openai")).thenReturn(Optional.of(protocolGateway));

            AtomicReference<String> received = new AtomicReference<>();
            proxyService.proxyStream(testRequest, testAuthResult, RoutingStrategy.WEIGHTED,
                    received::set, () -> {}, error -> {});

            verify(protocolGateway).chatStream(eq(testRequest), eq("https://api.openai.com"),
                    eq("sk-test-key"), eq(60), any());
        }

        @Test
        @DisplayName("协议网关不存在时抛出异常")
        void proxyStream_protocolGatewayNotFound_throwsException() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayRegistry.getGateway("openai")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> proxyService.proxyStream(testRequest, testAuthResult, RoutingStrategy.WEIGHTED,
                    data -> {}, () -> {}, error -> {}))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No protocol gateway found");
        }
    }
}