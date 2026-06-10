---
change: resilience-enhancement
design-doc: docs/superpowers/specs/2026-06-10-resilience-enhancement-design.md
base-ref: 48497ca575c41de1c3ef359c48d817fb4e7e0d4f
---

# 韧性增强实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完善上游大模型异常处理体系，实现异常细分类、SSE 结构化错误、差异化重试、故障转移、Metrics 埋点、智能降级

**Architecture:** 策略模式（ErrorClassificationStrategy / RetryStrategy）+ Application 层编排（故障转移/降级）+ Infrastructure 层埋点（Micrometer）。不引入新外部依赖。

**依赖链:** Task 1 → Task 3,4,5,7 → Task 6 → Task 8（Task 2 独立）

---

### Task 1: ProviderException 增强 + ErrorClassificationStrategy 接口 + OpenAI 实现

**核心变更:** ProviderException 增加 errorType/上下文字段；定义 ErrorClassificationStrategy 接口；实现 OpenAIErrorClassifier；改造 OpenAIUpstreamClient 集成分类器。

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/exception/ProviderException.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/ErrorClassificationStrategy.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/OpenAIErrorClassifier.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/upstream/OpenAIUpstreamClient.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/upstream/OpenAIErrorClassifierTest.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/OpenAIUpstreamClientTest.java`

- [ ] **Step 1: 扩展 ProviderException，增加 errorType 和上下文字段**

```java
package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

/**
 * 供应商异常
 *
 * <p>表示调用外部模型供应商时发生的错误，包含错误类型和上下文信息。</p>
 */
public class ProviderException extends GatewayException {

    private final ProviderErrorType errorType;
    private final String traceId;
    private final String model;
    private final String provider;
    private final Long channelEndpointId;
    private final Integer retryAfterSeconds;

    public ProviderException(String code, String message) {
        super(code, message);
        this.errorType = ProviderErrorType.UNKNOWN_ERROR;
        this.traceId = null;
        this.model = null;
        this.provider = null;
        this.channelEndpointId = null;
        this.retryAfterSeconds = null;
    }

    public ProviderException(String code, String message, Throwable cause) {
        super(code, message, cause);
        this.errorType = ProviderErrorType.UNKNOWN_ERROR;
        this.traceId = null;
        this.model = null;
        this.provider = null;
        this.channelEndpointId = null;
        this.retryAfterSeconds = null;
    }

    public ProviderException(ProviderErrorType errorType, String message) {
        super(errorType.name(), message);
        this.errorType = errorType;
        this.traceId = null;
        this.model = null;
        this.provider = null;
        this.channelEndpointId = null;
        this.retryAfterSeconds = null;
    }

    public ProviderException(ProviderErrorType errorType, String message, Throwable cause) {
        super(errorType.name(), message, cause);
        this.errorType = errorType;
        this.traceId = null;
        this.model = null;
        this.provider = null;
        this.channelEndpointId = null;
        this.retryAfterSeconds = null;
    }

    public ProviderException(ProviderErrorType errorType, String message,
                             String traceId, String model, String provider,
                             Long channelEndpointId, Integer retryAfterSeconds) {
        super(errorType.name(), message);
        this.errorType = errorType;
        this.traceId = traceId;
        this.model = model;
        this.provider = provider;
        this.channelEndpointId = channelEndpointId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ProviderException(ProviderErrorType errorType, String message, Throwable cause,
                             String traceId, String model, String provider,
                             Long channelEndpointId, Integer retryAfterSeconds) {
        super(errorType.name(), message, cause);
        this.errorType = errorType;
        this.traceId = traceId;
        this.model = model;
        this.provider = provider;
        this.channelEndpointId = channelEndpointId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ProviderErrorType getErrorType() { return errorType; }
    public String getTraceId() { return traceId; }
    public String getModel() { return model; }
    public String getProvider() { return provider; }
    public Long getChannelEndpointId() { return channelEndpointId; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
}
```

- [ ] **Step 2: 创建 ErrorClassificationStrategy 接口**

```java
package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

/**
 * 错误分类策略接口
 *
 * <p>根据 HTTP 状态码和响应体内容，将上游错误映射为 ProviderErrorType。</p>
 */
public interface ErrorClassificationStrategy {

    /**
     * 分类上游错误
     *
     * @param statusCode   HTTP 状态码
     * @param responseBody 响应体（可为 null）
     * @return 映射后的错误类型
     */
    ProviderErrorType classify(int statusCode, String responseBody);

    /**
     * 获取此策略支持的 Provider 名称
     */
    String supportedProvider();
}
```

- [ ] **Step 3: 编写 OpenAIErrorClassifier 测试**

```java
package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenAI 错误分类器测试")
class OpenAIErrorClassifierTest {

    private OpenAIErrorClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new OpenAIErrorClassifier();
    }

    @Nested
    @DisplayName("按 HTTP 状态码分类")
    class ByStatusCode {

        @Test
        @DisplayName("401 → AUTHENTICATION_ERROR")
        void status401_authenticationError() {
            assertThat(classifier.classify(401, "{\"error\":{\"type\":\"invalid_api_key\"}}"))
                    .isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
        }

        @Test
        @DisplayName("429 → RATE_LIMIT_ERROR")
        void status429_rateLimit() {
            assertThat(classifier.classify(429, "{\"error\":{\"type\":\"rate_limit_error\"}}"))
                    .isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR);
        }

        @Test
        @DisplayName("429 + quota 关键字 → QUOTA_EXCEEDED")
        void status429_withQuota_quotaExceeded() {
            assertThat(classifier.classify(429, "{\"error\":{\"message\":\"You exceeded your current quota\"}}"))
                    .isEqualTo(ProviderErrorType.QUOTA_EXCEEDED);
        }

        @Test
        @DisplayName("429 + insufficient_quota 关键字 → QUOTA_EXCEEDED")
        void status429_withInsufficientQuota_quotaExceeded() {
            assertThat(classifier.classify(429, "{\"error\":{\"message\":\"insufficient_quota\"}}"))
                    .isEqualTo(ProviderErrorType.QUOTA_EXCEEDED);
        }

        @Test
        @DisplayName("400 → INVALID_REQUEST")
        void status400_invalidRequest() {
            assertThat(classifier.classify(400, "{\"error\":{\"type\":\"invalid_request_error\"}}"))
                    .isEqualTo(ProviderErrorType.INVALID_REQUEST);
        }

        @Test
        @DisplayName("500 → UPSTREAM_ERROR")
        void status500_upstreamError() {
            assertThat(classifier.classify(500, "{}"))
                    .isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
        }

        @Test
        @DisplayName("502 → UPSTREAM_ERROR")
        void status502_upstreamError() {
            assertThat(classifier.classify(502, "{}"))
                    .isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
        }

        @Test
        @DisplayName("503 → UPSTREAM_ERROR")
        void status503_upstreamError() {
            assertThat(classifier.classify(503, "{}"))
                    .isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
        }

        @Test
        @DisplayName("504 → TIMEOUT_ERROR")
        void status504_timeoutError() {
            assertThat(classifier.classify(504, "{}"))
                    .isEqualTo(ProviderErrorType.TIMEOUT_ERROR);
        }

        @Test
        @DisplayName("499 → UNKNOWN_ERROR")
        void status499_unknown() {
            assertThat(classifier.classify(499, "{}"))
                    .isEqualTo(ProviderErrorType.UNKNOWN_ERROR);
        }
    }

    @Nested
    @DisplayName("supportedProvider")
    class SupportedProvider {

        @Test
        @DisplayName("返回 openai")
        void returnsOpenai() {
            assertThat(classifier.supportedProvider()).isEqualTo("openai");
        }
    }
}
```

- [ ] **Step 4: 实现 OpenAIErrorClassifier**

```java
package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

/**
 * OpenAI 错误分类器
 *
 * <p>根据 OpenAI API 错误响应格式，将 HTTP 状态码 + 错误体映射为 ProviderErrorType。</p>
 */
public class OpenAIErrorClassifier implements ErrorClassificationStrategy {

    private static final String PROVIDER = "openai";

    @Override
    public ProviderErrorType classify(int statusCode, String responseBody) {
        return switch (statusCode) {
            case 401 -> ProviderErrorType.AUTHENTICATION_ERROR;
            case 429 -> classifyRateLimit(responseBody);
            case 400 -> ProviderErrorType.INVALID_REQUEST;
            case 408 -> ProviderErrorType.TIMEOUT_ERROR;
            case 504 -> ProviderErrorType.TIMEOUT_ERROR;
            case 500, 502, 503 -> ProviderErrorType.UPSTREAM_ERROR;
            default -> ProviderErrorType.UNKNOWN_ERROR;
        };
    }

    private ProviderErrorType classifyRateLimit(String responseBody) {
        if (responseBody == null) return ProviderErrorType.RATE_LIMIT_ERROR;
        String lower = responseBody.toLowerCase();
        if (lower.contains("quota") || lower.contains("insufficient_quota")) {
            return ProviderErrorType.QUOTA_EXCEEDED;
        }
        return ProviderErrorType.RATE_LIMIT_ERROR;
    }

    @Override
    public String supportedProvider() {
        return PROVIDER;
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd gateway-boot && ../mvnw test -Dtest=OpenAIErrorClassifierTest -pl .`

- [ ] **Step 6: 改造 OpenAIUpstreamClient.chat() — 集成分类器**

在 `OpenAIUpstreamClient` 中新增字段 `ErrorClassificationStrategy classifier`，构造器注入。在 `chat()` 的 HTTP 错误处理处：

```java
// 找到 throws 处，替换为：
if (!response.isSuccessful()) {
    String responseBody = response.body() != null ? response.body().string() : "";
    ProviderErrorType errorType = classifier.classify(response.code(), responseBody);
    throw new ProviderException(errorType,
            "OpenAI API 调用失败: " + response.code() + " - " + responseBody);
}
```

`chatStream()` 同理改造（onResponse 非 2xx 处）：

```java
if (!response.isSuccessful() || body == null) {
    String errorBody = body != null ? body.string() : "no body";
    ProviderErrorType errorType = classifier.classify(response.code(), errorBody);
    callback.onError(new ProviderException(errorType,
            "OpenAI Stream 失败: " + response.code() + " - " + errorBody));
    return;
}
```

`onFailure(IOException)` 处映射为 `NETWORK_ERROR`：

```java
public void onFailure(Call call, IOException e) {
    ProviderErrorType errorType = e instanceof java.net.SocketTimeoutException
            ? ProviderErrorType.TIMEOUT_ERROR
            : ProviderErrorType.NETWORK_ERROR;
    callback.onError(new ProviderException(errorType, "OpenAI 网络异常: " + e.getMessage()));
}
```

IOException catch 块同样改造（chat 非流式）：

```java
catch (IOException e) {
    ProviderErrorType errorType = e instanceof java.net.SocketTimeoutException
            ? ProviderErrorType.TIMEOUT_ERROR
            : ProviderErrorType.NETWORK_ERROR;
    throw new ProviderException(errorType, "OpenAI API 调用异常", e);
}
```

- [ ] **Step 7: 更新 OpenAIUpstreamClient 构造器 — 接受 classifier**

```java
public OpenAIUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
                            int timeoutSeconds, ObjectMapper objectMapper,
                            ErrorClassificationStrategy classifier) {
    this.httpClient = httpClient;
    this.endpointUrl = endpointUrl;
    this.apiKey = apiKey;
    this.timeoutSeconds = timeoutSeconds;
    this.objectMapper = objectMapper;
    this.classifier = classifier;
}
```

- [ ] **Step 8: 运行已有测试验证未破坏现有行为**

Run: `cd gateway-boot && ../mvnw test -Dtest=ResilientUpstreamClientTest,CircuitBreakerTest,RetryExecutorTest -pl .`

- [ ] **Step 9: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/exception/ProviderException.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/ErrorClassificationStrategy.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/OpenAIErrorClassifier.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/upstream/OpenAIUpstreamClient.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/upstream/OpenAIErrorClassifierTest.java
git commit -m "feat(resilience): ProviderException 增加 errorType/上下文字段 + OpenAI 错误分类器"
```

---

### Task 2: AnthropicErrorClassifier + AnthropicUpstreamClient 改造

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/AnthropicErrorClassifier.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/upstream/AnthropicUpstreamClient.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/upstream/AnthropicErrorClassifierTest.java`

- [ ] **Step 1: 编写 AnthropicErrorClassifier 测试（同 Task 1 Step 3 模式，验证 Anthropic 错误格式）**

```java
@DisplayName("Anthropic 错误分类器测试")
class AnthropicErrorClassifierTest {
    // 401 → AUTHENTICATION_ERROR
    // 429 → RATE_LIMIT_ERROR  
    // 529 (Anthropic 过载) → UPSTREAM_ERROR
    // 400 → INVALID_REQUEST
    // 500 → UPSTREAM_ERROR
    // 504 → TIMEOUT_ERROR
    // supportedProvider → "anthropic"
}
```

- [ ] **Step 2: 实现 AnthropicErrorClassifier**

```java
public class AnthropicErrorClassifier implements ErrorClassificationStrategy {
    // 与 OpenAI 类似，但 Anthropic 用 529 表示过载（映射为 UPSTREAM_ERROR）
    // 没有 QUOTA_EXCEEDED 区分
}
```

- [ ] **Step 3: 改造 AnthropicUpstreamClient（同 Task 1 Step 6 模式，集成 classifier）**

- [ ] **Step 4: 运行测试**

Run: `cd gateway-boot && ../mvnw test -Dtest=AnthropicErrorClassifierTest -pl .`

- [ ] **Step 5: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/AnthropicErrorClassifier.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/upstream/AnthropicUpstreamClient.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/upstream/AnthropicErrorClassifierTest.java
git commit -m "feat(resilience): Anthropic 错误分类器 + UpstreamClient 集成"
```

---

### Task 3: CircuitOpenException 继承改造 + GlobalExceptionHandler 适配

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/CircuitOpenException.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/advice/GlobalExceptionHandler.java`

- [ ] **Step 1: CircuitOpenException 改为继承 ProviderException**

```java
package com.codingas.gateway.infrastructure.resilience;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;

/**
 * 熔断器开启异常
 *
 * <p>当熔断器处于 OPEN 状态时拒绝请求抛出此异常。</p>
 */
public class CircuitOpenException extends ProviderException {

    public CircuitOpenException(String message) {
        super(ProviderErrorType.UPSTREAM_ERROR, message);
    }

    public CircuitOpenException(String traceId, String model, String provider, Long endpointId) {
        super(ProviderErrorType.UPSTREAM_ERROR, "熔断器开启，拒绝请求",
              traceId, model, provider, endpointId, null);
    }
}
```

- [ ] **Step 2: 更新 GlobalExceptionHandler — 确认 CircuitOpenException 被 ProviderException handler 覆盖**

查看 `GlobalExceptionHandler`，`CircuitOpenException` 现在继承 `ProviderException`，`handleProviderException()` 自动捕获，返回 502。无需新增 handler。

- [ ] **Step 3: 运行测试验证**

Run: `cd gateway-boot && ../mvnw test -Dtest=ResilientUpstreamClientTest,GlobalExceptionHandlerTest -pl .`

- [ ] **Step 4: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/CircuitOpenException.java \
       gateway-boot/src/main/java/com/codingas/gateway/adapter/advice/GlobalExceptionHandler.java
git commit -m "feat(resilience): CircuitOpenException 继承 ProviderException，纳入统一异常体系"
```

---

### Task 4: 差异化重试策略（RetryStrategy 接口 + 四种实现 + RetryExecutor 集成）

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/RetryStrategy.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ExponentialBackoffStrategy.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/RateLimitRetryStrategy.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/FastRetryStrategy.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ServiceUnavailableStrategy.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/GatewayRetryProperties.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/RetryExecutor.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/RetryExecutorTest.java`

- [ ] **Step 1: 定义 RetryStrategy 接口**

```java
package com.codingas.gateway.infrastructure.resilience;

/**
 * 重试策略接口
 *
 * <p>根据重试次数计算退避时间。</p>
 */
public interface RetryStrategy {

    /**
     * 计算第 N 次重试的退避时间
     *
     * @param attempt 当前重试次数（从 1 开始）
     * @return 退避时间（毫秒）
     */
    long calculateDelay(int attempt);

    /**
     * 最大重试次数
     */
    int maxAttempts();
}
```

- [ ] **Step 2: 扩展 GatewayRetryProperties**

```java
// 新增字段
/** 限流重试配置 */
private RateLimitConfig rateLimit = new RateLimitConfig();

/** 快速重试配置（超时场景） */
private FastRetryConfig fastRetry = new FastRetryConfig();

/** 服务不可用重试配置 */
private ServiceUnavailableConfig serviceUnavailable = new ServiceUnavailableConfig();

// 内部类
public static class RateLimitConfig {
    private int maxAttempts = 5;
    private long backoffInitial = 2000;
    private double backoffMultiplier = 2.0;
    private long maxBackoff = 60000;
    private double jitterRate = 0.25;
    // getter/setter
}

public static class FastRetryConfig {
    private int maxAttempts = 2;
    private long backoffFixed = 500;
    // getter/setter
}

public static class ServiceUnavailableConfig {
    private int maxAttempts = 3;
    private long backoffFixed = 5000;
    // getter/setter
}
```

- [ ] **Step 3: 实现四种策略**

```java
// ExponentialBackoffStrategy — 默认
public class ExponentialBackoffStrategy implements RetryStrategy {
    private final int maxAttempts;
    private final long backoffInitial;
    private final double backoffMultiplier;

    public ExponentialBackoffStrategy(GatewayRetryProperties properties) {
        this.maxAttempts = properties.getMaxAttempts();
        this.backoffInitial = properties.getBackoffInitial();
        this.backoffMultiplier = properties.getBackoffMultiplier();
    }

    @Override
    public long calculateDelay(int attempt) {
        return (long) (backoffInitial * Math.pow(backoffMultiplier, attempt - 1));
    }

    @Override
    public int maxAttempts() { return maxAttempts; }
}

// RateLimitRetryStrategy — 429
public class RateLimitRetryStrategy implements RetryStrategy {
    private final int maxAttempts;
    private final long backoffInitial;
    private final double backoffMultiplier;
    private final long maxBackoff;
    private final double jitterRate;
    private final Random random = new Random();

    public RateLimitRetryStrategy(GatewayRetryProperties properties) {
        GatewayRetryProperties.RateLimitConfig cfg = properties.getRateLimit();
        this.maxAttempts = cfg.getMaxAttempts();
        this.backoffInitial = cfg.getBackoffInitial();
        this.backoffMultiplier = cfg.getBackoffMultiplier();
        this.maxBackoff = cfg.getMaxBackoff();
        this.jitterRate = cfg.getJitterRate();
    }

    @Override
    public long calculateDelay(int attempt) {
        long delay = (long) (backoffInitial * Math.pow(backoffMultiplier, attempt - 1));
        delay = Math.min(delay, maxBackoff);
        // 加入 ±jitterRate 随机抖动
        double jitter = 1.0 + (random.nextDouble() - 0.5) * 2 * jitterRate;
        return (long) (delay * jitter);
    }

    @Override
    public int maxAttempts() { return maxAttempts; }
}

// FastRetryStrategy — 504
public class FastRetryStrategy implements RetryStrategy {
    private final int maxAttempts;
    private final long backoffFixed;

    public FastRetryStrategy(GatewayRetryProperties properties) {
        this.maxAttempts = properties.getFastRetry().getMaxAttempts();
        this.backoffFixed = properties.getFastRetry().getBackoffFixed();
    }

    @Override
    public long calculateDelay(int attempt) { return backoffFixed; }
    @Override
    public int maxAttempts() { return maxAttempts; }
}

// ServiceUnavailableStrategy — 503
public class ServiceUnavailableStrategy implements RetryStrategy {
    private final int maxAttempts;
    private final long backoffFixed;

    public ServiceUnavailableStrategy(GatewayRetryProperties properties) {
        this.maxAttempts = properties.getServiceUnavailable().getMaxAttempts();
        this.backoffFixed = properties.getServiceUnavailable().getBackoffFixed();
    }

    @Override
    public long calculateDelay(int attempt) { return backoffFixed; }
    @Override
    public int maxAttempts() { return maxAttempts; }
}
```

- [ ] **Step 4: 改造 RetryExecutor — 内部集成策略选择**

```java
public <T> T execute(Supplier<T> action) {
    Exception lastException = null;
    // 第一次执行，先获取结果
    try {
        return action.get();
    } catch (Exception e) {
        lastException = e;
        if (!isRetryable(e)) throw e;
    }

    // 根据异常类型选择策略
    RetryStrategy strategy = selectStrategy(lastException);
    int maxAttempts = strategy.maxAttempts();

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return action.get();
        } catch (Exception e) {
            lastException = e;
            if (!isRetryable(e) || attempt == maxAttempts) throw e;
            long delay = strategy.calculateDelay(attempt);
            log.warn("重试 {}/{}，{}ms 后重试: {}", attempt, maxAttempts, delay, e.getMessage());
            sleep(delay);
        }
    }
    throw new RuntimeException("重试耗尽", lastException);
}

private RetryStrategy selectStrategy(Exception e) {
    if (e instanceof ProviderException pe) {
        return switch (pe.getErrorType()) {
            case RATE_LIMIT_ERROR -> new RateLimitRetryStrategy(properties);
            case QUOTA_EXCEEDED -> new ExponentialBackoffStrategy(properties); // 配额超限不重试
            case TIMEOUT_ERROR -> new FastRetryStrategy(properties);
            case UPSTREAM_ERROR -> new ServiceUnavailableStrategy(properties);
            default -> new ExponentialBackoffStrategy(properties);
        };
    }
    return new ExponentialBackoffStrategy(properties);
}
```

注意：`QUOTA_EXCEEDED` 不应重试（配额超限重试也没用），所以 `selectStrategy` 返回默认策略但 `isRetryable` 应拒绝对 `QUOTA_EXCEEDED` 重试。

更新 `isRetryable`：

```java
boolean isRetryable(Exception e) {
    if (e instanceof ProviderException pe) {
        // 配额超限和认证错误不可重试
        return switch (pe.getErrorType()) {
            case QUOTA_EXCEEDED, AUTHENTICATION_ERROR, INVALID_REQUEST -> false;
            default -> true;
        };
    }
    if (e instanceof RetryableException) return true;
    String message = e.getMessage();
    if (message == null) return false;
    return retryableStatusCodes.stream()
        .anyMatch(code -> message.contains(String.valueOf(code)));
}
```

- [ ] **Step 5: 更新 RetryExecutor 测试**

```java
// 新增测试
@Test
@DisplayName("429 限流使用 RateLimitRetryStrategy")
void rateLimit_usesRateLimitStrategy() {
    properties.getRateLimit().setMaxAttempts(3);
    properties.getRateLimit().setBackoffInitial(100);
    executor = new RetryExecutor(properties);
    
    AtomicInteger counter = new AtomicInteger(0);
    String result = executor.execute(() -> {
        if (counter.incrementAndGet() < 3) {
            throw new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR, "429 限流");
        }
        return "ok";
    });
    assertThat(result).isEqualTo("ok");
    assertThat(counter.get()).isEqualTo(3);
}

@Test
@DisplayName("QUOTA_EXCEEDED 不可重试")
void quotaExceeded_notRetryable() {
    AtomicInteger counter = new AtomicInteger(0);
    assertThatThrownBy(() -> executor.execute(() -> {
        counter.incrementAndGet();
        throw new ProviderException(ProviderErrorType.QUOTA_EXCEEDED, "配额超限");
    })).isInstanceOf(ProviderException.class);
    assertThat(counter.get()).isEqualTo(1); // 只执行一次
}
```

- [ ] **Step 6: 运行测试**

Run: `cd gateway-boot && ../mvnw test -Dtest=RetryExecutorTest -pl .`

- [ ] **Step 7: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/RetryStrategy.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ExponentialBackoffStrategy.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/RateLimitRetryStrategy.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/FastRetryStrategy.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ServiceUnavailableStrategy.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/GatewayRetryProperties.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/RetryExecutor.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/RetryExecutorTest.java
git commit -m "feat(resilience): 差异化重试策略 — 429 长退避/504 快速重试/503 固定等待"
```

---

### Task 5: SSE 流式错误结构化

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/SseErrorFormatter.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java`

- [ ] **Step 1: 创建 SseErrorFormatter 工具类**

```java
package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;

/**
 * SSE 错误格式化工具
 *
 * <p>将 ProviderException 格式化为结构化 SSE 错误事件。</p>
 */
public class SseErrorFormatter {

    private SseErrorFormatter() {}

    /**
     * 格式化 SSE 错误事件
     */
    public static String format(ProviderException e) {
        String type = switch (e.getErrorType()) {
            case RATE_LIMIT_ERROR -> "rate_limit";
            case QUOTA_EXCEEDED -> "quota_exceeded";
            case AUTHENTICATION_ERROR -> "authentication_error";
            case TIMEOUT_ERROR -> "timeout";
            case UPSTREAM_ERROR -> "api_error";
            case NETWORK_ERROR -> "network_error";
            case INVALID_REQUEST -> "invalid_request_error";
            case UNKNOWN_ERROR -> "unknown_error";
        };
        int retryAfter = e.getRetryAfterSeconds() != null ? e.getRetryAfterSeconds() : 0;
        return String.format("{\"error\":\"%s\",\"retry_after\":%d}", type, retryAfter);
    }
}
```

- [ ] **Step 2: 编写 SseErrorFormatter 测试**

```java
@DisplayName("SSE 错误格式化测试")
class SseErrorFormatterTest {
    @Test
    @DisplayName("RATE_LIMIT_ERROR 格式化为 rate_limit")
    void rateLimit_format() {
        ProviderException e = new ProviderException(ProviderErrorType.RATE_LIMIT_ERROR, "限流",
                null, null, null, null, 30);
        assertThat(SseErrorFormatter.format(e)).isEqualTo("{\"error\":\"rate_limit\",\"retry_after\":30}");
    }

    @Test
    @DisplayName("TIMEOUT_ERROR 格式化为 timeout")
    void timeout_format() {
        ProviderException e = new ProviderException(ProviderErrorType.TIMEOUT_ERROR, "超时");
        assertThat(SseErrorFormatter.format(e)).isEqualTo("{\"error\":\"timeout\",\"retry_after\":0}");
    }
}
```

- [ ] **Step 3: 在 ChatDispatchServiceImpl 的流式 callback 中集成**

在 `dispatchStream()` 方法中，`auditingCallback` 的 `onError` 处格式化错误：

```java
@Override
public void onError(Throwable t) {
    String errorJson;
    if (t instanceof ProviderException pe) {
        errorJson = SseErrorFormatter.format(pe);
    } else {
        errorJson = "{\"error\":\"unknown_error\",\"retry_after\":0}";
    }
    callLog.setDurationMs(System.currentTimeMillis() - startTime);
    callLog.setSuccess(false);
    callLog.setErrorMessage(errorJson);
    auditGateway.saveCallLog(callLog);
    callback.onError(new RuntimeException(errorJson));
}
```

- [ ] **Step 4: 运行测试**

Run: `cd gateway-boot && ../mvnw test -Dtest=SseErrorFormatterTest -pl .`

- [ ] **Step 5: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/SseErrorFormatter.java \
       gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/upstream/SseErrorFormatterTest.java
git commit -m "feat(resilience): SSE 流式错误结构化为 JSON 事件"
```

---

### Task 6: 渠道级故障转移（Key 级 + Channel 级）

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java`

- [ ] **Step 1: 在 ChatDispatchServiceImpl.dispatch() 中实现 Key 级故障转移**

```java
public ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
    String traceId = UUID.randomUUID().toString();
    Protocol inboundProtocol = getInboundProtocol(request);
    RoutingContext ctx = routingResolver.resolve(request.getModel(), inboundProtocol, identity.userId());

    // ... 前置阶段（转换、调谐）...

    // 阶段 5：上游调用（带 Key 级故障转移）
    List<ChannelCredential> credentials = credentialResolver.resolve(ctx.channelId());
    ProviderException lastException = null;

    for (ChannelCredential cred : credentials) {
        if (!circuitBreakerManager.isAvailable(cred.getEndpointId())) {
            log.debug("Key {} 熔断中，跳过", cred.getId());
            continue;
        }

        UpstreamClient rawClient = clientRegistry.getClient(
                ctx.upstreamProtocol().name().toLowerCase(),
                ctx.endpointUrl(),
                cred.getApiKey(),
                ctx.timeout() != null ? ctx.timeout() : 60);
        UpstreamClient client = resilientClientFactory.wrap(rawClient, cred.getEndpointId());

        try {
            ProtocolResponse response = client.chat(outboundReq);
            // 成功 → 返回
            // ... 后置处理 ...
            return response;
        } catch (ProviderException e) {
            lastException = e;
            log.warn("Key {} 失败: {} {}, 尝试下一个 Key", cred.getId(), e.getErrorType(), e.getMessage());
            // 继续尝试下一个 Key
        }
    }

    // 所有 Key 失败
    throw new ProviderException(ProviderErrorType.UPSTREAM_ERROR,
            "所有 Key 均失败", traceId, request.getModel(),
            ctx.upstreamProtocol().name(), ctx.channelEndpointId(), null);
}
```

- [ ] **Step 2: 添加 credentialResolver 依赖**

在 `ChatDispatchServiceImpl` 中新增 `CredentialResolver` 依赖，通过构造器注入。`CredentialResolver` 已在 `application/routing/` 中定义。

- [ ] **Step 3: 运行已有测试确认未破坏**

Run: `cd gateway-boot && ../mvnw test -Dtest=ChatDispatchServiceTest -pl .`

- [ ] **Step 4: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java
git commit -m "feat(resilience): 渠道级 Key 故障转移 — 重试耗尽后切换下一 Key"
```

---

### Task 7: Metrics 埋点

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ResilientUpstreamClient.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ResilientClientFactoryImpl.java`

- [ ] **Step 1: ResilientUpstreamClient 增加 MeterRegistry 参数和 Metrics 埋点**

```java
public class ResilientUpstreamClient implements UpstreamClient {

    private final UpstreamClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final RetryExecutor retryExecutor;
    private final MeterRegistry meterRegistry;
    private final String providerCode;
    private final Long endpointId;

    public ResilientUpstreamClient(UpstreamClient delegate, CircuitBreaker circuitBreaker,
                                    RetryExecutor retryExecutor, MeterRegistry meterRegistry,
                                    String providerCode, Long endpointId) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.retryExecutor = retryExecutor;
        this.meterRegistry = meterRegistry;
        this.providerCode = providerCode;
        this.endpointId = endpointId;
    }

    @Override
    public ProtocolResponse chat(ProtocolRequest request) {
        if (!circuitBreaker.allowRequest()) {
            meterRegistry.counter("gateway.circuitbreaker.blocked",
                    "provider", providerCode, "endpoint_id", String.valueOf(endpointId)).increment();
            throw new CircuitOpenException("熔断器开启，拒绝请求");
        }

        try {
            ProtocolResponse response = retryExecutor.execute(() -> delegate.chat(request));
            circuitBreaker.recordSuccess();
            return response;
        } catch (ProviderException e) {
            circuitBreaker.recordFailure();
            meterRegistry.counter("gateway.provider.errors",
                    "provider", providerCode,
                    "error_type", e.getErrorType().name()).increment();
            throw e;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            meterRegistry.counter("gateway.provider.errors",
                    "provider", providerCode,
                    "error_type", "UNKNOWN").increment();
            throw e;
        }
    }

    // chatStream 同理
}
```

- [ ] **Step 2: 更新 ResilientClientFactoryImpl**

```java
@Component
public class ResilientClientFactoryImpl implements ResilientClientFactory {

    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;
    private final RetryExecutor retryExecutor;
    private final MeterRegistry meterRegistry;

    public ResilientClientFactoryImpl(ChannelEndpointCircuitBreakerManager circuitBreakerManager,
                                       RetryExecutor retryExecutor, MeterRegistry meterRegistry) {
        this.circuitBreakerManager = circuitBreakerManager;
        this.retryExecutor = retryExecutor;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public UpstreamClient wrap(UpstreamClient rawClient, Long channelEndpointId) {
        CircuitBreaker breaker = circuitBreakerManager.getBreaker(channelEndpointId);
        // providerCode 从 rawClient 类型推断
        String providerCode = resolveProviderCode(rawClient);
        return new ResilientUpstreamClient(rawClient, breaker, retryExecutor,
                meterRegistry, providerCode, channelEndpointId);
    }

    private String resolveProviderCode(UpstreamClient client) {
        if (client instanceof com.codingas.gateway.infrastructure.supply.upstream.OpenAIUpstreamClient) {
            return "openai";
        }
        if (client instanceof com.codingas.gateway.infrastructure.supply.upstream.AnthropicUpstreamClient) {
            return "anthropic";
        }
        return "unknown";
    }
}
```

- [ ] **Step 3: 更新 ResilientUpstreamClientTest**

```java
// setUp 中增加 MeterRegistry
private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

resilientClient = new ResilientUpstreamClient(delegate, circuitBreaker, retryExecutor,
        meterRegistry, "test-provider", 1L);

// 新增测试
@Test
@DisplayName("失败时记录 Metrics")
void chat_failure_recordsMetrics() {
    when(delegate.chat(request)).thenThrow(new ProviderException(
            ProviderErrorType.RATE_LIMIT_ERROR, "429"));
    assertThatThrownBy(() -> resilientClient.chat(request))
            .isInstanceOf(ProviderException.class);
    assertThat(meterRegistry.counter("gateway.provider.errors",
            "provider", "test-provider", "error_type", "RATE_LIMIT_ERROR").count()).isPositive();
}
```

- [ ] **Step 4: 运行测试**

Run: `cd gateway-boot && ../mvnw test -Dtest=ResilientUpstreamClientTest -pl .`

- [ ] **Step 5: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ResilientUpstreamClient.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ResilientClientFactoryImpl.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/ResilientUpstreamClientTest.java
git commit -m "feat(resilience): ResilientUpstreamClient Metrics 埋点 — provider.errors/circuitbreaker.blocked"
```

---

### Task 8: 模型级智能降级骨架（DegradationService + 降级链 + 自动回切）

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/degradation/DegradationService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/degradation/DegradationServiceImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/degradation/DegradationEvent.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/degradation/DegradationProperties.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/degradation/DegradationServiceTest.java`

- [ ] **Step 1: 定义 DegradationEvent**

```java
package com.codingas.gateway.application.degradation;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import java.time.Instant;

public class DegradationEvent {
    private final String traceId;
    private final Long userId;
    private final String originalModel;
    private final String fallbackModel;
    private final ProviderErrorType reason;
    private final int chainStep;
    private final Instant triggeredAt;

    // 全参构造 + getter
}
```

- [ ] **Step 2: 定义 DegradationProperties**

```java
@ConfigurationProperties(prefix = "gateway.degradation")
public class DegradationProperties {
    private boolean enabled = true;
    private int maxChainDepth = 5;
    private List<DegradationChain> chains = new ArrayList<>();

    public static class DegradationChain {
        private String primary;
        private List<String> fallbacks;
        private RecoveryConfig recovery = new RecoveryConfig();
        // getter/setter
    }

    public static class RecoveryConfig {
        private Duration checkInterval = Duration.ofSeconds(60);
        private int successThreshold = 3;
        // getter/setter
    }
}
```

- [ ] **Step 3: 定义 DegradationService 接口**

```java
public interface DegradationService {
    String degrade(String originalModel, ProviderErrorType reason);
    boolean canRecover(String model);
    void recoveryCheck();
}
```

- [ ] **Step 4: 实现 DegradationServiceImpl**

```java
@Service
public class DegradationServiceImpl implements DegradationService {

    private static final Logger log = LoggerFactory.getLogger(DegradationServiceImpl.class);

    private final DegradationProperties properties;
    private final ProviderHealthTracker healthTracker;
    private final MeterRegistry meterRegistry;
    private final DomainEventPublisher eventPublisher;

    // 降级中的模型 → 降级信息
    private final ConcurrentMap<String, DegradedModel> degradedModels = new ConcurrentHashMap<>();

    // 配置验证在 @PostConstruct 中执行，检查循环引用

    @Override
    public String degrade(String originalModel, ProviderErrorType reason) {
        DegradationChain chain = findChain(originalModel);
        if (chain == null) return null;

        for (int i = 0; i < chain.getFallbacks().size() && i < properties.getMaxChainDepth(); i++) {
            String fallback = chain.getFallbacks().get(i);
            if (isAvailable(fallback)) {
                degradedModels.put(originalModel, new DegradedModel(originalModel, fallback, reason, i + 1));
                meterRegistry.counter("gateway.degradation.triggered",
                        "from_model", originalModel, "to_model", fallback,
                        "reason", reason.name()).increment();
                eventPublisher.publish(new DegradationEvent(..., reason, i + 1, Instant.now()));
                return fallback;
            }
        }
        return null;
    }

    @Override
    public boolean canRecover(String model) {
        DegradedModel degraded = degradedModels.get(model);
        if (degraded == null) return true; // 未降级
        return degraded.isRecovered();
    }

    @Scheduled(fixedDelayString = "${gateway.degradation.recovery-check-interval:60000}")
    @Override
    public void recoveryCheck() {
        for (Map.Entry<String, DegradedModel> entry : degradedModels.entrySet()) {
            String model = entry.getKey();
            DegradedModel degraded = entry.getValue();
            // 执行健康检查
            boolean healthy = healthTracker.getStatus(model).status() == Status.UP;
            if (healthy) {
                degraded.recordSuccess();
                if (degraded.consecutiveSuccesses() >= degraded.recoveryThreshold()) {
                    degraded.markRecovered();
                    meterRegistry.counter("gateway.degradation.recovered", "model", model).increment();
                    log.info("模型 {} 已恢复", model);
                }
            } else {
                degraded.resetSuccesses();
            }
        }
    }

    // 内部类记录降级状态
    static class DegradedModel {
        final String originalModel;
        final String currentModel;
        final ProviderErrorType reason;
        final int chainStep;
        final int recoveryThreshold;
        final AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
        volatile boolean recovered = false;
        // ...
    }
}
```

- [ ] **Step 5: 编写 DegradationServiceTest**

```java
@DisplayName("智能降级服务测试")
class DegradationServiceTest {
    // 降级链配置加载
    // 主模型不可用时返回备选模型
    // 降级链中所有模型不可用时返回 null
    // 循环引用配置拒绝加载
    // 连续 N 次健康检查后标记可恢复
    // 降级时发布 DegradationEvent
}
```

- [ ] **Step 6: 在 ChatDispatchServiceImpl 中集成降级**

在 `dispatch()` 的 catch 块中：

```java
catch (ProviderException e) {
    callLog.setDurationMs(System.currentTimeMillis() - startTime);
    callLog.setSuccess(false);
    callLog.setErrorMessage(e.getMessage());
    auditGateway.saveCallLog(callLog);

    // 尝试降级
    if (degradationService != null) {
        String fallbackModel = degradationService.degrade(request.getModel(), e.getErrorType());
        if (fallbackModel != null) {
            request.setModel(fallbackModel);
            return dispatch(request, identity, strategy); // 递归重试
        }
    }
    throw e;
}
```

- [ ] **Step 7: 运行测试**

Run: `cd gateway-boot && ../mvnw test -Dtest=DegradationServiceTest -pl .`

- [ ] **Step 8: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/degradation/ \
       gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java \
       gateway-boot/src/test/java/com/codingas/gateway/application/degradation/
git commit -m "feat(resilience): 模型级智能降级骨架 — 降级链/自动回切/Metrics"
```
