# LLM Provider Simulator 快速入门

## 简介

LLM Provider Simulator 是一个轻量级的上游大模型服务模拟器，无需真实 API Key 即可模拟 OpenAI 和 Anthropic 兼容的 HTTP 端点，帮助开发者快速验证 Gateway 集成。

> 模拟器包含两部分：
> - **测试工具包** — 在 `gateway-boot` 单元测试中使用，基于 OkHttp MockWebServer
> - **独立服务** — 独立的 Spring Boot 应用，用于集成测试和调试

---

## 快速启动（独立服务）

### 前置条件

- JDK 21+
- Maven

### 启动

```bash
# 编译并打包
./mvnw clean package -pl gateway-simulator -DskipTests

# 启动（默认端口 9090）
java -jar gateway-simulator/target/gateway-simulator-*.jar

# 或指定端口
java -jar gateway-simulator/target/gateway-simulator-*.jar --server.port=8080
```

启动后，模拟器在 `http://localhost:9090` 上监听。

### 验证启动

```bash
# 查看当前模式
curl http://localhost:9090/simulator/mode
# 输出: {"mode":"NORMAL"}
```

---

## 测试 API 调用

### OpenAI 格式

```bash
curl http://localhost:9090/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-any-key" \
  -d '{"model":"gpt-4","messages":[{"role":"user","content":"Hello"}]}'
```

### Anthropic 格式

```bash
curl http://localhost:9090/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: sk-ant-any-key" \
  -H "anthropic-version: 2023-06-01" \
  -d '{"model":"claude-3-5-sonnet-20241022","messages":[{"role":"user","content":"Hello"}],"max_tokens":100}'
```

---

## 测试流式响应

### OpenAI SSE 流

```bash
curl http://localhost:9090/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-any-key" \
  -d '{"model":"gpt-4","messages":[{"role":"user","content":"Hello"}],"stream":true}'
```

### Anthropic SSE 流

```bash
curl http://localhost:9090/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: sk-ant-any-key" \
  -H "anthropic-version: 2023-06-01" \
  -d '{"model":"claude-3-5-sonnet-20241022","messages":[{"role":"user","content":"Hello"}],"max_tokens":100,"stream":true}'
```

---

## 模拟异常场景

模拟器支持三种模式，通过管理 API 切换：

### 限流模式（HTTP 429）

```bash
curl -X POST http://localhost:9090/simulator/mode \
  -H "Content-Type: application/json" \
  -d '{"mode":"rate_limited"}'

# 此时所有请求返回 429
curl http://localhost:9090/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4","messages":[{"role":"user","content":"Hello"}]}'
```

### 故障模式（HTTP 500）

```bash
curl -X POST http://localhost:9090/simulator/mode \
  -H "Content-Type: application/json" \
  -d '{"mode":"fault"}'

# 此时所有请求返回 500
curl http://localhost:9090/v1/messages \
  -H "Content-Type: application/json" \
  -d '{"model":"claude-3-5-sonnet-20241022","messages":[{"role":"user","content":"Hello"}],"max_tokens":100}'
```

### 恢复正常

```bash
curl -X POST http://localhost:9090/simulator/mode \
  -H "Content-Type: application/json" \
  -d '{"mode":"normal"}'
```

---

## 查看请求记录

```bash
# 查看当前模式
curl http://localhost:9090/simulator/mode

# 查看最近 100 条请求记录
curl http://localhost:9090/simulator/requests
```

---

## 配置

编辑 `gateway-simulator/src/main/resources/application.yml`：

```yaml
simulator:
  mode: normal               # 启动模式：normal / rate_limited / fault
  request-log-capacity: 100  # 请求记录保留条数

server:
  port: 9090                 # 监听端口
```

---

## 对接 Gateway

在 Gateway 管理界面创建 Provider 时，将 Endpoint URL 指向模拟器：

| 参数 | 值 |
|------|----|
| Endpoint URL | `http://localhost:9090` |
| API Key | 任意值（模拟器不校验） |
| 协议 | `openai` 或 `anthropic` |

然后在 Gateway 中正常发送请求，无需真实 API Key。

---

## 在测试中使用（第一阶段测试工具包）

### Maven 依赖

测试工具包位于 `gateway-boot/src/test/` 下，在已有测试中直接使用：

```java
import com.codingas.gateway.support.ProviderSimulator;
import com.codingas.gateway.support.ResponseTemplates;

// try-with-resources 自动管理 MockWebServer 生命周期
try (ProviderSimulator sim = ProviderSimulator.create()) {

    // 入队期望的响应
    sim.enqueueOpenAISuccess();

    // 创建指向模拟器的 OpenAI 客户端
    OpenAIUpstreamClient client = sim.createOpenAIIClient("sk-test", 30);

    // 发起请求
    ProtocolResponse resp = client.chat(request);

    // 验证请求是否按预期发送
    RecordedRequest recorded = sim.takeRequest();
    assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
    assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-test");
}
```

### 入队方法速查

| 方法 | 说明 |
|------|------|
| `enqueueOpenAISuccess()` | OpenAI 200 成功响应 |
| `enqueueAnthropicSuccess()` | Anthropic 200 成功响应 |
| `enqueueStream(String sseBody)` | SSE 流式响应 |
| `enqueueError(int statusCode, String errorBody)` | 错误响应 |
| `enqueueTimeout()` | 超时（30s 延迟） |

### 工厂方法速查

| 方法 | 说明 |
|------|------|
| `createOpenAIIClient("sk-test", 30)` | 创建 OpenAI 客户端，指向模拟器 |
| `createAnthropicClient("sk-ant-test", 30)` | 创建 Anthropic 客户端，指向模拟器 |

### 流式测试示例

```java
sim.enqueueStream(ResponseTemplates.openaiStreamChunks());

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
