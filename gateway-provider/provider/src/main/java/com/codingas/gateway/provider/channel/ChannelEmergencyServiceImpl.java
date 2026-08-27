/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.resilience.circuitbreaker.ChannelEndpointCircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 渠道应急操作服务实现
 *
 * <p>提供运维应急场景下的渠道级操作：一键熔断/恢复（forceOpen/forceClose）、
 * 状态查询（getState）。</p>
 *
 * <p>应急操作前校验端点归属（endpoint.channelId == 传入 channelId），
 * 避免误操作其他渠道的端点。</p>
 *
 * <p>依赖 {@link ChannelEndpointCircuitBreakerService}（infrastructure）：
 * 遵循 Service→Gateway 分层范式，application 层
 * 直接注入 infrastructure Component 处理熔断器状态。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelEmergencyServiceImpl implements ChannelEmergencyService {

    private final ChannelEndpointCircuitBreakerService circuitBreakerService;
    private final ChannelEndpointRepository channelEndpointRepository;

    @Override
    public String forceOpen(Long channelId, Long endpointId) {
        validateEndpointBelongsToChannel(channelId, endpointId);
        circuitBreakerService.forceOpen(endpointId);
        String state = circuitBreakerService.getState(endpointId).name();
        log.warn("应急熔断端点: channelId={}, endpointId={}, state={}", channelId, endpointId, state);
        return state;
    }

    @Override
    public String forceClose(Long channelId, Long endpointId) {
        validateEndpointBelongsToChannel(channelId, endpointId);
        circuitBreakerService.forceClose(endpointId);
        String state = circuitBreakerService.getState(endpointId).name();
        log.warn("应急恢复端点: channelId={}, endpointId={}, state={}", channelId, endpointId, state);
        return state;
    }

    @Override
    public String getState(Long channelId, Long endpointId) {
        validateEndpointBelongsToChannel(channelId, endpointId);
        return circuitBreakerService.getState(endpointId).name();
    }

    /**
     * 校验端点存在且属于指定渠道
     *
     * <p>使用 {@code channelId.equals(endpoint.getChannelId())} 而非反向调用，
     * 因为 channelId 来自 {@code @PathVariable} 永非 null，可避免端点 channelId
     * 为 null（脏数据/旧数据）时触发 NPE 被 {@code handleGenericException}
     * 误映射为 HTTP 500。channelId 与 null 比较为 false，落入既有
     * {@code ENDPOINT_NOT_BELONG_TO_CHANNEL} 业务异常分支，由全局异常处理器映射为 400。</p>
     */
    private void validateEndpointBelongsToChannel(Long channelId, Long endpointId) {
        Optional<ChannelEndpoint> endpointOpt = channelEndpointRepository.findById(endpointId);
        if (endpointOpt.isEmpty()) {
            throw new GatewayRequestException("ENDPOINT_NOT_FOUND", "端点不存在: " + endpointId);
        }
        ChannelEndpoint endpoint = endpointOpt.get();
        if (!channelId.equals(endpoint.getChannelId())) {
            throw new GatewayRequestException("ENDPOINT_NOT_BELONG_TO_CHANNEL",
                    String.format("端点 %d 不属于渠道 %d", endpointId, channelId));
        }
    }
}
