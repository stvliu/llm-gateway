package com.codingas.gateway.domain.proxy.service;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.LLMGateway;
import com.codingas.gateway.domain.proxy.gateway.LLMGatewayRegistry;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProxyDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProxyDomainService 测试")
class ProxyDomainServiceTest {

    @Mock
    private LLMGatewayRegistry gatewayRegistry;

    @Mock
    private LLMGateway gateway;

    @Mock
    private StreamCallback callback;

    private ProxyDomainService service;

    @BeforeEach
    void setUp() {
        service = new ProxyDomainService(gatewayRegistry);
    }

    @Nested
    @DisplayName("selectGateway 方法测试")
    class SelectGatewayTests {

        @Test
        @DisplayName("成功选择 Gateway")
        void selectGateway_success() {
            // Given
            when(gatewayRegistry.getGateway(ProviderType.OPENAI))
                    .thenReturn(Optional.of(gateway));
            when(gateway.isAvailable()).thenReturn(true);

            // When
            LLMGateway result = service.selectGateway(ProviderType.OPENAI);

            // Then
            assertThat(result).isEqualTo(gateway);
        }

        @Test
        @DisplayName("Gateway 不存在抛出异常")
        void selectGateway_notFound_throwsException() {
            // Given
            when(gatewayRegistry.getGateway(ProviderType.OPENAI))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.selectGateway(ProviderType.OPENAI))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("No gateway available");
        }

        @Test
        @DisplayName("Gateway 不可用抛出异常")
        void selectGateway_notAvailable_throwsException() {
            // Given
            when(gatewayRegistry.getGateway(ProviderType.OPENAI))
                    .thenReturn(Optional.of(gateway));
            when(gateway.isAvailable()).thenReturn(false);
            when(gateway.getProviderCode()).thenReturn("openai");

            // When & Then
            assertThatThrownBy(() -> service.selectGateway(ProviderType.OPENAI))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Gateway not available");
        }
    }

    @Nested
    @DisplayName("forward 方法测试")
    class ForwardTests {

        @Test
        @DisplayName("成功转发请求")
        void forward_success() {
            // Given
            LLMRequest request = createTestRequest();
            LLMResponse response = createTestResponse();

            when(gateway.isAvailable()).thenReturn(true);
            when(gateway.chat(any())).thenReturn(response);

            // When
            LLMResponse result = service.forward(gateway, request);

            // Then
            assertThat(result).isEqualTo(response);
            verify(gateway).chat(request);
        }

        @Test
        @DisplayName("Gateway 不可用抛出异常")
        void forward_gatewayNotAvailable_throwsException() {
            // Given
            LLMRequest request = createTestRequest();
            when(gateway.isAvailable()).thenReturn(false);
            when(gateway.getProviderCode()).thenReturn("openai");

            // When & Then
            assertThatThrownBy(() -> service.forward(gateway, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Gateway not available");
        }
    }

    @Nested
    @DisplayName("forwardStream 方法测试")
    class ForwardStreamTests {

        @Test
        @DisplayName("成功转发流式请求")
        void forwardStream_success() {
            // Given
            LLMRequest request = createTestRequest();
            when(gateway.isAvailable()).thenReturn(true);
            doNothing().when(gateway).chatStream(any(), any());

            // When
            service.forwardStream(gateway, request, callback);

            // Then
            verify(gateway).chatStream(request, callback);
        }

        @Test
        @DisplayName("Gateway 不可用抛出异常")
        void forwardStream_gatewayNotAvailable_throwsException() {
            // Given
            LLMRequest request = createTestRequest();
            when(gateway.isAvailable()).thenReturn(false);
            when(gateway.getProviderCode()).thenReturn("anthropic");

            // When & Then
            assertThatThrownBy(() -> service.forwardStream(gateway, request, callback))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Gateway not available");
        }
    }

    @Nested
    @DisplayName("isGatewayAvailable 方法测试")
    class IsGatewayAvailableTests {

        @Test
        @DisplayName("Gateway 存在且可用返回 true")
        void isGatewayAvailable_available_returnsTrue() {
            // Given
            when(gatewayRegistry.getGateway(ProviderType.OPENAI))
                    .thenReturn(Optional.of(gateway));
            when(gateway.isAvailable()).thenReturn(true);

            // When
            boolean result = service.isGatewayAvailable(ProviderType.OPENAI);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Gateway 存在但不可用返回 false")
        void isGatewayAvailable_notAvailable_returnsFalse() {
            // Given
            when(gatewayRegistry.getGateway(ProviderType.OPENAI))
                    .thenReturn(Optional.of(gateway));
            when(gateway.isAvailable()).thenReturn(false);

            // When
            boolean result = service.isGatewayAvailable(ProviderType.OPENAI);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Gateway 不存在返回 false")
        void isGatewayAvailable_notFound_returnsFalse() {
            // Given
            when(gatewayRegistry.getGateway(ProviderType.OPENAI))
                    .thenReturn(Optional.empty());

            // When
            boolean result = service.isGatewayAvailable(ProviderType.OPENAI);

            // Then
            assertThat(result).isFalse();
        }
    }

    // Helper methods
    private LLMRequest createTestRequest() {
        return LLMRequest.builder()
                .model("gpt-4")
                .messages(java.util.List.of(
                        LLMRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .build();
    }

    private LLMResponse createTestResponse() {
        return LLMResponse.builder()
                .id("test-id")
                .model("gpt-4")
                .content(LLMResponse.Content.builder()
                        .text("Hello!")
                        .build())
                .build();
    }
}
