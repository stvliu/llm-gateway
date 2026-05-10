package com.codingas.gateway.application.provider.dto;

import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyResponse;
import java.util.List;

/**
 * Provider Key 信息响应
 *
 * <p>包含默认 Key 和完整 Key 列表，用于 Provider 详情页展示。</p>
 */
public record ProviderKeysResponse(
    ProviderApiKeyResponse defaultKey,
    List<ProviderApiKeyResponse> keys
) {}