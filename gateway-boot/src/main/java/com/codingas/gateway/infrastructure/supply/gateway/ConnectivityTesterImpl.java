package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.gateway.ConnectivityTester;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 连通性测试实现
 *
 * <p>基于 UpstreamClientRegistry 执行分层连通性测试。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConnectivityTesterImpl implements ConnectivityTester {

    private final UpstreamClientRegistry upstreamClientRegistry;

    @Override
    public ConnectivityTestResultVO test(Channel channel) {
        // TODO: Channel 已不再持有 endpointUrl/protocol，需要通过 ChannelEndpoint 获取
        // 将在后续 Task 中通过 ChannelEndpointGateway 重构此方法
        try {
            var client = upstreamClientRegistry.getClient(
                    "openai",
                    "",
                    null,
                    channel.getTimeout() != null ? channel.getTimeout() : 30
            );
            return client.testConnectivity();
        } catch (Exception e) {
            log.error("连通性测试失败, channelId={}", channel.getId(), e);
            return ConnectivityTestResultVO.failure(channel.getId(), e.getMessage());
        }
    }
}