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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 开发环境数据初始化器
 *
 * <p>在应用启动时初始化测试数据（仅 local/dev 环境）。</p>
 * <p>初始化内容：</p>
 * <ul>
 *   <li>测试用户</li>
 *   <li>网关 API Key</li>
 *   <li>火山引擎 Provider</li>
 *   <li>豆包模型</li>
 * </ul>
 */
@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;
    private final ProviderApiKeyGateway providerApiKeyGateway;
    private final UserGateway userGateway;
    private final ApiKeyGateway apiKeyGateway;
    private final PasswordEncoder passwordEncoder;

    @Value("${gateway.llm.volcengine.api-key:}")
    private String volcengineApiKey;

    /** 测试用的 Gateway API Key */
    private static final String TEST_API_KEY = "sk-test-volcengine-key-001";

    @Override
    public void run(String... args) {
        log.info("Starting data initialization for local/dev environment...");

        try {
            initTestUser();
            initVolcengineProvider();

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

    /**
     * 初始化火山引擎 Provider
     */
    private void initVolcengineProvider() {
        // 检查是否已存在
        if (providerGateway.existsByProviderCode("volcengine")) {
            log.info("Volcengine provider already exists, skipping provider initialization");
            return;
        }

        // 创建 Provider
        Provider provider = new Provider();
        provider.setProviderCode("volcengine");
        provider.setProviderName("火山引擎");
        provider.setProviderType(ProviderType.VOLCENGINE);
        provider.setBaseUrl("https://ark.cn-beijing.volces.com/api/v3");
        provider.setWebsiteUrl("https://www.volcengine.com");
        provider.setApiDocUrl("https://www.volcengine.com/docs/82379/1298454");
        provider.setPriority(100);
        provider.setStatus(Provider.ProviderStatus.ACTIVE);

        Provider savedProvider = providerGateway.save(provider);
        log.info("Created provider: {} (id={})", provider.getProviderName(), savedProvider.getId());

        // 创建 Provider API Key
        if (volcengineApiKey != null && !volcengineApiKey.isBlank()) {
            initProviderApiKey(savedProvider);
        } else {
            log.warn("Volcengine API key not configured, skipping ProviderApiKey initialization");
        }

        // 创建模型
        initVolcengineModels(savedProvider);
    }

    /**
     * 初始化火山引擎 API Key
     */
    private void initProviderApiKey(Provider provider) {
        ProviderApiKey apiKey = new ProviderApiKey();
        apiKey.setKeyCode("VOLCENGINE_KEY_001");
        apiKey.setProviderId(provider.getId());
        apiKey.setKeyName("火山引擎主密钥");
        apiKey.setApiKey(volcengineApiKey);
        apiKey.setPriority(100);
        apiKey.setStatus(ProviderApiKey.ProviderApiKeyStatus.ACTIVE);

        providerApiKeyGateway.save(apiKey);
        log.info("Created Provider API key for Volcengine");
    }

    /**
     * 初始化火山引擎模型
     */
    private void initVolcengineModels(Provider provider) {
        // 豆包 Pro 32K
        createModel(
            provider,
            "doubao-pro-32k",
            "doubao-pro-32k",
            "豆包 Pro 32K",
            32768,
            new BigDecimal("0.0008"),
            new BigDecimal("0.002")
        );

        // 豆包 Pro 128K
        createModel(
            provider,
            "doubao-pro-128k",
            "doubao-pro-128k",
            "豆包 Pro 128K",
            131072,
            new BigDecimal("0.005"),
            new BigDecimal("0.009")
        );

        // 豆包 Lite 32K
        createModel(
            provider,
            "doubao-lite-32k",
            "doubao-lite-32k",
            "豆包 Lite 32K",
            32768,
            new BigDecimal("0.0003"),
            new BigDecimal("0.0006")
        );

        // 豆包 Seed 2.0 Pro (用户指定的模型)
        createModel(
            provider,
            "doubao-seed-2-0-pro-260215",
            "doubao-seed-2-0-pro-260215",
            "豆包 Seed 2.0 Pro",
            128000,
            new BigDecimal("0.001"),
            new BigDecimal("0.002")
        );
    }

    /**
     * 创建模型
     */
    private void createModel(
            Provider provider,
            String modelCode,
            String providerModelId,
            String displayName,
            int contextWindow,
            BigDecimal inputPrice,
            BigDecimal outputPrice) {

        // 检查是否已存在
        if (modelGateway.existsByModelCode(modelCode)) {
            log.debug("Model {} already exists, skipping", modelCode);
            return;
        }

        Model model = new Model();
        model.setModelCode(modelCode);
        model.setProvider(provider);
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
        model.setStatus(Model.ModelStatus.ACTIVE);

        modelGateway.save(model);
        log.info("Created model: {} (code={})", displayName, modelCode);
    }
}
