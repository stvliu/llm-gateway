# 模块化重构（Modularization Restructure）

## Why

`gateway-boot` 单 jar 承载全部业务（399 个 main 文件、11 个领域、19 个 application 子包），COLA 的"包级分层"在规模增长后已逼近拆分临界点；能力插件化仅完成第 1 阶段（Canonical IR + ProtocolAdapter SPI），真正意义的插件化（独立能力模块、能力注册表、能力感知路由）尚未落地；且 `gateway-capability-*` 命名与 LLM"模型能力"（modality/capability）语义混淆。本 change 借鉴 Jmix 的模块化哲学，把"架构层边界"从 package 提升到 Maven 模块，并引入"抽象协议层 / 具体协议实现"对称命名。

## What Changes

- 把 `gateway-boot` 拆解为业务域 Maven 模块：`gateway-common`（横切底座）、`gateway-protocol`（协议域）、`gateway-provider`（供给域）、`gateway-iam`（身份域）、`gateway-security`（安全域）、`gateway-usage`（用量写）、`gateway-stats`（报表读）、`gateway-resilience`（韧性域）、`gateway-audit`（审计域）、`gateway-alert`（告警域）、`gateway-experience`（体验域）、`gateway-proxy`（派发域）。
- **BREAKING**：`gateway-capability-api` 重命名为 `gateway-protocol`（模块 artifactId 与包名 `com.codingas.gateway.api.capability.protocol` → `com.codingas.gateway.protocol` 迁移）。
- 新增协议能力插件模块 `gateway-protocol-openai` / `gateway-protocol-anthropic`（实现 `ProtocolAdapter` SPI + `AutoConfiguration`，`@ConditionalOnProperty` 控制启用）。
- `gateway-boot` 退化为纯组装/启动模块，不再含业务逻辑。
- 引入 `CapabilityRegistry` 能力注册表 + 能力感知路由（打通 `Model.capabilities` 参与决策）。
- 引入 ArchUnit 架构测试，把依赖方向固化为可执行约束。
- **行为不变**：本 change 是结构性重构，存量运行时行为保持不变（协议转换语义、路由、配额等均不改变）。

## Capabilities

### New Capabilities

- `modular-architecture`: 模块化体系——业务域模块划分、依赖方向（底座/中基/上层单向分层）、ArchUnit 依赖约束、`gateway-boot` 纯组装。
- `protocol-plugin`: 协议能力插件化——`gateway-protocol` 抽象协议层（Canonical IR + `ProtocolAdapter` SPI + Facade 按 SPI 装配）、`gateway-protocol-*` 具体实现插件（AutoConfiguration 注册、能力注册表、能力感知路由）。

### Modified Capabilities

（无——本 change 为结构性重构，不改变任何既有 spec 的业务行为要求。）

## Impact

- **构建**：根 `pom.xml` 的 `<modules>`、各模块 `pom.xml`、`gateway-boot/pom.xml` 依赖。
- **代码**：`gateway-boot` 内大量类迁移到新模块；`gateway-capability-api` 重命名（artifactId + 包名）。
- **依赖**：新增模块间依赖关系（`proxy → usage/stats/resilience/audit → provider/iam/protocol/security → common`；`protocol-openai/anthropic → gateway-protocol`）。
- **测试**：新增 ArchUnit 架构测试；既有单测/集成测试迁移到对应模块；覆盖率门槛（核心 ≥90% / 规则引擎 ≥85% / 适配器 ≥80%）不因迁移跌破。
- **前端/CLI/模拟器**：`gateway-console`、`gateway-cli`、`gateway-simulator` 不受影响（非后端业务域）。
- **配置**：能力插件通过 `@ConditionalOnProperty`（如 `gateway.protocol.openai.enabled`）控制启用，厂商数据仍全进 DB。
