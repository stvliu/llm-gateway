package com.codingas.gateway.infrastructure.proxy.gateway.protocol;

import com.codingas.gateway.domain.proxy.valueobject.ConnectivityTestResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAIProtocolGateway 单元测试
 */
class OpenAIProtocolGatewayTest {

    private OpenAIProtocolGateway gateway;

    @BeforeEach
    void setUp() {
        OkHttpClient httpClient = new OkHttpClient.Builder().build();
        ObjectMapper objectMapper = new ObjectMapper();
        gateway = new OpenAIProtocolGateway(httpClient, "https://invalid-url.example.com", "sk-test", 10, objectMapper);
    }

    @Nested
    @DisplayName("连通性测试")
    class ConnectivityTest {

        @Test
        @DisplayName("无效 URL 连通性测试失败")
        void testConnectivity_invalidUrl_returnsFailure() {
            ConnectivityTestResultVO result = gateway.testConnectivity();
            assertThat(result.success()).isFalse();
        }
    }
}