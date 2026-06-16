---
comet_change: llm-provider-simulator
role: technical-design
canonical_spec: openspec
archived-with: 2026-06-16-llm-provider-simulator
status: final
---

# LLM Provider Simulator 技术设计

## 1. 概述

为 OpenAIUpstreamClient 和 AnthropicUpstreamClient 提供基于 MockWebServer 的 HTTP 层单元测试，并为开发者提供独立运行的模拟服务，无需真实 API Key 即可验证 Gateway 集成。

## 2. 架构

```
第一阶段（测试工具包）                   第二阶段（独立模拟服务）
gateway-boot/src/test/                  gateway-simulator/

┌──────────────────┐                    ┌─────────────────────────┐
│ ResponseTemplates │                    │ SimulatorController      │
│ (纯静态 JSON)     │◄──复制──►         │ POST /v1/chat/completions│
└──────┬───────────┘                    │ POST /v1/messages        │
       │                                └───────────┬─────────────┘
┌──────▼───────────┐                    ┌───────────▼─────────────┐
│ ProviderSimulator │                    │ SimulatorModeService     │
│ (MockWebServer)   │                    │ mode + requestLog       │
└──────┬───────────┘                    └─────────────────────────┘
       │                                ┌─────────────────────────┐
┌──────▼───────────┐                    │ SimulatorAdminController│
│ UpstreamClient   │                    │ POST /simulator/mode    │
│ Test (×2)        │                    │ GET  /simulator/requests│
└──────────────────┘                    └─────────────────────────┘
                                        ┌─────────────────────────┐
                                        │ SimulatorResponse       │
                                        │ Templates (复制)        │
                                        └─────────────────────────┘
```

## 3. 第一阶段：测试工具包

### 3.1 ResponseTemplates

路径：`gateway-boot/src/test/java/com/codingas/gateway/support/ResponseTemplates.java`

纯静态工具类，方法均返回 `String`：

| 方法 | 返回内容 |
|------|---------|
| `openaiChatCompletion()` | OpenAI Chat Completion JSON（id、model、choices、usage） |
| `openaiStreamChunks()` | SSE 格式：3 个 `data:` 块 + `data: [DONE]` 终止标记 |
| `openaiError(int code)` | OpenAI 错误 JSON（`error.type`、`error.message`） |
| `anthropicMessages()` | Anthropic Messages JSON（id、model、content、usage） |
| `anthropicStreamChunks()` | SSE 格式：`event: content_block_delta` + `data:` 块 × 3 + `event: message_stop` |
| `anthropicError(int code)` | Anthropic 错误 JSON（`error.type`、`error.message`） |

### 3.2 ProviderSimulator

路径：`gateway-boot/src/test/java/com/codingas/gateway/support/ProviderSimulator.java`

实现 `AutoCloseable`，支持 try-with-resources。

**核心方法：**

```java
public class ProviderSimulator implements AutoCloseable {
    /** 创建并启动模拟服务器 */
    static ProviderSimulator create();

    /** 获取 base URL */
    String getUrl();

    /** 入队 OpenAI 成功响应 */
    void enqueueOpenAISuccess();
    /** 入队 Anthropic 成功响应 */
    void enqueueAnthropicSuccess();
    /** 入队 SSE 流式响应 */
    void enqueueStream(String sseBody);
    /** 入队错误响应 */
    void enqueueError(int statusCode, String errorBody);
    /** 入队超时响应（body 延迟 > 客户端 timeout） */
    void enqueueTimeout();

    /** 取录制的请求 */
    RecordedRequest takeRequest();

    /** 创建指向模拟器的 OpenAIUpstreamClient */
    OpenAIUpstreamClient createOpenAIIClient(String apiKey, int timeout);
    /** 创建指向模拟器的 AnthropicUpstreamClient */
    AnthropicUpstreamClient createAnthropicClient(String apiKey, int timeout);

    @Override void close();
}
```

**内部依赖：**
- `OkHttpClient`（共享实例，使用默认配置）
- `ObjectMapper`（共享实例，使用 Spring Boot 默认配置）
- `ErrorClassificationStrategy`：`createOpenAIIClient()` 使用 `OpenAIErrorClassifier`，`createAnthropicClient()` 使用 `AnthropicErrorClassifier`

### 3.3 流式测试模式

使用 `CountDownLatch` + `AtomicBoolean` + `CopyOnWriteArrayList` 处理异步回调：

```java
CountDownLatch latch = new CountDownLatch(1);
List<String> chunks = new CopyOnWriteArrayList<>();
AtomicBoolean completed = new AtomicBoolean(false);

StreamCallback callback = new StreamCallback() {
    @Override public void onChunk(String data) { chunks.add(data); }
    @Override public void onComplete() { completed.set(true); latch.countDown(); }
    @Override public void onError(Throwable t) { latch.countDown(); }
};

client.chatStream(request, callback);
assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
assertThat(completed).isTrue();
assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
```

### 3.4 测试覆盖场景

**OpenAIUpstreamClientTest**（~8 场景）：

| 场景 | 模拟器行为 | 期望结果 |
|------|-----------|---------|
| 非流式正常调用 | 200 + openaiChatCompletion | 返回 OpenAIChatResponse，字段正确 |
| 非流式请求验证 | 200 + openaiChatCompletion | 路径=`/v1/chat/completions`，Header 含 `Authorization: Bearer` |
| 流式正常调用 | SSE + openaiStreamChunks | onChunk 被调用 ≥2 次，onComplete 被调用 |
| 429 限流 | 429 + openaiError(429) | ProviderException(RATE_LIMIT_ERROR) |
| 401 鉴权失败 | 401 + openaiError(401) | ProviderException(AUTHENTICATION_ERROR) |
| 500 服务端错误 | 500 + openaiError(500) | ProviderException(UPSTREAM_ERROR) |
| 超时 | 超长延迟 | ProviderException(TIMEOUT_ERROR) |
| 连通性测试 | 200 (GET /v1/models) | ConnectivityTestResult(success=true) |

**AnthropicUpstreamClientTest**（~8 场景）：结构同上，差异点：
- 路径 = `/v1/messages`
- Header 含 `x-api-key` + `anthropic-version`
- SSE 解析 `event: message_stop` 终止标记
- 连通性测试使用 POST + 非 5xx 判成功

## 4. 第二阶段：独立模拟服务

### 4.1 模块结构

```
gateway-simulator/
├── pom.xml
└── src/
    ├── main/java/com/codingas/simulator/
    │   ├── LLMProviderSimulatorApplication.java
    │   ├── controller/
    │   │   ├── SimulatorController.java
    │   │   └── SimulatorAdminController.java
    │   ├── service/
    │   │   └── SimulatorModeService.java
    │   └── template/
    │       └── SimulatorResponseTemplates.java
    ├── main/resources/
    │   └── application.yml
    └── test/java/com/codingas/simulator/
        └── SimulatorIntegrationTest.java
```

### 4.2 SimulatorModeService

```java
public enum SimulatorMode { NORMAL, RATE_LIMITED, FAULT }

@Service
public class SimulatorModeService {
    private volatile SimulatorMode mode = SimulatorMode.NORMAL;
    private final EvictingQueue<RequestRecord> requestLog; // 环形缓冲

    public SimulatorMode getMode();
    public void setMode(SimulatorMode mode);
    public void recordRequest(RequestRecord record);
    public List<RequestRecord> getRequestLog();
}

public record RequestRecord(String method, String path, Instant timestamp) {}
```

### 4.3 SimulatorController

| 端点 | 模式 | 响应 |
|------|------|------|
| POST `/v1/chat/completions` | NORMAL | 200 + openaiChatCompletion JSON |
| POST `/v1/chat/completions` | RATE_LIMITED | 429 + 限流错误 JSON |
| POST `/v1/chat/completions` | FAULT | 500 + 服务器错误 JSON |
| POST `/v1/messages` | NORMAL | 200 + anthropicMessages JSON |
| POST `/v1/messages` | RATE_LIMITED | 429 + 限流错误 JSON |
| POST `/v1/messages` | FAULT | 500 + 服务器错误 JSON |

**流式支持**：请求 Body 含 `"stream": true` 时：
- Content-Type: `text/event-stream`
- 使用 `SseEmitter` 逐个发送 SSE 事件
- OpenAI → `data:` 块 + `data: [DONE]`
- Anthropic → `event:` + `data:` 块 + `event: message_stop`

### 4.4 SimulatorAdminController

| 端点 | 方法 | 说明 |
|------|------|------|
| `/simulator/mode` | POST | 切换模式，Body: `{"mode": "normal"\|"rate_limited"\|"fault"}` |
| `/simulator/requests` | GET | 返回最近 N 条请求记录 |

### 4.5 配置

```yaml
simulator:
  port: 9090
  mode: normal
  request-log-capacity: 100

server:
  port: ${simulator.port}
```

### 4.6 Maven 依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- 无 JPA、无 Redis、无 Security -->
</dependencies>
```

## 5. 测试策略

| 层级 | 范围 | 工具 |
|------|------|------|
| 第一阶段单元测试 | OpenAIUpstreamClient × 8、AnthropicUpstreamClient × 8 | MockWebServer + CountDownLatch |
| 第二阶段单元测试 | SimulatorModeService（模式切换+请求记录） | Mockito |
| 第二阶段集成测试 | SimulatorIntegrationTest（Spring Boot 启动 + 全模式 + 流式） | Spring Boot Test + TestRestTemplate |
| 回归 | 全模块 | `./mvnw clean test` |

## 6. 关键取舍

| 取舍 | 理由 |
|------|------|
| MockWebServer 而非 WireMock | 零新增依赖，项目已引入 |
| 响应模板硬编码 | 稳定、可读、无文件 I/O |
| 模板复制而非共享模块 | 纯字符串常量，重复可接受；避免测试 jar 依赖 |
| CountDownLatch 而非 CompletableFuture | 简单直接，与现有测试风格一致 |
| 不支持延迟模拟 | YAGNI — 超时场景由第一阶段覆盖 |
| 轻量 Controller 而非 Dispatcher 模式 | 两个协议不需要抽象层 |
