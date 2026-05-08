package com.codingas.gateway.application.proxy;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.common.event.TokenUsedEvent;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.service.ModelDomainService;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.gateway.LLMGateway;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.gateway.StreamCallbackFactory;
import com.codingas.gateway.domain.proxy.service.ProxyDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ProxyService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProxyService 测试")
class ProxyServiceTest {

    @Mock
    private ModelDomainService modelDomainService;

    @Mock
    private ProxyDomainService proxyDomainService;

    @Mock
    private StreamCallbackFactory streamCallbackFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LLMGateway gateway;

    @InjectMocks
    private ProxyServiceImpl proxyService;

    private Model testModel;
    private Provider testProvider;
    private ModelDomainService.ModelProviderInfo testModelInfo;
    private LLMRequest testRequest;
    private LLMResponse testResponse;

    @BeforeEach
    void setUp() {
        // 准备测试 Provider
        testProvider = new Provider();
        testProvider.setId(1L);
        testProvider.setProviderType(ProviderType.OPENAI);

        // 准备测试 Model
        testModel = new Model();
        testModel.setId(1L);
        testModel.setProviderId(testProvider.getId());
        testModel.setProviderName(testProvider.getProviderName());

        // 准备 ModelProviderInfo
        testModelInfo = new ModelDomainService.ModelProviderInfo(testModel, testProvider);

        // 准备测试请求
        testRequest = LLMRequest.builder()
                .model("gpt-4")
                .messages(List.of())
                .build();

        // 准备测试响应
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
            // given
            when(modelDomainService.getModelWithProviderByProviderModelId("gpt-4")).thenReturn(testModelInfo);
            when(proxyDomainService.selectGateway(ProviderType.OPENAI)).thenReturn(gateway);
            when(proxyDomainService.forward(gateway, testRequest)).thenReturn(testResponse);
            when(gateway.getProviderCode()).thenReturn("openai");

            // when
            LLMResponse response = proxyService.proxy(testRequest, RouteGroup.RoutingStrategy.WEIGHTED);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getModel()).isEqualTo("gpt-4");
            verify(eventPublisher).publishEvent(any(TokenUsedEvent.class));
        }

        @Test
        @DisplayName("模型不存在时抛出异常")
        void proxy_modelNotFound_throwsException() {
            // given
            LLMRequest request = LLMRequest.builder()
                    .model("unknown-model")
                    .messages(List.of())
                    .build();
            when(modelDomainService.getModelWithProviderByProviderModelId("unknown-model"))
                    .thenThrow(new NoSuchElementException("Model not found: unknown-model"));

            // when & then
            assertThatThrownBy(() -> proxyService.proxy(request, RouteGroup.RoutingStrategy.WEIGHTED))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Model not found");
        }

        @Test
        @DisplayName("Gateway 不可用时抛出异常")
        void proxy_gatewayNotAvailable_throwsException() {
            // given
            when(modelDomainService.getModelWithProviderByProviderModelId("gpt-4")).thenReturn(testModelInfo);
            when(proxyDomainService.selectGateway(ProviderType.OPENAI))
                    .thenThrow(new IllegalStateException("Gateway not available"));

            // when & then
            assertThatThrownBy(() -> proxyService.proxy(testRequest, RouteGroup.RoutingStrategy.WEIGHTED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Gateway not available");
        }
    }

    @Nested
    @DisplayName("proxyStream 方法测试")
    class ProxyStreamTests {

        @Test
        @DisplayName("成功代理流式请求")
        void proxyStream_success() {
            // given
            Consumer<String> onChunk = mock(Consumer.class);
            StreamCallback streamCallback = mock(StreamCallback.class);

            when(modelDomainService.getModelWithProviderByProviderModelId("gpt-4")).thenReturn(testModelInfo);
            when(proxyDomainService.selectGateway(ProviderType.OPENAI)).thenReturn(gateway);
            when(streamCallbackFactory.create(any(), any(), any())).thenReturn(streamCallback);
            when(gateway.getProviderCode()).thenReturn("openai");

            // when
            proxyService.proxyStream(testRequest, RouteGroup.RoutingStrategy.WEIGHTED, onChunk);

            // then
            verify(streamCallbackFactory).create(eq(onChunk), any(Runnable.class), any(Consumer.class));
            verify(proxyDomainService).forwardStream(eq(gateway), eq(testRequest), eq(streamCallback));
        }
    }
}
