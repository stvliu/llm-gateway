---
comet_change: simulator-verification-enhancement
role: technical-design
canonical_spec: openspec
archived-with: 2026-06-17-simulator-verification-enhancement
status: final
---

# Simulator 验证增强 — 技术设计文档

## 1. 概述

本文档描述了 gateway-simulator 的增强设计方案，使其能够模拟完整的错误场景和韧性行为，并基于增强后的 Simulator 编写 Gateway 全链路集成测试。

### 1.1 背景

当前 gateway-simulator 仅有 `NORMAL` / `RATE_LIMITED` / `FAULT` 三种模拟模式，无法覆盖 Gateway 韧性组件的完整验证场景（熔断器生命周期、Key 故障转移、SSE 稳定性等）。

### 1.2 目标

1. 扩展 Simulator 模式至 9 种，覆盖完整的错误分类映射
2. 增加行为序列能力，支持熔断器生命周期验证
3. 增加延迟/流控制/API Key 覆盖等高级模拟能力
4. 基于增强后的 Simulator 编写 Gateway 全链路集成测试

## 2. 架构设计

### 2.1 SimulatorModeService 增强

`SimulatorModeService` 从单一模式持有者升级为多策略模式分发中心：

```
SimulatorModeService
├── globalMode: SimulatorMode          ← 全局模式（现有）
├── behaviorSequence: BehaviorSequence ← 行为序列（新增）
├── delayConfig: DelayConfig           ← 延迟配置（新增）
├── streamConfig: StreamConfig         ← 流配置（新增）
├── apiKeyOverrides: Map<String, SimulatorMode>  ← API Key 覆盖（新增）
│
├── resolveMode(request): SimulatorMode
│   ├── 1. 检查 behaviorSequence → 有则消费步进
│   ├── 2. 检查 apiKeyOverrides → 匹配则返回覆盖模式
│   └── 3. 回退 globalMode
│
└── applyDelay(): void
    └── delayConfig.isActive → Thread.sleep(delayMs)
```

### 2.2 SimulatorMode 枚举

```java
public enum SimulatorMode {
    NORMAL,             // 200 + 正常 JSON
    AUTH_ERROR,         // 401 + authentication_error
    RATE_LIMITED,       // 429 + rate_limit_error
    QUOTA_EXCEEDED,     // 429 + insufficient_quota
    INVALID_REQUEST,    // 400 + invalid_request
    UPSTREAM_ERROR,     // 500 + server_error
    SERVICE_DOWN,       // 503 + service_unavailable
    TIMEOUT,            // 延迟后 408 + timeout
    INTERMITTENT        // 委托给 BehaviorSequence
}
```

### 2.3 BehaviorSequence

数据模型：

```java
public class BehaviorSequence {
    private List<Integer> steps;        // HTTP 状态码序列
    private boolean loop;               // 是否循环
    private int currentIndex;           // 当前步进
    private boolean active;             // 是否有活跃序列

    public Optional<SimulatorMode> consume();
    public void reset();
}
```

状态码映射规则：

| HTTP 状态码 | SimulatorMode |
|-------------|---------------|
| 200 | NORMAL |
| 401 | AUTH_ERROR |
| 429 | RATE_LIMITED |
| 400 | INVALID_REQUEST |
| 500 | UPSTREAM_ERROR |
| 503 | SERVICE_DOWN |
| 408 | TIMEOUT |

### 2.4 延迟配置

```java
public class DelayConfig {
    private long fixedDelayMs;  // 固定延迟毫秒数
    private boolean active;     // 是否启用

    public void applyDelay() { Thread.sleep(fixedDelayMs); }
}
```

### 2.5 流控制

```java
public class StreamConfig {
    private String action;           // normal / interrupt_after / invalid_data / duplicate_done / empty_chunk / incomplete
    private int chunkCount;          // chunk 数量
    private int chunkIntervalMs;     // chunk 间隔毫秒
    private int interruptAfter;      // 中断前发送的 chunk 数
    private String invalidChunk;     // 非法数据内容
}
```

### 2.6 API Key 覆盖

```java
public class ApiKeyOverrideConfig {
    private final Map<String, SimulatorMode> overrides = new ConcurrentHashMap<>();

    public void setOverride(String keyPrefix, SimulatorMode mode);
    public void removeOverride(String keyPrefix);
    public void clearAll();
    public Optional<SimulatorMode> matchOverride(String apiKey);
}
```

匹配逻辑：提取请求头 `Authorization: Bearer <key>`，遍历所有覆盖规则，检查 key 是否以 keyPrefix 开头。

## 3. API 设计

### 3.1 新增管理 API

| 方法 | 路径 | 请求体 | 功能 |
|------|------|--------|------|
| POST | `/simulator/behavior` | `{sequence: [200,500,...], loop: false}` | 设置行为序列 |
| GET | `/simulator/behavior` | - | 获取当前序列状态 |
| DELETE | `/simulator/behavior` | - | 清除行为序列 |
| POST | `/simulator/delay` | `{delayMs: 5000}` | 设置响应延迟 |
| DELETE | `/simulator/delay` | - | 清除延迟 |
| GET | `/simulator/delay` | - | 获取当前延迟配置 |
| POST | `/simulator/stream` | `{action: "interrupt_after", chunks: 2}` | 配置流行为 |
| POST | `/simulator/apikey-override` | `{keyPrefix: "sk-...", mode: "auth_error"}` | 设置 Key 覆盖 |
| DELETE | `/simulator/apikey-override/{keyPrefix}` | - | 清除指定 Key 覆盖 |
| DELETE | `/simulator/apikey-override` | - | 清除所有 Key 覆盖 |
| GET | `/simulator/apikey-override` | - | 获取所有覆盖规则 |

### 3.2 Simulator 现有 API 变更

`POST /simulator/mode` 的 parseMode 方法扩展支持新枚举值：`auth_error` / `quota_exceeded` / `invalid_request` / `upstream_error` / `service_down` / `timeout` / `intermittent`。

## 4. 控制器响应流程

### 4.1 非流式请求流程

```
SimulatorController.openaiChatCompletions(body)
  → modeService.recordRequest("POST", "/v1/chat/completions")
  → mode = modeService.resolveMode(request)  // 行为序列 → API Key → 全局
  → delayConfig.applyDelay()                  // 如有延迟则等待
  → switch(mode):
      NORMAL         → 200 + openaiChatCompletion()
      AUTH_ERROR     → 401 + openaiAuthError()
      RATE_LIMITED   → 429 + openaiRateLimitError()
      QUOTA_EXCEEDED → 429 + openaiQuotaExceeded()
      INVALID_REQUEST→ 400 + openaiInvalidRequest()
      UPSTREAM_ERROR → 500 + openaiServerError()
      SERVICE_DOWN   → 503 + openaiServiceDown()
      TIMEOUT        → 408 + openaiTimeoutError()
```

### 4.2 流式请求流程

```
SimulatorController.openaiChatCompletions(body)  [stream=true]
  → resolveMode()
  → delayConfig.applyDelay()
  → if 错误模式 → 返回 JSON 错误（非 SSE，保持向后兼容）
  → if NORMAL → 创建 SseEmitter
    → 根据 streamConfig 控制 SSE 发送：
      - normal: chunkCount 个 chunk + [DONE]
      - interrupt_after: interruptAfter 个 chunk → 关闭连接
      - invalid_data: 发送非法 JSON chunk
      - duplicate_done: 正常 chunk + 两次 [DONE]
      - empty_chunk: 发送空 data 行
      - incomplete: 正常 chunk → 不发送 [DONE]
```

## 5. 集成测试架构

### 5.1 Maven 依赖

在 `gateway-boot/pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-simulator</artifactId>
    <scope>test</scope>
</dependency>
```

### 5.2 测试配置

集成测试使用 `@SpringBootTest` 同时启动 Gateway 和 Simulator：

```java
@SpringBootTest(
    classes = {GatewayApplication.class, LLMProviderSimulatorApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "simulator.mode=normal",
        // 缩短熔断器参数加速测试
        "gateway.circuit-breaker.open-duration-ms=100",
        "gateway.circuit-breaker.sliding-window-size=5",
        "gateway.circuit-breaker.failure-rate-threshold=0.5",
        // 缩短超时配置
        "gateway.http-client.timeout=2s",
        // 禁用 Flyway（集成测试不需要数据库）
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        // Gateway 的 Provider endpoint 指向 Simulator
        "gateway.providers.openai.endpoint=http://localhost:${simulator.port}/v1",
        "gateway.providers.anthropic.endpoint=http://localhost:${simulator.port}/v1"
    }
)
class SimulatorGatewayIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int gatewayPort;

    // 通过 TestRestTemplate 调用 Gateway，Gateway 调用 Simulator
}
```

### 5.3 测试场景

| # | 测试方法 | Simulator 配置 | 预期 |
|---|---------|---------------|------|
| 1 | testNormalChat | 全局 NORMAL | 200 + ChatCompletionResponse |
| 2 | testNormalStream | 全局 NORMAL + stream | SSE 流式响应 |
| 3 | testRateLimit_retried | 全局 RATE_LIMITED | 重试后 RATE_LIMIT_ERROR |
| 4 | testAuthError_notRetried | 全局 AUTH_ERROR | 立即 AUTHENTICATION_ERROR |
| 5 | testUpstreamError_retried | 全局 UPSTREAM_ERROR | 重试后 UPSTREAM_ERROR |
| 6 | testCircuitBreaker_open | sequence: [500×10] | 第 11 次 CircuitOpenException |
| 7 | testCircuitBreaker_halfOpen | sequence: [500×10, 200] | OPEN → HALF_OPEN → CLOSED |
| 8 | testKeyFailover | Key1→AUTH_ERROR, Key2→NORMAL | Key1 失败 → Key2 成功 |
| 9 | testDegradation | 主模型 UPSTREAM_ERROR | 降级到备选模型 |
| 10 | testProtocolConversion | 跨协议配置 | 协议转换正确 |
| 11 | testTimeout | delay=5s, Gateway timeout=2s | TIMEOUT_ERROR |
| 12 | testStreamInterrupted | stream interrupt_after=2 | onError 被触发 |

## 6. 文件变更清单

### Phase 1：Simulator 增强（gateway-simulator）

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `service/SimulatorModeService.java` | 修改 | 扩展枚举，增加 resolveMode/applyDelay 方法 |
| `service/BehaviorSequence.java` | 新增 | 行为序列数据模型和逻辑 |
| `service/DelayConfig.java` | 新增 | 延迟配置 |
| `service/StreamConfig.java` | 新增 | 流控制配置 |
| `service/ApiKeyOverrideConfig.java` | 新增 | API Key 覆盖配置 |
| `controller/SimulatorController.java` | 修改 | 新增模式分发，延迟应用，流控制 |
| `controller/SimulatorAdminController.java` | 修改 | 新增管理端点 |
| `template/SimulatorResponseTemplates.java` | 修改 | 新增错误模板 |
| `service/RequestRecord.java` | 不变 | |
| `LLMProviderSimulatorApplication.java` | 不变 | |

### Phase 2：Gateway 集成测试（gateway-boot）

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `pom.xml` | 修改 | 添加 gateway-simulator test dependency |
| `src/test/.../integration/SimulatorGatewayIntegrationTest.java` | 新增 | 全链路集成测试 |

## 7. 风险与缓解

| 风险 | 缓解方案 |
|------|---------|
| Spring Boot 两个应用 Bean 冲突 | 使用不同 `@SpringBootApplication` 扫描路径，Simulator 的包为 `com.codingas.simulator`，Gateway 为 `com.codingas.gateway` |
| 集成测试执行时间长 | 缩短熔断器参数（openDurationMs=100ms, slidingWindowSize=5），缩短超时（2s） |
| 流测试异步不稳定 | 使用 Awaitility 等待异步完成 |
| Simulator 的 SSE executor 在测试后未清理 | 使用 `@DirtiesContext` 或 `@AfterEach` 清理 |
