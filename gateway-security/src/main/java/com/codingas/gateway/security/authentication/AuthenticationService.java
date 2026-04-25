package com.codingas.gateway.security.authentication;

import com.codingas.gateway.core.domain.entity.GatewayApiKey;
import com.codingas.gateway.core.domain.entity.User;
import com.codingas.gateway.core.repository.GatewayApiKeyRepository;
import com.codingas.gateway.core.repository.UserRepository;
import com.codingas.gateway.security.encryption.ApiKeyEncryptionService;
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
 * <p>通过 Spring Cache 抽象，开发环境使用本地缓存(Caffeine)，生产环境使用 Redis。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String CACHE_NAME = "auth";

    private final GatewayApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final ApiKeyEncryptionService encryptionService;

    /**
     * 认证 API Key
     *
     * <p>使用 Spring Cache 缓存结果，默认 5 分钟 TTL。</p>
     *
     * @param apiKey API Key (格式: sk-xK9mP2vL8nQ4wF7hJ3dR6tB0yC5sE8gU)
     * @return 认证用户信息，如果认证失败返回 null
     */
    @Cacheable(value = CACHE_NAME, key = "'auth:' + T(com.codingas.gateway.security.encryption.ApiKeyEncryptionService).hashKey(#apiKey)", unless = "#result == null")
    public UserAuthResult authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Empty API Key provided");
            return null;
        }

        // 计算哈希并查询数据库
        String keyHash = encryptionService.hashKey(apiKey);
        Optional<GatewayApiKey> optKey = apiKeyRepository.findByKeyHash(keyHash);

        if (optKey.isEmpty()) {
            log.debug("API Key not found in database");
            return null;
        }

        GatewayApiKey gatewayKey = optKey.get();

        // 验证密钥状态
        if (!isKeyActive(gatewayKey)) {
            log.debug("API Key is not active: status={}", gatewayKey.getStatus());
            return null;
        }

        // 验证过期时间
        if (isKeyExpired(gatewayKey)) {
            log.debug("API Key is expired");
            return null;
        }

        // 获取用户信息
        Optional<User> optUser = userRepository.findById(gatewayKey.getUserId());
        if (optUser.isEmpty()) {
            log.debug("User not found for API Key: userId={}", gatewayKey.getUserId());
            return null;
        }

        User user = optUser.get();

        // 验证用户状态
        if (!isUserActive(user)) {
            log.debug("User is not active: status={}", user.getStatus());
            return null;
        }

        // 构建认证结果
        UserAuthResult result = new UserAuthResult(
            user.getId(),
            user.getUserCode(),
            user.getRole(),
            gatewayKey.getId(),
            gatewayKey.getKeyCode()
        );

        log.info("API Key authenticated successfully: userId={}, keyCode={}",
            user.getId(), gatewayKey.getKeyCode());

        return result;
    }

    /**
     * 验证 API Key 是否有效
     *
     * @param apiKey API Key
     * @return true 如果有效
     */
    public boolean isValidApiKey(String apiKey) {
        return authenticate(apiKey) != null;
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 根据用户code获取用户信息
     *
     * @param userCode 用户编码
     * @return 用户信息
     */
    public Optional<User> getUserByCode(String userCode) {
        return userRepository.findByUserCode(userCode);
    }

    /**
     * 检查 Key 是否处于活跃状态
     */
    private boolean isKeyActive(GatewayApiKey key) {
        return key.getStatus() == GatewayApiKey.GatewayApiKeyStatus.ACTIVE;
    }

    /**
     * 检查 Key 是否已过期
     */
    private boolean isKeyExpired(GatewayApiKey key) {
        if (key.getExpiresAt() == null) {
            return false;
        }
        return Instant.now().isAfter(key.getExpiresAt());
    }

    /**
     * 检查用户是否处于活跃状态
     */
    private boolean isUserActive(User user) {
        return user.getStatus() == User.UserStatus.ACTIVE;
    }
}
