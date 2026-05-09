package com.codingas.gateway.application.provider;

import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderKeysResponse;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;

/**
 * 提供商应用服务接口
 *
 * <p>处理提供商管理的业务逻辑。</p>
 */
public interface ProviderService {

    /**
     * 创建提供商
     */
    ProviderResponse create(ProviderCreateRequest request);

    /**
     * 根据 ID 获取提供商
     */
    ProviderResponse getById(Long id);

    /**
     * 查询提供商列表
     */
    PageResponse<ProviderResponse> query(ProviderQueryRequest request);

    /**
     * 更新提供商
     */
    ProviderResponse update(Long id, ProviderUpdateRequest request);

    /**
     * 删除提供商（软删除）
     */
    void delete(Long id);

    /**
     * 启用/禁用提供商
     */
    ProviderResponse setEnabled(Long id, boolean enabled);

    /**
     * 获取 Provider 的 Key 信息（默认 Key + 列表）
     */
    ProviderKeysResponse getProviderKeys(Long providerId);
}