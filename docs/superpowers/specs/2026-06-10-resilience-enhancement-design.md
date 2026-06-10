---
comet_change: resilience-enhancement
role: technical-design
canonical_spec: openspec
---

# 韧性增强 — 上游大模型异常处理体系深度设计

> **版本**: v1.0
> **日期**: 2026-06-10
> **关联 Change**: resilience-enhancement

---

## 1. 技术架构

```
┌──────────────────────────────────────────────────────────────────┐
│                      异常处理体系架构                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Application 层                    Infrastructure 层              │
│  ┌──────────────────────┐        ┌──────────────────────────┐    │
│  │ ChatDispatchService   │        │ ResilientUpstreamClient   │    │
│  │  ├─ 故障转移编排       │───────▶│  ├─ CircuitBrecker       │    │
│  │  └─ 异常上下文注入     │        │  ├─ RetryExecutor         │    │
│  │                      │        │  └─ Metrics 埋点          │    │
│  │ DegradationService    │        │                          │    │
│  │  ├─ 降级链管理         │        │ OpenAIUpstreamClient      │    │
│  │  ├─ degrade()         │        │  ├─ ErrorClassifier       │    │
│  │  └─ recoveryCheck()   │        │  └─ SSE 错误格式化        │    │
│  └──────────────────────┘        │                          │    │
│                                   │ AnthropicUpstreamClient   │    │
│  Domain 层                       │  ├─ ErrorClassifier       │    │
│  ┌──────────────────────┐        │  └─ SSE 错误格式化        │    │
│  │ ProviderException     │        └──────────────────────────┘    │
│  │  ├─ errorType          │                                        │
│  │  ├─ traceId/model/... │                                        │
│  │  └─ retryAfterSeconds  │                                        │
│  │                      │                                        │
│  │ ProviderErrorType     │                                        │
│  │ (8 种枚举)            │                                        │
│  │                      │                                        │
│  │ CircuitOpenException  │                                        │
│  │ 继承 ProviderException│                                        │
│  └──────────────────────┘                                        │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

## 2. 详细设计方案

### 2.1 异常分类 — ErrorClassificationStrategy

**接口定义**（infrastructure/upstream/ 包）：

```java
public interface ErrorClassificationStrategy {
    ProviderErrorType classify(int statusCode, String responseBody);
}
```

**实现**：

| 实现类 | 归属 | 逻辑 |
|--------|------|------|
| `OpenAIErrorClassifier` | `infrastructure/upstream/` | 解析 OpenAI 错误格式 `error.type` + statusCode |
| `AnthropicErrorClassifier` | `infrastructure/upstream/` | 解析 Anthropic 错误格式 `error.type` + statusCode |

**通用映射规则**（所有实现共享，在抽象类或工具方法中）：

```
401 → AUTHENTICATION_ERROR
429 + body 含 "quota"/"insufficient_quota" → QUOTA_EXCEEDED
429 → RATE_LIMIT_ERROR
400 → INVALID_REQUEST
408 → TIMEOUT_ERROR
500 → UPSTREAM_ERROR
502/503 → UPSTREAM_ERROR
504 → TIMEOUT_ERROR
SocketTimeoutException → TIMEOUT_ERROR
其他 IOException → NETWORK_ERROR
default → UNKNOWN_ERROR
```

**网络异常直接在上游客户端 catch 块中处理**，不经过策略：

```java
// OpenAIUpstreamClient.chat()
catch (IOException e) {
    if (e instanceof SocketTimeoutException) {
        throw new ProviderException("TIMEOUT_ERROR", "上游超时", e);
    }
    throw new ProviderException("NETWORK_ERROR", "网络异常: " + e.getMessage(), e);
}
```

### 2.2 异常上下文注入 — ProviderException 增强

```java
public class ProviderException extends GatewayException {
    private final ProviderErrorType errorType;
    private final String traceId;
    private final String model;
    private final String provider;
    private final Long channelEndpointId;
    private final Integer retryAfterSeconds;  // 仅限流场景
}
```

**注入时机**：`ChatDispatchServiceImpl.dispatch()` 的 catch 块中

```java
catch (ProviderException e) {
    // 注入上下文
    throw new ProviderException(
        e.getErrorType(), e.getMessage(),
        traceId, request.getModel(), ctx.upstreamProtocol().name(),
        ctx.channelEndpointId(), e.getRetryAfterSeconds()
    );
}
```

`CircuitOpenException` 改为继承 `ProviderException`，复用上下文字段：

```java
public class CircuitOpenException extends ProviderException {
    public CircuitOpenException(String traceId, String model, String provider, Long endpointId) {
        super(ProviderErrorType.UPSTREAM_ERROR, "熔断器开启，拒绝请求",
              traceId, model, provider, endpointId, null);
    }
}
```

### 2.3 SSE 流式错误结构化

**异常中携带结构化字段**，在 `StreamCallback` 包装层格式化：

```java
// ProviderException 中
ProviderException.getErrorType()      → ProviderErrorType.RATE_LIMIT_ERROR
ProviderException.getRetryAfterSeconds() → 30

// ChatDispatchServiceImpl 的 StreamCallback 包装层格式化
String formatSseError(ProviderException e) {
    String type = switch (e.getErrorType()) {
        case RATE_LIMIT_ERROR -> "rate_limit";
        case QUOTA_EXCEEDED -> "quota_exceeded";
        case AUTHENTICATION_ERROR -> "authentication_error";
        case TIMEOUT_ERROR -> "timeout";
        case UPSTREAM_ERROR -> "api_error";
        case NETWORK_ERROR -> "network_error";
        default -> "unknown_error";
    };
    int retryAfter = e.getRetryAfterSeconds() != null ? e.getRetryAfterSeconds() : 0;
    return String.format("{\"error\":\"%s\",\"retry_after\":%d}", type, retryAfter);
}
```

**SSE 错误类型 → 格式映射**：

| 场景 | 格式化后 |
|------|---------|
| 429 Rate Limit | `{"error":"rate_limit","retry_after":30}` |
| 429 Quota Exceeded | `{"error":"quota_exceeded","retry_after":0}` |
| 503 Unavailable | `{"error":"server_error","retry_after":5}` |
| 504 Timeout | `{"error":"timeout","retry_after":0}` |
| 401 Authentication | `{"error":"authentication_error","retry_after":0}` |
| 500 Internal | `{"error":"api_error","retry_after":0}` |
| 网络异常 | `{"error":"network_error","retry_after":0}` |

### 2.4 差异化重试策略

**策略接口**：

```java
public interface RetryStrategy {
    long calculateDelay(int attempt);
    int maxAttempts();
}
```

**四种实现**：

| 实现类 | 触发条件 | calculateDelay | maxAttempts |
|--------|---------|----------------|-------------|
| `ExponentialBackoffStrategy` | 默认（无可识别类型） | `1000ms * 2^(attempt-1)` | 3 |
| `RateLimitRetryStrategy` | RATE_LIMIT_ERROR | `2000ms * 2^(attempt-1)` + 抖动 ±25%，上限 60s | 5 |
| `FastRetryStrategy` | TIMEOUT_ERROR | 500ms 固定 | 2 |
| `ServiceUnavailableStrategy` | UPSTREAM_ERROR (503) | 5000ms 固定 | 3 |

**策略选择器**（内置于 `RetryExecutor`）：

```java
// RetryExecutor.execute() 内部
catch (ProviderException e) {
    if (attempt == maxAttemptsFor(e.getErrorType())) throw e;
    RetryStrategy strategy = selectStrategy(e.getErrorType());
    long delay = strategy.calculateDelay(attempt);
    sleep(delay);
}

private RetryStrategy selectStrategy(ProviderErrorType errorType) {
    return switch (errorType) {
        case RATE_LIMIT_ERROR -> new RateLimitRetryStrategy(properties);
        case TIMEOUT_ERROR -> new FastRetryStrategy(properties);
        case UPSTREAM_ERROR -> new ServiceUnavailableStrategy(properties); // 503
        default -> new ExponentialBackoffStrategy(properties);
    };
}
```

**配置扩展**（`GatewayRetryProperties`）：

```yaml
gateway:
  retry:
    max-attempts: 3
    backoff-initial: 1000
    backoff-multiplier: 2.0
    retryable-status-codes: [429, 500, 502, 503, 504]
    # 新增差异化配置
    rate-limit:
      max-attempts: 5
      backoff-initial: 2000
      backoff-multiplier: 2.0
      max-backoff: 60000
      jitter-rate: 0.25
    fast-retry:
      max-attempts: 2
      backoff-fixed: 500
    service-unavailable:
      max-attempts: 3
      backoff-fixed: 5000
```

### 2.5 渠道级故障转移

**Key 级故障转移**（同一 Channel 内多 Key）：

```java
// ChatDispatchServiceImpl.dispatch()
List<ChannelCredential> credentials = credentialResolver.resolve(ctx.channelId());
for (ChannelCredential cred : credentials) {
    if (!circuitBreakerManager.isAvailable(cred.endpointId())) continue;
    UpstreamClient client = clientRegistry.getClient(
        ctx.upstreamProtocol().name().toLowerCase(),
        ctx.endpointUrl(), cred.apiKey(), ctx.timeout());
    client = resilientClientFactory.wrap(client, cred.endpointId());
    try {
        return client.chat(outboundReq);
    } catch (ProviderException e) {
        log.warn("Key {} failed: {}", cred.id(), e.getErrorType());
        // 继续尝试下一个 Key
    }
}
// 所有 Key 失败 → 进入 Channel 级故障转移
```

**Channel 级故障转移**（多 Channel）：

```java
// ChatDispatchServiceImpl.dispatch()
List<RoutingContext> channels = routingResolver.resolveAll(model, protocol, userId);
for (RoutingContext ctx : channels) {
    try {
        return callWithRetry(ctx, outboundReq);
    } catch (ProviderException e) {
        meterRegistry.counter("gateway.failover.triggered",
            "from_provider", ctx.upstreamProtocol().name()).increment();
        // 继续尝试下一个 Channel
    }
}
throw new ProviderException("ALL_PROVIDERS_FAILED", traceId, model, ...);
```

**审计日志记录**故障转移事件：

| 字段 | 值 |
|------|-----|
| `failover_from` | 原 Channel ID / Key ID |
| `failover_to` | 目标 Channel ID / Key ID |
| `failover_reason` | 错误类型 + 错误消息 |
| `failover_step` | KEY_FAILOVER 或 CHANNEL_FAILOVER |

### 2.6 Metrics 埋点

所有 Metrics 在 `ResilientUpstreamClient` 和 `ChatDispatchServiceImpl` 中埋点：

```java
// ResilientUpstreamClient.chat()
try {
    ProtocolResponse response = retryExecutor.execute(() -> delegate.chat(request));
    circuitBreaker.recordSuccess();
    return response;
} catch (ProviderException e) {
    circuitBreaker.recordFailure();
    meterRegistry.counter("gateway.provider.errors",
        "provider", providerCode,
        "error_type", e.getErrorType().name()
    ).increment();
    throw e;
}
```

`MeterRegistry` 通过构造器注入 `ResilientUpstreamClient`，由 `ResilientClientFactoryImpl` 传入。

### 2.7 智能降级

**层归属**：`application/degradation/DegradationServiceImpl.java`

**核心接口**：

```java
public interface DegradationService {
    /**
     * 获取备选模型。返回降级链中第一个可用的模型名。
     */
    String degrade(String originalModel, ProviderErrorType reason);
    
    /**
     * 检查原模型是否已恢复。
     */
    boolean canRecover(String model);
    
    /**
     * 定期健康检查任务，恢复后标记模型可用。
     */
    void recoveryCheck();
}
```

**降级触发点**：`ChatDispatchServiceImpl.dispatch()` catch 块中

```java
catch (ProviderException e) {
    String fallbackModel = degradationService.degrade(request.getModel(), e.getErrorType());
    if (fallbackModel != null) {
        request.setModel(fallbackModel);
        return dispatch(request, identity, strategy);  // 递归重试
    }
    throw e;  // 无可用备选，抛出原始异常
}
```

**配置**：

```yaml
gateway:
  degradation:
    enabled: true
    max-chain-depth: 5
    chains:
      - primary: "gpt-4o"
        fallbacks: ["claude-sonnet-4", "gpt-4o-mini"]
        recovery:
          check-interval: 60s
          success-threshold: 3
```

## 3. 测试策略

| 测试维度 | 测试类型 | 关键验证点 |
|---------|---------|-----------|
| ErrorClassifier | 单元测试 | 每种 HTTP 状态码 + 错误体组合的映射正确性 |
| RetryStrategy | 单元测试 | 各策略退避时间计算、抖动范围、最大次数 |
| RetryExecutor | 单元测试 | 按异常类型选择策略、重试耗尽行为 |
| CircuitOpenException | 单元测试 | 继承体系验证、全局异常处理器捕获 |
| ProviderException | 单元测试 | 上下文注入字段完整性 |
| SSE 错误格式化 | 单元测试 | 各错误类型的 JSON 输出格式 |
| 故障转移 | 集成测试 | Key 级转移、Channel 级转移、全部失败 |
| DegradationService | 单元测试 | 降级链解析、触发逻辑、回切判断 |
| Metrics 埋点 | 单元测试 | SimpleMeterRegistry 验证指标值 |

## 4. 边界条件

| 场景 | 预期行为 |
|------|---------|
| 上游返回未知 HTTP 状态码（如 499） | 映射为 UNKNOWN_ERROR，使用默认重试策略 |
| 限流 Retry-After 头超过配置上限 | 取 min(header值, 配置上限 60s) |
| 所有 Key 都被熔断器 OPEN | 跳过所有 Key，直接进入 Channel 级故障转移 |
| 降级链形成循环引用 | 启动时配置校验拒绝，抛出异常 |
| 同时触发限流和超时 | 限流优先（先收到 429，不会同时触发） |
| 故障转移时目标 Channel 也被熔断 | 跳过熔断中的 Channel，尝试下一个 |
| 空降级链配置 | degrade() 返回 null，抛出原始异常 |

## 5. 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `domain/supply/exception/ProviderException.java` | 修改 | 增加 errorType/traceId/model/provider/channelEndpointId/retryAfterSeconds 字段 |
| `domain/supply/exception/CircuitOpenException.java` | 修改 | 改为继承 ProviderException |
| `infrastructure/upstream/OpenAIUpstreamClient.java` | 修改 | 集成 ErrorClassifier，SSE 错误格式化 |
| `infrastructure/upstream/AnthropicUpstreamClient.java` | 修改 | 集成 ErrorClassifier，SSE 错误格式化 |
| `infrastructure/upstream/ErrorClassificationStrategy.java` | **新增** | 错误分类策略接口 |
| `infrastructure/upstream/OpenAIErrorClassifier.java` | **新增** | OpenAI 错误分类实现 |
| `infrastructure/upstream/AnthropicErrorClassifier.java` | **新增** | Anthropic 错误分类实现 |
| `infrastructure/resilience/RetryExecutor.java` | 修改 | 内部集成策略选择 |
| `infrastructure/resilience/RetryStrategy.java` | **新增** | 重试策略接口 |
| `infrastructure/resilience/ExponentialBackoffStrategy.java` | **新增** | 默认退避策略 |
| `infrastructure/resilience/RateLimitRetryStrategy.java` | **新增** | 限流退避策略 |
| `infrastructure/resilience/FastRetryStrategy.java` | **新增** | 快速重试策略 |
| `infrastructure/resilience/ServiceUnavailableStrategy.java` | **新增** | 服务不可用重试策略 |
| `infrastructure/resilience/ResilientUpstreamClient.java` | 修改 | 添加 Metrics 埋点 |
| `infrastructure/config/GatewayRetryProperties.java` | 修改 | 扩展差异化配置 |
| `application/proxy/ChatDispatchServiceImpl.java` | 修改 | 故障转移、异常上下文注入、降级集成 |
| `application/degradation/DegradationService.java` | **新增** | 智能降级接口 |
| `application/degradation/DegradationServiceImpl.java` | **新增** | 智能降级实现 |
| `application/degradation/DegradationEvent.java` | **新增** | 降级事件 |
| `adapter/advice/GlobalExceptionHandler.java` | 修改 | 适配新异常类型 |
