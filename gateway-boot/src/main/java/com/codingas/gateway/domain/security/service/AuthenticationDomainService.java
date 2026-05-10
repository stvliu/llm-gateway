package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import com.codingas.gateway.domain.security.enums.UserStatus;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
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
    private static final String HASH_ALGORITHM = "SHA-256";

    private final ApiKeyGateway apiKeyGateway;
    private final UserGateway userGateway;

    @Value("${gateway.security.api-key.salt:default-salt-change-in-production}")
    private String hashSalt;

    public AuthenticationDomainService(ApiKeyGateway apiKeyGateway, UserGateway userGateway) {
        this.apiKeyGateway = apiKeyGateway;
        this.userGateway = userGateway;
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
            log.debug("User is not active: status={}", user.getStatus());
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
        return user.getStatus() == UserStatus.ACTIVE;
    }

    /**
     * 使用 SHA-256 哈希 API Key
     *
     * <p>配合配置的 salt，提供安全的 API Key 哈希存储。</p>
     *
     * @param apiKey 原始 API Key
     * @return 哈希后的字符串（十六进制格式）
     */
    private String hashKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            // 组合 salt 和 apiKey
            String saltedKey = hashSalt + apiKey;
            byte[] hashBytes = digest.digest(saltedKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 Java 标准实现，不应发生
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
