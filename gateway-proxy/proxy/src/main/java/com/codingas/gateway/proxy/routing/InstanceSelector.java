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
import com.codingas.gateway.iam.application.ApplicationChannel;
import com.codingas.gateway.iam.application.ApplicationChannelRepository;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型实例选择器 — 委托给 RouterChain 执行权限过滤 + 健康过滤 + 优先级分组，返回候选列表
 *
 * <p>Task 3：查 {@link ApplicationChannelRepository#findByApplicationId(Long)} 取该应用所有授权渠道
 * 的 priority，构建 {@code channelPriorityMap} 填入 {@link RoutingRequest}，供 {@link PriorityRouter}
 * 按应用级 priority 升序排序（同一渠道对不同应用可有不同转移顺序）。applicationId 为 null 时传空映射。</p>
 *
 * <p>Task 8：移除容灾画像解析（{@code ResilienceResolver} 退场）。timeout 已下沉到
 * {@code Application.timeout}，不再在路由链解析画像。</p>
 */
@Component
@RequiredArgsConstructor
public class InstanceSelector {

    private static final Logger log = LoggerFactory.getLogger(InstanceSelector.class);

    private final ModelInstanceRepository modelInstanceRepository;
    private final RouterChain routerChain;
    /** 应用-渠道授权网关（Task 3 取应用级渠道 priority） */
    private final ApplicationChannelRepository applicationChannelRepository;

    /**
     * 根据 modelId 和用户身份选择模型实例候选列表
     *
     * <p>返回按应用级 priority 升序的候选列表（顺序由 {@link PriorityRouter} 保证），
     * 供 L1 故障转移逐个尝试。LoadBalanceRouter 已降级为透传，不再收敛到单实例。</p>
     *
     * @param modelId       模型 ID
     * @param applicationId 应用 ID（权限锚点；透传至 RoutingRequest 供 PermissionRouter 判定可见渠道）
     * @param userId        用户 ID
     * @param role          用户角色
     * @param strategy      路由策略
     * @param protocol      入站协议（透传至 RoutingRequest 供 HealthRouter 派生 endpointId）
     * @return 按应用级 priority 升序的候选实例列表（顺序由 PriorityRouter 保证，供 L1 故障转移逐个尝试）
     * @throws ResourceNotFoundException 无可用实例
     */
    public List<ModelInstance> select(Long modelId, Long applicationId, Long userId, String role,
                                      RoutingStrategy strategy, Protocol protocol) {
        // 获取所有活跃实例（DB 按 ModelInstance.priority 粗排，PriorityRouter 用应用级映射精排覆盖顺序）
        List<ModelInstance> allInstances = modelInstanceRepository.findActiveByModelIdOrderByPriority(modelId);
        if (allInstances.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 构建应用级渠道优先级映射（Task 3）：同一渠道对不同应用可有不同转移顺序
        Map<Long, Integer> channelPriorityMap = buildChannelPriorityMap(applicationId);

        // 委托 RouterChain 执行过滤链（applicationId 权限锚点、protocol 派生 endpointId、
        // channelPriorityMap 应用级转移顺序）
        RoutingRequest request = new RoutingRequest(modelId, applicationId, userId, role, strategy, protocol,
                channelPriorityMap);
        List<ModelInstance> result = routerChain.filter(allInstances, request);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 返回候选列表（已按应用级 priority 升序，供 L1 故障转移逐个尝试；不再收敛到单实例）
        return result;
    }

    /**
     * 构建应用级渠道优先级映射（Task 3）
     *
     * <p>查 {@link ApplicationChannelRepository#findByApplicationId(Long)} 取该应用所有授权渠道，
     * 以 channelId 为 key、priority 为 value 构建映射；priority 为 null 的渠道不放入映射
     *（{@link PriorityRouter} 回退默认值 100）。applicationId 为 null 时返回空映射，不查网关。</p>
     *
     * @param applicationId 应用 ID
     * @return 应用级渠道优先级映射（key=channelId, value=priority）；applicationId 为 null 时为空
     */
    private Map<Long, Integer> buildChannelPriorityMap(Long applicationId) {
        if (applicationId == null) {
            return Map.of();
        }
        List<ApplicationChannel> channels = applicationChannelRepository.findByApplicationId(applicationId);
        Map<Long, Integer> map = new LinkedHashMap<>();
        for (ApplicationChannel channel : channels) {
            if (channel.getPriority() != null) {
                map.put(channel.getChannelId(), channel.getPriority());
            }
        }
        return map;
    }
}
