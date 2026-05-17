package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import com.codingas.gateway.domain.security.enums.UserState;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * 认证服务
 *
 * <p>处理 API Key 的认证和用户信息加载。</p>
 */
@Slf4j
@Service
public class AuthenticationDomainService {

    private static final String CACHE_NAME = "auth";

    private final ApiKeyGateway apiKeyGateway;
    private final UserGateway userGateway;
    private final ApiKeyEncryptionDomainService encryptionService;

    public AuthenticationDomainService(
            ApiKeyGateway apiKeyGateway,
            UserGateway userGateway,
            ApiKeyEncryptionDomainService encryptionService) {
        this.apiKeyGateway = apiKeyGateway;
        this.userGateway = userGateway;
        this.encryptionService = encryptionService;
    }

    /**
     * 认证 API Key
     *
     * @param apiKey API Key
     * @return 认证结果，不存在或无效返回 null
     */
    @Cacheable(value = CACHE_NAME, key = "'auth:' + #apiKey", unless = "#result == null")
    public UserAuthResult authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Empty API Key provided");
            return null;
        }

        String keyHash = hashKey(apiKey);
        GatewayApiKey gatewayKey = apiKeyGateway.findByKeyHash(keyHash);

        if (gatewayKey == null) {
            log.debug("API Key not found in database");
            return null;
        }

        if (!isKeyActive(gatewayKey)) {
            log.debug("API Key is not active: state={}", gatewayKey.getState());
            return null;
        }

        if (isKeyExpired(gatewayKey)) {
            log.debug("API Key is expired");
            return null;
        }

        Long userId = gatewayKey.getUserId();
        if (userId == null) {
            log.debug("User ID not found for API Key");
            return null;
        }

        User user = userGateway.findById(userId).orElse(null);
        if (user == null) {
            log.debug("User not found for API Key");
            return null;
        }

        if (!isUserActive(user)) {
            log.debug("User is not active: state={}", user.getState());
            return null;
        }

        apiKeyGateway.updateLastUsed(gatewayKey.getId(), Instant.now());

        return new UserAuthResult(
            user.getId(),
            null,  // role from UserRole entity, not directly on User
            gatewayKey.getId()
        );
    }

    /**
     * 获取用户
     */
    public Optional<User> getUserById(Long userId) {
        return userGateway.findById(userId);
    }

    private boolean isKeyActive(GatewayApiKey key) {
        return key.getState() == GatewayApiKeyState.ACTIVE;
    }

    private boolean isKeyExpired(GatewayApiKey key) {
        if (key.getExpiresAt() == null) {
            return false;
        }
        return Instant.now().isAfter(key.getExpiresAt());
    }

    private boolean isUserActive(User user) {
        return user.getState() == UserState.ACTIVE;
    }

    /**
     * 使用 SHA-256 哈希 API Key
     *
     * <p>委托给 {@link ApiKeyEncryptionDomainService} 保持与创建时一致的哈希算法。</p>
     *
     * @param apiKey 原始 API Key
     * @return 哈希后的字符串（十六进制格式）
     */
    private String hashKey(String apiKey) {
        return encryptionService.hashKey(apiKey);
    }
}
