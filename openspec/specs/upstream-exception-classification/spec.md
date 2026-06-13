# Upstream Exception Classification
## Summary

## Requirements

### Requirement: 上游异常按 HTTP 状态码分类

`UpstreamClient` 实现 SHALL 根据上游 HTTP 响应状态码和错误体内容，映射到正确的 `ProviderErrorType`，抛出带错误类型的 `ProviderException`。

| HTTP 状态码 | ProviderErrorType | 补充判定 |
|------------|------------------|---------|
| 401 | AUTHENTICATION_ERROR | — |
| 429 | RATE_LIMIT_ERROR | 除非错误体含 "quota"/"insufficient_quota" → QUOTA_EXCEEDED |
| 400 | INVALID_REQUEST | — |
| 408 / ReadTimeout | TIMEOUT_ERROR | — |
| 500 | UPSTREAM_ERROR | — |
| 502/503 | UPSTREAM_ERROR | — |
| 504 | TIMEOUT_ERROR | — |
| IOException (连接/DNS/SSL) | NETWORK_ERROR | — |
| 其他 | UNKNOWN_ERROR | — |

#### Scenario: 401 认证失败映射为 AUTHENTICATION_ERROR
- **WHEN** 上游返回 HTTP 401
- **THEN** 抛出 `ProviderException`，`errorType` 为 `AUTHENTICATION_ERROR`

#### Scenario: 429 限流映射为 RATE_LIMIT_ERROR
- **WHEN** 上游返回 HTTP 429 且错误体不含 "quota" 关键字
- **THEN** 抛出 `ProviderException`，`errorType` 为 `RATE_LIMIT_ERROR`

#### Scenario: 429 配额超限映射为 QUOTA_EXCEEDED
- **WHEN** 上游返回 HTTP 429 且错误体含 "quota" 或 "insufficient_quota"
- **THEN** 抛出 `ProviderException`，`errorType` 为 `QUOTA_EXCEEDED`

#### Scenario: 504 超时映射为 TIMEOUT_ERROR
- **WHEN** 上游返回 HTTP 504
- **THEN** 抛出 `ProviderException`，`errorType` 为 `TIMEOUT_ERROR`

#### Scenario: IOException 网络错误映射为 NETWORK_ERROR
- **WHEN** `OkHttp` 调用抛出 `IOException`（连接超时、DNS 解析失败、SSL 握手失败）
- **THEN** 抛出 `ProviderException`，`errorType` 为 `NETWORK_ERROR`

### Requirement: SSE 流式错误结构化

`UpstreamClient.chatStream()` 在遇到上游 HTTP 错误时，SHALL 构建结构化 SSE 错误事件而非裸抛异常。

结构化 SSE 错误格式：

```
event: error
data: {"error":"<type>","retry_after":<seconds>}
```

错误类型映射：

| 场景 | error type | retry_after |
|------|-----------|-------------|
| 429 Rate Limit | rate_limit | 从 Retry-After 头或默认 30 |
| 429 Quota Exceeded | quota_exceeded | 0 |
| 503 Unavailable | server_error | 5 |
| 504 Timeout | timeout | 0 |
| 401/403 认证失败 | authentication_error | 0 |
| 500 内部错误 | api_error | 0 |
| 其他 | unknown_error | 0 |

#### Scenario: 流式请求 429 限流时返回结构化错误事件
- **WHEN** 流式请求时上游返回 HTTP 429
- **THEN** `callback.onError` 收到的异常消息包含 `{"error":"rate_limit","retry_after":30}`

#### Scenario: 流式请求 503 不可用时返回结构化错误事件
- **WHEN** 流式请求时上游返回 HTTP 503
- **THEN** `callback.onError` 收到的异常消息包含 `{"error":"server_error","retry_after":5}`

#### Scenario: 流式请求网络错误时返回 NETWORK_ERROR
- **WHEN** 流式请求时发生 `IOException`
- **THEN** `callback.onError` 收到的异常消息包含 `{"error":"network_error"}`

### Requirement: CircuitOpenException 纳入异常体系

`CircuitOpenException` SHALL 继承 `ProviderException`，而非 `RuntimeException`。

#### Scenario: 熔断器开启时抛出带 ProviderException 类型的异常
- **WHEN** 熔断器处于 OPEN 状态拒绝请求
- **THEN** 抛出 `CircuitOpenException`，其继承自 `ProviderException`，可在 `GlobalExceptionHandler` 中被统一捕获并返回 502

### Requirement: 异常上下文注入

`ProviderException` SHALL 包含以下上下文字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `traceId` | String | 全链路追踪 ID |
| `model` | String | 请求模型名 |
| `provider` | String | 上游 Provider 代码 |
| `channelEndpointId` | Long | 渠道端点 ID |
| `errorType` | ProviderErrorType | 错误类型枚举 |

#### Scenario: 异常包含完整的上下文信息
- **WHEN** 上游调用失败抛出 `ProviderException`
- **THEN** 异常中 `traceId`/`model`/`provider`/`channelEndpointId`/`errorType` 均不为 null

### Requirement: 差异化重试策略

`RetryExecutor` SHALL 根据异常类型选择不同的退避策略：

| HTTP 状态码 | 策略 | 参数 |
|------------|------|------|
| 429 (RATE_LIMIT) | 指数退避 + 抖动 | 初始 2s，倍率 2x，最大 60s |
| 503 (UPSTREAM) | 固定等待 | 5s，最多 3 次 |
| 504 (TIMEOUT) | 快速重试 | 500ms，最多 2 次 |
| 其他可重试 | 默认指数退避 | 初始 1s，倍率 2x，最多 3 次 |

#### Scenario: 429 限流使用长退避策略
- **WHEN** 上游返回 429 触发重试
- **THEN** 退避时间为 2s→4s→8s…（指数增长），最大不超过 60s，且加入 ±25% 随机抖动

#### Scenario: 504 超时使用快速重试策略
- **WHEN** 上游返回 504 触发重试
- **THEN** 退避时间为 500ms，最多重试 2 次

#### Scenario: 503 不可用使用固定等待策略
- **WHEN** 上游返回 503 触发重试
- **THEN** 退避时间为 5s 固定，最多重试 3 次

### Requirement: 渠道级故障转移

`ChatDispatchServiceImpl` SHALL 在重试耗尽后尝试下一可用 Channel/Key：

故障转移流程：
1. 路由解析返回主 Channel + 备用 Channel 列表（按优先级排序）
2. 调用主 Channel（带熔断 + 重试）
3. 主 Channel 失败 → 记录失败事件 → 尝试下一备用 Channel
4. 备用 Channel 也失败 → 全部失败 → 抛出 `ProviderException("ALL_PROVIDERS_FAILED")`

#### Scenario: 主 Channel 失败后自动切换到备用 Channel
- **WHEN** 主 Channel 调用失败（重试耗尽）
- **THEN** 自动尝试下一备用 Channel，切换延迟 ≤100ms

#### Scenario: 全部 Channel 失败时抛出 ALL_PROVIDERS_FAILED
- **WHEN** 所有可用 Channel 均调用失败
- **THEN** 抛出 `ProviderException("ALL_PROVIDERS_FAILED")`

#### Scenario: 故障转移计入审计日志
- **WHEN** 发生 Channel 切换
- **THEN** 审计日志记录 `failover_from`、`failover_to`、`failover_reason` 字段

### Requirement: Metrics 埋点

以下关键事件 SHALL 上报 Micrometer 指标：

| 指标名 | 类型 | 标签 | 触发点 |
|--------|------|------|--------|
| `gateway.provider.errors` | Counter | `provider`, `error_type` | CircuitBreaker.recordFailure |
| `gateway.circuitbreaker.state` | Gauge | `provider`, `endpoint_id` | 熔断器状态变更 |
| `gateway.retry.attempts` | Counter | `provider`, `status_code` | RetryExecutor 每次重试 |
| `gateway.retry.exhausted` | Counter | `provider` | RetryExecutor 重试耗尽 |
| `gateway.failover.triggered` | Counter | `from_provider`, `to_provider` | 故障转移切换 |
| `gateway.failover.exhausted` | Counter | — | 全部 Channel 失败 |

#### Scenario: 熔断器失败时上报 error Metrics
- **WHEN** `CircuitBreaker.recordFailure()` 被调用
- **THEN** `gateway.provider.errors` Counter 自增 1，携带 `provider` 和 `error_type` 标签

#### Scenario: 重试耗尽时上报 exhausted Metrics
- **WHEN** `RetryExecutor` 所有重试均失败
- **THEN** `gateway.retry.exhausted` Counter 自增 1
