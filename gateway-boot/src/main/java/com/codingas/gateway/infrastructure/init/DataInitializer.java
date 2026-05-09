package com.codingas.gateway.infrastructure.init;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.common.enums.UserStatus;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 开发环境数据初始化器
 *
 * <p>在应用启动时初始化测试数据（仅 local/dev 环境）。</p>
 * <p>初始化内容：</p>
 * <ul>
 *   <li>Provider（火山引擎）</li>
 *   <li>Provider API Key</li>
 *   <li>模型数据（豆包系列）</li>
 *   <li>管理员用户：admin/admin (ADMIN 角色)</li>
 *   <li>测试用户：test/test (USER 角色)</li>
 *   <li>网关 API Key</li>
 * </ul>
 */
@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserGateway userGateway;
    private final ApiKeyGateway apiKeyGateway;
    private final PasswordEncoder passwordEncoder;
    private final ProviderGateway providerGateway;
    private final ProviderApiKeyGateway providerApiKeyGateway;
    private final ModelGateway modelGateway;

    /** 测试用的 Gateway API Key */
    private static final String TEST_API_KEY = "sk-test-volcengine-key-001";

    @Override
    public void run(String... args) {
        log.info("Starting data initialization for local/dev environment...");

        try {
            // 初始化 Provider 和 Model 数据
            Provider provider = initProvider();
            if (provider != null) {
                initProviderApiKey(provider);
                initModels(provider);
            }

            // 初始化用户数据
            initAdminUser();
            initTestUser();

            log.info("Data initialization completed successfully");
        } catch (Exception e) {
            log.error("Data initialization failed", e);
        }
    }

    /**
     * 初始化 Provider（火山引擎）
     */
    private Provider initProvider() {
        // 检查是否已存在 Provider
        if (providerGateway.count() > 0) {
            log.info("Provider already exists, skipping");
            return providerGateway.findAll().get(0);
        }

        Provider provider = new Provider();
        provider.setName("火山引擎");
        provider.setType(ProviderType.VOLCENGINE);
        provider.setBaseUrl("https://ark.cn-beijing.volces.com/api/v3");
        provider.setWebsiteUrl("https://www.volcengine.com");
        provider.setApiDocUrl("https://www.volcengine.com/docs/82379/1298454");
        provider.setPriority(100);
        provider.setEnabled(true);

        Provider savedProvider = providerGateway.save(provider);
        log.info("Created provider: {} (id={})", provider.getName(), savedProvider.getId());
        return savedProvider;
    }

    /**
     * 初始化 Provider API Key
     */
    private void initProviderApiKey(Provider provider) {
        // 检查是否已存在
        if (!providerApiKeyGateway.findByProviderId(provider.getId()).isEmpty()) {
            log.info("Provider API key already exists, skipping");
            return;
        }

        ProviderApiKey apiKey = new ProviderApiKey();
        apiKey.setProviderId(provider.getId());
        apiKey.setKeyName("火山引擎主密钥");
        apiKey.setApiKey("1fb8bdcf-3383-426d-9f3d-4c2979895c58");
        apiKey.setPriority(100);
        apiKey.setStatus(ProviderApiKey.ProviderApiKeyStatus.ACTIVE);

        providerApiKeyGateway.save(apiKey);
        log.info("Created provider API key: {}", apiKey.getKeyName());
    }

    /**
     * 初始化模型数据
     */
    private void initModels(Provider provider) {
        // 检查是否已存在模型
        if (modelGateway.count() > 0) {
            log.info("Models already exist, skipping");
            return;
        }

        // 豆包 Pro 32K
        createModel(provider, "doubao-pro-32k", "豆包 Pro 32K",
                32768, new BigDecimal("0.0008"), new BigDecimal("0.002"));

        // 豆包 Pro 128K
        createModel(provider, "doubao-pro-128k", "豆包 Pro 128K",
                131072, new BigDecimal("0.005"), new BigDecimal("0.009"));

        // 豆包 Lite 32K
        createModel(provider, "doubao-lite-32k", "豆包 Lite 32K",
                32768, new BigDecimal("0.0003"), new BigDecimal("0.0006"));

        // 豆包 Seed 2.0 Pro
        createModel(provider, "doubao-seed-2-0-pro-260215", "豆包 Seed 2.0 Pro",
                128000, new BigDecimal("0.001"), new BigDecimal("0.002"));

        // 豆包 Seed 2.0 Code Preview
        createModel(provider, "doubao-seed-2-0-code-preview-260215", "豆包 Seed 2.0 Code Preview",
                128000, new BigDecimal("0.001"), new BigDecimal("0.002"));

        // 豆包 Seed 2.0 Mini
        createModel(provider, "doubao-seed-2-0-mini-260215", "豆包 Seed 2.0 Mini",
                128000, new BigDecimal("0.0005"), new BigDecimal("0.001"));

        log.info("Created {} models for provider: {}", modelGateway.count(), provider.getName());
    }

    /**
     * 创建模型
     */
    private void createModel(Provider provider, String providerModelId, String displayName,
                             int contextWindow, BigDecimal inputPrice, BigDecimal outputPrice) {
        Model model = new Model();
        model.setProviderId(provider.getId());
        model.setProviderName(provider.getName());
        model.setProviderModelId(providerModelId);
        model.setDisplayName(displayName);
        model.setContextWindow(contextWindow);
        model.setInputPrice(inputPrice);
        model.setOutputPrice(outputPrice);
        model.setCapabilities(Map.of(
                "chat", true,
                "streaming", true,
                "function_calling", true
        ));
        model.setEnabled(true);

        modelGateway.save(model);
        log.debug("Created model: {}", displayName);
    }

    /**
     * 初始化管理员用户
     */
    private void initAdminUser() {
        // 检查是否已存在
        if (userGateway.existsByUsername("admin")) {
            log.info("Admin user already exists, skipping");
            return;
        }

        // 创建管理员用户
        User user = new User();
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setPasswordHash(passwordEncoder.encode("admin"));
        user.setStatus(UserStatus.ENABLED);
        user.setRole("ADMIN");
        user.setEmailVerified(true);

        User savedUser = userGateway.save(user);
        log.info("Created admin user: {} (id={})", user.getUsername(), savedUser.getId());
    }

    /**
     * 初始化测试用户
     */
    private void initTestUser() {
        // 检查是否已存在
        if (userGateway.existsByUsername("test")) {
            log.info("Test user already exists, skipping user initialization");
            return;
        }

        // 创建测试用户
        User user = new User();
        user.setUsername("test");
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("test"));
        user.setStatus(UserStatus.ENABLED);
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
        if (apiKeyGateway.count() > 0) {
            log.info("Test API key already exists, skipping");
            return;
        }

        GatewayApiKey apiKey = new GatewayApiKey();
        // 使用与 AuthenticationDomainService 相同的 hash 方法
        apiKey.setKeyHash(String.valueOf(TEST_API_KEY.hashCode()));
        apiKey.setUserId(user.getId());
        apiKey.setUsername(user.getUsername());
        apiKey.setName("测试用 API Key");
        apiKey.setStatus(GatewayApiKey.ApiKeyStatus.ACTIVE);
        apiKey.setExpiresAt(Instant.now().plusSeconds(365 * 24 * 60 * 60)); // 1 年有效期

        apiKeyGateway.save(apiKey);
        log.info("Created test API key: {}", TEST_API_KEY);
    }
}
