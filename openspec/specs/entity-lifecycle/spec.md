# Entity Lifecycle
## Summary

## Purpose

定义供应域实体（Channel、ModelInstance 等）生命周期阶段的状态机契约：明确各阶段允许的迁移与路由资格，保证实体状态流转可预期、可审计，并作为路由与告警规则的判定依据。

## Requirements

### Requirement: 实体生命周期阶段定义


供应域实体（Channel、ModelInstance）SHALL 支持五阶段生命周期：PENDING、ACTIVE、SUSPENDED、DEPRECATED、RETIRED。

- PENDING：实体已创建但配置不完整，不得参与路由
- ACTIVE：实体配置完整，正常运行，参与路由
- SUSPENDED：管理员暂停，可恢复，不参与路由
- DEPRECATED：上游标记即将下线，仍参与路由但优先级低于 ACTIVE
- RETIRED：已废弃，不可逆，不参与路由

#### Scenario: 新创建的实体默认为 PENDING

- **WHEN** 一个新的 Channel 或 ModelInstance 被创建
- **THEN** 其 phase 必须默认为 PENDING

#### Scenario: PENDING 实体不参与路由

- **WHEN** InstanceSelector 查询可用模型实例
- **THEN** phase 为 PENDING 的 ModelInstance 不得出现在结果中

#### Scenario: DEPRECATED 实体可路由但优先级低于 ACTIVE

- **WHEN** 存在 phase=ACTIVE 和 phase=DEPRECATED 的 ModelInstance 同时可用
- **THEN** InstanceSelector 必须优先选择 ACTIVE 的实例

### Requirement: 状态转换规则


所有状态转换 SHALL 通过 canTransitionTo() 校验，非法转换必须拒绝。

- PENDING → ACTIVE（管理员激活）
- ACTIVE → SUSPENDED（管理员暂停）
- ACTIVE → DEPRECATED（上游标记下线）
- SUSPENDED → ACTIVE（管理员恢复）
- SUSPENDED → DEPRECATED（上游标记下线）
- DEPRECATED → RETIRED（废弃）

#### Scenario: 合法转换通过校验

- **WHEN** 调用 canTransitionTo() 检查合法转换（如 PENDING → ACTIVE）
- **THEN** 返回 true

#### Scenario: 非法转换被拒绝

- **WHEN** 调用 canTransitionTo() 检查非法转换（如 PENDING → RETIRED）
- **THEN** 返回 false

#### Scenario: RETIRED 是终端状态

- **WHEN** 实体 phase 为 RETIRED
- **THEN** isTerminal() 返回 true，canTransitionTo(任何值) 返回 false

### Requirement: 路由可见性判断


实体的路由可见性 SHALL 由 isRoutable() 决定。

- ACTIVE：可路由
- DEPRECATED：可路由（优先级低于 ACTIVE）
- PENDING、SUSPENDED、RETIRED：不可路由

#### Scenario: 路由过滤

- **WHEN** InstanceSelector 或 ChannelGateway 查询可用实体
- **THEN** 只返回 phase.isRoutable() 为 true 的实体

### Requirement: Model 废弃信息


Model 实体 SHALL 支持记录上游废弃信息，不依赖状态枚举。

- deprecatedAt：上游标记废弃的时间，null 表示正常
- scheduledRetiredAt：计划下线日期
- deprecationMessage：下线原因或建议迁移目标

#### Scenario: Model 废弃信息不影响路由

- **WHEN** Model 的 deprecatedAt 不为 null
- **THEN** 其参与路由的能力不受影响（由 ModelInstance.phase 决定）

#### Scenario: 管理界面展示废弃标记

- **WHEN** Model 的 deprecatedAt 不为 null
- **THEN** 管理界面应展示黄色警告标识和 deprecationMessage
