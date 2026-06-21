package com.codingas.gateway.application.channel;

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.resilience.entity.Cluster;
import com.codingas.gateway.domain.resilience.gateway.ClusterGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 渠道应急操作服务实现
 *
 * <p>提供运维应急场景下的渠道级操作：一键熔断/恢复（forceOpen/forceClose）、
 * 状态查询（getState）、紧切域（switchCluster）。</p>
 *
 * <p>应急操作前校验端点归属（endpoint.channelId == 传入 channelId），
 * 避免误操作其他渠道的端点。紧切域校验目标故障域存在，不校验域健康
 * （是否跨域是运维决策）。</p>
 *
 * <p>依赖 {@link ChannelEndpointCircuitBreakerManager}（infrastructure）：
 * 遵循既有模式（参照 {@code ClusterHealthAggregator}），application 层
 * 直接注入 infrastructure Component 处理熔断器状态。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelEmergencyServiceImpl implements ChannelEmergencyService {

    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;
    private final ChannelEndpointGateway channelEndpointGateway;
    private final ChannelGateway channelGateway;
    private final ClusterGateway clusterGateway;

    @Override
    public String forceOpen(Long channelId, Long endpointId) {
        validateEndpointBelongsToChannel(channelId, endpointId);
        circuitBreakerManager.forceOpen(endpointId);
        String state = circuitBreakerManager.getState(endpointId).name();
        log.warn("应急熔断端点: channelId={}, endpointId={}, state={}", channelId, endpointId, state);
        return state;
    }

    @Override
    public String forceClose(Long channelId, Long endpointId) {
        validateEndpointBelongsToChannel(channelId, endpointId);
        circuitBreakerManager.forceClose(endpointId);
        String state = circuitBreakerManager.getState(endpointId).name();
        log.warn("应急恢复端点: channelId={}, endpointId={}, state={}", channelId, endpointId, state);
        return state;
    }

    @Override
    public String getState(Long channelId, Long endpointId) {
        validateEndpointBelongsToChannel(channelId, endpointId);
        return circuitBreakerManager.getState(endpointId).name();
    }

    @Override
    @Transactional
    public void switchCluster(Long channelId, Long clusterId) {
        // 渠道存在校验
        Channel channel = channelGateway.findById(channelId)
                .orElseThrow(() -> new GatewayRequestException("CHANNEL_NOT_FOUND", "渠道不存在: " + channelId));

        // 目标故障域存在校验
        Cluster targetCluster = clusterGateway.findById(clusterId);
        if (targetCluster == null) {
            throw new GatewayRequestException("CLUSTER_NOT_FOUND", "目标故障域不存在: " + clusterId);
        }

        Long oldClusterId = channel.getClusterId();
        channel.setClusterId(clusterId);
        channelGateway.save(channel);
        log.warn("紧急切换渠道故障域: channelId={}, {}→{}", channelId, oldClusterId, clusterId);
    }

    /**
     * 校验端点存在且属于指定渠道
     */
    private void validateEndpointBelongsToChannel(Long channelId, Long endpointId) {
        Optional<ChannelEndpoint> endpointOpt = channelEndpointGateway.findById(endpointId);
        if (endpointOpt.isEmpty()) {
            throw new GatewayRequestException("ENDPOINT_NOT_FOUND", "端点不存在: " + endpointId);
        }
        ChannelEndpoint endpoint = endpointOpt.get();
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new GatewayRequestException("ENDPOINT_NOT_BELONG_TO_CHANNEL",
                    String.format("端点 %d 不属于渠道 %d", endpointId, channelId));
        }
    }
}
