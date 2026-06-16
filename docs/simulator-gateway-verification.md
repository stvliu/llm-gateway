# 通过 LLM Provider Simulator 验证 Gateway

## 概述

本文档系统性地描述了如何通过 [LLM Provider Simulator](../gateway-simulator/quickstart.md) 验证 LLM-Gateway 的**功能完整性、异常完整性、可用性、稳定性、鲁棒性**五个维度。

## Gateway 韧性链路全景

```
请求进入
    │
    ▼
┌──────────────────────┐
│  安全拦截器链          │  ← 认证/鉴权/限流/脱敏（独立于上游）
├──────────────────────┤
│  ChatDispatchService  │  ← 七阶段调度
├──────────────────────┤
│  DegradationInvoker   │  ← 降级保护（模型降级）
├──────────────────────┤
│  KeyFailoverInvoker   │  ← Key 级故障转移
├──────────────────────┤
│  ResilientUpstream    │  ← 熔断器 + 重试
│  Client               │
├──────────────────────┤
│  UpstreamClient       │  ← HTTP 调用上游
│  (OpenAI/Anthropic)   │
└──────────────────────┘
         │
         ▼
    Simulator ──→ 返回 200/429/401/500/超时/SSE 流
```

## 当前 Simulator 能力

| 模式 | HTTP 状态 | 用途 |
|------|-----------|------|
| `NORMAL` | 200 + 正常 JSON | 正常路径验证 |
| `RATE_LIMITED` | 429 + 限流错误 | 限流场景 |
| `FAULT` | 500 + 服务器错误 | 服务端错误 |

## 一、功能完整性验证

### 1.1 双协议非流式

| 场景 | 模拟器配置 | 验证点 |
|------|-----------|--------|
| OpenAI → OpenAI | mode=NORMAL, POST /v1/chat/completions | dispatch 返回 OpenAIChatResponse |
| Anthropic → Anthropic | mode=NORMAL, POST /v1/messages | dispatch 返回 AnthropicMessagesResponse |
| OpenAI → Anthropic（跨协议） | endpoint 指向模拟器, upstreamProtocol=anthropic | 协议转换 + 响应转换 |
| Anthropic → OpenAI（跨协议） | endpoint 指向模拟器, upstreamProtocol=openai | 协议转换 + 响应转换 |

### 1.2 双协议流式

| 场景 | 模拟器配置 | 验证点 |
|------|-----------|--------|
| OpenAI 流式 | stream=true, SSE 返回 | onChunk 收到 data 块, onComplete 收到 [DONE] |
| Anthropic 流式 | stream=true, SSE 返回 | onChunk 收到 data 块, onComplete 收到 message_stop |
| 跨协议流式 | stream=true + 协议转换 | 流式 chunk 被正确转换 |

### 1.3 Token 计量

模拟器返回的 JSON 中包含 `usage` 字段，Gateway 的 `publishTokenUsedEvent` 会解析并发布事件：

```json
{"usage": {"prompt_tokens": 10, "completion_tokens": 8, "total_tokens": 18}}
```

**验证点**：TokenUsedEvent 被正确发布，prompt/completion 数值正确。

## 二、异常完整性验证

### 2.1 错误分类验证

Gateway 的 `OpenAIErrorClassifier` 和 `AnthropicErrorClassifier` 根据 HTTP 状态码和响应体将错误映射为 `ProviderErrorType`：

| HTTP 状态 | 响应体特征 | ProviderErrorType |
|-----------|-----------|-------------------|
| 401 | authentication_error | AUTHENTICATION_ERROR |
| 429 | insufficient_quota | QUOTA_EXCEEDED |
| 429 | rate_limit_error | RATE_LIMIT_ERROR |
| 400 | invalid_request | INVALID_REQUEST |
| 408/504 | timeout | TIMEOUT_ERROR |
| 500/502 | server_error | UPSTREAM_ERROR |
| 503 | service_unavailable | SERVICE_UNAVAILABLE |
| 529 (Anthropic) | overloaded | UPSTREAM_ERROR |

### 2.2 重试行为验证

`RetryExecutor` 根据 `GatewayRetryProperties.retryableStatusCodes` 决定是否重试：

| ProviderErrorType | 是否重试 | 预期行为 |
|------------------|---------|---------|
| RATE_LIMIT_ERROR | 是 (429 ∈ retryable) | 最多重试 N 次后抛出 |
| UPSTREAM_ERROR | 是 (500 ∈ retryable) | 最多重试 N 次后抛出 |
| AUTHENTICATION_ERROR | 否 | 立即抛出 |
| QUOTA_EXCEEDED | 否 | 立即抛出 |
| TIMEOUT_ERROR | 否 | 立即抛出 |
| INVALID_REQUEST | 否 | 立即抛出 |

**验证方法**：Simulator 连续返回 429/500，验证 Gateway 重试了 N 次后最终失败。

## 三、可用性验证

### 3.1 熔断器全生命周期

`CircuitBreaker` 基于滑动窗口统计失败率，支持 CLOSED → OPEN → HALF_OPEN 状态转换：

| 阶段 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| CLOSED → OPEN | 连续返回 10 次 500（> 50% 失败率） | 第 11 次请求被 CircuitOpenException 拒绝 |
| OPEN → HALF_OPEN | 等待 30s（openDurationMs） | 第 31 次请求允许通过 |
| HALF_OPEN → CLOSED | HALF_OPEN 时返回 1 次 200 | 熔断器关闭，后续请求正常 |
| HALF_OPEN → OPEN | HALF_OPEN 时返回 1 次 500 | 熔断器重新打开 |

**需要增强**：Simulator 需要支持行为序列模式，按预定义序列返回响应。

```
POST /simulator/behavior
{"sequence": [200, 200, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500]}
```

### 3.2 Key 故障转移

`KeyFailoverInvoker` 遍历同一 Channel 下的多个 Credential（Key），跳过熔断中的端点：

| 场景 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| 多 Key 自动切换 | Gateway 配置 2 个 Key, Key1 返回 401, Key2 返回 200 | Key1 失败 → 自动切换到 Key2 |
| 全部 Key 失败 | 2 个 Key 都返回 500 | ProviderException("所有 Key 均失败") |
| 熔断跳过 | Key1 熔断中, Key2 正常 | Gateway 跳过 Key1, 使用 Key2 |

**需要增强**：Simulator 需要支持根据 API Key 返回不同响应。

### 3.3 模型降级

`DegradationInvoker` 捕获 `ProviderException` 后根据降级链切换到备选模型：

| 场景 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| 主模型失败自动降级 | gpt-4 返回 500, 降级后 gpt-3.5-turbo 返回 200 | DegradationInvoker 降级到 gpt-3.5-turbo |
| 降级链耗尽 | gpt-4 和 gpt-3.5-turbo 都返回 500 | ProviderException, 无可用降级 |

### 3.4 Provider 健康检查

`ProviderHealthTracker` 基于实际请求结果被动推断 Provider 健康状态：

| 场景 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| Provider 健康 | 连续请求成功 | ProviderHealthTracker → UP |
| Provider 不健康 | 连续 3 次失败 | ProviderHealthTracker → DOWN |
| Provider 恢复 | DOWN 后连续 2 次成功 | ProviderHealthTracker → UP |

## 四、稳定性验证

### 4.1 SSE 流中断

| 场景 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| 流中段中断 | 发送 2 个 chunk 后关闭连接 | Gateway 触发 onError |
| 流超时 | 发送 1 个 chunk 后暂停 60s | Gateway 的 SseEmitter 超时 |
| 流返回非法数据 | 发送非法 JSON chunk | Gateway 正确处理 |

### 4.2 慢响应

| 场景 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| 慢但成功 | 延迟 5s 后返回 200 | Gateway 正常处理（审计日志记录高延迟） |
| 超过客户端超时 | 延迟 65s（> 60s 超时） | TIMEOUT_ERROR |

### 4.3 间歇故障（抖动测试）

| 场景 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| 间歇 429 | [200, 200, 429, 200, 200, 429] | Gateway 重试后恢复 |
| 间歇 500 | [200, 500, 200, 500, 200] | 熔断器不触发（失败率 < 50%）, 但重试被触发 |

## 五、鲁棒性验证

### 5.1 非法请求

| 场景 | Gateway 发送 | Simulator 验证点 |
|------|-------------|-----------------|
| 非法 JSON | 损坏的请求体 | 返回 400, Gateway 正确处理 |
| 超大请求 | >10MB 请求体 | Gateway 的请求大小限制 |
| 缺少必填字段 | model 为空 | Gateway 的协议校验器拦截 |
| Content-Type 错误 | text/plain | Gateway 的拦截器或 Controller 处理 |

### 5.2 边界 SSE

| 场景 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| 空 SSE | data: 空行 | Gateway 忽略 |
| 不完整 SSE | data: 无 [DONE] 标记 | Gateway 的 onComplete 被调用 |
| 重复 [DONE] | 两个 [DONE] | Gateway 不会崩溃 |

## 六、Simulator 增强路线图

### Phase 1：增强 Simulator 模式

扩展 `SimulatorModeService.SimulatorMode`：

```java
public enum SimulatorMode {
    NORMAL,
    AUTH_ERROR,       // 401
    RATE_LIMITED,     // 429
    QUOTA_EXCEEDED,   // 429 + quota body
    INVALID_REQUEST,  // 400
    UPSTREAM_ERROR,   // 500
    SERVICE_DOWN,     // 503
    TIMEOUT,          // 延迟超时
    INTERMITTENT      // 行为序列
}
```

扩展管理 API：

```
POST /simulator/behavior       Body: {"sequence": [200, 200, 500, 500, ...]}
POST /simulator/delay          Body: {"delayMs": 5000}
POST /simulator/stream         Body: {"action": "interrupt_after", "chunks": 2}
```

### Phase 2：Gateway 集成测试

创建全链路集成测试，启动完整 Spring Boot 上下文，将 Provider endpoint 指向 Simulator：

```
GatewayIntegrationTest
├── testNormalChat()              // 正常非流式
├── testNormalStream()            // 正常流式
├── testRateLimit_retried()       // 429 → 重试
├── testAuthError_notRetried()    // 401 → 不重试
├── testCircuitBreaker_opens()    // 连续失败 → 熔断
├── testCircuitBreaker_halfOpen() // 熔断 → 半开 → 恢复
├── testKeyFailover()             // Key1 失败 → Key2 成功
├── testDegradation()             // gpt-4 失败 → gpt-3.5 成功
├── testProtocolConversion()      // OpenAI → Anthropic 跨协议
├── testStreamInterrupted()       // 流中断
├── testTimeout()                 // 超时
└── testLongRunningStability()    // 长时间运行（100 次调用）
```

### Phase 3：混沌测试（可选）

基于增强后的 Simulator 构建自动化混沌测试套件：

```
ChaosTestSuite
├── randomFailure()               // 随机返回 200/429/500
├── slowThenFast()                // 先慢后快
├── flakyConnection()             // 间歇断开
├── peakLoad()                    // 高并发（100 并发请求）
└── longHaul()                    // 1 小时持续运行
```

## 七、验证优先级矩阵

| 优先级 | 增强项 | 理由 |
|--------|--------|------|
| **P0** | 增加 AUTH_ERROR / QUOTA_EXCEEDED / UPSTREAM_ERROR / TIMEOUT 模式 | 覆盖完整的错误分类映射和重试/不重试策略 |
| **P0** | 支持行为序列（INTERMITTENT 模式） | 熔断器生命周期、间歇故障验证 |
| **P1** | 支持按 API Key 区分响应 | Key 故障转移验证 |
| **P1** | 流式中断模拟 | SSE 稳定性验证 |
| **P2** | Gateway 全链路集成测试 | 端到端验证所有韧性组件协同工作 |
| **P3** | 混沌测试套件 | 长时间运行稳定性 |
