# Comet Design Handoff

- Change: llm-provider-simulator
- Phase: design
- Mode: compact
- Context hash: 65fd9d0a50ff13df946a84b4a34dbdda8bd600dfd4058cbe3cf63b6a6cf7fd3d

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/llm-provider-simulator/proposal.md

- Source: openspec/changes/llm-provider-simulator/proposal.md
- Lines: 1-27
- SHA256: 97ffcb6dc7b8578131a279fbb7d6c63432cec99322f92cfd5a23714de5cda3fd

```md
## Why

OpenAIUpstreamClient 和 AnthropicUpstreamClient 是 Gateway 与外部 LLM API 通信的核心组件，但目前零单元测试覆盖——所有现有测试通过 Mockito mock `UpstreamClient` 接口跳过了 HTTP 请求构造、JSON 序列化/反序列化、SSE 流解析等关键逻辑。同时，集成测试和开发调试依赖真实 API Key 和网络连接，限流、鉴权失败、超时等异常场景难以可靠复现。需要一个可控、可预测、零外部依赖的模拟测试工具。

## What Changes

- 新增测试工具包（`ProviderSimulator`、`ResponseTemplates`、`ProviderSimulatorConfig`），封装 OkHttp MockWebServer，为 `OpenAIUpstreamClient` 和 `AnthropicUpstreamClient` 提供完整的 HTTP 层单元测试覆盖
- 新增 `OpenAIUpstreamClientTest` 和 `AnthropicUpstreamClientTest`，覆盖正常非流式/流式调用、限流（429）、鉴权错误（401）、服务端错误（500）、超时、SSE 流中断等场景
- 新增 `gateway-simulator` 独立 Maven 模块，提供可独立运行的模拟 HTTP 服务，支持通过管理 API 切换行为模式（正常/限流/故障）和查看请求记录
- `gateway-simulator` 复用第一阶段的响应模板，Gateway 只需将 Provider endpoint 指向模拟器即可使用

## Capabilities

### New Capabilities
- `provider-simulator`: 上游大模型服务模拟器，包括测试工具包（基于 MockWebServer 的 ProviderSimulator/ResponseTemplates/ProviderSimulatorConfig）和独立运行服务（gateway-simulator 模块），提供 OpenAI/Anthropic 双协议的 HTTP 端点模拟、响应模板、错误注入和 SSE 流式模拟

### Modified Capabilities

（无现有 spec 需要修改）

## Impact

- **新增测试文件**：`gateway-boot/src/test/java/.../support/ProviderSimulator.java`、`ResponseTemplates.java`、`ProviderSimulatorConfig.java`、`OpenAIUpstreamClientTest.java`、`AnthropicUpstreamClientTest.java`
- **新增 Maven 模块**：`gateway-simulator/`，父 POM 为 `gateway-project`，依赖 `spring-boot-starter-web` 和项目内的响应模板
- **父 POM 修改**：`gateway/pom.xml` 的 `<modules>` 增加 `gateway-simulator`
- **零生产代码修改**：第一阶段纯测试代码，第二阶段为独立模块
- **无破坏性变更**
```

## openspec/changes/llm-provider-simulator/design.md

- Source: openspec/changes/llm-provider-simulator/design.md
- Lines: 1-87
- SHA256: 267d197dbd44e7dafd71a3ea404db5bb692c9e61130e7c592a7211535d481e77

[TRUNCATED]

```md
## Context

LLM-Gateway 通过 `UpstreamClient` 接口与外部 LLM API 通信，当前有两个实现：`OpenAIUpstreamClient`（调用 `/v1/chat/completions`）和 `AnthropicUpstreamClient`（调用 `/v1/messages`）。两个客户端构造函数参数相同：`OkHttpClient`、`endpointUrl`、`apiKey`、`timeoutSeconds`、`ObjectMapper`、`ErrorClassificationStrategy`。

**当前测试现状**：所有上游客户端测试通过 Mockito mock `UpstreamClient` 接口，从未验证过真实 HTTP 请求/响应。`OpenAIUpstreamClient`（175 行）和 `AnthropicUpstreamClient`（189 行）的 HTTP 序列化、SSE 流解析、错误分类路径测试覆盖率为零。

**已有依赖**：`com.squareup.okhttp3:mockwebserver`（v4.12.0）已在 `gateway-boot/pom.xml` 中声明为 test scope。

## Goals / Non-Goals

**Goals:**
- 为 `OpenAIUpstreamClient` 和 `AnthropicUpstreamClient` 提供完整的 HTTP 层单元测试，覆盖正常调用、流式调用、错误分类、超时、连通性测试等场景
- 提供可复用的测试工具包（`ProviderSimulator`、`ResponseTemplates`），方便未来新增 Provider 适配器时快速编写测试
- 提供独立运行的 `gateway-simulator` Maven 模块，开发者无需真实 API Key 即可通过模拟端点验证 Gateway 集成

**Non-Goals:**
- 不模拟真实模型推理（只返回预置 JSON 响应）
- 不替换现有 Mockito 测试模式（补充而非替代）
- 不引入 WireMock 等新依赖
- 不修改任何生产代码
- 不做性能/负载测试模拟

## Decisions

### D1: 使用 MockWebServer 而非 WireMock

**选择**：OkHttp MockWebServer（已存在于项目依赖）

**替代方案**：WireMock — 功能更丰富（请求匹配 DSL、录制/回放、独立运行模式），但引入 2MB+ 新依赖

**理由**：
- 项目已引入 mockwebserver，零新增依赖
- MockWebServer 与项目使用的 OkHttp 客户端同生态，API 自然匹配
- 我们的模拟需求（固定响应、错误注入、SSE 流）MockWebServer 完全满足
- 第二阶段独立服务基于 Spring Boot 自建 Controller，不需要 WireMock 的独立运行模式

### D2: 测试工具包放在 src/test/java 下

**选择**：`gateway-boot/src/test/java/com/codingas/gateway/support/`

**替代方案**：独立 `gateway-test` Maven 模块

**理由**：
- 测试工具仅供 gateway-boot 内部测试使用，不跨模块共享
- 避免为少量工具类引入新 Maven 模块的复杂性
- 与项目现有测试组织方式一致

### D3: 响应模板硬编码为 Java 文本块

**选择**：在 `ResponseTemplates` 类中使用 Java 17 text block 硬编码 JSON 模板

**替代方案**：外部 JSON 资源文件（`src/test/resources/`）

**理由**：
- JSON 模板结构简单且稳定（OpenAI/Anthropic API 格式极少变动）
- 硬编码更易于 IDE 内联查看和调试，不需要跳转文件
- 文本块语法清晰，不引入文件 I/O 开销

### D4: 第二阶段 gateway-simulator 作为独立 Maven 模块

**选择**：新建 `gateway-simulator/` 模块，父 POM 为 `gateway-project`

**替代方案**：在 gateway-boot 中通过 Spring Profile 启用模拟模式

**理由**：
- 模拟器与网关核心职责完全不同，独立模块符合单一职责
- 模拟器可独立启动/停止，不影响 Gateway 主进程
- 模拟器只依赖 Spring Boot Web + 项目内的响应模板，不依赖 Gateway 的 JPA/Redis/Security 等重型依赖
- 开发者可以同时运行 Gateway + Simulator，将 Provider endpoint 指向模拟器

### D5: 第二阶段模拟器复用第一阶段的 ResponseTemplates

**选择**：`gateway-simulator` 通过 Maven 依赖引用 `gateway-boot` 的测试 jar，或直接复制核心模板类

**理由**：
- 响应模板逻辑简单（纯静态 JSON 字符串），复制代价极低
- 避免 `gateway-boot` 测试代码作为 jar 被 `gateway-simulator` 依赖（test scope 不会传递）
- 模拟器需要独立打包运行，不能依赖测试 jar

## Risks / Trade-offs
```

Full source: openspec/changes/llm-provider-simulator/design.md

## openspec/changes/llm-provider-simulator/tasks.md

- Source: openspec/changes/llm-provider-simulator/tasks.md
- Lines: 1-51
- SHA256: 40bfb138f1f4a7072b615021b8864dc92e5e47896189ab55a4f49cce40908315

```md
# Tasks: llm-provider-simulator

## 第一阶段：测试工具包

- [ ] T1: 创建 `ResponseTemplates` 响应模板工厂
  - 路径：`gateway-boot/src/test/java/com/codingas/gateway/support/ResponseTemplates.java`
  - 内容：OpenAI 非流式/流式/错误模板 + Anthropic 非流式/流式/错误模板

- [ ] T2: 创建 `ProviderSimulator` MockWebServer 封装
  - 路径：`gateway-boot/src/test/java/com/codingas/gateway/support/ProviderSimulator.java`
  - 内容：start/close、enqueueSuccess/enqueueError/enqueueStream、takeRequest

- [ ] T3: 创建 `OpenAIUpstreamClientTest` 测试类
  - 路径：`gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/OpenAIUpstreamClientTest.java`
  - 覆盖：非流式调用（请求验证+响应反序列化）、流式调用（SSE 解析+DONE 标记）、429/401/500 错误分类、超时、连通性测试

- [ ] T4: 创建 `AnthropicUpstreamClientTest` 测试类
  - 路径：`gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/AnthropicUpstreamClientTest.java`
  - 覆盖：非流式调用（请求验证+响应反序列化）、流式调用（SSE 解析+message_stop 标记）、429/401/500 错误分类、超时、连通性测试

- [ ] T5: 运行全部测试验证通过
  - 执行：`./mvnw test -pl gateway-boot -Dtest="*OpenAIUpstreamClientTest,*AnthropicUpstreamClientTest"`

## 第二阶段：独立运行模拟服务

- [ ] T6: 创建 `gateway-simulator` Maven 模块骨架
  - 路径：`gateway-simulator/pom.xml`、父 POM 更新
  - 依赖：spring-boot-starter-web

- [ ] T7: 实现模拟端点 Controller
  - 路径：`gateway-simulator/src/main/java/.../controller/SimulatorController.java`
  - 端点：POST `/v1/chat/completions`、POST `/v1/messages`（含流式支持）

- [ ] T8: 实现响应模板和服务层
  - 路径：`gateway-simulator/src/main/java/.../template/`、`.../service/`
  - 内容：SimulatorResponseTemplates（复用第一阶段模板逻辑）、SimulatorModeService（模式管理）

- [ ] T9: 实现管理 API Controller
  - 路径：`gateway-simulator/src/main/java/.../controller/SimulatorAdminController.java`
  - 端点：切换模式（正常/限流/故障）、查看请求记录

- [ ] T10: 创建 Spring Boot 启动类和配置
  - 路径：`gateway-simulator/src/main/java/.../LLMProviderSimulatorApplication.java`
  - 配置：`application.yml`（端口、默认模式、请求记录容量）

- [ ] T11: 编写 gateway-simulator 集成测试
  - 路径：`gateway-simulator/src/test/java/.../`
  - 覆盖：正常/限流/故障模式切换、流式端点、管理 API

- [ ] T12: 全量回归测试
  - 执行：`./mvnw clean test` 确保所有模块测试通过
```

## openspec/changes/llm-provider-simulator/specs/provider-simulator/spec.md

- Source: openspec/changes/llm-provider-simulator/specs/provider-simulator/spec.md
- Lines: 1-165
- SHA256: 343427c38975538c1914d6629581afc5722ad5204b043080edd53386a326365e

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: ProviderSimulator 封装 MockWebServer

系统 SHALL 提供 `ProviderSimulator` 测试工具类，封装 OkHttp MockWebServer，支持启动/停止模拟服务器、入队预配置响应、录制并验证请求。

#### Scenario: 启动并获取模拟服务器 URL
- **WHEN** 调用 `ProviderSimulator.start()` 启动模拟服务器
- **THEN** 模拟服务器在随机可用端口启动，`getUrl()` 返回可访问的 base URL

#### Scenario: 入队成功响应
- **WHEN** 调用 `simulator.enqueueOpenAISuccess()` 入队 OpenAI 格式响应
- **THEN** 下一个到达模拟服务器的请求返回 HTTP 200 + 标准 OpenAI Chat Completion JSON

#### Scenario: 入队错误响应
- **WHEN** 调用 `simulator.enqueueError(429)` 入队限流响应
- **THEN** 下一个到达模拟服务器的请求返回 HTTP 429 + 限流错误 JSON

#### Scenario: 验证请求内容
- **WHEN** 客户端完成请求后调用 `simulator.takeRequest()`
- **THEN** 返回 `RecordedRequest` 对象，可验证 HTTP 方法、路径、Header、Body

#### Scenario: 关闭模拟服务器
- **WHEN** 调用 `ProviderSimulator.close()`
- **THEN** 模拟服务器停止并释放端口

### Requirement: ResponseTemplates 提供预制响应模板

系统 SHALL 提供 `ResponseTemplates` 工具类，包含 OpenAI 和 Anthropic 协议的预制 JSON 响应模板，覆盖非流式响应、流式 SSE 片段、错误响应。

#### Scenario: OpenAI 非流式响应模板
- **WHEN** 调用 `ResponseTemplates.openaiChatCompletion()`
- **THEN** 返回符合 OpenAI Chat Completions API 格式的 JSON 字符串，包含 id、model、choices、usage 字段

#### Scenario: OpenAI 流式 SSE 片段模板
- **WHEN** 调用 `ResponseTemplates.openaiStreamChunks()`
- **THEN** 返回包含多个 `data:` 行和 `data: [DONE]` 终止标记的 SSE 格式字符串

#### Scenario: Anthropic 非流式响应模板
- **WHEN** 调用 `ResponseTemplates.anthropicMessages()`
- **THEN** 返回符合 Anthropic Messages API 格式的 JSON 字符串，包含 id、model、content、usage 字段

#### Scenario: Anthropic 流式 SSE 片段模板
- **WHEN** 调用 `ResponseTemplates.anthropicStreamChunks()`
- **THEN** 返回包含 `event:` + `data:` 行和 `message_stop` 事件终止标记的 SSE 格式字符串

#### Scenario: 错误响应模板
- **WHEN** 调用 `ResponseTemplates.openaiError()` 或 `ResponseTemplates.anthropicError()`
- **THEN** 返回对应协议格式的错误 JSON 字符串

### Requirement: OpenAIUpstreamClient HTTP 层测试覆盖

系统 SHALL 提供 `OpenAIUpstreamClientTest` 测试类，基于 ProviderSimulator 验证 OpenAIUpstreamClient 的 HTTP 请求构造、响应反序列化、错误分类等行为。

#### Scenario: 非流式调用发送正确的 HTTP 请求
- **WHEN** 调用 `client.chat(request)` 且模拟器返回成功响应
- **THEN** 请求路径为 `/v1/chat/completions`，Header 包含 `Authorization: Bearer {apiKey}` 和 `Content-Type: application/json`，Body 包含模型名称

#### Scenario: 非流式调用正确反序列化响应
- **WHEN** 模拟器返回标准 OpenAI Chat Completion JSON
- **THEN** 返回的 `ProtocolResponse` 类型为 `OpenAIChatResponse`，model、choices、usage 字段正确填充

#### Scenario: 流式调用正确解析 SSE 事件流
- **WHEN** 模拟器返回 SSE 格式的流式响应（含多个 `data:` 行和 `data: [DONE]` 终止标记）
- **THEN** `StreamCallback.onChunk()` 被每个有效数据块调用，`onComplete()` 在收到 `[DONE]` 后调用

#### Scenario: 限流错误映射为 RATE_LIMIT_ERROR
- **WHEN** 模拟器返回 HTTP 429
- **THEN** 抛出 `ProviderException`，`errorType` 为 `RATE_LIMIT_ERROR`

#### Scenario: 鉴权错误映射为 AUTHENTICATION_ERROR
- **WHEN** 模拟器返回 HTTP 401
- **THEN** 抛出 `ProviderException`，`errorType` 为 `AUTHENTICATION_ERROR`

#### Scenario: 服务端错误映射为 UPSTREAM_ERROR
- **WHEN** 模拟器返回 HTTP 500
- **THEN** 抛出 `ProviderException`，`errorType` 为 `UPSTREAM_ERROR`

#### Scenario: 超时映射为 TIMEOUT_ERROR
- **WHEN** 模拟器设置响应延迟超过客户端超时时间
```

Full source: openspec/changes/llm-provider-simulator/specs/provider-simulator/spec.md

