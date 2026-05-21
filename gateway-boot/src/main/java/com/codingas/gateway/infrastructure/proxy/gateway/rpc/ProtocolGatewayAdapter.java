package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;

/**
 * ProtocolGateway 适配器包装
 *
 * <p>将 ProtocolGateway 包装为 LLMAdapter，用于旧架构兼容。</p>
 * <p>临时适配器，baseUrl 和 apiKey 在构造时传入。</p>
 */
public class ProtocolGatewayAdapter implements LLMAdapter {

    private final ProtocolGateway protocolGateway;
    private final String baseUrl;
    private final String apiKey;

    public ProtocolGatewayAdapter(ProtocolGateway protocolGateway, String baseUrl, String apiKey) {
        this.protocolGateway = protocolGateway;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderCode() {
        return protocolGateway.getProtocolName();
    }

    @Override
    public String getProviderName() {
        return protocolGateway.getProtocolName();
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public boolean isHealthy() {
        return isAvailable();
    }

    @Override
    public boolean checkConnection() {
        return protocolGateway.testConnectivity(apiKey, baseUrl, protocolGateway.getDefaultTestModel()).success();
    }

    @Override
    public ConnectivityTestResult testConnectivity(String testApiKey, String testBaseUrl, String testModel) {
        return protocolGateway.testConnectivity(testApiKey, testBaseUrl, testModel);
    }

    @Override
    public String getDefaultTestModel() {
        return protocolGateway.getDefaultTestModel();
    }

    @Override
    public String getDefaultBaseUrl() {
        return protocolGateway.getDefaultBaseUrl();
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        return protocolGateway.chat(request, baseUrl, apiKey, 60);
    }

    @Override
    public void chatStream(LLMRequest request, StreamCallback callback) {
        protocolGateway.chatStream(request, baseUrl, apiKey, 60, callback);
    }

    @Override
    public int getDefaultTimeout() {
        return 60;
    }
}