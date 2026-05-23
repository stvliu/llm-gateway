package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.proxy.protocol.*;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.entity.RoutingStrategy;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.domain.iam.valueobject.Identity;
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

    @Mock
    private ProtocolConverter protocolConverter;

    private ProxyServiceImpl proxyService;

    private OpenAIChatRequest testOpenAIRequest;
    private AnthropicMessagesRequest testAnthropicRequest;
    private OpenAIChatResponse testOpenAIResponse;
    private AnthropicMessagesResponse testAnthropicResponse;
    private Identity testAuthResult;
    private RoutingContext testOpenAIContext;
    private RoutingContext testAnthropicContext;

    @BeforeEach
    void setUp() {
        proxyService = new ProxyServiceImpl(channelRoutingService, protocolGatewayFactory, protocolConverter);

        testAuthResult = mock(Identity.class);

        testOpenAIContext = RoutingContext.builder()
                .providerId(1L)
                .providerName("openai")
                .productId(10L)
                .model("gpt-4")
                .protocol("openai")
                .providerApiKey("sk-test-key")
                .endpoint("https://api.openai.com")
                .build();

        testAnthropicContext = RoutingContext.builder()
                .providerId(2L)
                .providerName("anthropic")
                .productId(10L)
                .model("claude-3-5-sonnet-20241022")
                .protocol("anthropic")
                .providerApiKey("sk-ant-key")
                .endpoint("https://api.anthropic.com")
                .build();

        testOpenAIRequest = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                .build();

        testAnthropicRequest = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()))
                .maxTokens(1024)
                .build();

        testOpenAIResponse = OpenAIChatResponse.builder()
                .id("chatcmpl-123")
                .model("gpt-4")
                .build();

        testAnthropicResponse = AnthropicMessagesResponse.builder()
                .id("msg-123")
                .model("claude-3-5-sonnet-20241022")
                .build();
    }

    @Nested
    @DisplayName("proxy 方法测试")
    class ProxyTests {

        @Test
        @DisplayName("同协议代理：OpenAI→OpenAI")
        void proxy_sameProtocol_openai() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testOpenAIContext);
            when(protocolGatewayFactory.create(eq("openai"), anyString(), anyString(), anyInt())).thenReturn(protocolGateway);
            when(protocolGateway.chat(any())).thenReturn(testOpenAIResponse);

            var response = proxyService.proxy(testOpenAIRequest, testAuthResult, RoutingStrategy.WEIGHTED);

            assertThat(response).isInstanceOf(OpenAIChatResponse.class);
            verify(protocolConverter, never()).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter, never()).toOpenAI(any(AnthropicMessagesRequest.class));
        }

        @Test
        @DisplayName("跨协议代理：OpenAI→Anthropic")
        void proxy_crossProtocol_openaiToAnthropic() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testAnthropicContext);
            when(protocolGatewayFactory.create(eq("anthropic"), anyString(), anyString(), anyInt())).thenReturn(protocolGateway);
            when(protocolGateway.chat(any())).thenReturn(testAnthropicResponse);
            when(protocolConverter.toAnthropic(any(OpenAIChatRequest.class))).thenReturn(testAnthropicRequest);
            when(protocolConverter.toOpenAI(any(AnthropicMessagesResponse.class))).thenReturn(testOpenAIResponse);

            var response = proxyService.proxy(testOpenAIRequest, testAuthResult, RoutingStrategy.WEIGHTED);

            assertThat(response).isInstanceOf(OpenAIChatResponse.class);
            verify(protocolConverter).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter).toOpenAI(any(AnthropicMessagesResponse.class));
        }

        @Test
        @DisplayName("不支持的协议时抛出异常")
        void proxy_unsupportedProtocol_throwsException() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testOpenAIContext);
            when(protocolGatewayFactory.create(eq("openai"), anyString(), anyString(), anyInt()))
                    .thenThrow(new IllegalArgumentException("不支持的协议: openai"));

            assertThatThrownBy(() -> proxyService.proxy(testOpenAIRequest, testAuthResult, RoutingStrategy.WEIGHTED))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("proxyStream 方法测试")
    class ProxyStreamTests {

        @Test
        @DisplayName("同协议流式代理：OpenAI→OpenAI")
        void proxyStream_sameProtocol() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testOpenAIContext);
            when(protocolGatewayFactory.create(eq("openai"), anyString(), anyString(), anyInt())).thenReturn(protocolGateway);

            AtomicReference<String> received = new AtomicReference<>();
            proxyService.proxyStream(testOpenAIRequest, testAuthResult, RoutingStrategy.WEIGHTED,
                    received::set, () -> {}, error -> {});

            verify(protocolGateway).chatStream(any(), any());
        }

        @Test
        @DisplayName("不支持的协议时抛出异常")
        void proxyStream_unsupportedProtocol_throwsException() {
            when(channelRoutingService.resolve(any(), anyString(), any())).thenReturn(testOpenAIContext);
            when(protocolGatewayFactory.create(eq("openai"), anyString(), anyString(), anyInt()))
                    .thenThrow(new IllegalArgumentException("不支持的协议: openai"));

            assertThatThrownBy(() -> proxyService.proxyStream(testOpenAIRequest, testAuthResult, RoutingStrategy.WEIGHTED,
                    data -> {}, () -> {}, error -> {}))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
