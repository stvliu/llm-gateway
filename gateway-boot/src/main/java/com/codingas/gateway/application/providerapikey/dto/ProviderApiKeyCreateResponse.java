package com.codingas.gateway.application.providerapikey.dto;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import lombok.Data;

import java.time.Instant;

/**
 * Provider API Key 创建响应（一次性返回明文 API Key）
 */
@Data
/**
 * @deprecated 旧架构 DTO
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ProviderApiKeyCreateResponse {

    private Long id;
    private Long providerId;
    private String keyName;
    private String apiKey;
    private Integer priority;
    private Integer weight;
    private Boolean isDefault;
    private ProviderApiKeyState state;
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
        response.setState(key.getState());
        response.setCreatedAt(key.getCreatedAt());
        return response;
    }
}
