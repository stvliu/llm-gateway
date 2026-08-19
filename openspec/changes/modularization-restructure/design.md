# 模块化重构：技术设计

## Context

`gateway-boot` 是单 jar 承载全部业务的后端模块（399 个 main 文件、11 个领域、19 个 application 子包），采用 COLA Light 5.0 的"包级分层"（adapter/application/domain/infrastructure）。能力插件化改造已完成第 1 阶段——在 `gateway-capability-api` 定义了 Canonical IR（7 个规范模型）与 `ProtocolAdapter` SPI，并实现 OpenAI/Anthropic 两个 Adapter + `ProtocolConversionFacade`，删除了旧 `ProtocolConverter`（消除 N×N 转换器）。

但存在结构性问题：
1. `gateway-boot` 仍物理单 jar，所有领域/技术栈耦合在同一 Spring 上下文，逼近拆分临界点。
2. `ProtocolConversionFacade` 跨层依赖 `infrastructure.protocol` 的 Adapter **具体类**（构造注入），违反"domain 只依赖 Gateway 接口"的分层约束。
3. 真正插件化（独立能力模块、能力注册表、能力感知路由、打通 `Model.capabilities`）仅在设计阶段，`gateway-capability-openai/anthropic` 模块物理上不存在。
4. `gateway-capability-*` 命名与 LLM"模型能力"（modality/capability）语义混淆。

参考：Jmix 框架（`E:\workspace\jmix-3.0.1`）的模块化哲学——"BOM 统一版本 + Gradle 插件 + Spring Boot Starter 自动配置 + 纯接口 SPI"，以"业务能力/技术域"为模块边界。llm-gateway 采用其范式但以"私有单实例网关"定位为尺。

设计素材：`docs/modularity-matrix-and-boundary-design.md`（依赖矩阵/边界）、`docs/modularity-implementation-plan.md`（分阶段任务）、`docs/jvm-modularity-philosophy-and-llm-gateway-feasibility.md`（总评估）。

## Goals / Non-Goals

**Goals:**
- 把 `gateway-boot` 拆解为业务域 Maven 模块，边界从"包"提升到"模块"，每模块内部保持 application/domain/infrastructure 分层。
- 引入"抽象协议层 `gateway-protocol` / 具体实现 `gateway-protocol-*`"对称命名，落地协议能力插件化。
- `gateway-boot` 退化为纯组装/启动模块。
- 用 ArchUnit 把依赖方向固化为可执行约束，防止回归。
- 新增协议（如 Gemini）= 新增一个 `gateway-protocol-*` 模块 + DB 配置，不改核心代码。

**Non-Goals:**
- 不做外部 JAR 热插拔（采用 Spring Boot Starter 构建期模块化，见 Decision D1）。
- 不改变任何存量运行时业务行为（纯结构性重构）。
- 不引入 Jmix 的完整 addon 生态（Studio、BOM 商业分层、跨项目复用）。
- 本轮不覆盖 embedding/rerank 能力类型（YAGNI）。
- 不重构 `gateway-console`/`gateway-cli`/`gateway-simulator`（非后端业务域）。

## Decisions

### D1：插件形态 = Spring Boot Starter 构建期模块化（非热插拔）
采用构建期多模块 + `AutoConfiguration` 自动装配，而非外部 JAR 热插拔。
**理由**：llm-gateway 是私有单实例部署，无热插拔诉求；Starter + AutoConfiguration 是 Jmix/Spring 生态成熟范式，成本低、可观测性好。
**备选**：OSGi/Java ServiceLoader 热插拔——过度设计，弃。

### D2：模块粒度 = 业务域，控制数量在 8~10 个
按业务域拆（provider/iam/security/usage/stats/resilience/audit/alert/experience/proxy），弱内聚域（alert/experience/stats）评估合并。
**理由**：与现有 domain 领域对应；避免"模块数陷阱"（模块过多导致依赖管理与构建成本激增）。Jmix 有 120 个模块因它是开源框架，llm-gateway 是私有网关，需克制。

### D3：抽象协议层 `gateway-protocol` / 具体实现 `gateway-protocol-*` 对称命名
`gateway-protocol`（SPI + Canonical IR + 契约 DTO + Facade）供实现依赖；`gateway-protocol-openai/anthropic` 实现 SPI。
**理由**：`capability` 与 LLM"模型能力"混淆；`protocol-*` 与抽象层形成对称族，表达"某协议方言的实现"，按协议扩展时命名体系自洽。
**BREAKING**：现状 `gateway-capability-api` 重命名为 `gateway-protocol`（artifactId + 包名 `com.codingas.gateway.api.capability.protocol` → `com.codingas.gateway.protocol`）。

### D4：`ProtocolConversionFacade` 改为"按 SPI 装配"，protocol 不依赖具体能力
`gateway-protocol` 内的 Facade 通过 Spring 注入 `List<ProtocolAdapter<?>>` 收集所有已注册的 Adapter Bean（`Map<protocol, adapter>`），不 import 任何具体实现类。
**理由**：解决已核实的跨层依赖（Facade 构造注入 `OpenAIProtocolAdapter`/`AnthropicProtocolAdapter` 具体类）。借鉴 Jmix 的 `DataStoreFactory`（core 定义接口，实现以原型 Bean 注册、按名装配）。
**备选**：Facade 放在能力模块——会造成 SPI 模块反向依赖实现，弃。

### D5：能力注册表 `CapabilityRegistry` + 能力感知路由
引入能力注册表按协议/能力类型注册；打通 `Model.capabilities` DB 字段参与路由/降级决策。
**理由**：spec D4"按协议/能力类型划分，不按厂商/模型"；让 `capabilities` 字段从"仅存储"变为"参与决策"。
**备选**：路由仍硬编码协议——无法支撑新增协议不改核心，弃。

### D6：依赖方向铁律 + ArchUnit 固化
```
common ← protocol/provider/iam/security ← usage/stats/resilience/audit/alert/experience ← proxy ← boot
protocol-openai/anthropic → gateway-protocol(SPI)
```
用 ArchUnit `noClasses()` + `layeredArchitecture()` 固化 5 条铁律（无人反向依赖 boot、能力插件只依赖 SPI、protocol 不依赖 capability-*、中基不被底座反向依赖、common 纯横切）。
**理由**：依赖方向是模块化的生命线，需可执行约束防回归。

### D7：usage（写）/ stats（读）拆分
`gateway-usage` 负责配额扣减/用量事件（高一致写路径）；`gateway-stats` 负责聚合统计/仪表盘（最终一致读路径）。**明细表归 usage 所有**，stats 经 usage 的只读查询接口读取，保持 `stats → usage` 单向依赖。
**理由**：写读分离（CQRS 思路）降低关键路径耦合。
**备选**：stats 直连明细表——造成双向依赖，弃。

### D8：security / iam / usage 边界
security = 入口防护/拦截（IP 级限流、威胁、脱敏）；usage = 资源管控/配额（Key 级 TokenLimit）。**拦截链是跨域编排，放应用层**（不塞进 security），避免 security 反向依赖 usage。

### D9：`RoutingContext` 收敛为不可变值对象
`RoutingContext` 在供给域产出、被 proxy 消费，位置合理（不需下沉 common）；收敛为字段 final、无 setter、Builder 构建、外部只读；provider 公开面收敛为"实体 + Gateway 接口"，必要时拆 `provider-api` 子模块。

## Risks / Trade-offs

- **[拆分期间行为回归] → 每阶段迁移后跑全量集成测试，先迁移后删原码**。
- **[模块数过多，过度模块化] → D2 合并弱内聚域，控制在 8~10 个；以私有网关定位为尺，避免照搬 Jmix 完整 addon 生态**。
- **[provider 全局中枢被过度依赖] → D9 收敛公开面 + 拆 provider-api**。
- **[依赖环（security↔usage 等）] → D8 拦截链放应用层 + ArchUnit 环检测**。
- **[Facade 反向依赖未解决则 Phase 4 无法进行] → D4 必须在拆分能力模块前完成**。
- **[`gateway-capability-api` 重命名波及面广] → 作为显式 Task（T1.9）分步迁移 artifactId + 包名 + 依赖，独立验证**。

## Migration Plan

按 `docs/modularity-implementation-plan.md` 分 5 阶段，每阶段独立交付、可验证：
1. **Phase 0**：ArchUnit 测试基座 + 基线冻结 + 跨层违规点清单。
2. **Phase 1**：底座拆分（common → provider/iam → protocol），含 `gateway-capability-api` 重命名（T1.9）。
3. **Phase 2**：中基拆分（security/usage/resilience/audit/stats/alert/experience）。
4. **Phase 3**：上层拆分（proxy），boot 退化为纯组装。
5. **Phase 4**：协议能力插件化（protocol-openai/anthropic + AutoConfiguration + 健康机制 + Gemini 示例）。

**回滚策略**：git 分阶段提交；每阶段是一个可回退的独立 commit；`gateway-capability-api` 重命名保留旧模块到最终确认前。

## Open Questions

- （无重大未决）`provider` 是否拆 `provider-api` 子模块（D9）可在 Phase 1 根据实际依赖面现场定夺。
- 弱内聚域合并的最终边界（D2）在 Phase 2 拆分时依据真实耦合度调整。
