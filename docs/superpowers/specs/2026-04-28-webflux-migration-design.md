# WebFlux 全量迁移设计方案

**日期**: 2026-04-28
**状态**: 已批准
**版本**: v1.0

---

## 1. 背景与目标

### 1.1 当前架构问题

| 问题 | 现状 | 影响 |
|------|------|------|
| 双客户端并存 | RestClient（同步） + OkHttp（流式） | 维护成本高，增加学习负担 |
| 无连接池共享 | 每个 Adapter 独立创建 OkHttpClient | 高并发下资源浪费 |
| 无拦截器配置 | 未配置日志、重试、认证拦截器 | 可观测性差 |
| 依赖冗余 | okhttp + spring-boot-starter-web | 包体积偏大 |

### 1.2 迁移目标

- **统一技术栈**：单一 WebClient 客户端，涵盖同步和非同步
- **消除双客户端复杂度**：移除 OkHttp，统一使用 Spring WebFlux WebClient
- **全链路响应式**：请求处理全流程非阻塞
- **更好的可观测性**：内置响应式堆栈的指标和追踪

### 1.3 关键决策

| 维度 | 选择 |
|------|------|
| 团队响应式水平 | 有基础，了解 Reactor/WebFlux |
| 最终架构 | 全 WebFlux（纯响应式） |
| 测试策略 | TDD 驱动 |
| 迁移方案 | 大爆炸直接全部替换 |
| 可用性策略 | 低峰期停机窗口切换 |

---

## 2. 目标架构

```
外部请求
    ↓
WebFlux Controller（网关层）
    ↓
认证/鉴权 WebFilter Chain
    ↓
Application Service（应用层 - Flux/Mono）
    ↓
Domain Service（领域层 - Flux/Mono）
    ↓
Infrastructure（WebClient 统一 HTTP 客户端）
    ↓
外部 LLM API（OpenAI / Anthropic）
```

---

## 3. 核心组件设计

### 3.1 依赖变更（pom.xml）

**移除依赖**：
- `okhttp` v4.12.0
- `okhttp-sse` v4.12.0
- `spring-boot-starter-web`（同步 MVC）

**添加依赖**：
- `spring-boot-starter-webflux`（响应式 Web）

```xml
<!-- 移除 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp-sse</artifactId>
</dependency>

<!-- 添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### 3.2 WebClient 配置类

**文件**: `infrastructure/config/WebClientConfig.java`

```java
@Configuration
public class WebClientConfig {

    @Bean
    public ClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(200);
        manager.setDefaultMaxPerRoute(20);
        manager.setValidateAfterInactivity(Duration.ofSeconds(20));
        return manager;
    }

    @Bean
    public WebClient webClient(ClientConnectionManager connectionManager) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpRequestFactory(connectionManager))
            .codecDefaulter(codec -> codec.defaultCodecs().maxInMemorySize(1024 * 1024 * 10))
            .build();
    }
}
```

**配置说明**：
- 连接池最大 200 连接
- 单路由最大 20 连接
- 闲置连接 20 秒后验证
- 最大内存 10MB 用于响应缓存

### 3.3 抽象 LLM 客户端接口

**文件**: `domain/router/gateway/LLMProviderPort.java`

```java
/**
 * LLM 提供商端口接口 - 定义统一的 LLM 调用契约
 */
public interface LLMProviderPort {

    /**
     * 同步聊天完成
     * @param request 聊天请求
     * @return 响应 Mono
     */
    Mono<LLMResponse> chat(ChatRequest request);

    /**
     * 流式聊天完成
     * @param request 聊天请求
     * @param callback 流式回调
     * @return 完成信号 Mono
     */
    Mono<Void> chatStream(ChatRequest request, StreamCallback callback);
}
```

### 3.4 流式处理接口

**文件**: `domain/router/gateway/StreamCallback.java`

```java
/**
 * 流式回调接口 - 用于 SSE 流式响应处理
 */
public interface StreamCallback {

    /**
     * 接收到数据块
     * @param data 原始数据
     */
    void onChunk(String data);

    /**
     * 流完成
     */
    void onComplete();

    /**
     * 流异常
     * @param error 异常信息
     */
    void onError(Throwable error);
}
```

### 3.5 OpenAI Adapter（重写）

**文件**: `infrastructure/adapter/openai/OpenAIAdapter.java`

核心变更：
- 移除 `RestClient` 和 `OkHttpClient`
- 统一使用 `WebClient`
- 返回类型从 `LLMResponse` 改为 `Mono<LLMResponse>`
- 流式处理使用 `exchangeToFlux()`

```java
@Service
public class OpenAIAdapter implements LLMProviderPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String CHAT_COMPLETIONS_URL = "/chat/completions";

    public OpenAIAdapter(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<LLMResponse> chat(ChatRequest request) {
        String requestBody = buildRequestBody(request);

        return webClient.post()
            .uri(CHAT_COMPLETIONS_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseResponse)
            .doOnError(e -> log.error("OpenAI chat error: {}", e.getMessage(), e))
            .onErrorMap(e -> new RuntimeException("OpenAI chat request failed", e));
    }

    @Override
    public Mono<Void> chatStream(ChatRequest request, StreamCallback callback) {
        String requestBody = buildRequestBody(request);

        return webClient.post()
            .uri(CHAT_COMPLETIONS_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchangeToFlux(response -> response.bodyToFlux(String.class))
            .filter(data -> !data.isEmpty() && !"[DONE]".equals(data))
            .doOnNext(callback::onChunk)
            .doOnComplete(callback::onComplete)
            .doOnError(callback::onError)
            .then();
    }

    private String buildRequestBody(ChatRequest request) {
        // 构建 OpenAI 请求体
    }

    private LLMResponse parseResponse(String responseBody) {
        // 解析 OpenAI 响应
    }
}
```

### 3.6 Anthropic Adapter（重写）

**文件**: `infrastructure/adapter/anthropic/AnthropicAdapter.java`

核心变更同上，统一使用 `WebClient`，返回 `Mono<T>` 类型。

### 3.7 管理服务（LLMDispatcher）变更

**文件**: `domain/router/service/LLMDispatcher.java`

```java
@Service
public class LLMDispatcher {

    private final LLMProviderPort openAIAdapter;
    private final LLMProviderPort anthropicAdapter;

    /**
     * 根据提供商分发请求
     * @param request 聊天请求
     * @param provider 提供商标识
     * @return 响应 Mono
     */
    public Mono<LLMResponse> dispatch(ChatRequest request, String provider) {
        LLMProviderPort adapter = selectAdapter(provider);

        if (request.isStream()) {
            return dispatchStream(request, adapter);
        } else {
            return adapter.chat(request);
        }
    }

    /**
     * 流式分发
     * @param request 聊天请求
     * @param adapter 适配器
     * @return 完成信号 Mono
     */
    private Mono<Void> dispatchStream(ChatRequest request, LLMProviderPort adapter) {
        StreamCallback callback = request.getStreamCallback();
        return adapter.chatStream(request, callback);
    }
}
```

### 3.8 应用层（LLMChatUseCase）变更

**文件**: `application/chat/LLMChatUseCase.java`

```java
@Service
public class LLMChatUseCase {

    private final LLMDispatcher dispatcher;
    private final AuthenticationService authService;

    /**
     * 处理聊天请求
     * @param request 聊天请求 DTO
     * @return 聊天响应 Mono
     */
    public Mono<ChatResponse> chat(ChatRequestDTO request) {
        // 认证
        return authService.validateApiKey(request.getApiKey())
            .flatMap(auth -> {
                // 权限检查
                return checkPermission(auth, request.getModel());
            })
            .flatMap(auth -> {
                // 转换为领域对象并分发
                ChatRequest domainRequest = toDomainRequest(request);
                return dispatcher.dispatch(domainRequest, request.getProvider());
            })
            .map(this::toResponseDTO);
    }
}
```

### 3.9 WebFlux 控制器

**文件**: `adapter/chat/controller/OpenAIController.java`

```java
@RestWebFlux
public class OpenAIController {

    private final LLMChatUseCase chatUseCase;

    @PostMapping("/v1/chat/completions")
    public Mono<ResponseEntity<ApiResponse<ChatResponse>>> chat(
            @RequestBody ChatRequestDTO request) {
        return chatUseCase.chat(request)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PostMapping(value = "/v1/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ResponseEntity<Void>> chatStream(@RequestBody ChatRequestDTO request) {
        // 流式响应处理
    }
}
```

---

## 4. 迁移文件清单

| 层级 | 文件 | 操作 | 优先级 |
|------|------|------|--------|
| **依赖** | pom.xml | 改依赖 | P0 |
| **配置** | WebClientConfig.java | 新增 | P0 |
| **端口** | LLMProviderPort.java | 改接口 | P0 |
| **回调** | StreamCallback.java | 改接口 | P0 |
| **基础设施** | OpenAIAdapter.java | 重写 | P0 |
| **基础设施** | AnthropicAdapter.java | 重写 | P0 |
| **领域** | LLMDispatcher.java | 改返回类型 | P1 |
| **应用** | LLMChatUseCase.java | 改返回类型 | P1 |
| **适配器** | OpenAIController.java | 改WebFlux | P1 |
| **适配器** | AnthropicController.java | 改WebFlux | P1 |
| **异常** | GlobalExceptionHandler.java | 适配响应式 | P1 |
| **测试** | OpenAIAdapterTest.java | 新增 | P0 |
| **测试** | AnthropicAdapterTest.java | 新增 | P0 |

---

## 5. TDD 驱动迁移流程

### Phase 1: 准备阶段
1. 添加 `spring-boot-starter-webflux` 依赖
2. 创建 `WebClientConfig` 配置类
3. 编写 `WebClientConfig` 单元测试

### Phase 2: 抽象接口重构
1. 修改 `LLMProviderPort` 接口返回 `Mono/Flux`
2. 修改 `StreamCallback` 接口
3. 编写接口层测试

### Phase 3: 基础设施层迁移
1. 重写 `OpenAIAdapter` 使用 `WebClient`
2. 重写 `AnthropicAdapter` 使用 `WebClient`
3. TDD：先写测试，再实现

### Phase 4: 领域层迁移
1. 修改 `LLMDispatcher` 返回 `Mono`
2. 编写管理服务测试

### Phase 5: 应用层迁移
1. 修改 `LLMChatUseCase` 返回 `Mono`
2. 编写管理服务测试

### Phase 6: 适配器层迁移
1. 将 `@RestController` 改为 `@RestWebFlux`
2. 修改返回类型为 `Mono<ResponseEntity<T>>`
3. 端到端测试

### Phase 7: 清理阶段
1. 移除 `okhttp` 依赖
2. 移除 `spring-boot-starter-web`（如有冲突）
3. 全量回归测试

---

## 6. 关键风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 响应式异常处理模式差异 | 错误传播不同 | 统一使用 `.onErrorMap()` 封装 |
| 背压处理缺失 | SSE 流控问题 | 使用 WebFlux 内置背压机制 |
| 线程模型变化 | 调试困难 | 添加响应式堆栈日志 |
| 测试复杂度增加 | 单元测试需 mock `Mono/Flux` | 使用 StepVerifier 进行测试 |

---

## 7. 验收标准

- [ ] 所有 HTTP 调用统一使用 `WebClient`
- [ ] 移除 `okhttp` 和 `okhttp-sse` 依赖
- [ ] 所有 Service 层方法返回 `Mono<T>` 或 `Flux<T>`
- [ ] 控制器使用 `@RestWebFlux` 注解
- [ ] 流式和非流式请求均正常工作
- [ ] 单元测试覆盖率 ≥ 80%
- [ ] 端到端测试（手动）通过
- [ ] 代码中无 `RestClient` 和 `OkHttpClient` 引用

---

## 8. 测试策略

### 单元测试（必须 TDD）

```java
@Test
void chat_shouldReturnMono_whenRequestValid() {
    // Given
    ChatRequest request = new ChatRequest("gpt-4", "Hello");
    when(webClient.post()).thenReturn(...);

    // When
    Mono<LLMResponse> result = adapter.chat(request);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(response -> response.getContent().contains("Hello"))
        .verifyComplete();
}
```

### 集成测试

- 使用 `WebTestClient` 测试 WebFlux 端点
- 使用 MockServer 模拟外部 LLM API

---

## 9. 设计决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| 连接池位置 | 共享 Bean | 避免每个 Adapter 独立创建 |
| 流式处理方式 | `exchangeToFlux()` | WebFlux 内置 SSE 支持 |
| 错误处理 | `.onErrorMap()` | 统一封装为运行时异常 |
| 测试框架 | StepVerifier | 响应式代码标准测试方式 |

---

**审批状态**: 用户已批准（2026-04-28）
