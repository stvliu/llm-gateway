package com.codingas.gateway.infrastructure.init;

import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.security.enums.UserState;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.team.entity.Team;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.TeamState;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 开发环境数据初始化器
 *
 * <p>在应用启动时初始化测试数据（仅 local/dev 环境）。</p>
 * <p>初始化内容：</p>
 * <ul>
 *   <li>Provider（火山引擎）+ Provider API Key + 模型</li>
 *   <li>产品（按量计费）</li>
 *   <li>4 个团队：默认、产品、开发、龙虾</li>
 *   <li>1 个管理员 + 6 个测试用户</li>
 *   <li>10 个 UserApiKey，分布在不同团队下</li>
 * </ul>
 */
@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;
    private final ProviderGateway providerGateway;
    private final ProviderApiKeyGateway providerApiKeyGateway;
    private final ModelGateway modelGateway;
    private final ProductGateway productGateway;
    private final TeamGateway teamGateway;
    private final UserApiKeyGateway userApiKeyGateway;

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

            // 初始化产品
            Product product = initProduct(provider);

            // 初始化团队（4 个）
            Team defaultTeam = initTeam("默认团队", "开发环境默认团队");
            Team productTeam = initTeam("产品团队", "产品部门团队");
            Team devTeam = initTeam("开发团队", "研发部门团队");
            Team lobsterTeam = initTeam("龙虾团队", "龙虾部门团队");

            // 初始化用户（1 管理员 + 6 测试用户）
            initAdminUser();
            User testUser1 = initUser("test1", "test1@example.com", "test1");
            User testUser2 = initUser("test2", "test2@example.com", "test2");
            User testUser3 = initUser("test3", "test3@example.com", "test3");
            User testUser4 = initUser("test4", "test4@example.com", "test4");
            User testUser5 = initUser("test5", "test5@example.com", "test5");
            User testUser6 = initUser("test6", "test6@example.com", "test6");

            // 初始化 10 个 UserApiKey，分布在不同团队
            if (product != null) {
                // test1: 默认团队 2 个 Key
                initUserApiKey(testUser1, defaultTeam, product, "sk-test1-default-001", "test1 默认团队 Key 1");
                initUserApiKey(testUser1, defaultTeam, product, "sk-test1-default-002", "test1 默认团队 Key 2");

                // test1: 开发团队 1 个 Key（同一用户跨团队）
                initUserApiKey(testUser1, devTeam, product, "sk-test1-dev-001", "test1 开发团队 Key");

                // test2: 产品团队 2 个 Key
                initUserApiKey(testUser2, productTeam, product, "sk-test2-product-001", "test2 产品团队 Key 1");
                initUserApiKey(testUser2, productTeam, product, "sk-test2-product-002", "test2 产品团队 Key 2");

                // test3: 龙虾团队 2 个 Key
                initUserApiKey(testUser3, lobsterTeam, product, "sk-test3-lobster-001", "test3 龙虾团队 Key 1");
                initUserApiKey(testUser3, lobsterTeam, product, "sk-test3-lobster-002", "test3 龙虾团队 Key 2");

                // test4: 默认团队 1 个 Key
                initUserApiKey(testUser4, defaultTeam, product, "sk-test4-default-001", "test4 默认团队 Key");

                // test5: 开发团队 1 个 Key
                initUserApiKey(testUser5, devTeam, product, "sk-test5-dev-001", "test5 开发团队 Key");

                // test6: 龙虾团队 1 个 Key（跨团队共享龙虾团队）
                initUserApiKey(testUser6, lobsterTeam, product, "sk-test6-lobster-001", "test6 龙虾团队 Key");
            }

            log.info("Data initialization completed successfully");
        } catch (Exception e) {
            log.error("Data initialization failed", e);
        }
    }

    /**
     * 初始化 Provider（火山引擎）
     */
    private Provider initProvider() {
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
        provider.setState(ProviderState.ACTIVE);

        Provider saved = providerGateway.save(provider);
        log.info("Created provider: {} (id={})", provider.getName(), saved.getId());
        return saved;
    }

    /**
     * 初始化 Provider API Key
     */
    private void initProviderApiKey(Provider provider) {
        if (!providerApiKeyGateway.findByProviderId(provider.getId()).isEmpty()) {
            log.info("Provider API key already exists, skipping");
            return;
        }

        ProviderApiKey apiKey = new ProviderApiKey();
        apiKey.setProviderId(provider.getId());
        apiKey.setKeyName("火山引擎主密钥");
        apiKey.setApiKey("1fb8bdcf-3383-426d-9f3d-4c2979895c58");
        apiKey.setPriority(100);
        apiKey.setState(ProviderApiKeyState.ACTIVE);

        providerApiKeyGateway.save(apiKey);
        log.info("Created provider API key: {}", apiKey.getKeyName());
    }

    /**
     * 初始化模型数据
     */
    private void initModels(Provider provider) {
        if (modelGateway.count() > 0) {
            log.info("Models already exist, skipping");
            return;
        }

        createModel(provider, "doubao-seed-2-0-lite-260428", "Doubao-Seed-2.0-lite",
                32768, new BigDecimal("0.0008"), new BigDecimal("0.002"));

        createModel(provider, "doubao-seed-2-0-mini-260428", "Doubao-Seed-2.0-mini",
                131072, new BigDecimal("0.005"), new BigDecimal("0.009"));

        createModel(provider, "deepseek-v3-2-251201", "DeepSeek-V3.2",
                32768, new BigDecimal("0.0003"), new BigDecimal("0.0006"));

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
        model.setState(ModelState.ACTIVE);

        modelGateway.save(model);
        log.debug("Created model: {}", displayName);
    }

    /**
     * 初始化产品（按量计费）
     */
    private Product initProduct(Provider provider) {
        if (provider == null) {
            log.warn("Provider not initialized, skipping product initialization");
            return null;
        }

        if (productGateway.existsByProviderIdAndName(provider.getId(), "豆包按量计费")) {
            log.info("Product already exists, skipping");
            return productGateway.findByProviderIdAndType(provider.getId(), ProductType.PAY_AS_YOU_GO)
                    .stream().findFirst().orElse(null);
        }

        Product product = new Product();
        product.setProviderId(provider.getId());
        product.setProviderName(provider.getName());
        product.setName("豆包按量计费");
        product.setProductType(ProductType.PAY_AS_YOU_GO);
        product.setModels(List.of(
                "doubao-seed-2-0-lite-260428", "doubao-seed-2-0-mini-260428", "deepseek-v3-2-251201"
        ));
        product.setEndpoints(Map.of(
                "openai", "https://ark.cn-beijing.volces.com/api/v3"
        ));
        product.setState(ProductState.ACTIVE);

        Product saved = productGateway.save(product);
        log.info("Created product: {} (id={})", product.getName(), saved.getId());
        return saved;
    }

    /**
     * 初始化团队
     */
    private Team initTeam(String name, String description) {
        if (teamGateway.existsByName(name)) {
            log.info("Team '{}' already exists, skipping", name);
            return teamGateway.findAllActive().stream()
                    .filter(t -> name.equals(t.getName()))
                    .findFirst().orElse(null);
        }

        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setState(TeamState.ACTIVE);

        Team saved = teamGateway.save(team);
        log.info("Created team: {} (id={})", team.getName(), saved.getId());
        return saved;
    }

    /**
     * 初始化管理员用户
     */
    private User initAdminUser() {
        if (userGateway.existsByUsername("admin")) {
            log.info("Admin user already exists, skipping");
            return null;
        }

        User user = new User();
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setPasswordHash(passwordEncoder.encode("admin"));
        user.setState(UserState.ACTIVE);
        user.setRole("ADMIN");
        user.setEmailVerified(true);

        User saved = userGateway.save(user);
        log.info("Created admin user: {} (id={})", user.getUsername(), saved.getId());
        return saved;
    }

    /**
     * 初始化测试用户
     */
    private User initUser(String username, String email, String password) {
        if (userGateway.existsByUsername(username)) {
            log.info("User '{}' already exists, skipping", username);
            return null;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setState(UserState.ACTIVE);
        user.setRole("USER");
        user.setEmailVerified(true);

        User saved = userGateway.save(user);
        log.info("Created user: {} (id={})", user.getUsername(), saved.getId());
        return saved;
    }

    /**
     * 初始化用户 API Key（新架构）
     *
     * <p>创建 UserApiKey，keyPlain 由基础设施层自动加密和哈希。</p>
     * <p>同一用户可以在不同团队下拥有多个 Key，实现跨团队访问产品。</p>
     */
    private void initUserApiKey(User user, Team team, Product product, String keyPlain, String name) {
        if (user == null || team == null || product == null) {
            log.warn("Missing dependencies, skipping UserApiKey creation: {}", name);
            return;
        }

        // 检查同团队下是否已有同名 Key
        if (userApiKeyGateway.countByTeamId(team.getId()) > 0) {
            List<UserApiKey> existing = userApiKeyGateway.findByTeamId(team.getId());
            boolean exists = existing.stream().anyMatch(k -> name.equals(k.getName()));
            if (exists) {
                log.info("UserApiKey '{}' already exists in team '{}', skipping", name, team.getName());
                return;
            }
        }

        UserApiKey apiKey = new UserApiKey();
        apiKey.setTeamId(team.getId());
        apiKey.setOwnerUserId(user.getId());
        apiKey.setProductId(product.getId());
        apiKey.setKeyPlain(keyPlain);
        apiKey.setName(name);
        apiKey.setState(UserApiKeyState.ACTIVE);

        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Created UserApiKey: {} (id={}, prefix={}, team={}, user={})",
                name, saved.getId(), saved.getKeyPrefix(), team.getName(), user.getUsername());
    }
}