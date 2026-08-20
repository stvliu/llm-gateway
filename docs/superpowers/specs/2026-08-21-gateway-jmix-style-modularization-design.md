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
`com.codingas.gateway.domain..` / `application..` / `infrastructure..` 三层前缀移除，以模块根包取代。业务语义子包（`entity`/`gateway`/`enums`/`exception`/`valueobject`/`catalog`/`upstream`/`dto` 等）保留。

**R3｜绑定模块用拼接根包（不用子包）**
`provider.data` 是 `provider` 的子目录，会被核心模块 `@ComponentScan(basePackages="com.codingas.gateway.provider")` 误扫；`providerdata` 是平行兄弟目录，扫不到——这正是 Jmix 用 `securitydata` 而非 `security.data` 的原因。绑定模块用拼接根包。

**迁移规则**：`com.codingas.gateway.<layer>.<域段>.<rest>` → `com.codingas.gateway.<模块根包>.<rest>`；绑定模块 → `<模块根包><binding>.<rest>`。域段与模块根包映射见下表。

**包名映射表**：

| 模块 | 根包 | 域段映射 | 说明 |
|---|---|---|---|
| `gateway-common` | `com.codingas.gateway.common` | — | 不变 |
| `gateway-protocol` | `com.codingas.gateway.protocol` | — | 不变 |
| `gateway-provider`（核心） | `com.codingas.gateway.provider` | `supply` → `provider` | 模型/端口/服务/非绑定实现 |
| `gateway-provider-data` | `com.codingas.gateway.providerdata` | 同上 | JPA 绑定，拼接根包 |
| `gateway-provider-http` | `com.codingas.gateway.providerhttp` | 同上 | HTTP 绑定，拼接根包 |
| `gateway-iam` | `com.codingas.gateway.iam` | `iam` → `iam`；`application` → `iam.application` | 核心 |
| `gateway-iam-data` | `com.codingas.gateway.iamdata` | 同上 | JPA 绑定 |
| `gateway-usage` / `-data` | `com.codingas.gateway.usage` / `.usagedata` | `usage` → `usage` | — |
| `gateway-security` / `-data` | `com.codingas.gateway.security` / `.securitydata` | `dataprotection` → `security.dataprotection`；`threat` → `security.threat` | — |
| `gateway-audit` / `-data` | `com.codingas.gateway.audit` / `.auditdata` | `audit` → `audit` | — |
| `gateway-alert` / `-data` | `com.codingas.gateway.alert` / `.alertdata` | `alert` → `alert` | — |
| `gateway-resilience` / `-data` | `com.codingas.gateway.resilience` / `.resiliencedata` | `resilience` → `resilience` | — |
| `gateway-proxy` | `com.codingas.gateway.proxy` | `proxy` → `proxy` | 无绑定拆分 |
| `gateway-stats` | `com.codingas.gateway.stats` | `stats` → `stats` | 无绑定拆分 |
| `gateway-xxx-starter` | `com.codingas.gateway.autoconfigure.<域>` | — | 装配（P2） |

**示例**（provider 域）：
- 核心：`domain.supply.entity.Channel` → `provider.entity.Channel`；`domain.supply.gateway.ChannelGateway` → `provider.gateway.ChannelGateway`；`infrastructure.supply.catalog.loader.BuiltinDataLoader` → `provider.catalog.loader.BuiltinDataLoader`（留核心）
- JPA：`infrastructure.supply.gateway.database.dataobject.ChannelDo` → `providerdata.dataobject.ChannelDo`（绑定模块）
- HTTP：`infrastructure.supply.upstream.AnthropicUpstreamClient` → `providerhttp.upstream.AnthropicUpstreamClient`（绑定模块）

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

## 5. 装配机制

### 5.1 starter 纯装配（仿 `security-starter`）

每个业务域新增 starter，结构如下：

```java
@AutoConfiguration
@Import({ProviderConfiguration.class, ProviderDataConfiguration.class})
@ConditionalOnProperty(prefix = "gateway.provider", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderAutoConfiguration { ... }
```

- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册
- starter 只依赖 `spring-boot-autoconfigure`，零业务逻辑
- `@ConditionalOnMissingBean` 保证可覆盖

### 5.2 核心模块装配入口（仿 `SecurityConfiguration`，不含 `@JmixModule` 运行时机制）

```java
@Configuration
@ComponentScan(basePackages = "com.codingas.gateway.provider")   // 限定本域包
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

### P3 boot 瘦身 + web 独立 + 协议插件自包含

- boot 的 9 个 application 门面服务下沉到对应域模块
- 拆出 `gateway-web` 承载全部 Controller/Interceptor/Advice
- `OpenAIProtocolValidator`/`AnthropicProtocolValidator`/`*Tuner` 从 boot 迁回协议插件模块
- 验证：boot 纯启动；web 独立构建；协议插件自包含集成测试

### P4 ArchUnit 模块级铁律 + DO 依赖清零

- 新增模块级 ArchUnit 铁律（解除 freeze）：禁止依赖 impl/DO；禁止反向依赖 boot/web；协议插件只依赖 SPI；starter 只依赖 autoconfigure + 本域模块
- 整改 audit/alert 穿透 DO 的耦合（`UsageLogDo` → 改用端口接口或只存 ID）
- 验证：ArchUnit 全绿；全量回归；覆盖率 ≥90/85/80%

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
