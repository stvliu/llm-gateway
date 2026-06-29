package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.entity.ApplicationChannel;
import com.codingas.gateway.domain.application.entity.ApplicationState;
import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.infrastructure.config.GatewayProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 示例数据加载器
 *
 * <p>从 classpath:data/sample/ 加载示例应用、用户和 API Key 数据。
 * 受 {@code gateway.init.demo-data-enabled} 开关控制，仅在开发/测试环境启用。</p>
 *
 * <p>权限锚点改造（Task 1.7）：团队体系已废弃，示例数据改为直接 seed
 * {@link Application} + {@link ApplicationChannel}，并将每个演示 API Key
 * 归属到对应应用，使数据面权限路由（应用→授权渠道→模型实例）在全新库上即开即用，
 * 不依赖 V52 迁移（V52 在全新库上是 no-op）。</p>
 */
@Slf4j
@Component
public class SampleDataLoader implements DataLoader {

    private static final String APPLICATIONS_JSON = "data/sample/applications.json";
    private static final String USERS_JSON = "data/sample/users.json";
    private static final String API_KEYS_JSON = "data/sample/apikeys.json";

    private final UserGateway userGateway;
    private final ApplicationGateway applicationGateway;
    private final ApplicationChannelGateway applicationChannelGateway;
    private final UserApiKeyGateway userApiKeyGateway;
    private final UserCreator userCreator;
    private final GatewayProperties gatewayProperties;

    public SampleDataLoader(UserGateway userGateway,
                            ApplicationGateway applicationGateway,
                            ApplicationChannelGateway applicationChannelGateway,
                            UserApiKeyGateway userApiKeyGateway,
                            UserCreator userCreator,
                            GatewayProperties gatewayProperties) {
        this.userGateway = userGateway;
        this.applicationGateway = applicationGateway;
        this.applicationChannelGateway = applicationChannelGateway;
        this.userApiKeyGateway = userApiKeyGateway;
        this.userCreator = userCreator;
        this.gatewayProperties = gatewayProperties;
    }

    // ========== JSON DTO records ==========

    private record SampleApplicationData(String name, String description, List<String> channels) {}

    private record SampleUserData(
            String username, String email, String password, String role, String application
    ) {}

    private record SampleApiKeyData(String username, String name) {}

    // ========== DataLoader ==========

    @Override
    public InitPhase getPhase() {
        return InitPhase.SAMPLE_DATA;
    }

    @Override
    public boolean isEnabled(GatewayProperties properties) {
        return properties.getInit().isDemoDataEnabled();
    }

    @Override
    public void load(DataLoadContext context) {
        // 幂等守卫
        if (isLoaded()) {
            log.info("示例数据已存在，跳过初始化");
            return;
        }

        log.info("开始初始化示例数据...");

        // 读取上游数据
        Map<String, Channel> channelMap = context.getRequired(DataLoadContext.ChannelIndex.class).getMap();
        Map<String, Application> applicationMap = loadApplications(channelMap);
        context.set(DataLoadContext.ApplicationIndex.class, new DataLoadContext.ApplicationIndex(applicationMap));

        Map<String, User> userMap = loadUsers();
        context.set(DataLoadContext.UserIndex.class, new DataLoadContext.UserIndex(userMap));

        loadApiKeys(userMap, applicationMap);

        logInitializationSummary(channelMap.size(), applicationMap.size(), userMap.size());
    }

    // ========== Internal methods ==========

    private boolean isLoaded() {
        return userGateway.findByUsername("test1").isPresent();
    }

    private Map<String, Application> loadApplications(Map<String, Channel> channelMap) {
        log.info("加载示例应用数据...");
        Map<String, Application> applicationMap = new HashMap<>();

        List<SampleApplicationData> apps = JsonResourceReader.readList(APPLICATIONS_JSON, new TypeReference<>() {});
        for (SampleApplicationData appData : apps) {
            Application saved = createApplication(appData.name(), appData.description());
            applicationMap.put(appData.name(), saved);

            if (appData.channels() != null) {
                List<ApplicationChannel> rels = new ArrayList<>();
                for (String channelKey : appData.channels()) {
                    Channel channel = channelMap.get(channelKey);
                    if (channel != null) {
                        rels.add(new ApplicationChannel(saved.getId(), channel.getId()));
                    } else {
                        log.warn("  渠道 key '{}' 未找到，跳过", channelKey);
                    }
                }
                if (!rels.isEmpty()) {
                    applicationChannelGateway.saveAll(rels);
                }
            }
        }

        log.info("  共加载 {} 个应用", applicationMap.size());
        return applicationMap;
    }

    private Map<String, User> loadUsers() {
        log.info("加载示例用户数据...");
        Map<String, User> userMap = new HashMap<>();

        List<SampleUserData> users = JsonResourceReader.readList(USERS_JSON, new TypeReference<>() {});
        for (SampleUserData userData : users) {
            User saved = userGateway.findByUsername(userData.username())
                    .orElseGet(() -> userCreator.create(
                            userData.username(), userData.email(),
                            userData.password(), userData.role(), false));
            userMap.put(userData.username(), saved);
        }

        log.info("  共加载 {} 个用户", userMap.size());
        return userMap;
    }

    private void loadApiKeys(Map<String, User> userMap, Map<String, Application> applicationMap) {
        log.info("加载示例 API Key 数据...");
        int count = 0;

        List<SampleApiKeyData> keys = JsonResourceReader.readList(API_KEYS_JSON, new TypeReference<>() {});
        // 用户名 → 归属应用编码（由 users.json 的 application 字段决定）
        Map<String, String> userApplicationMap = loadUserApplicationMap();
        for (SampleApiKeyData key : keys) {
            User user = userMap.get(key.username());
            if (user != null) {
                String appCode = userApplicationMap.get(key.username());
                Application app = appCode != null ? applicationMap.get(appCode) : null;
                Long applicationId = app != null ? app.getId() : null;
                createUserApiKey(user.getId(), key.name(), applicationId);
                count++;
            } else {
                log.warn("  用户 '{}' 未找到，跳过 API Key", key.username());
            }
        }

        log.info("  共加载 {} 个 API Key", count);
    }

    /**
     * 读取 users.json，构建用户名 → 应用编码映射，用于 API Key 归属。
     */
    private Map<String, String> loadUserApplicationMap() {
        Map<String, String> map = new HashMap<>();
        List<SampleUserData> users = JsonResourceReader.readList(USERS_JSON, new TypeReference<>() {});
        for (SampleUserData userData : users) {
            if (userData.application() != null) {
                map.put(userData.username(), userData.application());
            }
        }
        return map;
    }

    private Application createApplication(String code, String description) {
        Application app = new Application();
        app.setCode(code);
        app.setName(code);
        app.setDescription(description);
        app.setState(ApplicationState.ACTIVE);
        return applicationGateway.save(app);
    }

    private void createUserApiKey(Long userId, String name, Long applicationId) {
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setUserId(userId);
        userApiKey.setApplicationId(applicationId);
        userApiKey.setName(name);
        String uuid8 = UUID.randomUUID().toString().substring(0, 8);
        userApiKey.setKeyPlain("sk-" + uuid8 + "-" + name.toLowerCase().replace(" ", "-"));
        userApiKeyGateway.save(userApiKey);
    }

    private void logInitializationSummary(int channelCount, int applicationCount, int userCount) {
        log.info("========================================");
        log.info("示例数据初始化完成!");
        log.info("========================================");
        log.info("  渠道: {}", channelCount);
        log.info("  应用: {}", applicationCount);
        log.info("  用户: {}", userCount);
        log.info("========================================");
        log.info("Test accounts:");
        log.info("  Admin - Username: admin, Password: admin, Role: ADMIN");
        log.info("  Demo users - Username: test1 ~ test10, Password: same as username");
        log.info("  Volcengine users - hermes-agent / claude-code / bi-report");
        log.info("========================================");
    }
}
