package com.codingas.gateway.domain.proxy.gateway;

import java.util.List;
import java.util.Optional;

/**
 * 协议网关注册表接口
 *
 * <p>按协议名称查找 ProtocolGateway 实现。</p>
 */
public interface ProtocolGatewayRegistry {

    /**
     * 根据协议名称获取网关
     */
    Optional<ProtocolGateway> getGateway(String protocolName);

    /**
     * 获取所有已注册的协议网关
     */
    List<ProtocolGateway> getAllGateways();
}