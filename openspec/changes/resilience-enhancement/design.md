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
| 504 | TIMEOUT_ERROR | 上游超时 |
| 网络异常 (IOException) | NETWORK_ERROR | DNS/连接/SSL 错误 |
| 其他 | UNKNOWN_ERROR | 无法分类 |

**理由：** 状态码分类已足够区分主要场景，错误体内容解析作为补充（如区分 429 限流 vs 429 配额超限）。替代方案是统一字符串匹配，但状态码更可靠。

### D2：差异化重试策略 — RetryExecutor 内部策略选择

**决策**：不在调用方传入 errorType，`RetryExecutor` 在 catch 中自动提取 `ProviderException.getErrorType()` 选择策略。

```
RetryStrategy (接口)
├── ExponentialBackoffStrategy  — 默认，倍率 2x
├── RateLimitRetryStrategy      — 429：初始 2s，倍率 2x，最大 60s，加抖动
├── FastRetryStrategy           — 504：立即重试 1 次，间隔 500ms
└── ServiceUnavailableStrategy  — 503：等待 5s，最多 3 次
```

**理由**：errorType 只有在第一次执行失败后才能知道，调用方无法提前传入。RetryExecutor 内部分析保持接口不变，关注点分离更干净。

### D3：渠道级故障转移 — Key 级 + Channel 级两级转移

故障转移不在 `UpstreamClient` 层实现，而是在 `ChatDispatchServiceImpl` 层编排：

```
ChatDispatchServiceImpl.dispatch()
  → 获取主 Channel 的多个 Key（按优先级排序）
  → 遍历 Key：
      → 跳过熔断器 OPEN 的
      → 调用 client.chat(request)
      → 成功 → 返回
      → 失败 → 尝试下一个 Key
  → 当前 Channel 所有 Key 都失败
      → 如果有备用 Channel → 切换到下一个 Channel（重新获取 Key 列表）
      → 没有备用 Channel → 抛出 ALL_PROVIDERS_FAILED
```

**理由：** 故障转移涉及路由决策（选择哪个备用 Channel/Key），属于 Application 层编排职责。`UpstreamClient` 只负责单端点调用。即使只有一个 Channel，多个 Key 时故障转移仍然有效。

### D4：异常上下文注入 — 异常构造器增强

```java
ProviderException(code, message, traceId, model, provider, channelEndpointId)
```

上下文信息在 `ChatDispatchServiceImpl.dispatch()` 中已具备，在 catch 块中包装异常时注入。

**理由：** 不在 `UpstreamClient` 层注入（它不感知 Trace ID），在 Application 层包装。最小化侵入。

### D5：智能降级 — 基于 DegradationService

```
DegradationService
├── degrade(model, userId, reason) → 返回备选模型名
├── canRecover(model) → 检查原模型是否恢复
└── recoveryCheck(model) → 执行健康检查并触发回切
```

降级链配置：
```yaml
gateway:
  degradation:
    chains:
      - primary: "gpt-4o"
        fallbacks: ["claude-sonnet-4", "gpt-4o-mini"]
        recovery-check-interval: 60s
        recovery-success-threshold: 3
```

**理由：** 独立 Service 而非嵌入路由逻辑，职责清晰。替代方案是路由层内联降级逻辑，但独立 Service 更易测试和独立演进。

### D6：Metrics 埋点 — ResilientUpstreamClient 层埋点

**决策**：Metrics 不侵入 `CircuitBreaker`/`RetryExecutor`（保持纯 Java 零框架依赖），而是在 `ResilientUpstreamClient`（infrastructure 层）中埋点。同层依赖 Micrometer 不违反架构。

| 指标 | 类型 | 标签 | 位置 |
|------|------|------|------|
| `gateway.provider.errors` | Counter | provider, error_type | ResilientUpstreamClient |
| `gateway.circuitbreaker.state` | Gauge | provider, endpoint_id | ResilientUpstreamClient |
| `gateway.retry.attempts` | Counter | provider, status_code | ResilientUpstreamClient |
| `gateway.retry.exhausted` | Counter | provider | ResilientUpstreamClient |
| `gateway.failover.triggered` | Counter | from_provider, to_provider | ChatDispatchServiceImpl |
| `gateway.degradation.triggered` | Counter | from_model, to_model, reason | DegradationService |
| `gateway.degradation.recovered` | Counter | model | DegradationService |

**理由：** Micrometer 是 Spring Boot 3.5 内置指标门面，零额外依赖。`ResilientUpstreamClient`（infrastructure 层）使用 `MeterRegistry`（基础设施框架）是合理的同层依赖。单元测试可用 `SimpleMeterRegistry` 验证指标值。

### D7：SSE 流式错误 — ProviderException 携带结构化字段，StreamCallback 包装层格式化

**决策**：`ProviderException` 增加 `errorType` 和 `retryAfterSeconds` 字段。在 `ChatDispatchServiceImpl` 的 `StreamCallback` 包装层格式化为 SSE 事件。

```
ProviderException.getErrorType()        → ProviderErrorType.RATE_LIMIT_ERROR
ProviderException.getRetryAfterSeconds() → 30

ChatDispatchServiceImpl 包装层：
  callback.onError(t) → 提取 errorType + retryAfterSeconds
                       → 格式化为 {"error":"rate_limit","retry_after":30}
                       → 传递给客户端
```

**理由**：异常类承载结构化数据最干净，格式化在输出层完成。`UpstreamClient` 不需要知道 SSE 格式细节，全局异常处理器和 StreamCallback 包装层各自按需格式化。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 错误分类过于宽泛导致误判重试策略 | 按状态码优先，错误体内容辅助，保留 UNKNOWN_ERROR 兜底 |
| 故障转移增加端到端延迟 | 设置全局超时兜底，单次转移 ≤100ms |
| 智能降级可能循环切换 | 降级链深度限制（最多 5 级），切换记录防止回环 |
| Metrics 指标爆炸 | 仅记录关键事件（熔断状态变更、重试耗尽），不记录单次重试 |
| 异常上下文注入泄露敏感信息 | 只注入 traceId/model/provider，不注入 API Key 或原始请求体 |

## Open Questions

1. 故障转移时是否需要在切换前执行快速健康检查（`testConnectivity`）？— 会增加延迟但减少无效切换
2. 降级链配置是全局还是按渠道？— 先做全局，后续可按渠道覆盖
3. 差异化重试的配置参数是否需要动态更新（Spring Cloud Config）？— 初期配置文件，后续支持动态
