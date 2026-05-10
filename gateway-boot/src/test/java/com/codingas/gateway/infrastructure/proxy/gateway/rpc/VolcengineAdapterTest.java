package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.model.enums.ProviderType;
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
        // 使用测试 API Key
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
    @DisplayName("getProviderType() 返回 VOLCENGINE")
    void getProviderType_returnsVolcengine() {
        assertThat(adapter.getProviderType()).isEqualTo(ProviderType.VOLCENGINE);
    }

    @Test
    @DisplayName("getCapabilities() 返回正确的能力描述")
    void getCapabilities_returnsCorrectCapabilities() {
        var capabilities = adapter.getCapabilities();

        assertThat(capabilities.providerType()).isEqualTo(ProviderType.VOLCENGINE);
        assertThat(capabilities.supportsChatCompletion()).isTrue();
        assertThat(capabilities.supportsStreaming()).isTrue();
        assertThat(capabilities.supportsFunctionCalling()).isTrue();
        assertThat(capabilities.supportedModels()).contains("doubao-pro-32k");
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
    @DisplayName("checkConnection() 返回 isAvailable() 因为火山引擎不支持 models 端点")
    void checkConnection_returnsIsAvailable() {
        assertThat(adapter.checkConnection()).isTrue();
    }
}
