# modular-architecture Specification

## Purpose
TBD - created by archiving change modularization-restructure. Update Purpose after archive.
## Requirements
### Requirement: 业务域模块划分
后端运行时 SHALL 按业务域拆分为独立 Maven 模块，包括 `gateway-common`（横切底座）、`gateway-protocol`（协议域）、`gateway-provider`（供给域）、`gateway-iam`（身份域）、`gateway-security`（安全域）、`gateway-usage`（用量域）、`gateway-stats`（报表域）、`gateway-resilience`（韧性域）、`gateway-audit`（审计域）、`gateway-alert`（告警域）、`gateway-experience`（体验域）、`gateway-proxy`（派发域）。每个业务域模块内部 SHALL 保持 application/domain/infrastructure 分层。

#### Scenario: 每个域模块可独立编译
- **WHEN** 对某个业务域模块单独执行构建
- **THEN** 该模块无需依赖其他业务域模块即可编译通过（仅依赖其下层底座或 common）

#### Scenario: 域模块内部保留三层
- **WHEN** 检查任一业务域模块的源码结构
- **THEN** 其包含 application/domain/infrastructure 三层 package，遵循依赖倒置与 Gateway 模式

### Requirement: 依赖方向单向分层
后端模块依赖方向 SHALL 满足单向分层：`common ← protocol/provider/iam/security ← usage/stats/resilience/audit/alert/experience ← proxy`，`gateway-boot` 仅依赖所有业务模块进行组装。任何模块 SHALL NOT 反向依赖 `gateway-boot`；中基/上层模块 SHALL NOT 被底座模块反向依赖。

#### Scenario: 底座不依赖上层
- **WHEN** 检查 `gateway-provider`/`gateway-protocol`/`gateway-iam`/`gateway-security` 的依赖
- **THEN** 它们不依赖 `gateway-proxy`/`gateway-usage` 等上层或中基模块

#### Scenario: 无人依赖组装模块
- **WHEN** 检查所有业务模块的依赖
- **THEN** 除 `gateway-boot` 自身外，无任何模块反向依赖 `gateway-boot`

### Requirement: gateway-boot 纯组装
`gateway-boot` SHALL 退化为纯组装/启动模块，只负责装配各业务模块与启动 Spring Boot 应用，SHALL NOT 包含业务逻辑实现。

#### Scenario: boot 不含业务实现
- **WHEN** 检查 `gateway-boot` 源码
- **THEN** 其不含领域/应用/基础设施层的业务逻辑实现，仅含启动类与组装配置

### Requirement: ArchUnit 依赖约束
项目 SHALL 提供 ArchUnit 架构测试，将依赖方向铁律固化为可执行校验，作为 CI/构建的一部分。

#### Scenario: 违反依赖方向被拦截
- **WHEN** 代码出现违反依赖方向的跨模块引用
- **THEN** ArchUnit 架构测试失败，阻止该提交/构建通过

### Requirement: 模块数量克制
后端业务域模块数量 SHALL 保持克制（约 8~12 个），弱内聚域（如 alert/experience/stats）在拆分时依据真实耦合度评估合并，避免过度模块化。

#### Scenario: 弱内聚域合并评估
- **WHEN** 拆分弱内聚业务域
- **THEN** 依据其实际被依赖面决定独立成模块或并入相邻域，避免模块数量失控

