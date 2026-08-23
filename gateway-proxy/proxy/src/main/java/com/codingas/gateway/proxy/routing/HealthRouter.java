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
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.resilience.circuitbreaker.ChannelEndpointCircuitBreakerManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 健康路由器 — 按端点粒度过滤熔断中的实例
 *
 * <p>顺序语义：排在 {@link PriorityRouter} 之前（@Order 200 < 300），
 * 先剔除熔断端点，再由优先级路由在健康候选中择优，
 * 确保高优先级渠道熔断时可回退到次优先级健康渠道。</p>
 *
 * <p>熔断 key 统一为 endpointId（与 {@code KeyFailoverInvoker} 共享同一
 * {@link ChannelEndpointCircuitBreakerManager} 单例 bean）：RouterChain 在
 * ModelInstance（channel 粒度）过滤阶段尚未绑定 endpoint，故采用「运行时派生」方案——
 * 依据 {@link RoutingRequest#getProtocol()} 入站协议，通过 {@link EndpointResolver}
 * 从实例的 channelId 解析对应 {@link ChannelEndpoint}，取其 id 作为熔断 key。
 * 这样路由阶段过滤的熔断端点与调用阶段跳过的熔断端点完全一致，避免「路由放过、调用跳过」
 * 的语义割裂。</p>
 *
 * <p>边界处理：当入站协议为 null，或 {@link EndpointResolver#resolve} 无法为该 channel
 * 解析出任何端点（抛 {@link ResourceNotFoundException}）时，视为该实例不可用，予以过滤。
 * 保守策略——避免向熔断器注入未知 key，同时与「无可用端点的实例不应被路由」的语义一致。</p>
 */
@Component
@Order(200)
@RequiredArgsConstructor
public class HealthRouter implements Router {

    private static final Logger log = LoggerFactory.getLogger(HealthRouter.class);

    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;
    private final EndpointResolver endpointResolver;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        return instances.stream()
                .filter(mi -> isEndpointAvailable(mi, request.getProtocol()))
                .toList();
    }

    /**
     * 判断实例对应端点是否可用 — 按入站协议从 channelId 派生 endpointId，查询端点级熔断状态
     *
     * <p>endpoint 派生失败（协议为空或无可用端点）时返回 false，实例被视为不可用而过滤。</p>
     *
     * @param mi       模型实例
     * @param protocol 入站协议
     * @return 端点熔断器允许请求时返回 true；派生失败或熔断打开时返回 false
     */
    private boolean isEndpointAvailable(ModelInstance mi, Protocol protocol) {
        if (protocol == null) {
            log.debug("实例 {} 入站协议为空，无法派生 endpointId，视为不可用", mi.getId());
            return false;
        }
        try {
            ChannelEndpoint endpoint = endpointResolver.resolve(mi.getChannelId(), protocol);
            return circuitBreakerManager.isAvailable(endpoint.getId());
        } catch (ResourceNotFoundException e) {
            log.debug("实例 {} 的 channel {} 无 {} 协议端点，视为不可用",
                    mi.getId(), mi.getChannelId(), protocol);
            return false;
        }
    }

    @Override
    public boolean isForce() { return true; }
}
