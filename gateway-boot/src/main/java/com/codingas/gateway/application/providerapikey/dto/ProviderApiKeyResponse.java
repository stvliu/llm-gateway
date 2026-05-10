package com.codingas.gateway.application.providerapikey.dto;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import lombok.Data;

import java.time.Instant;

/**
 * Provider API Key 响应（查询时使用，apiKey 已被 mask）
 */
@Data
public class ProviderApiKeyResponse {

    private Long id;
    private Long providerId;
    private String keyName;
    private String keyHint;
    private Integer priority;
    private Integer weight;
    private Boolean isDefault;
    private ProviderApiKeyState state;
    private String healthStatus;
    private Long successCount;
    private Long errorCount;
    private Integer rpmLimit;
    private Long tpmLimit;
    private Instant lastUsedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProviderApiKeyResponse from(ProviderApiKey key) {
        if (key == null) {
            return null;
        }
        ProviderApiKeyResponse response = new ProviderApiKeyResponse();
        response.setId(key.getId());
        response.setProviderId(key.getProviderId());
        response.setKeyName(key.getKeyName());
        response.setKeyHint(maskApiKey(key.getApiKey()));
        response.setPriority(key.getPriority());
        response.setWeight(key.getWeight());
        response.setIsDefault(key.getIsDefault());
        response.setState(key.getState());
        response.setRpmLimit(key.getRpmLimit());
        response.setTpmLimit(key.getTpmLimit());
        response.setLastUsedAt(key.getLastUsedAt());
        response.setCreatedAt(key.getCreatedAt());
        response.setUpdatedAt(key.getUpdatedAt());
        return response;
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
