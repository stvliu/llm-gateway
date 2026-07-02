package com.codingas.gateway.application.channel;

/**
 * 渠道应急操作服务接口
 *
 * <p>提供运维应急场景下的渠道级操作：</p>
 * <ul>
 *   <li>一键熔断（forceOpen）：强制端点熔断器进入 OPEN，立即切断端点流量</li>
 *   <li>一键恢复（forceClose）：强制端点熔断器回到 CLOSED 并重置窗口，立即恢复流量</li>
 *   <li>状态查询（getState）：查询端点熔断器当前状态</li>
 * </ul>
 *
 * <p>委托 {@link com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager}
 * 与 {@link com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway}。</p>
 */
public interface ChannelEmergencyService {

    /**
     * 应急强制熔断端点
     *
     * @param channelId   渠道 ID
     * @param endpointId  端点 ID（须属于该渠道）
     * @return 熔断后的状态名（OPEN）
     */
    String forceOpen(Long channelId, Long endpointId);

    /**
     * 应急强制恢复端点
     *
     * @param channelId   渠道 ID
     * @param endpointId  端点 ID（须属于该渠道）
     * @return 恢复后的状态名（CLOSED）
     */
    String forceClose(Long channelId, Long endpointId);

    /**
     * 查询端点熔断器当前状态
     *
     * @param channelId   渠道 ID
     * @param endpointId  端点 ID（须属于该渠道）
     * @return 熔断器状态名（CLOSED/OPEN/HALF_OPEN）
     */
    String getState(Long channelId, Long endpointId);
}
