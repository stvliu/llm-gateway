# Resilience Console Delta Spec

## MODIFIED Requirements

### Requirement: 容灾总览页

容灾总览页 SHALL 作为容灾态势可观测大盘，展示转移事件流、耗尽告警、端点熔断状态。**删除** 故障域拓扑（Cluster 分组）与按 clusterId 归域展示（Cluster 退场）。**删除** 转移事件流的 clusterId 与「是否共因跳过」展示（共因跳过退场）。**新增** 端点熔断状态大盘区块，展示各端点熔断器当前状态（CLOSED/OPEN/HALF_OPEN）。

**展示内容**:
- 转移事件流（按 occurredAt 倒序，含 from→to 渠道、错误类型、决策、耗尽标记；**不含** clusterId/commonCauseSkip）
- 耗尽告警（exhausted=true）
- 端点熔断状态大盘（各端点熔断器状态 + 应急操作入口）

#### Scenario: 总览页展示端点熔断状态

- **WHEN** 管理员访问容灾总览页
- **THEN** 页面 SHALL 展示各端点熔断器当前状态（CLOSED/OPEN/HALF_OPEN）
- **THEN** 管理员可从大盘触发 forceOpen/forceClose 应急操作

#### Scenario: 总览页不展示 Cluster 拓扑与共因跳过

- **WHEN** 管理员访问容灾总览页
- **THEN** 页面 SHALL NOT 展示 Cluster 拓扑、按 clusterId 归域、共因跳过标记

### Requirement: 应用管理页容灾模式选择

应用管理页 SHALL 支持管理员为应用配置失败处理策略（`FAIL_FAST`/`FAIL_OVER`/`FAIL_RETRY`）与应用级超时 timeout。**移除** 容灾画像绑定相关（已随 ResilienceProfile 退场）。应用渠道 priority 排序配置（ChannelManageModal）保留。

#### Scenario: 管理员配置应用失败处理策略

- **WHEN** 管理员在应用编辑页选择失败处理策略（如 FAIL_RETRY）
- **THEN** 系统 SHALL 保存到应用 `failureStrategy` 字段
- **THEN** 该应用请求的 L0/L1 行为 SHALL 按所选策略执行

#### Scenario: 管理员配置应用超时

- **WHEN** 管理员在应用编辑页配置 timeout
- **THEN** 系统 SHALL 保存到应用 `timeout` 字段

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
