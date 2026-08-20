# protocol-plugin Specification

## Purpose
TBD - created by archiving change modularization-restructure. Update Purpose after archive.
## Requirements
### Requirement: 抽象协议层 gateway-protocol
项目 SHALL 提供抽象协议层模块 `gateway-protocol`，包含 Canonical IR 规范模型（CanonicalChatRequest/Message/Tool/ToolCall/Response/ContentBlock/Usage）、`ProtocolAdapter` SPI、协议契约 DTO 与 `ProtocolConversionFacade`。`gateway-protocol` SHALL 仅包含 SPI 与规范模型，SHALL NOT 依赖任何具体协议实现模块。

#### Scenario: 抽象层不依赖具体实现
- **WHEN** 检查 `gateway-protocol` 的依赖
- **THEN** 其不依赖任何 `gateway-protocol-*` 具体实现模块

#### Scenario: SPI 可被任意协议实现
- **WHEN** 任一协议实现模块需要接入
- **THEN** 其仅需实现 `ProtocolAdapter` SPI 并依赖 `gateway-protocol`

### Requirement: ProtocolConversionFacade 按 SPI 装配
`ProtocolConversionFacade` SHALL 通过 Spring 注入 `List<ProtocolAdapter>` 收集所有已注册的 Adapter Bean 并按协议名编排，SHALL NOT import 或构造注入任何具体协议实现的类。

#### Scenario: 转换门面不依赖具体实现类
- **WHEN** 检查 `ProtocolConversionFacade` 源码
- **THEN** 其通过注入的 Adapter Bean 列表完成 normalize/denormalize，无对具体 Adapter 类的直接依赖

### Requirement: 协议实现插件模块
每种协议 SHALL 由独立的能力插件模块实现，如 `gateway-protocol-openai`、`gateway-protocol-anthropic`。每个插件模块 SHALL 提供 `AutoConfiguration`（通过 `AutoConfiguration.imports` 注册）装配其 `ProtocolAdapter` Bean，并通过 `@ConditionalOnProperty` 控制启用。

#### Scenario: 插件通过 AutoConfiguration 注册
- **WHEN** 应用依赖某个 `gateway-protocol-*` 插件
- **THEN** 该插件的 Adapter Bean 通过其 AutoConfiguration 自动注册到 Spring 容器

#### Scenario: 插件可配置启停
- **WHEN** 通过配置关闭某协议的启用开关（如 `gateway.protocol.openai.enabled=false`）
- **THEN** 该协议插件不装配，网关不处理该协议

### Requirement: 新增协议不改核心
新增一种协议 SHALL 只需新增一个 `gateway-protocol-*` 插件模块（含 Adapter + AutoConfiguration）+ DB 配置，SHALL NOT 修改任何核心模块代码。

#### Scenario: 新增协议零核心改动
- **WHEN** 新增一种协议（如 Gemini）
- **THEN** 仅新增对应插件模块与配置即可支持，`gateway-protocol`/`gateway-proxy` 等核心模块无源码改动

### Requirement: 能力注册表与能力感知路由
项目 SHALL 提供能力注册表（`CapabilityRegistry`），按协议/能力类型注册能力；路由决策 SHALL 利用 `Model.capabilities` 字段参与模型选择与降级判断（该字段从"仅存储"变为"参与决策"）。

#### Scenario: 能力参与路由决策
- **WHEN** 路由解析器选择模型/通道
- **THEN** 其依据 `Model.capabilities` 与请求所需能力进行匹配与降级

### Requirement: 插件健康机制
协议能力插件 SHALL 提供健康检查（HealthIndicator/HealthGroup）与周期探活能力，使插件启用/禁用状态与上游可用性可观测。

#### Scenario: 插件健康可观测
- **WHEN** 运维查询插件健康状态
- **THEN** 能观测到各协议插件的启用状态与上游可用性

