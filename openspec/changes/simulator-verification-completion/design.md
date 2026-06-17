# Simulator 验证补全 — 设计文档

## 高层架构

### Phase 1：Simulator 集成测试补齐

在 `gateway-simulator` 的 `SimulatorEndToEndTest` 中新增 E2E 测试，通过 HTTP 调用验证增强后的管理 API。

```
SimulatorEndToEndTest (新增)
├── testBehaviorSequence_consumesStepsViaHttp()
│   POST /simulator/behavior {"sequence":[500,401,200],"loop":false}
│   → GET /v1/chat/completions 3 次 → 500/401/200
│   → 第 4 次恢复全局模式 NORMAL
│
├── testBehaviorSequence_loop_resets()
│   POST /simulator/behavior {"sequence":[200,500],"loop":true}
│   → GET 6 次 → 200/500/200/500/200/500
│
├── testDelayConfig_appliesDelay()
│   POST /simulator/delay {"delayMs":100}
│   → 测量响应时间 ≥ 100ms
│
├── testStreamConfig_interruptAfter()
│   POST /simulator/stream {"action":"interrupt_after","chunks":2}
│   → GET stream=true → 收到 2 chunk 后中断
│
├── testApiKeyOverride_matchesByPrefix()
│   POST /simulator/apikey-override {"keyPrefix":"sk-key1","mode":"auth_error"}
│   → 带 Bearer sk-key1-xxx 请求 → 401
│   → 带 Bearer sk-other-xxx 请求 → 200 (全局 NORMAL)
```

### Phase 2：Gateway 全链路集成测试

```
FullContextIntegrationTestBase
├── @MockBean AuthenticationDomainService (返回固定 Identity)
├── @MockBean CredentialResolver (返回可控的 Credential 列表)
├── @MockBean RoutingResolver (返回固定 RoutingContext)
├── @MockBean DegradationService (可选: 返回降级模型/抛出异常)
│
├── Upstream: ProviderSimulator (MockWebServer, 随机端口)
│   Gateway 配置 endpoint 指向 MockWebServer
│
└── 通过 ChatDispatchService.dispatch() 直接调用
    (绕过 Controller 层和认证拦截器)
```

### Mock 策略详情

| Mock Bean | 方法 | 固定返回值 |
|-----------|------|-----------|
| `AuthenticationDomainService` | `authenticateUser()` | `Identity.of(1L, "user", 100L)` |
| `CredentialResolver` | `resolveAll(channelId)` | 根据测试返回 1 或 2 个 `ChannelCredential` |
| `RoutingResolver` | `resolve()` | `RoutingContext` 指向 MockWebServer URL |

### 测试场景架构

```
测试类: FullContextIntegrationTest
├── Nested: KeyFailoverTests
│   ├── testKeyFailover_key1Fails_key2Succeeds()
│   │   Mock: CredentialResolver → [key1(401), key2(200)]
│   │   → KeyFailoverInvoker 遍历 2 个 Key
│   │   → key1 401 → key2 成功
│   │   → 返回 ProtocolResponse
│   │
│   ├── testKeyFailover_allKeysFail()
│   │   Mock: CredentialResolver → [key1(401), key2(401)]
│   │   → 两个 Key 都失败
│   │   → 抛出 ProviderException("所有 Key 均失败")
│   │
│   └── testKeyFailover_circuitBreakerSkips()
│       Mock: CredentialResolver → [key1(熔断), key2(200)]
│       → KeyFailoverInvoker 跳过熔断 Key
│       → key2 成功
│
├── Nested: DegradationTests
│   ├── testDegradation_primaryFails_fallbackSucceeds()
│   │   Config: degradation.properties.chain = gpt-4→gpt-3.5
│   │   Mock: UpstreamClient → gpt-4 失败, gpt-3.5 成功
│   │   → DegradationInvoker 降级 → gpt-3.5 成功
│   │
│   └── testDegradation_chainExhausted()
│       Config: degradation.properties.chain = gpt-4→gpt-3.5
│       Mock: 两个模型都失败
│       → 抛出 ProviderException
│
├── Nested: ProtocolConversionTests
│   ├── testProtocolConversion_openaiToAnthropic()
│   │   RoutingContext: upstreamProtocol=ANTHROPIC
│   │   Request: OpenAIChatRequest
│   │   → 请求转换为 Anthropic 格式
│   │   → 响应转换回 OpenAI 格式
│   │
│   └── testProtocolConversion_anthropicToOpenAI()
│       RoutingContext: upstreamProtocol=OPENAI
│       Request: AnthropicMessagesRequest
│       → 请求转换为 OpenAI 格式
│       → 响应转换回 Anthropic 格式
│
├── Nested: ResilienceTests
│   ├── testCircuitBreaker_behaviorSequence()
│   │   ProviderSimulator: 连续 10 次 500
│   │   → CircuitBreaker OPEN
│   │   → 第 11 次 → CircuitOpenException
│   │
│   ├── testIntermittentFailures_retryRecovers()
│   │   ProviderSimulator: [200, 500, 200, 500, 200]
│   │   → RetryExecutor 重试后恢复
│   │   → 最终成功
│   │
│   └── testSlowResponse_timeoutError()
│       ProviderSimulator: bodyDelay=5s, client timeout=2s
│       → TIMEOUT_ERROR
│
└── Nested: StreamTests
    └── testStreamInterrupted_triggersOnError()
        ProviderSimulator: 中断的 SSE 流
        → onError 被触发
```

## 数据流

```
Test (JUnit)
  │ Mock 认证 → dispatch(request, mockIdentity)
  ▼
ChatDispatchServiceImpl.dispatch()
  │ RoutingResolver → Mock 返回 RoutingContext
  │ (含 endpointUrl=MockWebServer, channelId=1, endpointId=1)
  ▼
DegradationInvoker.invoke()
  │ 捕获 ProviderException → 调用 DegradationService.degrade()
  ▼
KeyFailoverInvoker.invoke()
  │ CredentialResolver.resolveAll() → Mock 返回 Credential 列表
  │ 遍历 Credential → buildClient() → client.chat()
  ▼
ResilientUpstreamClient.chat()
  │ CircuitBreaker.allowRequest() → 检查熔断状态
  │ RetryExecutor.execute() → 重试逻辑
  ▼
OpenAIUpstreamClient/AnthropicUpstreamClient.chat()
  │ OkHttp POST → MockWebServer
  ▼
ProviderSimulator (MockWebServer)
  │ 返回预置的响应 (200/429/500/SSE)
  ▼
Test 断言响应状态码、异常类型、调用次数
```

## 文件变更清单

### Phase 1：Simulator 管理 API E2E 测试

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `src/test/.../SimulatorEndToEndTest.java` | 修改 | 新增 6 个 E2E 测试（behavior/delay/stream/apikey） |

### Phase 2：Gateway 全链路集成测试

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `src/test/.../integration/FullContextIntegrationTestBase.java` | 新增 | Mock 认证+路由的集成测试基类 |
| `src/test/.../integration/FullContextIntegrationTest.java` | 新增 | 全链路韧性场景集成测试 |
| `src/test/resources/application-integration-test.yml` | 新增 | 集成测试配置 |
| `pom.xml` | 不变 | 已有 ProviderSimulator 和 Mock 依赖 |

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| DegradationInvoker 需要真实 DegradationService | 使用 @MockBean 模拟 degrade() 返回值，或配置最小化的降级链属性 |
| CredentialResolver.resolveAll 需要数据库数据 | @MockBean 返回预构建的 ChannelCredential 列表 |
| KeyFailoverInvoker 的 buildClient 需要 UpstreamClientRegistry | @MockBean 返回指向 MockWebServer 的 UpstreamClient，或直接用 ProviderSimulator |
| 测试耦合多个 Mock Bean 导致脆弱 | 每个测试用 @BeforeEach 重置 Mock 行为 |

## 6. 实施过程中的构建配置修复

### 6.1 问题发现

实施 Phase 2 集成测试后，运行全量构建发现 Maven failsafe 插件未绑定 executions，导致 `**/*IntegrationTest.java` 在默认 `mvn install`/`mvn verify` 中不完整运行：
- failsafe 只有 `<configuration>` 缺少 `<executions>`，integration-test/verify goal 未绑定生命周期
- 顶层 `*IntegrationTest` 类被 surefire excludes，且 failsafe 未运行，导致大部分集成测试被跳过

### 6.2 修复方案

在根 `pom.xml` 的 failsafe 插件配置中：
1. 补充 `<executions>` 绑定 integration-test + verify goal
2. 补充 `<additionalClasspathElements>` 指向 `${project.build.outputDirectory}`，解决 spring-boot:repackage 后 fat jar（应用类嵌在 BOOT-INF/classes/）导致 failsafe fork 类路径找不到应用类的 NoClassDefFoundError 问题

### 6.3 验证结果

修复后 `mvn clean install` 完整运行：
- surefire（test 阶段）：585 单元测试通过
- failsafe（integration-test 阶段）：29 集成测试通过，覆盖全部 7 个 IntegrationTest 类及 @Nested 内部类
