# WebFlux 全量迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 HTTP 客户端从 RestClient+OkHttp 迁移到 Spring WebFlux WebClient，实现全链路响应式

**Architecture:** 统一使用 WebClient 替换双客户端，非流式返回 `Mono<LLMResponse>`，流式返回 `Mono<Void>` 并通过 `Flux<String>` 实现 SSE 流式处理

**Tech Stack:** Java 21, Spring Boot 3.5, Spring WebFlux WebClient, Reactor, JUnit 5, Mockito, StepVerifier

---

## 文件结构概览

| 层级 | 文件 | 操作 |
|------|------|------|
| **依赖** | `pom.xml` | 修改 |
| **配置** | `infrastructure/config/WebClientConfig.java` | 新增 |
| **端口** | `domain/router/gateway/LLMProviderPort.java` | 修改 |
| **基础设施** | `infrastructure/adapter/openai/OpenAIAdapter.java` | 重写 |
| **基础设施** | `infrastructure/adapter/anthropic/AnthropicAdapter.java` | 重写 |
| **领域** | `domain/router/service/LLMDispatcher.java` | 修改 |
| **测试** | `src/test/java/.../OpenAIAdapterTest.java` | 新增 |
| **测试** | `src/test/java/.../AnthropicAdapterTest.java` | 新增 |
| **测试** | `src/test/java/.../LLMDispatcherTest.java` | 新增 |

---

## Task 1: 依赖变更 (pom.xml)

**Files:**
- Modify: `pom.xml:29-84` (移除 okhttp 依赖)
- Modify: `pom.xml:166-171` (替换 spring-boot-starter-web → spring-boot-starter-webflux)

- [ ] **Step 1: 移除 okhttp 版本属性**

编辑 `okhttp.version` 所在行 (约第 30 行)，删除整段：
```xml
<okhttp.version>4.12.0</okhttp.version>
```

- [ ] **Step 2: 移除 okhttp 依赖管理**

删除 dependencyManagement 中约第 68-84 行的 okhttp 相关依赖：
```xml
<!-- 删除这些行 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>${okhttp.version}</version>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp-sse</artifactId>
    <version>${okhttp.version}</version>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <version>${okhttp.version}</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: 替换 web 依赖为 webflux**

在 dependencies 中替换 `spring-boot-starter-web` 为 `spring-boot-starter-webflux`：
```xml
<!-- 删除 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- 添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

- [ ] **Step 4: 移除 okhttp 运行时依赖**

删除约第 218-224 行：
```xml
<!-- 删除这些行 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp-sse</artifactId>
</dependency>
```

- [ ] **Step 5: 验证编译**

运行: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -q`
期望: 成功（无编译错误）

- [ ] **Step 6: 提交**

```bash
git add pom.xml && git commit -m "refactor: replace spring-boot-starter-web with webflux

- Remove okhttp 4.12.0 dependencies
- Add spring-boot-starter-webflux for unified reactive HTTP client"
```

---

## Task 2: WebClient 配置类

**Files:**
- Create: `src/main/java/com/codingas/gateway/infrastructure/config/WebClientConfig.java`

- [ ] **Step 1: 创建 WebClientConfig 配置类**

```java
package com.codingas.gateway.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpRequestFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * WebClient 配置类
 *
 * <p>提供共享的 WebClient 实例和连接池配置。</p>
 */
@Slf4j
@Configuration
public class WebClientConfig {

    /**
     * 创建共享连接池
     */
    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(200);
        manager.setDefaultMaxPerRoute(20);
        manager.setValidateAfterInactivity(Duration.ofSeconds(20).toMillis());
        log.info("WebClient connection pool configured: maxTotal=200, maxPerRoute=20");
        return manager;
    }

    /**
     * 创建请求配置（超时设置）
     */
    @Bean
    public RequestConfig requestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                .setResponseTimeout(Timeout.ofSeconds(120))
                .build();
    }

    /**
     * 创建共享 WebClient 实例
     */
    @Bean
    public WebClient webClient(PoolingHttpClientConnectionManager connectionManager, RequestConfig requestConfig) {
        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpRequestFactory(httpClient))
                .codecDefaulter(codec -> codec.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
```

- [ ] **Step 2: 验证编译**

运行: `./mvnw compile -q`
期望: 成功

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/config/WebClientConfig.java
git commit -m "feat: add WebClientConfig with shared connection pool

- PoolingHttpClientConnectionManager with 200 max connections
- RequestConfig with 30s connect timeout, 120s read timeout
- Shared WebClient bean for all adapters"
```

---

## Task 3: LLMProviderPort 接口重构

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/router/gateway/LLMProviderPort.java`

- [ ] **Step 1: 添加 Mono/Flux 导入**

在文件顶部添加：
```java
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
```

- [ ] **Step 2: 修改 chat 方法签名**

原：
```java
LLMResponse chat(LLMRequest request);
```

改：
```java
Mono<LLMResponse> chat(LLMRequest request);
```

- [ ] **Step 3: 修改 chatStream 方法签名**

原：
```java
void chatStream(LLMRequest request, StreamCallback callback);
```

改：
```java
Mono<Void> chatStream(LLMRequest request, StreamCallback callback);
```

- [ ] **Step 4: 修改 messages 方法签名**

原：
```java
LLMResponse messages(LLMRequest request);
```

改：
```java
Mono<LLMResponse> messages(LLMRequest request);
```

- [ ] **Step 5: 添加 messagesStream 方法**

原文件无此方法，需要添加：
```java
/**
 * 发送 Anthropic 流式消息 API 请求
 *
 * @param request LLM 请求
 * @param callback 流式响应回调
 * @return 完成信号 Mono
 */
Mono<Void> messagesStream(LLMRequest request, StreamCallback callback);
```

- [ ] **Step 6: 更新 Javadoc**

将所有返回类型的 Javadoc 从 `@return LLM 响应` 改为 `@return LLM 响应 Mono`

- [ ] **Step 7: 验证编译**

运行: `./mvnw compile -q`
期望: 成功（可能有未实现错误，等待 Task 4-5 修复）

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/codingas/gateway/domain/router/gateway/LLMProviderPort.java
git commit -m "refactor: update LLMProviderPort to return Mono/Flux

- chat() returns Mono<LLMResponse>
- chatStream() returns Mono<Void>
- messages() returns Mono<LLMResponse>
- messagesStream() returns Mono<Void>"
```

---

## Task 4: OpenAIAdapter 重写

**Files:**
- Modify: `src/main/java/com/codingas/gateway/infrastructure/adapter/openai/OpenAIAdapter.java`

- [ ] **Step 1: 重写导入和成员变量**

移除 okhttp 相关导入，添加 webflux：
```java
package com.codingas.gateway.infrastructure.adapter.openai;

import com.codingas.gateway.infrastructure.adapter.LLMProviderAdapter;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import com.codingas.gateway.common.ProviderCapabilities;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
```

成员变量改为：
```java
public class OpenAIAdapter implements LLMProviderAdapter {

    public static final String PROVIDER_CODE = "openai";
    private static final String CHAT_COMPLETIONS_URL = "/v1/chat/completions";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;
```

- [ ] **Step 2: 修改构造函数**

```java
public OpenAIAdapter(WebClient webClient, String baseUrl, String apiKey, int timeoutSeconds) {
    this.webClient = webClient;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.timeoutSeconds = timeoutSeconds;
    this.objectMapper = new ObjectMapper();
}
```

- [ ] **Step 3: 重写 chat 方法**

```java
@Override
public Mono<LLMResponse> chat(LLMRequest request) {
    log.info("OpenAI chat request: model={}, stream=false", request.getModel());

    Map<String, Object> requestBody = buildRequestBody(request);

    return webClient.post()
            .uri(baseUrl + CHAT_COMPLETIONS_URL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseResponse)
            .doOnSuccess(llmResponse -> log.info("OpenAI chat response: id={}, model={}",
                    llmResponse.getId(), llmResponse.getModel()))
            .doOnError(e -> log.error("OpenAI chat error: model={}, error={}",
                    request.getModel(), e.getMessage(), e))
            .onErrorMap(e -> new RuntimeException("OpenAI chat request failed", e));
}
```

- [ ] **Step 4: 重写 chatStream 方法**

```java
@Override
public Mono<Void> chatStream(LLMRequest request, StreamCallback callback) {
    log.info("OpenAI chat stream request: model={}, stream=true", request.getModel());

    request.setStream(true);
    Map<String, Object> requestBody = buildRequestBody(request);

    return webClient.post()
            .uri(baseUrl + CHAT_COMPLETIONS_URL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(requestBody)
            .exchangeToFlux(response -> response.bodyToFlux(String.class))
            .filter(data -> !data.isEmpty() && !"[DONE]".equals(data))
            .doOnNext(data -> {
                log.debug("OpenAI stream chunk: {}", data);
                callback.onChunk(data);
            })
            .doOnComplete(() -> {
                log.info("OpenAI stream completed");
                callback.onComplete();
            })
            .doOnError(error -> {
                log.error("OpenAI stream error: {}", error.getMessage(), error);
                callback.onError(error);
            })
            .then()
            .onErrorMap(e -> {
                if (e instanceof RuntimeException) {
                    return (RuntimeException) e;
                }
                return new RuntimeException("OpenAI stream request failed", e);
            });
}
```

- [ ] **Step 5: 更新 messages 方法**

```java
@Override
public Mono<LLMResponse> messages(LLMRequest request) {
    return Mono.error(new UnsupportedOperationException(
            "OpenAI adapter does not support Anthropic messages format. Use chat() instead."));
}

@Override
public Mono<Void> messagesStream(LLMRequest request, StreamCallback callback) {
    return Mono.error(new UnsupportedOperationException(
            "OpenAI adapter does not support Anthropic messages format. Use chatStream() instead."));
}
```

- [ ] **Step 6: 验证编译**

运行: `./mvnw compile -q`
期望: 成功

- [ ] **Step 7: 提交**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/adapter/openai/OpenAIAdapter.java
git commit -m "refactor: rewrite OpenAIAdapter with WebClient

- Replace RestClient+OkHttp with WebClient
- Return Mono<LLMResponse> for chat()
- Return Mono<Void> with Flux SSE for chatStream()
- Remove blocking SSE EventSourceListener"
```

---

## Task 5: AnthropicAdapter 重写

**Files:**
- Modify: `src/main/java/com/codingas/gateway/infrastructure/adapter/anthropic/AnthropicAdapter.java`

- [ ] **Step 1: 重写导入和成员变量**

```java
package com.codingas.gateway.infrastructure.adapter.anthropic;

import com.codingas.gateway.infrastructure.adapter.LLMProviderAdapter;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import com.codingas.gateway.common.ProviderCapabilities;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
```

成员变量：
```java
public class AnthropicAdapter implements LLMProviderAdapter {

    public static final String PROVIDER_CODE = "anthropic";
    private static final String MESSAGES_URL = "/v1/messages";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String version;
    private final int timeoutSeconds;
```

- [ ] **Step 2: 修改构造函数**

```java
public AnthropicAdapter(WebClient webClient, String baseUrl, String apiKey, String version, int timeoutSeconds) {
    this.webClient = webClient;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.version = version != null ? version : "2023-06-01";
    this.timeoutSeconds = timeoutSeconds;
    this.objectMapper = new ObjectMapper();
}
```

- [ ] **Step 3: 更新 chat 方法**

```java
@Override
public Mono<LLMResponse> chat(LLMRequest request) {
    return Mono.error(new UnsupportedOperationException(
            "Anthropic adapter does not support OpenAI chat format. Use messages() instead."));
}
```

- [ ] **Step 4: 更新 chatStream 方法**

```java
@Override
public Mono<Void> chatStream(LLMRequest request, StreamCallback callback) {
    return Mono.error(new UnsupportedOperationException(
            "Anthropic adapter does not support OpenAI chat format. Use messagesStream() instead."));
}
```

- [ ] **Step 5: 重写 messages 方法**

```java
@Override
public Mono<LLMResponse> messages(LLMRequest request) {
    log.info("Anthropic messages request: model={}, stream=false", request.getModel());

    Map<String, Object> requestBody = buildMessagesRequestBody(request);

    return webClient.post()
            .uri(baseUrl + MESSAGES_URL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("anthropic-version", version)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseResponse)
            .doOnSuccess(llmResponse -> log.info("Anthropic messages response: id={}, model={}",
                    llmResponse.getId(), llmResponse.getModel()))
            .doOnError(e -> log.error("Anthropic messages error: model={}, error={}",
                    request.getModel(), e.getMessage(), e))
            .onErrorMap(e -> new RuntimeException("Anthropic messages request failed", e));
}
```

- [ ] **Step 6: 重写 messagesStream 方法**

```java
@Override
public Mono<Void> messagesStream(LLMRequest request, StreamCallback callback) {
    log.info("Anthropic messages stream request: model={}, stream=true", request.getModel());

    request.setStream(true);
    Map<String, Object> requestBody = buildMessagesRequestBody(request);

    return webClient.post()
            .uri(baseUrl + MESSAGES_URL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("anthropic-version", version)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(requestBody)
            .exchangeToFlux(response -> response.bodyToFlux(String.class))
            .filter(data -> !data.isEmpty() && !"[DONE]".equals(data))
            .doOnNext(data -> {
                log.debug("Anthropic stream chunk: {}", data);
                callback.onChunk(data);
            })
            .doOnComplete(() -> {
                log.info("Anthropic stream completed");
                callback.onComplete();
            })
            .doOnError(error -> {
                log.error("Anthropic stream error: {}", error.getMessage(), error);
                callback.onError(error);
            })
            .then()
            .onErrorMap(e -> {
                if (e instanceof RuntimeException) {
                    return (RuntimeException) e;
                }
                return new RuntimeException("Anthropic stream request failed", e);
            });
}
```

- [ ] **Step 7: 验证编译**

运行: `./mvnw compile -q`
期望: 成功

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/codingas/gateway/infrastructure/adapter/anthropic/AnthropicAdapter.java
git commit -m "refactor: rewrite AnthropicAdapter with WebClient

- Replace RestClient+OkHttp with WebClient
- Return Mono<LLMResponse> for messages()
- Return Mono<Void> with Flux SSE for messagesStream()
- Remove blocking SSE EventSourceListener"
```

---

## Task 6: LLMDispatcher 重构

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/router/service/LLMDispatcher.java`

- [ ] **Step 1: 添加 Reactor 导入**

```java
import reactor.core.publisher.Mono;
```

- [ ] **Step 2: 修改 send 方法**

原：
```java
public LLMResponse send(LLMRequest request, RouteGroup.RoutingStrategy strategy)
```

改：
```java
public Mono<LLMResponse> send(LLMRequest request, RouteGroup.RoutingStrategy strategy)
```

方法体：
```java
public Mono<LLMResponse> send(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
    log.debug("Dispatching request, model={}, strategy={}", request.getModel(), strategy);

    LLMProviderPort adapter = modelRouter.select(request, strategy);
    log.debug("Selected provider: {}", adapter.getProviderCode());

    return adapter.chat(request);
}
```

- [ ] **Step 3: 修改 sendStream 方法**

原：
```java
public void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk)
```

改：
```java
public Mono<Void> sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk)
```

方法体：
```java
public Mono<Void> sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk) {
    log.debug("Dispatching stream request, model={}, strategy={}", request.getModel(), strategy);

    LLMProviderPort adapter = modelRouter.select(request, strategy);
    log.debug("Selected provider for stream: {}", adapter.getProviderCode());

    StreamCallback callback = new com.codingas.gateway.infrastructure.adapter.StreamCallbackImpl(onChunk);
    return adapter.chatStream(request, callback);
}
```

- [ ] **Step 4: 移除 Consumer 导入**

```java
// 删除
import java.util.function.Consumer;
```

- [ ] **Step 5: 验证编译**

运行: `./mvnw compile -q`
期望: 成功

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/codingas/gateway/domain/router/service/LLMDispatcher.java
git commit -m "refactor: update LLMDispatcher to return Mono

- send() returns Mono<LLMResponse>
- sendStream() returns Mono<Void>"
```

---

## Task 7: OpenAIAdapterTest

**Files:**
- Create: `src/test/java/com/codingas/gateway/infrastructure/adapter/openai/OpenAIAdapterTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.codingas.gateway.infrastructure.adapter.openai;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAI Adapter Tests")
class OpenAIAdapterTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private OpenAIAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new OpenAIAdapter(webClient, "https://api.openai.com", "test-api-key", 30);
    }

    @Test
    @DisplayName("chat() returns Mono with valid response")
    void chat_shouldReturnMonoWithValidResponse() {
        // Given
        LLMRequest request = new LLMRequest();
        request.setModel("gpt-4");
        request.setMessages(List.of(Map.of("role", "user", "content", "Hello")));

        String responseJson = """
            {
                "id": "chatcmpl-123",
                "model": "gpt-4",
                "choices": [{
                    "message": {"role": "assistant", "content": "Hello!"},
                    "finish_reason": "stop"
                }],
                "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
            }
            """;

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseJson));

        // When
        Mono<LLMResponse> result = adapter.chat(request);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        "chatcmpl-123".equals(response.getId()) &&
                        "gpt-4".equals(response.getModel()) &&
                        "Hello!".equals(response.getContent().getText()))
                .verifyComplete();
    }

    @Test
    @DisplayName("chat() returns error when API fails")
    void chat_shouldReturnErrorWhenApiFails() {
        // Given
        LLMRequest request = new LLMRequest();
        request.setModel("gpt-4");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("API Error")));

        // When
        Mono<LLMResponse> result = adapter.chat(request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("OpenAI chat request failed"))
                .verify();
    }

    @Test
    @DisplayName("messages() throws UnsupportedOperationException")
    void messages_shouldThrowUnsupportedOperationException() {
        // Given
        LLMRequest request = new LLMRequest();

        // When
        Mono<LLMResponse> result = adapter.messages(request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof UnsupportedOperationException &&
                        e.getMessage().contains("does not support Anthropic messages format"))
                .verify();
    }
}
```

- [ ] **Step 2: 验证测试通过**

运行: `./mvnw test -Dtest=OpenAIAdapterTest`
期望: PASS

- [ ] **Step 3: 提交**

```bash
git add src/test/java/com/codingas/gateway/infrastructure/adapter/openai/OpenAIAdapterTest.java
git commit -m "test: add OpenAIAdapterTest with WebClient

- Test chat() returns Mono with valid response
- Test chat() returns error when API fails
- Test messages() throws UnsupportedOperationException"
```

---

## Task 8: AnthropicAdapterTest

**Files:**
- Create: `src/test/java/com/codingas/gateway/infrastructure/adapter/anthropic/AnthropicAdapterTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.codingas.gateway.infrastructure.adapter.anthropic;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Anthropic Adapter Tests")
class AnthropicAdapterTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private AnthropicAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AnthropicAdapter(webClient, "https://api.anthropic.com", "test-api-key", "2023-06-01", 30);
    }

    @Test
    @DisplayName("messages() returns Mono with valid response")
    void messages_shouldReturnMonoWithValidResponse() {
        // Given
        LLMRequest request = new LLMRequest();
        request.setModel("claude-3-5-sonnet");
        request.setMessages(List.of(Map.of("role", "user", "content", "Hello")));

        String responseJson = """
            {
                "id": "msg_123",
                "model": "claude-3-5-sonnet",
                "content": [{"type": "text", "text": "Hello!"}],
                "stop_reason": "end_turn",
                "usage": {"input_tokens": 10, "output_tokens": 5}
            }
            """;

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseJson));

        // When
        Mono<LLMResponse> result = adapter.messages(request);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        "msg_123".equals(response.getId()) &&
                        "claude-3-5-sonnet".equals(response.getModel()) &&
                        "Hello!".equals(response.getContent().getText()))
                .verifyComplete();
    }

    @Test
    @DisplayName("chat() throws UnsupportedOperationException")
    void chat_shouldThrowUnsupportedOperationException() {
        // Given
        LLMRequest request = new LLMRequest();

        // When
        Mono<LLMResponse> result = adapter.chat(request);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof UnsupportedOperationException &&
                        e.getMessage().contains("does not support OpenAI chat format"))
                .verify();
    }
}
```

- [ ] **Step 2: 验证测试通过**

运行: `./mvnw test -Dtest=AnthropicAdapterTest`
期望: PASS

- [ ] **Step 3: 提交**

```bash
git add src/test/java/com/codingas/gateway/infrastructure/adapter/anthropic/AnthropicAdapterTest.java
git commit -m "test: add AnthropicAdapterTest with WebClient

- Test messages() returns Mono with valid response
- Test chat() throws UnsupportedOperationException"
```

---

## Task 9: LLMDispatcherTest

**Files:**
- Create: `src/test/java/com/codingas/gateway/domain/router/service/LLMDispatcherTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.gateway.LLMProviderPort;
import com.codingas.gateway.domain.router.gateway.ModelRouter;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LLM Dispatcher Tests")
class LLMDispatcherTest {

    @Mock
    private ModelRouter modelRouter;

    @Mock
    private LLMProviderPort adapter;

    private LLMDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new LLMDispatcher(modelRouter);
    }

    @Test
    @DisplayName("send() returns Mono from selected adapter")
    void send_shouldReturnMonoFromSelectedAdapter() {
        // Given
        LLMRequest request = new LLMRequest();
        request.setModel("gpt-4");

        LLMResponse expectedResponse = LLMResponse.builder()
                .id("response-123")
                .model("gpt-4")
                .content(LLMResponse.Content.builder().text("Hello!").build())
                .build();

        when(modelRouter.select(any(), any())).thenReturn(adapter);
        when(adapter.chat(any())).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<LLMResponse> result = dispatcher.send(request, RouteGroup.RoutingStrategy.COST_LOWEST);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> "response-123".equals(response.getId()))
                .verifyComplete();

        verify(modelRouter).select(request, RouteGroup.RoutingStrategy.COST_LOWEST);
        verify(adapter).chat(request);
    }

    @Test
    @DisplayName("send() returns error when adapter fails")
    void send_shouldReturnErrorWhenAdapterFails() {
        // Given
        LLMRequest request = new LLMRequest();
        request.setModel("gpt-4");

        when(modelRouter.select(any(), any())).thenReturn(adapter);
        when(adapter.chat(any())).thenReturn(Mono.error(new RuntimeException("Adapter error")));

        // When
        Mono<LLMResponse> result = dispatcher.send(request, RouteGroup.RoutingStrategy.COST_LOWEST);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("Adapter error"))
                .verify();
    }
}
```

- [ ] **Step 2: 验证测试通过**

运行: `./mvnw test -Dtest=LLMDispatcherTest`
期望: PASS

- [ ] **Step 3: 提交**

```bash
git add src/test/java/com/codingas/gateway/domain/router/service/LLMDispatcherTest.java
git commit -m "test: add LLMDispatcherTest

- Test send() returns Mono from selected adapter
- Test send() returns error when adapter fails"
```

---

## Task 10: 全量测试验证

- [ ] **Step 1: 运行所有单元测试**

运行: `./mvnw test`
期望: 所有测试通过，覆盖率报告生成

- [ ] **Step 2: 编译打包验证**

运行: `./mvnw package -DskipTests`
期望: JAR 包成功生成

- [ ] **Step 3: 检查依赖树**

运行: `./mvnw dependency:tree -q | grep -E "(okhttp|webflux)"`
期望: okhttp 不出现，webflux 出现

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "test: run full test suite after WebFlux migration

- All unit tests pass
- Package builds successfully
- OkHttp removed, WebFlux present in dependency tree"
```

---

## 自检清单

执行后运行以下检查：

1. **Spec 覆盖**: 检查 `2026-04-28-webflux-migration-design.md` 中的每个需求是否都有对应任务
2. **占位符扫描**: 检查是否还有 "TODO", "TBD", "implement later" 等
3. **类型一致性**: 检查接口方法签名在整个计划中是否一致
4. **编译验证**: `./mvnw compile -q` 无错误
5. **测试验证**: `./mvnw test` 全部通过

---

**计划完成时间估算**: 约 2-3 小时（取决于团队对响应式编程的熟悉程度）

**Plan saved to**: `docs/superpowers/plans/2026-04-28-webflux-migration-plan.md`
