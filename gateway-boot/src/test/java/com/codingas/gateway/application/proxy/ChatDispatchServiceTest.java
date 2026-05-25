package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ChatDispatchService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatDispatchService 单元测试")
class ChatDispatchServiceTest {

    @Mock
    private RoutingResolver routingResolver;

    @Mock
    private OutboundTuner outboundTuner;

    @Mock
    private UpstreamClientRegistry clientRegistry;

    @Mock
    private ProtocolConverter protocolConverter;

    private ChatDispatchServiceImpl dispatchService;

    private Identity testIdentity;
    private RoutingContext openAIContext;

    @BeforeEach
    void setUp() {
        dispatchService = new ChatDispatchServiceImpl(routingResolver, outboundTuner, clientRegistry, protocolConverter);

        testIdentity = Identity.of(1L, "user", 1L);
        openAIContext = new RoutingContext(10L, 20L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-test", 60, false);
    }

    @Nested
    @DisplayName("dispatch 非流式调度")
    class DispatchTests {

        @Test
        @DisplayName("同协议调度：OpenAI→OpenAI")
        void dispatch_sameProtocol_noConversion() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            OpenAIChatResponse response = OpenAIChatResponse.builder().id("chatcmpl-123").model("gpt-4o").build();

            when(routingResolver.resolve("gpt-4o", Protocol.OPENAI)).thenReturn(openAIContext);
            when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(request);

            UpstreamClient client = mock(UpstreamClient.class);
            when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-test", 60)).thenReturn(client);
            when(client.chat(any(ProtocolRequest.class))).thenReturn(response);

            // when
            ProtocolResponse result = dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            assertThat(result).isInstanceOf(OpenAIChatResponse.class);
            verify(protocolConverter, never()).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter, never()).toOpenAI(any(AnthropicMessagesResponse.class));
        }

        @Test
        @DisplayName("跨协议调度：OpenAI→Anthropic")
        void dispatch_crossProtocol_withConversion() {
            // given
            OpenAIChatRequest request = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .build();

            RoutingContext anthropicContext = new RoutingContext(10L, 21L, "https://api.anthropic.com",
                    Protocol.ANTHROPIC, "sk-ant-key", 60, true);

            AnthropicMessagesRequest convertedRequest = AnthropicMessagesRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .messages(List.of(AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()))
                    .maxTokens(1024)
                    .build();

            AnthropicMessagesResponse upstreamResponse = AnthropicMessagesResponse.builder()
                    .id("msg-123").model("claude-3-5-sonnet-20241022").build();

            OpenAIChatResponse finalResponse = OpenAIChatResponse.builder().id("chatcmpl-123").model("gpt-4o").build();

            when(routingResolver.resolve("gpt-4o", Protocol.OPENAI)).thenReturn(anthropicContext);
            when(protocolConverter.toAnthropic(any(OpenAIChatRequest.class))).thenReturn(convertedRequest);
            when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(convertedRequest);

            UpstreamClient client = mock(UpstreamClient.class);
            when(clientRegistry.getClient("anthropic", "https://api.anthropic.com", "sk-ant-key", 60)).thenReturn(client);
            when(client.chat(any(ProtocolRequest.class))).thenReturn(upstreamResponse);
            when(protocolConverter.toOpenAI(any(AnthropicMessagesResponse.class))).thenReturn(finalResponse);

            // when
            ProtocolResponse result = dispatchService.dispatch(request, testIdentity, RoutingStrategy.WEIGHTED);

            // then
            assertThat(result).isInstanceOf(OpenAIChatResponse.class);
            verify(protocolConverter).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter).toOpenAI(any(AnthropicMessagesResponse.class));
        }

        @Test
        @DisplayName("不支持的请求类型抛出异常")
        void dispatch_unsupportedRequestType_throwsException() {
            // given — 使用不属于任何已知协议类型的 mock
            ProtocolRequest unsupportedRequest = mock(ProtocolRequest.class);

            // when & then
            assertThatThrownBy(() -> dispatchService.dispatch(unsupportedRequest, testIdentity, RoutingStrategy.WEIGHTED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不支持的请求类型");
        }
    }
}