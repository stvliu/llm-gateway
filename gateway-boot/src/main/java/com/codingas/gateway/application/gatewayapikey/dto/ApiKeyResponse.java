package com.codingas.gateway.application.gatewayapikey.dto;

import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * API Key 响应
 */
@Data
public class ApiKeyResponse {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String key;  // 脱敏后的 Key 标识
    private GatewayApiKeyState state;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private List<String> ipWhitelist;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 原始 API Key（仅在创建时返回一次，后续查询不返回）
     */
    private String rawKey;
}
