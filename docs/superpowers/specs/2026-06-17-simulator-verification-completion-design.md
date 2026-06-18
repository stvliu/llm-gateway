---
comet_change: simulator-verification-completion
role: technical-design
canonical_spec: openspec
archived-with: 2026-06-18-simulator-verification-completion
status: final
---

# Simulator 验证补全 — 技术设计文档

## 1. 概述

本文档描述了如何补齐 Gateway 全链路集成验证，将覆盖率从约 40% 提升到约 100%。

### 1.1 背景

上一 change 完成了 Simulator 自身增强，但 Gateway 全链路集成测试仅覆盖了正常路径和基础错误场景。Key 故障转移、模型降级、跨协议转换、SSE 稳定性等核心韧性场景未在集成测试中验证。

### 1.2 目标

1. Phase 1：补齐 Simulator 管理 API 的 E2E 测试（behavior/delay/stream/apikey-override）
2. Phase 2：创建 Gateway 全链路集成测试，Mock 认证+路由，验证完整七阶段调度链

## 2. Phase 1：Simulator 管理 API E2E 测试

在现有 `SimulatorEndToEndTest` 中新增 6 个测试：

| 测试方法 | 验证内容 |
|---------|---------|
| testBehaviorSequence_consumesStepsViaHttp | 设置序列 → 3 次请求按序返回 500/401/200 → 第 4 次恢复全局模式 |
| testBehaviorSequence_loop_resetsOnEnd | 设置循环序列 [200,500] → 6 次请求交替 200/500 |
| testDelayConfig_appliesDelay | 设置 100ms 延迟 → 测量响应时间 ≥ 100ms |
| testStreamConfig_interruptAfter | 设置中断 → stream=true 请求收到中断 |
| testApiKeyOverride_matchesByPrefix | 设置 Key 前缀覆盖 → 匹配的 Key 返回 401 |
| testApiKeyOverride_noMatch_fallsbackToGlobal | 不匹配的 Key 回退到全局 NORMAL 模式 |

## 3. Phase 2：Gateway 全链路集成测试

### 3.1 测试基类

```java
@SpringBootTest(classes = GatewayApplication.class, webEnvironment = NONE)
@ActiveProfiles("test")
class FullContextIntegrationTestBase implements AutoCloseable {

    // Mock Beans
    @MockBean AuthenticationDomainService authService;
    @MockBean CredentialResolver credentialResolver;
    @MockBean RoutingResolver routingResolver;
    @MockBean DegradationService degradationService;

    // 真实 Beans
    @Autowired ChatDispatchService chatDispatchService;
    @Autowired DegradationInvoker degradationInvoker;
    @Autowired KeyFailoverInvoker keyFailoverInvoker;
    @Autowired UpstreamClientRegistry clientRegistry;
    @Autowired ResilientClientFactory resilientClientFactory;

    ProviderSimulator simulator;
    RoutingContext routingContext;

    @BeforeEach
    void setUp() throws IOException {
        simulator = ProviderSimulator.create();
        routingContext = createRoutingContext(simulator.getUrl());

        when(authService.authenticateUser(any()))
            .thenReturn(Identity.of(1L, "user", 100L));
        when(routingResolver.resolve(any(), any(), any(), any(), any()))
            .thenReturn(routingContext);
        when(credentialResolver.resolveAll(any()))
            .thenReturn(List.of(createCredential(1L, "sk-key1")));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (simulator != null) simulator.close();
    }

    private ChannelCredential createCredential(Long id, String apiKey) {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(id);
        cred.setApiKeyPlain(apiKey);
        cred.setChannelId(1L);
        cred.setPriority(1);
        return cred;
    }

    private RoutingContext createRoutingContext(String endpointUrl) {
        return new RoutingContext(1L, 1L, endpointUrl, Protocol.OPENAI, 30, null, null);
    }

    protected ProtocolRequest createRequest(String model) {
        OpenAIChatRequest request = OpenAIChatRequest.builder()
            .model(model)
            .messages(List.of(OpenAIChatRequest.Message.builder()
                .role("user").content("hi").build()))
            .build();
        request.setStream(false);
        return request;
    }
}
```

### 3.2 测试场景设计

#### Key 故障转移

```java
// Key1(401) → Key2(200): 自动故障转移
void testKeyFailover_key1Fails_key2Succeeds() {
    when(credentialResolver.resolveAll(any()))
        .thenReturn(List.of(key1("sk-key1"), key2("sk-key2")));
    simulator.enqueueError(401, ...);  // Key1
    simulator.enqueueOpenAISuccess();   // Key2

    ProtocolResponse response = chatDispatchService.dispatch(
        createRequest("gpt-4"), identity, WEIGHTED);

    assertThat(response).isNotNull();
}

// 全部 Key 失败 → ProviderException
void testKeyFailover_allKeysFail() {
    when(credentialResolver.resolveAll(any()))
        .thenReturn(List.of(key1("sk-key1"), key2("sk-key2")));
    simulator.enqueueError(401, ...);  // Key1
    simulator.enqueueError(401, ...);  // Key2

    assertThatThrownBy(() -> chatDispatchService.dispatch(...))
        .isInstanceOf(ProviderException.class)
        .satisfies(ex -> assertThat(ex.getMessage()).contains("所有 Key 均失败"));
}
```

#### 模型降级

```java
// Mock DegradationService: gpt-4 → gpt-3.5-turbo
void testDegradation_primaryFails_fallbackSucceeds() {
    when(degradationService.degrade(eq("gpt-4"), any()))
        .thenReturn("gpt-3.5-turbo");
    simulator.enqueueError(500, ...);  // gpt-4 失败

    ProtocolResponse response = degradationInvoker.invoke(
        ctx, request, OPENAI, 1L, "user", WEIGHTED);

    assertThat(response).isNotNull();
}

// 降级链耗尽 → 原异常抛出
void testDegradation_chainExhausted() {
    when(degradationService.degrade(any(), any())).thenReturn(null);
    simulator.enqueueError(500, ...);

    assertThatThrownBy(() -> degradationInvoker.invoke(...))
        .isInstanceOf(ProviderException.class);
}
```

#### 跨协议转换

```java
// RoutingContext: upstreamProtocol=ANTHROPIC
void testProtocolConversion_openaiToAnthropic() {
    RoutingContext crossCtx = new RoutingContext(
        1L, 1L, simulator.getUrl(), Protocol.ANTHROPIC, 30, null, null);
    when(routingResolver.resolve(any(), any(), any(), any(), any()))
        .thenReturn(crossCtx);

    simulator.enqueueAnthropicSuccess();

    ProtocolResponse response = chatDispatchService.dispatch(
        createRequest("gpt-4"), identity, WEIGHTED);

    // 返回的是 OpenAIChatResponse（跨协议转换后）
    assertThat(response).isInstanceOf(OpenAIChatResponse.class);
}
```

#### 熔断器

```java
void testCircuitBreaker_opensAfterFailures() {
    // 10 次连续 500 触发熔断
    for (int i = 0; i < 10; i++) {
        simulator.enqueueError(500, ...);
    }

    assertThatThrownBy(() -> chatDispatchService.dispatch(...))
        .isInstanceOf(ProviderException.class);
}
```

### 3.3 集成测试配置

`gateway-boot/src/test/resources/application-integration-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false

gateway:
  circuit-breaker:
    open-duration-ms: 100
    sliding-window-size: 5
    failure-rate-threshold: 0.5
  http-client:
    connect-timeout: 2s
    read-timeout: 2s
  degradation:
    enabled: true
    chains:
      - primary: gpt-4
        fallbacks:
          - gpt-3.5-turbo

logging:
  level:
    com.codingas.gateway: DEBUG
```

## 4. 文件变更清单

| 文件 | 变更 | 说明 |
|------|------|------|
| `gateway-simulator/.../SimulatorEndToEndTest.java` | 修改 | 新增 6 个管理 API E2E 测试 |
| `gateway-boot/.../integration/FullContextIntegrationTestBase.java` | 新增 | Mock 认证+路由的集成测试基类 |
| `gateway-boot/.../integration/FullContextIntegrationTest.java` | 新增 | 全链路韧性场景测试 |
| `gateway-boot/.../application-integration-test.yml` | 新增 | 集成测试配置 |

## 5. 风险与缓解

| 风险 | 缓解 |
|------|------|
| DegradationInvoker 降级后递归调用可能死循环 | degrade() 返回 null 时停止递归，测试验证链耗尽场景 |
| Mock 多个 Bean 导致测试脆弱 | 每个测试方法独立设置 Mock 行为，@BeforeEach 重置 |
| CircuitBreaker 状态跨测试污染 | 每个测试创建新的 ProviderSimulator，熔断器是 per-endpoint 的 |
