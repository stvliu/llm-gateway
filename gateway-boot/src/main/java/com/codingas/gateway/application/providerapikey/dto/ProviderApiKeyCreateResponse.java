package com.codingas.gateway.application.providerapikey.dto;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.entity.ProviderApiKey.ProviderApiKeyDisabledReason;
import com.codingas.gateway.domain.model.entity.ProviderApiKey.ProviderApiKeyStatus;
import lombok.Data;

import java.time.Instant;

/**
 * Provider API Key 创建响应（一次性返回明文 API Key）
 */
@Data
public class ProviderApiKeyCreateResponse {

    private Long id;
    private Long providerId;
    private String keyName;
    private String apiKey;
    private Integer priority;
    private Integer weight;
    private Boolean isDefault;
    private ProviderApiKeyStatus status;
    private Instant expiresAt;
    private Instant createdAt;

    public static ProviderApiKeyCreateResponse from(ProviderApiKey key, String rawApiKey) {
        if (key == null) {
            return null;
        }
        ProviderApiKeyCreateResponse response = new ProviderApiKeyCreateResponse();
        response.setId(key.getId());
        response.setProviderId(key.getProviderId());
        response.setKeyName(key.getKeyName());
        response.setApiKey(rawApiKey);
        response.setPriority(key.getPriority());
        response.setWeight(key.getWeight());
        response.setIsDefault(key.getIsDefault());
        response.setStatus(key.getStatus());
        response.setExpiresAt(key.getExpiresAt());
        response.setCreatedAt(key.getCreatedAt());
        return response;
    }
}