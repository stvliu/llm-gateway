# 可配置管道架构重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 ChatDispatchServiceImpl 的硬编码七阶段调度链重构为可配置的 StagePipeline 模式，同步修复已识别的 10+ 缺陷。

**Architecture:** 分 5 阶段渐进式交付，每阶段可独立编译。契约加固→基础设施抽象→Pipeline+控制器→横切关注点→缺陷修复。

**Tech Stack:** Java 21, Spring Boot 3.5.x, Caffeine, OkHttp 4.12.x, Lombok, JUnit5+Mockito

**关键实际路径（与 spec 预期不同之处）：**

| 组件 | 实际路径 |
|------|---------|
| 上游客户端 | `infrastructure/supply/upstream/` |
| ProviderException | `domain/supply/exception/` |
| OpenAIController/AnthropicController | `adapter/api/`（同级，非子目录） |
| SseStreamHelper | `adapter/api/` |
| OutboundTuner | `application/proxy/` |

---

## 文件清单

```
Phase 1 — 契约层加固 (9 files)
  M  domain/protocol/contract/ProtocolRequest.java
  M  domain/protocol/contract/ProtocolResponse.java
  M  domain/protocol/contract/OpenAIChatRequest.java
  M  domain/protocol/contract/AnthropicMessagesRequest.java
  M  domain/protocol/contract/OpenAIChatResponse.java
  M  domain/protocol/contract/AnthropicMessagesResponse.java
  C  domain/protocol/contract/ProtocolContract.java
  M  domain/supply/valueobject/RoutingContext.java
  M  domain/supply/entity/Channel.java

Phase 2 — 基础设施层抽象 (6 files)
  C  infrastructure/supply/upstream/AbstractUpstreamClient.java
  M  infrastructure/supply/upstream/OpenAIUpstreamClient.java
  M  infrastructure/supply/upstream/AnthropicUpstreamClient.java
  C  infrastructure/config/HttpClientConfig.java
  M  domain/supply/exception/ProviderException.java
  M  infrastructure/resilience/ResilientUpstreamClient.java

Phase 3 — Pipeline + 控制器 (17 files)
  C  application/proxy/pipeline/Stage.java
  C  application/proxy/pipeline/StreamStage.java
  C  application/proxy/pipeline/StageContext.java
  C  application/proxy/pipeline/StagePipeline.java
  C  application/proxy/pipeline/config/PipelineConfig.java
  C  application/proxy/pipeline/stage/ProtocolIdentifyStage.java
  C  application/proxy/pipeline/stage/RoutingStage.java
  C  application/proxy/pipeline/stage/RequestConversionStage.java
  C  application/proxy/pipeline/stage/OutboundTuneStage.java
  C  application/proxy/pipeline/stage/UpstreamCallStage.java
  C  application/proxy/pipeline/stage/ResponseConversionStage.java
  C  application/proxy/pipeline/stage/AuditStage.java
  M  application/proxy/ChatDispatchServiceImpl.java
  C  adapter/api/AbstractProtocolController.java
  M  adapter/api/OpenAIController.java
  M  adapter/api/AnthropicController.java
  M  application/proxy/OutboundTuner.java

Phase 4 — 横切关注点 (4 files)
  C  application/proxy/routing/CachedRoutingResolver.java
  C  infrastructure/config/CacheConfig.java
  C  application/proxy/routing/RoutingCacheInvalidator.java
  M  全部 Gateway 接口 (JSR-305)

Phase 5 — 缺陷修复 (4 files)
  M  adapter/api/SseStreamHelper.java
  M  domain/protocol/conversion/ProtocolConverter.java
  M  application/proxy/ChatDispatchServiceImpl.java
  M  application/proxy/pipeline/stage/AuditStage.java
```

---

## Phase 1

### Task 1：ProtocolRequest 不可变改造

**Files:**
- Modify: `domain/protocol/contract/ProtocolRequest.java`
- Modify: `domain/protocol/contract/OpenAIChatRequest.java`
- Modify: `domain/protocol/contract/AnthropicMessagesRequest.java`

- [ ] **Step 1: 替换 ProtocolRequest 接口，去掉 setter**

```java
package com.codingas.gateway.domain.protocol.contract;

public interface ProtocolRequest {
    String getModel();
    String getProtocol();
    boolean isStream();

    /** 返回设置模型名后的新实例 */
    ProtocolRequest withModel(String model);

    /** 返回设置流式标记后的新实例 */
    ProtocolRequest withStream(boolean stream);
}
```

Run: `mvn compile -pl gateway-boot -q` — 预期失败（子类未实现 withXxx + 调用 setModel/setStream 处编译报错）

- [ ] **Step 2: OpenAIChatRequest 实现 withModel/withStream**

在 OpenAIChatRequest.java 中，利用 Lombok `@Builder(toBuilder = true)`：

```java
@Override
public OpenAIChatRequest withModel(String model) {
    return toBuilder().model(model).build();
}

@Override
public OpenAIChatRequest withStream(boolean stream) {
    return toBuilder().stream(stream).build();
}
```

如果当前没有 `toBuilder = true`，在 `@Builder` 注解上加上该属性。同时删除 `setModel` 和 `setStream` 方法。

- [ ] **Step 3: AnthropicMessagesRequest 实现 withModel/withStream**

同样方式：

```java
@Override
public AnthropicMessagesRequest withModel(String model) {
    return toBuilder().model(model).build();
}

@Override
public AnthropicMessagesRequest withStream(boolean stream) {
    return toBuilder().stream(stream).build();
}
```

- [ ] **Step 4: 修复编译——所有调用 request.setModel/setStream 的地方改为 withXxx**

全局搜索 `request.setModel` → 改为 `request = request.withModel(...)`。涉及文件：
- `ChatDispatchServiceImpl.java` — 无直接调用（已用 withModel），但旧代码中有 setModel
- `OpenAIUpstreamClient.java` 和 `AnthropicUpstreamClient.java` 中的 `request.setStream(true)` →
  → 这部分在 Task 5/6 中会被 AbstractUpstreamClient 替代，暂时保留编译错误，等 Phase 2 解决

Run: `mvn compile -pl gateway-boot -q`
Expected: 编译通过（Phase 2 会最终解决 UpstreamClient 的 setStream 调用）

---

### Task 2：TokenUsage record + ProtocolResponse.getUsage()

**Files:**
- Create: `domain/protocol/contract/ProtocolContract.java`
- Modify: `domain/protocol/contract/ProtocolResponse.java`
- Modify: `domain/protocol/contract/OpenAIChatResponse.java`
- Modify: `domain/protocol/contract/AnthropicMessagesResponse.java`

- [ ] **Step 1: 创建 ProtocolContract.java**

```java
package com.codingas.gateway.domain.protocol.contract;

public record TokenUsage(int promptTokens, int completionTokens) {}
```

- [ ] **Step 2: ProtocolResponse 增加 getUsage 默认方法**

```java
package com.codingas.gateway.domain.protocol.contract;

import java.util.Optional;

public interface ProtocolResponse {
    String getModel();
    String getFinishReason();

    default Optional<TokenUsage> getUsage() { return Optional.empty(); }
}
```

- [ ] **Step 3: OpenAIChatResponse 覆写**

```java
// 在 OpenAIChatResponse 类中
@Override
public Optional<TokenUsage> getUsage() {
    if (usage == null) return Optional.empty();
    return Optional.of(new TokenUsage(
        usage.getPromptTokens() != null ? usage.getPromptTokens() : 0,
        usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0
    ));
}
```

- [ ] **Step 4: AnthropicMessagesResponse 覆写**

```java
// 在 AnthropicMessagesResponse 类中
@Override
public Optional<TokenUsage> getUsage() {
    if (usage == null) return Optional.empty();
    return Optional.of(new TokenUsage(
        usage.getInputTokens() != null ? usage.getInputTokens() : 0,
        usage.getOutputTokens() != null ? usage.getOutputTokens() : 0
    ));
}
```

- [ ] **Step 5: 编写单元测试验证 getUsage**

```java
// 新建测试文件或在现有测试中追加
@Test
@DisplayName("OpenAIChatResponse.getUsage 返回正确结构")
void getUsage_returnsTokenUsage() {
    OpenAIChatResponse.Usage usage = OpenAIChatResponse.Usage.builder()
            .promptTokens(10).completionTokens(20).build();
    OpenAIChatResponse response = OpenAIChatResponse.builder().usage(usage).build();
    assertThat(response.getUsage()).isPresent();
    assertThat(response.getUsage().get().promptTokens()).isEqualTo(10);
    assertThat(response.getUsage().get().completionTokens()).isEqualTo(20);
}

@Test
@DisplayName("getUsage 在 usage=null 时返回 Optional.empty")
void getUsage_nullUsage_returnsEmpty() {
    OpenAIChatResponse response = OpenAIChatResponse.builder().build();
    assertThat(response.getUsage()).isEmpty();
}
```

Run: `mvn test -pl gateway-boot -Dtest="*OpenAIChatResponseTest" -q`
Expected: 测试通过

- [ ] **Step 6: 编译验证**

Run: `mvn compile -pl gateway-boot -q`
Expected: 通过

---

### Task 3：RoutingContext 完善 + Channel 增加 providerCode

**Files:**
- Modify: `domain/supply/valueobject/RoutingContext.java`
- Modify: `domain/supply/entity/Channel.java`

- [ ] **Step 1: RoutingContext 增加 providerCode + toString 脱敏**

如果 RoutingContext 是 Java record：

```java
public record RoutingContext(
    Long channelId,
    Long channelEndpointId,
    String endpointUrl,
    Protocol upstreamProtocol,
    String providerApiKey,
    Integer timeout,
    boolean needsProtocolAdaptation,
    String modelName,
    String upstreamModelName,
    String providerCode       // ← 新增
) {
    @Override
    public String toString() {
        String masked = providerApiKey != null
            ? providerApiKey.substring(0, Math.min(5, providerApiKey.length())) + "****"
            : null;
        return "RoutingContext[channelId=" + channelId
            + ", endpointUrl=" + endpointUrl
            + ", upstreamProtocol=" + upstreamProtocol
            + ", providerApiKey=" + masked + "]";
    }
}
```

如果是 class，用 `@Data`：

```java
// 新增字段
private String providerCode;

// toString 覆写
@Override
public String toString() {
    String masked = providerApiKey != null
        ? providerApiKey.substring(0, Math.min(5, providerApiKey.length())) + "****"
        : null;
    return "RoutingContext(channelId=" + channelId + ", providerApiKey=" + masked + ", ...)";
}
```

- [ ] **Step 2: Channel 新增 providerCode**

在 `Channel.java` 中新增：

```java
/** 提供商代号，如 "openai"、"anthropic"、"azure" */
private String providerCode;
```

- [ ] **Step 3: 修复 RoutingResolver 构建 RoutingContext 处**

在 RoutingResolver（或 endpointResolver.resolve 调用处）补充 providerCode 参数：

```java
routingContext = new RoutingContext(
    channel.getId(), endpoint.getId(), endpoint.getEndpointUrl(),
    endpoint.getProtocol(), credential, channel.getTimeout(),
    endpoint.getProtocol() != inboundProtocol,
    model.getModelName(), channelModel.getUpstreamModelName(),
    channel.getProviderCode()   // ← 补充
);
```

- [ ] **Step 4: 单元测试验证 toString 脱敏**

```java
@Test
@DisplayName("RoutingContext.toString 脱敏 API Key")
void toString_masksApiKey() {
    RoutingContext ctx = new RoutingContext(1L, 1L, "https://api.openai.com",
        Protocol.OPENAI, "sk-test-key-12345", 60, false, "gpt-4", null, "openai");
    assertThat(ctx.toString()).doesNotContain("sk-test-key-12345");
    assertThat(ctx.toString()).contains("sk-te****");
}
```

Run: `mvn test -pl gateway-boot -Dtest="*RoutingResolverTest" -q`
Expected: 通过

---

## Phase 2

### Task 4：AbstractUpstreamClient 模板方法

**Files:**
- Create: `infrastructure/supply/upstream/AbstractUpstreamClient.java`

- [ ] **Step 1: 创建基类 + 模板方法 chat**

```java
package com.codingas.gateway.infrastructure.supply.upstream;

import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractUpstreamClient implements UpstreamClient {

    private final OkHttpClient httpClient;
    private final String endpointUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    protected AbstractUpstreamClient(OkHttpClient httpClient, String endpointUrl,
                                      String apiKey, int timeoutSeconds,
                                      ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
    }

    // === 子类实现 ===
    protected abstract String getEndpointPath();
    protected abstract Map<String, String> getHeaders(String apiKey);
    protected abstract Class<? extends ProtocolResponse> getResponseType();
    protected abstract boolean isStreamComplete(String line);

    /** 逐行钩子 — Anthropic 追踪 event: 行状态 */
    protected void onStreamLine(String line) {}

    // === 模板方法（final） ===

    @Override
    public final ProtocolResponse chat(ProtocolRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS).build();
            Request httpRequest = buildRequest(json);
            try (Response response = timedClient.newCall(httpRequest).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new ProviderException("UPSTREAM_ERROR",
                            "API 调用失败: " + response.code() + " - " + body, response.code());
                }
                return objectMapper.readValue(body, getResponseType());
            }
        } catch (IOException e) {
            throw new ProviderException("UPSTREAM_ERROR", "API 调用异常", e);
        }
    }

    @Override
    public final void chatStream(ProtocolRequest request, StreamCallback callback) {
        try {
            String json = objectMapper.writeValueAsString(request);
            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS).build();
            Request httpRequest = buildRequest(json);

            timedClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (ResponseBody body = response.body()) {
                        if (!response.isSuccessful() || body == null) {
                            callback.onError(new ProviderException("UPSTREAM_ERROR",
                                    "Stream 失败: " + response.code(), response.code()));
                            return;
                        }
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            onStreamLine(line);
                            if (isStreamComplete(line)) {
                                callback.onComplete();
                                return;
                            }
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if (!data.isEmpty()) callback.onChunk(data);
                            }
                        }
                        callback.onComplete();
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            });
        } catch (IOException e) {
            callback.onError(e);
        }
    }

    private Request buildRequest(String json) {
        Request.Builder builder = new Request.Builder()
                .url(endpointUrl + getEndpointPath())
                .post(RequestBody.create(json, MediaType.parse("application/json")));
        getHeaders(apiKey).forEach(builder::addHeader);
        return builder.build();
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl gateway-boot -q`
Expected: 通过

---

### Task 5：OpenAIUpstreamClient 精简

- [ ] **Step 1: 继承 AbstractUpstreamClient，删除 chat/chatStream**

```java
package com.codingas.gateway.infrastructure.supply.upstream;

import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OpenAIUpstreamClient extends AbstractUpstreamClient {

    private static final String CHAT_PATH = "/v1/chat/completions";
    private static final String MODELS_PATH = "/v1/models";

    // 保留因 testConnectivity 所需的字段
    private final OkHttpClient httpClient;
    private final String endpointUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final int timeoutSeconds;

    public OpenAIUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
                                 int timeoutSeconds, ObjectMapper objectMapper) {
        super(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper);
        this.httpClient = httpClient;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    protected String getEndpointPath() { return CHAT_PATH; }

    @Override
    protected Map<String, String> getHeaders(String apiKey) {
        return Map.of("Authorization", "Bearer " + apiKey,
                      "Content-Type", "application/json");
    }

    @Override
    protected Class<? extends ProtocolResponse> getResponseType() { return OpenAIChatResponse.class; }

    @Override
    protected boolean isStreamComplete(String line) { return "[DONE]".equals(line.trim()); }

    @Override
    public ConnectivityTestResult testConnectivity() {
        try {
            OkHttpClient tc = httpClient.newBuilder().readTimeout(10, TimeUnit.SECONDS).build();
            Request req = new Request.Builder().url(endpointUrl + MODELS_PATH)
                    .addHeader("Authorization", "Bearer " + apiKey).get().build();
            try (Response resp = tc.newCall(req).execute()) {
                return resp.isSuccessful()
                    ? new ConnectivityTestResult(true, null, null, 0)
                    : new ConnectivityTestResult(false, null, "HTTP " + resp.code(), 0);
            }
        } catch (Exception e) {
            return new ConnectivityTestResult(false, null, e.getMessage(), 0);
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl gateway-boot -q`
Expected: 通过

---

### Task 6：AnthropicUpstreamClient 精简

- [ ] **Step 1: 继承 AbstractUpstreamClient，使用 onStreamLine 追踪 event:**

```java
package com.codingas.gateway.infrastructure.supply.upstream;

import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AnthropicUpstreamClient extends AbstractUpstreamClient {

    private static final String MESSAGES_PATH = "/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final OkHttpClient httpClient;
    private final String endpointUrl;
    private final String apiKey;
    private String currentEvent;  // 由 onStreamLine 追踪

    public AnthropicUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
                                    int timeoutSeconds, ObjectMapper objectMapper) {
        super(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper);
        this.httpClient = httpClient;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
    }

    @Override
    protected String getEndpointPath() { return MESSAGES_PATH; }

    @Override
    protected Map<String, String> getHeaders(String apiKey) {
        return Map.of("x-api-key", apiKey,
                      "anthropic-version", ANTHROPIC_VERSION,
                      "Content-Type", "application/json");
    }

    @Override
    protected Class<? extends ProtocolResponse> getResponseType() { return AnthropicMessagesResponse.class; }

    @Override
    protected boolean isStreamComplete(String line) { return "message_stop".equals(currentEvent); }

    @Override
    protected void onStreamLine(String line) {
        if (line.startsWith("event: ")) {
            currentEvent = line.substring(7).trim();
        }
    }

    @Override
    public ConnectivityTestResult testConnectivity() {
        // ... 保持现有逻辑，复制自当前 AnthropicUpstreamClient
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl gateway-boot -q`
Expected: 通过

---

### Task 7：HttpClientConfig 共享连接池

- [ ] **Step 1: 创建配置类**

```java
package com.codingas.gateway.infrastructure.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "app.http.client")
public class HttpClientConfig {

    private int maxIdleConnections = 50;
    private int keepAliveMinutes = 5;

    @Bean
    public OkHttpClient sharedHttpClient() {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveMinutes, TimeUnit.MINUTES))
                .build();
    }

    public int getMaxIdleConnections() { return maxIdleConnections; }
    public void setMaxIdleConnections(int maxIdleConnections) { this.maxIdleConnections = maxIdleConnections; }
    public int getKeepAliveMinutes() { return keepAliveMinutes; }
    public void setKeepAliveMinutes(int keepAliveMinutes) { this.keepAliveMinutes = keepAliveMinutes; }
}
```

application.yml 中可覆盖：

```yaml
app:
  http:
    client:
      max-idle-connections: 100
      keep-alive-minutes: 10
```

- [ ] **Step 2: 编译验证**

---

### Task 8：ProviderException 增强 + ResilientUpstreamClient 4xx 不熔断

- [ ] **Step 1: ProviderException 新增 statusCode + isServerError**

```java
public class ProviderException extends GatewayException {
    private final int statusCode;

    public ProviderException(String code, String message) {
        super(code, message);
        this.statusCode = 0;
    }

    public ProviderException(String code, String message, int statusCode) {
        super(code, message);
        this.statusCode = statusCode;
    }

    public ProviderException(String code, String message, Throwable cause) {
        super(code, message, cause);
        this.statusCode = 0;
    }

    public boolean isServerError() { return statusCode >= 500; }
    public int getStatusCode() { return statusCode; }
}
```

- [ ] **Step 2: ResilientUpstreamClient 替换 @Log 为 @Slf4j**

```java
// 文件顶部
import lombok.extern.slf4j.Slf4j;
@Slf4j  // 替代 @Log
public class ResilientUpstreamClient implements UpstreamClient {
```

- [ ] **Step 3: chat() 方法区分 4xx/5xx**

```java
@Override
public ProtocolResponse chat(ProtocolRequest request) {
    if (!circuitBreaker.allowRequest()) {
        throw new CircuitOpenException("熔断器开启，拒绝请求");
    }
    try {
        ProtocolResponse response = retryExecutor.execute(() -> delegate.chat(request));
        circuitBreaker.recordSuccess();
        return response;
    } catch (ProviderException e) {
        if (e.isServerError()) {
            circuitBreaker.recordFailure();
            log.warn("5xx 错误，记录熔断失败: {}", e.getMessage());
        } else {
            log.warn("4xx 错误，不触发熔断: {}", e.getMessage());
        }
        throw e;
    } catch (Exception e) {
        circuitBreaker.recordFailure();
        throw e;
    }
}
```

- [ ] **Step 4: chatStream() 同理处理 onError**

```java
@Override
public void chatStream(ProtocolRequest request, StreamCallback callback) {
    if (!circuitBreaker.allowRequest()) {
        throw new CircuitOpenException("熔断器开启，拒绝流式请求");
    }
    try {
        delegate.chatStream(request, new StreamCallback() {
            @Override
            public void onChunk(String data) { callback.onChunk(data); }

            @Override
            public void onComplete() {
                circuitBreaker.recordSuccess();
                callback.onComplete();
            }

            @Override
            public void onError(Throwable t) {
                if (t instanceof ProviderException pe && pe.isServerError()) {
                    circuitBreaker.recordFailure();
                }
                // 4xx 不 recordFailure
                callback.onError(t);
            }
        });
    } catch (Exception e) {
        if (e instanceof ProviderException pe && pe.isServerError()) {
            circuitBreaker.recordFailure();
        }
        throw e;
    }
}
```

---

## Phase 3

### Task 9：Stage 接口体系

- [ ] **Step 1: Stage.java**

```java
package com.codingas.gateway.application.proxy.pipeline;

public interface Stage {
    int order();
    String name();
    void execute(StageContext context);
}
```

- [ ] **Step 2: StreamStage.java**

```java
package com.codingas.gateway.application.proxy.pipeline;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;

public interface StreamStage extends Stage {
    void executeStream(StageContext context, StreamCallback callback);
}
```

- [ ] **Step 3: StageContext.java**

```java
package com.codingas.gateway.application.proxy.pipeline;
import com.codingas.gateway.domain.audit.entity.CallLog;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class StageContext {
    private String traceId;
    private Identity identity;
    private RoutingStrategy routingStrategy;
    private RoutingContext routingContext;
    private ProtocolRequest currentRequest;
    private ProtocolResponse currentResponse;
    private Protocol inboundProtocol;
    private CallLog callLog;
    private final Map<String, Object> attributes = new HashMap<>();
    private Throwable executionError;
}
```

- [ ] **Step 4: 编译验证**

---

### Task 10：StagePipeline 编排器

- [ ] **Step 1: 创建 StagePipeline**

```java
package com.codingas.gateway.application.proxy.pipeline;
import com.codingas.gateway.application.proxy.pipeline.stage.AuditStage;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.*;
import java.util.stream.Collectors;

public class StagePipeline {

    private static final Logger log = LoggerFactory.getLogger(StagePipeline.class);

    private final List<Stage> stages;
    private final AuditStage auditStage;

    public StagePipeline(List<Stage> stages, AuditStage auditStage) {
        this.stages = stages.stream()
                .filter(s -> !(s instanceof AuditStage))
                .sorted(Comparator.comparingInt(Stage::order))
                .collect(Collectors.toList());
        this.auditStage = auditStage;
    }

    public ProtocolResponse execute(ProtocolRequest request, Identity identity,
                                     RoutingStrategy strategy) {
        StageContext context = createContext(request, identity, strategy);
        MDC.put("traceId", context.getTraceId());
        try {
            for (Stage stage : stages) {
                log.debug("Pipeline stage: {} (order={})", stage.name(), stage.order());
                stage.execute(context);
            }
            return context.getCurrentResponse();
        } catch (Exception e) {
            context.setExecutionError(e);
            throw e;
        } finally {
            try {
                auditStage.execute(context);
            } catch (Exception e) {
                log.warn("AuditStage 异常: {}", e.getMessage());
            }
            MDC.clear();
        }
    }

    public void executeStream(ProtocolRequest request, Identity identity,
                               RoutingStrategy strategy, StreamCallback callback) {
        StageContext context = createContext(request, identity, strategy);
        MDC.put("traceId", context.getTraceId());
        try {
            boolean isStreamStage = stages.stream().anyMatch(s -> s instanceof StreamStage);
            if (isStreamStage) {
                for (Stage stage : stages) {
                    if (stage instanceof StreamStage ss) {
                        ss.executeStream(context, callback);
                        break;
                    } else {
                        stage.execute(context);
                    }
                }
            }
        } catch (Exception e) {
            context.setExecutionError(e);
            callback.onError(e);
        } finally {
            try { auditStage.execute(context); } catch (Exception e) { /* 吞没 */ }
            MDC.clear();
        }
    }

    private StageContext createContext(ProtocolRequest request, Identity identity,
                                        RoutingStrategy strategy) {
        StageContext ctx = new StageContext();
        ctx.setTraceId(UUID.randomUUID().toString());
        ctx.setIdentity(identity);
        ctx.setRoutingStrategy(strategy);
        ctx.setCurrentRequest(request);
        return ctx;
    }
}
```

- [ ] **Step 2: PipelineConfig.java**

```java
package com.codingas.gateway.application.proxy.pipeline.config;
import com.codingas.gateway.application.proxy.pipeline.*;
import com.codingas.gateway.application.proxy.pipeline.stage.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class PipelineConfig {

    @Bean
    public ProtocolIdentifyStage protocolIdentifyStage() { return new ProtocolIdentifyStage(); }
    @Bean
    public RoutingStage routingStage() { return new RoutingStage(); }
    @Bean
    public RequestConversionStage requestConversionStage() { return new RequestConversionStage(); }
    @Bean
    public OutboundTuneStage outboundTuneStage() { return new OutboundTuneStage(); }
    @Bean
    public UpstreamCallStage upstreamCallStage() { return new UpstreamCallStage(); }
    @Bean
    public ResponseConversionStage responseConversionStage() { return new ResponseConversionStage(); }
    @Bean
    public AuditStage auditStage() { return new AuditStage(); }

    @Bean
    @ConditionalOnProperty(value = "app.pipeline.enabled", matchIfMissing = true)
    public StagePipeline stagePipeline(List<Stage> stages, AuditStage auditStage) {
        return new StagePipeline(stages, auditStage);
    }
}
```

---

### Task 11：ProtocolIdentifyStage

- [ ] **Step 1: 创建阶段**

```java
package com.codingas.gateway.application.proxy.pipeline.stage;
import com.codingas.gateway.application.proxy.pipeline.Stage;
import com.codingas.gateway.application.proxy.pipeline.StageContext;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.supply.enums.Protocol;

public class ProtocolIdentifyStage implements Stage {
    @Override
    public int order() { return 100; }

    @Override
    public String name() { return "ProtocolIdentify"; }

    @Override
    public void execute(StageContext context) {
        var req = context.getCurrentRequest();
        if (req instanceof OpenAIChatRequest) {
            context.setInboundProtocol(Protocol.OPENAI);
        } else if (req instanceof AnthropicMessagesRequest) {
            context.setInboundProtocol(Protocol.ANTHROPIC);
        } else {
            throw new IllegalArgumentException("不支持的协议类型: " + req.getClass().getSimpleName());
        }
        context.setCallLog(new com.codingas.gateway.domain.audit.entity.CallLog());
        context.getCallLog().setTraceId(context.getTraceId());
        context.getCallLog().setInboundProtocol(context.getInboundProtocol().name());
    }
}
```

---

### Task 12：RoutingStage

- [ ] **Step 1: 创建阶段**

```java
package com.codingas.gateway.application.proxy.pipeline.stage;
import com.codingas.gateway.application.proxy.pipeline.Stage;
import com.codingas.gateway.application.proxy.pipeline.StageContext;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;

public class RoutingStage implements Stage {
    private final RoutingResolver routingResolver;

    public RoutingStage(RoutingResolver routingResolver) {
        this.routingResolver = routingResolver;
    }

    @Override
    public int order() { return 200; }

    @Override
    public String name() { return "Routing"; }

    @Override
    public void execute(StageContext context) {
        var req = context.getCurrentRequest();
        Protocol inbound = context.getInboundProtocol();
        RoutingContext routingCtx = routingResolver.resolve(req.getModel(), inbound);
        context.setRoutingContext(routingCtx);

        // 填充 CallLog
        var callLog = context.getCallLog();
        if (callLog != null) {
            callLog.setModel(req.getModel());
            callLog.setChannelId(routingCtx.channelId());
            callLog.setChannelEndpointId(routingCtx.channelEndpointId());
            callLog.setUpstreamProtocol(routingCtx.upstreamProtocol().name());
        }
    }
}
```

---

### Task 13：RequestConversionStage

- [ ] **Step 1: 创建阶段**

```java
package com.codingas.gateway.application.proxy.pipeline.stage;
import com.codingas.gateway.application.proxy.pipeline.Stage;
import com.codingas.gateway.application.proxy.pipeline.StageContext;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.supply.enums.Protocol;

public class RequestConversionStage implements Stage {
    private final ProtocolConverter protocolConverter;

    public RequestConversionStage(ProtocolConverter protocolConverter) {
        this.protocolConverter = protocolConverter;
    }

    @Override
    public int order() { return 300; }

    @Override
    public String name() { return "RequestConversion"; }

    @Override
    public void execute(StageContext context) {
        var ctx = context.getRoutingContext();
        if (!ctx.needsProtocolAdaptation()) return;

        var req = context.getCurrentRequest();
        Protocol inbound = context.getInboundProtocol();
        Protocol upstream = ctx.upstreamProtocol();

        if (inbound == Protocol.OPENAI && upstream == Protocol.ANTHROPIC) {
            context.setCurrentRequest(protocolConverter.toAnthropic(
                (com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest) req));
        } else if (inbound == Protocol.ANTHROPIC && upstream == Protocol.OPENAI) {
            context.setCurrentRequest(protocolConverter.toOpenAI(
                (com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest) req));
        }
    }
}
```

---

### Task 14：OutboundTuneStage

- [ ] **Step 1: 创建阶段（先不实现，依赖 OutboundTuner 改造）**

```java
package com.codingas.gateway.application.proxy.pipeline.stage;
import com.codingas.gateway.application.proxy.OutboundTuner;
import com.codingas.gateway.application.proxy.pipeline.Stage;
import com.codingas.gateway.application.proxy.pipeline.StageContext;

public class OutboundTuneStage implements Stage {
    private final OutboundTuner outboundTuner;

    public OutboundTuneStage(OutboundTuner outboundTuner) { this.outboundTuner = outboundTuner; }

    @Override
    public int order() { return 400; }

    @Override
    public String name() { return "OutboundTune"; }

    @Override
    public void execute(StageContext context) {
        var tuned = outboundTuner.tune(context.getCurrentRequest(), context.getRoutingContext());
        context.setCurrentRequest(tuned);
    }
}
```

---

### Task 15：UpstreamCallStage

- [ ] **Step 1: 创建阶段**

```java
package com.codingas.gateway.application.proxy.pipeline.stage;
import com.codingas.gateway.application.proxy.pipeline.Stage;
import com.codingas.gateway.application.proxy.pipeline.StageContext;
import com.codingas.gateway.domain.supply.gateway.ResilientClientFactory;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;

public class UpstreamCallStage implements Stage {
    private final UpstreamClientRegistry clientRegistry;
    private final ResilientClientFactory resilientClientFactory;

    public UpstreamCallStage(UpstreamClientRegistry clientRegistry,
                              ResilientClientFactory resilientClientFactory) {
        this.clientRegistry = clientRegistry;
        this.resilientClientFactory = resilientClientFactory;
    }

    @Override
    public int order() { return 500; }

    @Override
    public String name() { return "UpstreamCall"; }

    @Override
    public void execute(StageContext context) {
        var ctx = context.getRoutingContext();
        UpstreamClient rawClient = clientRegistry.getClient(
                ctx.upstreamProtocol().name().toLowerCase(),
                ctx.endpointUrl(), ctx.providerApiKey(),
                ctx.timeout() != null ? ctx.timeout() : 60);
        UpstreamClient client = resilientClientFactory.wrap(rawClient, ctx.channelEndpointId());
        context.setCurrentResponse(client.chat(context.getCurrentRequest()));
    }
}
```

---

### Task 16：ResponseConversionStage

- [ ] **Step 1: 创建阶段**

```java
package com.codingas.gateway.application.proxy.pipeline.stage;
import com.codingas.gateway.application.proxy.pipeline.Stage;
import com.codingas.gateway.application.proxy.pipeline.StageContext;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.supply.enums.Protocol;

public class ResponseConversionStage implements Stage {
    private final ProtocolConverter protocolConverter;

    public ResponseConversionStage(ProtocolConverter protocolConverter) {
        this.protocolConverter = protocolConverter;
    }

    @Override
    public int order() { return 600; }

    @Override
    public String name() { return "ResponseConversion"; }

    @Override
    public void execute(StageContext context) {
        var ctx = context.getRoutingContext();
        if (!ctx.needsProtocolAdaptation()) return;

        var response = context.getCurrentResponse();
        Protocol inbound = context.getInboundProtocol();
        Protocol upstream = ctx.upstreamProtocol();

        if (upstream == Protocol.ANTHROPIC && inbound == Protocol.OPENAI) {
            context.setCurrentResponse(protocolConverter.toOpenAI(
                (com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse) response));
        } else if (upstream == Protocol.OPENAI && inbound == Protocol.ANTHROPIC) {
            context.setCurrentResponse(protocolConverter.toAnthropic(
                (com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse) response));
        }
    }
}
```

---

### Task 17：AuditStage

- [ ] **Step 1: 创建阶段**

```java
package com.codingas.gateway.application.proxy.pipeline.stage;
import com.codingas.gateway.application.proxy.pipeline.Stage;
import com.codingas.gateway.application.proxy.pipeline.StageContext;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.audit.entity.CallLog;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import com.codingas.gateway.domain.protocol.contract.TokenUsage;
import com.codingas.gateway.domain.usage.event.TokenUsedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

public class AuditStage implements Stage {
    private static final Logger log = LoggerFactory.getLogger(AuditStage.class);
    private final AuditGateway auditGateway;
    private final DomainEventPublisher eventPublisher;

    public AuditStage(AuditGateway auditGateway, DomainEventPublisher eventPublisher) {
        this.auditGateway = auditGateway;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public int order() { return 700; }

    @Override
    public String name() { return "Audit"; }

    @Override
    public void execute(StageContext context) {
        CallLog callLog = context.getCallLog();
        if (callLog == null) return;

        callLog.setUserId(context.getIdentity().userId());
        callLog.setCalledAt(Instant.now());

        if (context.getExecutionError() != null) {
            callLog.setSuccess(false);
            callLog.setErrorMessage(context.getExecutionError().getMessage());
        } else {
            callLog.setSuccess(true);
            // Token 事件：零值兜底
            var response = context.getCurrentResponse();
            if (response != null) {
                TokenUsage usage = response.getUsage().orElse(new TokenUsage(0, 0));
                eventPublisher.publish(TokenUsedEvent.builder()
                        .userId(context.getIdentity().userId())
                        .apiKeyId(context.getIdentity().credentialId())
                        .model(response.getModel())
                        .provider(context.getRoutingContext().upstreamProtocol().name().toLowerCase())
                        .promptTokens(usage.promptTokens())
                        .completionTokens(usage.completionTokens())
                        .traceId(context.getTraceId())
                        .build());
            }
        }

        long duration = callLog.getCalledAt() != null
            ? java.time.Duration.between(callLog.getCalledAt(), Instant.now()).toMillis()
            : 0;
        callLog.setDurationMs(duration);
        auditGateway.saveCallLog(callLog);
    }
}
```

---

### Task 18：ChatDispatchServiceImpl 委托给 Pipeline + 清理旧方法

- [ ] **Step 1: 注入 StagePipeline，委托 dispatch/dispatchStream**

```java
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.pipeline.StagePipeline;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(StagePipeline.class)
public class ChatDispatchServiceImpl implements ChatDispatchService {

    private final StagePipeline pipeline;

    public ChatDispatchServiceImpl(StagePipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public ProtocolResponse dispatch(ProtocolRequest request, Identity identity,
                                      RoutingStrategy strategy) {
        return pipeline.execute(request, identity, strategy);
    }

    @Override
    public void dispatchStream(ProtocolRequest request, Identity identity,
                                RoutingStrategy strategy, StreamCallback callback) {
        pipeline.executeStream(request, identity, strategy, callback);
    }
}
```

> 注意：当 `app.pipeline.enabled=false` 时，`@ConditionalOnBean(StagePipeline.class)` 会使该 Bean 不创建，需要保留旧实现的 ChatDispatchServiceImpl 作为回退。

- [ ] **Step 2: 保留旧实现作为回退**

创建 `ChatDispatchServiceImplLegacy.java`（复制当前 ChatDispatchServiceImpl，去掉 Pipeline 依赖），加上 `@ConditionalOnMissingBean(StagePipeline.class)`。

---

### Task 19：AbstractProtocolController + 控制器迁移

- [ ] **Step 1: 创建 AbstractProtocolController**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.proxy.ChatDispatchService;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.validation.ProtocolValidationException;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import java.io.IOException;

@RequiredArgsConstructor
public abstract class AbstractProtocolController<T extends ProtocolRequest> {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ChatDispatchService chatDispatchService;

    protected abstract void validate(T request);

    protected abstract ResponseEntity<?> wrapResponse(ProtocolResponse response);

    protected ResponseEntity<?> handleRequest(T request, Identity identity,
                                               HttpServletResponse response) throws IOException {
        log.info("Request: model={}, stream={}", request.getModel(), request.isStream());
        validate(request);
        if (identity == null) {
            throw new IllegalStateException("认证信息缺失");
        }
        if (request.isStream()) {
            SseStreamHelper.executeStream(chatDispatchService, request, identity, response);
            return null;
        }
        ProtocolResponse result = chatDispatchService.dispatch(request, identity, RoutingStrategy.WEIGHTED);
        return wrapResponse(result);
    }
}
```

- [ ] **Step 2: OpenAIController 简化**

```java
@RestController
@RequestMapping("/v1")
public class OpenAIController extends AbstractProtocolController<OpenAIChatRequest> {

    private final OpenAIProtocolValidator validator;

    public OpenAIController(ChatDispatchService chatDispatchService,
                             OpenAIProtocolValidator validator) {
        super(chatDispatchService);
        this.validator = validator;
    }

    @Override
    protected void validate(OpenAIChatRequest request) { validator.validate(request); }

    @Override
    protected ResponseEntity<?> wrapResponse(ProtocolResponse response) {
        if (response instanceof OpenAIChatResponse openai) return ResponseEntity.ok(openai);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(
            @RequestBody OpenAIChatRequest request,
            @RequestAttribute("identity") Identity identity,
            HttpServletResponse response) throws IOException {
        return handleRequest(request, identity, response);
    }

    @ExceptionHandler(ProtocolValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ProtocolValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", Map.of("message", ex.getMessage(), "type", "invalid_request_error")));
    }
}
```

- [ ] **Step 3: AnthropicController 简化**

```java
@RestController
@RequestMapping("/anthropic/v1")
public class AnthropicController extends AbstractProtocolController<AnthropicMessagesRequest> {

    private final AnthropicProtocolValidator validator;

    public AnthropicController(ChatDispatchService chatDispatchService,
                                AnthropicProtocolValidator validator) {
        super(chatDispatchService);
        this.validator = validator;
    }

    @Override
    protected void validate(AnthropicMessagesRequest request) { validator.validate(request); }

    @Override
    protected ResponseEntity<?> wrapResponse(ProtocolResponse response) {
        if (response instanceof AnthropicMessagesResponse anthropic) return ResponseEntity.ok(anthropic);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages")
    public ResponseEntity<?> messages(
            @RequestBody AnthropicMessagesRequest request,
            @RequestAttribute("identity") Identity identity,
            HttpServletResponse response) throws IOException {
        return handleRequest(request, identity, response);
    }

    @ExceptionHandler(ProtocolValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ProtocolValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "type", "error", "error", Map.of("type", "invalid_request_error", "message", ex.getMessage())));
    }
}
```

---

### Task 20：OutboundTuner 适配不可变请求

- [ ] **Step 1: tune 方法不调用 setModel，返回新实例**

```java
@SuppressWarnings("unchecked")
public <T extends ProtocolRequest> T tune(T request, RoutingContext context) {
    String protocol = request.getProtocol();
    ProtocolTuner<T> tuner = (ProtocolTuner<T>) tunersByProtocol.get(protocol);
    if (tuner != null) {
        request = tuner.tune(request);
    }
    // 通道级调谐：不调用 setModel，改为 withModel 返回新实例
    String upstreamModelName = context.upstreamModelName();
    if (upstreamModelName != null && !upstreamModelName.isBlank()) {
        request = (T) request.withModel(upstreamModelName);
    }
    return request;
}
```

---

## Phase 4

### Task 21：CachedRoutingResolver

- [ ] **Step 1: CachedRoutingResolver 装饰器**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

public class CachedRoutingResolver extends RoutingResolver {

    private final RoutingResolver delegate;
    private final Cache<String, RoutingContext> cache;

    public CachedRoutingResolver(RoutingResolver delegate, int maxSize, Duration expireAfterWrite) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWrite)
                .recordStats()
                .build();
    }

    @Override
    public RoutingContext resolve(String modelName, Protocol inboundProtocol) {
        String key = modelName + "::" + inboundProtocol.name();
        return cache.get(key, k -> delegate.resolve(modelName, inboundProtocol));
    }

    public CacheStats getStats() { return cache.stats(); }
    public void invalidateAll() { cache.invalidateAll(); }
}
```

- [ ] **Step 2: CacheConfig.java**

```java
@Configuration
public class CacheConfig {
    @Bean
    @ConditionalOnProperty("app.routing.cache.enabled")
    public CachedRoutingResolver cachedRoutingResolver(RoutingResolver delegate) {
        return new CachedRoutingResolver(delegate, 10_000, Duration.ofSeconds(60));
    }
}
```

- [ ] **Step 3: RoutingCacheInvalidator.java**

```java
@Component
public class RoutingCacheInvalidator {
    private final CachedRoutingResolver cachedResolver;

    public RoutingCacheInvalidator(Optional<CachedRoutingResolver> cachedResolver) {
        this.cachedResolver = cachedResolver.orElse(null);
    }

    @EventListener
    public void onChannelStateChanged(ChannelStateChangedEvent event) {
        if (cachedResolver != null) {
            cachedResolver.invalidateAll();
            log.info("渠道状态变更，路由缓存已清空");
        }
    }
}
```

---

### Task 22：JSR-305 注解

- [ ] **Step 1: 在所有 Gateway 接口参数/返回值增加 @NonNull/@Nullable**

涉及文件（domain 层的 gateway 接口）：
- `domain/supply/gateway/ChannelGateway.java`
- `domain/supply/gateway/ChannelEndpointGateway.java`
- `domain/supply/gateway/UpstreamClientRegistry.java`
- `domain/audit/gateway/AuditGateway.java`

```java
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

// 示例
public interface ChannelGateway {
    @Nonnull Optional<Channel> findById(@Nonnull Long id);
    @Nonnull List<Channel> findAll();
}
```

---

## Phase 5

### Task 23：SSE 客户端断开取消上游

- [ ] **Step 1: SseStreamHelper 追踪上游 Call 并支持取消**

```java
public static void executeStream(ChatDispatchService dispatchService, ProtocolRequest protocolRequest,
                                   Identity identity, HttpServletResponse response) throws IOException {
    setupSseResponse(response);
    PrintWriter writer = response.getWriter();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> errorRef = new AtomicReference<>();

    // 监听客户端断开
    AtomicBoolean clientDisconnected = new AtomicBoolean(false);
    // SseEmitter 方式或通过 response 检测
    if (response instanceof org.apache.catalina.connector.ResponseFacade) {
        // 在 Tomcat 中可以通过 response.getResponse().isClosed() 检测
        // Spring 提供了更可靠的 DisconnectedClientHelper
    }

    dispatchService.dispatchStream(protocolRequest, identity, RoutingStrategy.WEIGHTED, new StreamCallback() {
        @Override
        public void onChunk(String data) {
            if (clientDisconnected.get()) return;
            writeChunk(writer, data, errorRef, latch);
        }

        @Override
        public void onComplete() { completeStream(writer, latch); }

        @Override
        public void onError(Throwable t) {
            errorRef.set(t);
            latch.countDown();
        }
    });
    awaitCompletion(latch, errorRef);
}
```

实际取消上游请求依赖 OkHttp 的 `Call.cancel()`。Phase 5 实现思路：AbstractUpstreamClient 在 enqueue 时保存 `Call` 引用到 `AtomicReference`，在 SseStreamHelper 的 onChunk 中检测客户端断开后调用 `call.cancel()`。此方案需在 AbstractUpstreamClient 中暴露 cancel 能力，作为 Phase 5 独立子任务实现。

---

### Task 24：ProtocolConverter JSON 解析容错 + tool_choice

- [ ] **Step 1: JSON 解析异常时 log warn + 透传**

找到 `ProtocolConverter` 中 `toAnthropic` / `toOpenAI` 方法内调用 Jackson 解析的地方：

```java
try {
    JsonNode toolsNode = objectMapper.readTree(toolsJson);
    // ... 原逻辑
} catch (JsonProcessingException e) {
    log.warn("工具格式解析失败，透传原始数据: {}", e.getMessage());
    // 返回原始请求，不做转换，由上游自行处理
    return anthropicRequest;
}
```

- [ ] **Step 2: tool_choice 支持对象格式**

在 ProtocolConverter 中处理 tool_choice 字段的反序列化，区分字符串格式 `"auto"` 和对象格式 `{type: "function", function: {name: "..."}}`：

```java
JsonNode toolChoice = requestNode.get("tool_choice");
if (toolChoice != null) {
    if (toolChoice.isTextual()) {
        // "auto" / "required" / "none"
        anthropicRequest.setToolChoice(toolChoice.asText());
    } else if (toolChoice.isObject()) {
        // {type: "function", function: {name: "..."}}
        JsonNode funcNode = toolChoice.get("function");
        if (funcNode != null && funcNode.has("name")) {
            anthropicRequest.setToolChoice(funcNode.get("name").asText());
        }
    }
}
```

---

### Task 25：ChatDispatchServiceImpl 清理

- [ ] **Step 1: 删除 publishTokenUsedEvent 方法**

逻辑已迁移到 AuditStage，从 ChatDispatchServiceImpl 中删除该方法及所有调用。

---

### Task 26: 全量测试验证

- [ ] **Step 1: 运行全量测试**

Run: `mvn test -pl gateway-boot`
Expected: 430+ 测试全部通过

- [ ] **Step 2: 修复失败的测试**

搜索测试失败的原因，逐条修复（通常为 mock 调整、异常类型匹配）。

- [ ] **Step 3: 新增验收标准测试**

为每个 AC 编写对应的测试用例（参考 Task 2 Step 5 样式）。

---

## 验收标准对照表

| AC | 验证方式 | 对应检查点 |
|----|---------|-----------|
| AC1.1 | 编译 + 430 测试 | Task 26 |
| AC1.2 | 不可变性测试 | Task 1 Step 2-3 |
| AC1.3 | toString 脱敏 | Task 3 Step 4 |
| AC1.4 | getUsage 结构 | Task 2 Step 5 |
| AC2.1 | 子类继承验证 | Task 5-6 编译 |
| AC2.3 | 连接池可配置 | Task 7 application.yml |
| AC2.4 | ProviderException.statusCode | Task 8 Step 1 |
| AC2.5 | 4xx 不熔断 | Task 8 Step 3 |
| AC3.1 | 接口签名不变 | Task 18 编译 |
| AC3.4 | 异常时审计 | Task 10 finally 测试 |
| AC3.5 | 回退开关 | Task 18 Step 2 |
| AC4.2 | 缓存失效 | Task 21 Step 3 |
| AC4.4 | 缓存关闭 | Task 21 Step 2 |
| AC5.3 | 零值兜底 | Task 17 AuditStage |
| AC5.4 | JSON 容错 | Task 24 Step 1 |