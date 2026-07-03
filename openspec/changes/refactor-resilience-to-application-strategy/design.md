## Context

刚归档的 `simplify-resilience-architecture` change 建立了 Cluster 故障域 + L1 共因跳过机制。但实践中发现：
1. **职责交叉**：Cluster（渠道侧全局共因分组）与 Application（应用侧 priority 转移顺序）都在影响故障转移走向，配置入口分离、彼此不可见，应用配的顺序可能被共因跳过覆盖。
2. **场景差异无法表达**：不同下游应用（流程自动化/研发自动化/AGI/BI）对容灾的诉求差异大（失败代价、延迟敏感、成本敏感、共因风险），当前全局 Cluster + 全局熔断参数无法表达应用级差异。

下游场景诉求摘要（来自场景验证）：
- 流程自动化（ERP/CRM/SRM/WMS/PLM）：高可用、静默转移、候选耗尽时希望降级兜底而非流程卡死
- 研发自动化（Claude Code/CodeX）：延迟敏感、同供应商多 Key 共因限流风险高、需长 timeout
- AGI（OpenClaw）：跨供应商分散、过载保护、需看端点熔断态势
- 商业智能 BI：成本优先、可重跑、转移时不愿越级到更贵渠道

当前可配置项：`ApplicationChannel.priority`（转移顺序）、`Application.timeout`（单一超时）、端点级熔断器（全局参数）。不足以为四场景差异化配置。

## Goals / Non-Goals

**Goals:**
- 删除 Cluster 故障域聚合根与全局共因跳过，消除与 Application 的职责交叉
- 引入应用级容灾策略，让管理员为不同下游场景配置差异化容灾
- 提供场景模板（研发自动化/流程自动化/AGI/BI）快速配置
- 补齐管理员容灾管理前端功能（端点熔断应急 UI、熔断状态大盘、策略配置页）
- 容灾总览页删 Cluster 拓扑后功能完整重组

**Non-Goals:**
- 不做下游应用请求级选择渠道分组
- 不恢复 Cluster 全局共因分组
- 不恢复 ResilienceProfile（独立实体+全局解析链+L2/PinnedModel/会话亲和）
- 不恢复 L2 自动降级链（「候选耗尽降级」仅指应用预配兜底模型）
- 不引入新的上游客户端/重试/熔断算法（复用既有）

## Decisions

> 本 change 含多个需深度 brainstorming 的设计决策，以下为方向性草案，详细方案在 `/comet-design` 阶段 brainstorming 定稿。

### D1: 容灾配置收敛到 Application
Cluster 退场，容灾走向完全由 Application 决定（授权哪些渠道 + priority 顺序 + 应用级策略）。L1 转移保留按 `ApplicationChannel.priority` 顺序逐候选尝试。

### D2: 共因跳过替代方案（待 brainstorming）
删 Cluster 后，场景2（研发自动化）同供应商多 Key 共因限流问题需替代机制。候选方向：
- **方向甲**：共因判定从 Cluster 迁移到 `Channel.providerId`（已有）+ 应用级开关——应用可选是否启用「同供应商失败跳过剩余同供应商候选」。把共因从渠道侧全局强加变为应用侧可选，化解职责重叠。
- **方向乙**：纯靠端点级熔断器 + 应用级熔断参数配置。首次故障慢，后续靠熔断器跳过。
- design 阶段 brainstorming 定方向（倾向甲）。

### D3: 应用级容灾策略数据模型（待 brainstorming）
策略挂 Application，轻量，不独立实体（避免 ResilienceProfile 回头路）。候选挂载方式：
- 作为 Application 的内嵌字段组
- 作为轻量子实体（ApplicationStrategy）关联 Application
design 阶段定。

### D4: 策略维度（待 brainstorming 筛选）
草案维度：共因跳过开关 / 候选耗尽行为（抛错或降级到预配模型）/ 成本控制（转移是否允许越级到更贵渠道）/ 转移触发条件（是否应用级覆盖全局 ErrorClassifier）。design 阶段按四场景诉求筛选最终维度集，避免过度设计。

### D5: 场景模板
预设四场景推荐值（研发自动化/流程自动化/AGI/BI），管理员套用后可微调。模板为静态推荐配置，非可自定义实体（design 确认）。

### D6: 端点熔断管理前端
补 forceOpen/forceClose 应急操作 UI + 熔断状态大盘。放 Channels 页端点维度（单渠道操作）+ 容灾总览页（全局态势）。后端 API 已有，仅前端补 UI。

### D7: 容灾总览页重组
删 Cluster 拓扑卡片后，总览页 = 转移事件流 + 耗尽告警 + 端点熔断状态大盘。保留可观测大盘职责。

## Risks / Trade-offs

- **共因跳过删除的延迟代价**：方向甲若采用，同供应商共因仍可应用级开启；方向乙则首次故障多试 N 个同供应商候选（N×RTT 延迟）。研发自动化场景痛感强，需 design 权衡。
- **回头路风险**：应用级策略可能演变为 ResilienceProfile 借尸还魂。design 阶段须明确边界（轻量、应用级、不含已删概念）。
- **降级语义滑向 L2**：「候选耗尽降级」需严格限定为应用预配兜底模型，非网关自动降级链。
- **成本控制数据依赖**：成本维度需渠道价格数据，数据来源与模型需 design 确认。
- **BREAKING 影响**：Cluster 端点整删、转移事件字段变更、failover_events 表列删除，影响外部消费者（gateway-cli 骨架不涉及，但需确认无其他消费方）。
- **大 change 风险**：减法（删 Cluster）+ 加法（策略+模板+前端）合一，design/build 周期长，需严格 brainstorming 控制范围。
