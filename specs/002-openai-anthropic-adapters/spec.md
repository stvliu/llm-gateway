# Feature Specification: 实现OpenAI和Anthropic双适配器

**Feature Branch**: `002-openai-anthropic-adapters`
**Created**: 2026-04-23
**Status**: Draft
**Input**: User description: "实现OpenAI 适配器、Anthropic 适配器"

## Clarifications

### Session 2026-04-23

- Q: API Key 如何配置和传递给适配器？ → A: 数据库加密存储 + 服务启动时加载到内存
- Q: Function Calling / Tool Use 是否为必需功能？ → A: 完整支持，OpenAI 和 Anthropic 的 Function Calling 都必须实现
- Q: OpenAI 到 Anthropic 协议转换范围？ → A: 完整双向转换：OpenAI ↔ Anthropic，包括 chat/completions ↔ messages 格式转换
- Q: 错误处理策略？ → A: 根据请求来源适配：如果用 OpenAI 格式请求，返回 OpenAI 错误格式；Anthropic 同理
- Q: 测试与 Mock 策略？ → A: 单元测试使用 WireMock 模拟外部 API 响应，集成测试使用 Testcontainers

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 企业用户通过统一网关调用多种模型 (Priority: P1)

作为企业用户，我希望使用统一的 API 接口调用 OpenAI GPT-4 或 Anthropic Claude 等多种模型，这样我可以不必关心底层模型的差异。

**Why this priority**: 统一网关的核心价值在于提供一致的 API 体验，企业用户无需为每个模型提供商维护独立的集成代码。

**Independent Test**: 可以通过向网关发送 OpenAI 格式请求并指定 `openai/gpt-4o` 模型，验证响应是否符合 OpenAI 标准格式。

**Acceptance Scenarios**:

1. **Given** 用户已配置 OpenAI API Key，**When** 向 `/v1/chat/completions` 发送请求指定模型 `openai/gpt-4o`，**Then** 网关返回 OpenAI 标准格式响应，包含 id、model、choices、usage 字段
2. **Given** 用户已配置 Anthropic API Key，**When** 向 `/v1/messages` 发送请求指定模型 `anthropic/claude-opus-4-5`，**Then** 网关返回 Anthropic 标准格式响应，包含 id、model、content、usage 字段
3. **Given** 用户使用 OpenAI 格式请求 Anthropic 模型，**When** 网关进行协议转换后转发，**Then** 用户获得正确格式的响应，且响应内容与直接调用 Anthropic API 一致
4. **Given** 用户使用 Anthropic 格式请求 OpenAI 模型，**When** 网关进行协议转换后转发，**Then** 用户获得正确格式的响应

---

### User Story 2 - 模型提供商动态切换 (Priority: P2)

作为运维人员，我希望在模型提供商不可用时自动切换到备用提供商，这样我可以保证服务的连续性。

**Why this priority**: 生产环境中提供商故障是常见场景，备用切换机制确保业务不中断。

**Independent Test**: 可以通过模拟主提供商超时，验证请求自动切换到备用提供商并成功返回响应。

**Acceptance Scenarios**:

1. **Given** 主提供商 OpenAI 超时，**When** 配置了备用提供商，**Then** 网关自动切换到备用提供商完成请求
2. **Given** 主提供商返回速率限制错误，**When** 备用提供商可用，**Then** 网关自动切换到备用提供商

---

### User Story 3 - 流式响应实时返回 (Priority: P3)

作为前端开发者，我希望实时看到模型生成的回复片段，这样我可以实现打字机效果的交互体验。

**Why this priority**: 流式响应是现代 AI 应用的核心交互模式，提升用户体验。

**Independent Test**: 可以通过向流式端点发送请求，验证是否逐步收到 SSE 格式的响应片段。

**Acceptance Scenarios**:

1. **Given** 用户请求流式响应，**When** 模型开始生成，**Then** 网关实时推送 SSE 格式的 `data:` 行，每行包含增量内容
2. **Given** 流式响应进行中，**When** 用户取消请求，**Then** 网关立即停止接收并返回已生成的部分

---

### User Story 4 - 请求和响应的 Token 精确计量 (Priority: P2)

作为财务人员，我希望精确了解每个请求消耗的 Token 数量，这样我可以准确核算各团队的模型使用成本。

**Why this priority**: Token 透明是第五原则的核心，预算控制和成本分摊都依赖于准确的计量。

**Independent Test**: 可以通过发送已知内容的请求，验证响应的 usage 中包含准确的 prompt_tokens、completion_tokens 统计。

**Acceptance Scenarios**:

1. **Given** 用户发送消息请求，**Then** 响应中的 usage 字段包含准确的 prompt_tokens 和 completion_tokens
2. **Given** 请求包含 system prompt 和多轮对话，**When** 模型生成回复，**Then** prompt_tokens 准确反映输入总 token 数

---

### User Story 5 - Function Calling / Tool Use (Priority: P2)

作为开发者，我希望在调用 LLM 时指定工具（函数），这样我可以实现复杂的代理行为。

**Why this priority**: Function Calling 是现代 AI 应用的核心能力，支持代理和工具调用场景。

**Independent Test**: 可以通过发送包含 tools 参数的请求，验证适配器正确转发并处理工具调用响应。

**Acceptance Scenarios**:

1. **Given** OpenAI 请求包含 tools 参数，**When** 发送 chat 请求，**Then** 适配器正确转发到 OpenAI API 并返回 function_call 响应
2. **Given** Anthropic 请求包含 tools 参数，**When** 发送 messages 请求，**Then** 适配器正确转发到 Anthropic API 并返回 tool_use 响应

---

### User Story 6 - 跨提供商协议转换 (Priority: P3)

作为企业用户，我希望用 OpenAI 格式的请求调用 Anthropic 模型（或反之），这样我可以无痛切换模型提供商。

**Why this priority**: 支持双向协议转换是企业级网关的核心能力，方便用户迁移和混用模型。

**Independent Test**: 可以用 OpenAI 格式请求 Anthropic 模型，验证返回 OpenAI 格式响应。

**Acceptance Scenarios**:

1. **Given** 用户发送 OpenAI 格式请求指定 Anthropic 模型，**When** 网关自动转换请求格式并转发，**Then** 用户收到 OpenAI 格式响应
2. **Given** 用户发送 Anthropic 格式请求指定 OpenAI 模型，**When** 网关自动转换请求格式并转发，**Then** 用户收到 Anthropic 格式响应

---

### User Story 7 - 一致的错误格式体验 (Priority: P2)

作为开发者，我希望在出错时收到与我请求格式一致的错误响应，这样我可以统一处理错误。

**Why this priority**: 错误格式一致性让开发者更容易处理异常，提升开发体验。

**Independent Test**: 可以发送会导致错误的请求，验证错误响应格式与请求格式一致。

**Acceptance Scenarios**:

1. **Given** 用户发送 OpenAI 格式请求出错，**When** 网关返回错误，**Then** 错误格式符合 OpenAI 标准错误格式
2. **Given** 用户发送 Anthropic 格式请求出错，**When** 网关返回错误，**Then** 错误格式符合 Anthropic 标准错误格式

---

### Edge Cases

- 当模型名称格式错误时（如缺少 provider 前缀），系统返回友好错误提示
- 当 API Key 无效或权限不足时，适配器返回清晰的认证错误
- 当网络超时或中断时，支持配置重试次数和超时时间
- 当响应格式异常时（如缺少必需字段），适配器记录错误并返回降级响应
- 当提供商返回速率限制（429）时，系统按 Retry-After 头延迟后重试
- 当协议转换失败时，返回明确的错误信息，不做静默降级

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 网关必须同时支持 OpenAI `/v1/chat/completions` 和 Anthropic `/v1/messages` 两种标准端点
- **FR-002**: 适配器必须实现 `LLMProviderAdapter` 接口，包括 `chat()`, `chatStream()`, `messages()` 三个核心方法
- **FR-003**: OpenAI 适配器必须兼容 OpenAI API v1 标准，支持 chat 和 stream 模式
- **FR-004**: Anthropic 适配器必须兼容 Anthropic Messages API 2023-06-01 版本
- **FR-005**: 适配器必须使用 Spring MVC + RestClient（同步）或 OkHttp（流式）实现，支持高并发场景
- **FR-006**: 流式响应必须使用 SSE 格式（`data:` 前缀），并在流结束时发送 `data: [DONE]`
- **FR-007**: 所有适配器必须支持健康检查（`isHealthy()`）和可用性检查（`isAvailable()`）
- **FR-008**: 适配器必须返回标准的 `LLMResponse` 格式，包含 providerCode、id、model、content、usage 等字段
- **FR-009**: 请求和响应的转换必须准确，参数映射误差 ≤0.1%
- **FR-010**: 每个适配器必须支持可配置的超时时间，默认 30 秒
- **FR-011**: 适配器必须无状态，支持多线程并发调用
- **FR-012**: 适配器必须通过 RestClient（同步请求）或 OkHttp（流式SSE）进行 HTTP 通信，支持连接池配置
- **FR-013**: API Key 必须加密存储（AES-256），服务启动时从数据库加载到内存
- **FR-014**: OpenAI 适配器必须支持 Function Calling，包括 function_call 请求和响应处理
- **FR-015**: Anthropic 适配器必须支持 Tool Use，包括 tool_use 请求和响应处理
- **FR-016**: 网关必须支持 OpenAI ↔ Anthropic 完整双向协议转换，包括 chat/completions ↔ messages 格式互转
- **FR-017**: 错误响应格式必须根据请求来源适配：OpenAI 格式请求返回 OpenAI 错误格式，Anthropic 格式请求返回 Anthropic 错误格式

### Key Entities

- **LLMProviderAdapter**: 模型提供商适配器接口，定义 chat、chatStream、messages 能力
- **OpenAIAdapter**: OpenAI 兼容端点适配器，实现 OpenAI API v1 协议，支持 Function Calling
- **AnthropicAdapter**: Anthropic 消息 API 适配器，实现 Anthropic Messages API 协议，支持 Tool Use
- **ProtocolTranslator**: 协议转换器，负责 OpenAI ↔ Anthropic 格式双向转换
- **LLMRequest**: 统一的 LLM 请求 DTO，包含模型、消息、参数配置、tools 定义
- **LLMResponse**: 统一的 LLM 响应 DTO，包含内容、Token 使用、结束原因、function_call/tool_use
- **ProviderCredentials**: 提供商凭证实体，存储加密的 API Key 和配置信息
- **ToolDefinition**: 工具定义，包含 name、description、parameters

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: OpenAI 适配器对 `/v1/chat/completions` 请求的响应格式 100% 符合 OpenAI API v1 标准
- **SC-002**: Anthropic 适配器对 `/v1/messages` 请求的响应格式 100% 符合 Anthropic Messages API 标准
- **SC-003**: 协议转换准确率 ≥99.9%（OpenAI ↔ Anthropic 格式互转）
- **SC-004**: 非流式请求平均延迟 <200ms（P50），<500ms（P95）
- **SC-005**: 流式响应首字节延迟 <300ms（P95）
- **SC-006**: 适配器支持 10,000 QPS 单实例吞吐量
- **SC-007**: 所有适配器实现 `isHealthy()` 和 `isAvailable()` 方法，支持运行时健康检查
- **SC-008**: 每个适配器提供 `ProviderCapabilities` 描述支持的模型和能力
- **SC-009**: 单元测试覆盖率 ≥80%，核心路径 100% 覆盖
- **SC-010**: 适配器通过 WebClient 配置，支持连接池、超时、重试等可观测性指标
- **SC-011**: API Key 在数据库中加密存储（AES-256），服务启动时安全加载
- **SC-012**: OpenAI Function Calling 响应格式 100% 符合 OpenAI 标准
- **SC-013**: Anthropic Tool Use 响应格式 100% 符合 Anthropic 标准
- **SC-014**: 双向协议转换（OpenAI ↔ Anthropic）测试用例 100% 通过
- **SC-015**: 错误响应格式与请求来源格式一致，测试用例 100% 通过

## Assumptions

- 模型提供商 API 端点稳定，假设 OpenAI API 端点为 `https://api.openai.com`，Anthropic 端点为 `https://api.anthropic.com`
- API Key 通过数据库加密存储，服务启动时加载到内存（不依赖环境变量）
- 企业用户使用标准模型名称（如 `openai/gpt-4o`、`anthropic/claude-opus-4-5`）
- 传输层使用 HTTPS/TLS 1.3 加密
- 响应式编程使用 Project Reactor（Flux/Mono）
- 请求参数验证在网关入口完成，适配器接收已验证的请求
- Function Calling / Tool Use 第一版仅支持请求转发，不实现本地工具执行
- 协议转换在网关调度层完成，适配器保持单一协议接口
- 错误格式适配由网关统一处理，适配器仅返回标准 LLMResponse
- 单元测试使用 WireMock 模拟外部 API，集成测试使用 Testcontainers
