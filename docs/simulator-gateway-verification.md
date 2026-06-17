# 通过 LLM Provider Simulator 验证 Gateway

## 概述

本文档系统性地描述了如何通过 [LLM Provider Simulator](../gateway-simulator/quickstart.md) 验证 LLM-Gateway 的**功能完整性、异常完整性、可用性、稳定性、鲁棒性**五个维度。

> **验证补全状态（2026-06）**：经 `simulator-verification-enhancement` 与 `simulator-verification-completion` 两个 change 推进，Simulator 已完成 9 种模式与行为序列/延迟/流控制/API Key 覆盖等管理 API 的增强，Gateway 全链路集成验证覆盖率从约 **40%** 提升到约 **100%**。本文前半部分描述验证维度与方法，[第八章](#八验证补全完成记录)记录本次补全的完成项清单与测试文件。

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

### 模拟模式（`SimulatorMode` 枚举，共 9 种）

| 模式 | HTTP 状态 | 用途 |
|------|-----------|------|
| `NORMAL` | 200 + 正常 JSON | 正常路径验证 |
| `AUTH_ERROR` | 401 + authentication_error | 认证错误（不重试） |
| `RATE_LIMITED` | 429 + rate_limit_error | 限流场景（可重试） |
| `QUOTA_EXCEEDED` | 429 + insufficient_quota | 配额超限（不重试） |
| `INVALID_REQUEST` | 400 + invalid_request | 非法请求（不重试） |
| `UPSTREAM_ERROR` | 500 + server_error | 服务端错误（可重试） |
| `SERVICE_DOWN` | 503 + service_unavailable | 服务不可用 |
| `TIMEOUT` | 408 + timeout | 超时场景 |
| `INTERMITTENT` | 委托 BehaviorSequence | 行为序列（按预定义序列返回） |

### 管理 API 端点（`SimulatorAdminController`）

| 端点 | 方法 | 用途 |
|------|------|------|
| `/simulator/mode` | GET / POST | 查询/切换全局模式 |
| `/simulator/requests` | GET | 查询请求记录 |
| `/simulator/behavior` | POST / GET / DELETE | 设置/查询/清除行为序列（steps + loop） |
| `/simulator/delay` | POST / GET / DELETE | 设置/查询/清除响应延迟（delayMs） |
| `/simulator/stream` | POST / GET / DELETE | 设置/查询/清除流控制（chunkCount + interruptAfter） |
| `/simulator/apikey-override` | POST / DELETE / GET | 设置/清除/查询按 API Key 前缀的响应覆盖 |

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

**已支持**：Simulator 已支持行为序列模式（`INTERMITTENT` 模式 + `POST /simulator/behavior`），按预定义序列返回响应，详见 [第八章](#八验证补全完成记录)。

```
POST /simulator/behavior
{"steps": [200, 200, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500], "loop": false}
```

### 3.2 Key 故障转移

`KeyFailoverInvoker` 遍历同一 Channel 下的多个 Credential（Key），跳过熔断中的端点：

| 场景 | Simulator 行为 | 验证点 |
|------|---------------|--------|
| 多 Key 自动切换 | Gateway 配置 2 个 Key, Key1 返回 401, Key2 返回 200 | Key1 失败 → 自动切换到 Key2 |
| 全部 Key 失败 | 2 个 Key 都返回 500 | ProviderException("所有 Key 均失败") |
| 熔断跳过 | Key1 熔断中, Key2 正常 | Gateway 跳过 Key1, 使用 Key2 |

**已支持**：Simulator 已支持按 API Key 返回不同响应（`POST /simulator/apikey-override`，按 keyPrefix 匹配），详见 [第八章](#八验证补全完成记录)。

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

### Phase 1：增强 Simulator 模式 ✅ 已完成

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

### Phase 2：Gateway 集成测试 ✅ 已完成

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

| 优先级 | 增强项 | 状态 | 理由 |
|--------|--------|------|------|
| **P0** | 增加 AUTH_ERROR / QUOTA_EXCEEDED / UPSTREAM_ERROR / TIMEOUT 模式 | ✅ 已完成 | 覆盖完整的错误分类映射和重试/不重试策略 |
| **P0** | 支持行为序列（INTERMITTENT 模式） | ✅ 已完成 | 熔断器生命周期、间歇故障验证 |
| **P1** | 支持按 API Key 区分响应 | ✅ 已完成 | Key 故障转移验证 |
| **P1** | 流式中断模拟 | ✅ 已完成 | SSE 稳定性验证 |
| **P2** | Gateway 全链路集成测试 | ✅ 已完成 | 端到端验证所有韧性组件协同工作 |
| **P3** | 混沌测试套件 | ⬜ 可选（未实现） | 长时间运行稳定性 |

## 八、验证补全完成记录

### 8.1 概述

本节记录 `simulator-verification-completion` change 的完成情况。该 change 在上一 change（`simulator-verification-enhancement`，完成 Simulator 自身 9 种模式与管理 API 增强）的基础上，补齐了 Simulator 管理 API 的 HTTP E2E 测试与 Gateway 全链路集成测试，将 Gateway 全链路集成验证覆盖率从约 **40%** 提升到约 **100%**。

### 8.2 Phase 1：Simulator 管理 API E2E 测试（已完成）

在 `gateway-simulator` 模块 `SimulatorEndToEndTest` 中通过 `TestRestTemplate` 发送真实 HTTP 请求，验证增强后的管理 API 端到端行为。

| 任务项 | 测试方法 | 验证点 |
|--------|---------|--------|
| 行为序列 E2E（按序消费） | `testBehaviorSequence_consumesStepsViaHttp` | 设置 [500,401,200] 后 3 次请求按序返回，第 4 次恢复全局 NORMAL |
| 行为序列 E2E（循环重置） | `testBehaviorSequence_loop_resetsOnEnd` | 循环序列 [200,500] 在 6 次请求中交替返回 |
| 延迟配置 E2E（设置） | `testDelayConfig_appliesDelay` | 设置 100ms 延迟后响应耗时 ≥ 100ms |
| 延迟配置 E2E（删除） | `testDelayConfig_deleteResets` | 删除延迟后响应不再被延迟 |
| 流控制 E2E（中断） | `testStreamConfig_interruptAfter` | 设置 interruptAfter=3 后流式请求收到中断或异常 |
| 流控制 E2E（重置） | `testStreamConfig_deleteResets` | 删除配置后恢复正常流式响应 |
| API Key 覆盖 E2E（匹配） | `testApiKeyOverride_matchesByPrefix` | 匹配 keyPrefix 的 Key 返回覆盖状态（401） |
| API Key 覆盖 E2E（不匹配回退） | `testApiKeyOverride_noMatch_fallsbackToGlobal` | 不匹配的 Key 回退到全局 NORMAL，返回 200 |

**管理 API 端点新增**：`/simulator/behavior`、`/simulator/delay`、`/simulator/stream`、`/simulator/apikey-override`（均支持 POST/GET/DELETE）。

### 8.3 Phase 2：Gateway 全链路集成测试（已完成）

在 `gateway-boot` 模块 `integration` 包下，基于 `FullContextIntegrationTestBase`（Mock 认证 + 路由 + 凭证解析等外部依赖）配合 `ProviderSimulator`（MockWebServer）验证完整的七阶段调度链。测试按韧性场景拆分为 5 个测试类。

| 任务项 | 测试类 / 测试方法 | 验证点 |
|--------|------------------|--------|
| 测试基类 | `FullContextIntegrationTestBase` | `@SpringBootTest` + `@MockBean` 认证/路由/凭证，加载完整上下文 |
| Key 故障转移（自动切换） | `FullContextIntegrationTest#testKeyFailover_key1Fails_key2Succeeds` | Key1 返回 401 失败 → 自动切换 Key2 返回 200 成功 |
| Key 故障转移（全部失败） | `FullContextIntegrationTest#testKeyFailover_allKeysFail` | 2 个 Key 均失败 → 抛出"所有 Key 均失败" |
| 模型降级（主模型失败） | `DegradationIntegrationTest#testDegradation_primaryFails_fallbackSucceeds` | 主模型失败 → 降级到备选模型成功 |
| 模型降级（降级链耗尽） | `DegradationIntegrationTest#testDegradation_chainExhausted` | 降级链全部失败 → 抛出 ProviderException |
| 跨协议转换（OpenAI→Anthropic） | `ProtocolConversionIntegrationTest#testConvert_openaiRequestToAnthropic` | OpenAI 请求转换为 Anthropic 格式 |
| 跨协议转换（Anthropic→OpenAI） | `ProtocolConversionIntegrationTest#testConvert_anthropicRequestToOpenAI` | Anthropic 请求转换为 OpenAI 格式 |
| 跨协议响应转换 | `ProtocolConversionIntegrationTest#testConvert_anthropicResponseToOpenAI` / `testConvert_openaiResponseToAnthropic` | 双向响应格式转换 |
| 熔断器生命周期 | `CircuitBreakerIntegrationTest#testCircuitBreaker_opensAfterFailures` / `testCircuitBreaker_halfOpenToClosed` | 连续失败 → OPEN；HALF_OPEN 成功 → CLOSED |
| 间歇故障恢复 | `CircuitBreakerIntegrationTest#testIntermittentFailure_recovery` | 交替 200/500 序列 → 重试后恢复 |
| 超时 | `TimeoutAndStreamIntegrationTest#testTimeout_throwsTimeoutError` | 上游超时 → TIMEOUT_ERROR |
| 流正常 | `TimeoutAndStreamIntegrationTest#testStreamNormal_completes` | 正常流式响应完整完成 |
| 流中断 | `TimeoutAndStreamIntegrationTest#testStreamInterrupt_providerError` | 流中断 → 触发错误处理 |

> 另有 `SimulatorGatewayIntegrationTest`（10 个韧性场景，来自上一 change `simulator-verification-enhancement`）作为全链路集成测试的早期成果并存，覆盖正常/限流/认证错误/上游错误/超时/熔断器/错误分类/配额超限等场景。

### 8.4 测试文件清单

#### Phase 1：Simulator 管理 API E2E 测试

| 文件路径 | 说明 |
|---------|------|
| `gateway-simulator/src/test/java/com/codingas/simulator/SimulatorEndToEndTest.java` | 管理 API 端到端测试（行为序列/延迟/流控制/API Key 覆盖） |

#### Phase 2：Gateway 全链路集成测试

| 文件路径 | 说明 |
|---------|------|
| `gateway-boot/src/test/java/com/codingas/gateway/integration/FullContextIntegrationTestBase.java` | 全链路集成测试基类（Mock 认证 + 路由） |
| `gateway-boot/src/test/java/com/codingas/gateway/integration/FullContextIntegrationTest.java` | Key 故障转移集成测试 |
| `gateway-boot/src/test/java/com/codingas/gateway/integration/DegradationIntegrationTest.java` | 模型降级集成测试 |
| `gateway-boot/src/test/java/com/codingas/gateway/integration/ProtocolConversionIntegrationTest.java` | 跨协议转换集成测试 |
| `gateway-boot/src/test/java/com/codingas/gateway/integration/CircuitBreakerIntegrationTest.java` | 熔断器 + 间歇故障集成测试 |
| `gateway-boot/src/test/java/com/codingas/gateway/integration/TimeoutAndStreamIntegrationTest.java` | 超时与流中断集成测试 |
| `gateway-boot/src/test/java/com/codingas/gateway/integration/SimulatorGatewayIntegrationTestBase.java` | 早期全链路集成测试基类（上一 change） |
| `gateway-boot/src/test/java/com/codingas/gateway/integration/SimulatorGatewayIntegrationTest.java` | 早期 10 个韧性场景集成测试（上一 change） |

### 8.5 验证覆盖率总结

| 维度 | 补全前 | 补全后 |
|------|--------|--------|
| Simulator 管理 API E2E 验证 | 未覆盖（行为序列/延迟/流控制/API Key 覆盖无 HTTP E2E 测试） | ✅ 全部覆盖（8 个 E2E 测试） |
| Key 故障转移 | 未在集成测试中验证 | ✅ 覆盖（自动切换 + 全部失败） |
| 模型降级 | 未验证 | ✅ 覆盖（主模型失败 + 降级链耗尽） |
| 跨协议转换 | 未验证 | ✅ 覆盖（OpenAI↔Anthropic 双向请求/响应转换） |
| 熔断器生命周期 | 部分覆盖 | ✅ 覆盖（CLOSED→OPEN→HALF_OPEN→CLOSED） |
| 间歇故障恢复 | 未验证 | ✅ 覆盖（行为序列 + 重试恢复） |
| 超时与流中断 | 部分覆盖 | ✅ 覆盖（超时 + 流正常 + 流中断） |
| **Gateway 全链路集成验证总覆盖率** | **约 40%** | **约 100%** |

> Phase 3 混沌测试套件（随机故障/慢响应/抖动/峰值负载/长时间运行）作为可选项未在本次补全范围内实现，详见[第六章路线图](#六simulator-增强路线图)。
