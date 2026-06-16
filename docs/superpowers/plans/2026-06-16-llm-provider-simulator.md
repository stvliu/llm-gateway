---
change: llm-provider-simulator
design-doc: docs/superpowers/specs/2026-06-16-llm-provider-simulator-design.md
base-ref: 03271624544114b9cbda6115114fbf24aa0e6434
---

# LLM Provider Simulator 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 OpenAIUpstreamClient 和 AnthropicUpstreamClient 提供 MockWebServer 驱动的 HTTP 层单元测试，并构建独立运行的模拟服务用于集成验证。

**Architecture:** 两阶段交付。第一阶段在 `gateway-boot/src/test/` 中创建 `ResponseTemplates`（纯静态 JSON 模板）和 `ProviderSimulator`（MockWebServer 封装），然后编写两个 UpstreamClient 的单元测试。第二阶段创建独立 `gateway-simulator` Maven 模块，包含 Spring Boot 应用、模式管理服务和模拟端点 Controller。

**Tech Stack:** Java 21, OkHttp MockWebServer 4.12.0（项目已有依赖）, JUnit 5, AssertJ, Spring Boot 3.5.x, SseEmitter

---

## 文件结构总览

### 第一阶段：测试工具包（gateway-boot）

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 创建 | `gateway-boot/src/test/java/com/codingas/gateway/support/ResponseTemplates.java` | 纯静态 JSON 响应模板工厂 |
| 创建 | `gateway-boot/src/test/java/com/codingas/gateway/support/ProviderSimulator.java` | MockWebServer 封装，AutoCloseable |
| 创建 | `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/OpenAIUpstreamClientTest.java` | OpenAI 客户端 8 场景测试 |
| 创建 | `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/AnthropicUpstreamClientTest.java` | Anthropic 客户端 8 场景测试 |

### 第二阶段：独立模拟服务（gateway-simulator）

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 创建 | `gateway-simulator/pom.xml` | 模块 POM，依赖 spring-boot-starter-web |
| 修改 | `pom.xml` | 父 POM 添加 gateway-simulator 模块 |
| 创建 | `gateway-simulator/src/main/java/com/codingas/simulator/LLMProviderSimulatorApplication.java` | Spring Boot 启动类 |
| 创建 | `gateway-simulator/src/main/java/com/codingas/simulator/template/SimulatorResponseTemplates.java` | 模拟服务响应模板（复制自第一阶段逻辑） |
| 创建 | `gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java` | 模式管理 + 请求记录（环形缓冲） |
| 创建 | `gateway-simulator/src/main/java/com/codingas/simulator/service/RequestRecord.java` | 请求记录 record |
| 创建 | `gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorController.java` | 模拟端点（/v1/chat/completions, /v1/messages） |
| 创建 | `gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorAdminController.java` | 管理 API（模式切换 + 请求记录查询） |
| 创建 | `gateway-simulator/src/main/resources/application.yml` | 配置文件 |
| 创建 | `gateway-simulator/src/test/java/com/codingas/simulator/SimulatorModeServiceTest.java` | 模式服务单元测试 |
| 创建 | `gateway-simulator/src/test/java/com/codingas/simulator/SimulatorIntegrationTest.java` | Spring Boot 集成测试 |

---

## 第一阶段：测试工具包

### Task 1: 创建 ResponseTemplates 响应模板工厂

**Files:**
- 创建: `gateway-boot/src/test/java/com/codingas/gateway/support/ResponseTemplates.java`

- [ ] **Step 1: 创建 ResponseTemplates 类**

```java
package com.codingas.gateway.support;

/**
 * LLM Provider 响应模板工厂
 *
 * <p>提供 OpenAI 和 Anthropic 协议的标准 JSON 响应模板，
 * 用于 MockWebServer 构建模拟响应。</p>
 */
public final class ResponseTemplates {

    private ResponseTemplates() {}

    /**
     * OpenAI Chat Completion 非流式成功响应
     */
    public static String openaiChatCompletion() {
        return """
                {
                  "id": "chatcmpl-test123",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "Hello! How can I help you today?"
                    },
                    "finish_reason": "stop"
                  }],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 8,
                    "total_tokens": 18
                  }
                }
                """;
    }

    /**
     * OpenAI SSE 流式响应块（3 个 data 块 + [DONE] 终止标记）
     */
    public static String openaiStreamChunks() {
        return "data: {\"id\":\"chatcmpl-stream1\",\"object\":\"chat.completion.chunk\",\"created\":1700000000,\"model\":\"gpt-4\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"Hello\"},\"finish_reason\":null}]}\n\n"
             + "data: {\"id\":\"chatcmpl-stream1\",\"object\":\"chat.completion.chunk\",\"created\":1700000000,\"model\":\"gpt-4\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"! How\"},\"finish_reason\":null}]}\n\n"
             + "data: {\"id\":\"chatcmpl-stream1\",\"object\":\"chat.completion.chunk\",\"created\":1700000000,\"model\":\"gpt-4\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\" can I help?\"},\"finish_reason\":\"stop\"}]}\n\n"
             + "data: [DONE]\n\n";
    }

    /**
     * OpenAI 错误响应
     *
     * @param code HTTP 状态码
     */
    public static String openaiError(int code) {
        String type = switch (code) {
            case 401 -> "authentication_error";
            case 429 -> "rate_limit_error";
            case 500 -> "server_error";
            default -> "invalid_request_error";
        };
        return """
                {
                  "error": {
                    "type": "%s",
                    "message": "OpenAI error: HTTP %d"
                  }
                }
                """.formatted(type, code);
    }

    /**
     * Anthropic Messages 非流式成功响应
     */
    public static String anthropicMessages() {
        return """
                {
                  "id": "msg_test123",
                  "type": "message",
                  "role": "assistant",
                  "model": "claude-3-5-sonnet-20241022",
                  "content": [{
                    "type": "text",
                    "text": "Hello! How can I help you today?"
                  }],
                  "stop_reason": "end_turn",
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 8
                  }
                }
                """;
    }

    /**
     * Anthropic SSE 流式响应块（event + data 块 × 3 + message_stop 终止标记）
     */
    public static String anthropicStreamChunks() {
        return "event: content_block_start\n"
             + "data: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}\n\n"
             + "event: content_block_delta\n"
             + "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"Hello\"}}\n\n"
             + "event: content_block_delta\n"
             + "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"! How can I help?\"}}\n\n"
             + "event: message_stop\n"
             + "data: {\"type\":\"message_stop\"}\n\n";
    }

    /**
     * Anthropic 错误响应
     *
     * @param code HTTP 状态码
     */
    public static String anthropicError(int code) {
        String type = switch (code) {
            case 401 -> "authentication_error";
            case 429 -> "rate_limit_error";
            case 500 -> "api_error";
            default -> "invalid_request_error";
        };
        return """
                {
                  "type": "error",
                  "error": {
                    "type": "%s",
                    "message": "Anthropic error: HTTP %d"
                  }
                }
                """.formatted(type, code);
    }
}
```

- [ ] **Step 2: 验证编译通过**

运行: `./mvnw compile -pl gateway-boot -q`
预期: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/support/ResponseTemplates.java
git commit -m "test(simulator): 添加 ResponseTemplates 响应模板工厂"
```

---

### Task 2: 创建 ProviderSimulator MockWebServer 封装

**Files:**
- 创建: `gateway-boot/src/test/java/com/codingas/gateway/support/ProviderSimulator.java`

**前置:** Task 1（ResponseTemplates）已完成

- [ ] **Step 1: 创建 ProviderSimulator 类**

```java
package com.codingas.gateway.support;

import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.infrastructure.supply.upstream.AnthropicUpstreamClient;
import com.codingas.gateway.infrastructure.supply.upstream.OpenAIUpstreamClient;
import com.codingas.gateway.infrastructure.upstream.AnthropicErrorClassifier;
import com.codingas.gateway.infrastructure.upstream.ErrorClassificationStrategy;
import com.codingas.gateway.infrastructure.upstream.OpenAIErrorClassifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MockWebServer;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.RecordedResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Provider 模拟器 — 基于 MockWebServer 封装
 *
 * <p>提供 OpenAI / Anthropic 上游 API 模拟，支持 try-with-resources。</p>
 */
public class ProviderSimulator implements AutoCloseable {

    private final MockWebServer server;
    private final OkHttpClient sharedClient;
    private final ObjectMapper sharedMapper;

    private ProviderSimulator() throws IOException {
        server = new MockWebServer();
        server.start();
        sharedClient = new OkHttpClient.Builder().build();
        sharedMapper = new ObjectMapper();
        sharedMapper.findAndRegisterModules();
    }

    /**
     * 创建并启动模拟服务器
     */
    public static ProviderSimulator create() {
        try {
            return new ProviderSimulator();
        } catch (IOException e) {
            throw new RuntimeException("启动 MockWebServer 失败", e);
        }
    }

    /**
     * 获取模拟服务器 base URL（如 http://127.0.0.1:xxxxx）
     */
    public String getUrl() {
        return server.url("").toString();
    }

    /**
     * 入队 OpenAI 非流式成功响应
     */
    public void enqueueOpenAISuccess() {
        server.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setBody(ResponseTemplates.openaiChatCompletion())
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));
    }

    /**
     * 入队 Anthropic 非流式成功响应
     */
    public void enqueueAnthropicSuccess() {
        server.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setBody(ResponseTemplates.anthropicMessages())
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));
    }

    /**
     * 入队 SSE 流式响应
     *
     * @param sseBody SSE 格式响应体（含 data: 行）
     */
    public void enqueueStream(String sseBody) {
        server.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setBody(sseBody)
                .setHeader("Content-Type", "text/event-stream")
                .setResponseCode(200));
    }

    /**
     * 入队错误响应
     *
     * @param statusCode HTTP 状态码
     * @param errorBody  错误响应体
     */
    public void enqueueError(int statusCode, String errorBody) {
        server.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setBody(errorBody)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(statusCode));
    }

    /**
     * 入队超时响应（body 延迟远超客户端 timeout）
     */
    public void enqueueTimeout() {
        server.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setBodyDelay(30, TimeUnit.SECONDS)
                .setBody("{}")
                .setResponseCode(200));
    }

    /**
     * 取录制的请求
     */
    public RecordedResponse takeRequest() throws InterruptedException {
        return server.takeRequest();
    }

    /**
     * 创建指向模拟器的 OpenAIUpstreamClient
     *
     * @param apiKey   API Key
     * @param timeout  超时秒数
     */
    public OpenAIUpstreamClient createOpenAIIClient(String apiKey, int timeout) {
        ErrorClassificationStrategy classifier = new OpenAIErrorClassifier();
        return new OpenAIUpstreamClient(sharedClient, getUrl(), apiKey, timeout, sharedMapper, classifier);
    }

    /**
     * 创建指向模拟器的 AnthropicUpstreamClient
     *
     * @param apiKey   API Key
     * @param timeout  超时秒数
     */
    public AnthropicUpstreamClient createAnthropicClient(String apiKey, int timeout) {
        ErrorClassificationStrategy classifier = new AnthropicErrorClassifier();
        return new AnthropicUpstreamClient(sharedClient, getUrl(), apiKey, timeout, sharedMapper, classifier);
    }

    @Override
    public void close() {
        try {
            server.shutdown();
        } catch (IOException e) {
            // 关闭时忽略异常
        }
    }
}
```

- [ ] **Step 2: 验证编译通过**

运行: `./mvnw compile -pl gateway-boot -q`
预期: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/support/ProviderSimulator.java
git commit -m "test(simulator): 添加 ProviderSimulator MockWebServer 封装"
```

---

### Task 3: 创建 OpenAIUpstreamClientTest 测试类

**Files:**
- 创建: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/OpenAIUpstreamClientTest.java`

**前置:** Task 1 和 Task 2 已完成

- [ ] **Step 1: 创建测试类（8 场景）**

```java
package com.codingas.gateway.infrastructure.supply.upstream;

import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResult;
import com.codingas.gateway.support.ProviderSimulator;
import com.codingas.gateway.support.ResponseTemplates;
import okhttp3.mockwebserver.RecordedResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenAIUpstreamClient 单元测试
 *
 * <p>使用 MockWebServer 模拟 OpenAI API，覆盖非流式/流式/错误/超时/连通性场景。</p>
 */
@DisplayName("OpenAIUpstreamClient 测试")
class OpenAIUpstreamClientTest {

    private ProviderSimulator simulator;
    private OpenAIUpstreamClient client;

    @BeforeEach
    void setUp() {
        simulator = ProviderSimulator.create();
        client = simulator.createOpenAIIClient("sk-test-key", 5);
    }

    @AfterEach
    void tearDown() {
        simulator.close();
    }

    private OpenAIChatRequest buildRequest() {
        return OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                        OpenAIChatRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .maxTokens(100)
                .build();
    }

    @Nested
    @DisplayName("非流式调用")
    class NonStreamTests {

        @Test
        @DisplayName("正常调用返回正确响应")
        void chat_success_returnsResponse() {
            simulator.enqueueOpenAISuccess();

            ProtocolResponse response = client.chat(buildRequest());

            assertThat(response).isInstanceOf(OpenAIChatResponse.class);
            OpenAIChatResponse openai = (OpenAIChatResponse) response;
            assertThat(openai.getId()).isEqualTo("chatcmpl-test123");
            assertThat(openai.getModel()).isEqualTo("gpt-4");
            assertThat(openai.getChoices()).hasSize(1);
            assertThat(openai.getChoices().get(0).getMessage().getContent())
                    .isEqualTo("Hello! How can I help you today?");
            assertThat(openai.getChoices().get(0).getFinishReason()).isEqualTo("stop");
            assertThat(openai.getUsage().getPromptTokens()).isEqualTo(10);
            assertThat(openai.getUsage().getCompletionTokens()).isEqualTo(8);
            assertThat(openai.getUsage().getTotalTokens()).isEqualTo(18);
        }

        @Test
        @DisplayName("请求发送到正确路径并携带 Authorization 头")
        void chat_success_sendsCorrectRequest() throws InterruptedException {
            simulator.enqueueOpenAISuccess();

            client.chat(buildRequest());

            RecordedResponse recorded = simulator.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
            assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-test-key");
            assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json");
            assertThat(recorded.getMethod()).isEqualTo("POST");
        }
    }

    @Nested
    @DisplayName("流式调用")
    class StreamTests {

        @Test
        @DisplayName("流式调用收到多个 chunk 并正常完成")
        void chatStream_success_receivesChunks() throws InterruptedException {
            simulator.enqueueStream(ResponseTemplates.openaiStreamChunks());

            CountDownLatch latch = new CountDownLatch(1);
            List<String> chunks = new CopyOnWriteArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);

            StreamCallback callback = new StreamCallback() {
                @Override public void onChunk(String data) { chunks.add(data); }
                @Override public void onComplete() { completed.set(true); latch.countDown(); }
                @Override public void onError(Throwable t) { latch.countDown(); }
            };

            client.chatStream(buildRequest(), callback);

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(completed).isTrue();
            assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("错误分类")
    class ErrorClassificationTests {

        @Test
        @DisplayName("429 限流错误 → RATE_LIMIT_ERROR")
        void chat_429_rateLimitError() {
            simulator.enqueueError(429, ResponseTemplates.openaiError(429));

            assertThatThrownBy(() -> client.chat(buildRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> assertThat(((ProviderException) ex).getErrorType())
                            .isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR));
        }

        @Test
        @DisplayName("401 鉴权失败 → AUTHENTICATION_ERROR")
        void chat_401_authenticationError() {
            simulator.enqueueError(401, ResponseTemplates.openaiError(401));

            assertThatThrownBy(() -> client.chat(buildRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> assertThat(((ProviderException) ex).getErrorType())
                            .isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR));
        }

        @Test
        @DisplayName("500 服务端错误 → UPSTREAM_ERROR")
        void chat_500_upstreamError() {
            simulator.enqueueError(500, ResponseTemplates.openaiError(500));

            assertThatThrownBy(() -> client.chat(buildRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> assertThat(((ProviderException) ex).getErrorType())
                            .isEqualTo(ProviderErrorType.UPSTREAM_ERROR));
        }
    }

    @Nested
    @DisplayName("超时和连通性")
    class TimeoutAndConnectivityTests {

        @Test
        @DisplayName("超时 → TIMEOUT_ERROR")
        void chat_timeout_timeoutError() {
            // 使用极短超时的客户端
            OpenAIUpstreamClient shortTimeoutClient = simulator.createOpenAIIClient("sk-test-key", 1);
            simulator.enqueueTimeout();

            assertThatThrownBy(() -> shortTimeoutClient.chat(buildRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> assertThat(((ProviderException) ex).getErrorType())
                            .isEqualTo(ProviderErrorType.TIMEOUT_ERROR));
        }

        @Test
        @DisplayName("连通性测试成功 → ConnectivityTestResult(success=true)")
        void testConnectivity_success() {
            // MockWebServer 需要为 GET /v1/models 返回 200
            simulator.enqueueOpenAISuccess();

            ConnectivityTestResult result = client.testConnectivity();

            assertThat(result.success()).isTrue();
        }
    }
}
```

- [ ] **Step 2: 运行测试验证通过**

运行: `./mvnw test -pl gateway-boot -Dtest="OpenAIUpstreamClientTest" -pl gateway-boot`
预期: 8 tests passed, 0 failed

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/OpenAIUpstreamClientTest.java
git commit -m "test(simulator): 添加 OpenAIUpstreamClient 8 场景测试"
```

---

### Task 4: 创建 AnthropicUpstreamClientTest 测试类

**Files:**
- 创建: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/AnthropicUpstreamClientTest.java`

**前置:** Task 1 和 Task 2 已完成

- [ ] **Step 1: 创建测试类（8 场景）**

```java
package com.codingas.gateway.infrastructure.supply.upstream;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResult;
import com.codingas.gateway.support.ProviderSimulator;
import com.codingas.gateway.support.ResponseTemplates;
import okhttp3.mockwebserver.RecordedResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AnthropicUpstreamClient 单元测试
 *
 * <p>使用 MockWebServer 模拟 Anthropic API，覆盖非流式/流式/错误/超时/连通性场景。</p>
 */
@DisplayName("AnthropicUpstreamClient 测试")
class AnthropicUpstreamClientTest {

    private ProviderSimulator simulator;
    private AnthropicUpstreamClient client;

    @BeforeEach
    void setUp() {
        simulator = ProviderSimulator.create();
        client = simulator.createAnthropicClient("sk-ant-test-key", 5);
    }

    @AfterEach
    void tearDown() {
        simulator.close();
    }

    private AnthropicMessagesRequest buildRequest() {
        return AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(
                        AnthropicMessagesRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .maxTokens(100)
                .build();
    }

    @Nested
    @DisplayName("非流式调用")
    class NonStreamTests {

        @Test
        @DisplayName("正常调用返回正确响应")
        void chat_success_returnsResponse() {
            simulator.enqueueAnthropicSuccess();

            ProtocolResponse response = client.chat(buildRequest());

            assertThat(response).isInstanceOf(AnthropicMessagesResponse.class);
            AnthropicMessagesResponse anthropic = (AnthropicMessagesResponse) response;
            assertThat(anthropic.getId()).isEqualTo("msg_test123");
            assertThat(anthropic.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
            assertThat(anthropic.getContent()).hasSize(1);
            assertThat(anthropic.getContent().get(0).getText())
                    .isEqualTo("Hello! How can I help you today?");
            assertThat(anthropic.getStopReason()).isEqualTo("end_turn");
            assertThat(anthropic.getUsage().getInputTokens()).isEqualTo(10);
            assertThat(anthropic.getUsage().getOutputTokens()).isEqualTo(8);
        }

        @Test
        @DisplayName("请求发送到正确路径并携带 x-api-key 和 anthropic-version 头")
        void chat_success_sendsCorrectRequest() throws InterruptedException {
            simulator.enqueueAnthropicSuccess();

            client.chat(buildRequest());

            RecordedResponse recorded = simulator.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/v1/messages");
            assertThat(recorded.getHeader("x-api-key")).isEqualTo("sk-ant-test-key");
            assertThat(recorded.getHeader("anthropic-version")).isEqualTo("2023-06-01");
            assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json");
            assertThat(recorded.getMethod()).isEqualTo("POST");
        }
    }

    @Nested
    @DisplayName("流式调用")
    class StreamTests {

        @Test
        @DisplayName("流式调用收到多个 chunk 并以 message_stop 正常完成")
        void chatStream_success_receivesChunks() throws InterruptedException {
            simulator.enqueueStream(ResponseTemplates.anthropicStreamChunks());

            CountDownLatch latch = new CountDownLatch(1);
            List<String> chunks = new CopyOnWriteArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);

            StreamCallback callback = new StreamCallback() {
                @Override public void onChunk(String data) { chunks.add(data); }
                @Override public void onComplete() { completed.set(true); latch.countDown(); }
                @Override public void onError(Throwable t) { latch.countDown(); }
            };

            client.chatStream(buildRequest(), callback);

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(completed).isTrue();
            assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("错误分类")
    class ErrorClassificationTests {

        @Test
        @DisplayName("429 限流错误 → RATE_LIMIT_ERROR")
        void chat_429_rateLimitError() {
            simulator.enqueueError(429, ResponseTemplates.anthropicError(429));

            assertThatThrownBy(() -> client.chat(buildRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> assertThat(((ProviderException) ex).getErrorType())
                            .isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR));
        }

        @Test
        @DisplayName("401 鉴权失败 → AUTHENTICATION_ERROR")
        void chat_401_authenticationError() {
            simulator.enqueueError(401, ResponseTemplates.anthropicError(401));

            assertThatThrownBy(() -> client.chat(buildRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> assertThat(((ProviderException) ex).getErrorType())
                            .isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR));
        }

        @Test
        @DisplayName("500 服务端错误 → UPSTREAM_ERROR")
        void chat_500_upstreamError() {
            simulator.enqueueError(500, ResponseTemplates.anthropicError(500));

            assertThatThrownBy(() -> client.chat(buildRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> assertThat(((ProviderException) ex).getErrorType())
                            .isEqualTo(ProviderErrorType.UPSTREAM_ERROR));
        }
    }

    @Nested
    @DisplayName("超时和连通性")
    class TimeoutAndConnectivityTests {

        @Test
        @DisplayName("超时 → TIMEOUT_ERROR")
        void chat_timeout_timeoutError() {
            AnthropicUpstreamClient shortTimeoutClient = simulator.createAnthropicClient("sk-ant-test-key", 1);
            simulator.enqueueTimeout();

            assertThatThrownBy(() -> shortTimeoutClient.chat(buildRequest()))
                    .isInstanceOf(ProviderException.class)
                    .satisfies(ex -> assertThat(((ProviderException) ex).getErrorType())
                            .isEqualTo(ProviderErrorType.TIMEOUT_ERROR));
        }

        @Test
        @DisplayName("连通性测试 — 非 5xx 响应视为成功")
        void testConnectivity_non5xx_success() {
            // Anthropic 连通性测试发送 POST /v1/messages，非 5xx 即成功
            simulator.enqueueError(400, ResponseTemplates.anthropicError(400));

            ConnectivityTestResult result = client.testConnectivity();

            assertThat(result.success()).isTrue();
        }
    }
}
```

- [ ] **Step 2: 运行测试验证通过**

运行: `./mvnw test -pl gateway-boot -Dtest="AnthropicUpstreamClientTest" -pl gateway-boot`
预期: 8 tests passed, 0 failed

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/AnthropicUpstreamClientTest.java
git commit -m "test(simulator): 添加 AnthropicUpstreamClient 8 场景测试"
```

---

### Task 5: 第一阶段全量测试验证

**前置:** Task 1 ~ Task 4 已完成

- [ ] **Step 1: 运行第一阶段全部测试**

运行: `./mvnw test -pl gateway-boot -Dtest="OpenAIUpstreamClientTest,AnthropicUpstreamClientTest"`
预期: 16 tests passed, 0 failed

- [ ] **Step 2: 运行 gateway-boot 全量单元测试确认无回归**

运行: `./mvnw test -pl gateway-boot`
预期: BUILD SUCCESS，所有既有测试通过

- [ ] **Step 3: 提交（如有修复）**

仅在需要修复时提交。

---

## 第二阶段：独立模拟服务

### Task 6: 创建 gateway-simulator Maven 模块骨架

**Files:**
- 创建: `gateway-simulator/pom.xml`
- 修改: `pom.xml`（父 POM 添加模块）

- [ ] **Step 1: 创建 gateway-simulator/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.codingas.gateway</groupId>
        <artifactId>gateway-project</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>gateway-simulator</artifactId>
    <packaging>jar</packaging>

    <name>Gateway Simulator</name>
    <description>LLM Provider Simulator - 独立模拟服务，无需真实 API Key 即可验证 Gateway 集成</description>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 修改父 POM 添加模块**

在 `pom.xml` 的 `<modules>` 中添加 `<module>gateway-simulator</module>`：

```xml
    <modules>
        <module>gateway-boot</module>
        <module>gateway-cli</module>
        <module>gateway-simulator</module>
    </modules>
```

- [ ] **Step 3: 验证模块解析**

运行: `./mvnw help:effective-pom -pl gateway-simulator -q > /dev/null`
预期: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add gateway-simulator/pom.xml pom.xml
git commit -m "feat(simulator): 创建 gateway-simulator Maven 模块骨架"
```

---

### Task 7: 实现响应模板和模式服务

**Files:**
- 创建: `gateway-simulator/src/main/java/com/codingas/simulator/template/SimulatorResponseTemplates.java`
- 创建: `gateway-simulator/src/main/java/com/codingas/simulator/service/RequestRecord.java`
- 创建: `gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java`

- [ ] **Step 1: 创建 SimulatorResponseTemplates**

```java
package com.codingas.simulator.template;

/**
 * 模拟服务响应模板
 *
 * <p>提供 OpenAI 和 Anthropic 协议的标准 JSON 响应模板。
 * 复制自 gateway-boot 测试工具包的 ResponseTemplates 逻辑。</p>
 */
public final class SimulatorResponseTemplates {

    private SimulatorResponseTemplates() {}

    /**
     * OpenAI Chat Completion 成功响应
     */
    public static String openaiChatCompletion() {
        return """
                {
                  "id": "chatcmpl-sim001",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "This is a simulated response from LLM Provider Simulator."
                    },
                    "finish_reason": "stop"
                  }],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 12,
                    "total_tokens": 22
                  }
                }
                """;
    }

    /**
     * OpenAI SSE 流式 chunk（单个 delta）
     */
    public static String openaiStreamChunk(String content) {
        return "data: {\"id\":\"chatcmpl-sim-stream\",\"object\":\"chat.completion.chunk\","
             + "\"created\":1700000000,\"model\":\"gpt-4\","
             + "\"choices\":[{\"index\":0,\"delta\":{\"content\":\"" + content + "\"},\"finish_reason\":null}]}\n\n";
    }

    /**
     * OpenAI SSE 流终止标记
     */
    public static String openaiStreamDone() {
        return "data: [DONE]\n\n";
    }

    /**
     * OpenAI 限流错误响应
     */
    public static String openaiRateLimitError() {
        return """
                {
                  "error": {
                    "type": "rate_limit_error",
                    "message": "Simulator: rate limit exceeded"
                  }
                }
                """;
    }

    /**
     * OpenAI 服务器错误响应
     */
    public static String openaiServerError() {
        return """
                {
                  "error": {
                    "type": "server_error",
                    "message": "Simulator: internal server error"
                  }
                }
                """;
    }

    /**
     * Anthropic Messages 成功响应
     */
    public static String anthropicMessages() {
        return """
                {
                  "id": "msg_sim001",
                  "type": "message",
                  "role": "assistant",
                  "model": "claude-3-5-sonnet-20241022",
                  "content": [{
                    "type": "text",
                    "text": "This is a simulated response from LLM Provider Simulator."
                  }],
                  "stop_reason": "end_turn",
                  "usage": {
                    "input_tokens": 10,
                    "output_tokens": 12
                  }
                }
                """;
    }

    /**
     * Anthropic SSE 流式 content_block_delta chunk
     */
    public static String anthropicStreamDelta(String text) {
        return "event: content_block_delta\n"
             + "data: {\"type\":\"content_block_delta\",\"index\":0,"
             + "\"delta\":{\"type\":\"text_delta\",\"text\":\"" + text + "\"}}\n\n";
    }

    /**
     * Anthropic SSE 流终止标记
     */
    public static String anthropicStreamStop() {
        return "event: message_stop\n"
             + "data: {\"type\":\"message_stop\"}\n\n";
    }

    /**
     * Anthropic 限流错误响应
     */
    public static String anthropicRateLimitError() {
        return """
                {
                  "type": "error",
                  "error": {
                    "type": "rate_limit_error",
                    "message": "Simulator: rate limit exceeded"
                  }
                }
                """;
    }

    /**
     * Anthropic 服务器错误响应
     */
    public static String anthropicServerError() {
        return """
                {
                  "type": "error",
                  "error": {
                    "type": "api_error",
                    "message": "Simulator: internal server error"
                  }
                }
                """;
    }
}
```

- [ ] **Step 2: 创建 RequestRecord**

```java
package com.codingas.simulator.service;

import java.time.Instant;

/**
 * 请求记录
 *
 * @param method    HTTP 方法
 * @param path      请求路径
 * @param timestamp 请求时间
 */
public record RequestRecord(String method, String path, Instant timestamp) {
}
```

- [ ] **Step 3: 创建 SimulatorModeService**

```java
package com.codingas.simulator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模拟器模式管理服务
 *
 * <p>管理模拟器运行模式（正常/限流/故障）和请求记录。</p>
 */
@Service
public class SimulatorModeService {

    /**
     * 模拟器运行模式
     */
    public enum SimulatorMode {
        /** 正常模式 — 返回成功响应 */
        NORMAL,
        /** 限流模式 — 返回 429 错误 */
        RATE_LIMITED,
        /** 故障模式 — 返回 500 错误 */
        FAULT
    }

    private volatile SimulatorMode mode;
    private final int logCapacity;
    private final List<RequestRecord> requestLog;

    /**
     * 构造模式管理服务
     *
     * @param mode          初始模式
     * @param logCapacity   请求记录容量
     */
    public SimulatorModeService(
            @Value("${simulator.mode:normal}") String mode,
            @Value("${simulator.request-log-capacity:100}") int logCapacity) {
        this.mode = parseMode(mode);
        this.logCapacity = logCapacity;
        this.requestLog = Collections.synchronizedList(new ArrayList<>(logCapacity));
    }

    /**
     * 获取当前模式
     */
    public SimulatorMode getMode() {
        return mode;
    }

    /**
     * 设置模式
     *
     * @param mode 目标模式
     */
    public void setMode(SimulatorMode mode) {
        this.mode = mode;
    }

    /**
     * 记录请求
     *
     * @param method HTTP 方法
     * @param path   请求路径
     */
    public void recordRequest(String method, String path) {
        synchronized (requestLog) {
            if (requestLog.size() >= logCapacity) {
                requestLog.removeFirst();
            }
            requestLog.add(new RequestRecord(method, path, Instant.now()));
        }
    }

    /**
     * 获取请求记录（不可变副本）
     */
    public List<RequestRecord> getRequestLog() {
        synchronized (requestLog) {
            return List.copyOf(requestLog);
        }
    }

    private SimulatorMode parseMode(String modeStr) {
        return switch (modeStr.toLowerCase()) {
            case "rate_limited" -> SimulatorMode.RATE_LIMITED;
            case "fault" -> SimulatorMode.FAULT;
            default -> SimulatorMode.NORMAL;
        };
    }
}
```

- [ ] **Step 4: 验证编译通过**

运行: `./mvnw compile -pl gateway-simulator -q`
预期: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/template/SimulatorResponseTemplates.java \
       gateway-simulator/src/main/java/com/codingas/simulator/service/RequestRecord.java \
       gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java
git commit -m "feat(simulator): 添加响应模板和模式管理服务"
```

---

### Task 8: 实现模拟端点 Controller

**Files:**
- 创建: `gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorController.java`

**前置:** Task 7 已完成

- [ ] **Step 1: 创建 SimulatorController**

```java
package com.codingas.simulator.controller;

import com.codingas.simulator.service.SimulatorModeService;
import com.codingas.simulator.template.SimulatorResponseTemplates;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 模拟端点 Controller
 *
 * <p>提供 OpenAI 和 Anthropic 兼容的模拟 API 端点。</p>
 */
@RestController
public class SimulatorController {

    private final SimulatorModeService modeService;

    public SimulatorController(SimulatorModeService modeService) {
        this.modeService = modeService;
    }

    /**
     * OpenAI Chat Completions 端点
     */
    @PostMapping("/v1/chat/completions")
    public ResponseEntity<?> openaiChatCompletions(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        modeService.recordRequest("POST", "/v1/chat/completions");

        boolean stream = body.containsKey("stream") && Boolean.TRUE.equals(body.get("stream"));

        return switch (modeService.getMode()) {
            case NORMAL -> stream
                    ? ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                            .body(createOpenAIStreamEmitter())
                    : ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(SimulatorResponseTemplates.openaiChatCompletion());
            case RATE_LIMITED -> ResponseEntity.status(429)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiRateLimitError());
            case FAULT -> ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.openaiServerError());
        };
    }

    /**
     * Anthropic Messages 端点
     */
    @PostMapping("/v1/messages")
    public ResponseEntity<?> anthropicMessages(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        modeService.recordRequest("POST", "/v1/messages");

        boolean stream = body.containsKey("stream") && Boolean.TRUE.equals(body.get("stream"));

        return switch (modeService.getMode()) {
            case NORMAL -> stream
                    ? ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                            .body(createAnthropicStreamEmitter())
                    : ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(SimulatorResponseTemplates.anthropicMessages());
            case RATE_LIMITED -> ResponseEntity.status(429)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicRateLimitError());
            case FAULT -> ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(SimulatorResponseTemplates.anthropicServerError());
        };
    }

    /**
     * 创建 OpenAI 流式 SSE 发射器
     */
    private SseEmitter createOpenAIStreamEmitter() {
        SseEmitter emitter = new SseEmitter(30_000L);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String[] chunks = {"Hello", " from", " simulator!"};
                for (String chunk : chunks) {
                    emitter.send(SseEmitter.event()
                            .data(SimulatorResponseTemplates.openaiStreamChunk(chunk).replace("data: ", "")));
                    Thread.sleep(50);
                }
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * 创建 Anthropic 流式 SSE 发射器
     */
    private SseEmitter createAnthropicStreamEmitter() {
        SseEmitter emitter = new SseEmitter(30_000L);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String[] chunks = {"Hello", " from", " simulator!"};
                for (String chunk : chunks) {
                    String deltaJson = SimulatorResponseTemplates.anthropicStreamDelta(chunk);
                    // 解析 event 行和 data 行
                    String[] lines = deltaJson.split("\n");
                    for (String line : lines) {
                        if (line.startsWith("event: ")) {
                            // SseEmitter.event().name() 设置事件类型
                        } else if (line.startsWith("data: ")) {
                            emitter.send(SseEmitter.event()
                                    .name("content_block_delta")
                                    .data(line.substring(6)));
                        }
                    }
                    Thread.sleep(50);
                }
                emitter.send(SseEmitter.event().name("message_stop").data("{\"type\":\"message_stop\"}"));
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
```

- [ ] **Step 2: 验证编译通过**

运行: `./mvnw compile -pl gateway-simulator -q`
预期: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorController.java
git commit -m "feat(simulator): 实现模拟端点 Controller"
```

---

### Task 9: 实现管理 API Controller

**Files:**
- 创建: `gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorAdminController.java`

**前置:** Task 7 已完成

- [ ] **Step 1: 创建 SimulatorAdminController**

```java
package com.codingas.simulator.controller;

import com.codingas.simulator.service.RequestRecord;
import com.codingas.simulator.service.SimulatorModeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 模拟器管理 API Controller
 *
 * <p>提供模式切换和请求记录查询接口。</p>
 */
@RestController
@RequestMapping("/simulator")
public class SimulatorAdminController {

    private final SimulatorModeService modeService;

    public SimulatorAdminController(SimulatorModeService modeService) {
        this.modeService = modeService;
    }

    /**
     * 切换模拟器模式
     *
     * @param body 包含 mode 字段的请求体，值为 "normal"、"rate_limited" 或 "fault"
     */
    @PostMapping("/mode")
    public ResponseEntity<Map<String, String>> setMode(@RequestBody Map<String, String> body) {
        String modeStr = body.get("mode");
        if (modeStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "缺少 mode 参数"));
        }

        try {
            SimulatorModeService.SimulatorMode mode = switch (modeStr.toLowerCase()) {
                case "normal" -> SimulatorModeService.SimulatorMode.NORMAL;
                case "rate_limited" -> SimulatorModeService.SimulatorMode.RATE_LIMITED;
                case "fault" -> SimulatorModeService.SimulatorMode.FAULT;
                default -> throw new IllegalArgumentException("不支持的模式: " + modeStr);
            };
            modeService.setMode(mode);
            return ResponseEntity.ok(Map.of("mode", mode.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取当前模式
     */
    @GetMapping("/mode")
    public ResponseEntity<Map<String, String>> getMode() {
        return ResponseEntity.ok(Map.of("mode", modeService.getMode().name()));
    }

    /**
     * 查询请求记录
     */
    @GetMapping("/requests")
    public ResponseEntity<List<RequestRecord>> getRequestLog() {
        return ResponseEntity.ok(modeService.getRequestLog());
    }
}
```

- [ ] **Step 2: 验证编译通过**

运行: `./mvnw compile -pl gateway-simulator -q`
预期: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorAdminController.java
git commit -m "feat(simulator): 实现管理 API Controller"
```

---

### Task 10: 创建 Spring Boot 启动类和配置

**Files:**
- 创建: `gateway-simulator/src/main/java/com/codingas/simulator/LLMProviderSimulatorApplication.java`
- 创建: `gateway-simulator/src/main/resources/application.yml`

**前置:** Task 7 ~ Task 9 已完成

- [ ] **Step 1: 创建启动类**

```java
package com.codingas.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LLM Provider 模拟器启动类
 *
 * <p>独立运行的模拟服务，提供 OpenAI 和 Anthropic 兼容的模拟 API 端点，
 * 无需真实 API Key 即可验证 Gateway 集成。</p>
 */
@SpringBootApplication
public class LLMProviderSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LLMProviderSimulatorApplication.class, args);
    }
}
```

- [ ] **Step 2: 创建配置文件**

```yaml
simulator:
  mode: normal
  request-log-capacity: 100

server:
  port: 9090
```

- [ ] **Step 3: 验证模块编译和启动**

运行: `./mvnw compile -pl gateway-simulator -q`
预期: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/LLMProviderSimulatorApplication.java \
       gateway-simulator/src/main/resources/application.yml
git commit -m "feat(simulator): 添加 Spring Boot 启动类和配置文件"
```

---

### Task 11: 编写 SimulatorModeService 单元测试

**Files:**
- 创建: `gateway-simulator/src/test/java/com/codingas/simulator/SimulatorModeServiceTest.java`

**前置:** Task 7 已完成

- [ ] **Step 1: 创建 SimulatorModeServiceTest**

```java
package com.codingas.simulator;

import com.codingas.simulator.service.SimulatorModeService;
import com.codingas.simulator.service.SimulatorModeService.SimulatorMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimulatorModeService 单元测试
 */
@DisplayName("SimulatorModeService 测试")
class SimulatorModeServiceTest {

    private SimulatorModeService service;

    @BeforeEach
    void setUp() {
        service = new SimulatorModeService("normal", 5);
    }

    @Nested
    @DisplayName("模式管理")
    class ModeTests {

        @Test
        @DisplayName("初始模式为 NORMAL")
        void initialMode_isNormal() {
            assertThat(service.getMode()).isEqualTo(SimulatorMode.NORMAL);
        }

        @Test
        @DisplayName("切换到 RATE_LIMITED 模式")
        void setMode_rateLimited() {
            service.setMode(SimulatorMode.RATE_LIMITED);
            assertThat(service.getMode()).isEqualTo(SimulatorMode.RATE_LIMITED);
        }

        @Test
        @DisplayName("切换到 FAULT 模式")
        void setMode_fault() {
            service.setMode(SimulatorMode.FAULT);
            assertThat(service.getMode()).isEqualTo(SimulatorMode.FAULT);
        }

        @Test
        @DisplayName("解析 rate_limited 字符串")
        void parseMode_rateLimited() {
            SimulatorModeService svc = new SimulatorModeService("rate_limited", 10);
            assertThat(svc.getMode()).isEqualTo(SimulatorMode.RATE_LIMITED);
        }

        @Test
        @DisplayName("解析 fault 字符串")
        void parseMode_fault() {
            SimulatorModeService svc = new SimulatorModeService("fault", 10);
            assertThat(svc.getMode()).isEqualTo(SimulatorMode.FAULT);
        }

        @Test
        @DisplayName("未知字符串默认为 NORMAL")
        void parseMode_unknown_defaultsToNormal() {
            SimulatorModeService svc = new SimulatorModeService("unknown", 10);
            assertThat(svc.getMode()).isEqualTo(SimulatorMode.NORMAL);
        }
    }

    @Nested
    @DisplayName("请求记录")
    class RequestLogTests {

        @Test
        @DisplayName("记录请求后可查询")
        void recordRequest_canQuery() {
            service.recordRequest("POST", "/v1/chat/completions");

            assertThat(service.getRequestLog()).hasSize(1);
            assertThat(service.getRequestLog().getFirst().method()).isEqualTo("POST");
            assertThat(service.getRequestLog().getFirst().path()).isEqualTo("/v1/chat/completions");
        }

        @Test
        @DisplayName("超过容量时淘汰最旧记录")
        void recordRequest_evictsOldest() {
            for (int i = 0; i < 6; i++) {
                service.recordRequest("POST", "/v1/path" + i);
            }

            List<?> log = service.getRequestLog();
            assertThat(log).hasSize(5);
            // 最旧的 path0 被淘汰
            assertThat(service.getRequestLog().getFirst().path()).isEqualTo("/v1/path1");
        }

        @Test
        @DisplayName("返回不可变副本")
        void getRequestLog_returnsImmutableCopy() {
            service.recordRequest("GET", "/test");

            List<?> log = service.getRequestLog();
            assertThatThrownBy(() -> log.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
```

注意：`getRequestLog_returnsImmutableCopy` 测试需要补充 import：

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

- [ ] **Step 2: 运行测试验证**

运行: `./mvnw test -pl gateway-simulator -Dtest="SimulatorModeServiceTest"`
预期: 8 tests passed, 0 failed

- [ ] **Step 3: 提交**

```bash
git add gateway-simulator/src/test/java/com/codingas/simulator/SimulatorModeServiceTest.java
git commit -m "test(simulator): 添加 SimulatorModeService 单元测试"
```

---

### Task 12: 编写 SimulatorIntegrationTest 集成测试

**Files:**
- 创建: `gateway-simulator/src/test/java/com/codingas/simulator/SimulatorIntegrationTest.java`

**前置:** Task 10 已完成

- [ ] **Step 1: 创建 SimulatorIntegrationTest**

```java
package com.codingas.simulator;

import com.codingas.simulator.service.SimulatorModeService.SimulatorMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟服务集成测试
 *
 * <p>启动完整的 Spring Boot 上下文，验证端到端行为。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("模拟服务集成测试")
class SimulatorIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        // 每个测试前重置为 NORMAL 模式
        restTemplate.postForObject(baseUrl + "/simulator/mode",
                Map.of("mode", "normal"), Map.class);
    }

    @Nested
    @DisplayName("OpenAI 端点")
    class OpenAIEndpointTests {

        @Test
        @DisplayName("非流式 — NORMAL 模式返回 200 + 成功响应")
        void openai_normal_success() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("model", "gpt-4", "messages", java.util.List.of(
                            Map.of("role", "user", "content", "hi")), "stream", false),
                    headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v1/chat/completions", HttpMethod.POST, entity, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsKey("id");
            assertThat(response.getBody()).containsKey("choices");
        }

        @Test
        @DisplayName("限流模式返回 429")
        void openai_rateLimited_429() {
            restTemplate.postForObject(baseUrl + "/simulator/mode",
                    Map.of("mode", "rate_limited"), Map.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("model", "gpt-4", "messages", java.util.List.of(
                            Map.of("role", "user", "content", "hi"))),
                    headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v1/chat/completions", HttpMethod.POST, entity, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("故障模式返回 500")
        void openai_fault_500() {
            restTemplate.postForObject(baseUrl + "/simulator/mode",
                    Map.of("mode", "fault"), Map.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("model", "gpt-4", "messages", java.util.List.of(
                            Map.of("role", "user", "content", "hi"))),
                    headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v1/chat/completions", HttpMethod.POST, entity, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("Anthropic 端点")
    class AnthropicEndpointTests {

        @Test
        @DisplayName("非流式 — NORMAL 模式返回 200 + 成功响应")
        void anthropic_normal_success() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("model", "claude-3-5-sonnet-20241022",
                            "messages", java.util.List.of(
                                    Map.of("role", "user", "content", "hi")),
                            "max_tokens", 100),
                    headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v1/messages", HttpMethod.POST, entity, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsKey("id");
            assertThat(response.getBody()).containsKey("content");
        }
    }

    @Nested
    @DisplayName("管理 API")
    class AdminApiTests {

        @Test
        @DisplayName("切换模式后查询模式一致")
        void setMode_thenGetMode() {
            restTemplate.postForObject(baseUrl + "/simulator/mode",
                    Map.of("mode", "rate_limited"), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, String> result = restTemplate.getForObject(
                    baseUrl + "/simulator/mode", Map.class);

            assertThat(result.get("mode")).isEqualTo("RATE_LIMITED");
        }

        @Test
        @DisplayName("请求记录包含已发送的请求")
        void requestLog_containsSentRequests() {
            // 先发一个模拟请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("model", "gpt-4", "messages", java.util.List.of(
                            Map.of("role", "user", "content", "hi"))),
                    headers);
            restTemplate.exchange(baseUrl + "/v1/chat/completions",
                    HttpMethod.POST, entity, Map.class);

            // 查询请求记录
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> log = restTemplate.getForObject(
                    baseUrl + "/simulator/requests", java.util.List.class);

            assertThat(log).isNotEmpty();
        }

        @Test
        @DisplayName("无效模式返回 400")
        void setMode_invalid_returns400() {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/simulator/mode",
                    Map.of("mode", "invalid_mode"), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
```

- [ ] **Step 2: 运行集成测试**

运行: `./mvnw test -pl gateway-simulator -Dtest="SimulatorIntegrationTest"`
预期: 7 tests passed, 0 failed

注意：集成测试类名以 `IntegrationTest` 结尾，会被 Surefire 排除、Failsafe 包含。如果 Surefire 排除了它，使用 Failsafe 运行：

运行: `./mvnw verify -pl gateway-simulator -Dit.test="SimulatorIntegrationTest"`

如果 TestRestTemplate 在 Surefire 下也能运行（取决于项目 surefire 配置），则直接用 test 命令即可。

- [ ] **Step 3: 提交**

```bash
git add gateway-simulator/src/test/java/com/codingas/simulator/SimulatorIntegrationTest.java
git commit -m "test(simulator): 添加模拟服务集成测试"
```

---

### Task 13: 全量回归测试

**前置:** 所有 Task 已完成

- [ ] **Step 1: 运行全模块测试**

运行: `./mvnw clean test`
预期: BUILD SUCCESS，所有模块测试通过

- [ ] **Step 2: 最终提交（如有修复）**

仅在需要修复时提交。

---

## 自审检查清单

### 1. Spec 覆盖检查

| 设计文档要求 | 对应 Task |
|-------------|----------|
| ResponseTemplates 6 个模板方法 | Task 1 |
| ProviderSimulator AutoCloseable 封装 | Task 2 |
| OpenAIUpstreamClientTest 8 场景 | Task 3 |
| AnthropicUpstreamClientTest 8 场景 | Task 4 |
| gateway-simulator Maven 模块 | Task 6 |
| SimulatorResponseTemplates | Task 7 |
| SimulatorModeService + RequestRecord | Task 7 |
| SimulatorController（两个端点 + 流式） | Task 8 |
| SimulatorAdminController（模式切换 + 请求记录） | Task 9 |
| Spring Boot 启动类 + application.yml | Task 10 |
| SimulatorModeService 单元测试 | Task 11 |
| SimulatorIntegrationTest 集成测试 | Task 12 |
| 全量回归 | Task 13 |

### 2. 占位符扫描

无 TBD、TODO、"implement later"、"fill in details"、"add appropriate error handling"、"add validation"、"handle edge cases"、"Write tests for the above"、"Similar to Task N" 等占位符。

### 3. 类型一致性检查

- `ProviderSimulator.createOpenAIIClient()` 返回 `OpenAIUpstreamClient`，与 Task 3 测试中 `client.chat()` 返回 `ProtocolResponse` 一致
- `ProviderSimulator.createAnthropicClient()` 返回 `AnthropicUpstreamClient`，与 Task 4 一致
- `ResponseTemplates` 方法签名与 `ProviderSimulator.enqueueOpenAISuccess()` 等调用一致
- `SimulatorMode` 枚举在 `SimulatorModeService` 和 `SimulatorAdminController` 中引用一致
- `RequestRecord` record 在 `SimulatorModeService` 和 `SimulatorAdminController` 中引用一致
