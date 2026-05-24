package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.gateway.ConnectivityTester;
import com.codingas.gateway.domain.supply.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 连通性测试实现
 *
 * <p>基于 ProtocolGatewayFactory 执行分层连通性测试。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConnectivityTesterImpl implements ConnectivityTester {

    private final ProtocolGatewayFactory protocolGatewayFactory;

    @Override
    public ConnectivityTestResultVO test(Channel channel) {
        try {
            var gateway = protocolGatewayFactory.create(
                    channel.getProtocol().getCode(),
                    channel.getEndpointUrl(),
                    null,
                    channel.getTimeout() != null ? channel.getTimeout() : 30
            );
            return gateway.testConnectivity();
        } catch (Exception e) {
            log.error("连通性测试失败, channelId={}", channel.getId(), e);
            return ConnectivityTestResultVO.failure(channel.getId(), e.getMessage());
        }
    }
}