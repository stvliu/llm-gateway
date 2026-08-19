# llm-gateway 模块化重构：分阶段实施任务清单

> 配套：`docs/jvm-modularity-philosophy-and-llm-gateway-feasibility.md`（总评估）、`docs/modularity-matrix-and-boundary-design.md`（依赖矩阵/边界）
> 目标：把 `gateway-boot` 从"单 jar 承载全部"拆分为"底座/中基/上层业务域 + 能力插件 + 纯组装 boot"。
> 每阶段可独立交付、可验证；全程以 ArchUnit 架构测试锁定依赖方向。

---

## Phase 0：基础设施与基线（先立规矩）

| Task | 目标 | 具体内容 | 验收标准 |
|------|------|---------|---------|
| **T0.1** | 建立 ArchUnit 测试基座 | 在 `gateway-boot` 或新建 `gateway-arch-test` 模块引入 `archunit-junit5`，落地 `docs/modularity-matrix-and-boundary-design.md` §二 的全部规则（noClasses + layeredArchitecture） | 架构测试可运行、对当前代码**全部通过**（当前无反向依赖） |
| **T0.2** | 冻结 baseline 提交 | 在拆分前对 gateway-boot 全量跑一遍 `mvn test`，记录通过率与覆盖率基线 | 基线测试全绿，JaCoCo 覆盖率达门槛（核心≥90%/适配器≥80%） |
| **T0.3** | 建立模块依赖黑名单 | 在 ArchUnit 中列出当前跨层违规点（如 Facade→Adapter 具体类）作为"待治理清单" | 违规点清单成文，作为后续各阶段验收参照 |

> 依赖：无。产出：可运行的架构测试基座。

---

## Phase 1：底座拆分（common / provider / iam / protocol）

> 原则：先拆最被依赖的底层，把 `RoutingContext`、`Facade`、实体/网关接口这些"公共面"先定型。

### 1.1 gateway-common（横切底座）

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T1.1** | 从 `gateway-boot/common/` 迁移：`exception/`（GatewayException 根异常）、`entity/`（BaseEntity 审计字段）、`util/`、`event/`（事件接口） | `common` 模块无任何业务/框架外依赖；BaseEntity 含 `created_by/created_at/updated_by/updated_at` |
| **T1.2** | 检查是否有领域特有逻辑误放在 common，下沉回各域 | common 仅含纯横切，通过 ArchUnit `common 不得依赖业务包` 校验 |

### 1.2 gateway-provider（全局中枢）

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T1.3** | 从 `gateway-boot/domain/supply/` 迁移全部：entity（Provider/Channel/Model/ModelInstance/ChannelCredential/ChannelEndpoint/ChannelActions/ChannelOperationLog）、catalog、gateway 接口、enums、valueobject（RoutingContext/ConnectivityTestResult）、exception | provider 模块可独立编译；对外只暴露实体 + Gateway 接口 |
| **T1.4** | **RoutingContext 收敛**：字段 final、无 setter、Builder 构建、外部只读 | 编译期强制不可变；proxy 层只读访问 |
| **T1.5** | **（可选）拆 `provider-api` 子模块**：把实体 + Gateway 接口 + RoutingContext 放 api，上层只依赖 api | 上层依赖收窄到 api；provider 实现变更不影响上层编译 |
| **T1.6** | 迁移 `application/`（ProviderService/ChannelService 等）+ `infrastructure/`（JPA/Repository/UpstreamClient 实现） | provider 内 application/domain/infrastructure 三层自洽 |

### 1.3 gateway-iam（身份域）

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T1.7** | 从 `gateway-boot/domain/iam/` + 相关 application/infrastructure 迁移：User/Application/APIKey/Auth/加密 | iam 模块可独立编译；加密密钥走配置外部化，禁止硬编码 |
| **T1.8** | 确认 iam 依赖面：只依赖 common（+必要时 provider 的被拦截对象） | 通过 ArchUnit 底座方向校验 |

### 1.4 gateway-protocol（协议域 + Facade 按 SPI 改造）

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T1.9** | **重命名 `gateway-capability-api` → `gateway-protocol`**（现状模块改名为目标抽象协议层）：改 `gateway-capability-api/pom.xml` 的 artifactId、父 pom `<modules>`、`gateway-boot/pom.xml` 依赖；包名 `com.codingas.gateway.api.capability.protocol` → `com.codingas.gateway.protocol`；把 Canonical IR 7 模型 + `ProtocolAdapter` SPI + 契约 DTO（OpenAIChatRequest/Response、AnthropicMessagesRequest/Response）全部并入 `gateway-protocol` | `gateway-protocol` 只含 SPI + Canonical IR + 契约 DTO，无 Spring 业务逻辑；gateway-boot 依赖更新后编译 + 测试通过 |
| **T1.10** | **Facade 按 SPI 装配改造**（解决已核实的跨层依赖）：`ProtocolConversionFacade` 改为构造注入 `List<ProtocolAdapter<?>>` 收集所有 Bean，删除对 OpenAI/Anthropic Adapter 具体类的 import | Facade 不再 import `infrastructure.protocol` 任何具体类；ArchUnit `protocol 不得依赖 capability` 通过 |
| **T1.11** | 迁移出站调谐（tuning/）、入站校验（validation/）接口到 protocol | protocol 契约完整 |

> 依赖：T1.1→T1.2→T1.3~1.8→T1.9~1.11（provider/iam/protocol 可并行，均只依赖 common）。
> 里程碑：底座四模块（common/provider/iam/protocol）全部可独立编译 + ArchUnit 通过。

---

## Phase 2：中基拆分（security / usage / resilience / audit / stats / alert / experience）

> 原则：中基只依赖底座，互不反向依赖；先拆有明确边界的，再拆弱内聚域。

### 2.1 gateway-security（安全域）

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T2.1** | 迁移 threat（IP/威胁检测）+ dataprotection（脱敏）到 security 模块 | security 只依赖 common/provider/iam |
| **T2.2** | **拦截链澄清**：把跨 auth+threat+quota 的拦截链编排放到**应用层**（或由 boot 组装），security 模块内只保留 security 自身的拦截实现 | security 不反向依赖 usage；ArchUnit 校验 |

### 2.2 gateway-usage（用量/配额，写路径）

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T2.3** | 迁移 TokenLimit/配额/用量事件/限流执行（写入/管控） | usage 依赖 common/provider/iam/protocol/security(限流接口)；限流写路径可独立验证 |
| **T2.4** | 明确 usage 的**明细表归属**，作为 stats 的读数据源（见 §附） | usage 暴露稳定的用量事件/查询接口 |

### 2.3 gateway-resilience / gateway-audit / gateway-stats / gateway-alert / gateway-experience

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T2.5** | resilience：迁移 failover/retry/circuit-breaker（infrastructure/resilience 的 Retry/CircuitBreaker/ResilientUpstreamClient） | 依赖 common/provider/protocol；不依赖上层 |
| **T2.6** | audit：迁移调用日志/审计事件 | 依赖 common/provider/iam/protocol/usage（记谁、什么请求、多少用量） |
| **T2.7** | stats：迁移聚合统计/仪表盘（读路径，见 §附） | 依赖 common/provider/usage(读明细)；**不写**用量数据 |
| **T2.8** | alert：迁移告警 | 依赖 common/provider/audit/usage/stats |
| **T2.9** | **评估合并弱内聚域**：experience（playground）可合并进 proxy 或作为 boot 的一个 view 包，避免模块过多 | 业务模块数量收敛到 8~10 个 |

> 依赖：T2.x 均依赖 Phase 1 底座；T2.3(usage) 先于 T2.7(stats)。
> 里程碑：中基模块全部可独立编译 + ArchUnit 通过。

---

## Phase 3：上层拆分（gateway-proxy）

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T3.1** | 迁移 proxy 全部：application/proxy（ChatDispatchService、routing/RoutingResolver/ModelMatcher/ChannelSelector/CredentialResolver/EndpointResolver、invoker/ChannelFailoverInvoker/KeyFailoverInvoker）、domain/proxy | proxy 依赖所有底座+中基，不反向；七阶段调度可独立验证 |
| **T3.2** | 确认 proxy 通过 Facade 编排协议转换，不依赖任何具体协议实现模块 | proxy 无 protocol-openai/anthropic 依赖 |

> 依赖：全部 Phase 1/2。
> 里程碑：gateway-boot 仅剩组装代码，业务逻辑全部迁出。

---

## Phase 4：协议能力插件化（protocol-openai / protocol-anthropic）

| Task | 迁移内容 | 验收标准 |
|------|---------|---------|
| **T4.1** | 把 `OpenAIProtocolAdapter` 从 `gateway-boot/infrastructure/protocol/` 迁入新模块 `gateway-protocol-openai`，实现 `gateway-protocol` 的 `ProtocolAdapter`，加 `OpenAIProtocolAutoConfiguration`（`@Bean` 注册 + `@ConditionalOnProperty`） | 插件模块只依赖 protocol SPI；无 Spring 业务依赖 |
| **T4.2** | 同法迁移 `AnthropicProtocolAdapter` + `ProtocolStreamConverter` 到 `gateway-protocol-anthropic` | 同上 |
| **T4.3** | `gateway-boot` 依赖 `protocol-openai/anthropic`，通过 AutoConfiguration 启用；删除原 infrastructure/protocol 实现 | boot 退化为纯组装；`mvn test` 全绿 |
| **T4.4** | 落一份 `protocol-gemini` 示例插件验证可扩展性（含 Adapter + AutoConfiguration + DB 配置） | **验收金标准**：新增协议仅新增插件模块 + DB 配置，不改任何核心代码 |
| **T4.5** | 健康机制：HealthIndicator/HealthGroup + `@Scheduled` 周期探活 | 插件启用/禁用状态可观测 |

> 依赖：Phase 3（Facade 已按 SPI 装配）。
> 里程碑：能力插件化落地，gateway-boot 纯组装。

---

## 每阶段通用验收（DoD）

1. 该阶段涉及模块**独立 `mvn clean install` 成功**。
2. **ArchUnit 架构测试全部通过**（依赖方向不回退）。
3. 迁移前后行为等价：gateway-boot 集成测试（含 ProtocolConversionIntegrationTest、ChatDispatch 相关）全绿。
4. JaCoCo 覆盖率不因迁移跌破门槛。

## 关键风险与应对

| 风险 | 应对 |
|------|------|
| 拆分期间行为回归 | 每阶段迁移后跑全量集成测试；先迁移后删原码 |
| provider 中枢被过度依赖 | T1.5 拆 provider-api，上层只依赖 api |
| Facade 反向依赖 | T1.10 必须先做，否则 Phase 4 无法进行 |
| 模块数过多 | T2.9 合并弱内聚域（alert/experience/stats 评估） |
| 依赖环（security↔usage） | T2.2 拦截链放应用层 + ArchUnit 环检测 |

---

## 附：usage（写）/ stats（读）拆分深入

### 边界
- **gateway-usage**：TokenLimit/配额/用量事件，负责**写入与管控**（高一致关键路径，配额扣减）。
- **gateway-stats**：聚合统计/仪表盘，负责**读取与展示**（最终一致）。

### 数据流（避免双向依赖）
```
usage(写)  ──写入──>  用量明细表 <──读取──  stats(读)
                          ▲
                     (usage 拥有明细表所有权)
```
- **明细表归 usage 所有**，stats 通过 usage 暴露的**只读查询接口**（而非直连表）读取，使依赖单向：`stats → usage`。
- 若 stats 需要离线聚合，可引入**异步事件 + 物化聚合表**（usage 发用量事件，stats 订阅），进一步解耦。

### 关键一致性问题
- 配额扣减（usage）是**高一致**；统计（stats）是**最终一致**。二者模型不同：usage 存"逐笔/增量"，stats 存"聚合快照"。
- **禁止 stats 反向写 usage**；配额扣减只允许在 usage 内发生。
- 若单机不满足统计实时性，先做"读 usage 明细 + 内存聚合"，避免过早引入异步链路（YAGNI）。

### ArchUnit 约束
```java
@ArchTest
static final ArchRule STATS_NOT_WRITE_USAGE =
    noClasses().that().resideInAPackage("..stats..")
        .should().dependOnClassesThat().resideInAPackage("..usage..")
        .andShould().resideInAPackage("..stats..");  // 仅允许读，方向 stats→usage 单向
```
