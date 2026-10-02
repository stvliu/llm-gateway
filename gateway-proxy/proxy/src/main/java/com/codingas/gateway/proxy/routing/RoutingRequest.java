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

import com.codingas.gateway.protocol.Protocol;

import java.util.Map;

/**
 * 路由请求上下文 — 携带 RouterChain 各环节所需的信息
 *
 * <p>数据面认证即授权后，渠道可见性由 {@link InstanceSelector} 读取应用渠道配置保证
 * （配置而非授权判定），本上下文不再承担数据面授权；{@code applicationId} 为配置锚点。</p>
 *
 * <p>{@code protocol} 为入站协议，供 {@link HealthRouter} 按协议从 channelId 派生 endpointId，
 * 统一熔断 key 为 endpoint 粒度（与 {@code KeyFailoverInvoker} 共享同一熔断器 bean）。</p>
 *
 * <p>{@code channelPriorityMap} 携带应用级渠道转移优先级（key=channelId, value=priority），
 * 供 {@link PriorityRouter} 按应用级 priority 升序排序，实现同一渠道对不同应用不同转移顺序。
 * 为空表示无应用级映射（{@code PriorityRouter} 回退默认值 100）。</p>
 */
public class RoutingRequest {

    private final Long modelId;
    private final Long applicationId;
    private final RoutingStrategy strategy;
    private final Protocol protocol;
    /**
     * 应用级渠道转移优先级映射（key=channelId, value=priority，数值越小越优先）
     *
     * <p>由 {@link InstanceSelector} 查 {@code ApplicationChannelRepository.findByApplicationId}
     * 构建；为空（如 applicationId 为 null）时 {@link PriorityRouter} 回退默认值 100。</p>
     */
    private final Map<Long, Integer> channelPriorityMap;

    /**
     * 构造路由请求上下文
     *
     * <p>数据面认证即授权后，{@code userId}/{@code role} 不再透传（全链无消费者）；
     * 保留 {@code applicationId}（应用配置锚点）、{@code protocol}（HealthRouter 派生
     * endpointId）、{@code channelPriorityMap}（PriorityRouter 应用级优先级）。</p>
     *
     * @param modelId            模型 ID
     * @param applicationId      应用 ID（配置锚点）
     * @param strategy           路由策略
     * @param protocol           入站协议（可 null）
     * @param channelPriorityMap 应用级渠道优先级映射（为 null 时归并为空映射）
     */
    public RoutingRequest(Long modelId, Long applicationId, RoutingStrategy strategy,
                          Protocol protocol, Map<Long, Integer> channelPriorityMap) {
        this.modelId = modelId;
        this.applicationId = applicationId;
        this.strategy = strategy;
        this.protocol = protocol;
        // 防御 PriorityRouter.get() 的 NPE 潜伏：null 归并为空映射
        this.channelPriorityMap = channelPriorityMap != null ? channelPriorityMap : Map.of();
    }

    public Long getModelId() { return modelId; }

    public Long getApplicationId() { return applicationId; }

    public Protocol getProtocol() { return protocol; }

    /**
     * 返回应用级渠道转移优先级映射（key=channelId, value=priority，数值越小越优先）
     *
     * @return 渠道优先级映射（不应被调用方修改）；为空表示无应用级映射，{@link PriorityRouter} 回退默认值 100
     */
    public Map<Long, Integer> getChannelPriorityMap() { return channelPriorityMap; }
}
