# DataInitializer 完善方案 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 DataInitializer 守卫条件错误，分离 admin 内置用户与演示数据，通过配置属性控制演示数据启用

**Architecture:** 保持 DataInitializer 单一类，run() 方法分三阶段执行。利用现有 GatewayProperties 模式新增 InitProperties 配置项，通过 application yml profile 控制开关。不改动 BuiltinDataLoader，保留 provider/model 创建作为后备。

**Tech Stack:** Java 21 + Spring Boot 3.5.x + @ConfigurationProperties + CommandLineRunner

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `gateway-boot/.../infrastructure/config/GatewayProperties.java` | 修改 | 新增 `InitProperties` 内部类 |
| `gateway-boot/src/main/resources/application.yml` | 修改 | 添加 `gateway.init.demo-data-enabled: false` 默认 |
| `gateway-boot/src/main/resources/application-local.yml` | 修改 | 添加 `gateway.init.demo-data-enabled: true` |
| `gateway-boot/src/main/resources/application-dev.yml` | 修改 | 添加 `gateway.init.demo-data-enabled: true` |
| `gateway-boot/.../application/init/DataInitializer.java` | 修改 | 三阶段重构核心改造 |
| `gateway-boot/src/test/.../application/init/DataInitializerTest.java` | 创建 | 单元测试 |

---

### Task 1: GatewayProperties 新增 InitProperties

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/config/GatewayProperties.java`

- [ ] **Step 1: 在 GatewayProperties 中新增 InitProperties 内部类**

在 `RouterProperties` 内部类之后添加：

```java
@Getter
@Setter
public static class InitProperties {
    private boolean demoDataEnabled = false;
}
```

- [ ] **Step 2: 在 GatewayProperties 中添加 init 属性字段**

在 `router` 字段之后添加：

```java
private InitProperties init = new InitProperties();
```

- [ ] **Step 3: 编译确保通过**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/config/GatewayProperties.java
git commit -m "refactor: GatewayProperties 新增 InitProperties 配置项"
```

---

### Task 2: application yml 配置变更

**Files:**
- Modify: `gateway-boot/src/main/resources/application.yml`
- Modify: `gateway-boot/src/main/resources/application-local.yml`
- Modify: `gateway-boot/src/main/resources/application-dev.yml`

- [ ] **Step 1: application.yml 添加默认值（false）**

在 `application.yml` 的 `gateway:` 块末尾（`logging` 配置之前）添加：

```yaml
  init:
    demo-data-enabled: false
```

- [ ] **Step 2: application-local.yml 开启演示数据**

在 `application-local.yml` 末尾添加：

```yaml
gateway:
  init:
    demo-data-enabled: true
```

- [ ] **Step 3: application-dev.yml 开启演示数据**

在 `application-dev.yml` 末尾添加：

```yaml
gateway:
  init:
    demo-data-enabled: true
```

- [ ] **Step 4: Commit**

```bash
git add gateway-boot/src/main/resources/application.yml \
      gateway-boot/src/main/resources/application-local.yml \
      gateway-boot/src/main/resources/application-dev.yml
git commit -m "config: 添加 gateway.init.demo-data-enabled 配置项"
```

---

### Task 3: DataInitializer 三阶段重构

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/init/DataInitializer.java`

- [ ] **Step 1: 注入 demoDataEnabled 配置**

在 `PasswordEncoder` 依赖之后添加：

```java
/**
 * 演示数据开关（由配置 gateway.init.demo-data-enabled 控制）
 */
private final boolean demoDataEnabled;
```

将 `demoDataEnabled` 通过构造器注入。由于类使用 `@RequiredArgsConstructor`，将字段声明为 `@Value` 会导致冲突。改为手动添加构造器（或直接在字段上使用 `@Value`）。

更好的方式——利用 `GatewayProperties`：

```java
private final GatewayProperties gatewayProperties;
```

在 `run()` 中使用 `gatewayProperties.getInit().isDemoDataEnabled()`。

- [ ] **Step 2: 重写 run() 方法**

将原 `run()` 方法（第 91-109 行）替换为：

```java
@Override
@Transactional
public void run(String... args) {
    // Phase 1: 基础设施 — 确保 admin 内置用户存在（无条件执行）
    ensureAdminUser();

    // Phase 2: 演示开关检查
    if (!gatewayProperties.getInit().isDemoDataEnabled()) {
        log.info("演示数据初始化已禁用 (demo-data-enabled=false)");
        return;
    }

    // Phase 3: 幂等守卫 — 演示数据是否已初始化
    if (userGateway.findByUsername("test1").isPresent()) {
        log.info("演示数据已存在，跳过初始化");
        return;
    }

    log.info("Initializing demo data...");

    // Phase 4: 执行初始化
    // 后备：如果 BuiltinDataLoader 未执行，补充创建供应商和模型
    if (providerGateway.count() == 0) {
        log.info("BuiltinDataLoader 未加载供应商数据，执行后备初始化");
        initializeProviders();
        initializeModels();
    }

    Map<String, Channel> channels = initializeChannels();
    List<Team> teams = initializeTeams();
    initializeTeamChannelAssignments(teams, channels);
    List<User> users = initializeDemoUsers();
    initializeUserTeamAssignments(users, teams);
    initializeApiKeys(users);
}
```

- [ ] **Step 3: 新增 ensureAdminUser() 方法**

在 `run()` 方法之后添加：

```java
/**
 * 确保 admin 内置用户存在
 *
 * <p>admin 是系统基础设施用户，生产环境和开发环境都需要。
 * 如果 admin 已存在则跳过，不存在则创建。</p>
 */
private void ensureAdminUser() {
    if (userGateway.findByUsername("admin").isPresent()) {
        return;
    }
    User admin = createUser("admin", "admin@example.com", ADMIN_ROLE, true);
    log.info("Admin 内置用户已创建 (id={})", admin.getId());
}
```

- [ ] **Step 4: 新增 initializeDemoUsers() 方法**

将原 `initializeUsers()` 方法（第 418-436 行）拆分为：

a) 保留 `ensureAdminUser()`（上面已写）

b) 新增 `initializeDemoUsers()`：

```java
/**
 * 初始化演示用户
 *
 * <p>创建 test1-test10 共 10 个测试用户，密码与用户名相同。</p>
 */
private List<User> initializeDemoUsers() {
    log.info("Step 6: Initializing demo users...");

    List<User> users = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
        String username = "test" + i;
        String email = username + "@example.com";
        users.add(createUser(username, email, USER_ROLE, false));
    }

    log.info("  Created {} demo users (test1-test10)", users.size());
    return users;
}
```

- [ ] **Step 5: 更新日志摘要方法**

原 `logInitializationSummary` 引用的是 `initializeUsers()`，改为 `initializeDemoUsers()` 后检查其中的日志引用。更新第 521 行：

```java
log.info("  Users: {} (admin + test1-test10)", userCount);
```

（不需要改，因为方法签名不变，只是内部实现变化）

但需要删除或更新第 533 行的登录提示，改为关于演示数据的提示：

```
log.info("Demo accounts:");
log.info("  Admin - Username: admin, Password: admin, Role: ADMIN");
log.info("  Test users - Username: test1 ~ test10, Password: same as username, Role: USER");
```

改为：

```
log.info("Demo accounts:");
log.info("  Admin (built-in) - Username: admin, Password: admin, Role: ADMIN");
log.info("  Demo users - Username: test1 ~ test10, Password: same as username, Role: USER");
```

- [ ] **Step 6: 编译确保通过**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/init/DataInitializer.java
git commit -m "refactor: DataInitializer 三阶段重构 - 分离 admin/demo、修复守卫、添加配置开关"
```

---

### Task 4: 单元测试

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/init/DataInitializerTest.java`

由于 DataInitializer 是 `CommandLineRunner` 且依赖多个 Gateway 和 Spring 上下文，测试采用 Spring Boot 测试切片方式。

- [ ] **Step 1: 创建测试配置文件**

创建 `gateway-boot/src/test/resources/application-test.yml`（禁用 flyway 和 demo 数据）：

```yaml
gateway:
  init:
    demo-data-enabled: false

spring:
  jpa:
    show-sql: false
  flyway:
    enabled: false
```

创建 `gateway-boot/src/test/resources/application-test-demo.yml`（启用演示数据）：

```yaml
gateway:
  init:
    demo-data-enabled: true

spring:
  jpa:
    show-sql: false
  flyway:
    enabled: false
```

- [ ] **Step 2: 编写 DataInitializerTest**

创建 `gateway-boot/src/test/java/com/codingas/gateway/application/init/DataInitializerTest.java`：

```java
package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test-demo")
@Transactional
class DataInitializerTest {

    @Autowired
    private UserGateway userGateway;

    @Autowired
    private ProviderGateway providerGateway;

    @Autowired
    private TeamGateway teamGateway;

    @Autowired
    private DataInitializer dataInitializer;

    // ===== Phase 1: Admin 内置用户 =====

    @Test
    @DisplayName("run 后 admin 用户应存在")
    void adminUserShouldExist() {
        dataInitializer.run();
        assertTrue(userGateway.findByUsername("admin").isPresent());
    }

    @Test
    @DisplayName("admin 用户应标记为 builtin")
    void adminUserShouldBeBuiltin() {
        dataInitializer.run();
        User admin = userGateway.findByUsername("admin").get();
        assertTrue(admin.isBuiltin());
    }

    @Test
    @DisplayName("admin 用户角色应为 ADMIN")
    void adminUserShouldHaveAdminRole() {
        dataInitializer.run();
        User admin = userGateway.findByUsername("admin").get();
        assertEquals("ADMIN", admin.getRole());
    }

    // ===== Phase 3/4: Demo 数据 =====

    @Test
    @DisplayName("应创建 test1-test10 共 10 个演示用户")
    void shouldCreateTenDemoUsers() {
        dataInitializer.run();
        for (int i = 1; i <= 10; i++) {
            String username = "test" + i;
            assertTrue(userGateway.findByUsername(username).isPresent(),
                "演示用户 " + username + " 应存在");
        }
    }

    @Test
    @DisplayName("演示用户不应标记为 builtin")
    void demoUserShouldNotBeBuiltin() {
        dataInitializer.run();
        User test1 = userGateway.findByUsername("test1").get();
        assertFalse(test1.isBuiltin());
    }

    @Test
    @DisplayName("应创建 4 个演示团队（default, dev, product, openclaw）")
    void shouldCreateFourDemoTeams() {
        dataInitializer.run();
        assertTrue(teamGateway.existsByName("default"));
        assertTrue(teamGateway.existsByName("dev"));
        assertTrue(teamGateway.existsByName("product"));
        assertTrue(teamGateway.existsByName("openclaw"));
    }

    @Test
    @DisplayName("应创建供应商和模型（后备逻辑）")
    void shouldCreateProvidersAndModels() {
        dataInitializer.run();
        assertTrue(providerGateway.count() > 0);
    }

    // ===== 幂等性 =====

    @Nested
    @DisplayName("幂等性")
    class Idempotency {

        @Test
        @DisplayName("重复调用不应创建重复的 admin")
        void repeatedRunShouldNotDuplicateAdmin() {
            dataInitializer.run();
            long adminCount1 = userGateway.findByUsername("admin").stream().count();
            dataInitializer.run();
            long adminCount2 = userGateway.findByUsername("admin").stream().count();
            assertEquals(adminCount1, adminCount2);
        }

        @Test
        @DisplayName("重复调用不应创建重复的演示用户")
        void repeatedRunShouldNotDuplicateDemoUsers() {
            dataInitializer.run();
            long count1 = userGateway.count();
            dataInitializer.run();
            long count2 = userGateway.count();
            assertEquals(count1, count2);
        }

        @Test
        @DisplayName("重复调用不应创建重复的团队")
        void repeatedRunShouldNotDuplicateTeams() {
            dataInitializer.run();
            long count1 = teamGateway.count();
            dataInitializer.run();
            long count2 = teamGateway.count();
            assertEquals(count1, count2);
        }
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `./mvnw test -pl gateway-boot -Dtest=DataInitializerTest -q`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/application/init/DataInitializerTest.java \
      gateway-boot/src/test/resources/application-test.yml \
      gateway-boot/src/test/resources/application-test-demo.yml
git commit -m "test: 添加 DataInitializerTest 覆盖 admin/demo/幂等性场景"
```

---

## 执行验证

所有任务完成后，执行最终验证：

```bash
# 编译检查
./mvnw compile -pl gateway-boot -q

# 测试
./mvnw test -pl gateway-boot -Dtest=DataInitializerTest -q

# 本地启动（默认 local profile，应看到演示数据初始化日志）
./mvnw spring-boot:run -pl gateway-boot
```

验证日志应包含：
1. `Admin 内置用户已创建`（首次启动）或跳过（已有数据）
2. `演示数据初始化已禁用`（如果 demo-data-enabled=false）
3. 或完整的演示数据初始化步骤日志