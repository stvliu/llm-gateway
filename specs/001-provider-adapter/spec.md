# Feature Specification: Provider Adapter Framework

**Feature Branch**: `001-provider-adapter`
**Created**: 2026-04-23
**Status**: In Progress
**Input**: 创建 Provider 适配器框架，用于 LLM-Gateway 连接 OpenAI、Anthropic 等模型提供商。需定义 LLMProviderAdapter 接口、Provider/Model/ProviderApiKey/GatewayApiKey 实体、实现开闭原则（对扩展开放、对修改关闭）。技术栈：Java 21 + Spring Boot 3.5.x

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 适配器框架抽象 (Priority: P1)

网关开发者需要一套标准接口，以便在不影响现有代码的情况下接入新的模型提供商（如未来新增 Google Gemini、Cohere 等）。

**Why this priority**: 这是整个网关的基础，所有后续功能（路由、计量、监控）都依赖适配器层。没有标准接口就无法实现双 API 兼容和开闭原则。

**Independent Test**: 可以通过编写单元测试验证新增 Provider 实现接口后能被网关识别和使用，无需启动完整网关。

**Acceptance Scenarios**:

1. **Given** 网关系统运行中，**When** 管理员新增一个 Provider 配置（OpenAI），**Then** 系统自动发现并注册该 Provider 的适配器
2. **Given** 已实现 OpenAI 适配器，**When** 接入 Anthropic 适配器（实现相同接口），**Then** 网关无需修改现有代码即可同时支持两种 Provider
3. **Given** 适配器接口定义完成，**When** 第三方开发者实现一个新 Provider 适配器，**Then** 网关能自动识别并使用

---

### User Story 2 - Provider 管理 (Priority: P2)

管理员需要通过管理界面配置模型提供商，包括名称、API 端点、优先级等。

**Why this priority**: 配置管理是运维的基础功能，没有它就无法动态管理 Provider。

**Independent Test**: 可以通过 CRUD 测试验证管理员可以创建/读取/更新/删除 Provider 配置。

**Acceptance Scenarios**:

1. **Given** 管理员登录系统，**When** 创建新 Provider（名称、API Endpoint、类型），**Then** Provider 被保存并可被路由引擎使用
2. **Given** 已存在 Provider，**When** 管理员编辑其配置或调整优先级，**Then** 变更立即生效（热加载）
3. **Given** 已存在 Provider，**When** 管理员删除它，**Then** 该 Provider 被标记为 DELETED 状态

---

### User Story 3 - 模型关联 (Priority: P2)

系统需要维护 Provider 与其提供的模型之间的关联关系，支持模型映射。

**Why this priority**: 管理员需要知道每个 Provider 下有哪些可用模型，以便配置路由策略。

**Independent Test**: 可以通过测试验证 Provider 创建后可以关联多个 Model，且模型列表查询正确。

**Acceptance Scenarios**:

1. **Given** Provider 已创建，**When** 管理员关联多个 Model（如 gpt-4o、gpt-4o-mini），**Then** 这些模型被记录并可被路由引擎识别
2. **Given** 存在 Provider 和 Model，**When** 配置模型映射规则（如 user-model-1 → provider-model-a），**Then** 映射规则被保存并在路由时生效

---

### User Story 4 - 用户 API Key 管理 (Priority: P3)

用户需要管理自己的 API Key，支持多 Provider、多 Key 配置实现密钥轮换。

**Why this priority**: 密钥轮换是高可用策略的关键，确保单一 Key 失效不影响服务。

**Independent Test**: 可以通过测试验证多 Key 场景下的自动轮换和故障转移。

**Acceptance Scenarios**:

1. **Given** 用户已认证，**When** 为某 Provider 创建多个 API Key，**Then** 系统按优先级选择 Key
2. **Given** 用户有多个 API Key，**When** 当前使用中的 Key 达到速率限制或失败，**Then** 系统自动切换到下一个 Key

---

## Edge Cases

- 当 Provider 的 API 返回非标准格式时，适配器应抛出明确的 ProviderException 而非静默失败
- API Key 过期或无效时，适配器应返回认证错误并标记该 ProviderApiKey 为不可用
- 网络超时后适配器应支持重试（可配置重试次数和间隔）
- Provider 全部不可用时，路由引擎应返回 503 Service Unavailable

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 提供 `LLMProviderAdapter` 接口，定义标准方法：`chat()`, `chatStream()`, `messages()`, `getCapabilities()`
- **FR-002**: 系统 MUST 支持 Provider 注册发现机制，通过 SPI（Service Provider Interface）自动发现并加载适配器实现
- **FR-003**: 系统 MUST 支持 Provider 类型的扩展，新增 Provider 无需修改现有代码（开闭原则）
- **FR-004**: 系统 MUST 提供 Provider 实体，包含：code、name、type（OPENAI/ANTHROPIC/OTHER）、base_url、priority、status
- **FR-005**: 系统 MUST 提供 Model 实体，包含：code、provider_id、model_id（如 gpt-4o）、display_name、context_window、input_price、output_price
- **FR-006**: 系统 MUST 提供两层 API Key 体系：
  - **ProviderApiKey**（Provider 调用凭证）：网关调用大模型 Provider 的凭据，管理员配置，加密存储，支持多 Key 轮换
  - **GatewayApiKey**（网关访问凭证）：用户调用 LLM-Gateway 网关的凭据，用户自管理，哈希存储，支持白名单控制
- **FR-007**: 系统 MUST 支持 ProviderApiKey 多 Key 配置，实现 Key 的自动轮换和故障转移
- **FR-008**: 系统 MUST 支持 Provider/Model 的热加载，配置变更不中断进行中请求
- **FR-009**: 系统 MUST 提供 Provider 能力查询接口，返回该 Provider 支持的功能（chat/messages/embeddings/streaming）

### Key Entities

- **Provider**: 模型提供商，如 OpenAI、Anthropic。包含连接配置（base_url、timeout）和凭证信息。
- **Model**: 具体模型，如 gpt-4o、claude-sonnet-4-20250514。关联到 Provider，包含模型元数据。
- **ProviderApiKey**: 网关调用大模型 Provider 的凭据，属于系统维度，支持加密存储和优先级管理。
- **GatewayApiKey**: 用户调用 LLM-Gateway 网关的凭据，属于用户维度，支持哈希存储和 IP/模型白名单。
- **User**: 最终用户，拥有 GatewayApiKey 集合。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 新增 Provider 适配器时间 ≤2 小时（不修改框架代码）
- **SC-002**: 适配器接口方法调用准确率 ≥99.9%（参数转换不丢失信息）
- **SC-003**: Provider 配置变更后热加载延迟 ≤100ms，不影响进行中请求
  - **测量方法**: 在无进行中请求时，修改 Provider 配置并记录 EnvironmentChangeEvent 到配置生效的时间，重复 10 次取平均值
- **SC-004**: ProviderApiKey 多 Key 场景下，故障转移时间 ≤500ms
  - **测量方法**: 在 Key 标记为 unhealthy 后，记录到选择下一个可用 Key 的时间，重复 10 次取平均值
- **SC-005**: 系统支持至少 10 种不同 Provider 类型而不出现架构腐化

## Assumptions

- Provider API 遵循 REST + JSON 规范（HTTP POST/GET）
- 使用 Java SPI 机制实现适配器发现（`META-INF/services`）
- 加密存储使用 AES-256，具体实现由 security 模块提供
- Provider 配置信息存储在数据库，支持 CRUD 操作
- 模型列表预填充（数据库初始化时导入 50+ 主流模型配置）
