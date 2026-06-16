# 模拟器验证增强 — 设计文档

## 高层架构

### Phase 1：Simulator 增强（gateway-simulator 模块）

```
┌───────────────────────────────────────────────────────────┐
│                  SimulatorModeService                      │
│                                                           │
│  ┌──────────────────┐   ┌──────────────────────────────┐ │
│  │  SimulatorMode    │   │  BehaviorSequence            │ │
│  │  (增强枚举)       │   │  - List<Step> steps          │ │
│  │                   │   │  - int currentIndex          │ │
│  │  NORMAL           │   │  - boolean loop              │ │
│  │  AUTH_ERROR       │   │                              │ │
│  │  RATE_LIMITED     │   │  getCurrentMode():            │ │
│  │  QUOTA_EXCEEDED   │   │    消费步进，返回当前模式      │ │
│  │  INVALID_REQUEST  │   │    循环/恢复全局模式          │ │
│  │  UPSTREAM_ERROR   │   └──────────────────────────────┘ │
│  │  SERVICE_DOWN     │                                     │
│  │  TIMEOUT          │   ┌──────────────────────────────┐ │
│  │  INTERMITTENT     │   │  DelayConfig                 │ │
│  └──────────────────┘   │  - long fixedDelayMs          │ │
│                         │  - boolean isActive            │ │
│  ┌──────────────────┐   └──────────────────────────────┘ │
│  │  StreamConfig    │                                     │
│  │  - action         │   ┌──────────────────────────────┐ │
│  │  - interruptAfter │   │  ApiKeyOverride              │ │
│  │  - invalidData    │   │  - Map<keyPrefix, mode>      │ │
│  └──────────────────┘   └──────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

### SimulatorMode 增强

当前 `SimulatorMode` 从 3 个值扩展到 9 个值：

```java
public enum SimulatorMode {
    NORMAL,             // 200 + 正常 JSON
    AUTH_ERROR,         // 401
    RATE_LIMITED,       // 429 + rate_limit_error
    QUOTA_EXCEEDED,     // 429 + insufficient_quota
    INVALID_REQUEST,    // 400
    UPSTREAM_ERROR,     // 500
    SERVICE_DOWN,       // 503
    TIMEOUT,            // 延迟超时
    INTERMITTENT        // 行为序列
}
```

### 响应模板增强

`SimulatorResponseTemplates` 新增错误模板：

| 模式 | 新增模板方法 | 对应 Error Type |
|------|-------------|-----------------|
| AUTH_ERROR | `openaiAuthError()` / `anthropicAuthError()` | authentication_error |
| QUOTA_EXCEEDED | `openaiQuotaExceeded()` / `anthropicQuotaExceeded()` | insufficient_quota |
| INVALID_REQUEST | `openaiInvalidRequest()` / `anthropicInvalidRequest()` | invalid_request |
| UPSTREAM_ERROR | 复用现有 server_error 模板 | server_error |
| SERVICE_DOWN | `openaiServiceDown()` / `anthropicServiceDown()` | service_unavailable |
| TIMEOUT | (延迟后返回 408) | timeout |

### 管理 API 增强

`SimulatorAdminController` 新增端点：

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/simulator/behavior` | 设置行为序列 `{sequence: [200, 401, 500], loop: false}` |
| GET | `/simulator/behavior` | 获取当前行为序列状态 |
| DELETE | `/simulator/behavior` | 清除行为序列，恢复全局模式 |
| POST | `/simulator/delay` | 设置响应延迟 `{delayMs: 5000}` |
| DELETE | `/simulator/delay` | 清除延迟配置 |
| POST | `/simulator/stream` | 控制流行为 `{action: "interrupt_after", chunks: 2}` |
| POST | `/simulator/apikey-override` | 设置 API Key 覆盖 `{keyPrefix: "sk-test-", mode: "auth_error"}` |
| DELETE | `/simulator/apikey-override/{keyPrefix}` | 清除指定 Key 的覆盖 |

### 控制器逻辑增强

`SimulatorController` 响应流程：

```
请求进入
  → 记录请求
  → 检查是否有 API Key 覆盖模式 → 有则使用覆盖模式
  → 检查是否有行为序列 → 有则消费步进获取模式
  → 获取全局模式
  → 检查是否有延迟配置 → 有则 sleep(delayMs)
  → 根据模式返回响应
    - TIMEOUT: sleep(timeoutMs) 后返回 408
    - stream + interrupt: 发送 N 个 chunk 后关闭连接
```

### Phase 2：Gateway 集成测试（gateway-boot 模块）

```
gateway-boot/src/test/java/com/codingas/gateway/integration/
└── SimulatorGatewayIntegrationTest.java    # 全链路集成测试
```

架构方式：`gateway-boot` 的 `pom.xml` 在 `test` scope 引入 `gateway-simulator` 依赖。
集成测试中启动 `LLMProviderSimulatorApplication` 的 Spring Context（随机端口），配置 Gateway 的 Provider endpoint 指向 Simulator。

```java
@SpringBootTest(
    classes = {GatewayApplication.class, LLMProviderSimulatorApplication.class},
    webEnvironment = RANDOM_PORT
)
class SimulatorGatewayIntegrationTest {
    // 使用 TestRestTemplate 调用 Gateway API
    // Gateway 配置的 provider endpoint 指向本地的 Simulator
}
```

## 数据流

```
Test (JUnit)
  │ POST /v1/chat/completions (stream=false, model=gpt-4)
  ▼
Gateway (localhost:8080)
  │ 安全拦截 → ChatDispatchService → DegradationInvoker
  │ → KeyFailoverInvoker → ResilientUpstreamClient → UpstreamClient
  ▼
Simulator (localhost:random-port)
  │ POST /v1/chat/completions
  │ 根据模式返回 200/429/401/500 + JSON/SSE
  ▼
Test 断言响应状态码、响应体、异常类型
```

## 测试场景矩阵

| 测试方法 | Simulator 配置 | Gateway 验证点 |
|---------|---------------|---------------|
| testNormalChat | 全局 NORMAL | 200 + ChatCompletionResponse |
| testNormalStream | 全局 NORMAL, stream=true | SSE 流式响应 |
| testRateLimit_retried | 全局 RATE_LIMITED | 重试 N 次后抛出 RATE_LIMIT_ERROR |
| testAuthError_notRetried | 全局 AUTH_ERROR | 立即抛出 AUTHENTICATION_ERROR，不重试 |
| testUpstreamError_retried | 全局 UPSTREAM_ERROR | 重试 N 次后抛出 UPSTREAM_ERROR |
| testCircuitBreaker_open | 行为序列: 10×500 | 第 11 次请求被 CircuitOpenException 拒绝 |
| testCircuitBreaker_halfOpen | 行为序列: 10×500 → 200 | 熔断 OPEN → HALF_OPEN → CLOSED |
| testKeyFailover | API Key 覆盖: Key1→401, Key2→200 | Key1 失败自动切换到 Key2 |
| testDegradation | 主模型 500 | 降级到备选模型 |
| testProtocolConversion | 跨协议配置 | 协议转换正确 |
| testTimeout | 全局 TIMEOUT + 延迟 | 超时错误 |
| testStreamInterrupted | 流中断配置 | 触发 onError |

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| Simulator 和 Gateway 的 Spring Context 冲突（Bean 重名）| 使用 `@SpringBootApplication(exclude=...)` 和不同包扫描路径 |
| 测试执行时间过长（熔断器等待 30s）| 测试中注入缩短后的配置属性（如 `circuitBreaker.openDurationMs=100`）|
| 流测试不稳定 | 使用 `Awaitility` 等待异步完成 |
