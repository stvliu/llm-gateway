# P2 主线 starter 化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 每个业务域核心模块补 `@Configuration` 装配入口，新增各 `-starter`（AutoConfiguration + imports），boot 装配显式化（去全包扫描），达成「实现与组装用 starter」的 Jmix 式装配。

**Architecture:** 9 个业务域核心模块各加 `XxxConfiguration`（`@ComponentScan` 限定本域核心 + data 两个根包 + `@ConfigurationPropertiesScan`）；各域新建 `gateway-<域>-starter` 模块（`@AutoConfiguration` + `@Import(XxxConfiguration)` + `AutoConfiguration.imports` 注册，只依赖 `spring-boot-autoconfigure` + 本域核心模块）；`GatewayApplication` 移入 `com.codingas.gateway.boot` 包，`scanBasePackages` 限定为 boot 自身 + 底座（adapter/application/infrastructure/common/protocol），业务域包由各域 starter 装配。绑定模块（-data）按 2026-08-23 用户决策由核心 Configuration 兼扫（data-starter 留后续）。行为不变。

**Tech Stack:** Java 21、Spring Boot 3.5.13、Spring MVC、Maven 多模块

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试；`@SpringBootTest` 完整上下文是装配正确性的最强验证）
- 每任务独立提交，commit message 中文
- 行为不变：禁止改变业务逻辑；本次只加装配层（Configuration/starter/boot 扫描调整）
- **原子切换约束**：boot 停止扫描业务域包 ↔ 9 个 Configuration + 9 个 starter 就绪必须同一任务完成——boot 一旦不扫域包，所有域 bean 必须立刻由 starter 提供，否则应用启动失败（@SpringBootTest 暴露）。Task 2 为原子切换点
- **绑定模块装配（用户决策）**：核心 Configuration `@ComponentScan` 兼扫本域两个根包（核心 + data），不建 data-starter
- 9 域与根包：
  | 域 | 核心根包 | data 根包 | starter artifactId | starter groupId |
  |---|---|---|---|---|
  | provider | `com.codingas.gateway.provider` | `com.codingas.gateway.providerdata` | `gateway-provider-starter` | `com.codingas.gateway.provider` |
  | iam | `com.codingas.gateway.iam` | `com.codingas.gateway.iamdata` | `gateway-iam-starter` | `com.codingas.gateway.iam` |
  | usage | `com.codingas.gateway.usage` | `com.codingas.gateway.usagedata` | `gateway-usage-starter` | `com.codingas.gateway.usage` |
  | security | `com.codingas.gateway.security` | `com.codingas.gateway.securitydata` | `gateway-security-starter` | `com.codingas.gateway.security` |
  | audit | `com.codingas.gateway.audit` | `com.codingas.gateway.auditdata` | `gateway-audit-starter` | `com.codingas.gateway.audit` |
  | alert | `com.codingas.gateway.alert` | `com.codingas.gateway.alertdata` | `gateway-alert-starter` | `com.codingas.gateway.alert` |
  | resilience | `com.codingas.gateway.resilience` | `com.codingas.gateway.resiliencedata` | `gateway-resilience-starter` | `com.codingas.gateway.resilience` |
  | proxy | `com.codingas.gateway.proxy` | （无 data） | `gateway-proxy-starter` | `com.codingas.gateway.proxy` |
  | stats | `com.codingas.gateway.stats` | （无 data） | `gateway-stats-starter` | `com.codingas.gateway.stats` |
- starter 目录：`gateway-<域>/<域>-starter/`（仿 Jmix 目录规则，parent `relativePath=../../pom.xml`）
- 底座（common/protocol）由 boot 直接扫描装配（不是业务域，不建 starter）；协议插件已有 AutoConfiguration
- boot 依赖切换：9 个核心 + 7 个 data → **9 个 starter**（starter 传递依赖核心模块，boot 编译期类型可达）；common/protocol/协议插件依赖保留
- `GatewayApplication` 移入 `com.codingas.gateway.boot`，`scanBasePackages = {"com.codingas.gateway.boot", "com.codingas.gateway.adapter", "com.codingas.gateway.application", "com.codingas.gateway.infrastructure", "com.codingas.gateway.common", "com.codingas.gateway.protocol"}`（不含任何业务域根包）
- 质量基建（jacoco/freeze/provider-data 补测试）不在本计划范围

---

## Task 1: 基线验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 基线全绿，回归基准

- [ ] **Step 1: 全量构建 + 测试**

```bash
cd /e/workspace/llm-gateway
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（master `d2ebe245`）。

- [ ] **Step 2: 确认无失败**

若有失败先排查（systematic-debugging），不进入下一任务。

---

## Task 2: 核心 Configuration + 9 starter + boot 原子切换

> **原子切换说明**：boot 的 `scanBasePackages` 一旦移除业务域包，所有域 bean 必须已由各域 starter 提供。因此 9 个 Configuration、9 个 starter 模块、boot 改造必须在同一任务内完成。步骤按「先建域装配 → 最后切 boot → 全量验证」推进，中间态无需构建绿，**最终必须 `./mvnw clean install` 全绿（@SpringBootTest 完整上下文启动 = 装配正确性验证）**。

**Files:**
- Create（9 个核心模块各一）：
  - `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/ProviderConfiguration.java`
  - `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/IamConfiguration.java`
  - `gateway-usage/usage/src/main/java/com/codingas/gateway/usage/UsageConfiguration.java`
  - `gateway-security/security/src/main/java/com/codingas/gateway/security/SecurityConfiguration.java`
  - `gateway-audit/audit/src/main/java/com/codingas/gateway/audit/AuditConfiguration.java`
  - `gateway-alert/alert/src/main/java/com/codingas/gateway/alert/AlertConfiguration.java`
  - `gateway-resilience/resilience/src/main/java/com/codingas/gateway/resilience/ResilienceConfiguration.java`
  - `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/ProxyConfiguration.java`
  - `gateway-stats/stats/src/main/java/com/codingas/gateway/stats/StatsConfiguration.java`
- Create（9 个 starter 模块）：
  - `gateway-provider/provider-starter/pom.xml` + `src/main/java/com/codingas/gateway/autoconfigure/provider/ProviderAutoConfiguration.java` + `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  - `gateway-iam/iam-starter/`（同构，`autoconfigure.iam`）
  - `gateway-usage/usage-starter/`（`autoconfigure.usage`）
  - `gateway-security/security-starter/`（`autoconfigure.security`）
  - `gateway-audit/audit-starter/`（`autoconfigure.audit`）
  - `gateway-alert/alert-starter/`（`autoconfigure.alert`）
  - `gateway-resilience/resilience-starter/`（`autoconfigure.resilience`）
  - `gateway-proxy/proxy-starter/`（`autoconfigure.proxy`）
  - `gateway-stats/stats-starter/`（`autoconfigure.stats`）
- Modify：
  - 根 `pom.xml`：`<modules>` 加 9 个 starter 模块（`<module>gateway-provider/provider-starter</module>` 等）；`<dependencyManagement>` 视需要加 starter 版本管理（同 `${revision}`，通常依赖声明用 groupId/artifactId 即可，boot 的依赖传递）
  - `gateway-boot/src/main/java/com/codingas/gateway/GatewayApplication.java` → 移动到 `com.codingas.gateway.boot` 包 + `scanBasePackages` 限定
  - `gateway-boot/pom.xml`：依赖切换（9 核心 + 7 data → 9 starter；common/protocol/协议插件保留）

**Interfaces:**
- Consumes: 现有各域 `@Component`/`@Service`/`@Repository`（已在各域包内）
- Produces:
  - `XxxConfiguration`（每域，`@Configuration` + `@ComponentScan(basePackages = {核心根包, data根包})` + `@ConfigurationPropertiesScan`）
  - `XxxAutoConfiguration`（starter，`@AutoConfiguration` + `@Import(XxxConfiguration)` + `@ConditionalOnProperty(prefix="gateway.<域>", name="enabled", matchIfMissing=true)`）
  - `AutoConfiguration.imports` 注册 starter 类
  - `GatewayApplication` 在 `com.codingas.gateway.boot`，scanBasePackages 限定

- [ ] **Step 1: 创建 9 个 XxxConfiguration**

每域核心模块新建（以 provider 为例，其余同构替换域根包）：

```java
package com.codingas.gateway.provider;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Provider 域装配入口
 *
 * <p>限定扫描本域核心包 {@code com.codingas.gateway.provider} 与绑定包
 * {@code com.codingas.gateway.providerdata}（绑定模块过渡期由核心兼扫，
 * data-starter 留后续），并扫描本域 @ConfigurationProperties。</p>
 */
@Configuration
@ComponentScan(basePackages = {
        "com.codingas.gateway.provider",
        "com.codingas.gateway.providerdata"
})
@ConfigurationPropertiesScan
public class ProviderConfiguration {
}
```

各域替换：`provider→provider+providerdata`、`iam→iam+iamdata`、`usage→usage+usagedata`、`security→security+securitydata`、`audit→audit+auditdata`、`alert→alert+alertdata`、`resilience→resilience+resiliencedata`、`proxy→proxy`（无 data）、`stats→stats`（无 data）。类名与包名一一对应（ProviderConfiguration 在 provider 包等）。

- [ ] **Step 2: 创建 9 个 starter 模块骨架**

每域新建模块（以 provider 为例）：

`gateway-provider/provider-starter/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.codingas.gateway</groupId>
        <artifactId>gateway-project</artifactId>
        <version>${revision}</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <groupId>com.codingas.gateway.provider</groupId>
    <artifactId>gateway-provider-starter</artifactId>
    <packaging>jar</packaging>
    <name>Gateway Provider Starter</name>
    <description>Provider 域纯装配：AutoConfiguration 引入 ProviderConfiguration（零业务逻辑）</description>

    <dependencies>
        <!-- 装配框架 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <!-- 本域核心模块（@Import 编译期引用 + 传递组件依赖） -->
        <dependency>
            <groupId>com.codingas.gateway.provider</groupId>
            <artifactId>gateway-provider</artifactId>
            <version>${revision}</version>
        </dependency>
    </dependencies>
</project>
```

`gateway-provider/provider-starter/src/main/java/com/codingas/gateway/autoconfigure/provider/ProviderAutoConfiguration.java`：

```java
package com.codingas.gateway.autoconfigure.provider;

import com.codingas.gateway.provider.ProviderConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Provider 域自动装配（纯装配，零业务逻辑）
 *
 * <p>通过 {@code gateway.provider.enabled}（默认开启）控制域装配开关。</p>
 */
@AutoConfiguration
@Import(ProviderConfiguration.class)
@ConditionalOnProperty(prefix = "gateway.provider", name = "enabled", matchIfMissing = true)
public class ProviderAutoConfiguration {
}
```

`gateway-provider/provider-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
com.codingas.gateway.autoconfigure.provider.ProviderAutoConfiguration
```

其余 8 域同构：artifactId、groupId（按 Global Constraints 表）、`autoconfigure.<域>` 包、`XxxAutoConfiguration` 类、`@ConditionalOnProperty(prefix = "gateway.<域>")`、imports 文件、`@Import(XxxConfiguration)`。**无 data 的域（proxy/stats）`@ComponentScan` 只扫核心根包（已在 Step 1 完成）。**

注意：starter 依赖的版本用 `${revision}`（父 POM 已定义）；`spring-boot-autoconfigure` 版本由父 POM dependencyManagement 管理（boot 父依赖已管理）。

- [ ] **Step 3: 根 pom 注册 9 个 starter 模块**

根 `pom.xml` 的 `<modules>` 按目录顺序添加：

```xml
<module>gateway-provider/provider-starter</module>
<module>gateway-iam/iam-starter</module>
<module>gateway-usage/usage-starter</module>
<module>gateway-security/security-starter</module>
<module>gateway-audit/audit-starter</module>
<module>gateway-alert/alert-starter</module>
<module>gateway-resilience/resilience-starter</module>
<module>gateway-proxy/proxy-starter</module>
<module>gateway-stats/stats-starter</module>
```

（放置在各自域核心模块的 `<module>` 之后，保持目录顺序可读性）

- [ ] **Step 4: GatewayApplication 移包 + scanBasePackages 限定（原子切换点）**

把 `GatewayApplication.java` 移到 `gateway-boot/src/main/java/com/codingas/gateway/boot/GatewayApplication.java`，改：

```java
package com.codingas.gateway.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 *
 * <p>scanBasePackages 限定 boot 自身层 + 底座（common/protocol）——业务域
 * 组件由各域 starter 的 AutoConfiguration 显式装配（装配显式化，去全包扫描）。</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.codingas.gateway.boot",
        "com.codingas.gateway.adapter",
        "com.codingas.gateway.application",
        "com.codingas.gateway.infrastructure",
        "com.codingas.gateway.common",
        "com.codingas.gateway.protocol"
})
@EnableScheduling
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

删除旧位置的 `GatewayApplication.java`。

- [ ] **Step 5: boot pom 依赖切换**

`gateway-boot/pom.xml`：
- **移除** 9 个核心模块依赖：`gateway-provider`、`gateway-iam`、`gateway-usage`、`gateway-security`、`gateway-resilience`、`gateway-audit`、`gateway-stats`、`gateway-alert`、`gateway-proxy`
- **移除** 7 个 data 模块依赖：`gateway-provider-data`、`gateway-iam-data`、`gateway-usage-data`、`gateway-security-data`、`gateway-resilience-data`、`gateway-audit-data`、`gateway-alert-data`
- **新增** 9 个 starter 依赖：`gateway-provider-starter`、`gateway-iam-starter`、`gateway-usage-starter`、`gateway-security-starter`、`gateway-resilience-starter`、`gateway-audit-starter`、`gateway-alert-starter`、`gateway-proxy-starter`、`gateway-stats-starter`（groupId 按 Global Constraints 表）
- **保留**：`gateway-common`、`gateway-protocol`、`gateway-protocol-openai`、`gateway-protocol-anthropic`、`gateway-protocol-gemini`（及全部第三方依赖）

- [ ] **Step 6: 全量构建 + 测试（装配验证）**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿。**关键**：boot 的 `@SpringBootTest` 集成测试（FullContextIntegrationTest 等）会加载完整上下文——若某域 bean 因装配遗漏缺失，启动失败暴露。若启动失败，定位缺失 bean 的域与组件，检查该域 Configuration 的 `@ComponentScan` 是否覆盖对应包。

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: 9 域核心 Configuration + starter 装配 + boot 去全包扫描（P2 主线）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: 装配显式化防回归 + 配置属性下沉审查 + 全量回归

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/arch/ExplicitAssemblyTest.java`
- Modify: 若有配置下沉遗漏则按 §5.5 处理

**Interfaces:**
- Consumes: Task 2 全部产物
- Produces: 装配显式化防回归守护；确认配置属性注册完整

- [ ] **Step 1: 写装配显式化防回归测试**

`ExplicitAssemblyTest.java`：

```java
package com.codingas.gateway.arch;

import com.codingas.gateway.boot.GatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 装配显式化守护：boot 的 scanBasePackages 不得含业务域根包
 *
 * <p>防止回退到「@SpringBootApplication 全包扫描隐式装配」——业务域组件
 * 必须由各域 starter 的 AutoConfiguration 显式装配。</p>
 */
class ExplicitAssemblyTest {

    private static final List<String> FORBIDDEN_ROOTS = List.of(
            "com.codingas.gateway.provider",
            "com.codingas.gateway.iam",
            "com.codingas.gateway.usage",
            "com.codingas.gateway.security",
            "com.codingas.gateway.audit",
            "com.codingas.gateway.alert",
            "com.codingas.gateway.resilience",
            "com.codingas.gateway.proxy",
            "com.codingas.gateway.stats"
    );

    @Test
    void bootDoesNotScanBusinessDomains() {
        SpringBootApplication annotation = GatewayApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertThat(annotation).isNotNull();
        String[] packages = annotation.scanBasePackages();
        assertThat(packages).isNotEmpty();
        for (String forbidden : FORBIDDEN_ROOTS) {
            assertThat(Arrays.asList(packages))
                    .as("boot 不得直接扫描业务域根包 %s（应由 starter 装配）", forbidden)
                    .doesNotContain(forbidden);
        }
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=ExplicitAssemblyTest
```

Expected: PASS（3 条断言：注解存在、packages 非空、9 个域根包均不含）。

- [ ] **Step 3: 配置属性下沉审查**

逐域核对 `@ConfigurationProperties` 类是否在其域根包内（由对应 `@ConfigurationPropertiesScan` 注册）：
- resilience `GatewayRetryProperties`（在 `resilience/retry/` 包 ✓ 由 ResilienceConfiguration 扫）
- security `RateLimitProperties`（在 `security/threat/` 包 ✓）
- 其余域若存在属性类，确认在域根包内；boot 的全局/技术配置（GatewayProperties/Web/Cors/HttpClient/JpaAuditing/OpenApi/HealthCheckExecutor/ThreatRateLimitConfig 等）保留 boot（由 boot 扫描装配），不做下沉。

```bash
grep -rln "@ConfigurationProperties" gateway-*/ --include="*.java" | grep -v target | grep -v gateway-boot
```

Expected: 输出为空或全部位于各域根包内（若有 boot 外且不在域根包的属性类，迁移到所属域根包并确认被扫描）。

- [ ] **Step 4: 全量回归**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（含 ExplicitAssemblyTest）。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: 装配显式化防回归测试（boot 不扫业务域根包）+ 配置属性审查（P2 主线）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**（设计文档 §6 P2 主线 + §5 装配机制 → 任务）：
- 核心模块补 @Configuration 装配入口（@ComponentScan 限定本域包 + @ConfigurationPropertiesScan）→ Task 2 Step 1
- 新增各 -starter（AutoConfiguration + imports，只依赖 spring-boot-autoconfigure + 本域核心模块）→ Task 2 Step 2
- boot 依赖切换为各 starter；装配显式化（去全包扫描）→ Task 2 Step 4/5
- 验证：构建绿；装配显式化测试 → Task 2 Step 6 + Task 3
- §5.4 GatewayApplication 移入独立包（com.codingas.gateway.boot）+ 扫描范围缩至 boot 自身 → Task 2 Step 4
- §5.5 配置属性：各域 @ConfigurationPropertiesScan 注册 + boot 保留全局/安全配置 → Task 3 Step 3
- §4.5 目录规则：starter 子模块目录 `<域>-starter`、relativePath `../../pom.xml`、groupId 按域 → Task 2 Step 2/3
- 绑定模块用户决策（核心兼扫 data 包，不建 data-starter）→ Global Constraints + Task 2 Step 1

**Placeholder 扫描**：所有新代码（Configuration/AutoConfiguration/pom/imports/测试）含完整代码；9 域为同构替换，已给完整模式 + 差异表。

**Type/命名一致性**：
- `XxxConfiguration`（域根包）↔ `XxxAutoConfiguration`（`autoconfigure.<域>` 包）↔ `@Import(XxxConfiguration)` 编译期引用 ✓
- starter artifactId `gateway-<域>-starter` / groupId 域 groupId ✓
- `@ConditionalOnProperty(prefix="gateway.<域>", enabled)` 每域唯一前缀 ✓
- GatewayApplication 移包后 boot 测试类（`com.codingas.gateway.**`）仍能向上找到 `@SpringBootConfiguration` ✓

**风险**：
- 原子切换期间若有域组件漏配 → @SpringBootTest 启动失败暴露（Task 2 Step 6 处理路径已写）
- `@ConfigurationPropertiesScan` 与 boot 的 `@EnableConfigurationProperties` 无冲突（不同包）
- AutoConfiguration 无域间顺序依赖（bean 按依赖解析），无需 @AutoConfigureAfter
