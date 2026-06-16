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

| 风险 | 缓解措施 |
|------|---------|
| MockWebServer 的 SSE 模拟与真实服务器行为可能有细微差异 | 使用 `throttleBody()` 模拟分块传输；测试重点在客户端解析逻辑而非服务器行为 |
| 硬编码的响应模板可能与未来 API 格式变更不同步 | 模板仅用于测试，真实 API 兼容性由集成测试保证 |
| gateway-simulator 复制模板类导致重复代码 | 模板代码极简（纯字符串常量），重复可接受；如后续扩展可提取为共享模块 |
| 流式测试的异步回调验证可能不稳定 | 使用 `CountDownLatch` 或 `CompletableFuture` 确保回调完成后断言 |
