# llm-gateway 架构与模块化现状研究报告

> 生成日期：2026-08-16
> 探索级别：very thorough（全仓源码 + pom + 设计文档 + git 历史）
> 技术栈：Java 21 + Spring Boot 3.5.13 + PostgreSQL + Redis；COLA Light 5.0（单模块、package 分层）

---

## 一、模块清单及依赖

### 1.1 顶层 Maven 模块（根 pom.xml `<modules>`）

根 pom（`E:\workspace\llm-gateway\pom.xml`）声明的模块：

```xml
<modules>
    <module>gateway-capability-api</module>
    <module>gateway-boot</module>
    <module>gateway-cli</module>
    <module>gateway-simulator</module>
</modules>
```

注意：**`gateway-console`（前端 React/Vue）不在 Maven 多模块内**，它是独立的 Web 前端工程，通过 HTTP 调用 gateway-boot，不属于 Maven reactor。

各模块文件规模：

| 模块 | main Java 文件 | test Java 文件 | 说明 |
|------|------|------|------|
| gateway-capability-api | 8 | 2 | 能力 SPI 契约（纯接口 + Canonical IR 模型） |
| gateway-boot | 399 | 121 | 核心后端（所有层） |
| gateway-cli | 1 | 0 | 命令行工具（骨架） |
| gateway-simulator | 10 | 5 | LLM 提供商模拟服务 |

### 1.2 模块依赖关系

```
gateway-project (父 POM, pom 打包)
├── gateway-capability-api   (jar, 无 Spring, 仅依赖 jackson-databind + lombok/provided + junit/assertj/test)
│     └── (被 gateway-boot 依赖)
├── gateway-boot             (jar, 依赖 gateway-capability-api + 全部 Spring 生态)
├── gateway-cli              (jar, 独立, 依赖 spring-shell-starter; 不依赖 boot)
└── gateway-simulator        (jar, 独立, 依赖 spring-boot-starter-web; 不依赖 boot)
```

**关键依赖证据：**

- **gateway-boot → gateway-capability-api**（`gateway-boot/pom.xml`）：
  ```xml
  <dependency>
      <groupId>com.codingas.gateway</groupId>
      <artifactId>gateway-capability-api</artifactId>
      <version>${revision}</version>
  </dependency>
  ```
- **gateway-capability-api 无 Spring 依赖**（`gateway-capability-api/pom.xml` description 明确声明"能力 SPI 契约：规范内部模型 + ProtocolAdapter 纯接口（无 Spring 依赖）"），只依赖 `jackson-databind`（用于 CanonicalTool/CanonicalToolCall 的 JsonNode 字段）。
- **gateway-boot 的完整技术依赖**：spring-boot-starter-web、okhttp(+okhttp-sse)、spring-boot-starter-data-jpa、flyway-core、validation、actuator、cache、data-redis、caffeine、logback、logstash-logback-encoder、postgresql(runtime)、h2(runtime)、micrometer-prometheus、opentelemetry-api、sa-token、jgit、lombok。测试：spring-boot-starter-test、junit、mockito、assertj、mockwebserver。
- **gateway-cli 独立于 gateway-boot**：只依赖 `spring-shell-starter`（Spring Shell 3.3.4 BOM），不依赖 gateway-boot。这是"API 消费者"式模块。
- **gateway-simulator 独立**：只依赖 `spring-boot-starter-web`，是测试/联调用的模拟提供商，不依赖 gateway-boot。

**模块间耦合现状**：除 gateway-boot → gateway-capability-api 这一条内聚依赖外，gateway-cli / gateway-simulator 与核心完全解耦（仅通过 HTTP 消费 API，符合 CLAUDE.md "API 消费者"定位）。

---

## 二、gateway-boot 内部层次（COLA Light 分层）

`gateway-boot/src/main/java/com/codingas/gateway/` 顶层 package：

```
adapter/   application/   common/   domain/   infrastructure/
```

### 2.1 各层职责与内容（目录级证据）

| 层 | 职责 | 实际子包（目录证据） |
|----|------|------|
| **adapter** | 接收请求、返回响应 | `advice/`(全局异常)、`api/`(Controller)、`interceptor/`(拦截器)、`protocol/`(openai/、anthropic/ 两个协议校验/调谐器) |
| **application** | 用例编排、跨域协调 | `application/`、`audit/`、`auth/`、`catalog/`、`channel/`、`channelcredential/`、`experience/`、`init/`、`model/`、`protocol/`(含 conversion/)、`provider/`、`proxy/`(含 routing/、invoker/)、`quota/`、`resilience/`、`stats/`、`supply/`、`user/`、`userapikey/` |
| **domain** | 业务逻辑、领域模型 | `alert/`、`application/`、`audit/`、`dataprotection/`、`iam/`、`protocol/`(contract/、tuning/、validation/)、`quota/`、`resilience/`、`supply/`、`threat/`、`usage/` |
| **infrastructure** | 技术实现 | `actuator/`、`alert/`、`application/`、`audit/`、`common/`、`config/`、`dataprotection/`、`event/`、`iam/`、`protocol/`(OpenAIProtocolAdapter 等)、`resilience/`、`supply/`、`threat/`、`upstream/`、`usage/` |
| **common** | 跨领域共享 | `dto/`、`entity/`、`event/`、`exception/`(GatewayException 根异常)、`util/` |

### 2.2 架构设计意图（文档证据）

- **CLAUDE.md（`E:\workspace\llm-gateway\CLAUDE.md`）**：明确"COLA Light 5.0 架构：单模块架构，用 package 代替模块划分层次"。约束：
  - 分层依赖：上层依赖下层接口，禁止跨层调用或反向依赖
  - Gateway 模式：接口定义在 `domain/xxx/gateway/`，实现在 `infrastructure/xxx/gateway/`
  - 依赖倒置：Domain 只依赖 Gateway 接口，不直接依赖外部资源
  - 职责拆分架构：按业务领域内聚 Entity + Domain Service + Gateway
  - 领域模型纯洁性：JPA 实体只含 Getter/Setter，禁止含业务逻辑
- **`doc/constitution.md`（架构章程）**、**`doc/spec.md`（完整需求规格）**、**`doc/应用架构.md` / `doc/技术架构.md`**：定义了双 API 兼容、安全零信任、测试驱动、可观测性、Token 成本透明五大核心原则与架构铁律。

**依赖方向**：`adapter → application → domain → infrastructure`（应用层编排领域层服务 + 依赖基础设施的 Gateway 接口；领域层仅依赖 Gateway 接口，由 infrastructure 实现）。

---

## 三、能力插件化改造进展（核心发现）

### 3.1 背景与目标

设计 spec：`docs/superpowers/specs/2026-08-11-capability-plugin-design.md`（标题《能力标准化定义与插件化接入设计》，status: draft）

**现状痛点（spec §1.1）**：旧 `ProtocolConverter` 是硬编码的 OpenAI↔Anthropic 两两转换器（N×N 组合爆炸）；`Model.capabilities` 字段仅存储未参与决策；新增协议 = 改核心转换器。

**设计决策（spec §2）**：
- D2：插件形态 = **Spring Boot Starter 构建期模块化**（放弃外部 JAR 热插拔）
- D3：`gateway-capability-api` 为**稳定 SPI（纯接口）**，能力模块只依赖它
- D4：按协议/能力类型划分，不按厂商/模型
- D6：**插件零厂商数据**——厂商数据全部进 DB（provider_config/models_config）
- D7：本轮仅覆盖 chat 类（embedding/rerank 后置，YAGNI）
- D8：每协议一个 Adapter 做"原生↔规范"，消除 N×N

**完整落地架构（spec §3）**：最终目标是拆出 `gateway-capability-openai` / `gateway-capability-anthropic` 两个 Starter 模块，`gateway-boot` 改为纯组装。当前只做到第 1 阶段。

### 3.2 Canonical IR（规范中间表示）

定义于 `gateway-capability-api`，包 `com.codingas.gateway.api.capability.protocol`。是**与厂商无关的中立表示**，用于"原生→规范→原生"两跳转换：

| 规范类 | 文件路径 | 关键字段 |
|--------|---------|---------|
| CanonicalChatRequest | `.../api/capability/protocol/CanonicalChatRequest.java` | model、system、messages、maxTokens、temperature、stop、tools、toolChoice、stream |
| CanonicalMessage | `.../CanonicalMessage.java` | role、content、toolCalls、toolCallId、name |
| CanonicalTool | `.../CanonicalTool.java` | name、description、parameters(JsonNode JSON Schema) |
| CanonicalToolCall | `.../CanonicalToolCall.java` | id、name、arguments(JsonNode) |
| CanonicalChatResponse | `.../CanonicalChatResponse.java` | id、model、content(List<CanonicalContentBlock>)、stopReason、usage |
| CanonicalContentBlock | `.../CanonicalContentBlock.java` | type("text"/"toolUse")、text、toolUse |
| CanonicalUsage | `.../CanonicalUsage.java` | inputTokens、outputTokens |
| **ProtocolAdapter (SPI)** | `.../ProtocolAdapter.java` | 见下 |

**ProtocolAdapter SPI**（`ProtocolAdapter.java`，纯接口，无 Spring）：

```java
public interface ProtocolAdapter<T> {
    String protocol();                                   // "openai" / "anthropic"
    CanonicalChatRequest normalizeRequest(T nativeRequest);   // 入站: 原生→规范
    T denormalizeRequest(CanonicalChatRequest canonical);     // 出站: 规范→原生
    CanonicalChatResponse normalizeResponse(Object nativeResponse);
    Object denormalizeResponse(CanonicalChatResponse canonical);
}
```

任意两协议互转 = normalize + denormalize 两跳，从根上消除 N×N 转换器。

### 3.3 第 1 阶段实施计划

`docs/superpowers/plans/2026-08-12-capability-plugin-phase1.md`，标题《能力插件化第 1 阶段：Canonical IR + ProtocolAdapter SPI 实施计划》，包含 5 个 Task：

- **Task 1**：新建 `gateway-capability-api` 模块 + Canonical IR 模型（7 个类）
- **Task 2**：定义 `ProtocolAdapter` SPI
- **Task 3**：实现 `OpenAIProtocolAdapter`
- **Task 4**：实现 `AnthropicProtocolAdapter`
- **Task 5**：实现 `ProtocolConversionFacade` 并重写 `ChannelFailoverInvoker`，删除旧 `ProtocolConverter`

计划落地顺序（spec §9）：IR 落地 → 迁移 Adapter → Capability 体系(注册表) → 拆 Starter 模块 → 示例插件（Gemini）。

### 3.4 已完成 vs 未完成（实际源码证据）

**✅ 已完成（第 1 阶段全部落地，git 历史 `4405d284`~`35ca4088`）：**

| 文件 | 路径 | 状态 |
|------|------|------|
| Canonical IR 7 模型 | `gateway-capability-api/.../protocol/*.java` | 已实现（8 个 main 文件） |
| ProtocolAdapter SPI | `gateway-capability-api/.../protocol/ProtocolAdapter.java` | 已实现 |
| OpenAIProtocolAdapter | `gateway-boot/.../infrastructure/protocol/OpenAIProtocolAdapter.java` | 已实现 |
| AnthropicProtocolAdapter | `gateway-boot/.../infrastructure/protocol/AnthropicProtocolAdapter.java` | 已实现 |
| ProtocolStreamConverter（流式，平移旧逻辑） | `gateway-boot/.../infrastructure/protocol/ProtocolStreamConverter.java` | 已实现 |
| ProtocolConversionFacade | `gateway-boot/.../application/protocol/conversion/ProtocolConversionFacade.java` | 已实现 |
| 旧 ProtocolConverter 删除 | 已删除（`domain/protocol/conversion/` 目录已空） | ✅ |

测试已落地：`CanonicalChatRequestTest`、`ProtocolAdapterContractTest`（capability-api）；`OpenAIProtocolAdapterTest`、`AnthropicProtocolAdapterTest`、`ProtocolStreamConverterTest`、`ProtocolConversionFacadeTest`、`ProtocolConversionIntegrationTest`、`AnthropicTunerTest`（gateway-boot）。

**⚠️ 与计划的两处偏离（git 提交证据）：**

1. **Facade 落位变更**：计划要求在 `domain/protocol/conversion/` 建 Facade，实际提交 `4033d228 "fix: 迁移 ProtocolConversionFacade 到 application 层并清理已删除 ProtocolConverter 断链引用"` 把它迁移到了 `application/protocol/conversion/`。因此 `domain/protocol/conversion/` 现在为空目录，`application/protocol/conversion/` 承载 Facade。注：这属于跨层落位——转换门面依赖 `infrastructure.protocol` 的 Adapter 具体类（`OpenAIProtocolAdapter`/`AnthropicProtocolAdapter` 构造注入），放在 application 层调用 infrastructure 具体实现，与"domain 只依赖 Gateway 接口"的纯度约束存在张力（见第四节问题 2）。

2. **流式转换保持原样**：spec §4.2 提到"规范 chunk 事件"，但计划按 YAGNI 将流式 chunk 转换平移为 `ProtocolStreamConverter`（仍走 JSON 字符串方向），未做 canonical 化。计划 Self-Review 明确标注"流式 canonical 化…后续阶段再做"。

**❌ 未完成（spec §3、§9 的后续阶段，仅设计无代码）：**
- `Capability` 模型 + `CapabilityRegistry`（能力注册表）——未实现
- 能力感知路由（打通 `Model.capabilities` DB 字段参与路由/降级）——未实现
- `gateway-capability-openai` / `gateway-capability-anthropic` 两个独立 Starter 模块拆分——未实现（当前两个 Adapter 仍在 `gateway-boot` 内，`gateway-boot` 依赖 `gateway-capability-api`，但未再拆出能力模块）
- 健康机制（HealthIndicator/HealthGroup/`@Scheduled` 周期探活）——未实现
- Gemini 示例插件、embedding 能力类型——未实现

**结论**：能力插件化目前**只完成了"Canonical IR + ProtocolAdapter SPI"这第 1 阶段（协议转换层的重构与去 N×N）**；真正意义上的"插件化"（独立能力 Starter 模块、能力注册表、能力感知路由、厂商数据全进 DB）尚在设计阶段，未开始编码。设计文档中的 `gateway-core`、`gateway-capability-openai/anthropic/gemini` 模块**在物理上还不存在**。

---

## 四、模块化约束与潜在问题

### 4.1 现有约束（良好实践）

1. **依赖倒置 + Gateway 模式**：domain 只依赖 `domain/xxx/gateway/` 接口，实现在 `infrastructure/xxx/gateway/`（CLAUDE.md 明文约束）。
2. **gateway-capability-api 纯净**：无 Spring 依赖，只依赖 jackson，是真正可被第三方能力模块复用的稳定契约（依赖方向单向：boot → capability-api）。
3. **消费者解耦**：gateway-cli / gateway-simulator 不依赖 gateway-boot，走 HTTP，是干净的 API 消费者。
4. **配置外部化**：根 pom `dependencyManagement` + `@ConfigurationProperties`；JaCoCo 覆盖率门槛（核心 ≥90% / 规则引擎 ≥85% / 适配器 ≥80%）。
5. **全实体可审计**：`created_by/created_at/updated_by/updated_at`。

### 4.2 潜在问题

**问题 1：gateway-boot 仍然过重（单体过载）。**
- gateway-boot 399 个 main 文件，application 层 19 个子包、domain 层 11 个领域、infrastructure 15 个子包。虽然用 package 分层，但**物理上仍是单 jar**，所有领域/技术栈（协议、供给、配额、审计、告警、威胁、IAM、韧性、usage、dataprotection）耦合在同一个 Spring 上下文中。
- 能力插件化设计文档（§3）计划将 gateway-boot 演进为"纯组装"，但当前尚未拆分出任何 `gateway-capability-*` 模块，说明"COLA 单模块分层"在规模增长后已逼近拆分临界点。

**问题 2：Facade 跨层落位，与领域纯净约束冲突。**
- `ProtocolConversionFacade`（application 层）直接依赖 `infrastructure.protocol.OpenAIProtocolAdapter`/`AnthropicProtocolAdapter` 具体类，未走 domain 的 Gateway 接口。这违反"上层依赖下层接口"的分层依赖约束——application 直接越过 domain 依赖了 infrastructure 实现。设计文档原本规划 Facade 在 `domain/protocol/conversion/`（依赖能力 SPI），实际因 Adapter 在 infrastructure 而被迫上移，暴露了"Adapter 放哪一层"未定的架构遗留问题（当前 Adapter 既是 capability-api SPI 实现、又被放在 infrastructure，却未被 domain 抽象）。

**问题 3：真正可跨模块复用的能力边界尚未形成。**
- 目前只有 `gateway-capability-api`（规范模型 + SPI）是"可复用契约"。OpenAI/Anthropic 两个 Adapter 仍嵌在 gateway-boot 内，无法被外部能力模块复用；Capability 注册表、能力感知路由缺失，`Model.capabilities` 字段（spec §1.1 指出）仍是"仅存储、未参与决策"。
- 也就是说，**复用边界目前只有"协议转换"一处打通，能力/插件边界还是纸面设计**。

**问题 4：gateway-cli 名不副实。**
- gateway-cli 仅 1 个 main 文件、0 测试，是骨架。其与 gateway-boot 完全无依赖（不共享任何契约/模型），意味着 CLI 要调用 API 只能靠 HTTP 或自行重复定义 DTO，存在潜在重复与漂移风险（对 gateway-capability-api 这种"纯接口契约"也没有复用）。

**问题 5：双模块分层 vs 插件化的张力（粒度差异）。**
- 当前模块按"架构层次 + 部署形态"划分（boot / cli / simulator / capability-api），而能力插件化设计希望按"业务能力/协议"划分（openai / anthropic / gemini…）。两种粒度并存，`gateway-boot` 既承载全部层次又（当前）承载全部协议实现，是矛盾集中点。

---

## 五、领域划分表（domain 层）

`gateway-boot/.../domain/` 下共 11 个领域（目录证据）：

| 领域 | 职责（据命名与 CLAUDE.md） | 关键子结构 |
|------|------|------|
| protocol | 协议领域 | `contract/`(OpenAIChatRequest/Response、AnthropicMessagesRequest/Response、ProtocolRequest/Response、StreamCallback/StreamChunkResult)、`tuning/`(出站调谐接口)、`validation/`(入站校验接口) |
| supply | 供给域 | 提供商/模型供给相关 |
| proxy | 模型代理领域 | 代理调度（application 层 `proxy/routing/`、`proxy/invoker/` 消费） |
| model | 模型广场领域 | Model 实体（含 capabilities/modalities 字段） |
| security → iam | 访问控制领域 | 实际目录名为 `iam/`（身份与访问管理） |
| quota | 用量管控领域 | Token/配额预算 |
| usage | 用量 | 请求用量统计 |
| audit | 审计追溯领域 | 全实体审计 |
| alert | 告警通知领域 | 告警 |
| dataprotection | 数据保护 | 脱敏/数据保护 |
| threat | 威胁 | 安全威胁检测 |
| resilience | 韧性 | 重试/熔断/限流规则（对应 infrastructure `resilience/` 的 Retry/CircuitBreaker） |

> 说明：CLAUDE.md 所述"security/access 控制领域"在源码中体现为 `domain/iam/`；"proxy"领域实体在 domain 层为 `proxy/`，调度编排在 application 层 `application/proxy/`（routing/、invoker/ 含 `ChannelFailoverInvoker`）。**domain 层 `protocol/` 只有 contract/tuning/validation，没有 conversion**（conversion 被迁移到 application 层，见第三节偏离 1）。

---

## 六、与 Jmix 的模块化差异（简要）

| 维度 | llm-gateway（COLA 分层式） | Jmix（按能力/技术域 addon 式） |
|------|------|------|
| 划分粒度 | **按架构层次**：adapter/application/domain/infrastructure 一层一个大包，领域在其中分包 | **按业务能力/技术域**：每个 addon 是独立模块，含自己的实体+服务+UI |
| 模块实体 | 单模块 `gateway-boot` 承载所有层次；能力拆分刚开始（capability-api 是新契模块） | 每个 addon 独立 jar/artifact，天然可插拔复用 |
| 复用边界 | 目前仅 `gateway-capability-api`（规范模型+SPI）是稳定契约可复用 | addon 即为复用单元，跨项目可装 |
| 分层 vs 聚合 | 强调**层间依赖倒置**（domain 只依赖 Gateway 接口），但物理上单 jar | 强调**能力内聚**（一个 addon 内自含完整纵向切片），靠 addon 依赖关系组织 |
| 在 llm-gateway 的体现 | gateway-boot 内部 11 领域 + 19 个 application 子包都挤在一个模块，靠 package 约定隔离；能力插件化设计（spec §3）设想拆出 gateway-capability-openai/anthropic 为独立模块，即从"分层式"向"能力式"演进，但尚未落地 | llm-gateway 无 Jmix 的 addon 体系；其模块化差异点在于：**COLA 的模块边界是"架构层"，Jmix 的模块边界是"业务能力/技术域"**——llm-gateway 的能力插件化正是试图引入"能力模块"这种第二种粒度，但目前两种粒度未统一，是架构演进中的主要张力来源（见第四节问题 5） |

---

## 七、关键文件路径索引

**构建与模块**
- `E:\workspace\llm-gateway\pom.xml`（父 POM，`<modules>` 声明 + 全部依赖管理）
- `E:\workspace\llm-gateway\gateway-capability-api\pom.xml`
- `E:\workspace\llm-gateway\gateway-boot\pom.xml`
- `E:\workspace\llm-gateway\gateway-cli\pom.xml`
- `E:\workspace\llm-gateway\gateway-simulator\pom.xml`

**架构文档**
- `E:\workspace\llm-gateway\CLAUDE.md`
- `E:\workspace\llm-gateway\doc\constitution.md`（架构章程）
- `E:\workspace\llm-gateway\doc\spec.md`（需求规格）
- `E:\workspace\llm-gateway\doc\应用架构.md`、`doc\技术架构.md`、`doc\数据架构.md`

**能力插件化设计与计划**
- `E:\workspace\llm-gateway\docs\superpowers\specs\2026-08-11-capability-plugin-design.md`（设计 spec）
- `E:\workspace\llm-gateway\docs\superpowers\plans\2026-08-12-capability-plugin-phase1.md`（第 1 阶段实施计划）

**Canonical IR + SPI（gateway-capability-api）**
- `E:\workspace\llm-gateway\gateway-capability-api\src\main\java\com\codingas\gateway\api\capability\protocol\CanonicalChatRequest.java`（及其同目录的 CanonicalMessage/CanonicalTool/CanonicalToolCall/CanonicalChatResponse/CanonicalContentBlock/CanonicalUsage）
- `E:\workspace\llm-gateway\gateway-capability-api\src\main\java\com\codingas\gateway\api\capability\protocol\ProtocolAdapter.java`（SPI）

**Adapter 与 Facade（gateway-boot）**
- `E:\workspace\llm-gateway\gateway-boot\src\main\java\com\codingas\gateway\infrastructure\protocol\OpenAIProtocolAdapter.java`
- `E:\workspace\llm-gateway\gateway-boot\src\main\java\com\codingas\gateway\infrastructure\protocol\AnthropicProtocolAdapter.java`
- `E:\workspace\llm-gateway\gateway-boot\src\main\java\com\codingas\gateway\infrastructure\protocol\ProtocolStreamConverter.java`
- `E:\workspace\llm-gateway\gateway-boot\src\main\java\com\codingas\gateway\application\protocol\conversion\ProtocolConversionFacade.java`（注意：application 层，非 domain）
- `E:\workspace\llm-gateway\gateway-boot\src\main\java\com\codingas\gateway\domain\protocol\contract\`（OpenAIChatRequest/Response、AnthropicMessagesRequest/Response、ProtocolRequest/Response、StreamCallback、StreamChunkResult）
- `E:\workspace\llm-gateway\gateway-boot\src\main\java\com\codingas\gateway\application\proxy\invoker\ChannelFailoverInvoker.java`（改造后走 Facade）

**测试**
- `E:\workspace\llm-gateway\gateway-capability-api\src\test\...\CanonicalChatRequestTest.java`、`ProtocolAdapterContractTest.java`
- `E:\workspace\llm-gateway\gateway-boot\src\test\...\infrastructure\protocol\OpenAIProtocolAdapterTest.java`、`AnthropicProtocolAdapterTest.java`、`ProtocolStreamConverterTest.java`
- `E:\workspace\llm-gateway\gateway-boot\src\test\...\application\protocol\conversion\ProtocolConversionFacadeTest.java`
- `E:\workspace\llm-gateway\gateway-boot\src\test\...\integration\ProtocolConversionIntegrationTest.java`
- `E:\workspace\llm-gateway\gateway-boot\src\test\...\adapter\protocol\anthropic\AnthropicTunerTest.java`
