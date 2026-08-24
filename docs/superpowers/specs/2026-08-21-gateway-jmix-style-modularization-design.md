# LLM-Gateway 模块化改造设计（Jmix 内涵级对齐）

> 日期：2026-08-21
> 状态：已获用户批准（设计评审通过）
> 范围：模块边界重划——核心（纯逻辑）/ 绑定（技术）/ starter（装配）三态模块
> 参考：Jmix 3.0.1 源码（E:\workspace\jmix-3.0.1）

## 1. 背景与目标

LLM-Gateway 已完成第一轮模块化（`modularization-restructure` 已归档），当前为 17 个 Maven 模块。但现状与「模块依赖 API、实现与组装用 starter」的目标仍有差距：技术实现（JPA DO/Repository/HTTP client）与领域核心处于同一模块，装配靠 `@SpringBootApplication` 全包扫描隐式完成，业务域模块没有独立的 starter 装配。

本次改造**参照 Jmix 框架的模块化内涵**（而非表面机制）重划模块边界，达到：

1. **模块依赖 API**：跨模块依赖只能命中对方域的根包 API（领域模型 + 端口接口），JPA DO/Repository/HTTP 实现物理不可见、不可依赖。
2. **实现与组装用 starter**：每个业务域拆分为「核心模块（纯逻辑）+ 绑定模块（技术实现）+ starter（纯装配）」三态；装配显式化，替代隐式包扫描。

**行为不变**：本次为结构性重构，存量运行时行为（协议转换语义、路由、配额、审计等）保持不变。

## 2. Jmix 模块化内涵分析（源码证据）

Jmix 的模块化不是按子域划分，而是**按依赖方向划分**，每个功能域拆成三态模块。以 `jmix-security` 为例（jmix-3.0.1 源码）：

```
jmix-security/
├── security/               # ① 核心模块：纯领域逻辑 + API 根包 + 纯逻辑 impl
│                           #    security.gradle 零 JPA/Spring Boot 依赖
│                           #    io.jmix.security 根包 7 个 API 文件
│                           #    io.jmix.security.impl 38 个纯逻辑实现文件
│                           #    模型是纯 POJO（@JmixEntity ResourceRoleModel，非 JPA @Entity）
├── security-data/          # ② 绑定模块：JPA 实体/Repository 实现
│                           #    securitydata/entity/ResourceRoleEntity（JPA @Entity + @Table）
│                           #    DatabaseRolePersistence 等 JPA 实现
│                           #    依赖 security 核心 + JPA 技术
├── security-flowui/        # ② 绑定模块：UI 实现
├── security-starter/       # ③ 装配：@AutoConfiguration + @Import(SecurityConfiguration)
│                           #    只用 spring-boot-autoconfigure，零业务逻辑
└── security-data-starter/  # ③ 装配：绑定模块的装配
```

**Jmix 内涵的三条铁律**：

1. **核心模块零技术依赖**：`security.gradle` 无 jakarta.persistence / spring-boot / eclipselink；核心模块的领域模型是纯 POJO（`@JmixEntity`），真正的 JPA 实体在绑定模块（`security-data`）。
2. **技术实现拆独立绑定模块**：JPA（`-data`）、UI（`-flowui`）、远程数据源（`-restds`）各成绑定模块，依赖核心模块 + 具体技术，应用按需组装。
3. **starter 纯装配**：`SecurityAutoConfiguration` 只含 `@AutoConfiguration` + `@Import({CoreConfiguration, SecurityConfiguration})` + `@ConditionalOnMissingBean` + bean 注册；`META-INF/spring/...AutoConfiguration.imports` 注册；`@ConfigurationProperties`（`SecurityProperties`）放核心模块，构造器绑定 + `@DefaultValue`，`@ConfigurationPropertiesScan` 统一扫描。

**配套运行时秩序机制**（Jmix 模块化的"灵魂"）：

- `@JmixModule(id, dependsOn)`：主 `@Configuration` 类声明模块 id 与依赖
- `JmixModulesProcessor`：`BeanDefinitionRegistryPostProcessor`，启动早期扫描 `@JmixModule` 配置，按依赖拓扑排序、校验 id 唯一性，把各模块 `module.properties` 注册为 PropertySource
- `JmixModules`：模块注册表（`getAll()`/`get(id)`/`getLast()`/`getPropertyValues()` 加法式属性合并）
- `JmixModulesAwareBeanSelector.selectFrom(Collection<T>)`：从多个同类型 bean 选出模块层级最低者（应用 > 绑定模块 > 核心），实现"高层模块覆盖框架默认"而无需 `allow-bean-definition-overriding`
- `module.properties`：每模块一个加法式配置文件
- `@Internal`：标注内部实现，跨模块不得使用
- `AccessConstraintsRegistry`：注册表模式做可插拔策略
- 事件机制：`EntityChangedEvent`（`ApplicationEvent + ResolvableTypeProvider`）跨模块解耦

> **注**：以上运行时秩序机制是 Jmix 为「框架被下游应用扩展」设计的。llm-gateway 是单体应用，模块拓扑编译期静态确定，**本次不实现**（用户评估决策），替代方案见 §5.3。

## 3. llm-gateway 现状与差距

### 3.1 已具备的 Jmix 内核（源码核实）

| Jmix 内涵 | llm-gateway 现状 | 证据 |
|---|---|---|
| 核心模块纯模型 | `@DomainEntity` 纯 POJO（`@Component` 原型 Bean + `BaseEntity` 抽象类，零 JPA 注解） | `common/entity/DomainEntity.java`、`domain/supply/entity/Channel.java` |
| 核心模块零技术依赖 | domain 层 0 个 JPA/Redis/HTTP import | `grep -r "jakarta.persistence\|redis\|okhttp\|RestClient" gateway-provider/.../domain` = 0 |
| 端口接口（API） | 9 个 `*Gateway` 端口接口 | `ChannelGateway/ProviderGateway/ModelGateway/...` |
| JPA 实体独立于领域模型 | JPA DO（`@Entity`）独立于领域 POJO | `infrastructure/supply/**/dataobject/ChannelDo`（9 个 `@Entity`）vs `@DomainEntity`（6 个） |

llm-gateway 已天然采用 Jmix 的 model/entity 分离：**领域模型（纯 POJO）↔ JPA DO（持久化）** 双层，与 `ResourceRoleModel ↔ ResourceRoleEntity` 同构。**不需要"POJO 化"**——领域模型本就是纯 POJO。

### 3.2 真正的差距

| # | 差距 | 影响 |
|---|---|---|
| 1 | **技术实现与领域核心同模块**：JPA DO/Repository/HTTP client 与 domain 在同一个 `gateway-provider` 等模块内，未拆独立绑定模块 | 跨模块依赖可穿透到 DO（audit 的 `UsageLogDo` 直接 import `ModelDo/ProviderDo/UserDo`） |
| 2 | **无 starter 装配**：业务域模块无 `AutoConfiguration.imports`，靠 `GatewayApplication` 全包扫描隐式装配 | 装配隐式、无法按需组装、模块边界不生效 |
| 3 | **boot 非纯组装**：仍含 43 个 Spring 类（application 门面 + adapter Controller + 全部基础设施配置） | 违反"boot 纯组装" |
| 4 | **协议插件不自包含**：`OpenAIProtocolValidator`/`*Tuner` 留在 boot，未随插件模块走 | 插件模块不完整 |
| 5 | **无运行时模块秩序**：无 `@JmixModule` 式声明、无模块注册表、无 `@Internal` | 依赖方向无运行期约束 |

### 3.3 关键结论

llm-gateway 的改造重心不是"写新逻辑"，而是**重划模块边界**：把各业务域的 `infrastructure/**`（技术实现）拆出为独立绑定模块，让核心模块只保留 domain（纯领域逻辑 + 端口）。

## 4. 目标架构（三态模块）

对 provider / iam / usage / security / audit / alert / resilience / proxy / stats 每个业务域，拆分为：

```
gateway-xxx/              # ① 核心模块：纯领域逻辑
│                         #    domain/xxx/*（纯 POJO 模型 + *Gateway 端口 + 领域服务）
│                         #    纯逻辑 impl（不碰 JPA/HTTP/Redis）
gateway-xxx-data/         # ② 绑定模块（JPA）：DO + Repository 实现 *Gateway 端口
│                         #    + model↔DO 转换器（转换随 data 走，核心不 import DO）
gateway-xxx-http/         # ② 绑定模块（HTTP，按需）：Upstream client
gateway-xxx-starter/      # ③ 装配：@AutoConfiguration + @Import + imports
```

### 4.1 provider 拆分映射（示例）

| 现状（gateway-provider） | 目标模块 |
|---|---|
| `domain/supply/*`（纯 POJO + 9 个端口 + 领域服务） | `gateway-provider`（核心） |
| `infrastructure/supply/gateway/database/*`（9 个 `@Entity` DO + Repository + model↔DO 转换） | `gateway-provider-data` |
| `infrastructure/supply/upstream/*`（HTTP client） | `gateway-provider-http` |
| `infrastructure/supply/catalog/*`、`repository/*` | 按依赖拆分到 `-data` 或核心 impl |
| （无） | `gateway-provider-starter` |

**核心效果**：跨模块依赖（proxy/audit/stats → provider）只能命中 provider 的根包 API（领域模型 + 端口接口），`ChannelDo` 物理不可见——"模块依赖 API"在物理层成立。

### 4.2 包名治理（Jmix 式：模块 = 根包，去 DDD 词汇）

拆分涉及全项目包名迁移，对齐 Jmix「根包 = 模块 id」哲学（`io.jmix.security` / `io.jmix.securitydata` / `io.jmix.autoconfigure.security`），确立规则：

**R1｜一个 Maven 模块 = 一组唯一包名前缀（禁止同包跨模块）**
每个模块贡献的包路径互不重叠，模块边界在包名层面可见、可判定。

**R2｜包名 = 模块名（根包），去除 domain/application/infrastructure 顶层 DDD 前缀**
`com.codingas.gateway.domain..` / `application..` / `infrastructure..` 三层前缀移除，以模块根包取代。

**R3｜绑定模块用拼接根包（不用子包）**
`provider.data` 是 `provider` 的子目录，会被核心模块 `@ComponentScan(basePackages="com.codingas.gateway.provider")` 误扫；`providerdata` 是平行兄弟目录，扫不到——这正是 Jmix 用 `securitydata` 而非 `security.data` 的原因。绑定模块用拼接根包。

**R4｜内部业务子包化（仿 Jmix 业务子包，去 entity/gateway 架构子包）**
核心模块内部按**业务概念**分组（仿 `io.jmix.security` 的 `model/`、`role/`、`user/`、`authentication/`、`constraint/`、`impl/`），不再用 `entity`/`gateway`/`enums` 等技术角色子包。原则：
- 根包直放核心 API（服务接口、跨概念端口）
- 业务概念子包放领域模型 + 所属端口 + 所属枚举/异常（如 `channel.Channel`、`channel.ChannelGateway`、`channel.ChannelState`）
- `service/` 放应用服务；`dto/` 放公开 DTO
- `impl/` 放根包 API 的非绑定实现
- 绑定模块独立根包（`providerdata`/`providerhttp`），内部按语义子包（`dataobject`/`gateway`/`upstream`）

**迁移规则**（两步）：
1. **顶层**：`com.codingas.gateway.<layer>.<域段>.<rest>` → `com.codingas.gateway.<模块根包>.<rest>`
2. **内部业务子包化**：按 R4 把 `entity`/`gateway`/`enums`/`exception`/`valueobject` 打散到业务概念子包 + `service/` + `impl/`

**包名映射表（9 域业务子包）**：

### provider（根包 `provider`，绑定 `providerdata`/`providerhttp`）

```
com.codingas.gateway.provider
├── channel/      Channel ChannelActions ChannelCredential ChannelEndpoint ChannelOperationLog
│                 ChannelGateway ChannelCredentialGateway ChannelEndpointGateway
│                 ChannelOperationLogGateway ChannelKeyProbe
│                 ChannelState ChannelHealthSource ChannelHealthStatus
│                 ChannelException ChannelNotFoundException
├── model/        Model ModelInstance ModelGateway ModelInstanceGateway BillingMode
├── vendor/       Provider ProviderGateway ProviderException
├── catalog/      PlanCatalog PlanModelCatalog PlanCatalogGateway PlanModelCatalogGateway CatalogException
├── upstream/     UpstreamClient UpstreamClientRegistry ConnectivityTester ResilientClientFactory
│                 ConnectivityTestResult KeyTestResult AuthStatus Protocol RoutingStrategy RoutingContext
├── service/      ChannelHealthService CredentialEncryptor
├── dto/          ChannelHealthResult KeyMatrixRow
└── impl/         BuiltinDataLoader StubChannelKeyProbe
```
`providerdata.*`：`ChannelDo` 等 JPA（`dataobject`/`repository`/`gateway` 子包）；`providerhttp.*`：`AnthropicUpstreamClient` 等（`upstream` 子包）。

### iam（根包 `iam`，绑定 `iamdata`）

```
com.codingas.gateway.iam
├── user/         User UserGateway UserState
├── apikey/       UserApiKey UserApiKeyGateway UserApiKeyGenerator DefaultUserApiKeyGenerator GeneratedApiKey
├── application/  Application ApplicationChannel ApplicationGateway ApplicationChannelGateway
├── auth/         AuthenticationDomainService AuthenticationFailedException AuthService AuthServiceImpl
├── encryption/   EncryptionService Aes256EncryptionService PasswordEncoder CredentialEncryptorAdapter
├── service/      UserService UserServiceImpl UserApiKeyService UserApiKeyServiceImpl
│                 ApplicationService ApplicationServiceImpl ApiKeyEncryptionDomainService
├── dto/
├── valueobject/  Identity
└── exception/    IamException ForbiddenException UnauthorizedException
```
`iamdata.*`：`UserDo`/`UserApiKeyDo`/`ApplicationDo`/`ApplicationChannelDo` + REPO + IMPL。

### usage（根包 `usage`，绑定 `usagedata`）

```
com.codingas.gateway.usage
├── tokenlimit/   TokenLimit TokenLimitGateway TokenLimitService TokenLimitServiceImpl
├── ratelimit/    RateLimitConfig
├── event/        TokenUsedEvent TokenUsageEventListener
├── enums/        ExceededAction PeriodType
└── dto/
```
`usagedata.*`：`TokenLimitDo`/`RateLimitConfigDo` + `TokenLimitRepository` + `TokenLimitGatewayImpl`。

### security（根包 `security`，绑定 `securitydata`）

```
com.codingas.gateway.security
├── dataprotection/  SensitiveDataRule SensitiveDataRuleGateway DataProtectionException
│                    SensitiveDataRuleInitializer
├── threat/          IpBlocklist IpBlockGateway IpBlocklistDomainService
│                    TokenBucketRateLimiter InMemoryTokenBucketRateLimiter TokenBucketStatus
│                    RateLimitDomainService RateLimitProperties
│                    IpBlockedException RateLimitExceededException ThreatException
└── impl/
```
`securitydata.*`：`SensitiveDataRuleDo`/`IpBlocklistDo` + REPO + GatewayImpl + Converter（`dataprotection`/`threat` 子包）。

### audit（根包 `audit`，绑定 `auditdata`）

```
com.codingas.gateway.audit
├── AuditLog AuditLogGateway AuditGateway
├── CallLog CallLogGateway
├── AuditContext
├── event/        AuditEventListener
└── impl/
```
`auditdata.*`：`AuditLogDo`/`CallLogDo`/`UsageLogDo` + REPO + GatewayImpl。

### alert（根包 `alert`，绑定 `alertdata`）

```
com.codingas.gateway.alert
├── AlertNotification
├── AlertRule
└── impl/
```
`alertdata.*`：`AlertNotificationDo`/`AlertRuleDo`。

### resilience（根包 `resilience`，绑定 `resiliencedata`）

```
com.codingas.gateway.resilience
├── circuitbreaker/  CircuitBreaker CircuitBreakerState ChannelEndpointCircuitBreakerManager
│                    CircuitOpenException
├── retry/           RetryExecutor RetryStrategy FastRetryStrategy ExponentialBackoffStrategy
│                    RateLimitRetryStrategy ServiceUnavailableStrategy RetryableException
│                    GatewayRetryProperties
├── metrics/         EndpointMetrics EndpointMetricsRegistry
├── upstream/        ResilientUpstreamClient ResilientClientFactoryImpl
├── failover/        FailoverEvent FailoverEventGateway ResilienceEventService
│                    ResilienceEventServiceImpl FailoverEventListener
└── dto/             FailoverEventResponse
```
`resiliencedata.*`：`FailoverEventDo` + `FailoverEventRepository` + `FailoverEventGatewayImpl`。

### proxy（根包 `proxy`，无绑定拆分）

```
com.codingas.gateway.proxy
├── chat/         ChatDispatchService ChatDispatchServiceImpl ErrorClassifier
├── routing/      Router RouterChain RoutingResolver RoutingRequest CredentialResolver
│                 EndpointResolver InstanceSelector LoadBalanceRouter ModelMatcher
│                 PermissionRouter PriorityRouter HealthRouter
├── invoker/      ChannelFailoverInvoker KeyFailoverInvoker
├── conversion/   ProtocolConversionFacade ProtocolStreamConverter OutboundTuner
└── dto/
```

### stats（根包 `stats`，无绑定拆分）

```
com.codingas.gateway.stats
├── StatsService
└── dto/          StatsResponse
```

### common / protocol（不变）

`com.codingas.gateway.common`、`com.codingas.gateway.protocol` 保持。`gateway-xxx-starter` → `com.codingas.gateway.autoconfigure.<域>`（P2）。

**ArchUnit 影响**：现有 `LayerDependencyTest` 基于 `com.codingas.gateway.domain..` 等顶层前缀，包名迁移后**重写**为模块级规则（核心不得依赖本域 data/http；跨模块只依赖对方根包；P1 过渡态豁免），见 P4 与 P1 计划。

**机制保障**：Maven 依赖 = 物理隔离（编译期强制）；ArchUnit 按模块根包判定违规；`@ComponentScan` 限定本模块唯一根包；本映射表随实施逐域对照执行。

### 4.3 非绑定技术实现归属

审计确认各域存在**非 JPA 非 HTTP 的技术实现**（依赖通用库而非绑定技术），这类实现**留在核心模块**（仿 Jmix 核心模块的纯逻辑 impl，可依赖通用库如 Micrometer/Sa-Token/JDK crypto），不拆绑定模块：

| 域 | 留在核心的技术实现 | 依赖技术 |
|---|---|---|
| iam | `PasswordEncoder`（SHA-256）、`Aes256EncryptionService`（AES-256-GCM）、`CredentialEncryptorAdapter`（桥接） | Sa-Token / JDK crypto / Spring |
| security | `InMemoryTokenBucketRateLimiter`（内存限流）、`SensitiveDataRuleInitializer`（启动装载） | JDK 并发 |
| resilience | 熔断/重试/指标/装饰器 18 个（`CircuitBreaker`、`RetryExecutor`、`ChannelEndpointCircuitBreakerManager` 等） | Micrometer |
| provider | `BuiltinDataLoader`（启动装载）、`StubChannelKeyProbe`（占位） | Spring |
| proxy | `ProtocolStreamConverter`（SSE 转换） | Jackson |

### 4.4 模块依赖规则

- 核心模块（`gateway-xxx`）：只依赖 `gateway-common`、`gateway-protocol` 等底层 API 模块 + 所需其他域的核心模块
- 绑定模块（`gateway-xxx-data`）：依赖本域核心模块 + spring-data-jpa + 所需绑定模块
- 绑定模块（`gateway-xxx-http`）：依赖本域核心模块 + okhttp/jackson + 所需绑定模块
- starter（`gateway-xxx-starter`）：只依赖 `spring-boot-autoconfigure` + 本域核心模块（+ 本域绑定模块，若需自动装配 JPA）
- `gateway-boot`：依赖各 starter + `gateway-web`，纯启动
- `gateway-web`：承载 Controller，依赖各域核心模块的根包 API（门面接口），禁止依赖 impl/DO
- **P1 过渡态**：已知跨模块 infrastructure 依赖（stats→provider/iam 的 Repository、audit/alert→provider/iam 的 DO、resilience→provider 的 upstream client、proxy/boot→resilience 的熔断管理器）在 P1 以「依赖对应 -data/-http 模块」保持编译，P4 解耦为端口调用或 ID 关联。

### 4.5 目录组织（功能域目录汇聚，仿 Jmix）

参照 Jmix 的**功能域目录**组织（`jmix-security/` 目录汇聚 `security/`、`security-data/`、`security-starter/` 等所有该域子模块），llm-gateway 每个业务域一个目录，汇聚该域全部子模块；子模块目录用**短名**（与 artifactId 不同，仿 Jmix `security/` vs `jmix-security`）。

**完整目录树**：

```
gateway/                              # 项目根（父 POM）
├── pom.xml
├── gateway-common/                   # 底座（保持根目录）
├── gateway-protocol/                 # 协议域目录（含域父 POM，聚合 protocol/protocol-openai/protocol-anthropic/protocol-gemini）
│   ├── protocol/                     # 协议 API 核心（artifactId gateway-protocol）
│   ├── protocol-openai/              # 插件（gateway-protocol-openai）
│   ├── protocol-anthropic/           # 插件
│   └── protocol-gemini/              # 插件
├── gateway-provider/                 # 供给域目录（含域父 POM，聚合 provider/provider-data/provider-starter）
│   ├── provider/                     # 核心（gateway-provider）
│   ├── provider-data/                # JPA 绑定（gateway-provider-data）
│   ├── provider-http/                # HTTP 绑定（gateway-provider-http）
│   └── provider-starter/             # 装配（P2）
├── gateway-iam/                      # 身份域目录（含域父 POM，聚合 iam/iam-data/iam-starter）
│   ├── iam/                          # 核心（gateway-iam）
│   ├── iam-data/                     # 绑定（gateway-iam-data）
│   └── iam-starter/                  # 装配（P2）
├── gateway-usage/                    # usage/usage-data/usage-starter（含域父 POM）
├── gateway-security/                 # security/security-data/security-starter（含域父 POM）
├── gateway-audit/                    # audit/audit-data/audit-starter（含域父 POM）
├── gateway-alert/                    # alert/alert-data/alert-starter（含域父 POM）
├── gateway-resilience/               # resilience/resilience-data/resilience-starter（含域父 POM）
├── gateway-proxy/                    # proxy/proxy-starter（含域父 POM）
├── gateway-stats/                    # stats/stats-starter（含域父 POM）
├── gateway-boot/                     # 应用（保持根目录）
├── gateway-web/                      # HTTP 承载层（Controller/Interceptor/Advice，已落地）
├── gateway-cli/  gateway-simulator/  # 保持根目录
└── gateway-console/                  # 前端（保持根目录）
```

**目录 → 模块映射规则**：
- 域目录名 = `gateway-<域>`（如 `gateway-provider`）
- 核心子模块目录 = `<域>` 短名（如 `provider`，artifactId `gateway-provider`）
- 绑定子模块目录 = `<域>-<绑定>`（如 `provider-data`，artifactId `gateway-provider-data`）
- 协议插件 = `gateway-protocol/<protocol-openai>` 等（汇聚到协议域目录）
- 底座/应用/工具模块（common/protocol 核心、boot/web/cli/simulator/console）保持根目录

**groupId 命名（按域划分，仿 Jmix `io.jmix.security`/`io.jmix.data`）**：

| 功能域 | groupId | 包含模块 |
|---|---|---|
| 底座 | `com.codingas.gateway.common` | `gateway-common` |
| 协议 | `com.codingas.gateway.protocol` | `gateway-protocol`、`gateway-protocol-openai/anthropic/gemini` |
| 供给 | `com.codingas.gateway.provider` | `gateway-provider`、`-data`、`-http`、`-starter` |
| 身份 | `com.codingas.gateway.iam` | `gateway-iam`、`-data`、`-starter` |
| 用量 | `com.codingas.gateway.usage` | `gateway-usage`、`-data`、`-starter` |
| 安全 | `com.codingas.gateway.security` | `gateway-security`、`-data`、`-starter` |
| 审计 | `com.codingas.gateway.audit` | `gateway-audit`、`-data`、`-starter` |
| 告警 | `com.codingas.gateway.alert` | `gateway-alert`、`-data`、`-starter` |
| 韧性 | `com.codingas.gateway.resilience` | `gateway-resilience`、`-data`、`-starter` |
| 派发 | `com.codingas.gateway.proxy` | `gateway-proxy`、`-starter` |
| 报表 | `com.codingas.gateway.stats` | `gateway-stats`、`-starter` |
| 应用/工具 | `com.codingas.gateway` | `gateway-boot`、`gateway-web`、`gateway-cli`、`gateway-simulator` |

**starter 对应（对齐 Jmix `security-starter` + `security-data-starter`）**：核心模块有 `gateway-<域>-starter`，每个绑定模块有独立 `gateway-<域>-<绑定>-starter`（如 `provider-starter` + `provider-data-starter` + `provider-http-starter`）。

**Maven 影响**：根 `pom.xml` 的 `<module>` 指向相对路径（如 `gateway-provider/provider`）；嵌套子模块 pom 的 parent `relativePath` 显式设为 `../../pom.xml`（根 POM）；所有模块 pom 的依赖声明 groupId 按上表使用对应域 groupId。依赖声明仍按 artifactId，不受目录影响。

> **2026-08-24 决策（偏离原方案）**：域目录增加中间父 POM（层级聚合）——每个域目录一个 `pom.xml`（`gateway-<域>-parent`，parent=根 pom，packaging=pom，聚合域内子模块），子模块 parent 指向域 pom（`relativePath=../pom.xml`），根 pom `<modules>` 只聚合 10 个域 pom + 4 个根目录模块（common/boot/cli/simulator）。收益：cd 进域目录可独立构建该域；域级模块组织语义化。成本：pom 层级 +1、14 个新 pom 文件。原「根 pom 直接聚合子模块、relativePath=../../pom.xml」方案废弃。

> **2026-08-24 决策（命名规范，待执行）**：Maven 坐标两级命名体系——`gateway-` 前缀标识「构建聚合/根工具模块」，域内业务模块用短名（核心子模块 artifactId 去前缀）：
>
> | 层 | groupId | artifactId | 目录 |
> |---|---|---|---|
> | 域父 POM（聚合/继承） | `com.codingas.gateway`（根） | `gateway-<域>`（无 parent 后缀） | `gateway-<域>/` |
> | 核心子模块 | `com.codingas.gateway.<域>` | `<域>`（如 `provider`） | `<域>/`（= artifactId） |
> | data/starter | `com.codingas.gateway.<域>` | `<域>-data` / `<域>-starter` | `<域>-data/` / `<域>-starter/` |
> | 根工具/应用 | `com.codingas.gateway` | `gateway-boot/common/cli/simulator/web`（带前缀，不变） | 根目录 |
>
> 协议域示例：父 `com.codingas.gateway:gateway-protocol`；核心 `com.codingas.gateway.protocol:protocol`；插件 `com.codingas.gateway.protocol:protocol-openai/anthropic/gemini`。
>
> **收益**：① 模块名（目录）与 artifactId 一致（目录本为短名，去前缀后天然一致）；② 域 groupId（`com.codingas.gateway.<域>`）保持干净、父 POM 不占域 groupId；③ 域父 POM 无 parent 后缀（父与核心 artifactId 不同，无需靠 groupId 区分）。
> **代价**：坐标连锁变更——30+ 依赖声明 artifactId 改名（`gateway-provider` → `provider` 等）、10 个域父 POM + 29 子模块 parent 引用调整、文档/记忆同步。**待执行**（P4 合并后独立分支）。
>
> 注：此决策与上方「域父 POM `gateway-<域>-parent`」决策有冲突（后者为过渡态），执行命名重构时以本决策为准。

## 5. 装配机制

### 5.1 starter 纯装配（仿 `security-starter`）

每个业务域新增 starter，结构如下：

```java
@AutoConfiguration
@Import(ProviderConfiguration.class)   // 核心 starter 只装配本域核心 Configuration
@ConditionalOnProperty(prefix = "gateway.provider", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderAutoConfiguration { ... }
```

- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册
- starter 只依赖 `spring-boot-autoconfigure`，零业务逻辑
- `@ConditionalOnMissingBean` 保证可覆盖

**两级 starter 模式（取舍）**：核心 starter 只 `@Import` 本域核心 Configuration（如 `ProviderConfiguration`），不直连 data 绑定模块——data 绑定装配由独立 `-data-starter` 导入（如未来拆分时，`ProviderDataConfiguration` 由 `provider-data-starter` 装配，对齐 Jmix `security-starter` + `security-data-starter`）。**当前阶段（2026-08-23 决策）**：data 绑定模块由核心 Configuration 的 `@ComponentScan` 兼扫（见 §5.2），不建 data-starter。故示例为 `@Import(ProviderConfiguration.class)` 而非 `@Import({ProviderConfiguration.class, ProviderDataConfiguration.class})`——后者与 ArchUnit 规则 `STARTER_ONLY_AUTOCONFIGURE`（starter 禁止依赖 *data 绑定根包）矛盾。

### 5.2 核心模块装配入口（仿 `SecurityConfiguration`，不含 `@JmixModule` 运行时机制）

```java
@Configuration
@ComponentScan(basePackages = {"com.codingas.gateway.provider", "com.codingas.gateway.providerdata"})   // 限定本域核心 + 绑定包（绑定模块由核心兼扫，2026-08-23 决策）
@ConfigurationPropertiesScan
public class ProviderConfiguration { ... }
```

> 注：Jmix 的 `SecurityConfiguration` 带 `@JmixModule` + `@PropertySource(module.properties)`，因运行时秩序机制不实现（见 §5.3），llm-gateway 的 `XxxConfiguration` 不引入这两者。

### 5.3 运行时模块秩序：不实现（YAGNI）

**决策**：`@GatewayModule`/`GatewayModulesProcessor`/`GatewayModules`/`GatewayModulesAwareBeanSelector`/`module.properties`/`@Internal` 等运行时模块秩序机制**不实现**（用户评估决策）。目标架构（三态模块）必须实现，但运行时秩序机制为框架化外壳，单体应用不采用。

**理由**：这些机制是 Jmix 作为**框架**为「下游应用扩展」设计的（运行时发现模块、应用覆盖框架默认、加法式配置）。llm-gateway 是**单体应用**，模块拓扑在 pom 编译期静态确定，无运行时发现需求。对应需求由更轻的 Spring 原生机制承担：

| 框架场景（Jmix 机制） | 单体替代（llm-gateway） |
|---|---|
| 运行时模块发现/排序（`JmixModulesProcessor`） | pom 编译期依赖 = 模块拓扑；Spring 按 bean 依赖装配 |
| 多 bean 模块覆盖（`JmixModulesAwareBeanSelector`） | `@ConditionalOnMissingBean` + `@Primary` |
| 加法式配置（`module.properties`） | `List<ProtocolAdapter>` 注入 + AutoConfiguration（协议插件已用） |
| 内部 API 标注（`@Internal`） | ArchUnit 按包路径判定 impl/DO 依赖（静态、可测试） |

### 5.4 装配显式化

- `GatewayApplication` 移入独立包（如 `com.codingas.gateway.boot`），`@SpringBootApplication` 扫描范围缩至 boot 自身
- 各域 bean 由各自 `XxxConfiguration` 的 `@ComponentScan` 显式扫描（限定本域包）
- boot 依赖各 starter，靠 `AutoConfiguration.imports` 自动装配

### 5.5 配置属性下沉

- `@ConfigurationProperties` 从 boot 的 `infrastructure/config` 下沉到各域核心/绑定模块
- 构造器绑定 + `@DefaultValue`（仿 `SecurityProperties`），`@ConfigurationPropertiesScan` 统一扫描
- boot 只保留全局 Web/安全配置

## 6. 迁移阶段

每阶段独立可验证、可提交，全量 `./mvnw clean install` 回归。

### P1 绑定模块拆分（工作量最大）

- 8 个业务域各拆 `-data`（JPA DO/Repository/转换器）+ 按需 `-http`（HTTP client）
- 核心模块只保留 domain（纯领域逻辑 + 端口）与纯逻辑 impl
- 转换器（model↔DO）随 data 模块走，核心不 import 任何 DO
- 验证：全量构建 + 全量测试绿；临时 ArchUnit 规则验证「核心 0 依赖 data」

### P2 starter 化

- 核心模块补 `@Configuration` 装配入口（`@ComponentScan` 限定本域包 + `@ConfigurationPropertiesScan`）
- 新增各 `-starter`（AutoConfiguration + imports，只依赖 `spring-boot-autoconfigure`）
- boot 依赖切换为各 starter；装配显式化（去全包扫描）
- 验证：构建绿；装配显式化测试

### P2 架构优化项（P1 final review 承接 + 架构演进）

> **2026-08-23 评估补强**：本组条目经代码勘察复核后修订——修正 `ProviderErrorType` 上浮描述（保持 common 不上浮）、补充 `ResilientClientFactoryImpl` instanceof 依赖处理、澄清 `UpstreamClientRegistry` 工厂注册语义、新增 `ResilientClientFactory` 接口归属/`ConnectivityTestResult` 双版本/协议域包名 Jmix 化/P2 范围拆分四项。标注「修订」处为本次评估变更，未标注处为原已批准表述。

- **协议传输归协议域 + 插件自包含**（架构演进）：`UpstreamClient` SPI + `ConnectivityTestResult`（upstream 版）上浮 `gateway-protocol`（协议传输端口）；`OpenAIUpstreamClient`/`AnthropicUpstreamClient` + `ErrorClassificationStrategy`/`AnthropicErrorClassifier`/`OpenAIErrorClassifier`/`SseErrorFormatter` 分别并入 `gateway-protocol/protocol-openai`/`protocol-anthropic`（协议插件自包含：格式转换 + 传输调用）；`gateway-provider-http` 模块解散；`UpstreamClientRegistry` 改为协议域注册表；`ConnectivityTesterImpl` 归 provider 核心（用 SPI 做连通性测试，现实现仅依赖 SPI、无 HTTP 技术，归核心可行）；`ResilientClientFactory` 保持 resilience（包装 SPI）
  - **修订｜`ResilientClientFactoryImpl` instanceof 依赖（必改）**：当前 `resolveProviderCode` 用 `instanceof OpenAIUpstreamClient/AnthropicUpstreamClient` 判断协议（`resilience/.../upstream/ResilientClientFactoryImpl.java`），构成 resilience → provider-http 直接依赖；provider-http 解散后必须消除，否则 resilience 反向依赖协议插件。方案二选一：① `UpstreamClient` SPI 增加 `supportedProvider()`（推荐，去 instanceof）；② `ResilientClientFactory.wrap()` 增加 `providerCode` 参数（`KeyFailoverInvoker.buildClient` 已知 `ctx.upstreamProtocol()`）
  - **修订｜`UpstreamClientRegistry` 为「工厂注册」语义**：`getClient(protocol, endpointUrl, apiKey, timeout)` 现为每请求创建绑定配置实例的工厂语义（client 非单例），`List` 注入收集的应是「client 工厂」（协议插件注册 `ProtocolUpstreamClientFactory`）而非 client 实例；注册表按协议选工厂再创建，对外签名不变，proxy/boot 现有 6 处调用零改动
  - **修订｜boot 4 个使用者依赖调整**：`ProtocolController`/`ModelExperienceService`/`ProviderHealthProbe`/`ProviderHealthTracker` 均使用 `UpstreamClientRegistry`，provider-http 解散后 boot pom 依赖改为协议插件模块
  - **修订｜测试随迁**：provider-http 的 UpstreamClient/ErrorClassifier 测试现位于 boot/src/test 下（12+），须随实现并入协议插件模块（TDD 随迁）
  - **明确不做**：Gemini 插件 P2 仍仅转换（无传输 client），`getSupportedProtocols` 行为与现状一致，不阻塞；Gemini 传输为未来项
- **`ProviderException` 上浮**（最大连锁）：`ProviderException` 当前在 `provider.vendor`，被 provider/proxy/resilience 广泛使用；上浮到 `gateway-protocol`（协议/传输异常归协议域）后协议插件才不反向依赖 provider——跨域重构。**修订｜`ProviderErrorType` 保持 common、不上浮**：它已在 `gateway-common/common/enums/`，且被 common 内部类（`FailoverDecision`/`FailoverOccurredEvent`）使用，上浮会使 common → protocol 反向依赖；只需上浮 `ProviderException`（其依赖的 `GatewayException`/`ProviderErrorType` 均在 common，上浮后无新增依赖缺口）
- **UpstreamClient 泛型化**（随搬迁顺带完成）：`UpstreamClient<T extends ProtocolRequest>` + `chat(T request)`，把多态参数的运行时约定（`ctx.upstreamProtocol` 保证类型匹配）升级为编译期类型约束。**修订｜收益限定**：类型约束落在协议插件内部与测试（如 `OpenAIUpstreamClient.chat(OpenAIChatRequest)`、消除 `setStream(true)` 基类可变副作用）；proxy 主链路（`KeyFailoverInvoker`）拿到的是 `UpstreamClient<? extends ProtocolRequest>`，运行时 `ctx.upstreamProtocol()` 分支依然存在——泛型化不消除 proxy 运行时分支，不承诺「编译期消除运行时约定」
- **`ResilientClientFactory` 接口归属**（修订新增）：接口当前在 `provider.upstream`；UpstreamClient/Registry 上浮后建议一并上浮协议域（协议域聚合「传输端口 + 韧性端口」，proxy 无需为它依赖 provider 核心）；或明确留在 provider 核心（proxy 本就因 `RoutingContext` 等依赖 provider 核心）。实施时二选一并记录决策
- **`ConnectivityTestResult` 双版本区分**（修订新增）：`provider.upstream.ConnectivityTestResult`（`UpstreamClient.testConnectivity` 返回，上浮协议域）与 boot `application/provider/dto.ConnectivityTestResult`（ChannelHealth 分层 DTO，字段完全不同：success/message/models/level1/level2/totalLatencyMs）——只上浮 upstream 版，应用层 DTO 不动，勿合并
- **协议域包名 Jmix 化**（修订新增）：gateway-protocol 核心现仍为 `domain.protocol.contract.*` + `api.capability.protocol.*` 两套旧包名（与 §4.2「protocol 包名不变」表述有偏差）；P4 模块级 ArchUnit 按模块根包判定将无法统一命中协议模块——建议 P2 上浮 UpstreamClient 时顺手把协议域根包迁移为 `com.codingas.gateway.protocol.*`（连锁 import 协议契约的 provider/proxy/resilience/boot），或明确推迟到 P3，不得拖到 P4
- **jacoco 全模块 + report-aggregate**：P1 拆分后核心域覆盖率不可测（jacoco 仅配置 gateway-boot），扩展全模块并加聚合报告（放根 POM 或独立模块），`check` 门槛按聚合结果 + 合理 excludes（DO/适配器），恢复覆盖率 DoD（核心 ≥90% / 规则引擎 ≥85% / 适配器 ≥80%）可验证
- **freeze 基线上库**：ArchUnit 基线数据（`freeze.store.default.path=target/archunit`，gitignored）使全新检出/CI 首跑静默重建基线。**修订｜描述修正**：`archunit.properties` 本身已入库（`gateway-boot/src/test/resources/archunit.properties`），需入库的是 freeze 基线数据目录（改为 `src/test/resources/archunit/`）；并在 CI 禁止 `allowStoreCreation=true`（双保险），保证模块依赖铁律可复现守护
- **provider-data 补真测试**：P1 拆分后 `gateway-provider-data` 无任何测试（12 个 JPA GatewayImpl 覆盖为 0），随 jacoco 扩展补核心覆盖
- **proxy 依赖调整**（随协议传输归域）：proxy 改用 `UpstreamClient` SPI（protocol 域），不再依赖 provider-http 传递依赖（原「显式声明 provider-http」项被本项取代）
- **P2 范围拆分建议**（修订新增）：P2 当前 = starter 化（本节主线）+ 架构演进（本组）+ 质量基建（jacoco/freeze/补测试）三大块；建议「质量基建」独立并行（与架构演进无强依赖，仅 provider-data 补测试链式依赖 jacoco 全模块），避免架构演进（有回归风险）与纯增量混在同一变更、无法独立回滚

### P3 boot 瘦身 + web 独立 + 协议插件自包含

- boot 的 9 个 application 门面服务下沉到对应域模块
- 拆出 `gateway-web` 承载全部 Controller/Interceptor/Advice
- `OpenAIProtocolValidator`/`AnthropicProtocolValidator`/`*Tuner` 从 boot 迁回协议插件模块
- 验证：boot 纯启动；web 独立构建；协议插件自包含集成测试

> **2026-08-24 完成（P3 全部落地）**：
> - **boot 瘦身达成**：boot 主源码仅剩 `boot`（启动类）+ `infrastructure`（装配/全局配置）+ `application/init`（9 类初始化种子，CommandLineRunner）三个包；9 个 application 门面服务全部下沉对应域（provider 六组 + experience→proxy），boot main 零门面残留；`scanBasePackages` 最终化为 `boot/infrastructure/common/protocol/application`（application 仅命中 init 种子）。
> - **gateway-web 落地**：34 个 Controller/Interceptor/Advice 文件全部搬迁（包名 `com.codingas.gateway.adapter.*` 不变），`WebAutoConfiguration`（@AutoConfiguration + imports）装配，boot 依赖 gateway-web 即生效；web 独立构建。
> - **协议插件自包含完成**：`OpenAIProtocolValidator`/`AnthropicProtocolValidator`/`*Tuner` 迁回 `protocol-openai`/`protocol-anthropic` 插件（改 @Bean 注册，boot 不再扫插件类），插件自包含集成测试通过。
> - **验证**：全量 `./mvnw clean install` 绿（boot 纯启动 + web 独立构建 + 协议插件自包含集成测试）。
> - **已知例外（boot 保留全局 Web 配置）**：boot `infrastructure/config/WebConfig`（WebMvcConfigurer：拦截器注册 + SPA 路由 + ActuatorHealthProperties）保留 boot，注入 `adapter.interceptor.SecurityInterceptorChain`（gateway-web 模块）——符合 §5.5「boot 只保留全局 Web/安全配置」，为 Task 4 明确文档化决策（boot 依赖 web，classpath 可达）；boot main 唯一 adapter 引用即此（专项 grep 验证）。

### P4 ArchUnit 模块级铁律 + DO 依赖清零

- 新增模块级 ArchUnit 铁律（解除 freeze）：禁止依赖 impl/DO；禁止反向依赖 boot/web；协议插件只依赖 SPI；starter 只依赖 autoconfigure + 本域模块
- 整改 audit/alert 穿透 DO 的耦合（`UsageLogDo` → 改用端口接口或只存 ID）
- 验证：ArchUnit 全绿；全量回归；覆盖率 ≥90/85/80%

> **2026-08-24 完成（P4 全部落地）**：
> - **ArchUnit 模块级铁律硬化**：`LayerDependencyTest` 升级为 7 条硬规则——3 条原 freeze 规则（`NO_CORE_DEPENDS_BINDING_MODULES`/`NO_BINDING_CROSS_DOMAIN_DEPENDS`/`COMMON_NOT_DEPEND_ON_BUSINESS`）解除 `freeze()` 转为硬规则；新增 4 条：`NO_DEPENDS_ON_BOOT_OR_WEB`（业务域/绑定模块禁止反向依赖 boot/web 承载层）、`PROTOCOL_PLUGIN_ONLY_SPI`（协议插件只依赖协议核心+底座）、`PROTOCOL_PLUGIN_NO_COMPONENT`（协议插件包禁止 `@Component/@Service/@Repository`，实测 `@AutoConfiguration` 类未被误伤）、`STARTER_ONLY_AUTOCONFIGURE`（starter 装配类只依赖本域模块，按「源类域 == 目标类域」自定义条件实现，放行各 starter 对本域 Configuration 的自引用，禁止跨域）。分析范围限定主源码（`ImportOption.DoNotIncludeTests`），集成测试合法依赖承载层。
> - **DO 依赖清零**：`UsageLogDo` 改存 ID（`user_id/provider_id/model_id`，表结构不变）、`AlertNotificationDo` 改存 `target_user_id`，audit/alert 域不再穿透跨域 DO；`StatsService` 改注入 4 个 Gateway count 端口（`Provider/Model/Channel/UserGateway.count()`），stats→providerdata/iamdata 的 Repository 依赖清零。
> - **web 测试归位（P3 承接）**：boot 的 `adapter/**` 下 16 个测试迁入 `gateway-web/src/test`（物理迁移、包名不变），gateway-web pom 补测试依赖（junit-jupiter/mockito-junit-jupiter/assertj-core/spring-test/hamcrest）；`ChannelHealthControllerIT` 依赖 boot `GatewayApplication` 全量起上下文，留 boot（集成测试）。
> - **验证**：全量 `./mvnw clean install` BUILD SUCCESS，ArchUnit 7 条铁律全绿 + web 58 项测试在新模块运行。freeze 存储（target/archunit）不再需要——规则已硬化。

## 7. 测试与风险

**测试策略**：全程 TDD/回归——每阶段先跑现有全量测试确认不回归，再增量；Controller/门面迁移用逐项验证；P1 拆包用 IDE 批量重构 + ArchUnit 守护。

| 风险 | 缓解 |
|---|---|
| P1 拆分回归风险最高（大批类搬移） | 纯搬移为主，领域 POJO 与 DO 转换逻辑已存在；ArchUnit 守护 + 全量测试双保险 |
| 跨模块 DO 依赖整改（audit/alert） | P4 专项：审计字段改存 ID 或走端口接口 |
| 装配顺序/重复 bean | `@ComponentScan` 限定本域包 + `@ConditionalOnMissingBean` 兜底 |
| 覆盖率门槛跌破 | 每阶段 jacoco 校验 ≥90/85/80% |
| 改造范围过大 | 四阶段独立可回滚；每阶段独立提交 |

## 8. 明确不做（YAGNI）

- **不建自有 BOM**：llm-gateway 是单体应用（非框架），`${revision}` + 根 POM 管理已够
- **不实现运行时模块秩序**（`@GatewayModule`/`GatewayModulesProcessor`/`GatewayModules`/`GatewayModulesAwareBeanSelector`/`module.properties`/`@Internal`）：框架为下游扩展设计，单体用 Spring 原生机制（`@ConditionalOnMissingBean`/`@Primary`/`List<X>` 注入）+ ArchUnit 替代，见 §5.3
- **不引入 `StoreAwareLocator`**：单数据源，无多 store 需求
- **不改前端/CLI/模拟器**：`gateway-console`/`gateway-cli`/`gateway-simulator` 不受影响
