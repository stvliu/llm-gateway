# llm-gateway 目标模块化结构：依赖矩阵 / 环检测 / 关键域边界设计

> 配套文档：`docs/jvm-modularity-philosophy-and-llm-gateway-feasibility.md`（总评估）
> 本文给出可直接落地的 **完整依赖矩阵、ArchUnit 环检测方案、以及 provider 中枢 / 能力插件 / security-iam-usage 三个关键域的边界细化设计**。

---

## 一、完整依赖矩阵

### 1.1 模块与分层归属

| 层 | 模块 | 内部分层 |
|----|------|---------|
| **底座** | `gateway-common` | 无（纯横切） |
| **底座** | `gateway-protocol` | SPI + Canonical IR + 契约 DTO（不依赖具体能力） |
| **底座** | `gateway-provider`（原 supply） | application/domain/infrastructure |
| **底座** | `gateway-iam` | application/domain/infrastructure |
| **底座** | `gateway-security` | application/domain/infrastructure |
| **中基** | `gateway-usage` | application/domain/infrastructure |
| **中基** | `gateway-stats` | application/domain/infrastructure |
| **中基** | `gateway-resilience` | application/domain/infrastructure |
| **中基** | `gateway-audit` | application/domain/infrastructure |
| **中基** | `gateway-alert` | application/domain/infrastructure |
| **中基** | `gateway-experience` | application/domain/infrastructure |
| **上层** | `gateway-proxy` | application/domain/infrastructure |
| **能力插件** | `gateway-protocol-openai` / `anthropic` | Adapter + AutoConfiguration（依赖 protocol SPI） |
| **组装** | `gateway-boot` | 纯组装/启动 |

### 1.2 依赖矩阵（行→列 = 行模块依赖列模块）

| 模块 ↓ 依赖 → | common | protocol | provider | iam | security | usage | stats | resilience | audit | alert | experience | protocol-* |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **gateway-common** | — | | | | | | | | | | | |
| **gateway-protocol** | ✅ | — | | | | | | | | | | |
| **gateway-provider** | ✅ | ✅(协议枚举/契约) | — | | | | | | | | | |
| **gateway-iam** | ✅ | | | — | | | | | | | | |
| **gateway-security** | ✅ | | ✅(被拦截对象) | ✅(认证) | — | | | | | | | |
| **gateway-usage** | ✅ | ✅(用量事件) | ✅(模型/通道) | ✅(APIKey) | ✅(限流接口) | — | | | | | | |
| **gateway-stats** | ✅ | | ✅ | ✅ | | ✅(明细读) | — | | | | | |
| **gateway-resilience** | ✅ | ✅(错误类型) | ✅(通道/端点) | | | | | — | | | | |
| **gateway-audit** | ✅ | ✅(请求/响应) | ✅ | ✅(用户) | | ✅(用量) | | | — | | | |
| **gateway-alert** | ✅ | | ✅ | ✅ | | ✅ | ✅ | | ✅ | — | | |
| **gateway-experience** | ✅ | ✅ | ✅ | ✅ | ✅ | | | | ✅ | | — | |
| **gateway-proxy** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | | ✅ | ✅(经 Facade→protocol 编排) |
| **gateway-protocol-openai/anthropic** | | ✅(SPI) | | | | | | | | | | — |
| **gateway-boot** | 组装全部 | | | | | | | | | | | ✅ |

> 说明：
> - 能力插件**只依赖** `gateway-protocol` 的 SPI，这是"新增协议不改核心"的关键约束。
> - `gateway-proxy` 是唯一同时触碰 usage/stats/resilience/audit 的上层编排域，负责组装，**不允许**这些上层域反向依赖 proxy。
> - `gateway-security` 依赖 `iam`（认证）、`provider`（被拦截对象），因此 security 不是"纯底座"——它依赖 iam/provider，仍在底座层内（不反依赖上层）。

### 1.3 依赖方向规则（架构铁律）

```
gateway-common                        ← 所有人依赖，不依赖任何人
   ▲
gateway-protocol / provider / iam / security      ← 底座，只依赖 common(+provider内部互依)
   ▲
usage / stats / resilience / audit / alert / experience   ← 中基，只依赖底座+common
   ▲
gateway-proxy                         ← 上层编排，依赖所有中基+底座
   ▲
gateway-boot（组装）
能力插件 protocol-*  →  只依赖 gateway-protocol SPI
```

**禁止规则**：
1. 任何模块不得反向依赖 `gateway-boot`（组装层无业务）。
2. 能力插件不得依赖任何"应用/领域实现"，只能依赖 `gateway-protocol` 的 SPI。
3. `gateway-protocol` 不得依赖任何 `protocol-*`（SPI 不被具体实现污染）。
4. 中基/上层不得被底座反向依赖。
5. `gateway-common` 不允许引入业务依赖（保持纯横切）。

---

## 二、ArchUnit 环检测方案

把上述依赖方向固化为架构测试，防止回归。在 `gateway-boot`（或新增 `gateway-arch-test` 模块）的测试源码目录建：

```java
// gateway-arch-test/src/test/java/com/codingas/gateway/arch/DependencyRuleTest.java
package com.codingas.gateway.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.codingas.gateway")
public class DependencyRuleTest {

    /** 依赖方向：上层 → 中基 → 底座 → common，禁止反向 */
    @ArchTest
    static final ArchRule NO_BASE_DEPEND_ON_UPPER =
        noClasses().that().resideInAPackage("..gateway..provider..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..gateway..proxy..", "..gateway..usage..", "..gateway..stats..",
                "..gateway..audit..", "..gateway..alert..", "..gateway..experience..");

    /** 能力插件只允许依赖 protocol SPI，禁止触碰任何业务实现 */
    @ArchTest
    static final ArchRule CAPABILITY_ONLY_DEPENDS_ON_PROTOCOL_SPI =
        noClasses().that().resideInAPackage("..capability.openai..")
            .or().resideInAPackage("..capability.anthropic..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..gateway..proxy..", "..gateway..provider..", "..gateway..iam..",
                "..gateway..security..", "..gateway..usage..");

    /** protocol SPI 不被具体能力污染：protocol 不得依赖 protocol-* */
    @ArchTest
    static final ArchRule PROTOCOL_NOT_DEPEND_ON_CAPABILITY =
        noClasses().that().resideInAPackage("..protocol..")
            .should().dependOnClassesThat().resideInAPackage("..capability..");

    /** 组装层无业务：boot 之外的模块不得反向依赖 boot */
    @ArchTest
    static final ArchRule NOBODY_DEPENDS_ON_BOOT =
        noClasses().that().resideOutsideOfPackage("..gatewayboot..")
            .should().dependOnClassesThat().resideInAPackage("..gatewayboot..");
}
```

**完整环检测**（含依赖矩阵约束的包级验证）可用 ArchUnit 的 `Architectures.layeredArchitecture()`：

```java
@ArchTest
static final ArchRule LAYERED =
    Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .layer("common").definedBy("..common..")
        .layer("base").definedBy("..provider..", "..protocol..", "..iam..", "..security..")
        .layer("mid").definedBy("..usage..", "..stats..", "..resilience..", "..audit..", "..alert..", "..experience..")
        .layer("upper").definedBy("..proxy..")
        .layer("capability").definedBy("..capability..")
        .whereLayer("common").mayNotBeAccessedByAnyLayer()
        .whereLayer("base").mayOnlyBeAccessedByLayers("mid", "upper", "capability", "base")
        .whereLayer("mid").mayOnlyBeAccessedByLayers("upper")
        .whereLayer("upper").mayOnlyBeAccessedByLayers("boot")
        .whereLayer("capability").mayOnlyAccessLayers("protocol");
```

> ArchUnit 依赖：`com.tngtech.archunit:archunit-junit5`（测试 scope）。

---

## 三、provider 中枢边界细化设计

### 3.1 定位修正

核实结论：`RoutingContext` 位于 `domain/supply/valueobject/`，是**供给域产出的值对象**，被 proxy 层消费——**放 provider 域合理**，不需要下沉 common。风险不在位置，而在"公开面过大"。

### 3.2 收敛策略（把"全局中枢"收敛为"稳定只读契约"）

| 手段 | 具体做法 |
|------|---------|
| **公开面 = 实体 + Gateway 接口** | 对外只暴露：`Provider/Channel/Model/ModelInstance/ChannelCredential/ChannelEndpoint` 实体 + `ProviderGateway/ChannelGateway/ModelGateway/...` 接口 |
| **RoutingContext 收敛为不可变值对象** | 只在 provider 内部构建，外部（proxy）**只读**；字段 final，无 setter，构造走 Builder；避免任意模块往里面塞字段 |
| **内部结构不对外泄露** | catalog（PlanCatalog/PlanModelCatalog）、enums、exception 作为 provider 内部实现，**不放入公开 API**；外部只通过 Gateway 接口拿稳定数据 |
| **对外依赖只进不出** | 其他模块只能"读"provider 的数据（通过 Gateway 接口），不允许 provider 被改造为承载跨域编排逻辑 |

### 3.3 provider 内部 package 布局（边界从"包"到"模块"）

```
gateway-provider/
├── gateway-provider-api/            # 稳定公开契约（可选：若需独立给外部依赖编译期隔离）
│     └── com.codingas.gateway.provider.api   # 实体 + Gateway 接口 + RoutingContext
└── gateway-provider/                # 实现
      ├── domain/  (entity/gateway/enums/valueobject/exception)
      ├── application/  (ProviderService/ChannelService/... )
      └── infrastructure/  (JPA/Repository/UpstreamClient 实现)
```

> 若 gateway-provider 被依赖方过多（几乎所有上层），建议拆 `-api` 子模块（借鉴 Jmix 的"接口与实现分离"），上层只依赖 api，编译期隔离实现，进一步压缩耦合面。

---

## 四、能力插件 + Facade 边界细化设计（解决跨层依赖）

### 4.1 现状问题（已核实）

`ProtocolConversionFacade` 构造注入 `infrastructure.protocol.OpenAIProtocolAdapter`/`AnthropicProtocolAdapter` **具体类**。若把 Facade 放 `gateway-protocol` 而 Adapter 放 `protocol-*`，会导致 **gateway-protocol → protocol-* 反向依赖**，违反依赖方向。

### 4.2 解决方案：Facade 改为"按 SPI 装配"，protocol 不依赖具体能力

借鉴 Jmix 的 `DataStoreFactory`（core 定义接口，实现以原型 Bean 注册、按名装配）：

```java
// gateway-protocol 内：仅依赖 SPI，通过 Spring 收集所有 ProtocolAdapter Bean 编排
@Component
public class ProtocolConversionFacade {
    private final Map<String, ProtocolAdapter<?>> adapters;

    public ProtocolConversionFacade(List<ProtocolAdapter<?>> adapterList) {
        // 收集所有已注册的 Adapter Bean（来自各 protocol-* 模块的 AutoConfiguration）
        this.adapters = adapterList.stream()
                .collect(toMap(ProtocolAdapter::protocol, a -> a));
    }

    public CanonicalChatRequest normalize(String protocol, Object nativeRequest) {
        return adapters.get(protocol).normalizeRequest(nativeRequest);
    }
    // ... denormalize 等
}
```

关键点：
- `gateway-protocol` **只依赖 SPI（`ProtocolAdapter` 接口 + Canonical IR）**，通过 Spring 注入 `List<ProtocolAdapter>` 收集所有实现——**实现来自各能力插件，但 Facade 不 import 任何具体类**。
- `gateway-protocol-openai/anthropic` 各自提供 `AutoConfiguration` 注册自己的 Adapter Bean（`@Bean OpenAIProtocolAdapter`），gateway-boot 按依赖+配置启用。

### 4.3 能力插件模块骨架

```
gateway-protocol-openai/
├── pom.xml                          # 依赖 gateway-protocol(SPI) + spring-boot-autoconfigure
├── src/main/resources/META-INF/spring/
│     └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│           → com.codingas.gateway.capability.openai.OpenAICapabilityAutoConfiguration
└── src/main/java/com/codingas/gateway/capability/openai/
      ├── OpenAIProtocolAdapter.java          # 实现 gateway-protocol 的 ProtocolAdapter
      └── OpenAICapabilityAutoConfiguration.java   # @Bean 注册 Adapter + @ConditionalOnXxx 控制启用
```

```java
// OpenAICapabilityAutoConfiguration.java
@AutoConfiguration
@ConditionalOnProperty(prefix = "gateway.capability.openai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenAICapabilityAutoConfiguration {
    @Bean
    public ProtocolAdapter<?> openaiProtocolAdapter() {
        return new OpenAIProtocolAdapter();
    }
}
```

### 4.4 依赖方向闭环（最终）

```
gateway-protocol(SPI)  ←──  gateway-protocol-openai
       ▲                        (实现 SPI，提供 Adapter Bean)
       └── ProtocolConversionFacade 通过 List<ProtocolAdapter> 收集
gateway-proxy ──调用──> Facade（经 protocol 编排，不依赖具体能力）
gateway-boot（组装）依赖 protocol-openai/anthropic + protocol + proxy
```

---

## 五、security / iam / usage 边界澄清

| 关注点 | 归属 | 依据 |
|--------|------|------|
| 认证/身份（User/Application/APIKey/Auth/加密） | `gateway-iam` | 身份域，被 security 依赖 |
| 威胁检测（IP 黑名单、恶意请求） | `gateway-security` | 安全域 |
| **脱敏（Dataprotection）** | `gateway-security` | 数据保护属安全域 |
| **限流（IP 级/请求级）** | `gateway-security` | 威胁防护的入口拦截 |
| **配额/预算（TokenLimit，Key 级）** | `gateway-usage` | 用量管控，涉及写入与扣减 |
| **限流执行（配额扣减/管控）** | `gateway-usage` | 关键路径写入 |
| **拦截链编排** | **应用层（不塞进 security）** | 拦截链跨 auth+threat+quota 多域，作为 application 编排服务或由 gateway-boot 组装 |

> 关键分界：**security 做"入口防护/拦截"，usage 做"资源管控/配额"**。IP 级限流在 security，Key 级配额在 usage。拦截链本身是跨域编排，放应用层，避免 security 反向依赖 usage。

---

## 六、落地顺序建议

1. **先拆底座**：common → provider → iam → protocol（protocol 同时完成 Facade 按 SPI 装配改造）。
2. **再拆中基**：security → usage → resilience → audit → stats/alert/experience。
3. **后拆上层**：proxy。
4. **最后能力插件化**：把 OpenAI/Anthropic Adapter 移出 gateway-boot，建 protocol-* 模块 + AutoConfiguration，gateway-boot 退化为纯组装。
5. **全程**：每个模块拆分后立即接入 ArchUnit 架构测试，确保依赖方向不回退。

**验收标准**：新增一种协议（如 Gemini）= 新增一个 `protocol-gemini` 模块（含 Adapter + AutoConfiguration）+ DB 配置，不改任何核心代码。
