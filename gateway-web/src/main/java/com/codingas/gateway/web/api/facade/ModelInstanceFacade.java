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
package com.codingas.gateway.web.api.facade;

import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceManager;
import com.codingas.gateway.web.api.dto.ModelInstanceCreateRequest;
import com.codingas.gateway.web.api.dto.ModelInstanceResponse;
import com.codingas.gateway.web.api.dto.ModelInstanceUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型实例应用门面（管理服务层）
 *
 * <p>位于 Controller 与领域 Service 之间：负责组装对象（DTO 纯映射 + 模型规格展示数据）与跨域访问（协调核心服务），
 * 只依赖核心 Service，不访问持久化仓储。</p>
 */
@Component
@RequiredArgsConstructor
public class ModelInstanceFacade {

    private final ModelInstanceManager modelInstanceManager;

    /**
     * 查询指定渠道下的所有模型实例
     *
     * @param channelId 渠道 ID
     * @return 模型实例响应 DTO 列表
     */
    public List<ModelInstanceResponse> list(Long channelId) {
        return modelInstanceManager.getInstancesByChannelId(channelId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 创建模型实例
     *
     * @param channelId 渠道 ID
     * @param request   创建请求 DTO
     * @return 模型实例响应 DTO
     */
    public ModelInstanceResponse create(Long channelId, ModelInstanceCreateRequest request) {
        request.setChannelId(channelId);
        return toResponse(modelInstanceManager.create(request.toEntity()));
    }

    /**
     * 更新模型实例
     *
     * @param channelId 渠道 ID
     * @param id        模型实例 ID
     * @param request   更新请求 DTO
     * @return 模型实例响应 DTO
     */
    public ModelInstanceResponse update(Long channelId, Long id, ModelInstanceUpdateRequest request) {
        return toResponse(modelInstanceManager.update(channelId, id, request.toEntity()));
    }

    /**
     * 组装模型实例响应 DTO（纯映射 + 经核心 Service 获取的模型规格展示字段）
     */
    private ModelInstanceResponse toResponse(ModelInstance instance) {
        ModelInstanceResponse response = ModelInstanceResponse.from(instance);
        Model model = modelInstanceManager.getModel(instance.getModelId());
        if (model != null) {
            response.setModelName(model.getModelName());
            response.setDisplayName(model.getDisplayName());
            response.setModelFamily(model.getModelFamily());
        }
        return response;
    }
}
