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

import com.codingas.gateway.provider.model.ModelInstance;

import java.util.List;
import java.util.Optional;

/**
 * 模型实例仓储接口
 */
public interface ModelInstanceGateway {

    /**
     * 保存模型实例
     */
    ModelInstance save(ModelInstance instance);

    /**
     * 根据ID查找
     */
    Optional<ModelInstance> findById(Long id);

    /**
     * 根据渠道ID查找所有模型实例
     */
    List<ModelInstance> findByChannelId(Long channelId);

    /**
     * 根据渠道ID查找活跃的模型实例
     */
    List<ModelInstance> findActiveByChannelId(Long channelId);

    /**
     * 根据模型规格ID查找活跃的模型实例（按优先级排序）
     */
    List<ModelInstance> findActiveByModelIdOrderByPriority(Long modelId);

    /**
     * 检查渠道模型关联是否已存在
     */
    boolean existsByChannelIdAndModelId(Long channelId, Long modelId);

    /**
     * 批量保存模型实例
     */
    List<ModelInstance> saveAll(List<ModelInstance> instances);

    /**
     * 删除模型实例
     */
    void deleteById(Long id);
}