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
 * 模型应用服务接口
 *
 * <p>处理模型管理的业务逻辑。</p>
 */
public interface ModelService {

    /**
     * 创建模型
     */
    ModelResponse create(ModelCreateRequest request);

    /**
     * 根据 ID 获取模型
     */
    ModelResponse getById(Long id);

    /**
     * 查询模型列表
     */
    PageResponse<ModelResponse> query(ModelQueryRequest request);

    /**
     * 更新模型
     */
    ModelResponse update(Long id, ModelUpdateRequest request);

    /**
     * 删除模型（软删除）
     */
    void delete(Long id);

    /**
     * 启用/禁用模型
     */
    ModelResponse setEnabled(Long id, boolean enabled);
}