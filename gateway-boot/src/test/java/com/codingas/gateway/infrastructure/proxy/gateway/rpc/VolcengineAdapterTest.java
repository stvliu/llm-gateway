package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VolcengineAdapter 单元测试
 */
class VolcengineAdapterTest {

    private VolcengineAdapter adapter;
    private OkHttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = new OkHttpClient.Builder().build();
        adapter = new VolcengineAdapter(
            httpClient,
            "https://ark.cn-beijing.volces.com/api/v3",
            "test-api-key",
            30
        );
    }

    @Test
    @DisplayName("getProviderCode() 返回 'volcengine'")
    void getProviderCode_returnsVolcengine() {
        assertThat(adapter.getProviderCode()).isEqualTo("volcengine");
    }

    @Test
    @DisplayName("getProviderName() 返回 'volcengine'")
    void getProviderName_returnsVolcengine() {
        assertThat(adapter.getProviderName()).isEqualTo("volcengine");
    }

    @Test
    @DisplayName("getCapabilities() 返回正确的能力描述")
    void getCapabilities_returnsCorrectCapabilities() {
        var capabilities = adapter.getCapabilities();

        assertThat(capabilities.isSupportsChatCompletion()).isTrue();
        assertThat(capabilities.isSupportsStreaming()).isTrue();
        assertThat(capabilities.isSupportsFunctionCalling()).isTrue();
        assertThat(capabilities.getSupportedModels()).contains("doubao-pro-32k");
    }

    @Test
    @DisplayName("isAvailable() 当 API Key 存在时返回 true")
    void isAvailable_whenApiKeyPresent_returnsTrue() {
        assertThat(adapter.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable() 当 API Key 为空时返回 false")
    void isAvailable_whenApiKeyEmpty_returnsFalse() {
        VolcengineAdapter emptyKeyAdapter = new VolcengineAdapter(
            httpClient,
            "https://ark.cn-beijing.volces.com/api/v3",
            "",
            30
        );

        assertThat(emptyKeyAdapter.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("getDefaultTimeout() 返回配置的超时时间")
    void getDefaultTimeout_returnsConfiguredTimeout() {
        assertThat(adapter.getDefaultTimeout()).isEqualTo(30);
    }

    @Test
    @DisplayName("checkConnection() 返回 false 因为火山引擎需要 endpoint_id")
    void checkConnection_returnsFalse() {
        assertThat(adapter.checkConnection()).isFalse();
    }
}
