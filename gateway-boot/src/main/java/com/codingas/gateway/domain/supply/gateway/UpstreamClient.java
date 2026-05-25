package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResult;

/**
 * 上游调用接口，负责调用上游 LLM API
 *
 * <p>每个实例绑定特定 Provider 配置（endpointUrl/apiKey/timeout），通过 UpstreamClientRegistry 获取。</p>
 */
public interface UpstreamClient {

    /**
     * 非流式调用
     */
    ProtocolResponse chat(ProtocolRequest request);

    /**
     * 流式调用
     */
    void chatStream(ProtocolRequest request, StreamCallback callback);

    /**
     * 连通性测试（测试已绑定 Provider 的连通性）
     */
    ConnectivityTestResult testConnectivity();
}