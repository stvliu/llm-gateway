/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelCreateRequest;
import com.codingas.gateway.application.model.dto.ModelQueryRequest;
import com.codingas.gateway.application.model.dto.ModelResponse;
import com.codingas.gateway.application.model.dto.ModelUpdateRequest;
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