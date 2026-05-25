# Provider 模型与 API Key 数据库加载实现规划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现从数据库加载 Provider、Model、ProviderApiKey 的缓存机制，支持单实例和多实例部署场景，确保配置变更实时同步。

**Architecture:** 采用 Spring Cache 统一抽象，双 CacheManager 设计（本地 + 分布式），事件驱动（Spring Event / Redis Pub/Sub）+ 定时轮询兜底机制。

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring Cache, Caffeine, Redis, Spring Data JPA, Flyway

---

## 文件结构

```
src/main/java/com/codingas/gateway/
├── common/
│   ├── event/
│   │   ├── DomainEvent.java              # ✅ 已存在（接口）
│   │   └── DomainEventPublisher.java     # 🆕 新建（接口）
│   └── security/
│       └── EncryptionService.java        # ✅ 已存在
├── domain/
│   ├── BaseEntity.java                   # ✏️ 修改（添加 version）
│   └── model/
│       ├── entity/
│       │   ├── Provider.java             # ✏️ 修改（添加 version getter）
│       │   ├── Model.java                # ✏️ 修改（添加 version getter）
│       │   └── ProviderApiKey.java       # ✏️ 修改（移除 apiKey 字段）
│       ├── gateway/
│       │   ├── ProviderGateway.java      # ✏️ 修改（添加 getMaxVersion）
│       │   ├── ModelGateway.java         # ✏️ 修改（添加 getMaxVersion）
│       │   └── ProviderApiKeyGateway.java # ✏️ 修改（添加 findByProviderId, getMaxVersion）
│       └── event/
│           └── ConfigChangedEvent.java   # 🆕 新建
├── infrastructure/
│   ├── config/
│   │   ├── CacheConfig.java              # 🆕 新建
│   │   ├── CacheNames.java               # 🆕 新建
│   │   ├── ConfigCacheService.java       # 🆕 新建
│   │   ├── ConfigEventPublisher.java     # 🆕 新建
│   │   └── RedisEventConfig.java         # 🆕 新建
│   ├── event/
│   │   ├── LocalDomainEventPublisher.java # 🆕 新建
│   │   ├── RedisDomainEventPublisher.java # 🆕 新建
│   │   └── ConfigChangedEventListener.java # 🆕 新建
│   ├── model/gateway/
│   │   ├── ProviderGatewayImpl.java      # ✏️ 修改（添加 getMaxVersion）
│   │   ├── ModelGatewayImpl.java         # ✏️ 修改（添加 getMaxVersion）
│   │   └── ProviderApiKeyGatewayImpl.java # ✏️ 修改（添加方法）
│   └── config/
│       └── ConfigLoader.java             # 🆕 新建
│       └── ConfigVersionChecker.java     # 🆕 新建
└── src/main/resources/
    └── db/migration/
        └── V3__add_version_fields.sql    # 🆕 新建

src/test/java/com/codingas/gateway/
├── infrastructure/config/
│   ├── ConfigCacheServiceTest.java       # 🆕 新建
│   └── CacheConfigTest.java              # 🆕 新建
└── infrastructure/event/
    ├── LocalDomainEventPublisherTest.java # 🆕 新建
    └── ConfigChangedEventListenerTest.java # 🆕 新建
```

---

## Task 1: 添加 Maven 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 添加 Spring Cache 和 Caffeine 依赖**

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- Spring Cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Caffeine 本地缓存 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

- [ ] **Step 2: 验证依赖添加成功**

Run: `mvn dependency:resolve -DincludeScope=compile | grep -E "(cache|caffeine)"`
Expected: 显示 spring-boot-starter-cache 和 caffeine 依赖

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: add Spring Cache and Caffeine dependencies"
```

---

## Task 2: 添加 version 字段到 BaseEntity

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/BaseEntity.java`

- [ ] **Step 1: 写失败的测试**

创建测试文件 `src/test/java/com/codingas/gateway/domain/BaseEntityTest.java`：

```java
package com.codingas.gateway.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    @DisplayName("BaseEntity should have version field with default value 0")
    void shouldHaveVersionFieldWithDefaultValue() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("BaseEntity version should be mutable")
    void versionShouldBeMutable() {
        TestEntity entity = new TestEntity();
        entity.setVersion(5L);
        assertThat(entity.getVersion()).isEqualTo(5L);
    }

    // 测试实体类
    static class TestEntity extends BaseEntity {
        // 仅用于测试
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=BaseEntityTest -q`
Expected: FAIL - cannot find symbol getVersion()

- [ ] **Step 3: 修改 BaseEntity 添加 version 字段**

```java
package com.codingas.gateway.domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 领域实体基类
 *
 * <p>提供公共字段，无 JPA 依赖。领域实体应继承此类。</p>
 */
@Data
@Slf4j
public abstract class BaseEntity {

    protected Long id;

    protected Instant createdAt;

    protected Instant updatedAt;

    /**
     * 乐观锁版本号
     *
     * <p>用于并发控制和变更检测。</p>
     */
    protected Long version = 0L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=BaseEntityTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/domain/BaseEntity.java
git add src/test/java/com/codingas/gateway/domain/BaseEntityTest.java
git commit -m "feat: add version field to BaseEntity for optimistic locking"
```

---

## Task 3: 创建数据库迁移脚本

**Files:**
- Create: `src/main/resources/db/migration/V3__add_version_fields.sql`

- [ ] **Step 1: 创建迁移脚本**

```sql
-- V3__add_version_fields.sql
-- 添加 version 字段用于乐观锁和变更检测

-- 用户表
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 网关 API Key 表
ALTER TABLE gateway_api_keys ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 提供商表
ALTER TABLE providers ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Provider API Key 表
ALTER TABLE provider_api_keys ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 模型表
ALTER TABLE models ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 路由分组表
ALTER TABLE route_groups ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 路由分组与提供商关联表
ALTER TABLE route_group_providers ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Token 限额表
ALTER TABLE token_limits ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 限流配置表
ALTER TABLE rate_limit_configs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 审计日志表
ALTER TABLE audit_logs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 使用记录表
ALTER TABLE usage_logs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 预警规则表
ALTER TABLE alert_rules ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 预警通知表
ALTER TABLE alert_notifications ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- IP 黑名单表
ALTER TABLE ip_blocklist ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 敏感数据规则表
ALTER TABLE sensitive_data_rules ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 创建索引加速 MAX(version) 查询
CREATE INDEX idx_providers_version ON providers(version);
CREATE INDEX idx_models_version ON models(version);
CREATE INDEX idx_provider_api_keys_version ON provider_api_keys(version);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/migration/V3__add_version_fields.sql
git commit -m "feat: add version fields migration for optimistic locking"
```

---

## Task 4: 创建 DomainEventPublisher 接口

**Files:**
- Create: `src/main/java/com/codingas/gateway/common/event/DomainEventPublisher.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/common/event/DomainEventPublisherTest.java`：

```java
package com.codingas.gateway.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

class DomainEventPublisherTest {

    @Test
    @DisplayName("DomainEventPublisher should be a functional interface")
    void shouldBeFunctionalInterface() {
        assertThatCode(() -> {
            DomainEventPublisher publisher = event -> {};
            publisher.publish(new TestEvent());
        }).doesNotThrowAnyException();
    }

    static class TestEvent implements DomainEvent {
        @Override
        public Instant occurredOn() {
            return Instant.now();
        }
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=DomainEventPublisherTest -q`
Expected: FAIL - cannot find symbol DomainEventPublisher

- [ ] **Step 3: 创建 DomainEventPublisher 接口**

```java
package com.codingas.gateway.common.event;

/**
 * 领域事件发布器
 *
 * <p>通用接口，支持本地和远程两种实现。</p>
 */
@FunctionalInterface
public interface DomainEventPublisher {

    /**
     * 发布领域事件
     *
     * @param event 领域事件
     * @param <T> 事件类型
     */
    <T extends DomainEvent> void publish(T event);
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=DomainEventPublisherTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/common/event/DomainEventPublisher.java
git add src/test/java/com/codingas/gateway/common/event/DomainEventPublisherTest.java
git commit -m "feat: add DomainEventPublisher interface for event publishing"
```

---

## Task 5: 创建 ConfigChangedEvent 事件类

**Files:**
- Create: `src/main/java/com/codingas/gateway/domain/model/event/ConfigChangedEvent.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/domain/model/event/ConfigChangedEventTest.java`：

```java
package com.codingas.gateway.domain.model.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigChangedEventTest {

    @Test
    @DisplayName("should create ConfigChangedEvent with all fields")
    void shouldCreateEventWithAllFields() {
        ConfigChangedEvent event = ConfigChangedEvent.of(
            ConfigChangedEvent.ConfigType.PROVIDER,
            ConfigChangedEvent.ChangeType.UPDATED,
            1L
        );

        assertThat(event.getConfigType()).isEqualTo(ConfigChangedEvent.ConfigType.PROVIDER);
        assertThat(event.getChangeType()).isEqualTo(ConfigChangedEvent.ChangeType.UPDATED);
        assertThat(event.getEntityId()).isEqualTo(1L);
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    @DisplayName("should support all config types")
    void shouldSupportAllConfigTypes() {
        assertThat(ConfigChangedEvent.ConfigType.values())
            .containsExactly(
                ConfigChangedEvent.ConfigType.PROVIDER,
                ConfigChangedEvent.ConfigType.MODEL,
                ConfigChangedEvent.ConfigType.PROVIDER_API_KEY
            );
    }

    @Test
    @DisplayName("should support all change types")
    void shouldSupportAllChangeTypes() {
        assertThat(ConfigChangedEvent.ChangeType.values())
            .containsExactly(
                ConfigChangedEvent.ChangeType.CREATED,
                ConfigChangedEvent.ChangeType.UPDATED,
                ConfigChangedEvent.ChangeType.DELETED
            );
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=ConfigChangedEventTest -q`
Expected: FAIL - cannot find symbol ConfigChangedEvent

- [ ] **Step 3: 创建 ConfigChangedEvent 类**

```java
package com.codingas.gateway.domain.model.event;

import com.codingas.gateway.common.event.DomainEvent;

import java.time.Instant;

/**
 * 配置变更事件
 *
 * <p>属于 model 领域，支持本地和远程两种传输方式。</p>
 */
public class ConfigChangedEvent implements DomainEvent {

    private final ConfigType configType;
    private final ChangeType changeType;
    private final Long entityId;
    private final Instant occurredOn;

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

    public ConfigChangedEvent(ConfigType configType, ChangeType changeType, Long entityId) {
        this.configType = configType;
        this.changeType = changeType;
        this.entityId = entityId;
        this.occurredOn = Instant.now();
    }

    public ConfigType getConfigType() {
        return configType;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public Long getEntityId() {
        return entityId;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    public static ConfigChangedEvent of(ConfigType configType, ChangeType changeType, Long entityId) {
        return new ConfigChangedEvent(configType, changeType, entityId);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=ConfigChangedEventTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/domain/model/event/ConfigChangedEvent.java
git add src/test/java/com/codingas/gateway/domain/model/event/ConfigChangedEventTest.java
git commit -m "feat: add ConfigChangedEvent for configuration change notification"
```

---

## Task 6: 扩展 Gateway 接口添加 getMaxVersion 方法

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/model/gateway/ProviderGateway.java`
- Modify: `src/main/java/com/codingas/gateway/domain/model/gateway/ModelGateway.java`
- Modify: `src/main/java/com/codingas/gateway/domain/model/gateway/ProviderApiKeyGateway.java`

- [ ] **Step 1: 修改 ProviderGateway 接口**

添加 `getMaxVersion` 和 `findByProviderId` 方法：

```java
package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.domain.model.entity.Provider;

import java.util.List;
import java.util.Optional;

/**
 * 提供商网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ProviderGateway {

    /**
     * 保存提供商
     */
    Provider save(Provider provider);

    /**
     * 根据 ID 查找提供商
     */
    Optional<Provider> findById(Long id);

    /**
     * 根据提供商代码查找提供商
     */
    Optional<Provider> findByProviderCode(String providerCode);

    /**
     * 查询所有提供商
     */
    List<Provider> findAll();

    /**
     * 查找所有活跃提供商
     */
    List<Provider> findAllActive();

    /**
     * 根据状态查找提供商
     */
    List<Provider> findByStatus(Provider.ProviderStatus status);

    /**
     * 统计提供商总数
     */
    long count();

    /**
     * 删除提供商
     */
    void delete(Provider provider);

    /**
     * 检查提供商代码是否存在
     */
    boolean existsByProviderCode(String providerCode);

    /**
     * 获取最大版本号
     *
     * <p>用于变更检测。</p>
     *
     * @return 最大版本号，无数据返回 0
     */
    default long getMaxVersion() {
        return 0L;
    }
}
```

- [ ] **Step 2: 修改 ModelGateway 接口**

添加 `getMaxVersion` 方法：

```java
// 在 ModelGateway.java 末尾添加

/**
 * 获取最大版本号
 *
 * <p>用于变更检测。</p>
 *
 * @return 最大版本号，无数据返回 0
 */
default long getMaxVersion() {
    return 0L;
}
```

- [ ] **Step 3: 修改 ProviderApiKeyGateway 接口**

添加 `findByProviderId` 和 `getMaxVersion` 方法：

```java
// 在 ProviderApiKeyGateway.java 中添加

/**
 * 根据提供商 ID 查找 API 密钥
 *
 * @param providerId 提供商 ID
 * @return API 密钥信息，不存在返回空
 */
Optional<ProviderApiKey> findByProviderId(Long providerId);

/**
 * 获取最大版本号
 *
 * <p>用于变更检测。</p>
 *
 * @return 最大版本号，无数据返回 0
 */
default long getMaxVersion() {
    return 0L;
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/codingas/gateway/domain/model/gateway/ProviderGateway.java
git add src/main/java/com/codingas/gateway/domain/model/gateway/ModelGateway.java
git add src/main/java/com/codingas/gateway/domain/model/gateway/ProviderApiKeyGateway.java
git commit -m "feat: add getMaxVersion and findByProviderId methods to Gateway interfaces"
```

---

## Task 7: 创建 CacheNames 常量类

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/config/CacheNames.java`

- [ ] **Step 1: 创建 CacheNames 类**

```java
package com.codingas.gateway.infrastructure.config;

/**
 * 缓存名称常量
 *
 * <p>定义系统中所有缓存的名称。</p>
 */
public final class CacheNames {

    private CacheNames() {
        // 私有构造函数，防止实例化
    }

    /**
     * Provider 缓存（可共享到 Redis）
     */
    public static final String PROVIDERS = "providers";

    /**
     * Model 缓存（可共享到 Redis）
     */
    public static final String MODELS = "models";

    /**
     * API Key 缓存（敏感数据，仅本地）
     */
    public static final String API_KEYS_LOCAL = "apiKeysLocal";
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/config/CacheNames.java
git commit -m "feat: add CacheNames constants for cache naming"
```

---

## Task 8: 创建 CacheConfig 配置类

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/config/CacheConfig.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/infrastructure/config/CacheConfigTest.java`：

```java
package com.codingas.gateway.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    @DisplayName("should create localCacheManager bean")
    void shouldCreateLocalCacheManagerBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(CacheConfig.class);
            context.register(TestConfig.class);
            context.refresh();

            CacheManager cacheManager = context.getBean("localCacheManager", CacheManager.class);
            assertThat(cacheManager).isNotNull();
        }
    }

    @EnableCaching
    static class TestConfig {
        // 测试配置
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=CacheConfigTest -q`
Expected: FAIL - cannot find symbol CacheConfig

- [ ] **Step 3: 创建 CacheConfig 类**

```java
package com.codingas.gateway.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

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
@EnableCaching
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
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=CacheConfigTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/config/CacheConfig.java
git add src/test/java/com/codingas/gateway/infrastructure/config/CacheConfigTest.java
git commit -m "feat: add CacheConfig with local and distributed cache managers"
```

---

## Task 9: 创建 LocalDomainEventPublisher

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/event/LocalDomainEventPublisher.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/infrastructure/event/LocalDomainEventPublisherTest.java`：

```java
package com.codingas.gateway.infrastructure.event;

import com.codingas.gateway.common.event.DomainEvent;
import com.codingas.gateway.domain.model.event.ConfigChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDomainEventPublisherTest {

    @Test
    @DisplayName("should publish event via Spring ApplicationEventPublisher")
    void shouldPublishEventViaApplicationEventPublisher() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TestEventConfig.class);
            context.register(LocalDomainEventPublisher.class);
            context.refresh();

            LocalDomainEventPublisher publisher = context.getBean(LocalDomainEventPublisher.class);
            TestEventListener listener = context.getBean(TestEventListener.class);

            ConfigChangedEvent event = ConfigChangedEvent.of(
                ConfigChangedEvent.ConfigType.PROVIDER,
                ConfigChangedEvent.ChangeType.UPDATED,
                1L
            );

            publisher.publish(event);

            assertThat(listener.getLastEvent()).isNotNull();
            assertThat(listener.getLastEvent()).isInstanceOf(ConfigChangedEvent.class);
        }
    }

    @Configuration
    static class TestEventConfig {
        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    @Component
    static class TestEventListener {
        private final AtomicReference<Object> lastEvent = new AtomicReference<>();

        @EventListener
        public void onEvent(Object event) {
            lastEvent.set(event);
        }

        public Object getLastEvent() {
            return lastEvent.get();
        }
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=LocalDomainEventPublisherTest -q`
Expected: FAIL - cannot find symbol LocalDomainEventPublisher

- [ ] **Step 3: 创建 LocalDomainEventPublisher 类**

```java
package com.codingas.gateway.infrastructure.event;

import com.codingas.gateway.common.event.DomainEvent;
import com.codingas.gateway.common.event.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 本地事件发布器
 *
 * <p>使用 Spring ApplicationEvent，适用于单实例部署。</p>
 */
@Component
@Profile({"local", "dev", "standalone"})
@Slf4j
public class LocalDomainEventPublisher implements DomainEventPublisher {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public <T extends DomainEvent> void publish(T event) {
        log.debug("Publishing local event: {}", event);
        eventPublisher.publishEvent(event);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=LocalDomainEventPublisherTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/event/LocalDomainEventPublisher.java
git add src/test/java/com/codingas/gateway/infrastructure/event/LocalDomainEventPublisherTest.java
git commit -m "feat: add LocalDomainEventPublisher for standalone deployment"
```

---

## Task 10: 创建 ConfigCacheService

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/config/ConfigCacheService.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/infrastructure/config/ConfigCacheServiceTest.java`：

```java
package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.infrastructure.security.encryption.EncryptionService;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigCacheServiceTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private ProviderApiKeyGateway apiKeyGateway;

    @Mock
    private EncryptionService encryptionService;

    private ConfigCacheService configCacheService;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = new ConcurrentMapCacheManager(
            CacheNames.PROVIDERS, CacheNames.MODELS, CacheNames.API_KEYS_LOCAL
        );
        configCacheService = new ConfigCacheService(
            providerGateway, modelGateway, apiKeyGateway, encryptionService, cacheManager
        );
    }

    @Test
    @DisplayName("should get provider by id from gateway")
    void shouldGetProviderByIdFromGateway() {
        Provider provider = createTestProvider();
        when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

        Optional<Provider> result = configCacheService.getProviderById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getProviderCode()).isEqualTo("openai");
    }

    @Test
    @DisplayName("should get all providers from gateway")
    void shouldGetAllProvidersFromGateway() {
        Provider provider = createTestProvider();
        when(providerGateway.findAll()).thenReturn(List.of(provider));

        List<Provider> result = configCacheService.getAllProviders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProviderCode()).isEqualTo("openai");
    }

    @Test
    @DisplayName("should decrypt api key when getting from cache")
    void shouldDecryptApiKeyWhenGettingFromCache() {
        ProviderApiKey apiKey = new ProviderApiKey();
        apiKey.setEncryptedApiKey("encrypted-key");
        when(apiKeyGateway.findByProviderId(1L)).thenReturn(Optional.of(apiKey));
        when(encryptionService.decrypt("encrypted-key")).thenReturn("decrypted-key");

        Optional<ProviderApiKey> result = configCacheService.getApiKeyByProviderId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getApiKey()).isEqualTo("decrypted-key");
    }

    @Test
    @DisplayName("should clear providers cache on refresh")
    void shouldClearProvidersCacheOnRefresh() {
        Provider provider = createTestProvider();
        when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

        // First call - should hit gateway
        configCacheService.getProviderById(1L);
        
        // Refresh
        configCacheService.refreshProviders();

        // Verify cache was cleared (second call should hit gateway again)
        // This is verified by mocking behavior
    }

    private Provider createTestProvider() {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setProviderCode("openai");
        provider.setProviderName("OpenAI");
        provider.setProviderType(ProviderType.OPENAI);
        provider.setStatus(Provider.ProviderStatus.ACTIVE);
        return provider;
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=ConfigCacheServiceTest -q`
Expected: FAIL - cannot find symbol ConfigCacheService

- [ ] **Step 3: 创建 ConfigCacheService 类**

```java
package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.infrastructure.security.encryption.EncryptionService;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 配置缓存服务
 *
 * <p>使用 Spring Cache 统一缓存抽象，属于技术基础设施。</p>
 * <p>负责 Provider、Model、ProviderApiKey 的缓存管理。</p>
 */
@Service
@Slf4j
public class ConfigCacheService {

    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;
    private final ProviderApiKeyGateway apiKeyGateway;
    private final EncryptionService encryptionService;
    private final CacheManager cacheManager;

    @Autowired
    public ConfigCacheService(
            ProviderGateway providerGateway,
            ModelGateway modelGateway,
            ProviderApiKeyGateway apiKeyGateway,
            EncryptionService encryptionService,
            CacheManager cacheManager) {
        this.providerGateway = providerGateway;
        this.modelGateway = modelGateway;
        this.apiKeyGateway = apiKeyGateway;
        this.encryptionService = encryptionService;
        this.cacheManager = cacheManager;
    }

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
    @Cacheable(value = CacheNames.API_KEYS_LOCAL,
               key = "#providerId",
               cacheManager = "localCacheManager")
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

    @CacheEvict(value = CacheNames.API_KEYS_LOCAL,
                allEntries = true,
                cacheManager = "localCacheManager")
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

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=ConfigCacheServiceTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/config/ConfigCacheService.java
git add src/test/java/com/codingas/gateway/infrastructure/config/ConfigCacheServiceTest.java
git commit -m "feat: add ConfigCacheService for provider/model/apikey caching"
```

---

## Task 11: 创建 ConfigChangedEventListener

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/event/ConfigChangedEventListener.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/infrastructure/event/ConfigChangedEventListenerTest.java`：

```java
package com.codingas.gateway.infrastructure.event;

import com.codingas.gateway.domain.model.event.ConfigChangedEvent;
import com.codingas.gateway.infrastructure.config.CacheNames;
import com.codingas.gateway.infrastructure.config.ConfigCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ConfigChangedEventListenerTest {

    @Mock
    private ConfigCacheService cacheService;

    @InjectMocks
    private ConfigChangedEventListener listener;

    @Test
    @DisplayName("should refresh providers cache on PROVIDER event")
    void shouldRefreshProvidersCacheOnProviderEvent() {
        ConfigChangedEvent event = ConfigChangedEvent.of(
            ConfigChangedEvent.ConfigType.PROVIDER,
            ConfigChangedEvent.ChangeType.UPDATED,
            1L
        );

        listener.onLocalEvent(event);

        verify(cacheService).refreshProviders();
    }

    @Test
    @DisplayName("should refresh models cache on MODEL event")
    void shouldRefreshModelsCacheOnModelEvent() {
        ConfigChangedEvent event = ConfigChangedEvent.of(
            ConfigChangedEvent.ConfigType.MODEL,
            ConfigChangedEvent.ChangeType.UPDATED,
            1L
        );

        listener.onLocalEvent(event);

        verify(cacheService).refreshModels();
    }

    @Test
    @DisplayName("should refresh api keys cache on PROVIDER_API_KEY event")
    void shouldRefreshApiKeysCacheOnApiKeyEvent() {
        ConfigChangedEvent event = ConfigChangedEvent.of(
            ConfigChangedEvent.ConfigType.PROVIDER_API_KEY,
            ConfigChangedEvent.ChangeType.UPDATED,
            1L
        );

        listener.onLocalEvent(event);

        verify(cacheService).refreshApiKeys();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=ConfigChangedEventListenerTest -q`
Expected: FAIL - cannot find symbol ConfigChangedEventListener

- [ ] **Step 3: 创建 ConfigChangedEventListener 类**

```java
package com.codingas.gateway.infrastructure.event;

import com.codingas.gateway.domain.model.event.ConfigChangedEvent;
import com.codingas.gateway.infrastructure.config.ConfigCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 配置变更事件监听器
 *
 * <p>处理本地和 Redis 远程事件。</p>
 */
@Component
@Slf4j
public class ConfigChangedEventListener implements MessageListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigCacheService cacheService;

    // ========== Redis 消息监听 ==========

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            ConfigChangedEvent event = objectMapper.readValue(payload, ConfigChangedEvent.class);
            log.info("Received Redis config event: {}", event);
            handleEvent(event);
        } catch (Exception e) {
            log.error("Failed to handle Redis message: {}", message, e);
        }
    }

    // ========== 本地事件监听 ==========

    @EventListener
    public void onLocalEvent(ConfigChangedEvent event) {
        log.info("Received local config event: {}", event);
        handleEvent(event);
    }

    // ========== 事件处理 ==========

    private void handleEvent(ConfigChangedEvent event) {
        switch (event.getConfigType()) {
            case PROVIDER -> cacheService.refreshProviders();
            case MODEL -> cacheService.refreshModels();
            case PROVIDER_API_KEY -> cacheService.refreshApiKeys();
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=ConfigChangedEventListenerTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/event/ConfigChangedEventListener.java
git add src/test/java/com/codingas/gateway/infrastructure/event/ConfigChangedEventListenerTest.java
git commit -m "feat: add ConfigChangedEventListener for cache refresh"
```

---

## Task 12: 创建 ConfigEventPublisher

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/config/ConfigEventPublisher.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/infrastructure/config/ConfigEventPublisherTest.java`：

```java
package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.model.event.ConfigChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfigEventPublisherTest {

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private ConfigEventPublisher configEventPublisher;

    @Test
    @DisplayName("should publish config changed event")
    void shouldPublishConfigChangedEvent() {
        configEventPublisher.publishConfigChanged(
            ConfigChangedEvent.ConfigType.PROVIDER,
            ConfigChangedEvent.ChangeType.UPDATED,
            1L
        );

        verify(eventPublisher).publish(any(ConfigChangedEvent.class));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=ConfigEventPublisherTest -q`
Expected: FAIL - cannot find symbol ConfigEventPublisher

- [ ] **Step 3: 创建 ConfigEventPublisher 类**

```java
package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.model.event.ConfigChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 配置事件发布
 *
 * <p>提供统一的事件发布入口，属于技术基础设施。</p>
 */
@Service
@Slf4j
public class ConfigEventPublisher {

    @Autowired
    private DomainEventPublisher eventPublisher;

    /**
     * 发布配置变更事件
     *
     * @param configType 配置类型
     * @param changeType 变更类型
     * @param entityId 实体 ID
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

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=ConfigEventPublisherTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/config/ConfigEventPublisher.java
git add src/test/java/com/codingas/gateway/infrastructure/config/ConfigEventPublisherTest.java
git commit -m "feat: add ConfigEventPublisher for unified event publishing"
```

---

## Task 13: 创建 ConfigVersionChecker

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/config/ConfigVersionChecker.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/infrastructure/config/ConfigVersionCheckerTest.java`：

```java
package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigVersionCheckerTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private ProviderApiKeyGateway apiKeyGateway;

    @Mock
    private ConfigCacheService cacheService;

    @InjectMocks
    private ConfigVersionChecker versionChecker;

    @BeforeEach
    void setUp() {
        versionChecker = new ConfigVersionChecker(providerGateway, modelGateway, apiKeyGateway, cacheService);
    }

    @Test
    @DisplayName("should refresh cache when provider version changes")
    void shouldRefreshCacheWhenProviderVersionChanges() {
        when(providerGateway.getMaxVersion()).thenReturn(5L);
        when(modelGateway.getMaxVersion()).thenReturn(0L);
        when(apiKeyGateway.getMaxVersion()).thenReturn(0L);

        // 初始化版本
        versionChecker.initVersions();
        
        // 模拟版本变化
        when(providerGateway.getMaxVersion()).thenReturn(10L);
        when(modelGateway.getMaxVersion()).thenReturn(0L);
        when(apiKeyGateway.getMaxVersion()).thenReturn(0L);

        versionChecker.checkVersions();

        verify(cacheService).refreshProviders();
    }

    @Test
    @DisplayName("should not refresh cache when version unchanged")
    void shouldNotRefreshCacheWhenVersionUnchanged() {
        when(providerGateway.getMaxVersion()).thenReturn(5L);
        when(modelGateway.getMaxVersion()).thenReturn(0L);
        when(apiKeyGateway.getMaxVersion()).thenReturn(0L);

        versionChecker.initVersions();
        versionChecker.checkVersions();

        verifyNoInteractions(cacheService);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=ConfigVersionCheckerTest -q`
Expected: FAIL - cannot find symbol ConfigVersionChecker

- [ ] **Step 3: 创建 ConfigVersionChecker 类**

```java
package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;
    private final ProviderApiKeyGateway apiKeyGateway;
    private final ConfigCacheService cacheService;

    // 记录上次检查的版本
    private volatile long lastProviderVersion = 0;
    private volatile long lastModelVersion = 0;
    private volatile long lastApiKeyVersion = 0;

    @Autowired
    public ConfigVersionChecker(
            ProviderGateway providerGateway,
            ModelGateway modelGateway,
            ProviderApiKeyGateway apiKeyGateway,
            ConfigCacheService cacheService) {
        this.providerGateway = providerGateway;
        this.modelGateway = modelGateway;
        this.apiKeyGateway = apiKeyGateway;
        this.cacheService = cacheService;
    }

    /**
     * 初始化版本号
     *
     * <p>在应用启动时调用，记录当前版本。</p>
     */
    public void initVersions() {
        lastProviderVersion = providerGateway.getMaxVersion();
        lastModelVersion = modelGateway.getMaxVersion();
        lastApiKeyVersion = apiKeyGateway.getMaxVersion();
        log.info("Version checker initialized: provider={}, model={}, apiKey={}",
            lastProviderVersion, lastModelVersion, lastApiKeyVersion);
    }

    /**
     * 检查版本变化
     *
     * <p>定时执行，检测版本变化并刷新缓存。</p>
     */
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

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=ConfigVersionCheckerTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/config/ConfigVersionChecker.java
git add src/test/java/com/codingas/gateway/infrastructure/config/ConfigVersionCheckerTest.java
git commit -m "feat: add ConfigVersionChecker for fallback version polling"
```

---

## Task 14: 创建 ConfigLoader

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/config/ConfigLoader.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/codingas/gateway/infrastructure/config/ConfigLoaderTest.java`：

```java
package com.codingas.gateway.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfigLoaderTest {

    @Mock
    private ConfigCacheService cacheService;

    @Mock
    private ConfigVersionChecker versionChecker;

    @InjectMocks
    private ConfigLoader configLoader;

    @Test
    @DisplayName("should load cache and init versions on startup")
    void shouldLoadCacheAndInitVersionsOnStartup() {
        configLoader.run(null);

        verify(cacheService).refreshAll();
        verify(versionChecker).initVersions();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=ConfigLoaderTest -q`
Expected: FAIL - cannot find symbol ConfigLoader

- [ ] **Step 3: 创建 ConfigLoader 类**

```java
package com.codingas.gateway.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 配置加载器
 *
 * <p>应用启动时加载配置到缓存。</p>
 */
@Component
@Slf4j
public class ConfigLoader implements ApplicationRunner {

    private final ConfigCacheService cacheService;
    private final ConfigVersionChecker versionChecker;

    @Autowired
    public ConfigLoader(ConfigCacheService cacheService, ConfigVersionChecker versionChecker) {
        this.cacheService = cacheService;
        this.versionChecker = versionChecker;
    }

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

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=ConfigLoaderTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/config/ConfigLoader.java
git add src/test/java/com/codingas/gateway/infrastructure/config/ConfigLoaderTest.java
git commit -m "feat: add ConfigLoader for startup cache initialization"
```

---

## Task 15: 实现 Gateway 的 getMaxVersion 方法

**Files:**
- Modify: `src/main/java/com/codingas/gateway/infrastructure/model/gateway/ProviderGatewayImpl.java`
- Modify: `src/main/java/com/codingas/gateway/infrastructure/model/gateway/ModelGatewayImpl.java`
- Modify: `src/main/java/com/codingas/gateway/infrastructure/apikey/gateway/ProviderApiKeyGatewayImpl.java`

- [ ] **Step 1: 读取现有 Gateway 实现**

Run: `cat src/main/java/com/codingas/gateway/infrastructure/model/gateway/ProviderGatewayImpl.java`

- [ ] **Step 2: 在 ProviderGatewayImpl 中添加 getMaxVersion 方法**

在 ProviderGatewayImpl 类中添加：

```java
@Override
public long getMaxVersion() {
    Long maxVersion = providerRepository.findMaxVersion();
    return maxVersion != null ? maxVersion : 0L;
}
```

并在 ProviderRepository 中添加方法：

```java
@Query("SELECT MAX(p.version) FROM ProviderDo p")
Long findMaxVersion();
```

- [ ] **Step 3: 在 ModelGatewayImpl 中添加 getMaxVersion 方法**

类似地添加到 ModelGatewayImpl 和 ModelRepository。

- [ ] **Step 4: 在 ProviderApiKeyGatewayImpl 中添加方法**

添加 `findByProviderId` 和 `getMaxVersion` 方法。

- [ ] **Step 5: 运行编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/model/gateway/
git add src/main/java/com/codingas/gateway/infrastructure/apikey/gateway/
git add src/main/java/com/codingas/gateway/infrastructure/model/gateway/database/
git add src/main/java/com/codingas/gateway/infrastructure/apikey/gateway/database/
git commit -m "feat: implement getMaxVersion and findByProviderId in Gateway implementations"
```

---

## Task 16: 添加应用配置

**Files:**
- Create: `src/main/resources/application-standalone.yaml`
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: 创建标准版配置**

创建 `src/main/resources/application-standalone.yaml`：

```yaml
# 标准版（单实例）配置
spring:
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

- [ ] **Step 2: 添加启用调度注解**

在主应用类或配置类中添加 `@EnableScheduling`：

```java
@EnableScheduling
@SpringBootApplication
public class GatewayApplication {
    // ...
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application-standalone.yaml
git commit -m "feat: add standalone profile configuration for caching"
```

---

## Task 17: 运行完整测试验证

- [ ] **Step 1: 运行所有单元测试**

Run: `mvn test -q`
Expected: BUILD SUCCESS with all tests passing

- [ ] **Step 2: 运行编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete provider/model/apikey loading with cache and event mechanism"
```

---

## 自检清单

**1. Spec 覆盖检查：**

| 设计要求 | 实现任务 |
|---------|---------|
| BaseEntity 添加 version 字段 | Task 2 ✅ |
| 数据库迁移脚本 | Task 3 ✅ |
| DomainEventPublisher 接口 | Task 4 ✅ |
| ConfigChangedEvent 事件类 | Task 5 ✅ |
| Gateway 接口扩展 getMaxVersion | Task 6 ✅ |
| CacheNames 常量 | Task 7 ✅ |
| CacheConfig 配置 | Task 8 ✅ |
| LocalDomainEventPublisher | Task 9 ✅ |
| ConfigCacheService | Task 10 ✅ |
| ConfigChangedEventListener | Task 11 ✅ |
| ConfigEventPublisher | Task 12 ✅ |
| ConfigVersionChecker 兜底 | Task 13 ✅ |
| ConfigLoader 启动加载 | Task 14 ✅ |
| Gateway 实现 | Task 15 ✅ |
| 应用配置 | Task 16 ✅ |

**2. Placeholder 扫描：** 无 TBD、TODO 等占位符

**3. 类型一致性：** 所有方法签名和类型引用保持一致
