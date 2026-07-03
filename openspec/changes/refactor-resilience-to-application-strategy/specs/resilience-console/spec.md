# Resilience Console Delta Spec

## MODIFIED Requirements

### Requirement: 容灾总览页

容灾总览页 SHALL 作为容灾态势可观测大盘，展示转移事件流、耗尽告警、端点熔断状态。**删除** Cluster 拓扑卡片与按 cluster 分组展示（Cluster 退场）。**新增** 端点熔断状态大盘区块，展示各端点熔断器当前状态（CLOSED/OPEN/HALF_OPEN），支持从大盘触发应急操作。

**展示内容**:
- 转移事件流（按 occurredAt 倒序，含 errorType/decision/exhausted，**不含** clusterId/commonCauseSkip）
- 耗尽告警（exhausted=true）
- 端点熔断状态大盘（各端点熔断器状态 + 应急操作入口）

#### Scenario: 总览页展示端点熔断状态

- **WHEN** 管理员打开容灾总览页
- **THEN** 页面 SHALL 展示各端点熔断器当前状态
- **THEN** 管理员可从大盘触发 forceOpen/forceClose 应急操作

#### Scenario: 总览页不展示 Cluster 拓扑

- **WHEN** 管理员打开容灾总览页
- **THEN** 页面 SHALL NOT 展示 Cluster 拓扑卡片或按 cluster 分组

### Requirement: 应用管理页容灾模式选择

应用管理页 SHALL 支持管理员为应用配置容灾策略与套用场景模板。**移除** 容灾画像绑定相关（已随 ResilienceProfile 退场）。**新增** 应用级容灾策略配置入口（共因跳过开关、候选耗尽行为等，详见 application-resilience-strategy capability）与场景模板选择（研发自动化/流程自动化/AGI/BI）。应用 timeout 配置保留。

#### Scenario: 管理员套用场景模板配置策略

- **WHEN** 管理员在应用编辑页选择「研发自动化」模板
- **THEN** 系统 SHALL 将应用策略设为模板推荐值
- **THEN** 管理员可微调后保存

#### Scenario: 管理员配置应用容灾策略

- **WHEN** 管理员在应用编辑页配置共因跳过开关与耗尽行为
- **THEN** 系统 SHALL 保存到应用级容灾策略

## ADDED Requirements

### Requirement: 端点熔断应急操作

管理员 SHALL 能从前端对端点执行熔断应急操作：一键强制熔断（forceOpen，摘流量）、一键强制恢复（forceClose，解除手动熔断）、查询熔断器状态。操作入口位于 Channels 页端点维度与容灾总览页熔断状态大盘。

#### Scenario: 管理员一键熔断故障端点

- **WHEN** 管理员在端点维度点击「强制熔断」
- **THEN** 系统 SHALL 调用 forceOpen 使端点熔断器进入 OPEN
- **THEN** 该端点流量立即被切断

#### Scenario: 管理员一键恢复端点

- **WHEN** 管理员在端点维度点击「强制恢复」
- **THEN** 系统 SHALL 调用 forceClose 使端点熔断器回到 CLOSED 并重置窗口
