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
package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 模型发现服务
 *
 * <p>D8：废弃团队模型可见性机制后，模型可见性由应用授权的渠道挂哪些 ModelInstance 隐式决定。
 * 本服务以应用 ID（数据面权限锚点）查询应用授权的渠道集合，再发现其上的活跃模型，
 * 不再依赖任何独立的模型可见性配置或团队维度过滤。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDiscoveryService {

    private final ApplicationChannelGateway applicationChannelGateway;
    private final ModelInstanceGateway modelInstanceGateway;
    private final ModelGateway modelGateway;

    /**
     * 获取应用可见的模型列表
     *
     * <p>通过应用-渠道授权关联（{@link ApplicationChannelGateway}）查询应用可见的渠道集合，
     * 再汇聚这些渠道上活跃的 {@link ModelInstance}，映射为兼容 OpenAI 格式的模型列表。
     * 应用 ID 为 null（无权限锚点）时返回空列表。</p>
     *
     * @param applicationId 应用 ID（数据面权限锚点，已认证身份携带）
     * @return 兼容 OpenAI 格式的模型列表
     */
    public ModelDiscoveryResponse getVisibleModels(Long applicationId) {
        // 无权限锚点：不返回任何模型
        if (applicationId == null) {
            log.debug("应用 ID 为空，无可见模型");
            return new ModelDiscoveryResponse("list", List.of());
        }

        // 通过应用查找授权的渠道集合
        Set<Long> channelIds = applicationChannelGateway.findChannelIdsByApplicationId(applicationId);
        if (channelIds.isEmpty()) {
            log.debug("应用未授权任何渠道: applicationId={}", applicationId);
            return new ModelDiscoveryResponse("list", List.of());
        }

        // 通过渠道查找可用模型（按 ModelInstance 的 modelId 去重）
        List<ModelDiscoveryResponse.ModelItem> items = channelIds.stream()
                .flatMap(channelId -> modelInstanceGateway.findActiveByChannelId(channelId).stream())
                .map(mi -> modelGateway.findById(mi.getModelId()))
                .flatMap(opt -> opt.stream())
                .filter(Model::isAvailable)
                .map(m -> new ModelDiscoveryResponse.ModelItem(
                        m.getModelName(),
                        "model",
                        m.getCreatedAt() != null ? m.getCreatedAt().getEpochSecond() : 0L,
                        "system"
                ))
                .distinct()
                .toList();

        log.debug("模型发现: applicationId={}, visibleModels={}", applicationId, items.size());
        return new ModelDiscoveryResponse("list", items);
    }
}
