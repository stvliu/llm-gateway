package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.common.enums.UserStatus;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String CACHE_NAME = "auth";

    private final ApiKeyGateway apiKeyGateway;
    private final UserGateway userGateway;

    /**
     * 认证 API Key
     *
     * @param apiKey API Key
     * @return 认证结果，不存在或无效返回 null
     */
    @Cacheable(value = CACHE_NAME, key = "'auth:' + #apiKey.hashCode()", unless = "#result == null")
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
            log.debug("API Key is not active: status={}", gatewayKey.getStatus());
            return null;
        }

        if (isKeyExpired(gatewayKey)) {
            log.debug("API Key is expired");
            return null;
        }

        User user = gatewayKey.getUser();
        if (user == null) {
            log.debug("User not found for API Key");
            return null;
        }

        if (!isUserActive(user)) {
            log.debug("User is not active: status={}", user.getStatus());
            return null;
        }

        apiKeyGateway.updateLastUsed(gatewayKey.getKeyCode(), Instant.now());

        return new UserAuthResult(
            user.getId(),
            user.getUserCode(),
            null,  // role from UserRole entity, not directly on User
            gatewayKey.getId(),
            gatewayKey.getKeyCode()
        );
    }

    /**
     * 获取用户
     */
    public Optional<User> getUserById(Long userId) {
        return userGateway.findById(userId);
    }

    private boolean isKeyActive(GatewayApiKey key) {
        return key.getStatus() == GatewayApiKey.ApiKeyStatus.ACTIVE;
    }

    private boolean isKeyExpired(GatewayApiKey key) {
        if (key.getExpiresAt() == null) {
            return false;
        }
        return Instant.now().isAfter(key.getExpiresAt());
    }

    private boolean isUserActive(User user) {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    private String hashKey(String apiKey) {
        // TODO: 使用 EncryptionService
        return String.valueOf(apiKey.hashCode());
    }
}
