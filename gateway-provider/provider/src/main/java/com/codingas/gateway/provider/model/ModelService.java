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

import com.codingas.gateway.common.dto.PageResponse;

/**
 * 模型管理服务接口
 *
 * <p>出入参采用实体与轻量用例对象，HTTP 契约（Request/Response DTO）由 web 层负责转换。</p>
 */
public interface ModelService {

    /**
     * 创建模型
     *
     * @param model 模型实体（承载 modelName/displayName/capabilities 等）
     * @return 创建后的模型实体
     */
    Model create(Model model);

    /**
     * 复制模型规格生成新模型
     *
     * <p>基于源模型全量规格复制，按 {@code override} 覆盖 modelName（必填）与
     * displayName/modelFamily；新模型重置为人工来源（MANUAL）、externalId 清空、
     * 锁定字段清空、生命周期字段清空（可用状态）。不复制挂载。</p>
     *
     * @param sourceId 源模型 ID
     * @param override 覆盖字段（modelName 必填非 null）
     * @return 新模型
     */
    Model copy(Long sourceId, Model override);

    /**
     * 根据 ID 获取模型
     *
     * @param id 模型 ID
     * @return 模型实体
     */
    Model getById(Long id);

    /**
     * 查询模型列表
     *
     * @param query 查询条件
     * @return 模型实体分页
     */
    PageResponse<Model> query(ModelQuery query);

    /**
     * 更新模型（实体 null 字段表示不更新）
     *
     * @param id    模型 ID
     * @param model 模型实体
     * @return 更新后的模型实体
     */
    Model update(Long id, Model model);

    /**
     * 清除模型字段人工锁定（清空 lockedFields，恢复同步覆盖权限）
     *
     * @param id 模型 ID
     * @return 清除锁定后的模型实体
     */
    Model unlockFields(Long id);

    /**
     * 删除模型（软删除）
     *
     * @param id 模型 ID
     */
    void delete(Long id);

    /**
     * 启用/禁用模型
     *
     * @param id      模型 ID
     * @param enabled 是否启用
     * @return 更新后的模型实体
     */
    Model setEnabled(Long id, boolean enabled);
}
