# Application Resilience Strategy Delta Spec

> 新增能力：应用级场景化容灾策略。让管理员为不同下游应用场景（流程自动化/研发自动化/AGI/BI）配置差异化容灾，替代已退场的 Cluster 全局共因分组。

## ADDED Requirements

### Requirement: 应用级容灾策略

系统 SHALL 提供应用级容灾策略，挂在 Application 聚合根下（轻量，不独立成实体），承载该应用的差异化容灾配置。策略 SHALL 至少包含：共因跳过开关、候选耗尽行为。具体维度集与数据模型由 design 阶段 brainstorming 定稿。

**共因跳过开关**（替代 Cluster 共因，基于 providerId）：
- 开启时：候选共因失败（`FailoverDecision=L1`）后，跳过候选列表中同 `providerId` 的后续候选，直接尝试异供应商候选
- 关闭时：纯按 `ApplicationChannel.priority` 顺序逐候选尝试，不做共因跳过
- 默认值由场景模板决定

**候选耗尽行为**：
- 全部候选失败时，应用可选「抛出最后异常」或「降级到应用预配的兜底模型」
- 降级仅指应用预配的单一兜底模型，非网关自动降级链（不恢复 L2）

#### Scenario: 研发自动化场景启用共因跳过

- **WHEN** 应用策略开启共因跳过，候选1（providerId=openai）共因失败（如限流）
- **THEN** `ChannelFailoverInvoker` SHALL 跳过同 providerId 的后续候选
- **THEN** 系统 SHALL 继续尝试异供应商候选

#### Scenario: BI 场景关闭共因跳过

- **WHEN** 应用策略关闭共因跳过
- **THEN** `ChannelFailoverInvoker` SHALL 纯按 `ApplicationChannel.priority` 顺序逐候选尝试，不做共因跳过

#### Scenario: 候选耗尽降级到预配兜底模型

- **WHEN** 应用策略候选耗尽行为=降级，且全部候选失败
- **THEN** 系统 SHALL 转移到应用预配的兜底模型候选
- **THEN** 系统 SHALL NOT 触发网关自动降级链

### Requirement: 容灾场景模板

系统 SHALL 提供预设容灾场景模板，管理员可为应用套用模板快速配置策略。模板 SHALL 至少覆盖：研发自动化、流程自动化、AGI、商业智能 BI。每个模板 SHALL 提供该场景的策略推荐值（共因跳过开关、耗尽行为、timeout 等）。

#### Scenario: 管理员套用研发自动化模板

- **WHEN** 管理员为应用套用「研发自动化」模板
- **THEN** 系统 SHALL 将策略设为该模板推荐值（如共因跳过=开启、timeout=较长）
- **THEN** 管理员可在此基础上微调

#### Scenario: 管理员套用 BI 模板

- **WHEN** 管理员为应用套用「商业智能 BI」模板
- **THEN** 系统 SHALL 将策略设为该模板推荐值（如共因跳过=关闭、成本控制=严格）
