package com.codingas.gateway.domain.supply.gateway;

import java.util.List;

/**
 * 协议网关工厂，按协议类型创建绑定 Provider 配置的 Gateway 实例
 */
public interface ProtocolGatewayFactory {

    /**
     * 创建绑定特定 Provider 配置的 ProtocolGateway 实例
     *
     * @param protocol       协议标识（"openai" / "anthropic"）
     * @param baseUrl        上游 Base URL
     * @param apiKey         上游 API Key
     * @param timeoutSeconds 超时秒数
     * @return 绑定配置的 ProtocolGateway 实例
     */
    ProtocolGateway create(String protocol, String baseUrl, String apiKey, int timeoutSeconds);

    /**
     * 获取系统支持的所有协议标识
     */
    List<String> getSupportedProtocols();
}