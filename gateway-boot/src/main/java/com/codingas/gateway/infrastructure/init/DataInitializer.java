package com.codingas.gateway.infrastructure.init;

import com.codingas.gateway.common.enums.UserStatus;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 开发环境数据初始化器
 *
 * <p>在应用启动时初始化测试数据（仅 local/dev 环境）。</p>
 * <p>初始化内容：</p>
 * <ul>
 *   <li>测试用户</li>
 *   <li>网关 API Key</li>
 * </ul>
 *
 * <p>注意：Provider、ProviderApiKey、Model 数据已通过 Flyway 迁移脚本初始化。</p>
 */
@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserGateway userGateway;
    private final ApiKeyGateway apiKeyGateway;
    private final PasswordEncoder passwordEncoder;

    /** 测试用的 Gateway API Key */
    private static final String TEST_API_KEY = "sk-test-volcengine-key-001";

    @Override
    public void run(String... args) {
        log.info("Starting data initialization for local/dev environment...");

        try {
            initTestUser();

            log.info("Data initialization completed successfully");
        } catch (Exception e) {
            log.error("Data initialization failed", e);
        }
    }

    /**
     * 初始化测试用户
     */
    private void initTestUser() {
        // 检查是否已存在
        if (userGateway.existsByUsername("testuser")) {
            log.info("Test user already exists, skipping user initialization");
            return;
        }

        // 创建测试用户
        User user = new User();
        user.setUserCode("USR001");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole("USER");
        user.setEmailVerified(true);

        User savedUser = userGateway.save(user);
        log.info("Created test user: {} (id={})", user.getUsername(), savedUser.getId());

        // 创建 Gateway API Key
        initGatewayApiKey(savedUser);
    }

    /**
     * 初始化网关 API Key
     */
    private void initGatewayApiKey(User user) {
        // 检查是否已存在
        if (apiKeyGateway.existsByKeyCode(TEST_API_KEY)) {
            log.info("Test API key already exists, skipping");
            return;
        }

        GatewayApiKey apiKey = new GatewayApiKey();
        apiKey.setKeyCode(TEST_API_KEY);
        // 使用与 AuthenticationDomainService 相同的 hash 方法
        apiKey.setKeyHash(String.valueOf(TEST_API_KEY.hashCode()));
        apiKey.setUser(user);
        apiKey.setName("测试用 API Key");
        apiKey.setStatus(GatewayApiKey.ApiKeyStatus.ACTIVE);
        apiKey.setExpiresAt(Instant.now().plusSeconds(365 * 24 * 60 * 60)); // 1 年有效期

        apiKeyGateway.save(apiKey);
        log.info("Created test API key: {}", TEST_API_KEY);
    }
}
