package com.codingas.gateway.domain.proxy.service;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
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
            when(gatewayRegistry.getGateway("OPENAI"))
                    .thenReturn(Optional.of(gateway));

            LLMGateway result = service.selectGateway("OPENAI");

            assertThat(result).isEqualTo(gateway);
        }

        @Test
        @DisplayName("Gateway 不存在抛出异常")
        void selectGateway_notFound_throwsException() {
            when(gatewayRegistry.getGateway("OPENAI"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.selectGateway("OPENAI"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("No gateway available");
        }
    }

    @Nested
    @DisplayName("forward 方法测试")
    class ForwardTests {

        @Test
        @DisplayName("成功转发请求")
        void forward_success() {
            LLMRequest request = createTestRequest();
            LLMResponse response = createTestResponse();

            when(gateway.chat(any(), anyString(), anyString(), anyInt())).thenReturn(response);

            LLMResponse result = service.forward(gateway, request, "https://api.openai.com", "sk-key", 60);

            assertThat(result).isEqualTo(response);
            verify(gateway).chat(request, "https://api.openai.com", "sk-key", 60);
        }
    }

    @Nested
    @DisplayName("forwardStream 方法测试")
    class ForwardStreamTests {

        @Test
        @DisplayName("成功转发流式请求")
        void forwardStream_success() {
            LLMRequest request = createTestRequest();
            doNothing().when(gateway).chatStream(any(), anyString(), anyString(), anyInt(), any());

            service.forwardStream(gateway, request, "https://api.openai.com", "sk-key", 60, callback);

            verify(gateway).chatStream(request, "https://api.openai.com", "sk-key", 60, callback);
        }
    }

    @Nested
    @DisplayName("isGatewayAvailable 方法测试")
    class IsGatewayAvailableTests {

        @Test
        @DisplayName("Gateway 存在返回 true")
        void isGatewayAvailable_available_returnsTrue() {
            when(gatewayRegistry.getGateway("OPENAI"))
                    .thenReturn(Optional.of(gateway));

            boolean result = service.isGatewayAvailable("OPENAI");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Gateway 不存在返回 false")
        void isGatewayAvailable_notFound_returnsFalse() {
            when(gatewayRegistry.getGateway("OPENAI"))
                    .thenReturn(Optional.empty());

            boolean result = service.isGatewayAvailable("OPENAI");

            assertThat(result).isFalse();
        }
    }

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
