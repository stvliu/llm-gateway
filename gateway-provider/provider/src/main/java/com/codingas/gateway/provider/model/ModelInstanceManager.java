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

import java.util.List;

/**
 * 模型实例应用服务接口
 *
 * <p>出入参采用领域实体与轻量用例对象；模型规格展示字段（modelName 等）的组装
 * 由 web 层组装器（Assembler）负责。</p>
 */
public interface ModelInstanceManager {

    /**
     * 查询指定渠道下的所有模型实例
     *
     * @param channelId 渠道 ID
     * @return 模型实例实体列表
     */
    List<ModelInstance> getInstancesByChannelId(Long channelId);

    /**
     * 创建模型实例
     *
     * @param command 创建用例入参
     * @return 创建后的模型实例实体
     */
    ModelInstance create(ModelInstanceCreateCommand command);

    /**
     * 删除模型实例
     *
     * @param channelId 渠道 ID
     * @param id        模型实例 ID
     */
    void delete(Long channelId, Long id);

    /**
     * 切换模型实例状态
     *
     * <p>由后端校验 canTransitionTo()。</p>
     *
     * @param channelId 渠道 ID
     * @param id        模型实例 ID
     * @param command   状态切换用例入参
     */
    void setEnabled(Long channelId, Long id, ModelInstanceStateCommand command);

    /**
     * 更新模型实例的上游模型名
     *
     * @param channelId        渠道 ID
     * @param id               模型实例 ID
     * @param upstreamModelName 新的上游模型名，null 表示走默认（= Model.modelName）
     */
    void updateUpstreamModelName(Long channelId, Long id, String upstreamModelName);

    /**
     * 更新模型实例（支持修改 modelId 和 upstreamModelName）
     *
     * <p>字段为 null 表示不更新该字段。</p>
     *
     * @param channelId 渠道 ID
     * @param id        模型实例 ID
     * @param command   更新用例入参
     * @return 更新后的模型实例实体
     */
    ModelInstance update(Long channelId, Long id, ModelInstanceUpdateCommand command);

    /**
     * 按 ID 获取模型规格（供展示组装：模型实例响应需 modelName 等展示字段）
     *
     * @param modelId 模型 ID
     * @return 模型实体（不存在时为 null）
     */
    Model getModel(Long modelId);
}
