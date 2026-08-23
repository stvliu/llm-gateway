# P3 boot 瘦身 + gateway-web 独立 + 协议插件自包含 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** boot 瘦身为纯启动+装配（仅剩启动类、infrastructure 装配、init 种子）；10 个 application 门面服务下沉到 provider/proxy 域；拆出 gateway-web 模块承载全部 Controller/Interceptor/Advice；OpenAI/Anthropic validator+tuner 迁回协议插件（协议插件自包含达成）。

**Architecture:** 门面服务下沉到对应域模块（catalog/channel/channelcredential/model/provider 六组 → provider 域；experience → proxy 域），DTO 按业务概念落位（provider.catalog/channel/model/vendor 子包，避免 `catalog/dto/ModelResponse` 与 `model/dto/ModelResponse` 重名）；`gateway-web` 新模块（根目录，`com.codingas.gateway.adapter.*` 包名不变）承载 HTTP 承载层（Controller/Interceptor/Advice/SseStreamHelper/DTO），用 `@AutoConfiguration` + imports 装配（仿 starter 模式），boot 依赖 gateway-web 即装配；validator/tuner 迁回协议插件（插件 AutoConfiguration 注册 `@Bean`）；`WebConfig`/`CorsConfig`/`OpenApiConfig`/`static/` 留 boot（它们依赖 boot 配置类，web 不反向依赖 boot）；`init/*`（CommandLineRunner 种子）留 boot。**2026-08-24 用户决策**：ModelExperienceService → proxy（零新依赖）；ModelDiscoveryService/ChannelEmergencyServiceImpl → provider（接受 provider→iam、provider→resilience 新依赖方向）。

**Tech Stack:** Java 21、Spring Boot 3.5.13、Spring MVC、Maven 多模块（域父 POM 层级聚合）

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试；@SpringBootTest 完整上下文 = 装配正确性验证）
- 每任务独立提交，commit message 中文
- 行为不变：纯搬迁+装配重构，不改业务逻辑
- **下沉包名映射**（R4 业务概念落位）：

  | 来源（boot/application/<子包>） | 目标 |
  |---|---|
  | `catalog/*.java`（3 Service + 1 Facade，接口/Impl） | `com.codingas.gateway.provider.service` |
  | `catalog/dto/*.java`（8） | `com.codingas.gateway.provider.catalog` |
  | `channel/*.java`（6 Service 接口/Impl） | `com.codingas.gateway.provider.service` |
  | `channel/dto/*.java`（10） | `com.codingas.gateway.provider.channel` |
  | `channelcredential/*.java`（2 Service 接口/Impl） | `com.codingas.gateway.provider.service` |
  | `channelcredential/dto/*.java`（5） | `com.codingas.gateway.provider.channel` |
  | `model/*.java`（3 Service 接口/Impl） | `com.codingas.gateway.provider.service` |
  | `model/dto/*.java`（5） | `com.codingas.gateway.provider.model` |
  | `provider/*.java`（2 Service 接口/Impl） | `com.codingas.gateway.provider.service` |
  | `provider/dto/*.java`（7） | `com.codingas.gateway.provider.vendor` |
  | `experience/ModelExperienceService.java` | `com.codingas.gateway.proxy.experience` |
  | `experience/dto/*.java`（3） | `com.codingas.gateway.proxy.dto` |

  > 注意：`application/provider/dto/ConnectivityTestResult`（分层 DTO）与 `protocol.transport.ConnectivityTestResult`（上游探测）**同名勿合并**——下沉后分层 DTO 落在 `provider.vendor`，两者仍不同包。
- **gateway-web 模块**：`gateway-web/` 根目录（parent=根 pom），artifactId `gateway-web`，group 域 `com.codingas.gateway`；包名沿用 `com.codingas.gateway.adapter.*`（Controller/Interceptor/Advice/DTO 包名不变，只换模块归属）；`@AutoConfiguration` + `AutoConfiguration.imports` 装配（web 无 @ConditionalOnProperty，永远启用）
- **WebConfig/CorsConfig/OpenApiConfig/static 留 boot**（依赖 boot 配置类 GatewayProperties 等，web 不反向依赖 boot；WebConfig 注入 web 模块的 SecurityInterceptorChain——boot 依赖 web 后可用）
- **init/* 留 boot**（CommandLineRunner 种子装配，非门面服务）
- **跨域门面**：ModelDiscoveryService（依赖 iam.application）、ChannelEmergencyServiceImpl（依赖 resilience）下沉 provider，provider pom 新增 `gateway-iam`、`gateway-resilience` 依赖
- **validator/tuner 迁插件**：`adapter.protocol.openai.OpenAIProtocolValidator/OpenAITuner` → `protocol.openai`；`adapter.protocol.anthropic.*` → `protocol.anthropic`；插件 `AutoConfiguration` 加 `@Bean` 注册（删除 boot 旧类，防同包双注册）；`AnthropicController`/`OpenAIController` import 更新；`OutboundTuner`（proxy）集合注入 `List<ProtocolTuner<?>>` 自动收集新 bean
- **boot 收尾**：GatewayApplication `scanBasePackages` 移除 `adapter`（迁 web 后 boot 不再有 adapter 包）与 `application`（门面下沉后仅剩 init——boot 扫描 `com.codingas.gateway.application` 命中 init，可保留或迁移 init 包，二选一实施时定）；boot pom 保留全部域依赖（运行时装配者）
- 质量基建不在本计划范围

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

Expected: BUILD SUCCESS，全部测试绿（master `24aaf75c`）。

- [ ] **Step 2: 确认无失败**

若有失败先排查（systematic-debugging）。

---

## Task 2: provider 门面下沉（catalog/channel/channelcredential/model/provider）

**Files:**
- Move（52 个文件，boot/application → provider 核心模块，包名按 Global Constraints 映射）：
  - `gateway-boot/.../application/catalog/`（12）→ `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/{service,catalog}/`
  - `gateway-boot/.../application/channel/`（16）→ `.../provider/{service,channel}/`
  - `gateway-boot/.../application/channelcredential/`（7）→ `.../provider/{service,channel}/`
  - `gateway-boot/.../application/model/`（8）→ `.../provider/{service,model}/`
  - `gateway-boot/.../application/provider/`（9）→ `.../provider/{service,vendor}/`
- Modify：`gateway-provider/provider/pom.xml` 新增 `gateway-iam`、`gateway-resilience` 依赖（跨域门面需要）
- Modify（import 更新）：下沉类内部引用 `com.codingas.gateway.application.*` 的 import → 新包名；`common.*` 保留
- Modify（消费者 import）：boot 内引用 `application.<子包>.*` 的类（9 个 Controller 在 Task 4 迁 web 时改；GatewayApplication 的 scanBasePackages 在 Task 5 改）——Task 2 期间 boot 仍编译需临时兼容：**下沉时同步改 boot 内所有引用 application.* 的 import**（含 Controller）
- Move（测试随迁）：`gateway-boot/src/test/.../application/*` 相关测试 → provider 核心测试包（若有）

**Interfaces:**
- Consumes: 无
- Produces: provider 域新增 `service/`（17 个门面 Service 接口/Impl）与 `catalog/channel/model/vendor` 子包新增管理 DTO

- [ ] **Step 1: 搬迁 catalog 组**

把 `application/catalog/` 下 4 个 Service 类（CatalogSyncFacade、ChannelProvisionService、PlanCatalogService、PlanCatalogServiceImpl）移动为 `provider.service`，8 个 DTO 移动为 `provider.catalog`。改 package 声明 + 全部 import（`com.codingas.gateway.application.catalog[.dto].*` → `provider.service/provider.catalog`；引用 `provider.*` 的保留）。

- [ ] **Step 2: 搬迁 channel + channelcredential 组**

`channel/` 6 个 Service → `provider.service`；10 个 DTO + `channelcredential/` 5 个 DTO → `provider.channel`；`channelcredential/` 2 个 Service → `provider.service`。改 package + import。**注意**：`ChannelCredentialService` 引用 `application.channel.dto.ApiKeyTestResponse` → `provider.channel.ApiKeyTestResponse`。

- [ ] **Step 3: 搬迁 model + provider 组**

`model/` 3 个 Service → `provider.service`；5 个 DTO → `provider.model`；`provider/` 2 个 Service → `provider.service`；7 个 DTO → `provider.vendor`。改 package + import。**注意**：`provider/dto/ConnectivityTestResult`（分层 DTO）与 `protocol.transport.ConnectivityTestResult` 同名——下沉到 `provider.vendor` 后 `ProviderServiceImpl` 等引用处保持两者清晰（import 精确，不合并）。

- [ ] **Step 4: 同步修正 boot 内引用**

`grep -rln "application.catalog\|application.channel\|application.channelcredential\|application.model\|application.provider" gateway-boot/src` 定位 boot 内（main+test）引用下沉类的文件，全部改 import 为新包名（9 个 Controller + 测试）。**GatewayApplication 的 scanBasePackages 本任务不动**（Task 5 处理）——但 `com.codingas.gateway.application` 包内现在只剩 experience/init，boot 扫描它不再命中下沉类 ✓。

- [ ] **Step 5: provider pom 加依赖**

`gateway-provider/provider/pom.xml` 增加：

```xml
<!-- 跨域门面：模型发现查询应用渠道（ModelDiscoveryService） -->
<dependency>
    <groupId>com.codingas.gateway.iam</groupId>
    <artifactId>gateway-iam</artifactId>
    <version>${revision}</version>
</dependency>
<!-- 跨域门面：渠道紧急操作使用熔断管理器（ChannelEmergencyServiceImpl） -->
<dependency>
    <groupId>com.codingas.gateway.resilience</groupId>
    <artifactId>gateway-resilience</artifactId>
    <version>${revision}</version>
</dependency>
```

- [ ] **Step 6: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（@SpringBootTest 完整上下文验证新装配）。若 boot 测试仍引用旧包路径，按编译错误修正 import。

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: provider 域门面下沉（catalog/channel/channelcredential/model/provider → provider.service + 业务子包 DTO，P3）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: experience 下沉 proxy + validator/tuner 迁回协议插件

**Files:**
- Move（experience 组，boot/application → proxy 核心模块）：
  - `gateway-boot/.../application/experience/ModelExperienceService.java` → `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/experience/`
  - `gateway-boot/.../application/experience/dto/*.java`（3）→ `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/dto/`
- Move（validator/tuner，boot → 协议插件）：
  - `gateway-boot/.../adapter/protocol/openai/OpenAIProtocolValidator.java`、`OpenAITuner.java` → `gateway-protocol/protocol-openai/src/main/java/com/codingas/gateway/protocol/openai/`
  - `gateway-boot/.../adapter/protocol/anthropic/AnthropicProtocolValidator.java`、`AnthropicTuner.java` → `gateway-protocol/protocol-anthropic/src/main/java/com/codingas/gateway/protocol/anthropic/`
- Modify（插件 AutoConfiguration 注册 @Bean）：
  - `OpenAIProtocolAutoConfiguration.java`、`AnthropicProtocolAutoConfiguration.java` 各加 validator/tuner 的 `@Bean` 方法
- Modify（Controller import）：`AnthropicController.java`、`OpenAIController.java` 的 `adapter.protocol.anthropic.*` → `protocol.anthropic.*`（`adapter.protocol.openai.*` → `protocol.openai.*`）
- Modify（OutboundTuner 验证）：`gateway-proxy/proxy/.../conversion/OutboundTuner.java` 按 `List<ProtocolTuner<?>>` 集合注入——迁移后验证仍能收集到新 bean（无需改代码，靠测试验证）
- Move（测试随迁）：`gateway-boot/src/test/.../adapter/protocol/anthropic/AnthropicTunerTest.java` → 插件测试包；`integration/ProtocolConversionIntegrationTest.java` 等引用迁移（若有）

**Interfaces:**
- Consumes: Task 2 的 provider.service
- Produces: proxy 域新增 `experience` 子包；协议插件自包含（adapter + validator + tuner + upstream client 全部在插件内）

- [ ] **Step 1: 迁移 ModelExperienceService 到 proxy**

移动到 `proxy/experience/`，改 package + import（`application.experience[.dto].*` → `proxy.experience/proxy.dto`；`provider.*`/`protocol.*` 保留）。3 个 DTO → `proxy.dto`。`ExperienceController`（boot，Task 4 迁 web）的 import 同步改。

- [ ] **Step 2: 迁移 4 个 validator/tuner 到插件**

移动到 `protocol.openai`/`protocol.anthropic` 插件包，改 package 声明。**保持 `@Component` 注解或改 `@Bean` 注册（二选一）**——本任务采用：**移除 `@Component`，由插件 AutoConfiguration `@Bean` 注册**（插件不自扫包，自包含装配）。若保留 `@Component` 则 boot 扫描 `com.codingas.gateway.protocol` 会扫到（插件包在 protocol 根包下！）——注意：**插件包 `com.codingas.gateway.protocol.openai` 在 boot 的 scanBasePackages `com.codingas.gateway.protocol` 之下会被扫到**，因此必须改 `@Bean` 注册并删 `@Component` 才能确保"插件自装配"语义。检查现有插件包类是否被 boot 误扫（ProtocolAdapter 等已由插件 AutoConfiguration 注册，无 @Component）。

- [ ] **Step 3: 插件 AutoConfiguration 注册 @Bean**

`OpenAIProtocolAutoConfiguration.java` 增加：

```java
@Bean
public OpenAIProtocolValidator openAIProtocolValidator() {
    return new OpenAIProtocolValidator();
}

@Bean
public OpenAITuner openAITuner() {
    return new OpenAITuner();
}
```

`AnthropicProtocolAutoConfiguration.java` 对应 `AnthropicProtocolValidator`/`AnthropicTuner`。

- [ ] **Step 4: Controller import 更新 + 测试随迁**

`AnthropicController`/`OpenAIController` 的 validator import 改插件包。`AnthropicTunerTest` 迁插件测试包（改 package + import）。其余 boot 测试按编译错误修正。

- [ ] **Step 5: OutboundTuner 集合注入验证**

```bash
./mvnw test -pl gateway-proxy/proxy -Dtest=OutboundTunerTest
```

Expected: PASS（`List<ProtocolTuner<?>>` 注入仍收集到 openai/anthropic 两个 tuner）。再全量构建确认 @SpringBootTest 上下文无重复 bean。

- [ ] **Step 6: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（重点：协议插件测试、proxy conversion 测试、boot 集成测试——确认 validator/tuner 装配正确且无重复 bean）。

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: ModelExperienceService 下沉 proxy + validator/tuner 迁回协议插件自包含（P3）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: gateway-web 新模块

**Files:**
- Create（新模块）：
  - `gateway-web/pom.xml`
  - `gateway-web/src/main/java/com/codingas/gateway/adapter/WebAutoConfiguration.java`（@AutoConfiguration + @Import(WebConfiguration)）
  - `gateway-web/src/main/java/com/codingas/gateway/adapter/WebConfiguration.java`（@ComponentScan 限定 `com.codingas.gateway.adapter`）
  - `gateway-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Move（boot/adapter → gateway-web，**包名不变** `com.codingas.gateway.adapter.*`）：
  - `api/`：20 个 Controller + `SseStreamHelper` + `ApplicationChannelRequest` + `dto/ChannelHealthCheckRequest`（23）
  - `interceptor/`：7 个文件（GatewayInterceptor、AbstractGatewayInterceptor、ApiKeyAuthInterceptor、TokenAuthInterceptor、IPBlockCheckInterceptor、RateLimitInterceptor、SecurityInterceptorChain）
  - `advice/`：4 个（ApiResponseWrapperAdvice、GlobalExceptionHandler、IamExceptionHandler、ThreatExceptionHandler）
- Modify：
  - 根 `pom.xml`：`<modules>` 加 `gateway-web`（放 gateway-boot 前）
  - `gateway-boot/pom.xml`：新增 `gateway-web` 依赖
  - `gateway-web/pom.xml` 依赖：`gateway-common`、`gateway-protocol`、`gateway-protocol-openai`、`gateway-protocol-anthropic`、`gateway-provider`、`gateway-iam`、`gateway-security`、`gateway-resilience`、`gateway-proxy`、`gateway-stats`、`gateway-usage`（9 域 + common + protocol/插件）+ `spring-boot-starter-web` + `spring-boot-autoconfigure`

**Interfaces:**
- Consumes: Task 2/3 下沉后的门面（`provider.service` 等）
- Produces: `gateway-web` 模块（HTTP 承载层，@AutoConfiguration 装配）；boot 不再含 adapter 包

- [ ] **Step 1: 创建 gateway-web 模块骨架**

`gateway-web/pom.xml`（parent=根 pom、artifactId `gateway-web`、groupId `com.codingas.gateway`、packaging jar）+ 依赖（Global Constraints 清单）+ WebConfiguration + WebAutoConfiguration + imports 文件：

```java
package com.codingas.gateway.adapter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Web 承载层装配：限定扫描 HTTP 承载包（Controller/Interceptor/Advice）
 */
@Configuration
@ComponentScan(basePackages = "com.codingas.gateway.adapter")
public class WebConfiguration {
}
```

```java
package com.codingas.gateway.adapter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Web 自动装配（boot 依赖 gateway-web 即装配 HTTP 承载层）
 */
@AutoConfiguration
@Import(WebConfiguration.class)
public class WebAutoConfiguration {
}
```

imports 文件内容：`com.codingas.gateway.adapter.WebAutoConfiguration`

- [ ] **Step 2: 搬迁 api 包（23 文件）**

`adapter/api/` 全部类移动到 `gateway-web/src/main/java/com/codingas/gateway/adapter/api/`（**物理移动，package 声明不变**）。Controller 内引用 `application.*` 的 import 已在 Task 2/3 改过（验证无残留 `com.codingas.gateway.application.*` import）。

- [ ] **Step 3: 搬迁 interceptor + advice 包（11 文件）**

`adapter/interceptor/`、`adapter/advice/` 全部类移动到 gateway-web（包名不变）。`SecurityInterceptorChain` 等 @Component 由 WebConfiguration 扫描。

- [ ] **Step 4: 根 pom + boot pom 调整**

根 pom `<modules>` 加 `gateway-web`（在 gateway-boot 前）。boot pom 新增 `gateway-web` 依赖。

- [ ] **Step 5: boot 扫描面调整（部分）**

`GatewayApplication` 的 `scanBasePackages` 移除 `"com.codingas.gateway.adapter"`（web 承载层不再由 boot 扫描）——`"com.codingas.gateway.application"` 保留与否看 init 处理（Task 5 定；本任务先移除 adapter）。**注意**：WebConfiguration 的 `@ComponentScan("com.codingas.gateway.adapter")` 与 boot 扫描无冲突（boot 不再扫 adapter 包）。

- [ ] **Step 6: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿。**关键验证**：boot 测试 @SpringBootTest 上下文启动时，WebAutoConfiguration（boot 依赖 gateway-web）装配 Controller/Interceptor/Advice——若 WebConfig（留 boot）注入 SecurityInterceptorChain（web 模块），boot 依赖 web 后可解析。若测试类引用 `com.codingas.gateway.adapter.*` 且测试在 boot 模块——**boot 测试仍可访问 web 模块类（boot 依赖 web，classpath 可达）** ✓。

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: gateway-web 新模块承载 HTTP 承载层（Controller/Interceptor/Advice，@AutoConfiguration 装配，P3）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 5: boot 瘦身收尾 + 全量回归 + 设计文档更新

**Files:**
- Modify:
  - `gateway-boot/src/main/java/com/codingas/gateway/boot/GatewayApplication.java`（scanBasePackages 最终化）
  - `gateway-boot/pom.xml`（依赖收尾确认）
  - `docs/superpowers/specs/2026-08-21-gateway-jmix-style-modularization-design.md`（§6 P3 完成标注 + 目录树 gateway-web 已落地）

**Interfaces:**
- Consumes: Task 2-4 全部产物
- Produces: boot 纯启动+装配（启动类 + infrastructure + init + 资源）；装配显式化完整

- [ ] **Step 1: GatewayApplication scanBasePackages 最终化**

`com.codingas.gateway.boot` 包下，scanBasePackages 设为：

```java
@SpringBootApplication(scanBasePackages = {
        "com.codingas.gateway.boot",
        "com.codingas.gateway.infrastructure",
        "com.codingas.gateway.common",
        "com.codingas.gateway.protocol",
        "com.codingas.gateway.application"   // init 种子（仅剩 init 包）——若 init 迁移到 boot 包则移除此项
})
```

（决策：`application/init` 保留原位并由 boot 扫描 `com.codingas.gateway.application`（只命中 init），或迁移 init 到 `com.codingas.gateway.boot.init`——二选一，实施时选保留原位 + 扫描 `application`，改动最小。）

- [ ] **Step 2: boot pom 依赖收尾**

确认 boot pom：依赖 gateway-web + 9 域 starter + common/protocol/插件（无 adapter 相关残留）。`grep -rn "adapter\." gateway-boot/src/main --include="*.java"` 应无结果（boot main 不再引用 adapter 包）。

- [ ] **Step 3: 全局残留检查**

```bash
grep -rn "com.codingas.gateway.application" gateway-boot/src/main --include="*.java" | grep -v "application.init"
```

Expected: 无输出（boot main 仅 init 引用 application，且 init 在 application 包内）。

- [ ] **Step 4: 设计文档 §6 P3 更新**

设计文档 §6 P3 段标注完成状态：boot 瘦身达成（纯启动+装配）、gateway-web 落地（HTTP 承载层）、validator/tuner 迁回插件（协议插件自包含完成）。目录树 `gateway-web` 条目去掉「P3 新增」改为已落地。

- [ ] **Step 5: 全量回归**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（boot 纯启动验证 + gateway-web 独立构建 + 协议插件自包含集成测试）。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: boot 瘦身收尾（纯启动+装配）+ 设计文档 P3 完成标注（P3）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**（设计文档 §6 P3 → 任务）：
- boot 的 application 门面服务下沉到对应域模块 → Task 2（provider 六组）+ Task 3（experience→proxy）
- 拆出 gateway-web 承载全部 Controller/Interceptor/Advice → Task 4
- OpenAI/AnthropicProtocolValidator + *Tuner 从 boot 迁回协议插件模块 → Task 3
- 验证：boot 纯启动；web 独立构建；协议插件自包含集成测试 → Task 5 + 各任务构建验证
- §4.4 gateway-web 依赖各域核心模块根包 API（禁止依赖 impl/DO）→ Task 4 pom 依赖清单
- §4.5 目录树 gateway-web（保持根目录）→ Task 4

**Placeholder 扫描**：所有新代码（WebConfiguration/WebAutoConfiguration/pom/imports/@Bean 注册）含完整代码；搬迁类给出来源/目标/规则；无 TBD。

**Type/命名一致性**：
- 下沉门面 `provider.service`（接口+Impl）↔ DTO 业务子包（catalog/channel/model/vendor）✓ 避免 `catalog/dto/ModelResponse` vs `model/dto/ModelResponse` 重名
- `provider/dto/ConnectivityTestResult`（分层）→ `provider.vendor` 与 `protocol.transport.ConnectivityTestResult` 不同包 ✓
- validator/tuner：`protocol.openai/anthropic` 插件包 + 插件 AutoConfiguration @Bean ✓；boot 不再扫插件类（无 @Component）
- gateway-web：`com.codingas.gateway.adapter.*` 包名不变，boot 依赖 web 后 classpath 可达 ✓

**风险**：
- 下沉后 boot 扫描 `application` 命中 init（保留）——Task 5 决策
- `@Component` validator 迁插件后 boot 扫描 `protocol` 包可能误扫插件类 → Task 3 明确改 @Bean 注册
- boot 测试引用 adapter 类（boot 依赖 web，classpath 可达）✓
- 跨域门面新增 provider→iam/resilience 依赖（用户已确认接受）
- OutboundTuner 集合注入收集迁移后的 tuner → Task 3 Step 5 专项验证
