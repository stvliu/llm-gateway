# P2 协议传输归协议域 + 插件自包含 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把协议传输端口（UpstreamClient SPI）与 ProviderException 上浮 gateway-protocol 协议域，OpenAI/Anthropic 传输实现并入协议插件，解散 gateway-provider-http，达成「协议插件自包含：格式转换 + 传输调用」。

**Architecture:** 协议域核心模块 `gateway-protocol/protocol` 按业务概念重组包名（方案 B，2026-08-23 评审确定）：根包直放核心 API（ProtocolRequest/Response/StreamCallback/StreamChunkResult/ProtocolAdapter），子包 canonical/contract/validation/tuning/transport。`UpstreamClient<T extends ProtocolRequest>` 泛型化 + `supportedProvider()` 自描述（替代 instanceof），`UpstreamClientRegistry` 改为协议域注册表（按协议收集 `ProtocolUpstreamClientFactory`），`ResilientClientFactory` 一并上浮协议域。`ProviderErrorType` 保持 common（被 common 内部类使用，上浮会引入反向依赖）。行为不变（结构性重构）。

**Tech Stack:** Java 21、Spring Boot 3.5.13、Spring MVC、OkHttp 4.12、Jackson、Maven 多模块

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试）
- 每任务独立提交，commit message 中文（本计划全部提交已含）
- 行为不变：禁止在重构中改变协议转换语义、路由、配额、审计逻辑
- 目标包结构（方案 B）：
  ```
  com.codingas.gateway.protocol
  ├── 根包        ProtocolRequest ProtocolResponse StreamCallback StreamChunkResult ProtocolAdapter
  ├── canonical/  CanonicalChatRequest/Response/ContentBlock/Message/Tool/ToolCall/Usage
  ├── contract/   OpenAIChatRequest/Response AnthropicMessagesRequest/Response
  ├── validation/ ProtocolValidator ProtocolValidationException
  ├── tuning/     ProtocolTuner
  └── transport/  UpstreamClient<T> UpstreamClientRegistry UpstreamClientRegistryImpl
                  ProtocolUpstreamClientFactory ResilientClientFactory ConnectivityTestResult
                  ProviderException SseErrorFormatter ErrorClassificationStrategy
  ```
- `ProviderErrorType` 保持 `com.codingas.gateway.common.enums`（**不上浮**）
- provider 核心 `provider/upstream/` 保留：ConnectivityTester、Protocol、RoutingContext、RoutingStrategy、AuthStatus、KeyTestResult（非传输类不迁移）
- SseErrorFormatter 上浮协议核心 `protocol.transport`（**不进协议插件**——proxy 依赖它，进插件会引入 proxy→插件反向依赖；本点为设计文档 §6 偏差修正）
- 质量基建（jacoco 全模块 / freeze 基线入库 / provider-data 补测试）不在本计划范围，独立并行
- **原子切换约束**：旧 `UpstreamClient`/`UpstreamClientRegistry`/`ResilientClientFactory` 接口的删除与 client 迁移、装配、resilience/proxy 切换必须同一任务完成——中间态若新旧注册表并存会导致 Spring bean 冲突或 `getSupportedProtocols()` 变空（集成测试回归），故 Task 4 为原子切换点

---

## Task 1: 基线验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 确认基线全绿，作为后续任务回归基准

- [ ] **Step 1: 全量构建 + 测试**

```bash
cd /e/workspace/llm-gateway
./mvnw clean install
```

Expected: BUILD SUCCESS，全部模块测试通过（Windows 用 `./mvnw`，mvnw 在项目根）。

- [ ] **Step 2: 确认无失败**

检查输出无 `FAILURE`、无 `ERROR`。若有失败先记录并排查（用 systematic-debugging），不进入下一任务。

---

## Task 2: 协议域包名 Jmix 化（方案 B）

**Files:**
- Modify（协议核心模块内，改 package 声明 + 同模块 import）：
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/domain/protocol/contract/ProtocolRequest.java`、`ProtocolResponse.java`、`StreamCallback.java`、`StreamChunkResult.java` → `com.codingas.gateway.protocol`（根包）
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/domain/protocol/contract/OpenAIChatRequest.java`、`OpenAIChatResponse.java`、`AnthropicMessagesRequest.java`、`AnthropicMessagesResponse.java` → `com.codingas.gateway.protocol.contract`
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/domain/protocol/validation/ProtocolValidator.java`、`ProtocolValidationException.java` → `com.codingas.gateway.protocol.validation`
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/domain/protocol/tuning/ProtocolTuner.java` → `com.codingas.gateway.protocol.tuning`
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/api/capability/protocol/ProtocolAdapter.java` → `com.codingas.gateway.protocol`（根包）
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalChatRequest.java`、`CanonicalChatResponse.java`、`CanonicalContentBlock.java`、`CanonicalMessage.java`、`CanonicalTool.java`、`CanonicalToolCall.java`、`CanonicalUsage.java` → `com.codingas.gateway.protocol.canonical`
  - 协议核心测试：`CanonicalChatRequestTest.java`、`ProtocolAdapterContractTest.java`（同包迁移）
- Modify（连锁 import，规则：`domain.protocol.contract.ProtocolRequest|ProtocolResponse|StreamCallback|StreamChunkResult` → `protocol.*`；`domain.protocol.contract.OpenAIChatRequest|OpenAIChatResponse|AnthropicMessagesRequest|AnthropicMessagesResponse` → `protocol.contract.*`；`domain.protocol.validation.*` → `protocol.validation.*`；`domain.protocol.tuning.*` → `protocol.tuning.*`；`api.capability.protocol.ProtocolAdapter` → `protocol.ProtocolAdapter`；`api.capability.protocol.Canonical*` → `protocol.canonical.*`）：
  - **gateway-protocol 插件**：`protocol-openai/src/main/.../OpenAIProtocolAdapter.java`、`protocol-openai/src/test/.../OpenAIProtocolAdapterTest.java`、`protocol-anthropic/src/main/.../AnthropicProtocolAdapter.java`、`protocol-anthropic/src/test/.../AnthropicProtocolAdapterTest.java`、`protocol-gemini/src/main/.../GeminiProtocolAdapter.java`、`GeminiChatRequest.java`、`protocol-gemini/src/test/.../GeminiProtocolAdapterTest.java`
  - **gateway-boot main**：`adapter/api/AnthropicController.java`、`adapter/api/OpenAIController.java`、`adapter/api/SseStreamHelper.java`、`adapter/protocol/anthropic/AnthropicProtocolValidator.java`、`AnthropicTuner.java`、`adapter/protocol/openai/OpenAIProtocolValidator.java`、`OpenAITuner.java`、`application/experience/ModelExperienceService.java`
  - **gateway-boot test**：`adapter/protocol/anthropic/AnthropicTunerTest.java`、`integration/ChannelFailoverIntegrationTest.java`、`integration/CircuitBreakerIntegrationTest.java`、`integration/FullContextIntegrationTest.java`、`integration/FullContextIntegrationTestBase.java`、`integration/GeminiPluginIntegrationTest.java`、`integration/ProtocolConversionIntegrationTest.java`、`integration/SimulatorGatewayIntegrationTest.java`、`integration/TimeoutAndStreamIntegrationTest.java`、`providerhttp/upstream/AnthropicUpstreamClientTest.java`、`providerhttp/upstream/OpenAIUpstreamClientTest.java`、`proxy/conversion/ProtocolConversionFacadeTest.java`、`proxy/invoker/ChannelFailoverInvokerTest.java`、`support/ProviderSimulatorTest.java`
  - **gateway-proxy**：`proxy/chat/ChatDispatchService.java`、`ChatDispatchServiceImpl.java`、`proxy/conversion/OutboundTuner.java`、`ProtocolConversionFacade.java`、`ProtocolStreamConverter.java`、`proxy/invoker/ChannelFailoverInvoker.java`、`KeyFailoverInvoker.java` + 测试 5 个（`chat/ChatDispatchServiceTest.java`、`conversion/OutboundTunerTest.java`、`conversion/ProtocolStreamConverterTest.java`、`invoker/ChannelFailoverStrategyTest.java`、`invoker/KeyFailoverInvokerTest.java`）
  - **gateway-resilience**：`resilience/upstream/ResilientUpstreamClient.java` + `ResilientUpstreamClientTest.java`
  - **gateway-provider 核心**：`provider/upstream/UpstreamClient.java`（仅改自身 import，Task 4 删除）
  - **gateway-provider/provider-http**：`providerhttp/upstream/OpenAIUpstreamClient.java`、`AnthropicUpstreamClient.java`（仅改 import，Task 4 迁移）

**Interfaces:**
- Consumes: 无
- Produces: 协议域新包结构（本任务不含 transport 包，Task 3/4 创建）

- [ ] **Step 1: 协议核心模块内迁移**

用 IDE move（IntelliJ `Refactor > Move`）或手动：先改 8 个契约类（4 根包 + 4 contract），再 validation/tuning，再 ProtocolAdapter + 7 个 Canonical。每次移动让 IDE 自动更新同模块引用；手动则改 `package` 后全量编译按错误改 import。

- [ ] **Step 2: 连锁 import 修正**

按清单逐模块修正 import（规则见 Files 段）。全量编译，按编译错误逐个补齐。

- [ ] **Step 3: 验证无残留**

```bash
grep -rn "domain.protocol\|api.capability" gateway-protocol/protocol/src gateway-protocol/protocol-openai/src gateway-protocol/protocol-anthropic/src gateway-protocol/protocol-gemini/src gateway-boot/src gateway-proxy/proxy/src gateway-resilience/resilience/src gateway-provider/provider/src gateway-provider/provider-http/src 2>/dev/null | grep -v target
```

Expected: 无输出（空）。

- [ ] **Step 4: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: 协议域包名 Jmix 化（根包直放核心 API + canonical/contract/validation/tuning 子包，P2）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: ProviderException + SseErrorFormatter 上浮协议域

**Files:**
- Create: `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/ProviderException.java`（从 `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/vendor/ProviderException.java` 迁入，仅改 package，逻辑零改动）
- Create: `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/SseErrorFormatter.java`（从 `gateway-provider/provider-http/src/main/java/com/codingas/gateway/providerhttp/upstream/SseErrorFormatter.java` 迁入，改 package，删 `import ...provider.vendor.ProviderException;`）
- Delete: 上述两个源文件
- Create: `gateway-protocol/protocol/src/test/java/com/codingas/gateway/protocol/transport/SseErrorFormatterTest.java`（从 `gateway-boot/src/test/java/com/codingas/gateway/providerhttp/upstream/SseErrorFormatterTest.java` 迁入，改 package + import）
- Modify（import `provider.vendor.ProviderException` → `protocol.transport.ProviderException`）：
  - resilience main：`CircuitOpenException.java`（extends ProviderException）、`RetryExecutor.java`、`ResilientUpstreamClient.java`；测试 `RetryExecutorTest.java`、`ResilientUpstreamClientTest.java`
  - proxy main：`ChatDispatchServiceImpl.java`（含 `SseErrorFormatter.format` 全限定名 → 改 import 用短名）、`ChannelFailoverInvoker.java`、`KeyFailoverInvoker.java`；测试 `ChannelFailoverStrategyTest.java`、`KeyFailoverInvokerTest.java`
  - boot main：`adapter/advice/GlobalExceptionHandler.java`；测试 `GlobalExceptionHandlerTest.java`、`integration/ChannelFailoverIntegrationTest.java`、`integration/CircuitBreakerIntegrationTest.java`、`integration/FullContextIntegrationTest.java`、`integration/SimulatorGatewayIntegrationTest.java`、`integration/TimeoutAndStreamIntegrationTest.java`、`proxy/invoker/ChannelFailoverInvokerTest.java`
  - provider-http main：`providerhttp/upstream/OpenAIUpstreamClient.java`、`AnthropicUpstreamClient.java`（仅 import 改，Task 4 迁移）
- Modify（同步设计文档偏差修正）：`docs/superpowers/specs/2026-08-21-gateway-jmix-style-modularization-design.md` §6 P2 架构优化项——把 `SseErrorFormatter` 从「并入 protocol-openai」改为「上浮协议核心 protocol.transport」，注明原因（proxy 依赖，进插件会造成 proxy→插件反向依赖）

**Interfaces:**
- Consumes: Task 2 的 `com.codingas.gateway.protocol` 根包
- Produces: `com.codingas.gateway.protocol.transport.ProviderException`（构造器与 getter 完全不变）、`com.codingas.gateway.protocol.transport.SseErrorFormatter.format(ProviderException)`

- [ ] **Step 1: 迁入 ProviderException**

复制 `ProviderException.java` 到 `protocol/transport/`，改 `package com.codingas.gateway.protocol.transport;`。删除 provider 核心源文件。确认其 import 的 `GatewayException`、`ProviderErrorType` 均来自 `com.codingas.gateway.common.*`（现状已如此，无需改）。

- [ ] **Step 2: 迁入 SseErrorFormatter**

复制到 `protocol/transport/`，改 package，删对 `provider.vendor.ProviderException` 的 import（同包）。删除 provider-http 源文件。

- [ ] **Step 3: 连锁 import 修正**

按清单把全部 `import com.codingas.gateway.provider.vendor.ProviderException;` 改为 `import com.codingas.gateway.protocol.transport.ProviderException;`。特别注意 `ChatDispatchServiceImpl.java:150-151` 的两个全限定名（`com.codingas.gateway.provider.vendor.ProviderException`、`com.codingas.gateway.providerhttp.upstream.SseErrorFormatter`）改为短名。

- [ ] **Step 4: 测试随迁**

迁入 `SseErrorFormatterTest.java` 到协议核心测试包（改 package + import）。

- [ ] **Step 5: 同步设计文档**

设计文档 §6 P2 架构优化项：「`SseErrorFormatter` 并入 protocol-openai」→「`SseErrorFormatter` 上浮协议核心 `protocol.transport`（proxy 依赖它，进插件会造成 proxy→插件反向依赖）」。

- [ ] **Step 6: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（retry/circuitbreaker/resilient 相关测试绿，确认异常上浮无行为变化）。

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: ProviderException/SseErrorFormatter 上浮协议域 transport 包（P2）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: 协议传输 SPI 上浮 + 泛型化 + 插件自包含迁移（原子切换）

> **原子切换说明**：本任务同时完成「新建协议域 SPI → client 迁移并实现新接口 → 装配工厂 → resilience/proxy 切换 → 删除旧接口」，全程保持构建绿。不拆分的原因：旧接口删除瞬间所有实现方必须已迁移（provider-http client 依赖旧接口）；新旧注册表并存会 Spring bean 冲突；client 迁移后不立即装配会导致 `getSupportedProtocols()` 变空（集成测试回归）。步骤顺序为依赖序。

**Files:**
- Create（协议核心 transport 包）：
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/UpstreamClient.java`
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/UpstreamClientRegistry.java`
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/UpstreamClientRegistryImpl.java`
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/ProtocolUpstreamClientFactory.java`
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/ResilientClientFactory.java`
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/ConnectivityTestResult.java`（从 `provider/upstream/` 迁入，改 package）
  - `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/ErrorClassificationStrategy.java`（从 `provider-http/.../upstream/` 迁入，改 package）
- Create（协议插件）：
  - `gateway-protocol/protocol-openai/src/main/java/com/codingas/gateway/protocol/openai/OpenAIUpstreamClient.java`、`OpenAIErrorClassifier.java`（从 provider-http 迁入并改造）、`OpenAIUpstreamClientFactory.java`（新建）
  - `gateway-protocol/protocol-anthropic/src/main/java/com/codingas/gateway/protocol/anthropic/AnthropicUpstreamClient.java`、`AnthropicErrorClassifier.java`（迁入并改造）、`AnthropicUpstreamClientFactory.java`（新建）
- Create（测试随迁，boot → 插件）：
  - `gateway-protocol/protocol-openai/src/test/java/com/codingas/gateway/protocol/openai/OpenAIUpstreamClientTest.java`、`OpenAIErrorClassifierTest.java`
  - `gateway-protocol/protocol-anthropic/src/test/java/com/codingas/gateway/protocol/anthropic/AnthropicUpstreamClientTest.java`、`AnthropicErrorClassifierTest.java`
  - `gateway-protocol/protocol/src/test/java/com/codingas/gateway/protocol/transport/UpstreamClientRegistryImplTest.java`（新建）
- Delete（旧接口与旧实现）：
  - `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/upstream/UpstreamClient.java`、`UpstreamClientRegistry.java`、`ResilientClientFactory.java`、`ConnectivityTestResult.java`
  - `gateway-provider/provider-http/src/main/java/com/codingas/gateway/providerhttp/upstream/UpstreamClientRegistryImpl.java`（旧注册表，新注册表已建）
  - provider-http 已迁源文件（OpenAIUpstreamClient/AnthropicUpstreamClient/OpenAIErrorClassifier/AnthropicErrorClassifier——随 Step 移动删除）
  - boot 下已随迁测试 4 个（`providerhttp/upstream/OpenAIUpstreamClientTest/OpenAIErrorClassifierTest/AnthropicUpstreamClientTest/AnthropicErrorClassifierTest`）
- Modify（pom）：
  - `gateway-protocol/protocol/pom.xml`：加 `spring-context` 依赖（RegistryImpl `@Component`）
  - `gateway-protocol/protocol-openai/pom.xml`、`gateway-protocol/protocol-anthropic/pom.xml`：各加 `okhttp` 依赖（版本 `${okhttp.version}`）
- Modify（AutoConfiguration）：`OpenAIProtocolAutoConfiguration.java`、`AnthropicProtocolAutoConfiguration.java` 各注册 `XxxUpstreamClientFactory` Bean
- Modify（resilience）：
  - `ResilientUpstreamClient.java`：`implements UpstreamClient<ProtocolRequest>`、delegate 字段 `UpstreamClient<ProtocolRequest>`、import 更新
  - `ResilientClientFactoryImpl.java`：实现协议域 `ResilientClientFactory`、`resolveProviderCode` 改 `rawClient.supportedProvider()`、import 更新
  - 测试 `ResilientUpstreamClientTest.java`（import 更新）
- Modify（proxy）：`KeyFailoverInvoker.java`（import + 局部变量类型）、`KeyFailoverInvokerTest.java`、`ChannelFailoverInvokerTest.java`（boot 下）的 import 更新
- Modify（boot integration import）：`CircuitBreakerIntegrationTest.java`、`SimulatorGatewayIntegrationTest.java` 的 `providerhttp.upstream.OpenAIUpstreamClient` → `protocol.openai.OpenAIUpstreamClient`

**Interfaces:**
- Consumes: Task 3 的 `protocol.transport.ProviderException`；Task 2 的 `protocol.*`/`protocol.contract.*`
- Produces（本任务定稿，Task 5/6 依赖）：
  ```java
  package com.codingas.gateway.protocol.transport;

  public interface UpstreamClient<T extends ProtocolRequest> {
      ProtocolResponse chat(T request);
      void chatStream(T request, StreamCallback callback);
      ConnectivityTestResult testConnectivity();
      String supportedProvider();
  }

  public interface ProtocolUpstreamClientFactory {
      String supportedProtocol();
      UpstreamClient<? extends ProtocolRequest> create(String endpointUrl, String apiKey, int timeoutSeconds);
  }

  public interface UpstreamClientRegistry {
      UpstreamClient<ProtocolRequest> getClient(String protocol, String endpointUrl, String apiKey, int timeoutSeconds);
      List<String> getSupportedProtocols();
  }

  public interface ResilientClientFactory {
      UpstreamClient<ProtocolRequest> wrap(UpstreamClient<ProtocolRequest> rawClient, Long channelEndpointId);
  }
  ```

- [ ] **Step 1: 新建 UpstreamClient SPI**

`UpstreamClient.java`：

```java
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;

/**
 * 上游调用接口（协议传输端口）
 *
 * <p>每个实例绑定特定 Provider 配置（endpointUrl/apiKey/timeout），
 * 由 {@link ProtocolUpstreamClientFactory} 创建、{@link UpstreamClientRegistry} 按协议获取。</p>
 *
 * @param <T> 该客户端专署的协议请求类型
 */
public interface UpstreamClient<T extends ProtocolRequest> {

    /** 非流式调用 */
    ProtocolResponse chat(T request);

    /** 流式调用 */
    void chatStream(T request, StreamCallback callback);

    /** 连通性测试（测试已绑定 Provider 的连通性） */
    ConnectivityTestResult testConnectivity();

    /** 协议标识自描述（"openai"/"anthropic"），供韧性层按协议归类，替代对实现的 instanceof */
    String supportedProvider();
}
```

- [ ] **Step 2: 新建其余 SPI + 迁入两个类**

`ProtocolUpstreamClientFactory.java`、`UpstreamClientRegistry.java`、`ResilientClientFactory.java`、`UpstreamClientRegistryImpl.java`（见下方 Step 3 代码）；`ConnectivityTestResult.java` 从 `provider/upstream/` 原样迁入（record，字段 success/channelId/errorMessage/latencyMs + 静态工厂 success/failure，仅改 package）；`ErrorClassificationStrategy.java` 从 provider-http 迁入（`classify(int, String)` + `supportedProvider()`，改 package，import `common.enums.ProviderErrorType` 不变）。

```java
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.protocol.ProtocolRequest;

/**
 * 协议上游客户端工厂（每协议插件注册一个）
 *
 * <p>UpstreamClient 为每请求绑定配置的实例（非单例），注册表收集的是本工厂而非 client 实例。</p>
 */
public interface ProtocolUpstreamClientFactory {

    /** 支持的协议标识（"openai"/"anthropic"） */
    String supportedProtocol();

    /** 创建绑定指定 Provider 配置的 UpstreamClient */
    UpstreamClient<? extends ProtocolRequest> create(String endpointUrl, String apiKey, int timeoutSeconds);
}
```

```java
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.protocol.ProtocolRequest;
import java.util.List;

/**
 * 上游调用注册表（协议域）
 *
 * <p>按协议收集各插件 {@link ProtocolUpstreamClientFactory}，按协议获取绑定配置的 client。
 * 返回类型擦除为 {@code UpstreamClient<ProtocolRequest>}，调用方无需处理通配符。</p>
 */
public interface UpstreamClientRegistry {

    UpstreamClient<ProtocolRequest> getClient(String protocol, String endpointUrl, String apiKey, int timeoutSeconds);

    /** 获取系统支持的所有协议标识（来自已装配工厂，新增协议插件自动生效） */
    List<String> getSupportedProtocols();
}
```

```java
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.protocol.ProtocolRequest;

/**
 * 韧性客户端工厂（协议域端口，resilience 域提供实现）
 *
 * <p>为原始 UpstreamClient 包装熔断 + 重试等韧性保护。</p>
 */
public interface ResilientClientFactory {

    UpstreamClient<ProtocolRequest> wrap(UpstreamClient<ProtocolRequest> rawClient, Long channelEndpointId);
}
```

- [ ] **Step 3: 写 UpstreamClientRegistryImpl + 测试（先失败后过）**

`UpstreamClientRegistryImpl.java`：

```java
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.protocol.ProtocolRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 协议域注册表实现
 *
 * <p>注入全部 {@link ProtocolUpstreamClientFactory} Bean，按 supportedProtocol() 建立索引；
 * getClient 选择工厂创建实例并擦除泛型为基类，调用方无感。</p>
 */
@Component
public class UpstreamClientRegistryImpl implements UpstreamClientRegistry {

    private final Map<String, ProtocolUpstreamClientFactory> factories;

    public UpstreamClientRegistryImpl(List<ProtocolUpstreamClientFactory> factoryList) {
        this.factories = factoryList.stream()
                .collect(Collectors.toMap(ProtocolUpstreamClientFactory::supportedProtocol, Function.identity()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public UpstreamClient<ProtocolRequest> getClient(String protocol, String endpointUrl, String apiKey, int timeoutSeconds) {
        ProtocolUpstreamClientFactory factory = factories.get(protocol);
        if (factory == null) {
            throw new IllegalArgumentException("不支持的协议: " + protocol);
        }
        return (UpstreamClient<ProtocolRequest>) factory.create(endpointUrl, apiKey, timeoutSeconds);
    }

    @Override
    public List<String> getSupportedProtocols() {
        return new ArrayList<>(factories.keySet());
    }
}
```

`UpstreamClientRegistryImplTest.java`（协议核心测试包）：

```java
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpstreamClientRegistryImplTest {

    static class FakeFactory implements ProtocolUpstreamClientFactory {
        private final String protocol;
        FakeFactory(String protocol) { this.protocol = protocol; }
        @Override public String supportedProtocol() { return protocol; }
        @Override public UpstreamClient<? extends ProtocolRequest> create(String endpointUrl, String apiKey, int timeoutSeconds) {
            return new UpstreamClient<ProtocolRequest>() {
                @Override public ProtocolResponse chat(ProtocolRequest request) { return null; }
                @Override public void chatStream(ProtocolRequest request, StreamCallback callback) { }
                @Override public ConnectivityTestResult testConnectivity() { return null; }
                @Override public String supportedProvider() { return protocol; }
            };
        }
    }

    @Test
    void collectsFactoriesByProtocol() {
        UpstreamClientRegistry registry = new UpstreamClientRegistryImpl(
                List.of(new FakeFactory("openai"), new FakeFactory("anthropic")));
        assertThat(registry.getSupportedProtocols()).containsExactlyInAnyOrder("openai", "anthropic");
    }

    @Test
    void returnsClientForKnownProtocol() {
        UpstreamClientRegistry registry = new UpstreamClientRegistryImpl(List.of(new FakeFactory("openai")));
        UpstreamClient<ProtocolRequest> client = registry.getClient("openai", "http://x", "k", 30);
        assertThat(client.supportedProvider()).isEqualTo("openai");
    }

    @Test
    void rejectsUnknownProtocol() {
        UpstreamClientRegistry registry = new UpstreamClientRegistryImpl(List.of(new FakeFactory("openai")));
        assertThatThrownBy(() -> registry.getClient("gemini", "http://x", "k", 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的协议");
    }
}
```

- [ ] **Step 4: 运行测试确认通过 + 协议核心加 spring-context**

```bash
./mvnw test -pl gateway-protocol/protocol
```

Expected: PASS（3 个测试全绿）。然后 `gateway-protocol/protocol/pom.xml` 加：

```xml
<!-- Spring（注册表 @Component 装配） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
</dependency>
```

- [ ] **Step 5: 迁移 OpenAI 实现到插件并改造**

用**移动**（剪切，非复制）把 `OpenAIUpstreamClient.java`、`OpenAIErrorClassifier.java` 移到 `protocol-openai` 插件包，改造：
1. `package com.codingas.gateway.protocol.openai;`
2. `public class OpenAIUpstreamClient implements UpstreamClient<OpenAIChatRequest>`
3. `public ProtocolResponse chat(OpenAIChatRequest request)`、`public void chatStream(OpenAIChatRequest request, StreamCallback callback)`（参数类型精确化）
4. **新增 `supportedProvider()` 方法**：

```java
@Override
public String supportedProvider() {
    return "openai";
}
```

5. import：删 `provider.upstream.UpstreamClient/ConnectivityTestResult`、`provider.vendor.ProviderException`、`providerhttp.upstream.ErrorClassificationStrategy`；改 `com.codingas.gateway.protocol.UpstreamClient`、`com.codingas.gateway.protocol.transport.ConnectivityTestResult/ProviderException/ErrorClassificationStrategy`、`com.codingas.gateway.protocol.contract.OpenAIChatRequest/OpenAIChatResponse`
- 方法体零改动（`objectMapper.writeValueAsString(request)`、`request.setStream(true)` 不变）

`OpenAIErrorClassifier.java`：改 package + import（`ErrorClassificationStrategy` → `protocol.transport`）。

- [ ] **Step 6: OpenAI 工厂 + 装配 + pom**

`OpenAIUpstreamClientFactory.java`：

```java
package com.codingas.gateway.protocol.openai;

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.transport.ErrorClassificationStrategy;
import com.codingas.gateway.protocol.transport.ProtocolUpstreamClientFactory;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

/**
 * OpenAI 上游客户端工厂（协议插件自包含：格式转换 + 传输调用）
 */
public class OpenAIUpstreamClientFactory implements ProtocolUpstreamClientFactory {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ErrorClassificationStrategy classifier;

    public OpenAIUpstreamClientFactory(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.classifier = new OpenAIErrorClassifier();
    }

    @Override
    public String supportedProtocol() {
        return "openai";
    }

    @Override
    public UpstreamClient<? extends ProtocolRequest> create(String endpointUrl, String apiKey, int timeoutSeconds) {
        return new OpenAIUpstreamClient(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper, classifier);
    }
}
```

`OpenAIProtocolAutoConfiguration.java` 增加：

```java
@Bean
public OpenAIUpstreamClientFactory openAIUpstreamClientFactory(OkHttpClient httpClient, ObjectMapper objectMapper) {
    return new OpenAIUpstreamClientFactory(httpClient, objectMapper);
}
```

`protocol-openai/pom.xml` 增加：

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>${okhttp.version}</version>
</dependency>
```

- [ ] **Step 7: 迁移 Anthropic 实现 + 工厂 + 装配 + pom**

重复 Step 5-6 对应 Anthropic：`AnthropicUpstreamClient implements UpstreamClient<AnthropicMessagesRequest>`（`supportedProvider()` 返回 `"anthropic"`）、`AnthropicUpstreamClientFactory`、`AnthropicProtocolAutoConfiguration` 注册工厂、`protocol-anthropic/pom.xml` 加 okhttp。

- [ ] **Step 8: 测试随迁 + integration import**

把 boot 下 `providerhttp/upstream/` 的 4 个测试（OpenAIUpstreamClientTest/OpenAIErrorClassifierTest/AnthropicUpstreamClientTest/AnthropicErrorClassifierTest）移动到各自插件测试包，改 package + import（`providerhttp.upstream.*` → `protocol.openai.*`/`protocol.anthropic.*`、`protocol.transport.*`）。测试内 `OkHttpClient` 自行构建（`new OkHttpClient()`），若用 MockWebServer 则在插件 pom 加 `okhttp` 的 test 依赖 `mockwebserver`（`com.squareup.okhttp3:mockwebserver`，版本 `${okhttp.version}`）。

`CircuitBreakerIntegrationTest.java`、`SimulatorGatewayIntegrationTest.java`：`import com.codingas.gateway.providerhttp.upstream.OpenAIUpstreamClient;` → `import com.codingas.gateway.protocol.openai.OpenAIUpstreamClient;`

- [ ] **Step 9: resilience 切换**

`ResilientUpstreamClient.java`：
- `public class ResilientUpstreamClient implements UpstreamClient<ProtocolRequest>`
- 字段与构造器：`private final UpstreamClient<ProtocolRequest> delegate;`
- import：`provider.upstream.UpstreamClient/ConnectivityTestResult` → `protocol.transport.*`；`provider.vendor.ProviderException` → `protocol.transport.ProviderException`（Task 3 已改）；`domain.protocol.contract.*` → `com.codingas.gateway.protocol.*`（Task 2 已改，确认）
- 方法体零改动（`chat(ProtocolRequest)` 签名不变）

`ResilientClientFactoryImpl.java`：
- `implements ResilientClientFactory`（协议域，Task 3 后 `provider.upstream.ResilientClientFactory` 删除前先切到协议域版本）
- `resolveProviderCode` 改为：

```java
private String resolveProviderCode(UpstreamClient<ProtocolRequest> client) {
    return client.supportedProvider();
}
```

- import：删 `providerhttp.upstream.AnthropicUpstreamClient/OpenAIUpstreamClient`；`provider.upstream.UpstreamClient/ResilientClientFactory` → `protocol.transport.*`

- [ ] **Step 10: proxy 切换**

`KeyFailoverInvoker.java`：import `provider.upstream.UpstreamClient/UpstreamClientRegistry` → `protocol.transport.*`；`buildClient` 中 `UpstreamClient rawClient = clientRegistry.getClient(...)` → `UpstreamClient<ProtocolRequest> rawClient = ...`（Registry 擦除返回，无需 cast），`resilientClientFactory.wrap(rawClient, ...)` 返回值同为 `UpstreamClient<ProtocolRequest>`。`KeyFailoverInvokerTest.java`、`ChannelFailoverInvokerTest.java`（boot 下）import 同步更新。

- [ ] **Step 11: 删除旧接口与旧实现**

删除：
- provider 核心 `provider/upstream/`：`UpstreamClient.java`、`UpstreamClientRegistry.java`、`ResilientClientFactory.java`、`ConnectivityTestResult.java`
- provider-http `providerhttp/upstream/`：`UpstreamClientRegistryImpl.java`（旧注册表；其余 4 个 client/classifier 已在 Step 5/7 移动删除）
- 确认 provider-http 仅剩 `ConnectivityTesterImpl.java`（Task 5 处理）

- [ ] **Step 12: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（重点：RegistryImpl 测试、插件 upstream 测试、resilience 测试、boot integration 全绿；`getSupportedProtocols()` 仍返回 openai/anthropic——注册表从插件工厂收集）。

- [ ] **Step 13: Commit**

```bash
git add -A
git commit -m "refactor: UpstreamClient SPI 上浮协议域 + 泛型化 + 插件自包含迁移 + 注册表工厂化（P2）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 5: provider-http 解散 + ConnectivityTesterImpl 归 provider 核心

**Files:**
- Create: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/upstream/ConnectivityTesterImpl.java`（从 provider-http 迁入，改 import：`provider.upstream.UpstreamClientRegistry/ConnectivityTestResult` → `protocol.transport.*`）
- Delete: `gateway-provider/provider-http/` 整个模块目录
- Modify（pom）：
  - 根 `pom.xml`：移除 `<module>gateway-provider/provider-http</module>`
  - `gateway-boot/pom.xml`：移除 `gateway-provider-http` 依赖
  - `gateway-resilience/resilience/pom.xml`：移除 `gateway-provider-http` **和 `gateway-provider`** 依赖（Task 3/4 后 resilience 对 provider 的全部依赖已上浮协议域，可整体解耦）
- Modify（ArchUnit）：`gateway-boot/src/test/java/com/codingas/gateway/arch/LayerDependencyTest.java`——绑定模块根包列表（`"auditdata", "alertdata", "resiliencedata", "providerhttp"`）移除 `"providerhttp"`；删除 P1 过渡态违规注释中 `resilience→providerhttp`、`proxy→providerhttp` 两行（已消灭）

**Interfaces:**
- Consumes: Task 4 的协议域 SPI 与插件工厂
- Produces: provider-http 模块不存在；`provider.upstream.ConnectivityTesterImpl`（行为不变，`test(Channel)` 仍基于 Registry 执行）；resilience 不再依赖 provider 域

- [ ] **Step 1: 迁移 ConnectivityTesterImpl**

复制到 provider 核心 `provider/upstream/` 包（与 `ConnectivityTester` 接口同包），改 package + import（`provider.upstream.UpstreamClientRegistry/ConnectivityTestResult` → `protocol.transport.*`）。逻辑零改动（含既有 TODO 注释，不顺手扩大范围）。删除 provider-http 源文件。

- [ ] **Step 2: 删除 provider-http 模块**

删除目录 `gateway-provider/provider-http/`（含 pom.xml、src）。

- [ ] **Step 3: pom 清理**

根 pom 移除 module 声明；boot pom 移除 `gateway-provider-http`；resilience pom 移除 `gateway-provider-http` 和 `gateway-provider`。

- [ ] **Step 4: ArchUnit 规则更新**

`LayerDependencyTest.java`：绑定模块根包列表移除 `"providerhttp"`；删除注释中两条已消灭的 P1 过渡态违规说明。若 freeze 基线（`target/archunit`）含 providerhttp 残留条目导致测试失败，确认为基线残留后按 LayerDependencyTest 现有 freeze 管理方式更新基线（记录为已知操作；P2 质量基建项的「freeze 入库」另行处理）。

- [ ] **Step 5: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（ArchUnit 绿；resilience 构建依赖收敛验证）。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: gateway-provider-http 解散，ConnectivityTesterImpl 归 provider 核心，resilience 解耦 provider（P2）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 6: boot 使用方收尾 + 全局残留检查 + 全量回归

**Files:**
- Modify（boot 4 个使用者 import：`provider.upstream.UpstreamClient/UpstreamClientRegistry/ConnectivityTestResult` → `protocol.transport.*`）：
  - `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProtocolController.java`
  - `gateway-boot/src/main/java/com/codingas/gateway/application/experience/ModelExperienceService.java`（`UpstreamClient/UpstreamClientRegistry` → `protocol.transport.*`；`provider.upstream.Protocol` 保留不动）
  - `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderHealthProbe.java`（含 `ConnectivityTestResult`）
  - `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderHealthTracker.java`

**Interfaces:**
- Consumes: Task 2-5 全部产物
- Produces: 全部模块依赖方向收敛：proxy/resilience/boot 均通过协议域 SPI 使用传输能力，无 provider-http/provider.upstream 传输类残留

- [ ] **Step 1: boot 使用者 import 更新**

按清单把 4 个文件的 `provider.upstream` 传输类 import 改为 `protocol.transport`。`ModelExperienceService` 的 `provider.upstream.Protocol` 保留。

- [ ] **Step 2: 全局残留检查**

```bash
grep -rn "providerhttp\|provider.upstream.UpstreamClient\|provider.upstream.UpstreamClientRegistry\|provider.upstream.ResilientClientFactory\|provider.upstream.ConnectivityTestResult\|provider.vendor.ProviderException\|domain.protocol\|api.capability" gateway-*/ --include="*.java" 2>/dev/null | grep -v target
```

Expected: 仅剩 provider 核心保留类的合法使用（`provider.upstream.Protocol|RoutingContext|RoutingStrategy|AuthStatus|KeyTestResult|ConnectivityTester|ConnectivityTesterImpl`）及协议域自身；无任何 `providerhttp`/`provider.vendor`/旧协议包残留。测试文件也应无残留（若有 boot 测试遗漏，一并修正）。

- [ ] **Step 3: 全量构建 + 测试回归**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全模块测试绿（重点：boot integration、proxy invoker、resilience、插件 adapter/upstream 测试、协议核心 RegistryImpl 测试）。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: boot 改用协议域 UpstreamClient SPI，P2 协议传输归域收尾（P2）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**（设计文档 §6 P2 架构优化项 → 任务）：
- 协议传输归协议域 + 插件自包含 → T4/T5/T6
- ProviderException 上浮（ProviderErrorType 保持 common）→ T3
- UpstreamClient 泛型化 → T4
- ResilientClientFactory 上浮协议域（2026-08-23 决策）→ T4
- ConnectivityTestResult 双版本区分（只上浮 upstream 版）→ T4（boot 应用层 DTO 未动）
- 协议域包名 Jmix 化（2026-08-23 决策，方案 B）→ T2
- SseErrorFormatter 上浮修正（设计文档偏差）→ T3（含文档同步）
- proxy 依赖调整 → T4/T6
- resilience 解耦 provider（额外收益）→ T5
- 质量基建（jacoco/freeze/补测试）→ 不在本计划（独立并行）

**Placeholder 扫描**：无 TBD/TODO；所有新代码（SPI/RegistryImpl/工厂/测试）含完整代码；搬移类给出明确来源、目标包与改动点。

**Type 一致性核对**：
- `UpstreamClient<T extends ProtocolRequest>`（T4 定义）↔ `OpenAIUpstreamClient implements UpstreamClient<OpenAIChatRequest>`、`AnthropicUpstreamClient implements UpstreamClient<AnthropicMessagesRequest>`、`ResilientUpstreamClient implements UpstreamClient<ProtocolRequest>`（T4 使用）✓
- `ProtocolUpstreamClientFactory.supportedProtocol()/create()`（T4 定义）↔ OpenAI/AnthropicUpstreamClientFactory（T4 实现）↔ `UpstreamClientRegistryImpl` 按 supportedProtocol 建 map（T4）✓
- `UpstreamClientRegistry.getClient` 返回 `UpstreamClient<ProtocolRequest>`（T4）↔ `KeyFailoverInvoker.buildClient` 直接调用、`ResilientClientFactory.wrap` 参数类型（T4）✓
- `supportedProvider()`（T4 接口新增）↔ 两个 client 实现 + `ResilientClientFactoryImpl.resolveProviderCode` 消费（T4）✓
- `ResilientClientFactory.wrap`（T4 协议域）↔ `ResilientClientFactoryImpl` 实现（T4）✓

**遗漏检查**：
- LayerDependencyTest providerhttp 基线 → T5
- boot 4 个使用者 → T6
- boot 测试 import（provider.vendor/domain.protocol/providerhttp）→ T2/T3/T4/T6 覆盖
- 新旧注册表 Spring bean 冲突 / 注册表空集成回归 → 由 T4 原子切换约束规避
- gemini 插件无传输（行为不变，不阻塞）→ 明确不在范围
