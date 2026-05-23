package com.codingas.gateway.infrastructure.proxy.gateway.protocol;

import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AnthropicProtocolGateway 单元测试
 */
class AnthropicProtocolGatewayTest {

    private AnthropicProtocolGateway gateway;
    private OkHttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = new OkHttpClient.Builder().build();
        gateway = new AnthropicProtocolGateway(httpClient);
    }

    @Nested
    @DisplayName("协议元数据")
    class Metadata {

        @Test
        @DisplayName("协议名称为 anthropic")
        void protocolName() {
            assertThat(gateway.getProtocolName()).isEqualTo("anthropic");
        }

        @Test
        @DisplayName("协议标签非空")
        void protocolLabel() {
            assertThat(gateway.getProtocolLabel()).isNotBlank();
        }

        @Test
        @DisplayName("默认 Base URL 非空")
        void defaultBaseUrl() {
            assertThat(gateway.getDefaultBaseUrl()).isNotBlank();
        }

        @Test
        @DisplayName("默认测试模型非空")
        void defaultTestModel() {
            assertThat(gateway.getDefaultTestModel()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("API Key 验证")
    class ApiKeyValidation {

        @Test
        @DisplayName("sk-ant- 开头的 Key 有效")
        void validateApiKeyFormat_skAntPrefix_valid() {
            assertThat(gateway.validateApiKeyFormat("sk-ant-api03-abc123")).isTrue();
        }

        @Test
        @DisplayName("空 Key 无效")
        void validateApiKeyFormat_empty_invalid() {
            assertThat(gateway.validateApiKeyFormat("")).isFalse();
        }

        @Test
        @DisplayName("null Key 无效")
        void validateApiKeyFormat_null_invalid() {
            assertThat(gateway.validateApiKeyFormat(null)).isFalse();
        }

        @Test
        @DisplayName("不以 sk-ant- 开头的 Key 无效")
        void validateApiKeyFormat_noSkAntPrefix_invalid() {
            assertThat(gateway.validateApiKeyFormat("sk-abc123")).isFalse();
        }
    }

    @Nested
    @DisplayName("连通性测试")
    class ConnectivityTest {

        @Test
        @DisplayName("无效 URL 连通性测试失败")
        void testConnectivity_invalidUrl_returnsFailure() {
            ConnectivityTestResult result = gateway.testConnectivity(
                "sk-ant-test", "https://invalid-url.example.com", "claude-haiku-3-5-20250514");

            assertThat(result.success()).isFalse();
        }
    }
}