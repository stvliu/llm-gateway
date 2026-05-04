# Provider 模型与 API Key 数据库加载设计

**日期:** 2026-05-04
**状态:** 已批准
**作者:** Claude & Liu Ye

---

## 1. 概述

设计从数据库加载 Provider、Model、ProviderApiKey 的机制，支持单实例和多实例部署场景，确保配置变更能实时同步到所有实例。

### 1.1 设计目标

- 支持标准版（单实例）和企业版（多实例）两种部署模式
- 配置变更后，所有实例缓存能及时更新
- API Key 安全存储，不在数据库存明文
- 高可用：Redis 故障时有兜底机制

### 1.2 关键决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| 加载时机 | 启动加载 + 事件驱动刷新 | 兼顾性能与实时性 |
| 事件机制 | Spring Event（本地）/ Redis Pub/Sub（远程）+ 定时轮询兜底 | 可靠性保障，统一接口 |
| 缓存框架 | Spring Cache（Caffeene 本地 / Redis 分布式） | 统一抽象，配置切换 |
| 敏感数据处理 | API Key 仅本地缓存，不进 Redis | 安全性保障 |
| 缓存粒度 | 分离缓存（Provider/Model/ApiKey 各自独立） | 灵活刷新 |
| 版本号 | 全部实体加 version 字段 | 乐观锁 + 变更检测统一 |
| 刷新策略 | 全量刷新 | 实现简单，规模可控 |

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      配置加载与同步架构                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │  Instance 1 │    │  Instance 2 │    │  Instance N │     │
│  ├─────────────┤    ├─────────────┤    ├─────────────┤     │
│  │ Spring Cache│    │ Spring Cache│    │ Spring Cache│     │
│  │ (Caffeine)  │    │ (Caffeine)  │    │ (Caffeine)  │     │
│  │ ┌─────────┐ │    │ ┌─────────┐ │    │ ┌─────────┐ │     │
│  │ │providers│ │    │ │providers│ │    │ │providers│ │     │
│  │ │models   │ │    │ │models   │ │    │ │models   │ │     │
│  │ └─────────┘ │    │ └─────────┘ │    │ └─────────┘ │     │
│  │ ┌─────────┐ │    │ ┌─────────┐ │    │ ┌─────────┐ │     │
│  │ │apiKeys  │ │    │ │apiKeys  │ │    │ │apiKeys  │ │     │
│  │ │(本地专用)│ │    │ │(本地专用)│ │    │ │(本地专用)│ │     │
│  │ └─────────┘ │    │ └─────────┘ │    │ └─────────┘ │     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘     │
│         │                  │                  │             │
│         └──────────────────┼──────────────────┘             │
│                            │                                │
│                     ┌──────▼──────┐                         │
│                     │    Redis    │                         │
│                     │  Pub/Sub    │                         │
│                     │   (可选)    │                         │
│                     └──────┬──────┘                         │
│                            │                                │
│  ┌─────────────────────────▼─────────────────────────┐     │
│  │                   PostgreSQL                       │     │
│  │  - providers (version)                             │     │
│  │  - models (version)                                │     │
│  │  - provider_api_keys (version, encrypted_api_key)  │     │
│  └───────────────────────────────────────────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘

部署模式：
┌─────────────────────────────────────────────────────────────┐
│ 标准版（单实例）                                              │
│  - 缓存：Caffeine（本地）                                    │
│  - 事件：Spring ApplicationEvent（本地）                     │
│  - 无需 Redis                                               │
├─────────────────────────────────────────────────────────────┤
│ 企业版（多实例）                                              │
│  - 缓存：Caffeine（本地）+ Redis（可选共享非敏感数据）         │
│  - 事件：Redis Pub/Sub（远程广播）                           │
│  - 需要 Redis                                               │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 配置同步流程

**正常变更流程：**
```
管理后台更新配置
    ↓
数据库更新（version++）
    ↓
发布事件（Redis Pub/Sub）
    ↓
各实例收到事件 → 清空缓存 → 从数据库重新加载
```

**兜底轮询流程：**
```
每 30 秒检查各表的 MAX(version)
    ↓
发现变化 → 刷新对应缓存
```

---

## 3. 数据模型变更

### 3.1 移除字段

**provider_api_keys 表：**
- 移除 `api_key` 字段（不再存储明文）

### 3.2 新增字段

**所有实体表新增 version 字段（乐观锁）：**

```sql
-- 以下表新增 version 字段
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE gateway_api_keys ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE providers ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE provider_api_keys ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE models ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE route_groups ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE route_group_providers ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE token_limits ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE rate_limit_configs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE alert_rules ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE alert_notifications ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE ip_blocklist ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE sensitive_data_rules ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
```

### 3.3 乐观锁更新示例

```sql
UPDATE providers
SET provider_name = ?, version = version + 1, updated_at = NOW()
WHERE id = ? AND version = ?;

-- 如果 affected_rows = 0，说明版本冲突，需重试
```

---

## 4. 缓存设计

### 4.1 CacheManager 配置

**核心设计：两个独立 CacheManager**
- `localCacheManager` - 本地缓存（Caffeine），始终存在，用于敏感数据
- `distributedCacheManager` - 分布式缓存（Redis），企业版启用，用于非敏感数据
- `@Primary` 决定默认使用哪个

```java
/**
 * 缓存配置
 *
 * <p>提供两个独立的 CacheManager：</p>
 * <ul>
 *   <li>localCacheManager - 本地缓存（Caffeine），用于敏感数据</li>
 *   <li>distributedCacheManager - 分布式缓存（Redis），企业版启用</li>
 * </ul>
 */
@Configuration
@Slf4j
public class CacheConfig {

    // ========== 本地缓存管理器（始终存在）==========

    /**
     * 本地缓存管理器
     *
     * <p>用于敏感数据（API Key），始终使用 Caffeine 本地缓存。</p>
     * <p>标准版和企业版都使用此缓存管理器存储敏感数据。</p>
     */
    @Bean("localCacheManager")
    public CacheManager localCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats());
        log.info("Local cache manager (Caffeine) initialized");
        return manager;
    }

    // ========== 标准版：默认使用本地缓存 ==========

    /**
     * 标准版默认缓存管理器
     *
     * <p>单实例部署，所有数据使用本地缓存。</p>
     */
    @Bean
    @Primary
    @Profile({"local", "dev", "standalone"})
    public CacheManager defaultCacheManagerStandalone(
            @Qualifier("localCacheManager") CacheManager localCacheManager) {
        log.info("Using local cache as default (standalone mode)");
        return localCacheManager;
    }

    // ========== 企业版：默认使用分布式缓存 ==========

    /**
     * 分布式缓存管理器
     *
     * <p>用于非敏感数据（Provider、Model），多实例共享。</p>
     */
    @Bean("distributedCacheManager")
    @Profile({"cluster", "enterprise"})
    public CacheManager distributedCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        log.info("Distributed cache manager (Redis) initialized");
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }

    /**
     * 企业版默认缓存管理器
     *
     * <p>多实例部署，非敏感数据默认使用 Redis。</p>
     */
    @Bean
    @Primary
    @Profile({"cluster", "enterprise"})
    public CacheManager defaultCacheManagerCluster(
            @Qualifier("distributedCacheManager") CacheManager distributedCacheManager) {
        log.info("Using distributed cache as default (cluster mode)");
        return distributedCacheManager;
    }
}
```

**配置文件：**

```yaml
# application-standalone.yaml (标准版)
spring:
  profiles:
    active: standalone
  cache:
    type: caffeine

# application-cluster.yaml (企业版)
spring:
  profiles:
    active: cluster
  cache:
    type: redis
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
```

### 4.2 缓存名称定义

```java
/**
 * 缓存名称常量
 */
public final class CacheNames {

    /** Provider 缓存（可共享到 Redis） */
    public static final String PROVIDERS = "providers";

    /** Model 缓存（可共享到 Redis） */
    public static final String MODELS = "models";

    /** API Key 缓存（敏感数据，仅本地） */
    public static final String API_KEYS_LOCAL = "apiKeysLocal";
}
```

### 4.3 缓存服务实现

```java
/**
 * 配置缓存服务
 *
 * <p>使用 Spring Cache 统一缓存抽象。</p>
 */
@Service
@Slf4j
public class ConfigCacheService {

    @Autowired
    private ProviderGateway providerGateway;

    @Autowired
    private ModelGateway modelGateway;

    @Autowired
    private ProviderApiKeyGateway apiKeyGateway;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private CacheManager cacheManager;

    // ========== Provider 操作 ==========

    @Cacheable(value = CacheNames.PROVIDERS, key = "#id")
    public Optional<Provider> getProviderById(Long id) {
        return providerGateway.findById(id);
    }

    @Cacheable(value = CacheNames.PROVIDERS, key = "'code:' + #providerCode")
    public Optional<Provider> getProviderByCode(String providerCode) {
        return providerGateway.findByProviderCode(providerCode);
    }

    @Cacheable(value = CacheNames.PROVIDERS, key = "'all'")
    public List<Provider> getAllProviders() {
        return providerGateway.findAll();
    }

    @Cacheable(value = CacheNames.PROVIDERS, key = "'active'")
    public List<Provider> getActiveProviders() {
        return providerGateway.findAllActive();
    }

    // ========== Model 操作 ==========

    @Cacheable(value = CacheNames.MODELS, key = "#id")
    public Optional<Model> getModelById(Long id) {
        return modelGateway.findById(id);
    }

    @Cacheable(value = CacheNames.MODELS, key = "'code:' + #modelCode")
    public Optional<Model> getModelByCode(String modelCode) {
        return modelGateway.findByModelCode(modelCode);
    }

    @Cacheable(value = CacheNames.MODELS, key = "'all'")
    public List<Model> getAllModels() {
        return modelGateway.findAll();
    }

    @Cacheable(value = CacheNames.MODELS, key = "'active'")
    public List<Model> getActiveModels() {
        return modelGateway.findAllActive();
    }

    @Cacheable(value = CacheNames.MODELS, key = "'provider:' + #providerId")
    public List<Model> getModelsByProviderId(Long providerId) {
        return modelGateway.findByProviderId(providerId);
    }

    // ========== API Key 操作（敏感数据，仅本地缓存）==========

    /**
     * 获取 API Key（解密后）
     *
     * <p>敏感数据，使用本地专用缓存。</p>
     */
    @Cacheable(value = CacheNames.API_KEYS_LOCAL, key = "#providerId")
    public Optional<ProviderApiKey> getApiKeyByProviderId(Long providerId) {
        return apiKeyGateway.findByProviderId(providerId)
            .map(this::decryptApiKey);
    }

    /**
     * 解密 API Key
     */
    private ProviderApiKey decryptApiKey(ProviderApiKey apiKey) {
        if (apiKey.getEncryptedApiKey() != null) {
            String decrypted = encryptionService.decrypt(apiKey.getEncryptedApiKey());
            apiKey.setApiKey(decrypted);
        }
        return apiKey;
    }

    // ========== 缓存刷新 ==========

    @CacheEvict(value = CacheNames.PROVIDERS, allEntries = true)
    public void refreshProviders() {
        log.info("Providers cache refreshed");
    }

    @CacheEvict(value = CacheNames.MODELS, allEntries = true)
    public void refreshModels() {
        log.info("Models cache refreshed");
    }

    @CacheEvict(value = CacheNames.API_KEYS_LOCAL, allEntries = true)
    public void refreshApiKeys() {
        log.info("API Keys cache refreshed");
    }

    public void refreshAll() {
        refreshProviders();
        refreshModels();
        refreshApiKeys();
        log.info("All caches refreshed");
    }
}
```

### 4.4 API Key 本地缓存隔离

**关键：通过 `cacheManager` 参数显式指定**

```java
@Service
@Slf4j
public class ConfigCacheService {

    // ========== Provider 操作（使用默认 CacheManager）==========
    // 标准版：Caffeine 本地缓存
    // 企业版：Redis 分布式缓存

    @Cacheable(value = CacheNames.PROVIDERS, key = "#id")
    public Optional<Provider> getProviderById(Long id) {
        return providerGateway.findById(id);
    }

    @Cacheable(value = CacheNames.PROVIDERS, key = "'code:' + #providerCode")
    public Optional<Provider> getProviderByCode(String providerCode) {
        return providerGateway.findByProviderCode(providerCode);
    }

    // ========== Model 操作（使用默认 CacheManager）==========

    @Cacheable(value = CacheNames.MODELS, key = "#id")
    public Optional<Model> getModelById(Long id) {
        return modelGateway.findById(id);
    }

    @Cacheable(value = CacheNames.MODELS, key = "'code:' + #modelCode")
    public Optional<Model> getModelByCode(String modelCode) {
        return modelGateway.findByModelCode(modelCode);
    }

    // ========== API Key 操作（强制使用本地 CacheManager）==========
    // 关键：cacheManager = "localCacheManager" 确保不进入 Redis

    /**
     * 获取 API Key（解密后）
     *
     * <p>敏感数据，强制使用本地缓存管理器。</p>
     * <p>无论标准版还是企业版，API Key 都只存在本地内存中。</p>
     */
    @Cacheable(value = CacheNames.API_KEYS_LOCAL,
               key = "#providerId",
               cacheManager = "localCacheManager")  // 关键：显式指定本地缓存
    public Optional<ProviderApiKey> getApiKeyByProviderId(Long providerId) {
        return apiKeyGateway.findByProviderId(providerId)
            .map(this::decryptApiKey);
    }

    /**
     * 刷新 API Key 缓存
     */
    @CacheEvict(value = CacheNames.API_KEYS_LOCAL,
                allEntries = true,
                cacheManager = "localCacheManager")  // 同样需要指定
    public void refreshApiKeys() {
        log.info("API Keys cache refreshed");
    }
}
```

**缓存隔离总结：**

| 缓存名称 | 标准版存储位置 | 企业版存储位置 | 数据类型 |
|---------|--------------|--------------|---------|
| `providers` | Caffeine（本地） | Redis（分布式） | 非敏感 |
| `models` | Caffeine（本地） | Redis（分布式） | 非敏感 |
| `apiKeysLocal` | Caffeine（本地） | Caffeine（本地） | 敏感 |

**企业版数据流：**
```
┌─────────────────────────────────────────────────────────────┐
│                      企业版缓存架构                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   getProviderById()  ───► @Cacheable                        │
│                              ↓                              │
│                     distributedCacheManager                 │
│                              ↓                              │
│                           Redis  ✅ 共享                    │
│                                                             │
│   getApiKeyByProviderId() ───► @Cacheable(cacheManager="localCacheManager")
│                                   ↓                         │
│                          localCacheManager                  │
│                                   ↓                         │
│                           Caffeine  ✅ 仅本地               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. 事件机制设计

### 5.1 事件类型定义

```java
/**
 * 配置变更事件
 *
 * <p>支持本地和远程两种传输方式。</p>
 */
public record ConfigChangedEvent(
    ConfigType configType,
    ChangeType changeType,
    Long entityId,
    Instant timestamp
) {
    public enum ConfigType {
        PROVIDER,
        MODEL,
        PROVIDER_API_KEY
    }

    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }

    public static ConfigChangedEvent of(ConfigType configType, ChangeType changeType, Long entityId) {
        return new ConfigChangedEvent(configType, changeType, entityId, Instant.now());
    }
}
```

### 5.2 统一事件发布接口

```java
/**
 * 配置事件发布器
 *
 * <p>统一接口，支持本地和远程两种实现。</p>
 */
public interface ConfigEventPublisher {

    /**
     * 发布配置变更事件
     *
     * @param event 配置变更事件
     */
    void publish(ConfigChangedEvent event);
}
```

### 5.3 本地事件实现（标准版）

```java
/**
 * 本地事件发布器
 *
 * <p>使用 Spring ApplicationEvent，适用于单实例部署。</p>
 */
@Component
@Profile({"local", "dev", "standalone"})
@Slf4j
public class LocalConfigEventPublisher implements ConfigEventPublisher {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(ConfigChangedEvent event) {
        log.debug("Publishing local event: {}", event);
        eventPublisher.publishEvent(event);
    }
}
```

### 5.4 Redis 远程事件实现（企业版）

```java
/**
 * Redis 远程事件发布器
 *
 * <p>使用 Redis Pub/Sub，适用于多实例部署。</p>
 */
@Component
@Profile({"cluster", "enterprise"})
@Slf4j
public class RedisConfigEventPublisher implements ConfigEventPublisher {

    private static final String CHANNEL = "gateway:config:changed";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void publish(ConfigChangedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL, payload);
            log.debug("Published Redis event: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish Redis event: {}", event, e);
            // 降级：仍然触发本地事件
            throw new RuntimeException("Redis publish failed", e);
        }
    }
}
```

### 5.5 Redis 订阅者配置

```java
/**
 * Redis 消息监听器配置
 */
@Configuration
@Profile({"cluster", "enterprise"})
public class RedisEventConfig {

    private static final String CHANNEL = "gateway:config:changed";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ConfigEventMessageListener messageListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListener, new ChannelTopic(CHANNEL));
        return container;
    }
}

/**
 * Redis 配置事件监听器
 */
@Component
@Profile({"cluster", "enterprise"})
@Slf4j
public class ConfigEventMessageListener implements MessageListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigCacheService cacheService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            ConfigChangedEvent event = objectMapper.readValue(payload, ConfigChangedEvent.class);
            log.info("Received Redis config event: {}", event);

            // 刷新对应缓存
            handleEvent(event);
        } catch (Exception e) {
            log.error("Failed to handle Redis message: {}", message, e);
        }
    }

    private void handleEvent(ConfigChangedEvent event) {
        switch (event.configType()) {
            case PROVIDER -> cacheService.refreshProviders();
            case MODEL -> cacheService.refreshModels();
            case PROVIDER_API_KEY -> cacheService.refreshApiKeys();
        }
    }
}
```

### 5.6 本地事件监听器

```java
/**
 * 本地配置事件监听器
 *
 * <p>所有部署模式通用。</p>
 */
@Component
@Slf4j
public class ConfigEventListener {

    @Autowired
    private ConfigCacheService cacheService;

    @EventListener
    public void onConfigChanged(ConfigChangedEvent event) {
        log.info("Received local config event: {}", event);

        switch (event.configType()) {
            case PROVIDER -> cacheService.refreshProviders();
            case MODEL -> cacheService.refreshModels();
            case PROVIDER_API_KEY -> cacheService.refreshApiKeys();
        }
    }
}
```

### 5.7 统一事件发布服务

```java
/**
 * 配置事件服务
 *
 * <p>提供统一的事件发布入口，自动选择本地或远程实现。</p>
 */
@Service
@Slf4j
public class ConfigEventService {

    @Autowired
    private ConfigEventPublisher eventPublisher;

    /**
     * 发布配置变更事件
     */
    public void publishConfigChanged(ConfigChangedEvent.ConfigType configType,
                                     ConfigChangedEvent.ChangeType changeType,
                                     Long entityId) {
        ConfigChangedEvent event = ConfigChangedEvent.of(configType, changeType, entityId);
        log.info("Publishing config event: {}", event);
        eventPublisher.publish(event);
    }
}
```

---

## 6. 兜底机制

### 6.1 定时轮询服务

```java
/**
 * 配置版本检查服务
 *
 * <p>定时检查数据库配置版本，作为事件机制的兜底。</p>
 * <p>轮询间隔：30 秒</p>
 */
@Component
@Slf4j
public class ConfigVersionChecker {

    private static final long CHECK_INTERVAL_SECONDS = 30;

    @Autowired
    private ProviderGateway providerGateway;

    @Autowired
    private ModelGateway modelGateway;

    @Autowired
    private ProviderApiKeyGateway apiKeyGateway;

    @Autowired
    private ConfigCacheService cacheService;

    // 记录上次检查的版本
    private volatile long lastProviderVersion = 0;
    private volatile long lastModelVersion = 0;
    private volatile long lastApiKeyVersion = 0;

    @Scheduled(fixedRate = CHECK_INTERVAL_SECONDS * 1000)
    public void checkVersions() {
        // 检查 Provider 版本
        long currentProviderVersion = providerGateway.getMaxVersion();
        if (currentProviderVersion > lastProviderVersion) {
            log.info("Provider version changed: {} -> {}", lastProviderVersion, currentProviderVersion);
            cacheService.refreshProviders();
            lastProviderVersion = currentProviderVersion;
        }

        // 检查 Model 版本
        long currentModelVersion = modelGateway.getMaxVersion();
        if (currentModelVersion > lastModelVersion) {
            log.info("Model version changed: {} -> {}", lastModelVersion, currentModelVersion);
            cacheService.refreshModels();
            lastModelVersion = currentModelVersion;
        }

        // 检查 API Key 版本
        long currentApiKeyVersion = apiKeyGateway.getMaxVersion();
        if (currentApiKeyVersion > lastApiKeyVersion) {
            log.info("API Key version changed: {} -> {}", lastApiKeyVersion, currentApiKeyVersion);
            cacheService.refreshApiKeys();
            lastApiKeyVersion = currentApiKeyVersion;
        }
    }
}
```

### 6.2 Gateway 接口扩展

```java
public interface ProviderGateway {
    // ... 现有方法 ...

    /**
     * 获取最大版本号
     *
     * @return 最大版本号
     */
    long getMaxVersion();
}
```

---

## 7. 加密解密流程

### 7.1 写入流程

```
用户输入明文 API Key
    ↓
Controller 接收
    ↓
ProviderApiKeyService.create()
    ↓
EncryptionService.encrypt(plainText)
    ↓
ProviderApiKey.setEncryptedApiKey(cipherText)
    ↓
ProviderApiKeyGateway.save()
    ↓
数据库存储 encrypted_api_key
```

### 7.2 读取流程

```
缓存刷新触发
    ↓
ProviderApiKeyGateway.findAllActive()
    ↓
返回 ProviderApiKey（含 encrypted_api_key）
    ↓
EncryptionService.decrypt(cipherText)
    ↓
ProviderApiKey.setApiKey(plainText)  // 仅内存中
    ↓
存入本地缓存
```

### 7.3 加密服务接口

```java
/**
 * 加密服务
 *
 * <p>提供 API Key 的加密和解密功能。</p>
 */
public interface EncryptionService {

    /**
     * 加密
     *
     * @param plainText 明文
     * @return 密文
     */
    String encrypt(String plainText);

    /**
     * 解密
     *
     * @param cipherText 密文
     * @return 明文
     */
    String decrypt(String cipherText);
}
```

---

## 8. 启动加载流程

```java
/**
 * 配置加载器
 *
 * <p>应用启动时加载配置到缓存。</p>
 */
@Component
@Slf4j
public class ConfigLoader implements ApplicationRunner {

    @Autowired
    private ConfigCacheService cacheService;

    @Autowired
    private ConfigVersionChecker versionChecker;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Loading configuration into cache...");

        // 加载所有配置
        cacheService.refreshAll();

        // 初始化版本号
        versionChecker.initVersions();

        log.info("Configuration loaded successfully");
    }
}
```

---

## 9. 配置项

```yaml
gateway:
  config:
    cache:
      # 轮询检查间隔（秒）
      version-check-interval: 30
    event:
      # Redis Pub/Sub 通道
      redis-channel: "gateway:config:changed"
```

---

## 10. 数据库迁移脚本

**V3__add_version_and_remove_api_key.sql:**

```sql
-- 添加 version 字段（乐观锁）
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE gateway_api_keys ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE providers ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE provider_api_keys ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE models ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE route_groups ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE route_group_providers ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE token_limits ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE rate_limit_configs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE audit_logs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE usage_logs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE alert_rules ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE alert_notifications ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE ip_blocklist ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE sensitive_data_rules ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 移除 api_key 字段（不再存储明文）
ALTER TABLE provider_api_keys DROP COLUMN api_key;

-- 创建索引加速 MAX(version) 查询
CREATE INDEX idx_providers_version ON providers(version);
CREATE INDEX idx_models_version ON models(version);
CREATE INDEX idx_provider_api_keys_version ON provider_api_keys(version);
```

---

## 11. 部署模式配置

### 11.1 标准版（单实例）

```yaml
# application-standalone.yaml
spring:
  profiles:
    active: standalone
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=1h

gateway:
  config:
    event:
      mode: local
    cache:
      version-check-interval: 30s
```

### 11.2 企业版（多实例）

```yaml
# application-cluster.yaml
spring:
  profiles:
    active: cluster
  cache:
    type: redis
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

gateway:
  config:
    event:
      mode: redis
      redis-channel: "gateway:config:changed"
    cache:
      version-check-interval: 30s
```

### 11.3 依赖配置

```xml
<!-- 基础依赖（标准版必需） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- 企业版额外依赖（用于分布式缓存和事件广播） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 11.4 启动验证

**标准版启动日志：**
```
INFO  CacheConfig - Local cache manager (Caffeine) initialized
INFO  CacheConfig - Using local cache as default (standalone mode)
```

**企业版启动日志：**
```
INFO  CacheConfig - Local cache manager (Caffeine) initialized
INFO  CacheConfig - Distributed cache manager (Redis) initialized
INFO  CacheConfig - Using distributed cache as default (cluster mode)
INFO  RedisEventConfig - Redis message listener container started
```

---

## 12. 测试要点

### 12.1 单元测试

- `ConfigCacheService` 缓存读写与刷新
- `LocalConfigEventPublisher` 本地事件发布
- `RedisConfigEventPublisher` 远程事件发布
- `EncryptionService` 加密解密
- 乐观锁版本冲突处理

### 12.2 集成测试

- 启动加载流程验证
- Spring Cache 本地缓存（Caffeine）测试
- Spring Cache 分布式缓存（Redis）测试
- 本地事件（ApplicationEvent）触发缓存刷新
- 远程事件（Redis Pub/Sub）触发缓存刷新
- 定时轮询兜底机制
- API Key 不进入 Redis 缓存验证

### 12.3 场景测试

| 场景 | 预期结果 |
|------|---------|
| 新实例启动 | 从数据库加载最新配置 |
| 管理后台新增 Provider | 所有实例缓存更新 |
| 管理后台修改 Model 价格 | 所有实例缓存更新 |
| API Key 轮换 | 所有实例缓存更新 |
| Redis 故障 | 定时轮询兜底，30秒内同步 |
| 实例重启 | 启动时加载最新配置 |
| API Key 查询 | 仅本地缓存，不进入 Redis |

---

## 13. 后续优化方向

1. **增量刷新** - 当配置量大时，可考虑只刷新变更的记录
2. **二级缓存优化** - 企业版 Provider/Model 使用 Redis + Caffeine 二级缓存
3. **配置变更审计** - 记录谁在什么时间修改了什么配置
4. **灰度发布支持** - 配置变更可按比例逐步推送到实例
5. **缓存预热** - 启动时异步加载缓存，减少启动时间
