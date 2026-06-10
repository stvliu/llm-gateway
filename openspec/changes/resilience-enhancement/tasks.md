# 任务清单

## P0 — 异常分类与异常体系完善

- [x] **1. ProviderErrorType 落地到 UpstreamClient**
  - `OpenAIUpstreamClient.chat()` 和 `AnthropicUpstreamClient.chat()` 按 HTTP 状态码 + 错误体映射 `ProviderErrorType`
  - `OpenAIUpstreamClient.chatStream()` 和 `AnthropicUpstreamClient.chatStream()` 同样映射
  - IOException 统一映射为 `NETWORK_ERROR`

- [x] **2. CircuitOpenException 继承改造**
  - `CircuitOpenException` 改为继承 `ProviderException`
  - 更新 `GlobalExceptionHandler` 统一捕获

- [ ] **3. SSE 流式错误结构化**
  - `OpenAIUpstreamClient` / `AnthropicUpstreamClient` 流式错误格式化为结构化 SSE 事件
  - 按错误类型生成对应的 `error` / `retry_after` 字段

## P1 — 差异化重试与故障转移

- [ ] **4. 差异化重试策略**
  - 策略接口 `RetryStrategy` + 四种实现（ExponentialBackoff/RateLimit/Fast/ServiceUnavailable）
  - `RetryExecutor` 按 `ProviderErrorType` 选择策略
  - 配置参数支持（`GatewayRetryProperties` 扩展）

- [ ] **5. 异常上下文注入**
  - `ProviderException` 增加 `traceId`/`model`/`provider`/`channelEndpointId`/`errorType` 字段
  - `ChatDispatchServiceImpl` catch 块中注入上下文

- [ ] **6. 渠道级故障转移**
  - `RoutingResolver` 返回备用 Channel 列表
  - `ChatDispatchServiceImpl` 主→备切换逻辑
  - 审计日志记录故障转移事件

## P2 — Metrics 与智能降级

- [ ] **7. Metrics 埋点**
  - `CircuitBreaker` 上报 `gateway.provider.errors` 和 `gateway.circuitbreaker.state`
  - `RetryExecutor` 上报 `gateway.retry.attempts` 和 `gateway.retry.exhausted`
  - 故障转移上报 `gateway.failover.triggered` 和 `gateway.failover.exhausted`

- [ ] **8. 模型级智能降级骨架**
  - `DegradationService` 接口 + 实现
  - 降级链配置加载与解析
  - 降级触发逻辑（基于 ProviderException 类型）
  - 自动回切（健康检查 + 连续成功阈值）
  - `DegradationEvent` / `DegradationRecoveredEvent`
  - 降级 Metrics 埋点
