# P4 ArchUnit 模块级铁律 + DO 依赖清零 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 解除 P1 过渡态 freeze 基线，清零 DO 穿透耦合（UsageLogDo/AlertNotificationDo 改 ID 关联、StatsService 改端口调用），ArchUnit 升级为模块级铁律（含 P3 承接项：web 测试归位 + 协议插件 @Component 约束），全量回归。

**Architecture:** 各域核心 Gateway 新增 `count()` 端口（ProviderGateway/ModelGateway/ChannelGateway/UserGateway），data 模块 GatewayImpl 实现——StatsService 从注入 providerdata/iamdata Repository 改为注入各域核心 Gateway（端口调用）；audit-data 的 UsageLogDo 去掉 `@ManyToOne` 实体引用（UserDo/ProviderDo/ModelDo）改 `@Column Long`（保留 user_id/provider_id/model_id 列），alert-data 的 AlertNotificationDo 去掉 targetUser（UserDo）改 targetUserId——data 模块移除跨域 data 依赖；LayerDependencyTest 解除 freeze（违规清零后硬规则）+ 新增模块级规则（禁止反向依赖 boot/web、协议插件只依赖 SPI、starter 只依赖 autoconfigure+本域、协议插件包禁止 @Component 除 AutoConfiguration）；web 测试归位（boot 的 adapter 测试迁 gateway-web）。行为不变（统计口径/审计语义不变）。

**Tech Stack:** Java 21、Spring Boot 3.5.13、JPA、ArchUnit、Maven 多模块

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试；ArchUnit 测试是模块铁律的验证）
- 每任务独立提交，commit message 中文
- 行为不变：统计口径（Provider/Channel/Model/User count）、审计/告警语义不变
- **count 端口**（Task 2 新增，Task 3 使用）：
  ```java
  // provider 域（各自 Gateway 接口）
  long count();   // ProviderGateway / ModelGateway / ChannelGateway
  // iam 域
  long count();   // UserGateway
  ```
  对应 data 模块 `GatewayImpl` 用 `repository.count()` 实现；JPA `Repository.count()` 是 Spring Data 内置方法
- **StatsService 整改后依赖**：`gateway-stats` 核心只依赖 `gateway-provider`、`gateway-iam`、`gateway-common`（**移除** `gateway-provider-data`、`gateway-iam-data`）
- **UsageLogDo 整改**：去掉 3 个 `@ManyToOne`（user/provider/model），改 `@Column(name = "user_id"|"provider_id"|"model_id") private Long userId|providerId|modelId`（保留列名，审计表结构不变）；audit-data 移除 `gateway-provider-data`、`gateway-iam-data` 依赖
- **AlertNotificationDo 整改**：`@ManyToOne targetUser(UserDo)` → `@Column(name = "target_user_id") private Long targetUserId`；alert-data 移除 `gateway-iam-data` 依赖
- **ArchUnit 铁律**（LayerDependencyTest 升级，Task 5）：
  - 现有 3 条规则去 `freeze()`（硬规则）：NO_CORE_DEPENDS_BINDING_MODULES、NO_BINDING_CROSS_DOMAIN_DEPENDS、COMMON_NOT_DEPEND_ON_BUSINESS
  - 新增：NO_DEPENDS_ON_BOOT_OR_WEB（业务/绑定/协议模块不依赖 `com.codingas.gateway.boot..`/`adapter..`）、PROTOCOL_PLUGIN_ONLY_SPI（协议插件只依赖 protocol 核心 + common + 自身技术依赖，不依赖其他业务域）、STARTER_ONLY_AUTOCONFIGURE（starter 模块只依赖 spring-boot-autoconfigure + 本域模块）、PROTOCOL_PLUGIN_NO_COMPONENT（协议插件包禁止 @Component/@Service/@Repository，允许 @AutoConfiguration）
- **web 测试归位**（P3 承接）：boot 的 `src/test/.../adapter/` 下测试迁到 `gateway-web/src/test/java/com/codingas/gateway/adapter/`（物理迁移，包名不变）；boot 保留跨模块集成测试（integration/）
- **BaseDo 在 gateway-common**（`com.codingas.gateway.infrastructure.common`），DO extends 它合规——不动
- 质量基建（jacoco 全模块等）不在本计划范围；覆盖率验证留后续

---

## Task 1: 基线验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 基线全绿

- [ ] **Step 1: 全量构建 + 测试**

```bash
cd /e/workspace/llm-gateway
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（master `e206e617`；ArchUnit LayerDependencyTest 3 条规则绿——freeze 基线容忍 2 个已知违规）。

- [ ] **Step 2: 确认无失败**

若有失败先排查（systematic-debugging）。

---

## Task 2: 各域 Gateway 新增 count 端口

**Files:**
- Modify（provider 域 Gateway 接口）：
  - `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/vendor/ProviderGateway.java`（加 `long count();`）
  - `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelGateway.java`（加 `long count();`）
  - `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/channel/ChannelGateway.java`（加 `long count();`）
- Modify（iam 域 Gateway 接口）：
  - `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/user/UserGateway.java`（加 `long count();`）
- Modify（data 模块实现）：
  - `gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/gateway/ProviderGatewayImpl.java`（`return providerRepository.count();`）
  - `gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/gateway/ModelGatewayImpl.java`
  - `gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/gateway/ChannelGatewayImpl.java`
  - `gateway-iam/iam-data/src/main/java/com/codingas/gateway/iamdata/gateway/UserGatewayImpl.java`
- Create（测试）：
  - `gateway-provider/provider-data/src/test/java/com/codingas/gateway/providerdata/gateway/CountPortTest.java`（或扩展现有 GatewayImpl 测试）

**Interfaces:**
- Consumes: 无
- Produces: 4 个 Gateway 接口新增 `long count()` + data 实现

- [ ] **Step 1: 各域 Gateway 接口加 count()**

在 `ProviderGateway`/`ModelGateway`/`ChannelGateway`/`UserGateway` 接口增加：

```java
/**
 * 统计总数（供 stats 域等统计端口调用）
 */
long count();
```

放在接口末尾，保持现有风格（中文 Javadoc）。

- [ ] **Step 2: data 模块 GatewayImpl 实现**

各 `GatewayImpl` 增加：

```java
@Override
public long count() {
    return xxxRepository.count();
}
```

（注入对应 Repository 的类已存在；`count()` 是 Spring Data `JpaRepository` 内置。）

- [ ] **Step 3: 测试**

若对应 GatewayImpl 已有测试（如 `ProviderGatewayImplTest`），在测试中补 count 断言；若无，新建一个简短的 count 端口测试（构造 mock Repository + Impl，断言 count 调用并返回）。

- [ ] **Step 4: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（provider/iam 核心与 data 模块编译绿；stats 未改仍依赖 data——本任务不改 StatsService）。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: 各域 Gateway 新增 count 统计端口（Provider/Model/Channel/User，P4）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: StatsService 解耦（端口调用）

**Files:**
- Modify: `gateway-stats/stats/src/main/java/com/codingas/gateway/stats/StatsService.java`
- Modify: `gateway-stats/stats/pom.xml`（移除 `gateway-provider-data`、`gateway-iam-data` 依赖）

**Interfaces:**
- Consumes: Task 2 的 4 个 Gateway.count()
- Produces: stats 核心不再依赖任何 data 模块（freeze 违规二清零）

- [ ] **Step 1: 改写 StatsService**

`StatsService.java`：

```java
package com.codingas.gateway.stats;

import com.codingas.gateway.stats.dto.StatsResponse;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import com.codingas.gateway.iam.user.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报表服务
 *
 * <p>通过各域核心 Gateway 端口获取统计计数（端口调用，不依赖绑定模块 Repository）。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ProviderGateway providerGateway;
    private final ChannelGateway channelGateway;
    private final ModelGateway modelGateway;
    private final UserGateway userGateway;

    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        long providerCount = providerGateway.count();
        long channelCount = channelGateway.count();
        long modelCount = modelGateway.count();
        long userCount = userGateway.count();
        // TODO: 接入真实的请求统计和 Token 用量数据
        long todayRequests = 0;
        String tokenUsage = "0";
        return new StatsResponse(
                providerCount,
                channelCount,
                modelCount,
                userCount,
                todayRequests,
                tokenUsage
        );
    }
}
```

- [ ] **Step 2: stats pom 移除 data 依赖**

`gateway-stats/stats/pom.xml` 移除 `gateway-provider-data`、`gateway-iam-data` 两个依赖块（保留 `gateway-provider`、`gateway-iam`、`gateway-common`）。

- [ ] **Step 3: 测试更新**

检查 stats 相关测试（如有）——若 mock Repository 则改为 mock Gateway。

- [ ] **Step 4: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（stats 编译绿；ArchUnit NO_CORE_DEPENDS_BINDING_MODULES 的 freeze 违规二——StatsService 依赖 data Repository——已清零；freeze 规则对已消失违规自动容忍）。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: StatsService 改用各域 Gateway count 端口，stats 解耦 data 模块（P4）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: DO 穿透清零（UsageLogDo + AlertNotificationDo）

**Files:**
- Modify:
  - `gateway-audit/audit-data/src/main/java/com/codingas/gateway/auditdata/dataobject/UsageLogDo.java`
  - `gateway-audit/audit-data/pom.xml`（移除 `gateway-provider-data`、`gateway-iam-data` 依赖）
  - `gateway-alert/alert-data/src/main/java/com/codingas/gateway/alertdata/dataobject/AlertNotificationDo.java`
  - `gateway-alert/alert-data/pom.xml`（移除 `gateway-iam-data` 依赖）
- Modify（若被影响）：audit-data/alert-data 内引用 `user/provider/model` 实体关系的转换器/查询（勘察确认：CallLogDo/CallLogGatewayImpl 用标量 userId/model，不受影响；若有遗漏按编译错误修正）

**Interfaces:**
- Consumes: 无
- Produces: audit-data/alert-data 不再跨域依赖任何 data 模块（freeze 违规一清零）

- [ ] **Step 1: UsageLogDo 实体引用改 ID**

`UsageLogDo.java` 中 3 个 `@ManyToOne` 字段改为：

```java
/** 用户 ID（关联 iam 域用户，仅存 ID 不跨域引用实体） */
@Column(name = "user_id", nullable = false)
private Long userId;

/** 供应商 ID（关联 provider 域供应商，仅存 ID） */
@Column(name = "provider_id", nullable = false)
private Long providerId;

/** 模型 ID（关联 provider 域模型，仅存 ID） */
@Column(name = "model_id", nullable = false)
private Long modelId;
```

删除 3 个 `@ManyToOne` + 对应 import（`UserDo`/`ProviderDo`/`ModelDo`）。`@JoinColumn` 列名保留（`user_id`/`provider_id`/`model_id`——`@Column(name = ...)` 保持列名一致，**审计表结构不变**）。

- [ ] **Step 2: audit-data pom 移除跨域依赖**

`gateway-audit/audit-data/pom.xml` 移除 `gateway-provider-data`、`gateway-iam-data` 依赖（若 audit-data 还有其他跨域 data 依赖一并检查移除；audit-data 依赖 `gateway-audit` 核心 + spring-data-jpa 保留）。

- [ ] **Step 3: AlertNotificationDo 实体引用改 ID**

`AlertNotificationDo.java`：

```java
/** 目标用户 ID（仅存 ID，不跨域引用 iam 用户实体） */
@Column(name = "target_user_id", nullable = false)
private Long targetUserId;
```

删除 `@ManyToOne private UserDo targetUser` + import（`com.codingas.gateway.iamdata.dataobject.UserDo`）。

- [ ] **Step 4: alert-data pom 移除跨域依赖**

`gateway-alert/alert-data/pom.xml` 移除 `gateway-iam-data` 依赖。

- [ ] **Step 5: 编译修正 + 全量构建验证**

```bash
grep -rn "UserDo\|ProviderDo\|ModelDo" gateway-audit/audit-data/src gateway-alert/alert-data/src --include="*.java" | grep -v target
```

Expected: 无输出（跨域 DO 引用清零）。然后：

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（若 audit-data/alert-data 的转换器/查询用了 `do.getUser()` 等，按编译错误改为 ID 访问；freeze 违规一已清零）。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: UsageLogDo/AlertNotificationDo 跨域实体引用改 ID，data 模块解耦跨域依赖（P4）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 5: ArchUnit 模块级铁律 + web 测试归位 + 全量回归 + 设计文档

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/arch/LayerDependencyTest.java`（升级为铁律）
- Move（web 测试归位）：`gateway-boot/src/test/java/com/codingas/gateway/adapter/**` → `gateway-web/src/test/java/com/codingas/gateway/adapter/**`（约 17 个测试，物理迁移包名不变）
- Modify: `gateway-web/pom.xml`（加测试依赖 junit-jupiter/assertj，若需 mockito）
- Modify: `docs/superpowers/specs/2026-08-21-gateway-jmix-style-modularization-design.md`（§6 P4 完成标注）

**Interfaces:**
- Consumes: Task 2-4 全部产物
- Produces: 模块级铁律硬规则全绿；web 测试归位；设计文档 P4 标注

- [ ] **Step 1: 升级 LayerDependencyTest**

把 3 条 freeze 规则改为硬规则（去 `freeze(...)` 包装，直接 `noClasses()...` / `classes()...`），并新增 4 条规则：

```java
/** 业务域/绑定模块禁止反向依赖 boot/web 承载层 */
@ArchTest
static final ArchRule NO_DEPENDS_ON_BOOT_OR_WEB = noClasses()
    .that().resideInAnyPackage(
        "com.codingas.gateway.provider..", "com.codingas.gateway.iam..",
        "com.codingas.gateway.usage..", "com.codingas.gateway.security..",
        "com.codingas.gateway.audit..", "com.codingas.gateway.alert..",
        "com.codingas.gateway.resilience..", "com.codingas.gateway.proxy..",
        "com.codingas.gateway.stats..", "com.codingas.gateway.protocol..",
        "com.codingas.gateway.providerdata..", "com.codingas.gateway.iamdata..",
        "com.codingas.gateway.usagedata..", "com.codingas.gateway.securitydata..",
        "com.codingas.gateway.auditdata..", "com.codingas.gateway.alertdata..",
        "com.codingas.gateway.resiliencedata..")
    .should().dependOnClassesThat().resideInAnyPackage(
        "com.codingas.gateway.boot..", "com.codingas.gateway.adapter..");
```

```java
/** 协议插件只依赖协议核心 + 底座（不依赖其他业务域） */
@ArchTest
static final ArchRule PROTOCOL_PLUGIN_ONLY_SPI = noClasses()
    .that().resideInAnyPackage(
        "com.codingas.gateway.protocol.openai..",
        "com.codingas.gateway.protocol.anthropic..",
        "com.codingas.gateway.protocol.gemini..")
    .should().dependOnClassesThat().resideInAnyPackage(
        "com.codingas.gateway.provider..", "com.codingas.gateway.iam..",
        "com.codingas.gateway.usage..", "com.codingas.gateway.security..",
        "com.codingas.gateway.audit..", "com.codingas.gateway.alert..",
        "com.codingas.gateway.resilience..", "com.codingas.gateway.proxy..",
        "com.codingas.gateway.stats..");
```

```java
/** 协议插件包禁止 @Component/@Service/@Repository（防 boot 扫描穿透插件双注册；@AutoConfiguration 允许） */
@ArchTest
static final ArchRule PROTOCOL_PLUGIN_NO_COMPONENT = classes()
    .that().resideInAnyPackage(
        "com.codingas.gateway.protocol.openai..",
        "com.codingas.gateway.protocol.anthropic..",
        "com.codingas.gateway.protocol.gemini..")
    .should().notBeAnnotatedWith(org.springframework.stereotype.Component.class)
    .andShould().notBeAnnotatedWith(org.springframework.stereotype.Service.class)
    .andShould().notBeAnnotatedWith(org.springframework.stereotype.Repository.class);
```

```java
/** starter 只依赖 spring-boot-autoconfigure + 本域模块（禁止依赖其他业务域） */
@ArchTest
static final ArchRule STARTER_ONLY_AUTOCONFIGURE = noClasses()
    .that().resideInAnyPackage("com.codingas.gateway.autoconfigure..")
    .should().dependOnClassesThat().resideInAnyPackage(
        "com.codingas.gateway.provider..", "com.codingas.gateway.iam..",
        "com.codingas.gateway.usage..", "com.codingas.gateway.security..",
        "com.codingas.gateway.audit..", "com.codingas.gateway.alert..",
        "com.codingas.gateway.resilience..", "com.codingas.gateway.proxy..",
        "com.codingas.gateway.stats..");
```

注意：`@AutoConfiguration` 元注解含 `@Component`——`notBeAnnotatedWith(Component)` 对 `@AutoConfiguration` 类会失败（元注解传递）。若该规则误伤 AutoConfiguration，改用按包名的 `@Component` 直注解检查（如 `notBeAnnotatedWith` 会穿透元注解——需实测；若失败则改为排除 `*AutoConfiguration` 类或以 `@Service`/`@Repository` 检查为主）。**以实际测试结果为准**：若 `PROTOCOL_PLUGIN_NO_COMPONENT` 误伤 AutoConfiguration，调整规则为仅检查 `@Service`/`@Repository`/显式 `@Component`（非元注解）或排除 `*AutoConfiguration` 类名。

- [ ] **Step 2: 运行 ArchUnit 确认全绿**

```bash
./mvnw test -pl gateway-boot -Dtest=LayerDependencyTest
```

Expected: PASS（5 条规则全绿——Task 3/4 已清零 freeze 违规，硬规则通过）。若 `PROTOCOL_PLUGIN_NO_COMPONENT` 或 `STARTER_ONLY_AUTOCONFIGURE` 误报，按注释说明调整规则定义。

- [ ] **Step 3: web 测试归位**

把 `gateway-boot/src/test/java/com/codingas/gateway/adapter/**` 下约 17 个测试移动到 `gateway-web/src/test/java/com/codingas/gateway/adapter/**`（物理迁移，package 不变）。`gateway-web/pom.xml` 加测试依赖（junit-jupiter/assertj，参考其他模块）。若个别测试依赖 boot 的测试支撑类（如 `FullContextIntegrationTestBase`），留在 boot（集成测试）或评估迁移。

- [ ] **Step 4: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（ArchUnit 铁律全绿 + web 测试在新模块运行）。

- [ ] **Step 5: 设计文档 §6 P4 标注**

设计文档 §6 P4 段标注完成：ArchUnit 模块级铁律落地（freeze 解除 + 5 条硬规则）、DO 依赖清零（UsageLogDo/AlertNotificationDo 改 ID、StatsService 改端口）、web 测试归位。freeze 存储（target/archunit）不再需要——规则已硬化。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: ArchUnit 模块级铁律（解除 freeze）+ web 测试归位 + DO 依赖清零收尾（P4）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**（设计文档 §6 P4 → 任务）：
- 新增模块级 ArchUnit 铁律（解除 freeze）→ Task 5
- 禁止依赖 impl/DO → Task 3（StatsService）+ Task 4（DO 穿透）
- 禁止反向依赖 boot/web → Task 5（NO_DEPENDS_ON_BOOT_OR_WEB）
- 协议插件只依赖 SPI → Task 5（PROTOCOL_PLUGIN_ONLY_SPI）
- starter 只依赖 autoconfigure + 本域模块 → Task 5（STARTER_ONLY_AUTOCONFIGURE）
- 整改 audit/alert 穿透 DO 的耦合（UsageLogDo → 只存 ID）→ Task 4
- 验证：ArchUnit 全绿；全量回归 → Task 5
- P3 承接：web 测试归位 + 协议插件 @Component 约束 → Task 5

**Placeholder 扫描**：Gateway count 端口、StatsService 改写、DO 字段改造、ArchUnit 规则均含完整代码；无 TBD。

**Type/命名一致性**：
- `count()` 端口：Provider/Model/Channel/User Gateway 接口 + data GatewayImpl 实现 ✓
- StatsService 注入 4 个 Gateway ✓
- UsageLogDo：userId/providerId/modelId（Long）+ 列名 user_id/provider_id/model_id（表结构不变）✓
- AlertNotificationDo：targetUserId + 列名 target_user_id ✓
- ArchUnit 规则包名与 Global Constraints 一致 ✓

**风险**：
- `PROTOCOL_PLUGIN_NO_COMPONENT` 的 `@AutoConfiguration` 元注解含 @Component → Task 5 Step 2 已写调整路径（排除 AutoConfiguration 类或改检查项）
- freeze 规则去 freeze() 后若仍有隐性违规 → Task 5 Step 2 暴露，按违规整改
- web 测试归位后若有测试依赖 boot 测试支撑 → 留 boot（集成测试）或评估迁移
- audit-data/alert-data 移除依赖后若有转换器引用实体 → 编译错误暴露（Task 4 Step 5 处理路径）
