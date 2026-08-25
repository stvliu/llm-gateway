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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelRepository;
import lombok.Data;

import java.util.List;

/**
 * 模型实例响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(ModelInstance, ModelRepository)} 从 {@code ModelInstance} 实体
 * 展开模型规格展示字段（modelName/displayName/modelFamily）后生成。</p>
 */
@Data
public class ModelInstanceResponse {
    private Long id;
    private Long channelId;
    private Long modelId;
    private String modelName;
    private String displayName;
    private String modelFamily;
    private String upstreamModelName;
    private Integer priority;
    private Integer weight;
    private String state;

    /**
     * 从模型实例实体转换（展开模型规格展示字段）
     *
     * @param instance        模型实例实体
     * @param modelRepository 模型仓储（查规格展示字段）
     * @return 模型实例响应 DTO
     */
    public static ModelInstanceResponse from(ModelInstance instance, ModelRepository modelRepository) {
        ModelInstanceResponse resp = new ModelInstanceResponse();
        resp.setId(instance.getId());
        resp.setChannelId(instance.getChannelId());
        resp.setModelId(instance.getModelId());
        resp.setUpstreamModelName(instance.getUpstreamModelName());
        resp.setPriority(instance.getPriority());
        resp.setWeight(instance.getWeight());
        resp.setState(instance.getState().name());

        modelRepository.findById(instance.getModelId()).ifPresent(spec -> {
            resp.setModelName(spec.getModelName());
            resp.setDisplayName(spec.getDisplayName());
            resp.setModelFamily(spec.getModelFamily());
        });

        return resp;
    }

    /**
     * 从模型实例实体列表转换
     *
     * @param instances       模型实例实体列表
     * @param modelRepository 模型仓储（查规格展示字段）
     * @return 模型实例响应 DTO 列表
     */
    public static List<ModelInstanceResponse> from(List<ModelInstance> instances, ModelRepository modelRepository) {
        return instances.stream()
                .map(i -> from(i, modelRepository))
                .toList();
    }
}
