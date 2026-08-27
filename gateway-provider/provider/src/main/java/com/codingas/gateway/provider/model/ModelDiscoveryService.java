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
package com.codingas.gateway.provider.model;

import com.codingas.gateway.iam.application.ApplicationChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final ApplicationChannelRepository applicationChannelRepository;
    private final ModelInstanceRepository modelInstanceRepository;
    private final ModelRepository modelRepository;

    /**
     * 获取应用可见的模型列表
     *
     * <p>通过应用-渠道授权关联（{@link ApplicationChannelRepository}）查询应用可见的渠道集合，
     * 再汇聚这些渠道上活跃的 {@link ModelInstance}，返回可见的模型实体。
     * 应用 ID 为 null（无权限锚点）时返回空列表。</p>
     *
     * @param applicationId 应用 ID（数据面权限锚点，已认证身份携带）
     * @return 可见的模型实体列表（兼容 OpenAI 格式的响应由 web 层 DTO 组装）
     */
    public List<Model> getVisibleModels(Long applicationId) {
        // 无权限锚点：不返回任何模型
        if (applicationId == null) {
            log.debug("应用 ID 为空，无可见模型");
            return List.of();
        }

        // 通过应用查找授权的渠道集合
        Set<Long> channelIds = applicationChannelRepository.findChannelIdsByApplicationId(applicationId);
        if (channelIds.isEmpty()) {
            log.debug("应用未授权任何渠道: applicationId={}", applicationId);
            return List.of();
        }

        // 通过渠道查找可用模型（按 ModelInstance 的 modelId 去重，保持模型名唯一）
        return channelIds.stream()
                .flatMap(channelId -> modelInstanceRepository.findActiveByChannelId(channelId).stream())
                .map(mi -> modelRepository.findById(mi.getModelId()))
                .flatMap(opt -> opt.stream())
                .filter(Model::isAvailable)
                .collect(Collectors.toMap(Model::getModelName, m -> m, (a, b) -> a, LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }
}
