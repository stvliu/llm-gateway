package com.codingas.gateway.application.apikey;

import com.codingas.gateway.application.apikey.dto.ApiKeyCreateRequest;
import com.codingas.gateway.application.apikey.dto.ApiKeyQueryRequest;
import com.codingas.gateway.application.apikey.dto.ApiKeyUpdateRequest;
import com.codingas.gateway.application.apikey.dto.ApiKeyResponse;
import com.codingas.gateway.common.dto.PageResponse;

/**
 * API Key 应用服务接口
 *
 * <p>处理 API Key 管理的业务逻辑。</p>
 */
public interface ApiKeyService {

    /**
     * 创建 API Key
     */
    ApiKeyResponse create(ApiKeyCreateRequest request);

    /**
     * 根据 ID 获取 API Key
     */
    ApiKeyResponse getById(Long id);

    /**
     * 查询 API Key 列表
     */
    PageResponse<ApiKeyResponse> query(ApiKeyQueryRequest request);

    /**
     * 更新 API Key
     */
    ApiKeyResponse update(Long id, ApiKeyUpdateRequest request);

    /**
     * 删除 API Key（软删除）
     */
    void delete(Long id);

    /**
     * 启用/禁用 API Key
     */
    ApiKeyResponse setEnabled(Long id, boolean enabled);
}
