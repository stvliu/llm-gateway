package com.codingas.gateway.infrastructure.proxy.gateway.protocol;

import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ProtocolGatewayRegistryImpl 单元测试
 */
class ProtocolGatewayRegistryImplTest {

    private ProtocolGateway openaiGateway;
    private ProtocolGateway anthropicGateway;

    @BeforeEach
    void setUp() {
        openaiGateway = new StubProtocolGateway("openai", "OpenAI Chat Completions 协议");
        anthropicGateway = new StubProtocolGateway("anthropic", "Anthropic Messages 协议");
    }

    @Nested
    @DisplayName("注册表初始化")
    class Initialization {

        @Test
        @DisplayName("正常注册多个协议网关")
        void registerMultipleGateways() {
            ProtocolGatewayRegistry registry = new ProtocolGatewayRegistryImpl(
                List.of(openaiGateway, anthropicGateway));

            assertThat(registry.getAllGateways()).hasSize(2);
        }

        @Test
        @DisplayName("重复 protocolName 抛出异常")
        void duplicateProtocolName_throwsException() {
            ProtocolGateway duplicate = new StubProtocolGateway("openai", "重复的 OpenAI");

            assertThatThrownBy(() -> new ProtocolGatewayRegistryImpl(
                List.of(openaiGateway, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复");
        }
    }

    @Nested
    @DisplayName("查找协议网关")
    class Lookup {

        private ProtocolGatewayRegistry registry;

        @BeforeEach
        void setUp() {
            registry = new ProtocolGatewayRegistryImpl(
                List.of(openaiGateway, anthropicGateway));
        }

        @Test
        @DisplayName("按名称查找存在的协议网关")
        void getGateway_existing_returnsGateway() {
            Optional<ProtocolGateway> found = registry.getGateway("openai");

            assertThat(found).isPresent();
            assertThat(found.get().getProtocolName()).isEqualTo("openai");
        }

        @Test
        @DisplayName("按名称查找不存在的协议网关返回空")
        void getGateway_notFound_returnsEmpty() {
            Optional<ProtocolGateway> found = registry.getGateway("gemini");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("获取所有已注册网关")
        void getAllGateways_returnsAll() {
            List<ProtocolGateway> all = registry.getAllGateways();

            assertThat(all).hasSize(2);
            assertThat(all.stream().map(ProtocolGateway::getProtocolName))
                .containsExactlyInAnyOrder("openai", "anthropic");
        }

        @Test
        @DisplayName("getAllGateways 返回不可变列表")
        void getAllGateways_returnsImmutableList() {
            List<ProtocolGateway> all = registry.getAllGateways();

            assertThatThrownBy(() -> all.add(openaiGateway))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    /**
     * 测试用 Stub 协议网关
     */
    private record StubProtocolGateway(String protocolName, String protocolLabel)
        implements ProtocolGateway {

        @Override
        public String getProtocolName() {
            return protocolName;
        }

        @Override
        public String getProtocolLabel() {
            return protocolLabel;
        }

        @Override
        public boolean validateApiKeyFormat(String apiKey) {
            return apiKey != null;
        }

        @Override
        public com.codingas.gateway.application.proxy.dto.LLMResponse chat(
            com.codingas.gateway.application.proxy.dto.LLMRequest request,
            String baseUrl, String apiKey, int timeoutSeconds) {
            throw new UnsupportedOperationException("Stub");
        }

        @Override
        public void chatStream(
            com.codingas.gateway.application.proxy.dto.LLMRequest request,
            String baseUrl, String apiKey, int timeoutSeconds,
            com.codingas.gateway.domain.proxy.gateway.StreamCallback callback) {
            throw new UnsupportedOperationException("Stub");
        }

        @Override
        public String getDefaultBaseUrl() {
            return "https://stub.example.com";
        }

        @Override
        public String getDefaultTestModel() {
            return "stub-model";
        }

        @Override
        public com.codingas.gateway.application.provider.dto.ConnectivityTestResult testConnectivity(
            String apiKey, String baseUrl, String model) {
            throw new UnsupportedOperationException("Stub");
        }
    }
}