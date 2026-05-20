package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import com.codingas.gateway.domain.security.enums.UserState;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.team.entity.Team;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * 认证服务
 *
 * <p>处理 API Key 的认证和用户信息加载。</p>
 * <p>支持双路认证：优先新架构（UserApiKey），降级到旧架构（GatewayApiKey）。</p>
 */
@Slf4j
@Service
public class AuthenticationDomainService {

    private static final String CACHE_NAME = "auth";

    private final ApiKeyGateway apiKeyGateway;
    private final UserGateway userGateway;
    private final UserApiKeyGateway userApiKeyGateway;
    private final TeamGateway teamGateway;
    private final ProductGateway productGateway;
    private final ApiKeyEncryptionDomainService encryptionService;

    public AuthenticationDomainService(
            ApiKeyGateway apiKeyGateway,
            UserGateway userGateway,
            UserApiKeyGateway userApiKeyGateway,
            TeamGateway teamGateway,
            ProductGateway productGateway,
            ApiKeyEncryptionDomainService encryptionService) {
        this.apiKeyGateway = apiKeyGateway;
        this.userGateway = userGateway;
        this.userApiKeyGateway = userApiKeyGateway;
        this.teamGateway = teamGateway;
        this.productGateway = productGateway;
        this.encryptionService = encryptionService;
    }

    /**
     * 认证 API Key（双路认证）
     *
     * <p>优先使用新架构（UserApiKey），未找到时降级到旧架构（GatewayApiKey）。</p>
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

        // 1. 优先尝试新架构：UserApiKey
        UserAuthResult newResult = authenticateNewArchitecture(keyHash);
        if (newResult != null) {
            log.debug("Authenticated via new architecture: userApiKeyId={}", newResult.userApiKeyId());
            return newResult;
        }

        // 2. 降级到旧架构：GatewayApiKey
        UserAuthResult legacyResult = authenticateLegacy(keyHash);
        if (legacyResult != null) {
            log.debug("Authenticated via legacy architecture: apiKeyId={}", legacyResult.apiKeyId());
            return legacyResult;
        }

        log.debug("API Key not found in either architecture");
        return null;
    }

    /**
     * 新架构认证：通过 UserApiKey
     */
    private UserAuthResult authenticateNewArchitecture(String keyHash) {
        Optional<UserApiKey> userApiKeyOpt = userApiKeyGateway.findByKeyHash(keyHash);
        if (userApiKeyOpt.isEmpty()) {
            return null;
        }

        UserApiKey userApiKey = userApiKeyOpt.get();
        if (!isUserApiKeyActive(userApiKey)) {
            log.debug("UserApiKey is not active: state={}", userApiKey.getState());
            return null;
        }

        // 查找用户
        Long userId = userApiKey.getUserId();
        if (userId == null) {
            log.debug("Owner user ID not found for UserApiKey");
            return null;
        }

        User user = userGateway.findById(userId).orElse(null);
        if (user == null || !isUserActive(user)) {
            log.debug("User not found or not active for UserApiKey");
            return null;
        }

        // 验证 Team 状态
        Team team = teamGateway.findById(userApiKey.getTeamId()).orElse(null);
        if (team == null || !team.isAvailable()) {
            log.debug("Team not found or not available: teamId={}", userApiKey.getTeamId());
            return null;
        }

        // 验证 Product 状态
        Product product = productGateway.findById(userApiKey.getProductId()).orElse(null);
        if (product == null || !product.isAvailable()) {
            log.debug("Product not found or not available: productId={}", userApiKey.getProductId());
            return null;
        }

        return UserAuthResult.newArch(
            user.getId(),
            user.getRole(),
            userApiKey.getId(),
            userApiKey.getProductId(),
            userApiKey.getId(),
            userApiKey.getTeamId()
        );
    }

    /**
     * 旧架构认证：通过 GatewayApiKey
     */
    private UserAuthResult authenticateLegacy(String keyHash) {
        GatewayApiKey gatewayKey = apiKeyGateway.findByKeyHash(keyHash);

        if (gatewayKey == null) {
            return null;
        }

        if (!isKeyActive(gatewayKey)) {
            log.debug("GatewayApiKey is not active: state={}", gatewayKey.getState());
            return null;
        }

        if (isKeyExpired(gatewayKey)) {
            log.debug("GatewayApiKey is expired");
            return null;
        }

        Long userId = gatewayKey.getUserId();
        if (userId == null) {
            log.debug("User ID not found for GatewayApiKey");
            return null;
        }

        User user = userGateway.findById(userId).orElse(null);
        if (user == null || !isUserActive(user)) {
            log.debug("User not found or not active for GatewayApiKey");
            return null;
        }

        apiKeyGateway.updateLastUsed(gatewayKey.getId(), Instant.now());

        return UserAuthResult.legacy(user.getId(), user.getRole(), gatewayKey.getId());
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

    private boolean isUserApiKeyActive(UserApiKey key) {
        return key.getState() == UserApiKeyState.ACTIVE;
    }

    private boolean isUserActive(User user) {
        return user.getState() == UserState.ACTIVE;
    }

    /**
     * 使用 SHA-256 哈希 API Key
     */
    private String hashKey(String apiKey) {
        return encryptionService.hashKey(apiKey);
    }
}
