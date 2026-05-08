package com.codingas.gateway.application.providerapikey;

import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyCreateRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyCreateResponse;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyQueryRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyResponse;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;

/**
 * Provider API Key 应用服务接口
 */
public interface ProviderApiKeyService {

    ProviderApiKeyCreateResponse create(ProviderApiKeyCreateRequest request);

    ProviderApiKeyResponse getById(Long id);

    PageResponse<ProviderApiKeyResponse> query(ProviderApiKeyQueryRequest request);

    ProviderApiKeyResponse update(Long id, ProviderApiKeyUpdateRequest request);

    void delete(Long id);

    ProviderApiKeyResponse setEnabled(Long id, boolean enabled);
}