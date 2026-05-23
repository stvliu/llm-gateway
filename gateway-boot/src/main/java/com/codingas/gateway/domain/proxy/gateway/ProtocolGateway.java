package com.codingas.gateway.domain.proxy.gateway;

import com.codingas.gateway.domain.proxy.protocol.ProtocolRequest;
import com.codingas.gateway.domain.proxy.protocol.ProtocolResponse;
import com.codingas.gateway.domain.proxy.valueobject.ConnectivityTestResultVO;

/**
 * 协议网关接口，负责调用上游 LLM API
 *
 * <p>每个实例绑定特定 Provider 配置（baseUrl/apiKey/timeout），通过 ProtocolGatewayFactory 创建。</p>
 */
public interface ProtocolGateway {

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
    ConnectivityTestResultVO testConnectivity();
}