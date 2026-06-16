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
