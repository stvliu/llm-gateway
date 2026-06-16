# provider-simulator Specification

## Purpose
TBD - created by archiving change llm-provider-simulator. Update Purpose after archive.
## Requirements
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
- **THEN** 抛出 `ProviderException`，`errorType` 为 `TIMEOUT_ERROR`

#### Scenario: 连通性测试成功
- **WHEN** 调用 `client.testConnectivity()` 且模拟器对 GET `/v1/models` 返回 200
- **THEN** 返回 `ConnectivityTestResult`，`success` 为 true

#### Scenario: 连通性测试失败
- **WHEN** 调用 `client.testConnectivity()` 且模拟器返回 500
- **THEN** 返回 `ConnectivityTestResult`，`success` 为 false，包含错误信息

### Requirement: AnthropicUpstreamClient HTTP 层测试覆盖

系统 SHALL 提供 `AnthropicUpstreamClientTest` 测试类，基于 ProviderSimulator 验证 AnthropicUpstreamClient 的 HTTP 请求构造、响应反序列化、错误分类等行为。

#### Scenario: 非流式调用发送正确的 HTTP 请求
- **WHEN** 调用 `client.chat(request)` 且模拟器返回成功响应
- **THEN** 请求路径为 `/v1/messages`，Header 包含 `x-api-key: {apiKey}` 和 `anthropic-version: 2023-06-01`，Body 包含模型名称

#### Scenario: 非流式调用正确反序列化响应
- **WHEN** 模拟器返回标准 Anthropic Messages JSON
- **THEN** 返回的 `ProtocolResponse` 类型为 `AnthropicMessagesResponse`，model、content、usage 字段正确填充

#### Scenario: 流式调用正确解析 SSE 事件流
- **WHEN** 模拟器返回 SSE 格式的流式响应（含 `event:` + `data:` 行和 `message_stop` 事件终止标记）
- **THEN** `StreamCallback.onChunk()` 被每个有效数据块调用，`onComplete()` 在收到 `message_stop` 后调用

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
- **THEN** 抛出 `ProviderException`，`errorType` 为 `TIMEOUT_ERROR`

#### Scenario: 连通性测试成功
- **WHEN** 调用 `client.testConnectivity()` 且模拟器对 POST `/v1/messages` 返回非 5xx 状态码
- **THEN** 返回 `ConnectivityTestResult`，`success` 为 true

#### Scenario: 连通性测试失败
- **WHEN** 调用 `client.testConnectivity()` 且模拟器返回 5xx 状态码
- **THEN** 返回 `ConnectivityTestResult`，`success` 为 false，包含错误信息

### Requirement: gateway-simulator 独立运行模拟服务

系统 SHALL 提供 `gateway-simulator` Maven 模块，作为独立 Spring Boot 应用运行，提供模拟的 OpenAI 和 Anthropic HTTP 端点，开发者无需真实 API Key 即可验证 Gateway 集成。

#### Scenario: 启动模拟器并访问 OpenAI 端点
- **WHEN** 启动 `gateway-simulator` 应用，然后向 `http://localhost:{port}/v1/chat/completions` 发送 POST 请求
- **THEN** 返回 HTTP 200 + 标准 OpenAI Chat Completion JSON 响应

#### Scenario: 启动模拟器并访问 Anthropic 端点
- **WHEN** 启动 `gateway-simulator` 应用，然后向 `http://localhost:{port}/v1/messages` 发送 POST 请求
- **THEN** 返回 HTTP 200 + 标准 Anthropic Messages JSON 响应

#### Scenario: 流式端点返回 SSE 事件流
- **WHEN** 请求包含 `"stream": true` 参数
- **THEN** 响应 Content-Type 为 `text/event-stream`，Body 包含 SSE 格式的流式数据块和终止标记

#### Scenario: 通过管理 API 切换为限流模式
- **WHEN** 调用管理 API 将模式切换为限流模式
- **THEN** 后续对 `/v1/chat/completions` 和 `/v1/messages` 的请求返回 HTTP 429

#### Scenario: 通过管理 API 切换为故障模式
- **WHEN** 调用管理 API 将模式切换为故障模式
- **THEN** 后续对 `/v1/chat/completions` 和 `/v1/messages` 的请求返回 HTTP 500

#### Scenario: 通过管理 API 恢复为正常模式
- **WHEN** 调用管理 API 将模式切换回正常模式
- **THEN** 后续请求返回 HTTP 200 + 正常 JSON 响应

#### Scenario: 查看请求记录
- **WHEN** 调用管理 API 查看请求记录
- **THEN** 返回最近 N 条请求的方法、路径、Header 摘要和时间戳

#### Scenario: 配置模拟器端口
- **WHEN** 在 `application.yml` 中配置 `simulator.port=9090`
- **THEN** 模拟器在 9090 端口启动

