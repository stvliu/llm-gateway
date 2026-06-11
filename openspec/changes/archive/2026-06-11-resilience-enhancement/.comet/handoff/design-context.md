# Comet Design Handoff

- Change: resilience-enhancement
- Phase: design
- Mode: compact
- Context hash: 2419ac14d87b28c8fca0c7d0dc944d624ad93b69e395bdf84dc2f7fdb8c00c04

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/resilience-enhancement/proposal.md

- Source: openspec/changes/resilience-enhancement/proposal.md
- Lines: 1-46
- SHA256: e16d0de39bea31d79d8d2a944bd1592aeec0429bb811e9a678c9cff94a5d3b6e

```md
## Why

LLM-Gateway 作为企业级 AI 网关，上游大模型（Provider）异常处理直接影响业务连续性 SLA。当前代码存在以下问题：

1. **所有异常一律抛 `"UPSTREAM_ERROR"`** — `ProviderErrorType` 8 种枚举定义了但未使用，无法区分 429 限流、503 不可用、504 超时等不同场景
2. **SSE 流式错误裸奔** — 文档要求结构化 SSE 错误事件，实际直接 `callback.onError(t)` 抛原始 IOException
3. **熔断器异常未纳入异常体系** — `CircuitOpenException` 独立于 `RuntimeException`，全局异常处理器无法统一捕获
4. **差异化重试策略缺失** — 文档要求 429/503/504 不同退避策略，代码统一指数退避
5. **无渠道级故障转移** — 重试耗尽后不会切换 Channel/Key，请求直接失败
6. **异常上下文缺失** — Trace ID、Model、Provider 等诊断信息未注入异常
7. **无 Metrics 埋点** — 熔断器/重试器关键事件无监控指标
8. **智能降级未实现** — 模型级智能降级（P0 核心竞争力）代码空白

这些问题共同导致：上游异常不可观测、不可区分、不可差异化处理，严重影响运维排障能力和业务连续性承诺。

## What Changes

1. **ProviderErrorType 落地** — `OpenAIUpstreamClient` / `AnthropicUpstreamClient` 按 HTTP 状态码和错误体内容，映射到正确的 `ProviderErrorType`，抛出带类型的 `ProviderException`
2. **SSE 流式结构化错误** — 流式错误按文档要求格式化为 `event: error\ndata: {"error":"<type>","retry_after":N}` 格式
3. **CircuitOpenException 继承改造** — 改为继承 `ProviderException`，纳入统一异常体系
4. **差异化重试策略** — `RetryExecutor` 按状态码选择退避策略（429 长退避、504 快速重试、503 适中等待）
5. **渠道级故障转移** — 重试耗尽后，`ChatDispatchService` 尝试下一可用 Channel/Key
6. **异常上下文注入** — `ProviderException` 新增 `traceId`、`model`、`provider`、`channelEndpointId` 字段
7. **Metrics 埋点** — `CircuitBreaker` 和 `RetryExecutor` 关键事件上报 Micrometer 指标
8. **智能降级骨架** — 实现 `DegradationService`，支持降级链配置和自动切换

## Capabilities

### New Capabilities
- `upstream-exception-classification`: 上游异常分类与结构化错误响应
- `intelligent-degradation`: 模型级智能降级（降级链配置 + 自动切换 + 回切）

### Modified Capabilities
- （无现有 spec 变更 — 这是基础设施层增强，不改变已有 capability 的外部行为）

## Impact

| 影响范围 | 说明 |
|---------|------|
| `infrastructure/resilience/` | CircuitBreaker、RetryExecutor、ResilientUpstreamClient 增强 |
| `infrastructure/upstream/` | OpenAIUpstreamClient、AnthropicUpstreamClient 错误分类 |
| `domain/supply/exception/` | ProviderException 增加上下文字段 |
| `application/proxy/` | ChatDispatchServiceImpl 故障转移逻辑 |
| `application/degradation/` | 新建 DegradationService 实现 |
| `adapter/advice/` | GlobalExceptionHandler 适配新异常类型 |
| 配置 | 新增差异化重试配置、降级链配置 |
```

## openspec/changes/resilience-enhancement/design.md

- Source: openspec/changes/resilience-enhancement/design.md
- Lines: 1-198
- SHA256: 5f9da00bd97b1a55a6ae3f0890e0058d52d70d60510c9e89d819cedac08f8007

[TRUNCATED]

```md
## Context

LLM-Gateway 作为 API 网关，位于调用方与上游大模型之间。当前异常处理存在架构性缺陷：

```
调用方                    LLM-Gateway                    上游 Provider
  │                           │                              │
  │      chat(request)        │      HTTP POST (OkHttp)      │
  │ ──────────────────────►   │ ──────────────────────────►  │
  │                           │                              │
  │                           │  ← 429/502/503/504/超时      │
  │                           │     全部抛 "UPSTREAM_ERROR"  │
  │  ← "UPSTREAM_ERROR"       │                              │
  │     无法区分原因          │                              │
```

现有组件：
- `ResilientUpstreamClient` — 熔断器 + 重试包装器（已实现）
- `CircuitBreaker` — 滑动窗口熔断器（已实现）
- `RetryExecutor` — 统一指数退避重试（已实现）
- `ChannelEndpointCircuitBreakerManager` — 按端点独立熔断（已实现）
- `ProviderHealthTracker` — 被动推断健康状态（已实现）
- `ProviderErrorType` — 8 种枚举已定义但未使用

## Goals / Non-Goals

**Goals:**
1. 上游异常按 `ProviderErrorType` 细分类，下游可区分处理
2. SSE 流式错误格式化为结构化事件，兼容 OpenAI/Anthropic 格式
3. `CircuitOpenException` 纳入 `ProviderException` 异常体系
4. 差异化重试策略（429/503/504 不同退避）
5. 渠道级故障转移（重试耗尽后切换 Channel/Key）
6. 异常上下文注入（Trace ID + Model + Provider）
7. Metrics 埋点到熔断器/重试器关键事件
8. 模型级智能降级骨架（降级链 + 自动切换）

**Non-Goals:**
- 不引入新的外部依赖（如 Resilience4j、Hystrix）
- 不改动 Protocol 层契约（`ProtocolRequest`/`ProtocolResponse` 不变）
- 不涉及 License 过期降级（那是另一功能）
- 不涉及语义缓存降级（那是另一功能）

## Decisions

### D1：异常分类策略 — ErrorClassificationStrategy 接口

**决策**：定义 `ErrorClassificationStrategy` 接口，每个 Provider 各自实现分类逻辑。

```java
public interface ErrorClassificationStrategy {
    ProviderErrorType classify(int statusCode, String responseBody);
}
```

| 实现类 | 归属 |
|--------|------|
| `OpenAIErrorClassifier` | `infrastructure/upstream/` |
| `AnthropicErrorClassifier` | `infrastructure/upstream/` |

**理由**：每个 Provider 的错误体格式不同（OpenAI 用 `error.type`，Anthropic 用 `error.type`，未来其他 Provider 各有差异）。策略接口可扩展，新增 Provider 只需新增实现。替代方案（工具类静态方法）不易测试且不支持扩展。

**IOException 处理方式**：不经过策略，在上游客户端 catch 块中直接映射：
```
SocketTimeoutException → TIMEOUT_ERROR
其他 IOException → NETWORK_ERROR
```

```
HTTP 响应 → 解析状态码 + 错误体 JSON → 映射 ProviderErrorType → 抛出带类型的 ProviderException
```

| HTTP 状态码 | ProviderErrorType | 判定依据 |
|------------|------------------|---------|
| 401 | AUTHENTICATION_ERROR | API Key 无效/过期 |
| 429 | RATE_LIMIT_ERROR | 限流 |
| 429 + "quota" / "insufficient_quota" | QUOTA_EXCEEDED | 配额超限（OpenAI 特有） |
| 400 | INVALID_REQUEST | 请求格式错误 |
| 408 / 超时 | TIMEOUT_ERROR | 请求超时 |
| 500 | UPSTREAM_ERROR | 上游服务器错误 |
| 502/503 | UPSTREAM_ERROR | 上游不可用 |
```

Full source: openspec/changes/resilience-enhancement/design.md

## openspec/changes/resilience-enhancement/tasks.md

- Source: openspec/changes/resilience-enhancement/tasks.md
- Lines: 1-47
- SHA256: ff4d0479b197535869330e94e9f5b6605c06d9a422be6a720787674d91de8997

```md
# 任务清单

## P0 — 异常分类与异常体系完善

- [ ] **1. ProviderErrorType 落地到 UpstreamClient**
  - `OpenAIUpstreamClient.chat()` 和 `AnthropicUpstreamClient.chat()` 按 HTTP 状态码 + 错误体映射 `ProviderErrorType`
  - `OpenAIUpstreamClient.chatStream()` 和 `AnthropicUpstreamClient.chatStream()` 同样映射
  - IOException 统一映射为 `NETWORK_ERROR`

- [ ] **2. CircuitOpenException 继承改造**
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
```

## openspec/changes/resilience-enhancement/specs/intelligent-degradation/spec.md

- Source: openspec/changes/resilience-enhancement/specs/intelligent-degradation/spec.md
- Lines: 1-106
- SHA256: a20d2178ecc47d4e500acc35a9effb63d8c078cdfe840e1a2f606045a12e9aaf

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 降级链配置

系统 SHALL 支持配置模型降级链，定义主模型不可用时的备选模型列表和回切策略。

```yaml
gateway:
  degradation:
    enabled: true
    chains:
      - primary: "gpt-4o"
        fallbacks: ["claude-sonnet-4", "gpt-4o-mini"]
        recovery:
          check-interval: 60s
          success-threshold: 3
      - primary: "claude-opus-4"
        fallbacks: ["gpt-4o"]
        recovery:
          check-interval: 120s
          success-threshold: 5
    max-chain-depth: 5
```

#### Scenario: 配置合法时加载降级链
- **WHEN** 应用启动时加载降级配置
- **THEN** 所有降级链被解析并注册到 `DegradationService`

#### Scenario: 配置循环引用时拒绝加载
- **WHEN** 降级链配置存在循环引用（A→B→C→A）
- **THEN** 启动时抛出配置异常，提示循环引用路径

### Requirement: 降级触发

`DegradationService` SHALL 在以下条件触发降级：

| 触发条件 | 判定方式 | 触发动作 |
|---------|---------|---------|
| 主模型上游 429/5xx | `ProviderException` 异常类型 | 切换到备选模型 |
| 主模型熔断器 OPEN | `CircuitOpenException` | 切换到备选模型 |
| 主模型 Token 限额超限 | `QuotaExceededException` | 切换到备选模型 |
| 主模型超时 | `TIMEOUT_ERROR` | 切换到备选模型 |

#### Scenario: 主模型不可用时自动降级到备选模型
- **WHEN** 调用主模型抛出 `CircuitOpenException`
- **THEN** `DegradationService.degrade()` 返回降级链中第一个可用备选模型名

#### Scenario: 降级链中所有模型均不可用时抛出异常
- **WHEN** 降级链中所有模型均不可用
- **THEN** 抛出 `ProviderException("ALL_MODELS_DEGRADED")`

#### Scenario: 降级事件记录审计日志
- **WHEN** 降级发生
- **THEN** 审计日志记录 `from_model`、`to_model`、`reason`、`chain_step` 字段

### Requirement: 降级通知

降级发生时 SHALL 通过 `DomainEventPublisher` 发布 `DegradationEvent`：

```java
DegradationEvent {
    String traceId;
    Long userId;
    String originalModel;
    String fallbackModel;
    DegradationTrigger reason;
    int chainStep;       // 降级链第几步
    Instant triggeredAt;
}
```

#### Scenario: 降级时发布 DegradationEvent
- **WHEN** `DegradationService.degrade()` 成功返回备选模型
- **THEN** 发布 `DegradationEvent`，包含降级原因和链步数

### Requirement: 自动回切

`DegradationService` SHALL 定期检查已降级的主模型是否恢复：

1. 每 `check-interval` 执行一次健康检查（调用 `testConnectivity()` 或发送最小请求）
```

Full source: openspec/changes/resilience-enhancement/specs/intelligent-degradation/spec.md

## openspec/changes/resilience-enhancement/specs/upstream-exception-classification/spec.md

- Source: openspec/changes/resilience-enhancement/specs/upstream-exception-classification/spec.md
- Lines: 1-162
- SHA256: 1b0c3565cf6c4036f39838cbec8159a0f5b2671b54ed8c833eebc685e5ee63eb

[TRUNCATED]

```md
## ADDED Requirements

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
```

Full source: openspec/changes/resilience-enhancement/specs/upstream-exception-classification/spec.md

