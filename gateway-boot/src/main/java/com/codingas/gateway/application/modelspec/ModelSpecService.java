package com.codingas.gateway.application.modelspec;

import com.codingas.gateway.application.modelspec.dto.ModelSpecCreateRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecQueryRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecResponse;
import com.codingas.gateway.application.modelspec.dto.ModelSpecUpdateRequest;

import java.util.List;

/**
 * 模型规格应用服务接口
 */
public interface ModelSpecService {

    /**
     * 创建模型规格
     */
    ModelSpecResponse create(ModelSpecCreateRequest request);

    /**
     * 根据 ID 查询模型规格
     */
    ModelSpecResponse getById(Long id);

    /**
     * 查询模型规格列表
     */
    List<ModelSpecResponse> query(ModelSpecQueryRequest request);

    /**
     * 更新模型规格
     */
    ModelSpecResponse update(Long id, ModelSpecUpdateRequest request);

    /**
     * 删除模型规格
     */
    void delete(Long id);

    /**
     * 启用/禁用模型规格
     */
    ModelSpecResponse setEnabled(Long id, boolean enabled);
}
