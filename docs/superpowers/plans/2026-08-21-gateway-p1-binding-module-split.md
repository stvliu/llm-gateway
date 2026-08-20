# P1 绑定模块拆分 + 包名 Jmix 化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 全项目包名 Jmix 化（模块 = 根包，去 `domain/application/infrastructure` 前缀）+ 8 个业务域的 JPA/HTTP 技术实现拆到独立绑定模块（`-data`/`-http`），核心模块只保留纯领域逻辑 + 非绑定技术实现。

**Architecture:** 包名对齐 Jmix（`io.jmix.security` / `io.jmix.securitydata`）：核心模块根包 = 模块名（`provider`），绑定模块根包 = 拼接（`providerdata`/`providerhttp`，不用子包 `provider.data`——避免核心 `@ComponentScan` 误扫）。绑定拆分与包名迁移**一次到位**：核心类改包名留原模块，绑定类改包名移新模块。跨模块基础设施依赖（stats/audit/alert/resilience/proxy/boot）在 P1 以「依赖对应 -data/-http 模块」保持编译（过渡态），P4 解耦。

**Tech Stack:** Java 21、Spring Boot 3.5.13、Maven 多模块（`${revision}` CI 版本）、Spring Data JPA、OkHttp 4.12、Jackson、ArchUnit

## Global Constraints

### 包名迁移规则（设计文档 §4.2，已持久化到 doc/constitution.md §3.1）

- **R2 核心类**：`com.codingas.gateway.<layer>.<域段>.<rest>` → `com.codingas.gateway.<模块根包>.<rest>`（`layer` = domain/application/infrastructure，去掉；`域段` → 模块根包；`rest` 语义子包保留）
- **R3 绑定类**：同上但模块根包拼接绑定类型 → `<模块根包><binding>.<rest>`（JPA → `Xdata`，HTTP → `Xhttp`）
- **R1**：每个模块唯一根包，禁止同包跨模块
- **行为不变**：只改包名 + 模块归属，**不改任何业务逻辑**；`extends BaseDo` 的 DO 仍继承 `com.codingas.gateway.common.entity.BaseDo`（common 不变）

### 完整包名映射表（每个任务按此对照执行）

| 域（域段） | 核心模块根包 | JPA 绑定根包 | HTTP 绑定根包 |
|---|---|---|---|
| provider（`supply`） | `com.codingas.gateway.provider` | `com.codingas.gateway.providerdata` | `com.codingas.gateway.providerhttp` |
| iam（`iam` + `application`） | `com.codingas.gateway.iam` | `com.codingas.gateway.iamdata` | — |
| usage（`usage`） | `com.codingas.gateway.usage` | `com.codingas.gateway.usagedata` | — |
| security（`dataprotection` + `threat`） | `com.codingas.gateway.security` | `com.codingas.gateway.securitydata` | — |
| audit（`audit`） | `com.codingas.gateway.audit` | `com.codingas.gateway.auditdata` | — |
| alert（`alert`） | `com.codingas.gateway.alert` | `com.codingas.gateway.alertdata` | — |
| resilience（`resilience`） | `com.codingas.gateway.resilience` | `com.codingas.gateway.resiliencedata` | — |
| proxy（`proxy`） | `com.codingas.gateway.proxy` | — | — |
| stats（`stats`） | `com.codingas.gateway.stats` | — | — |

多域段子域映射：`supply`→`provider`；`iam`→`iam`、`application`→`iam.application`；`dataprotection`→`security.dataprotection`、`threat`→`security.threat`；其余 `域段`→`模块名` 一一对应。

### 非绑定技术实现留核心（设计文档 §4.3，不迁移到绑定模块）

| 域 | 留核心的实现（迁移到核心根包 `impl` 或原语义子包） |
|---|---|
| iam | `PasswordEncoder`、`Aes256EncryptionService`、`CredentialEncryptorAdapter`（encryption） |
| security | `InMemoryTokenBucketRateLimiter`、`SensitiveDataRuleInitializer` |
| resilience | `CircuitBreaker`、`CircuitBreakerState`、`ChannelEndpointCircuitBreakerManager`、`EndpointMetrics`、`EndpointMetricsRegistry`、`RetryExecutor`、`RetryStrategy`、`FastRetryStrategy`、`ExponentialBackoffStrategy`、`RateLimitRetryStrategy`、`ServiceUnavailableStrategy`、`GatewayRetryProperties`、`RetryableException`、`CircuitOpenException`、`ResilientClientFactoryImpl`、`ResilientUpstreamClient`（16 个） |
| provider | `BuiltinDataLoader`（catalog.loader）、`StubChannelKeyProbe` |
| proxy | `ProtocolStreamConverter`（infrastructure.protocol） |

### 构建与测试

- 全量：`.\mvnw clean install`；局部：`.\mvnw -pl <模块> -am clean install -DskipTests`
- 全量测试保持绿；覆盖率核心 ≥90% / 规则引擎 ≥85% / 适配器 ≥80% 不跌破
- 中文注释规范；`gateway-console`/`gateway-cli`/`gateway-simulator` 不受影响

### P1 过渡态（设计文档 §4.4，P4 解耦）

stats→providerdata/iamdata 的 Repository、audit→providerdata/iamdata 的 DO、alert→iamdata 的 DO、resilience→providerhttp 的 upstream client、proxy/boot→resilience 的熔断管理器，在 P1 以「pom 依赖对应绑定模块」保持编译，**不重构**。

---

### Task 1: 根 POM 注册新模块 + 新模块骨架 POM

**Files:**
- Modify: `pom.xml`（根，`<modules>` 段）
- Create: 8 个绑定模块骨架 pom：`gateway-provider-data`、`gateway-provider-http`、`gateway-iam-data`、`gateway-usage-data`、`gateway-security-data`、`gateway-audit-data`、`gateway-alert-data`、`gateway-resilience-data`

**Interfaces:**
- Consumes: 无
- Produces: 8 个空绑定模块（可构建），供后续任务迁移类

- [ ] **Step 1: 根 `pom.xml` 的 `<modules>` 追加 8 个绑定模块**

按依赖序插入（紧跟各自核心模块之后）：`gateway-provider` 后加 `gateway-provider-data`、`gateway-provider-http`；`gateway-iam` 后加 `gateway-iam-data`；`gateway-usage` 后加 `gateway-usage-data`；`gateway-security` 后加 `gateway-security-data`；`gateway-audit` 后加 `gateway-audit-data`；`gateway-alert` 后加 `gateway-alert-data`；`gateway-resilience` 后加 `gateway-resilience-data`。

- [ ] **Step 2: 创建 8 个骨架 pom（JPA 绑定模板）**

每个 `gateway-xxx-data/pom.xml` 参照以下模板（依赖：本域核心 + common + spring-boot-starter-data-jpa + lombok + 测试；**audit-data 额外依赖 providerdata + iamdata；alert-data 额外依赖 iamdata**——过渡态）：

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
    </parent>
    <artifactId>gateway-xxx-data</artifactId>
    <packaging>jar</packaging>
    <name>Gateway XXX Data</name>
    <description>XXX 域 JPA 绑定模块（根包 xxxdata）</description>
    <dependencies>
        <dependency>
            <groupId>com.codingas.gateway</groupId>
            <artifactId>gateway-xxx</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>com.codingas.gateway</groupId>
            <artifactId>gateway-common</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

`gateway-provider-http/pom.xml` 不同处：依赖 `gateway-provider`、`gateway-common`、`gateway-protocol`、`okhttp`、`jackson-databind`（无 data-jpa）。

- [ ] **Step 3: 空模块构建验证**

Run: `.\mvnw -pl gateway-provider-data,gateway-provider-http,gateway-iam-data,gateway-usage-data,gateway-security-data,gateway-audit-data,gateway-alert-data,gateway-resilience-data -am clean install -DskipTests`
Expected: BUILD SUCCESS（8 个空模块骨架可构建）

- [ ] **Step 4: Commit**

```bash
git add pom.xml gateway-provider-data gateway-provider-http gateway-iam-data gateway-usage-data gateway-security-data gateway-audit-data gateway-alert-data gateway-resilience-data
git commit -m "build: 新增 8 个绑定模块骨架 pom 并注册根模块（P1）"
```

---

### Task 2: provider 域 —— 包名 Jmix 化 + provider-data/http 拆分

**Files:**
- Modify（gateway-provider 核心，包名 `domain/application/infrastructure.supply.*` → `provider.*`）：
  - `domain/supply/**` → 留核心，包名改 `provider.**`（entity/gateway/catalog/enums/exception/valueobject 子包保留）
  - `application/supply/**` → 留核心，包名改 `provider.**`（`ChannelHealthService`、`dto`）
  - `infrastructure/supply/catalog/loader/BuiltinDataLoader.java`、`infrastructure/supply/gateway/StubChannelKeyProbe.java` → 留核心，包名改 `provider.catalog.loader` / `provider.gateway`
- Create（gateway-provider-data，包名 `supply` → `providerdata`）：`infrastructure/supply/gateway/database/**`（DO 6 + REPO 6 + JPA-IMPL 7 + catalog database + repository 的 `ChannelOperationLogJpaEntity/JpaRepository` 等 27 个 JPA 类）
- Create（gateway-provider-http，包名 → `providerhttp`）：`infrastructure/supply/upstream/**`（3 个）+ `infrastructure/upstream/**`（4 个）+ `infrastructure/supply/gateway/ConnectivityTesterImpl.java`（1 个，共 8 个 HTTP 类）
- Modify（引用方 pom）：`gateway-audit`、`gateway-stats`（+ providerdata）；`gateway-resilience`（+ providerhttp）
- Modify（引用方 import）：所有引用 `com.codingas.gateway.domain.supply.*`/`application.supply.*`/`infrastructure.supply.*`/`infrastructure.upstream.*` 的模块（见 Step 2 清单）
- Modify: `gateway-provider/pom.xml`（移除 `spring-boot-starter-data-jpa`、`okhttp`）

**Interfaces:**
- Consumes: 无（首拆）
- Produces: `com.codingas.gateway.provider`（核心 API：`provider.gateway.ChannelGateway` 等）+ `providerdata`（`providerdata.dataobject.ChannelDo` 等）+ `providerhttp`（`providerhttp.upstream.*`）

- [ ] **Step 1: 用 IDE 重构或脚本批量改包名（核心类）**

把 `gateway-provider/src/main/java/com/codingas/gateway/` 下：
- `domain.supply.` → `provider.`
- `application.supply.` → `provider.`
- 对留在核心的 `infrastructure.supply.` 类（BuiltinDataLoader/StubChannelKeyProbe）→ `provider.`

逐文件替换 `package` 与内部 import 引用（`sed -i 's/com\.codingas\.gateway\.domain\.supply\./com.codingas.gateway.provider./g'` 等，或 IDE Move 重构）。**绑定类（JPA/HTTP）暂不改**，先只处理留核心的类？——**不**：绑定类也要在 Step 2 一起处理（一次到位）。

**推荐做法**：对整个 provider 源树做**整体包名替换 + 模块移动**，一次完成：
- 核心类：包名 `supply` → `provider`，留在 gateway-provider
- JPA 类（database/** + repository/ 下 JPA）：包名 `supply` → `providerdata`，移到 gateway-provider-data
- HTTP 类（upstream/** + ConnectivityTesterImpl）：包名 → `providerhttp`，移到 gateway-provider-http

- [ ] **Step 2: 更新全部引用方 import（supply → provider/providerdata/providerhttp）**

引用 `com.codingas.gateway.domain.supply.*` 的模块（改 → `provider.*`）：
- `gateway-proxy/src/main/**`、`gateway-boot/src/main/**`、`gateway-usage/src/main/**`、`gateway-resilience/src/main/**`、`gateway-iam/src/main/**`（CredentialEncryptor）

引用 `com.codingas.gateway.infrastructure.supply.*` 的模块（JPA 类 → `providerdata.*`）：
- `gateway-audit/.../UsageLogDo.java`（ModelDo/ProviderDo）
- `gateway-stats/.../StatsService.java`（ChannelRepository/ModelRepository/ProviderRepository）
- `gateway-boot/src/test/**`、`gateway-proxy/src/test/**`

引用 `infrastructure.supply.upstream.*`/`infrastructure.upstream.*` 的模块（→ `providerhttp.*`）：
- `gateway-resilience/.../ResilientClientFactoryImpl.java`

用 `grep -rl "com\.codingas\.gateway\.\(domain\|application\|infrastructure\)\.supply\.\|infrastructure\.upstream\." --include="*.java"` 找出全部命中文件后逐个替换。

- [ ] **Step 3: 更新引用方 pom 依赖**

- `gateway-audit/pom.xml` + `gateway-stats/pom.xml`：追加 `gateway-provider-data`
- `gateway-resilience/pom.xml`：追加 `gateway-provider-http`
- `gateway-boot/pom.xml`：若测试引 provider DO/REPO，追加 `gateway-provider-data`（scope test）
- 各引用方若原本因依赖 provider 拿到 DO/REPO，现在需显式声明（按 Step 2 grep 结果核对）

- [ ] **Step 4: 核心模块 pom 瘦身**

`gateway-provider/pom.xml`：移除 `spring-boot-starter-data-jpa`、`okhttp`（JPA/HTTP 类已迁走；若仍有使用则保留并说明）。

- [ ] **Step 5: 构建验证**

Run: `.\mvnw -pl gateway-provider,gateway-provider-data,gateway-provider-http,gateway-audit,gateway-stats,gateway-resilience,gateway-proxy,gateway-boot,gateway-usage,gateway-iam -am clean install -DskipTests`
Expected: BUILD SUCCESS（全部引用方编译通过）

- [ ] **Step 6: 跑受影响模块测试**

Run: `.\mvnw -pl gateway-provider,gateway-provider-data,gateway-provider-http,gateway-audit,gateway-stats test`
Expected: 全部 PASS

- [ ] **Step 7: Commit**

```bash
git add -A gateway-provider gateway-provider-data gateway-provider-http gateway-audit gateway-stats gateway-resilience gateway-proxy gateway-boot gateway-usage gateway-iam
git commit -m "refactor: provider 域包名 Jmix 化（provider/providerdata/providerhttp）+ 绑定拆分（P1）"
```

---

### Task 3: iam 域 —— 包名 Jmix 化 + iam-data 拆分（加密留核心）

**Files:**
- Modify（gateway-iam 核心，`domain.iam.*`→`iam.*`、`domain.application.*`→`iam.application.*`、`application.*`→`iam.*`）：
  - `domain/iam/**`、`domain/application/**`、`application/**` → 留核心，包名改 `iam.**`（entity/gateway/service/enums/exception/valueobject 子包保留）
  - `infrastructure/iam/gateway/encryption/**`（PasswordEncoder/Aes256EncryptionService/CredentialEncryptorAdapter）→ 留核心，包名改 `iam.gateway.encryption`（非绑定）
- Create（gateway-iam-data，包名 → `iamdata`）：`infrastructure/iam/gateway/database/**`（UserDo/UserApiKeyDo/REPO/IMPL）+ `infrastructure/application/gateway/database/**`（ApplicationDo/ApplicationChannelDo/REPO/IMPL），共 12 个 JPA 类，包名 `iamdata.*`/`iamdata.application.*`
- Modify（引用方 pom）：`gateway-audit`、`gateway-alert`、`gateway-stats`（+ iamdata）
- Modify（引用方 import）：引用 `domain.iam.*`/`domain.application.*`/`infrastructure.iam.*`/`infrastructure.application.*` 的模块
- Modify: `gateway-iam/pom.xml`（移除 `spring-boot-starter-data-jpa`）

**Interfaces:**
- Consumes: Task 2 的 `provider` 包名（`CredentialEncryptorAdapter` 引 `provider.gateway.CredentialEncryptor`）
- Produces: `com.codingas.gateway.iam`（核心）+ `iamdata`

- [ ] **Step 1: 改包名 + 迁移 JPA 类（iam 域一次到位）**

核心类（留 gateway-iam）：`domain.iam.`/`domain.application.`/`application.` → `iam.`（application 子域 → `iam.application.`）；encryption 类 → `iam.gateway.encryption`。
绑定类（移 gateway-iam-data）：`infrastructure.iam.` → `iamdata.`、`infrastructure.application.` → `iamdata.application.`。

- [ ] **Step 2: 更新引用方 import**

- `gateway-audit/.../UsageLogDo.java`：`infrastructure.iam.gateway.database.dataobject.UserDo` → `iamdata.gateway.database.dataobject.UserDo`
- `gateway-alert/.../AlertNotificationDo.java`：同上
- `gateway-stats/.../StatsService.java`：`UserRepository` → `iamdata.gateway.database.repository.UserRepository`
- 其余引用 `domain.iam.*`/`domain.application.*`/`application.*`(iam) 的模块：proxy/boot/usage/resilience，`grep -rl "com\.codingas\.gateway\.\(domain\|application\|infrastructure\)\.\(iam\|application\)\."` 确认后替换
- `gateway-boot/.../UserCreator.java`：`infrastructure.iam.gateway.encryption.PasswordEncoder` → `iam.gateway.encryption.PasswordEncoder`（encryption 留核心）

- [ ] **Step 3: 更新引用方 pom**

`gateway-audit`、`gateway-alert`、`gateway-stats`：追加 `gateway-iam-data`。`gateway-boot` 若测试引 iam DO 追加 `gateway-iam-data`(test)。

- [ ] **Step 4: 核心模块 pom 瘦身**

`gateway-iam/pom.xml` 移除 `spring-boot-starter-data-jpa`。

- [ ] **Step 5: 构建验证**

Run: `.\mvnw -pl gateway-iam,gateway-iam-data,gateway-audit,gateway-alert,gateway-stats,gateway-proxy,gateway-boot,gateway-usage,gateway-resilience -am clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 6: 跑受影响模块测试**

Run: `.\mvnw -pl gateway-iam,gateway-iam-data,gateway-audit,gateway-alert test`
Expected: 全部 PASS

- [ ] **Step 7: Commit**

```bash
git add -A gateway-iam gateway-iam-data gateway-audit gateway-alert gateway-stats gateway-proxy gateway-boot gateway-usage gateway-resilience
git commit -m "refactor: iam 域包名 Jmix 化（iam/iamdata）+ 绑定拆分，加密留核心（P1）"
```

---

### Task 4: usage 域 —— 包名 Jmix 化 + usage-data 拆分

**Files:**
- Modify（gateway-usage 核心，`domain.usage.*`/`domain.quota.*`/`application.quota.*` → `usage.*`）：`domain/usage/**`、`domain/quota/**`、`application/quota/**` 留核心，包名改 `usage.**`
- Create（gateway-usage-data，包名 → `usagedata`）：`infrastructure/usage/gateway/database/dataobject/*`（TokenLimitDo/RateLimitConfigDo）+ `infrastructure/usage/gateway/database/TokenLimitRepository` + `infrastructure/usage/gateway/TokenLimitGatewayImpl`（4 个）
- Modify（引用方 import）：引用 `domain.usage.*`/`domain.quota.*` 的模块（audit/alert/proxy/boot）
- Modify: `gateway-usage/pom.xml`（移除 `spring-boot-starter-data-jpa`）

**Interfaces:**
- Consumes: `provider`/`iam` 包名（`TokenLimit` 引 `provider.entity.Model/Provider`、`iam.entity.User`）
- Produces: `com.codingas.gateway.usage` + `usagedata`

- [ ] **Step 1: 改包名 + 迁移 JPA 类**

核心：`domain.usage.`/`domain.quota.`/`application.quota.` → `usage.`。绑定：`infrastructure.usage.` → `usagedata.`（移 gateway-usage-data）。

- [ ] **Step 2: 更新引用方 import**

`grep -rl "com\.codingas\.gateway\.\(domain\|application\)\.\(usage\|quota\)\."` 确认（audit/alert/proxy/boot），替换为 `usage.`。

- [ ] **Step 3: 核心 pom 瘦身**

`gateway-usage/pom.xml` 移除 `spring-boot-starter-data-jpa`。

- [ ] **Step 4: 构建验证**

Run: `.\mvnw -pl gateway-usage,gateway-usage-data,gateway-audit,gateway-alert,gateway-proxy,gateway-boot -am clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: 跑受影响模块测试**

Run: `.\mvnw -pl gateway-usage,gateway-usage-data test`
Expected: 全部 PASS

- [ ] **Step 6: Commit**

```bash
git add -A gateway-usage gateway-usage-data gateway-audit gateway-alert gateway-proxy gateway-boot
git commit -m "refactor: usage 域包名 Jmix 化（usage/usagedata）+ 绑定拆分（P1）"
```

---

### Task 5: security 域 —— 包名 Jmix 化 + security-data 拆分（限流留核心）

**Files:**
- Modify（gateway-security 核心，`domain.dataprotection.*`→`security.dataprotection.*`、`domain.threat.*`→`security.threat.*`）：`domain/**` 留核心
- **留核心**：`infrastructure/dataprotection/SensitiveDataRuleInitializer`、`infrastructure/threat/gateway/InMemoryTokenBucketRateLimiter`（包名 → `security.dataprotection`/`security.threat`）
- Create（gateway-security-data，包名 → `securitydata`）：`infrastructure/dataprotection/gateway/database/**`（SensitiveDataRuleDo/REPO/GatewayImpl/Converter）+ `infrastructure/threat/gateway/database/**`（IpBlocklistDo/REPO/GatewayImpl/Converter），共 8 个 JPA 类，包名 `securitydata.dataprotection.*`/`securitydata.threat.*`
- Modify（引用方 import）：引用 `domain.threat.*`/`domain.dataprotection.*` 的模块（boot/proxy）
- Modify: `gateway-security/pom.xml`（移除 `spring-boot-starter-data-jpa`）

**Interfaces:**
- Consumes: `provider`/`iam` 包名
- Produces: `com.codingas.gateway.security` + `securitydata`

- [ ] **Step 1: 改包名 + 迁移 JPA 类**

核心：`domain.dataprotection.` → `security.dataprotection.`、`domain.threat.` → `security.threat.`；`SensitiveDataRuleInitializer`/`InMemoryTokenBucketRateLimiter` → `security.dataprotection`/`security.threat`。
绑定：`infrastructure.dataprotection.` → `securitydata.dataprotection.`、`infrastructure.threat.` → `securitydata.threat.`（移 gateway-security-data）。

- [ ] **Step 2: 更新引用方 import**

`grep -rl "com\.codingas\.gateway\.\(domain\|infrastructure\)\.\(threat\|dataprotection\)\."` 确认（boot/proxy），替换为 `security.`/`securitydata.` 对应。

- [ ] **Step 3: 核心 pom 瘦身**

`gateway-security/pom.xml` 移除 `spring-boot-starter-data-jpa`。

- [ ] **Step 4: 构建验证**

Run: `.\mvnw -pl gateway-security,gateway-security-data,gateway-boot,gateway-proxy -am clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: 跑受影响模块测试**

Run: `.\mvnw -pl gateway-security,gateway-security-data test`
Expected: 全部 PASS

- [ ] **Step 6: Commit**

```bash
git add -A gateway-security gateway-security-data gateway-boot gateway-proxy
git commit -m "refactor: security 域包名 Jmix 化（security/securitydata）+ 绑定拆分，限流留核心（P1）"
```

---

### Task 6: audit 域 —— 包名 Jmix 化 + audit-data 拆分

**Files:**
- Modify（gateway-audit 核心，`domain.audit.*`→`audit.*`、`application.audit.*`→`audit.*`）：`domain/audit/**`、`application/audit/**` 留核心
- Create（gateway-audit-data，包名 → `auditdata`）：`infrastructure/audit/gateway/**`（DO 3 + REPO 3 + IMPL 2，共 8 个 JPA 类），包名 `auditdata.**`
- Modify（引用方 import）：`gateway-proxy`（`domain.audit.entity.CallLog` 等 → `audit.entity.CallLog`）、`gateway-boot`
- Modify: `gateway-audit/pom.xml`（移除 data-jpa；保留 providerdata/iamdata 依赖——UsageLogDo 引跨域 DO）

**Interfaces:**
- Consumes: `providerdata`/`iamdata`（UsageLogDo 引 ModelDo/ProviderDo/UserDo，过渡态）
- Produces: `com.codingas.gateway.audit` + `auditdata`

- [ ] **Step 1: 改包名 + 迁移 JPA 类**

核心：`domain.audit.` → `audit.`、`application.audit.` → `audit.`。绑定：`infrastructure.audit.` → `auditdata.`（移 gateway-audit-data）。`UsageLogDo` 的跨域 import（`providerdata`/`iamdata`）保持 Task 2/3 已改的包名。

- [ ] **Step 2: 更新引用方 import**

`grep -rl "com\.codingas\.gateway\.\(domain\|application\|infrastructure\)\.audit\."` 确认（proxy/boot），替换为 `audit.`/`auditdata.` 对应。

- [ ] **Step 3: 核心 pom 调整**

`gateway-audit/pom.xml`：移除 `spring-boot-starter-data-jpa`；**保留** `gateway-provider-data`、`gateway-iam-data`（以 grep 确认 audit 核心剩余类是否仍引用跨域 DO，若已全部迁到 audit-data 则依赖随迁移到 audit-data 的 pom）。

- [ ] **Step 4: 构建验证**

Run: `.\mvnw -pl gateway-audit,gateway-audit-data,gateway-proxy,gateway-boot -am clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: 跑受影响模块测试**

Run: `.\mvnw -pl gateway-audit,gateway-audit-data,gateway-proxy test`
Expected: 全部 PASS

- [ ] **Step 6: Commit**

```bash
git add -A gateway-audit gateway-audit-data gateway-proxy gateway-boot
git commit -m "refactor: audit 域包名 Jmix 化（audit/auditdata）+ 绑定拆分（P1）"
```

---

### Task 7: alert 域 —— 包名 Jmix 化 + alert-data 拆分

**Files:**
- Modify（gateway-alert 核心，`domain.alert.*`→`alert.*`）：`domain/alert/**` 留核心
- Create（gateway-alert-data，包名 → `alertdata`）：`infrastructure/alert/gateway/database/dataobject/*`（AlertNotificationDo/AlertRuleDo，2 个 JPA DO），包名 `alertdata.**`
- Modify: `gateway-alert/pom.xml`（移除 data-jpa；保留 iamdata 依赖——AlertNotificationDo 引 UserDo）

**Interfaces:**
- Consumes: `iamdata`（AlertNotificationDo 引 UserDo，过渡态）
- Produces: `com.codingas.gateway.alert` + `alertdata`

- [ ] **Step 1: 改包名 + 迁移 JPA DO**

核心：`domain.alert.` → `alert.`。绑定：`infrastructure.alert.` → `alertdata.`（移 gateway-alert-data）。

- [ ] **Step 2: 更新引用方 import**

`grep -rl "com\.codingas\.gateway\.\(domain\|infrastructure\)\.alert\."` 确认，替换为 `alert.`/`alertdata.` 对应。

- [ ] **Step 3: 核心 pom 调整**

`gateway-alert/pom.xml`：移除 `spring-boot-starter-data-jpa`；保留 `gateway-iam-data`（以 grep 确认）。

- [ ] **Step 4: 构建验证**

Run: `.\mvnw -pl gateway-alert,gateway-alert-data -am clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: 跑受影响模块测试**

Run: `.\mvnw -pl gateway-alert,gateway-alert-data test`
Expected: 全部 PASS

- [ ] **Step 6: Commit**

```bash
git add -A gateway-alert gateway-alert-data
git commit -m "refactor: alert 域包名 Jmix 化（alert/alertdata）+ 绑定拆分（P1）"
```

---

### Task 8: resilience 域 —— 包名 Jmix 化 + resilience-data 拆分（熔断/重试留核心）

**Files:**
- Modify（gateway-resilience 核心，`domain.resilience.*`→`resilience.*`、`application.resilience.*`→`resilience.*`）：`domain/resilience/**`、`application/resilience/**` 留核心
- **留核心**（`infrastructure/resilience/**` 非 database 的 16 个熔断/重试/指标类）：`CircuitBreaker`、`CircuitBreakerState`、`ChannelEndpointCircuitBreakerManager`、`EndpointMetrics`、`EndpointMetricsRegistry`、`RetryExecutor`、`RetryStrategy`、`FastRetryStrategy`、`ExponentialBackoffStrategy`、`RateLimitRetryStrategy`、`ServiceUnavailableStrategy`、`GatewayRetryProperties`、`RetryableException`、`CircuitOpenException`、`ResilientClientFactoryImpl`、`ResilientUpstreamClient`（包名 → `resilience.*`）
- Create（gateway-resilience-data，包名 → `resiliencedata`）：`infrastructure/resilience/gateway/database/**`（FailoverEventDo/REPO/GatewayImpl，3 个），包名 `resiliencedata.**`
- Modify（引用方 import）：`gateway-proxy`（`infrastructure.resilience.ChannelEndpointCircuitBreakerManager` → `resilience.ChannelEndpointCircuitBreakerManager`）、`gateway-boot`（CircuitOpenException/ChannelEndpointCircuitBreakerManager）
- Modify: `gateway-resilience/pom.xml`（移除 data-jpa；保留 providerhttp——ResilientClientFactoryImpl 引 supplyhttp client）

**Interfaces:**
- Consumes: `providerhttp`（ResilientClientFactoryImpl 引 AnthropicUpstreamClient/OpenAIUpstreamClient，过渡态）
- Produces: `com.codingas.gateway.resilience` + `resiliencedata`

- [ ] **Step 1: 改包名 + 迁移 JPA 类**

核心：`domain.resilience.` → `resilience.`、`application.resilience.` → `resilience.`；`infrastructure.resilience.`（非 database）→ `resilience.`（16 个留核心）。绑定：`infrastructure.resilience.gateway.database.**` → `resiliencedata.`（移 gateway-resilience-data）。

- [ ] **Step 2: 更新引用方 import**

`gateway-proxy/.../KeyFailoverInvoker.java`、`.../routing/HealthRouter.java`：`infrastructure.resilience.ChannelEndpointCircuitBreakerManager` → `resilience.ChannelEndpointCircuitBreakerManager`。`gateway-boot/.../GlobalExceptionHandler.java`（CircuitOpenException）、`.../ChannelEmergencyServiceImpl.java`（ChannelEndpointCircuitBreakerManager）同理。`grep -rl "com\.codingas\.gateway\.infrastructure\.resilience\."` 确认无遗漏。

- [ ] **Step 3: 核心 pom 调整**

`gateway-resilience/pom.xml`：移除 `spring-boot-starter-data-jpa`；保留 `gateway-provider-http`（ResilientClientFactoryImpl 引 supplyhttp client）。

- [ ] **Step 4: 构建验证**

Run: `.\mvnw -pl gateway-resilience,gateway-resilience-data,gateway-provider-http,gateway-proxy,gateway-boot -am clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: 跑受影响模块测试**

Run: `.\mvnw -pl gateway-resilience,gateway-resilience-data,gateway-proxy test`
Expected: 全部 PASS

- [ ] **Step 6: Commit**

```bash
git add -A gateway-resilience gateway-resilience-data gateway-provider-http gateway-proxy gateway-boot
git commit -m "refactor: resilience 域包名 Jmix 化（resilience/resiliencedata）+ 绑定拆分，熔断重试留核心（P1）"
```

---

### Task 9: proxy / stats 域 —— 纯包名 Jmix 化（无绑定拆分）

**Files:**
- Modify（gateway-proxy，`application.proxy.*`/`application.protocol.*`/`infrastructure.protocol.*` → `proxy.*`）：`application/**`、`infrastructure/protocol/ProtocolStreamConverter.java` 包名改 `proxy.**`
- Modify（gateway-stats，`application.stats.*` → `stats.*`）：`application/**` 包名改 `stats.**`
- Modify（引用方 import）：`gateway-boot`（引 proxy 的 `application.proxy.*` 与 stats 的 `application.stats.*`）
- Modify: `gateway-stats/pom.xml`（确认已含 `gateway-provider-data`、`gateway-iam-data` 依赖——StatsService 引 Repository，过渡态）

**Interfaces:**
- Consumes: 全部已完成包名迁移的域（provider/iam/usage/security/audit/alert/resilience）
- Produces: `com.codingas.gateway.proxy` + `stats`（纯包名迁移完成）

- [ ] **Step 1: proxy 包名迁移**

`gateway-proxy`：`application.proxy.` → `proxy.`、`application.protocol.` → `proxy.protocol.`、`infrastructure.protocol.` → `proxy.protocol.`（ProtocolStreamConverter）。保留 `proxy` 子包结构。

- [ ] **Step 2: stats 包名迁移**

`gateway-stats`：`application.stats.` → `stats.`。确认 pom 含 `gateway-provider-data`、`gateway-iam-data`（Task 2/3 已加）。

- [ ] **Step 3: 更新引用方 import**

`gateway-boot`：`grep -rl "com\.codingas\.gateway\.\(application\|infrastructure\)\.\(proxy\|protocol\|stats\)\."` 确认引用 proxy/stats 的类，替换为 `proxy.`/`stats.` 对应。

- [ ] **Step 4: 构建验证**

Run: `.\mvnw -pl gateway-proxy,gateway-stats,gateway-boot -am clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: 跑受影响模块测试**

Run: `.\mvnw -pl gateway-proxy,gateway-stats,gateway-boot test`
Expected: 全部 PASS

- [ ] **Step 6: Commit**

```bash
git add -A gateway-proxy gateway-stats gateway-boot
git commit -m "refactor: proxy/stats 域包名 Jmix 化（proxy/stats，P1）"
```

---

### Task 10: ArchUnit 重写 + 全量验证

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/arch/LayerDependencyTest.java`（重写为模块级规则）
- Verify: 全量构建 + 全量测试 + 覆盖率

**Interfaces:**
- Consumes: 全部 P1 拆分结果
- Produces: P1 完成基线（模块级 ArchUnit 铁律雏形 + 全量绿）

- [ ] **Step 1: 重写 ArchUnit 为模块级规则**

现有 `LayerDependencyTest` 基于 `com.codingas.gateway.domain..` 等顶层前缀（包名迁移后失效）。重写为**模块级规则**（`@AnalyzeClasses(packages = "com.codingas.gateway")` 不变）：

```java
// 核心规则：跨模块只允许依赖对方根包 API，禁止依赖绑定模块根包/impl
@ArchTest
static final ArchRule NO_DEPEND_ON_BINDING_MODULES = freeze(
    noClasses()
        .that().resideInAPackage("com.codingas.gateway.provider..")
        .or().resideInAPackage("com.codingas.gateway.iam..")
        // ... 全部核心模块根包
        .should().dependOnClassesThat()
            .resideInAnyPackage("com.codingas.gateway.providerdata..",
                                "com.codingas.gateway.providerhttp..",
                                "com.codingas.gateway.iamdata..",
                                /* ...全部绑定模块根包 */));
```

并保留「common 不依赖业务层」等仍有效的规则。因 P1 过渡态（stats/audit/alert 依赖其他域 data）暂用 `freeze()` 冻结已知违规，P4 解除。

- [ ] **Step 2: 全量构建**

Run: `.\mvnw clean install`
Expected: BUILD SUCCESS（17 + 8 个模块全部通过）

- [ ] **Step 3: 全量测试**

Run: `.\mvnw test`
Expected: 全部 PASS，0 失败

- [ ] **Step 4: ArchUnit 验证**

Run: `.\mvnw -pl gateway-boot test -Dtest=LayerDependencyTest`
Expected: 全部 PASS

- [ ] **Step 5: 覆盖率校验**

Run: `.\mvnw jacoco:report`（或项目既有覆盖率检查）
Expected: 核心 ≥90% / 规则引擎 ≥85% / 适配器 ≥80%（拆分不跌破）

- [ ] **Step 6: 清理空目录**

删除迁移残留空目录：`find gateway-provider/src/main/java gateway-iam/src/main/java gateway-boot/src/main/java -type d -empty -delete`（含 boot 下 `infrastructure/supply`、`upstream`、`application/protocol/conversion`、`domain` 等）。

- [ ] **Step 7: 提交收尾**

```bash
git add -A gateway-boot
git commit -m "test: ArchUnit 重写为模块级规则 + P1 全量验证收尾"
```

- [ ] **Step 8: P1 完成检查**

对照设计文档 §4.2 包名映射表逐域确认：核心根包 = 模块名（R2 ✅）；绑定模块拼接根包（R1/R3 ✅）；非绑定实现留核心（§4.3 ✅）；过渡态依赖已登记待 P4（§4.4 ✅）。

---

## P1 完成定义（Definition of Done）

- 全量 `.\mvnw clean install` 成功（25 个模块）
- 全量测试绿（0 失败）；ArchUnit 模块级规则绿；覆盖率 ≥90/85/80%
- 9 个业务域包名全部 Jmix 化（`provider`/`iam`/`usage`/`security`/`audit`/`alert`/`resilience`/`proxy`/`stats` 根包）
- provider 拆出 `-data` + `-http`；iam/usage/security/audit/alert/resilience 各拆出 `-data`
- 非绑定技术实现（加密/限流/熔断/重试/转换/启动装载）全部留核心模块
- 行为不变：无业务逻辑改动（纯包名 + 模块归属迁移）

## P1 之后的后续阶段（不在本计划内）

- **P2**：核心模块 `@Configuration` 装配入口（`@ComponentScan` 限定本域根包）+ 各 `-starter`（AutoConfiguration）+ boot 依赖切换 + 装配显式化
- **P3**：boot 瘦身 + `gateway-web` 独立 + 协议插件自包含
- **P4**：ArchUnit 模块级铁律解冻 + 跨模块 DO 依赖解耦（stats/audit/alert/resilience/proxy 过渡态清零）
