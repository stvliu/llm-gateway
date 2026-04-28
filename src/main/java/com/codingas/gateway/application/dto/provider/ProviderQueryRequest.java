package com.codingas.gateway.adapter.admin.dto.provider;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.router.entity.Provider.ProviderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询提供商请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderQueryRequest extends PageRequest {

    private String keyword;

    private ProviderType providerType;

    private ProviderStatus status;
}
