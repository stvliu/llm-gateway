package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.proxy.protocol.OpenAIChatRequest;
import com.codingas.gateway.domain.proxy.protocol.OpenAIChatResponse;
import com.codingas.gateway.domain.proxy.protocol.ProtocolResponse;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.entity.RoutingStrategy;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    private ProtocolGatewayFactory protocolGatewayFactory;

    @Mock
    private ProtocolGateway protocolGateway;

    private ProxyServiceImpl proxyService;

    private OpenAIChatRequest testRequest;
    private ProtocolResponse testProtocolResponse;
    private UserAuthResult testAuthResult;
    private RoutingContext testContext;

    @BeforeEach
    void setUp() {
        proxyService = new ProxyServiceImpl(channelRoutingService, protocolGatewayFactory);

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

        testRequest = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of())
                .build();

        testProtocolResponse = OpenAIChatResponse.builder()
                .id("chatcmpl-123")
                .model("gpt-4")
                .build();
    }

    @Nested
    @DisplayName("proxy 方法测试")
    class ProxyTests {

        @Test
        @DisplayName("成功代理非流式请求")
        void proxy_success() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayFactory.create(eq("openai"), anyString(), anyString(), anyInt())).thenReturn(protocolGateway);
            when(protocolGateway.chat(any())).thenReturn(testProtocolResponse);

            var response = proxyService.proxy(testRequest, testAuthResult, RoutingStrategy.WEIGHTED);

            assertThat(response).isNotNull();
            verify(protocolGatewayFactory).create("openai", "https://api.openai.com", "sk-test-key", 60);
        }

        @Test
        @DisplayName("不支持的协议时抛出异常")
        void proxy_unsupportedProtocol_throwsException() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayFactory.create(eq("openai"), anyString(), anyString(), anyInt()))
                    .thenThrow(new IllegalArgumentException("不支持的协议: openai"));

            assertThatThrownBy(() -> proxyService.proxy(testRequest, testAuthResult, RoutingStrategy.WEIGHTED))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("proxyStream 方法测试")
    class ProxyStreamTests {

        @Test
        @DisplayName("成功代理流式请求（带回调）")
        void proxyStream_success() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayFactory.create(eq("openai"), anyString(), anyString(), anyInt())).thenReturn(protocolGateway);

            AtomicReference<String> received = new AtomicReference<>();
            proxyService.proxyStream(testRequest, testAuthResult, RoutingStrategy.WEIGHTED,
                    received::set, () -> {}, error -> {});

            verify(protocolGateway).chatStream(any(), any());
        }

        @Test
        @DisplayName("不支持的协议时抛出异常")
        void proxyStream_unsupportedProtocol_throwsException() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testContext);
            when(protocolGatewayFactory.create(eq("openai"), anyString(), anyString(), anyInt()))
                    .thenThrow(new IllegalArgumentException("不支持的协议: openai"));

            assertThatThrownBy(() -> proxyService.proxyStream(testRequest, testAuthResult, RoutingStrategy.WEIGHTED,
                    data -> {}, () -> {}, error -> {}))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
