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
import com.codingas.gateway.iam.application.ApplicationChannelConfigProvider;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.protocol.Protocol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型实例选择器 — 数据面认证即授权：配置读取前移 + 纯路由链
 *
 * <p>数据面授权已蕴含在认证（{@code Identity.applicationId} 即 Key 绑定的应用资源）：
 * 本类经 {@link ApplicationChannelConfigProvider#findChannelIdsByApplicationId}（缓存装饰）
 * 读取应用的渠道配置（配置而非授权判定），过滤活跃渠道（ABAC 属性 {@code state.isRoutable()}，
 * proxy 层执行），带条件查询实例
 * （{@link ModelInstanceRepository#findActiveByModelIdAndChannelIds}，DB 层过滤），
 * 再经 {@link RouterChain} 纯路由（优先级/健康）返回候选列表。</p>
 */
@Component
@RequiredArgsConstructor
public class InstanceSelector {

    private final ModelInstanceRepository modelInstanceRepository;
    private final RouterChain routerChain;
    /** 应用-渠道配置提供者（认证即授权：配置读取热点走本地缓存，非授权判定） */
    private final ApplicationChannelConfigProvider applicationChannelConfigProvider;
    /** 渠道仓储（活跃渠道过滤——ABAC 属性，proxy 层执行） */
    private final ChannelRepository channelRepository;

    /**
     * 根据 modelId 和用户身份选择模型实例候选列表
     *
     * <p>数据面授权已蕴含在认证（{@code Identity.applicationId} 即 Key 绑定的应用资源），
     * 本方法读取应用的渠道配置并过滤活跃渠道，带条件查询实例，再经 {@link RouterChain}
     * 纯路由（优先级/健康）返回候选列表。</p>
     *
     * @param modelId       模型 ID
     * @param applicationId 应用 ID（数据面权限锚点，认证产物）
     * @param userId        用户 ID（保留签名兼容，不再透传路由）
     * @param role          用户角色（保留签名兼容，不再透传路由）
     * @param strategy      路由策略
     * @param protocol      入站协议（透传至 RoutingRequest 供 HealthRouter 派生 endpointId）
     * @return 按优先级升序的候选实例列表
     * @throws ResourceNotFoundException 无可用实例
     */
    public List<ModelInstance> select(Long modelId, Long applicationId, Long userId, String role,
                                      RoutingStrategy strategy, Protocol protocol) {
        // 1. 应用渠道配置读取（配置，非授权）
        Set<Long> configuredChannelIds = getConfiguredChannelIds(applicationId);
        if (configuredChannelIds.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 2. 活跃渠道过滤（ABAC 属性：state.isRoutable）
        Set<Long> activeChannelIds = filterRoutableChannels(configuredChannelIds);
        if (activeChannelIds.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 3. 实例查询（DB 层过滤：模型 + 活跃渠道 + 活跃实例）
        List<ModelInstance> candidates = modelInstanceRepository
                .findActiveByModelIdAndChannelIds(modelId, activeChannelIds);
        if (candidates.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 4. 纯路由链（优先级/健康）
        Map<Long, Integer> channelPriorityMap = buildChannelPriorityMap(applicationId);
        RoutingRequest request = new RoutingRequest(modelId, applicationId, strategy, protocol, channelPriorityMap);
        List<ModelInstance> result = routerChain.filter(candidates, request);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }
        return result;
    }

    /** 应用渠道配置读取（缓存装饰；applicationId 为 null → 空集，由 Provider 内部处理） */
    private Set<Long> getConfiguredChannelIds(Long applicationId) {
        return applicationChannelConfigProvider.findChannelIdsByApplicationId(applicationId);
    }

    /** 活跃渠道过滤（state.isRoutable()） */
    private Set<Long> filterRoutableChannels(Set<Long> channelIds) {
        return channelRepository.findByIds(List.copyOf(channelIds)).stream()
                .filter(ch -> ch.getState() != null && ch.getState().isRoutable())
                .map(Channel::getId)
                .collect(Collectors.toSet());
    }

    /** 构建应用级渠道优先级映射（缓存装饰，null 优先级剔除由 Provider 内部处理；PriorityRouter 消费） */
    private Map<Long, Integer> buildChannelPriorityMap(Long applicationId) {
        return applicationChannelConfigProvider.findPriorityMapByApplicationId(applicationId);
    }
}
