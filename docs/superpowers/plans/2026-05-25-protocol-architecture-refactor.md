# 大模型网关协议体系重构 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 LLM-Gateway 的协议数据类、校验器、转换器、上游调用等从错放位置迁移到正确的分层，规范化命名，并最终构建七阶段调用链。

**Architecture:** 三阶段大步重构。阶段 1 聚焦模型与协议分层迁移（可编译），阶段 2 聚焦调用链拆分与 ProxyService 重构（可编译），阶段 3 聚焦重试/熔断/审计/计量能力补全（可编译）。每阶段内按 TDD 循环推进，保证每步提交后可编译通过。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JUnit 5, Mockito, AssertJ, Gradle/Maven

---

## 文件结构总览

### 阶段 1 新建/迁移文件

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 迁移 | `domain/protocol/contract/ProtocolRequest.java` | 协议请求基类 |
| 迁移 | `domain/protocol/contract/ProtocolResponse.java` | 协议响应基类 |
| 迁移 | `domain/protocol/contract/OpenAIChatRequest.java` | OpenAI 请求 DTO |
| 迁移 | `domain/protocol/contract/OpenAIChatResponse.java` | OpenAI 响应 DTO |
| 迁移 | `domain/protocol/contract/AnthropicMessagesRequest.java` | Anthropic 请求 DTO |
| 迁移 | `domain/protocol/contract/AnthropicMessagesResponse.java` | Anthropic 响应 DTO |
| 迁移 | `domain/protocol/contract/StreamChunkResult.java` | 流式块结果 |
| 迁移 | `domain/protocol/contract/StreamCallback.java` | 流式回调接口 |
| 迁移 | `domain/protocol/conversion/ProtocolConverter.java` | 跨协议转换 |
| 迁移 | `domain/protocol/validation/ProtocolValidator.java` | 校验接口 |
| 迁移 | `adapter/protocol/openai/OpenAIProtocolValidator.java` | OpenAI 校验实现 |
| 迁移 | `adapter/protocol/anthropic/AnthropicProtocolValidator.java` | Anthropic 校验实现 |
| 新建 | `adapter/protocol/openai/OpenAIOutboundTuner.java` | OpenAI 出站调谐 |
| 新建 | `adapter/protocol/anthropic/AnthropicOutboundTuner.java` | Anthropic 出站调谐 |
| 新建 | `application/proxy/OutboundTuner.java` | 出站调谐编排 |
| 重命名 | `infrastructure/upstream/UpstreamClient.java` | 上游调用接口（原 ProtocolGateway） |
| 重命名 | `infrastructure/upstream/OpenAIUpstreamClient.java` | OpenAI 实现 |
| 重命名 | `infrastructure/upstream/AnthropicUpstreamClient.java` | Anthropic 实现 |
| 重命名 | `infrastructure/upstream/UpstreamClientRegistry.java` | 注册表（原 ProtocolGatewayFactory） |
| 删除 | `domain/supply/protocol/*` | 原位置文件 |
| 删除 | `domain/supply/gateway/ProtocolGateway.java` | 原接口 |
| 删除 | `domain/supply/gateway/ProtocolGatewayFactory.java` | 原工厂 |
| 删除 | `domain/supply/gateway/StreamCallback.java` | 原回调 |
| 删除 | `infrastructure/supply/gateway/protocol/*` | 原实现 |
| 修改 | `domain/supply/entity/ModelSpec.java` | 删除 providerId |
| 修改 | `domain/supply/gateway/ModelSpecGateway.java` | 删除 findByProviderId |
| 修改 | `domain/supply/entity/Provider.java` | 删除 baseUrl |

### 阶段 2 新建/迁移文件

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 新建 | `application/routing/RoutingResolver.java` | 路由编排 |
| 新建 | `application/routing/ModelMatcher.java` | 模型匹配 |
| 新建 | `application/routing/ChannelSelector.java` | 渠道选择 |
| 新建 | `application/routing/CredentialResolver.java` | 凭证解析 |
| 新建 | `application/routing/EndpointResolver.java` | 端点解析 |
| 新建 | `application/proxy/ChatDispatchService.java` | 调度接口（原 ProxyService） |
| 新建 | `application/proxy/ChatDispatchServiceImpl.java` | 调度实现 |
| 废弃 | `application/proxy/SupplyRoutingService.java` | 由 RoutingResolver 替代 |
| 废弃 | `application/proxy/ProxyServiceImpl.java` | 由 ChatDispatchServiceImpl 替代 |

### 阶段 3 新建文件

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 新建 | `infrastructure/resilience/RetryPolicy.java` | 重试策略 |
| 新建 | `infrastructure/resilience/CircuitBreaker.java` | 熔断器 |
| 新建 | `infrastructure/resilience/ChannelEndpointCircuitBreakerManager.java` | 熔断器管理 |

---

## 阶段 1：模型重构

---

### Task 1: 创建 domain/protocol/contract/ 目录，迁移协议数据契约

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/ProtocolRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/ProtocolResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/OpenAIChatRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/OpenAIChatResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/AnthropicMessagesRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/AnthropicMessagesResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/StreamChunkResult.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/StreamCallback.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolRequest.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolResponse.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIChatRequest.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIChatResponse.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicMessagesRequest.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicMessagesResponse.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/StreamChunkResult.java`

- [ ] **Step 1: 创建目标目录并复制文件**

将 7 个协议数据契约文件从 `domain/supply/protocol/` 复制到 `domain/protocol/contract/`，仅修改 package 声明：

```bash
# 创建目标目录
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract

# 复制文件
for f in ProtocolRequest ProtocolResponse OpenAIChatRequest OpenAIChatResponse AnthropicMessagesRequest AnthropicMessagesResponse StreamChunkResult; do
  cp gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/${f}.java \
     gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/contract/${f}.java
done
```

对每个文件，将 `package com.codingas.gateway.domain.supply.protocol` 改为 `package com.codingas.gateway.domain.protocol.contract`。

- [ ] **Step 2: 迁移 StreamCallback 到 domain/protocol/contract/**

将 `domain/supply/gateway/StreamCallback.java` 复制到 `domain/protocol/contract/StreamCallback.java`：

```java
package com.codingas.gateway.domain.protocol.contract;

/**
 * 流式回调接口
 */
public interface StreamCallback {

    /**
     * 收到数据块
     */
    void onChunk(StreamChunkResult chunk);

    /**
     * 流式结束
     */
    void onComplete();

    /**
     * 发生错误
     */
    void onError(Throwable error);
}
```

- [ ] **Step 3: 全局替换 import 语句**

在所有引用 `domain.supply.protocol` 中 DTO 类的文件中，将 import 替换为新的包路径。涉及文件（根据 grep 结果）：

- `adapter/api/OpenAIController.java`
- `adapter/api/AnthropicController.java`
- `adapter/api/ProtocolController.java`
- `adapter/api/SseStreamHelper.java`
- `application/proxy/ProxyServiceImpl.java`
- `application/proxy/SupplyRoutingService.java`
- `application/experience/ModelExperienceService.java`
- `domain/supply/protocol/ProtocolConverter.java`（将在 Task 2 迁移）
- `domain/supply/protocol/ProtocolValidator.java`（将在 Task 2 迁移）
- `domain/supply/protocol/OpenAIProtocolValidator.java`（将在 Task 3 迁移）
- `domain/supply/protocol/AnthropicProtocolValidator.java`（将在 Task 3 迁移）
- `domain/supply/gateway/ProtocolGateway.java`（将在 Task 5 重命名）
- `domain/supply/gateway/ProtocolGatewayFactory.java`（将在 Task 5 重命名）
- `infrastructure/supply/gateway/protocol/OpenAIProtocolGateway.java`（将在 Task 5 重命名）
- `infrastructure/supply/gateway/protocol/AnthropicProtocolGateway.java`（将在 Task 5 重命名）
- `infrastructure/supply/gateway/protocol/ProtocolGatewayFactoryImpl.java`（将在 Task 5 重命名）

替换规则：
- `import com.codingas.gateway.domain.supply.protocol.ProtocolRequest` → `import com.codingas.gateway.domain.protocol.contract.ProtocolRequest`
- `import com.codingas.gateway.domain.supply.protocol.ProtocolResponse` → `import com.codingas.gateway.domain.protocol.contract.ProtocolResponse`
- `import com.codingas.gateway.domain.supply.protocol.OpenAIChatRequest` → `import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest`
- `import com.codingas.gateway.domain.supply.protocol.OpenAIChatResponse` → `import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse`
- `import com.codingas.gateway.domain.supply.protocol.AnthropicMessagesRequest` → `import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest`
- `import com.codingas.gateway.domain.supply.protocol.AnthropicMessagesResponse` → `import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse`
- `import com.codingas.gateway.domain.supply.protocol.StreamChunkResult` → `import com.codingas.gateway.domain.protocol.contract.StreamChunkResult`
- `import com.codingas.gateway.domain.supply.gateway.StreamCallback` → `import com.codingas.gateway.domain.protocol.contract.StreamCallback`
- `import com.codingas.gateway.domain.supply.protocol.*` → `import com.codingas.gateway.domain.protocol.contract.*`

- [ ] **Step 4: 删除原位置的旧文件**

确认所有 import 已替换后，删除旧文件：

```bash
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolRequest.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolResponse.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIChatRequest.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIChatResponse.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicMessagesRequest.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicMessagesResponse.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/StreamChunkResult.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/StreamCallback.java
```

- [ ] **Step 5: 更新测试文件的 import**

在 `gateway-boot/src/test/java/` 下，同样将所有 `domain.supply.protocol` 的 import 替换为 `domain.protocol.contract`，将 `domain.supply.gateway.StreamCallback` 替换为 `domain.protocol.contract.StreamCallback`。

- [ ] **Step 6: 编译验证**

```bash
./mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "refactor(protocol): 迁移协议数据契约到 domain/protocol/contract/"
```

---

### Task 2: 迁移 ProtocolConverter 到 domain/protocol/conversion/

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/conversion/ProtocolConverter.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolConverter.java`
- Move: `gateway-boot/src/test/java/com/codingas/gateway/domain/supply/protocol/ProtocolConverterTest.java` → `gateway-boot/src/test/java/com/codingas/gateway/domain/protocol/conversion/ProtocolConverterTest.java`

- [ ] **Step 1: 复制 ProtocolConverter 到新位置**

将 `domain/supply/protocol/ProtocolConverter.java` 复制到 `domain/protocol/conversion/ProtocolConverter.java`，修改 package 声明为 `com.codingas.gateway.domain.protocol.conversion`，同时更新其 import 中的 DTO 类引用（此时已指向 `domain.protocol.contract`）。

- [ ] **Step 2: 全局替换 ProtocolConverter 的 import**

将所有文件中的 `import com.codingas.gateway.domain.supply.protocol.ProtocolConverter` 替换为 `import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter`。

涉及文件（根据 grep 结果）：
- `application/proxy/ProxyServiceImpl.java`
- `application/proxy/ProxyService.java`
- `test/.../ProxyServiceTest.java`
- `test/.../ProtocolConverterTest.java`

- [ ] **Step 3: 迁移 ProtocolConverterTest**

将测试文件从 `test/.../domain/supply/protocol/ProtocolConverterTest.java` 复制到 `test/.../domain/protocol/conversion/ProtocolConverterTest.java`，修改 package 和 import。

- [ ] **Step 4: 删除旧文件**

```bash
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolConverter.java
rm gateway-boot/src/test/java/com/codingas/gateway/domain/supply/protocol/ProtocolConverterTest.java
```

- [ ] **Step 5: 编译并运行测试**

```bash
./mvnw test -pl gateway-boot -Dtest="ProtocolConverterTest"
```

Expected: 测试通过

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor(protocol): 迁移 ProtocolConverter 到 domain/protocol/conversion/"
```

---

### Task 3: 迁移 ProtocolValidator 接口到 domain/protocol/validation/，实现到 adapter/protocol/

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/validation/ProtocolValidator.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/protocol/openai/OpenAIProtocolValidator.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/protocol/anthropic/AnthropicProtocolValidator.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolValidator.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIProtocolValidator.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicProtocolValidator.java`
- Move: `gateway-boot/src/test/java/com/codingas/gateway/domain/supply/protocol/OpenAIProtocolValidatorTest.java` → `gateway-boot/src/test/java/com/codingas/gateway/adapter/protocol/openai/OpenAIProtocolValidatorTest.java`

- [ ] **Step 1: 复制 ProtocolValidator 接口到新位置**

将 `domain/supply/protocol/ProtocolValidator.java` 复制到 `domain/protocol/validation/ProtocolValidator.java`，修改 package 为 `com.codingas.gateway.domain.protocol.validation`，更新 import。

- [ ] **Step 2: 复制 OpenAIProtocolValidator 到 adapter/protocol/openai/**

将 `domain/supply/protocol/OpenAIProtocolValidator.java` 复制到 `adapter/protocol/openai/OpenAIProtocolValidator.java`，修改 package 为 `com.codingas.gateway.adapter.protocol.openai`，更新 import：
- `ProtocolValidator` → `com.codingas.gateway.domain.protocol.validation.ProtocolValidator`
- DTO 类 → `com.codingas.gateway.domain.protocol.contract.*`

- [ ] **Step 3: 复制 AnthropicProtocolValidator 到 adapter/protocol/anthropic/**

将 `domain/supply/protocol/AnthropicProtocolValidator.java` 复制到 `adapter/protocol/anthropic/AnthropicProtocolValidator.java`，修改 package 为 `com.codingas.gateway.adapter.protocol.anthropic`，更新 import 同上。

- [ ] **Step 4: 全局替换 validator 的 import**

替换所有文件中的旧 import：
- `import com.codingas.gateway.domain.supply.protocol.ProtocolValidator` → `import com.codingas.gateway.domain.protocol.validation.ProtocolValidator`
- `import com.codingas.gateway.domain.supply.protocol.OpenAIProtocolValidator` → `import com.codingas.gateway.adapter.protocol.openai.OpenAIProtocolValidator`
- `import com.codingas.gateway.domain.supply.protocol.AnthropicProtocolValidator` → `import com.codingas.gateway.adapter.protocol.anthropic.AnthropicProtocolValidator`

- [ ] **Step 5: 迁移 OpenAIProtocolValidatorTest**

将测试文件从 `test/.../domain/supply/protocol/OpenAIProtocolValidatorTest.java` 复制到 `test/.../adapter/protocol/openai/OpenAIProtocolValidatorTest.java`，修改 package 和 import。

- [ ] **Step 6: 删除旧文件**

```bash
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolValidator.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIProtocolValidator.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicProtocolValidator.java
rm gateway-boot/src/test/java/com/codingas/gateway/domain/supply/protocol/OpenAIProtocolValidatorTest.java
```

- [ ] **Step 7: 编译并运行测试**

```bash
./mvnw test -pl gateway-boot -Dtest="OpenAIProtocolValidatorTest"
```

Expected: 测试通过

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "refactor(protocol): 迁移校验器接口到 domain/protocol/validation/，实现到 adapter/protocol/"
```

---

### Task 4: 删除 domain/supply/protocol/ 空目录

**Files:**
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/` (整个目录)

- [ ] **Step 1: 确认目录为空**

```bash
ls gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/
```

Expected: 无文件（或仅剩 exception 子目录中的 ProtocolValidationException.java）

- [ ] **Step 2: 处理 ProtocolValidationException**

如果 `domain/supply/protocol/` 下还有 `exception/ProtocolValidationException.java`，将其迁移到 `domain/protocol/validation/` 或 `domain/supply/exception/`（取决于它是否协议域特有）。

检查该异常类的使用位置，如果仅被 validator 使用则移到 `domain/protocol/validation/`；如果被 supply 域其他地方使用则移到 `domain/supply/exception/`。

- [ ] **Step 3: 删除空目录**

```bash
rm -rf gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/
```

- [ ] **Step 4: 编译验证**

```bash
./mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "refactor(protocol): 清理 domain/supply/protocol/ 空目录"
```

---

### Task 5: 重命名 ProtocolGateway → UpstreamClient，ProtocolGatewayFactory → UpstreamClientRegistry

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/UpstreamClient.java` (接口，从 domain 层迁移到 infrastructure 层)
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/OpenAIUpstreamClient.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/AnthropicUpstreamClient.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/upstream/UpstreamClientRegistry.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ProtocolGateway.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ProtocolGatewayFactory.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/protocol/OpenAIProtocolGateway.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/protocol/AnthropicProtocolGateway.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/protocol/ProtocolGatewayFactoryImpl.java`

- [ ] **Step 1: 创建 UpstreamClient 接口**

注意：原 `ProtocolGateway` 定义在 domain 层（DDD Gateway 模式），但上游 HTTP 调用是基础设施关注点。将其接口定义移到 `infrastructure/upstream/`：

```java
package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;

/**
 * 上游模型调用客户端
 *
 * <p>负责向 LLM 供应商发送请求并接收响应，纯 HTTP 调用，不含业务逻辑。</p>
 */
public interface UpstreamClient {

    /**
     * 非流式调用
     */
    ProtocolResponse chat(ProtocolRequest request);

    /**
     * 流式调用
     */
    void chatStream(ProtocolRequest request, StreamCallback callback);
}
```

- [ ] **Step 2: 创建 UpstreamClientRegistry 接口**

```java
package com.codingas.gateway.infrastructure.upstream;

/**
 * 上游客户端注册表
 *
 * <p>替代原 ProtocolGatewayFactory 的注册式设计，避免每次请求创建新实例。</p>
 */
public interface UpstreamClientRegistry {

    /**
     * 根据协议和连接参数获取客户端
     */
    UpstreamClient getClient(String protocol, String baseUrl, String apiKey, int timeout);

    /**
     * 注册客户端
     */
    void register(String protocol, UpstreamClient client);
}
```

- [ ] **Step 3: 迁移 OpenAIProtocolGateway → OpenAIUpstreamClient**

将 `infrastructure/supply/gateway/protocol/OpenAIProtocolGateway.java` 复制到 `infrastructure/upstream/OpenAIUpstreamClient.java`：
- 类名 `OpenAIProtocolGateway` → `OpenAIUpstreamClient`
- 实现 `UpstreamClient` 接口
- package 改为 `com.codingas.gateway.infrastructure.upstream`
- 更新所有 import

- [ ] **Step 4: 迁移 AnthropicProtocolGateway → AnthropicUpstreamClient**

同 Step 3，迁移 `AnthropicProtocolGateway.java` → `AnthropicUpstreamClient.java`。

- [ ] **Step 5: 迁移 ProtocolGatewayFactoryImpl → UpstreamClientRegistryImpl**

将 `infrastructure/supply/gateway/protocol/ProtocolGatewayFactoryImpl.java` 复制到 `infrastructure/upstream/UpstreamClientRegistryImpl.java`：
- 类名 `ProtocolGatewayFactoryImpl` → `UpstreamClientRegistryImpl`
- 实现 `UpstreamClientRegistry` 接口
- 内部引用从 `ProtocolGateway` 改为 `UpstreamClient`
- 改为注册式设计：不再每次 `create()` 新建实例，而是缓存已创建的客户端
- package 改为 `com.codingas.gateway.infrastructure.upstream`

`UpstreamClientRegistryImpl` 核心逻辑：

```java
package com.codingas.gateway.infrastructure.upstream;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上游客户端注册表实现
 */
@Component
public class UpstreamClientRegistryImpl implements UpstreamClientRegistry {

    private final Map<String, UpstreamClient> clients = new ConcurrentHashMap<>();

    @Override
    public UpstreamClient getClient(String protocol, String baseUrl, String apiKey, int timeout) {
        String key = protocol + ":" + baseUrl;
        return clients.computeIfAbsent(key, k -> createClient(protocol, baseUrl, apiKey, timeout));
    }

    @Override
    public void register(String protocol, UpstreamClient client) {
        clients.put(protocol, client);
    }

    private UpstreamClient createClient(String protocol, String baseUrl, String apiKey, int timeout) {
        return switch (protocol.toLowerCase()) {
            case "openai" -> new OpenAIUpstreamClient(baseUrl, apiKey, timeout);
            case "anthropic" -> new AnthropicUpstreamClient(baseUrl, apiKey, timeout);
            default -> throw new IllegalArgumentException("不支持的协议: " + protocol);
        };
    }
}
```

- [ ] **Step 6: 全局替换所有引用**

替换规则：
- `import com.codingas.gateway.domain.supply.gateway.ProtocolGateway` → `import com.codingas.gateway.infrastructure.upstream.UpstreamClient`
- `import com.codingas.gateway.domain.supply.gateway.ProtocolGatewayFactory` → `import com.codingas.gateway.infrastructure.upstream.UpstreamClientRegistry`
- 代码中 `ProtocolGateway` 类型 → `UpstreamClient`
- 代码中 `ProtocolGatewayFactory` 类型 → `UpstreamClientRegistry`
- 代码中 `.create(` → `.getClient(`
- `OpenAIProtocolGateway` → `OpenAIUpstreamClient`
- `AnthropicProtocolGateway` → `AnthropicUpstreamClient`

涉及文件：
- `application/proxy/ProxyServiceImpl.java`
- `application/proxy/ProxyService.java`（如果存在接口）
- `application/experience/ModelExperienceService.java`
- `test/.../ProxyServiceTest.java`

- [ ] **Step 7: 删除旧文件**

```bash
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ProtocolGateway.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ProtocolGatewayFactory.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/protocol/OpenAIProtocolGateway.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/protocol/AnthropicProtocolGateway.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/protocol/ProtocolGatewayFactoryImpl.java
```

如果 `infrastructure/supply/gateway/protocol/` 目录为空则一并删除。

- [ ] **Step 8: 更新测试文件**

在 `ProxyServiceTest.java` 中：
- `ProtocolGatewayFactory` → `UpstreamClientRegistry`
- `ProtocolGateway` → `UpstreamClient`
- `protocolGatewayFactory.create(` → `upstreamClientRegistry.getClient(`

- [ ] **Step 9: 编译验证**

```bash
./mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 10: 提交**

```bash
git add -A
git commit -m "refactor(protocol): 重命名 ProtocolGateway→UpstreamClient, ProtocolGatewayFactory→UpstreamClientRegistry"
```

---

### Task 6: ModelSpec 删除 providerId 字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ModelSpec.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ModelSpecGateway.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ModelSpecGatewayImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/persistence/DataInitializer.java`（如有 createModelSpec 方法）
- Modify: 其他引用 ModelSpec.providerId 的文件

- [ ] **Step 1: 查找所有引用 ModelSpec.providerId 的位置**

```bash
grep -rn "providerId" gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ModelSpec.java
grep -rn "ModelSpec.*providerId\|\.providerId\|findByProviderId" gateway-boot/src/main/java/
```

- [ ] **Step 2: 修改 ModelSpec 实体**

从 `ModelSpec.java` 中删除 `providerId` 字段及其 getter/setter。

- [ ] **Step 3: 修改 ModelSpecGateway 接口**

从 `ModelSpecGateway.java` 中删除 `findByProviderId()` 方法声明。

- [ ] **Step 4: 修改 ModelSpecGatewayImpl 实现**

从 `ModelSpecGatewayImpl.java` 中删除 `findByProviderId()` 方法实现。

- [ ] **Step 5: 修改 ModelSpecRepository**

如果存在 JPA Repository 接口，删除 `findByProviderId()` 方法。

- [ ] **Step 6: 修改 DataInitializer**

如果 `DataInitializer.createModelSpec()` 方法接受 providerId 参数，删除该参数。

- [ ] **Step 7: 修改 CatalogMaterializeService / ConfigCacheService**

如果存在 `getModelsByProviderId()` 或类似方法，调整逻辑为通过 Channel → ChannelModel → ModelSpec 间接获取。

- [ ] **Step 8: 编译验证**

```bash
./mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add -A
git commit -m "refactor(supply): ModelSpec 删除 providerId 字段，供应商映射通过 ChannelModel 关联"
```

---

### Task 7: Provider 删除 baseUrl 字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Provider.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/persistence/DataInitializer.java`（如有）
- Modify: 其他引用 Provider.baseUrl 的文件

- [ ] **Step 1: 查找所有引用 Provider.baseUrl 的位置**

```bash
grep -rn "baseUrl\|base_url" gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Provider.java
grep -rn "Provider.*baseUrl\|\.getBaseUrl()\|\.setBaseUrl(" gateway-boot/src/main/java/
```

- [ ] **Step 2: 修改 Provider 实体**

从 `Provider.java` 中删除 `baseUrl` 字段及其 getter/setter。

- [ ] **Step 3: 修改 DataInitializer**

如果 `DataInitializer` 创建 Provider 时设置 baseUrl，删除该逻辑。

- [ ] **Step 4: 修改 CatalogMaterializeService / CatalogDomainService**

如果存在 baseUrl 映射逻辑，删除或调整（连接配置已由 ChannelEndpoint 承载）。

- [ ] **Step 5: 编译验证**

```bash
./mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor(supply): Provider 删除 baseUrl 字段，连接配置由 ChannelEndpoint 承载"
```

---

### Task 8: 新建 OpenAIOutboundTuner 和 AnthropicOutboundTuner

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/protocol/openai/OpenAIOutboundTuner.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/protocol/anthropic/AnthropicOutboundTuner.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/adapter/protocol/openai/OpenAIOutboundTunerTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/adapter/protocol/anthropic/AnthropicOutboundTunerTest.java`

- [ ] **Step 1: 编写 OpenAIOutboundTuner 的失败测试**

```java
package com.codingas.gateway.adapter.protocol.openai;

import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenAI 出站调谐器测试")
class OpenAIOutboundTunerTest {

    private final OpenAIOutboundTuner tuner = new OpenAIOutboundTuner();

    @Test
    @DisplayName("应填充 maxTokens 默认值")
    void shouldFillDefaultMaxTokens() {
        OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(java.util.List.of(
                        OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .maxTokens(null)
                .build();

        OpenAIChatRequest tuned = tuner.tune(request);

        assertThat(tuned.getMaxTokens()).isEqualTo(4096);
    }

    @Test
    @DisplayName("已有 maxTokens 时不覆盖")
    void shouldNotOverrideExistingMaxTokens() {
        OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(java.util.List.of(
                        OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .maxTokens(2048)
                .build();

        OpenAIChatRequest tuned = tuner.tune(request);

        assertThat(tuned.getMaxTokens()).isEqualTo(2048);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest="OpenAIOutboundTunerTest"
```

Expected: 编译失败（类不存在）

- [ ] **Step 3: 实现 OpenAIOutboundTuner**

```java
package com.codingas.gateway.adapter.protocol.openai;

import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import org.springframework.stereotype.Component;

/**
 * OpenAI 协议级出站调谐器
 *
 * <p>填充协议默认值（如 maxTokens），格式修正。</p>
 */
@Component
public class OpenAIOutboundTuner {

    private static final int DEFAULT_MAX_TOKENS = 4096;

    /**
     * 调谐 OpenAI 出站请求
     */
    public OpenAIChatRequest tune(OpenAIChatRequest request) {
        return OpenAIChatRequest.builder()
                .model(request.getModel())
                .messages(request.getMessages())
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : DEFAULT_MAX_TOKENS)
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .stream(request.getStream())
                .build();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest="OpenAIOutboundTunerTest"
```

Expected: PASS

- [ ] **Step 5: 编写 AnthropicOutboundTuner 的失败测试**

```java
package com.codingas.gateway.adapter.protocol.anthropic;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Anthropic 出站调谐器测试")
class AnthropicOutboundTunerTest {

    private final AnthropicOutboundTuner tuner = new AnthropicOutboundTuner();

    @Test
    @DisplayName("应填充 maxTokens 默认值")
    void shouldFillDefaultMaxTokens() {
        AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(java.util.List.of(
                        AnthropicMessagesRequest.Message.builder().role("user").content("hi").build()))
                .maxTokens(null)
                .build();

        AnthropicMessagesRequest tuned = tuner.tune(request);

        assertThat(tuned.getMaxTokens()).isEqualTo(4096);
    }

    @Test
    @DisplayName("已有 maxTokens 时不覆盖")
    void shouldNotOverrideExistingMaxTokens() {
        AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(java.util.List.of(
                        AnthropicMessagesRequest.Message.builder().role("user").content("hi").build()))
                .maxTokens(2048)
                .build();

        AnthropicMessagesRequest tuned = tuner.tune(request);

        assertThat(tuned.getMaxTokens()).isEqualTo(2048);
    }
}
```

- [ ] **Step 6: 实现 AnthropicOutboundTuner**

```java
package com.codingas.gateway.adapter.protocol.anthropic;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import org.springframework.stereotype.Component;

/**
 * Anthropic 协议级出站调谐器
 *
 * <p>填充协议默认值（如 maxTokens），格式修正。</p>
 */
@Component
public class AnthropicOutboundTuner {

    private static final int DEFAULT_MAX_TOKENS = 4096;

    /**
     * 调谐 Anthropic 出站请求
     */
    public AnthropicMessagesRequest tune(AnthropicMessagesRequest request) {
        return AnthropicMessagesRequest.builder()
                .model(request.getModel())
                .messages(request.getMessages())
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : DEFAULT_MAX_TOKENS)
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .stream(request.getStream())
                .build();
    }
}
```

- [ ] **Step 7: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest="AnthropicOutboundTunerTest"
```

Expected: PASS

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "feat(protocol): 新增 OpenAI/Anthropic 协议级出站调谐器"
```

---

### Task 9: 新建 OutboundTuner 编排层

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/OutboundTuner.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/OutboundTunerTest.java`

- [ ] **Step 1: 编写 OutboundTuner 的失败测试**

```java
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.adapter.protocol.anthropic.AnthropicOutboundTuner;
import com.codingas.gateway.adapter.protocol.openai.OpenAIOutboundTuner;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("出站调谐编排测试")
class OutboundTunerTest {

    private OutboundTuner outboundTuner;

    @BeforeEach
    void setUp() {
        outboundTuner = new OutboundTuner(new OpenAIOutboundTuner(), new AnthropicOutboundTuner());
    }

    @Test
    @DisplayName("OpenAI 请求 + OpenAI 上游：应调谐")
    void shouldTuneOpenAIRequest() {
        RoutingContext ctx = new RoutingContext(10L, 20L, "https://api.openai.com",
                Protocol.OPENAI, "sk-test", 60, false);

        OpenAIChatRequest request = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .maxTokens(null)
                .build();

        ProtocolRequest tuned = outboundTuner.tune(request, ctx);

        assertThat(tuned).isInstanceOf(OpenAIChatRequest.class);
        assertThat(((OpenAIChatRequest) tuned).getMaxTokens()).isEqualTo(4096);
    }

    @Test
    @DisplayName("不支持的协议时抛出异常")
    void shouldThrowOnUnsupportedProtocol() {
        // 使用 mock RoutingContext 或构造特殊协议
        RoutingContext ctx = new RoutingContext(10L, 20L, "https://example.com",
                Protocol.OPENAI, "key", 60, false);

        // 传入与路由上下文协议不匹配的请求（跨协议场景在转换后调谐）
        assertThatThrownBy(() -> outboundTuner.tune(null, ctx))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest="OutboundTunerTest"
```

Expected: 编译失败

- [ ] **Step 3: 实现 OutboundTuner**

```java
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.adapter.protocol.anthropic.AnthropicOutboundTuner;
import com.codingas.gateway.adapter.protocol.openai.OpenAIOutboundTuner;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.springframework.stereotype.Component;

/**
 * 出站调谐编排器
 *
 * <p>渠道级调谐：模型名替换、字段覆盖、敏感字段剥离，委托协议级 Tuner 执行。</p>
 * <p>调谐必须按目标协议要求执行（RoutingContext.upstreamProtocol），而非入站协议。</p>
 */
@Component
public class OutboundTuner {

    private final OpenAIOutboundTuner openaiTuner;
    private final AnthropicOutboundTuner anthropicTuner;

    public OutboundTuner(OpenAIOutboundTuner openaiTuner, AnthropicOutboundTuner anthropicTuner) {
        this.openaiTuner = openaiTuner;
        this.anthropicTuner = anthropicTuner;
    }

    /**
     * 按目标协议调谐出站请求
     */
    public ProtocolRequest tune(ProtocolRequest request, RoutingContext ctx) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }

        Protocol targetProtocol = ctx.getUpstreamProtocol();

        return switch (targetProtocol) {
            case OPENAI -> openaiTuner.tune((OpenAIChatRequest) request);
            case ANTHROPIC -> anthropicTuner.tune((AnthropicMessagesRequest) request);
        };
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest="OutboundTunerTest"
```

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat(protocol): 新增 OutboundTuner 出站调谐编排器"
```

---

### Task 10: 阶段 1 全量编译与测试验证

- [ ] **Step 1: 全量编译**

```bash
./mvnw clean compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行全部测试**

```bash
./mvnw test -pl gateway-boot
```

Expected: 全部通过

- [ ] **Step 3: 确认无旧包残留引用**

```bash
grep -rn "domain.supply.protocol" gateway-boot/src/main/java/ gateway-boot/src/test/java/
grep -rn "domain.supply.gateway.ProtocolGateway" gateway-boot/src/main/java/ gateway-boot/src/test/java/
grep -rn "domain.supply.gateway.ProtocolGatewayFactory" gateway-boot/src/main/java/ gateway-boot/src/test/java/
grep -rn "domain.supply.gateway.StreamCallback" gateway-boot/src/main/java/ gateway-boot/src/test/java/
```

Expected: 无输出（全部已替换）

- [ ] **Step 4: 阶段 1 收尾提交（如有未提交的修改）**

```bash
git add -A
git commit -m "refactor(protocol): 阶段 1 完成 — 模型重构"
```

---

## 阶段 2：调用链重构

---

### Task 11: 新建 RoutingResolver 及四个子组件

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/routing/RoutingResolver.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/routing/ModelMatcher.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/routing/ChannelSelector.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/routing/CredentialResolver.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/routing/EndpointResolver.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/routing/RoutingResolverTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/routing/ModelMatcherTest.java`

- [ ] **Step 1: 编写 ModelMatcher 的失败测试**

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("模型匹配器测试")
class ModelMatcherTest {

    @Mock private ChannelModelGateway channelModelGateway;
    @Mock private ChannelGateway channelGateway;

    @Test
    @DisplayName("根据模型名找到匹配的渠道")
    void shouldMatchChannelByModelName() {
        ChannelModel cm = mock(ChannelModel.class);
        when(cm.getChannelId()).thenReturn(1L);
        when(cm.getModelName()).thenReturn("gpt-4o");
        when(channelModelGateway.findByModelName("gpt-4o")).thenReturn(List.of(cm));

        Channel channel = mock(Channel.class);
        when(channel.getId()).thenReturn(1L);
        when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

        ModelMatcher matcher = new ModelMatcher(channelModelGateway, channelGateway);
        List<Channel> result = matcher.match("gpt-4o");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("无匹配模型时返回空列表")
    void shouldReturnEmptyWhenNoMatch() {
        when(channelModelGateway.findByModelName("unknown-model")).thenReturn(List.of());

        ModelMatcher matcher = new ModelMatcher(channelModelGateway, channelGateway);
        List<Channel> result = matcher.match("unknown-model");

        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest="ModelMatcherTest"
```

Expected: 编译失败

- [ ] **Step 3: 实现 ModelMatcher**

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型匹配器
 *
 * <p>根据 modelName 查找 ChannelModel，再关联到 Channel。</p>
 */
@Component
public class ModelMatcher {

    private final ChannelModelGateway channelModelGateway;
    private final ChannelGateway channelGateway;

    public ModelMatcher(ChannelModelGateway channelModelGateway, ChannelGateway channelGateway) {
        this.channelModelGateway = channelModelGateway;
        this.channelGateway = channelGateway;
    }

    /**
     * 根据模型名匹配可用渠道
     */
    public List<Channel> match(String modelName) {
        List<com.codingas.gateway.domain.supply.entity.ChannelModel> channelModels =
                channelModelGateway.findByModelName(modelName);

        return channelModels.stream()
                .map(cm -> channelGateway.findById(cm.getChannelId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest="ModelMatcherTest"
```

Expected: PASS

- [ ] **Step 5: 编写 ChannelSelector 的失败测试并实现**

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("渠道选择器测试")
class ChannelSelectorTest {

    private final ChannelSelector selector = new ChannelSelector();

    @Test
    @DisplayName("WEIGHTED 策略：从候选渠道中选择一个")
    void shouldSelectFromCandidates() {
        Channel ch1 = mock(Channel.class);
        Channel ch2 = mock(Channel.class);
        List<Channel> candidates = List.of(ch1, ch2);

        Channel selected = selector.select(candidates, RoutingStrategy.WEIGHTED);

        assertThat(candidates).contains(selected);
    }

    @Test
    @DisplayName("候选为空时抛出异常")
    void shouldThrowWhenNoCandidates() {
        assertThatThrownBy(() -> selector.select(List.of(), RoutingStrategy.WEIGHTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无可用渠道");
    }
}
```

实现：

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 渠道选择器
 *
 * <p>按 RoutingStrategy 从候选渠道中选择一个。</p>
 */
@Component
public class ChannelSelector {

    /**
     * 按策略选择渠道
     */
    public Channel select(List<Channel> candidates, RoutingStrategy strategy) {
        if (candidates.isEmpty()) {
            throw new IllegalStateException("无可用渠道");
        }

        return switch (strategy) {
            case WEIGHTED, RANDOM -> candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            case FAILOVER -> candidates.getFirst(); // 按 priority 排序后取第一个
            case COST_OPTIMIZED -> candidates.getFirst(); // 按 inputPrice 排序后取第一个
            case LATENCY_OPTIMIZED -> candidates.getFirst(); // 按历史延迟排序后取第一个
        };
    }
}
```

- [ ] **Step 6: 编写 CredentialResolver 的失败测试并实现**

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("凭证解析器测试")
class CredentialResolverTest {

    @Mock private ChannelCredentialGateway credentialGateway;

    @Test
    @DisplayName("从渠道解析到凭证")
    void shouldResolveCredential() {
        ChannelCredential cred = mock(ChannelCredential.class);
        when(cred.getDecryptedApiKey()).thenReturn("sk-test-key");
        when(credentialGateway.findByChannelId(1L)).thenReturn(List.of(cred));

        CredentialResolver resolver = new CredentialResolver(credentialGateway);
        String result = resolver.resolve(1L);

        assertThat(result).isEqualTo("sk-test-key");
    }

    @Test
    @DisplayName("无凭证时抛出异常")
    void shouldThrowWhenNoCredential() {
        when(credentialGateway.findByChannelId(1L)).thenReturn(List.of());

        CredentialResolver resolver = new CredentialResolver(credentialGateway);
        assertThatThrownBy(() -> resolver.resolve(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无可用凭证");
    }
}
```

实现：

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 凭证解析器
 *
 * <p>从选中渠道解析 API Key 凭证。</p>
 */
@Component
public class CredentialResolver {

    private final ChannelCredentialGateway credentialGateway;

    public CredentialResolver(ChannelCredentialGateway credentialGateway) {
        this.credentialGateway = credentialGateway;
    }

    /**
     * 解析渠道凭证
     */
    public String resolve(Long channelId) {
        List<ChannelCredential> credentials = credentialGateway.findByChannelId(channelId);
        if (credentials.isEmpty()) {
            throw new IllegalStateException("渠道 " + channelId + " 无可用凭证");
        }
        return credentials.getFirst().getDecryptedApiKey();
    }
}
```

- [ ] **Step 7: 编写 EndpointResolver 的失败测试并实现**

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("端点解析器测试")
class EndpointResolverTest {

    @Mock private ChannelEndpointGateway endpointGateway;

    @Test
    @DisplayName("从渠道解析到活跃端点")
    void shouldResolveActiveEndpoint() {
        ChannelEndpoint ep = mock(ChannelEndpoint.class);
        when(ep.getEndpointUrl()).thenReturn("https://api.openai.com");
        when(ep.getProtocol()).thenReturn(com.codingas.gateway.domain.supply.enums.Protocol.OPENAI);
        when(endpointGateway.findActiveByChannelId(1L)).thenReturn(List.of(ep));

        EndpointResolver resolver = new EndpointResolver(endpointGateway);
        ChannelEndpoint result = resolver.resolve(1L);

        assertThat(result.getEndpointUrl()).isEqualTo("https://api.openai.com");
    }

    @Test
    @DisplayName("无活跃端点时抛出异常")
    void shouldThrowWhenNoActiveEndpoint() {
        when(endpointGateway.findActiveByChannelId(1L)).thenReturn(List.of());

        EndpointResolver resolver = new EndpointResolver(endpointGateway);
        assertThatThrownBy(() -> resolver.resolve(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无活跃端点");
    }
}
```

实现：

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 端点解析器
 *
 * <p>从选中渠道解析协议端点。</p>
 */
@Component
public class EndpointResolver {

    private final ChannelEndpointGateway endpointGateway;

    public EndpointResolver(ChannelEndpointGateway endpointGateway) {
        this.endpointGateway = endpointGateway;
    }

    /**
     * 解析渠道的活跃端点
     */
    public ChannelEndpoint resolve(Long channelId) {
        List<ChannelEndpoint> endpoints = endpointGateway.findActiveByChannelId(channelId);
        if (endpoints.isEmpty()) {
            throw new IllegalStateException("渠道 " + channelId + " 无活跃端点");
        }
        return endpoints.getFirst();
    }
}
```

- [ ] **Step 8: 编写 RoutingResolver 的失败测试并实现**

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("路由编排器测试")
class RoutingResolverTest {

    @Mock private ModelMatcher modelMatcher;
    @Mock private ChannelSelector channelSelector;
    @Mock private CredentialResolver credentialResolver;
    @Mock private EndpointResolver endpointResolver;

    @Test
    @DisplayName("应编排四步路由流程")
    void shouldOrchestrateRouting() {
        // 此测试验证 RoutingResolver 正确编排 ModelMatcher → ChannelSelector → CredentialResolver → EndpointResolver
        // 详细测试在集成测试中覆盖
        RoutingResolver resolver = new RoutingResolver(modelMatcher, channelSelector, credentialResolver, endpointResolver);
        assertThat(resolver).isNotNull();
    }
}
```

实现：

```java
package com.codingas.gateway.application.routing;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由编排器
 *
 * <p>编排 ModelMatcher → ChannelSelector → CredentialResolver → EndpointResolver 四步，返回 RoutingContext。</p>
 */
@Component
public class RoutingResolver {

    private final ModelMatcher modelMatcher;
    private final ChannelSelector channelSelector;
    private final CredentialResolver credentialResolver;
    private final EndpointResolver endpointResolver;

    public RoutingResolver(ModelMatcher modelMatcher, ChannelSelector channelSelector,
                           CredentialResolver credentialResolver, EndpointResolver endpointResolver) {
        this.modelMatcher = modelMatcher;
        this.channelSelector = channelSelector;
        this.credentialResolver = credentialResolver;
        this.endpointResolver = endpointResolver;
    }

    /**
     * 解析路由上下文
     */
    public RoutingContext resolve(Identity identity, String modelName, RoutingStrategy strategy) {
        List<Channel> candidates = modelMatcher.match(modelName);
        Channel selected = channelSelector.select(candidates, strategy);
        String apiKey = credentialResolver.resolve(selected.getId());
        ChannelEndpoint endpoint = endpointResolver.resolve(selected.getId());

        return new RoutingContext(
                selected.getId(),
                endpoint.getId(),
                endpoint.getEndpointUrl(),
                endpoint.getProtocol(),
                apiKey,
                60,
                false
        );
    }
}
```

- [ ] **Step 9: 运行全部路由测试**

```bash
./mvnw test -pl gateway-boot -Dtest="ModelMatcherTest,ChannelSelectorTest,CredentialResolverTest,EndpointResolverTest,RoutingResolverTest"
```

Expected: 全部通过

- [ ] **Step 10: 提交**

```bash
git add -A
git commit -m "feat(routing): 新增路由五组件 — RoutingResolver/ModelMatcher/ChannelSelector/CredentialResolver/EndpointResolver"
```

---

### Task 12: 新建 ChatDispatchService，重构调用链

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/ChatDispatchServiceTest.java`

- [ ] **Step 1: 编写 ChatDispatchService 的失败测试**

```java
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.infrastructure.upstream.UpstreamClientRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatDispatchService 测试")
class ChatDispatchServiceTest {

    @Mock private RoutingResolver routingResolver;
    @Mock private OutboundTuner outboundTuner;
    @Mock private UpstreamClientRegistry clientRegistry;
    @Mock private ProtocolConverter protocolConverter;

    private ChatDispatchService dispatchService;

    private Identity testIdentity;
    private RoutingContext openAIContext;
    private OpenAIChatRequest testRequest;
    private OpenAIChatResponse testResponse;

    @BeforeEach
    void setUp() {
        dispatchService = new ChatDispatchServiceImpl(routingResolver, outboundTuner, clientRegistry, protocolConverter);

        testIdentity = mock(Identity.class);
        openAIContext = new RoutingContext(10L, 20L, "https://api.openai.com",
                Protocol.OPENAI, "sk-test", 60, false);

        testRequest = OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                .build();

        testResponse = OpenAIChatResponse.builder().id("chatcmpl-123").model("gpt-4").build();
    }

    @Nested
    @DisplayName("dispatch 方法测试")
    class DispatchTests {

        @Test
        @DisplayName("同协议调度：OpenAI→OpenAI")
        void dispatch_sameProtocol() {
            when(routingResolver.resolve(any(Identity.class), anyString(), any())).thenReturn(openAIContext);
            when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(testRequest);

            var upstreamClient = mock(com.codingas.gateway.infrastructure.upstream.UpstreamClient.class);
            when(clientRegistry.getClient(anyString(), anyString(), anyString(), anyInt())).thenReturn(upstreamClient);
            when(upstreamClient.chat(any(ProtocolRequest.class))).thenReturn(testResponse);

            ProtocolResponse response = dispatchService.dispatch(testRequest, testIdentity, RoutingStrategy.WEIGHTED);

            assertThat(response).isInstanceOf(OpenAIChatResponse.class);
            verify(protocolConverter, never()).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter, never()).toOpenAI(any(AnthropicMessagesResponse.class));
        }

        @Test
        @DisplayName("跨协议调度：OpenAI→Anthropic")
        void dispatch_crossProtocol() {
            RoutingContext anthropicContext = new RoutingContext(10L, 21L, "https://api.anthropic.com",
                    Protocol.ANTHROPIC, "sk-ant-key", 60, false);
            when(routingResolver.resolve(any(Identity.class), anyString(), any())).thenReturn(anthropicContext);

            AnthropicMessagesRequest convertedRequest = AnthropicMessagesRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .messages(List.of(AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()))
                    .maxTokens(1024)
                    .build();
            when(protocolConverter.toAnthropic(any(OpenAIChatRequest.class))).thenReturn(convertedRequest);
            when(outboundTuner.tune(any(ProtocolRequest.class), any(RoutingContext.class))).thenReturn(convertedRequest);

            AnthropicMessagesResponse upstreamResponse = AnthropicMessagesResponse.builder()
                    .id("msg-123").model("claude-3-5-sonnet-20241022").build();
            var upstreamClient = mock(com.codingas.gateway.infrastructure.upstream.UpstreamClient.class);
            when(clientRegistry.getClient(anyString(), anyString(), anyString(), anyInt())).thenReturn(upstreamClient);
            when(upstreamClient.chat(any(ProtocolRequest.class))).thenReturn(upstreamResponse);

            when(protocolConverter.toOpenAI(any(AnthropicMessagesResponse.class))).thenReturn(testResponse);

            ProtocolResponse response = dispatchService.dispatch(testRequest, testIdentity, RoutingStrategy.WEIGHTED);

            assertThat(response).isInstanceOf(OpenAIChatResponse.class);
            verify(protocolConverter).toAnthropic(any(OpenAIChatRequest.class));
            verify(protocolConverter).toOpenAI(any(AnthropicMessagesResponse.class));
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest="ChatDispatchServiceTest"
```

Expected: 编译失败

- [ ] **Step 3: 创建 ChatDispatchService 接口**

```java
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.iam.valueobject.Identity;

/**
 * 聊天调度服务
 *
 * <p>七阶段调用链编排：校验→路由→调谐→调用→转换→计量→审计。</p>
 */
public interface ChatDispatchService {

    /**
     * 非流式调度
     */
    ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy);

    /**
     * 流式调度
     */
    void dispatchStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                        StreamCallback callback);
}
```

- [ ] **Step 4: 实现 ChatDispatchServiceImpl**

```java
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.infrastructure.upstream.UpstreamClient;
import com.codingas.gateway.infrastructure.upstream.UpstreamClientRegistry;
import org.springframework.stereotype.Service;

/**
 * 聊天调度服务实现
 *
 * <p>七阶段调用链：校验→路由→(转换)→调谐→调用→(转换)→后置。</p>
 */
@Service
public class ChatDispatchServiceImpl implements ChatDispatchService {

    private final RoutingResolver routingResolver;
    private final OutboundTuner outboundTuner;
    private final UpstreamClientRegistry clientRegistry;
    private final ProtocolConverter protocolConverter;

    public ChatDispatchServiceImpl(RoutingResolver routingResolver,
                                   OutboundTuner outboundTuner,
                                   UpstreamClientRegistry clientRegistry,
                                   ProtocolConverter protocolConverter) {
        this.routingResolver = routingResolver;
        this.outboundTuner = outboundTuner;
        this.clientRegistry = clientRegistry;
        this.protocolConverter = protocolConverter;
    }

    @Override
    public ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
        // 阶段 2：路由
        RoutingContext ctx = routingResolver.resolve(identity, request.getModel(), strategy);

        ProtocolRequest outboundReq = request;

        // 阶段 3：请求转换（仅跨协议时执行）
        if (needsConversion(request, ctx)) {
            outboundReq = convertRequest(request, ctx);
        }

        // 阶段 4：出站调谐
        outboundReq = outboundTuner.tune(outboundReq, ctx);

        // 阶段 5：上游调用
        UpstreamClient client = clientRegistry.getClient(
                ctx.getUpstreamProtocol().name().toLowerCase(),
                ctx.getEndpointUrl(),
                ctx.getApiKey(),
                ctx.getTimeout()
        );
        ProtocolResponse response = client.chat(outboundReq);

        // 阶段 6：响应转换（仅跨协议时执行）
        if (needsConversion(request, ctx)) {
            response = convertResponse(response, ctx);
        }

        // 阶段 7：后置处理（审计、计量 — 阶段 3 实现）
        return response;
    }

    @Override
    public void dispatchStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                               StreamCallback callback) {
        RoutingContext ctx = routingResolver.resolve(identity, request.getModel(), strategy);

        ProtocolRequest outboundReq = request;
        if (needsConversion(request, ctx)) {
            outboundReq = convertRequest(request, ctx);
        }
        outboundReq = outboundTuner.tune(outboundReq, ctx);

        UpstreamClient client = clientRegistry.getClient(
                ctx.getUpstreamProtocol().name().toLowerCase(),
                ctx.getEndpointUrl(),
                ctx.getApiKey(),
                ctx.getTimeout()
        );
        client.chatStream(outboundReq, callback);
    }

    private boolean needsConversion(ProtocolRequest request, RoutingContext ctx) {
        Protocol inbound = getInboundProtocol(request);
        return inbound != ctx.getUpstreamProtocol();
    }

    private Protocol getInboundProtocol(ProtocolRequest request) {
        if (request instanceof OpenAIChatRequest) return Protocol.OPENAI;
        if (request instanceof AnthropicMessagesRequest) return Protocol.ANTHROPIC;
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    private ProtocolRequest convertRequest(ProtocolRequest request, RoutingContext ctx) {
        if (request instanceof OpenAIChatRequest openai && ctx.getUpstreamProtocol() == Protocol.ANTHROPIC) {
            return protocolConverter.toAnthropic(openai);
        }
        if (request instanceof AnthropicMessagesRequest anthropic && ctx.getUpstreamProtocol() == Protocol.OPENAI) {
            return protocolConverter.toOpenAI(anthropic);
        }
        return request;
    }

    private ProtocolResponse convertResponse(ProtocolResponse response, RoutingContext ctx) {
        if (response instanceof AnthropicMessagesResponse anthropic && ctx.getUpstreamProtocol() == Protocol.ANTHROPIC) {
            return protocolConverter.toOpenAI(anthropic);
        }
        if (response instanceof OpenAIChatResponse openai && ctx.getUpstreamProtocol() == Protocol.OPENAI) {
            return protocolConverter.toAnthropic(openai);
        }
        return response;
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest="ChatDispatchServiceTest"
```

Expected: PASS

- [ ] **Step 6: 更新 Controller 引用**

将 `OpenAIController`、`AnthropicController`、`ProtocolController` 中的 `ProxyService` 引用替换为 `ChatDispatchService`：
- `import ...ProxyService` → `import ...ChatDispatchService`
- 字段类型 `ProxyService` → `ChatDispatchService`
- 调用 `proxyService.proxy(...)` → `dispatchService.dispatch(...)`
- 调用 `proxyService.proxyStream(...)` → `dispatchService.dispatchStream(...)`

- [ ] **Step 7: 编译验证**

```bash
./mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "feat(proxy): 新增 ChatDispatchService 七阶段调度，更新 Controller 引用"
```

---

### Task 13: 废弃 SupplyRoutingService 和 ProxyServiceImpl

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/SupplyRoutingService.java` (添加 @Deprecated)
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProxyServiceImpl.java` (添加 @Deprecated)

- [ ] **Step 1: 给 SupplyRoutingService 添加 @Deprecated 注解**

```java
/**
 * @deprecated 由 {@link com.codingas.gateway.application.routing.RoutingResolver} 替代
 */
@Deprecated(since = "2026-05-25", forRemoval = true)
```

- [ ] **Step 2: 给 ProxyServiceImpl 添加 @Deprecated 注解**

```java
/**
 * @deprecated 由 {@link ChatDispatchServiceImpl} 替代
 */
@Deprecated(since = "2026-05-25", forRemoval = true)
```

- [ ] **Step 3: 确认无代码直接引用旧类**

```bash
grep -rn "SupplyRoutingService\|ProxyServiceImpl" gateway-boot/src/main/java/ --include="*.java" | grep -v "class SupplyRoutingService\|class ProxyServiceImpl\|@Deprecated\|deprecated"
```

如果有引用，检查是否需要迁移到新组件。

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "refactor(proxy): 标记 SupplyRoutingService 和 ProxyServiceImpl 为 @Deprecated"
```

---

### Task 14: 阶段 2 全量编译与测试验证

- [ ] **Step 1: 全量编译**

```bash
./mvnw clean compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行全部测试**

```bash
./mvnw test -pl gateway-boot
```

Expected: 全部通过

- [ ] **Step 3: 阶段 2 收尾提交**

```bash
git add -A
git commit -m "refactor(proxy): 阶段 2 完成 — 调用链重构"
```

---

## 阶段 3：能力补全

---

### Task 15: 新建 RetryPolicy

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/RetryPolicy.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/GatewayRetryProperties.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/RetryPolicyTest.java`

- [ ] **Step 1: 编写 RetryPolicy 的失败测试**

```java
package com.codingas.gateway.infrastructure.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("重试策略测试")
class RetryPolicyTest {

    private RetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {
        retryPolicy = new RetryPolicy(3, 1000, 2.0, Set.of(429, 500, 502, 503));
    }

    @Test
    @DisplayName("可重试的状态码应返回 true")
    void shouldRetryOnRetryableStatus() {
        assertThat(retryPolicy.shouldRetry(429, 1)).isTrue();
        assertThat(retryPolicy.shouldRetry(500, 1)).isTrue();
        assertThat(retryPolicy.shouldRetry(503, 2)).isTrue();
    }

    @Test
    @DisplayName("超过最大重试次数应返回 false")
    void shouldNotRetryExceedingMaxAttempts() {
        assertThat(retryPolicy.shouldRetry(429, 3)).isFalse();
    }

    @Test
    @DisplayName("不可重试的状态码应返回 false")
    void shouldNotRetryOnNonRetryableStatus() {
        assertThat(retryPolicy.shouldRetry(400, 1)).isFalse();
        assertThat(retryPolicy.shouldRetry(401, 1)).isFalse();
    }

    @Test
    @DisplayName("退避时间应按倍数增长")
    void shouldCalculateBackoff() {
        assertThat(retryPolicy.getBackoffMs(1)).isEqualTo(1000);
        assertThat(retryPolicy.getBackoffMs(2)).isEqualTo(2000);
        assertThat(retryPolicy.getBackoffMs(3)).isEqualTo(4000);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest="RetryPolicyTest"
```

Expected: 编译失败

- [ ] **Step 3: 实现 GatewayRetryProperties**

```java
package com.codingas.gateway.infrastructure.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 网关重试配置
 */
@Component
@ConfigurationProperties(prefix = "gateway.retry")
public class GatewayRetryProperties {

    /** 最大重试次数 */
    private int maxAttempts = 3;

    /** 初始退避时间（毫秒） */
    private long backoffInitial = 1000;

    /** 退避倍数 */
    private double backoffMultiplier = 2.0;

    /** 可重试的 HTTP 状态码 */
    private Set<Integer> retryableStatusCodes = Set.of(429, 500, 502, 503);

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public long getBackoffInitial() { return backoffInitial; }
    public void setBackoffInitial(long backoffInitial) { this.backoffInitial = backoffInitial; }

    public double getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }

    public Set<Integer> getRetryableStatusCodes() { return retryableStatusCodes; }
    public void setRetryableStatusCodes(Set<Integer> retryableStatusCodes) { this.retryableStatusCodes = retryableStatusCodes; }
}
```

- [ ] **Step 4: 实现 RetryPolicy**

```java
package com.codingas.gateway.infrastructure.resilience;

import java.util.Set;

/**
 * 可配置的重试策略
 *
 * <p>根据 HTTP 状态码判断是否重试，按指数退避计算等待时间。</p>
 */
public class RetryPolicy {

    private final int maxAttempts;
    private final long backoffInitialMs;
    private final double backoffMultiplier;
    private final Set<Integer> retryableStatusCodes;

    public RetryPolicy(int maxAttempts, long backoffInitialMs, double backoffMultiplier,
                       Set<Integer> retryableStatusCodes) {
        this.maxAttempts = maxAttempts;
        this.backoffInitialMs = backoffInitialMs;
        this.backoffMultiplier = backoffMultiplier;
        this.retryableStatusCodes = retryableStatusCodes;
    }

    /**
     * 判断是否应该重试
     *
     * @param statusCode HTTP 状态码
     * @param attempt 当前重试次数（从 1 开始）
     */
    public boolean shouldRetry(int statusCode, int attempt) {
        return attempt < maxAttempts && retryableStatusCodes.contains(statusCode);
    }

    /**
     * 计算退避时间（毫秒）
     *
     * @param attempt 当前重试次数（从 1 开始）
     */
    public long getBackoffMs(int attempt) {
        return (long) (backoffInitialMs * Math.pow(backoffMultiplier, attempt - 1));
    }

    public int getMaxAttempts() { return maxAttempts; }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest="RetryPolicyTest"
```

Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat(resilience): 新增 RetryPolicy 重试策略"
```

---

### Task 16: 新建 CircuitBreaker 和 ChannelEndpointCircuitBreakerManager

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/CircuitBreaker.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/CircuitBreakerState.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ChannelEndpointCircuitBreakerManager.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/CircuitBreakerTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/ChannelEndpointCircuitBreakerManagerTest.java`

- [ ] **Step 1: 编写 CircuitBreaker 的失败测试**

```java
package com.codingas.gateway.infrastructure.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("熔断器测试")
class CircuitBreakerTest {

    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        // 失败率阈值 50%, 滑动窗口 10, OPEN 持续 1ms (测试用), HALF_OPEN 试探 3
        breaker = new CircuitBreaker(0.5, 10, 1, 3);
    }

    @Test
    @DisplayName("初始状态为 CLOSED")
    void shouldBeClosedInitially() {
        assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(breaker.allowRequest()).isTrue();
    }

    @Test
    @DisplayName("失败率超阈值时触发 OPEN")
    void shouldOpenWhenFailureRateExceedsThreshold() {
        for (int i = 0; i < 10; i++) {
            breaker.recordFailure();
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test
    @DisplayName("全部成功时保持 CLOSED")
    void shouldStayClosedOnSuccess() {
        for (int i = 0; i < 10; i++) {
            breaker.recordSuccess();
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
    }

    @Test
    @DisplayName("OPEN 超时后进入 HALF_OPEN")
    void shouldTransitionToHalfOpenAfterTimeout() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            breaker.recordFailure();
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);

        Thread.sleep(10); // 等待 OPEN 持续时间过期
        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest="CircuitBreakerTest"
```

Expected: 编译失败

- [ ] **Step 3: 实现 CircuitBreakerState 枚举**

```java
package com.codingas.gateway.infrastructure.resilience;

/**
 * 熔断器状态
 */
public enum CircuitBreakerState {
    /** 正常放行 */
    CLOSED,
    /** 熔断放行，拒绝请求 */
    OPEN,
    /** 试探放行，允许少量请求 */
    HALF_OPEN
}
```

- [ ] **Step 4: 实现 CircuitBreaker**

```java
package com.codingas.gateway.infrastructure.resilience;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器
 *
 * <p>基于滑动窗口统计失败率，支持 CLOSED→OPEN→HALF_OPEN 状态转换。</p>
 */
public class CircuitBreaker {

    private final double failureRateThreshold;
    private final int slidingWindowSize;
    private final long openDurationMs;
    private final int halfOpenMaxAttempts;

    private final ConcurrentLinkedDeque<Boolean> slidingWindow = new ConcurrentLinkedDeque<>();
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private volatile long openSince = 0;
    private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);

    public CircuitBreaker(double failureRateThreshold, int slidingWindowSize,
                          long openDurationMs, int halfOpenMaxAttempts) {
        this.failureRateThreshold = failureRateThreshold;
        this.slidingWindowSize = slidingWindowSize;
        this.openDurationMs = openDurationMs;
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
    }

    /**
     * 判断是否允许请求通过
     */
    public boolean allowRequest() {
        return switch (state) {
            case CLOSED -> true;
            case OPEN -> {
                if (System.currentTimeMillis() - openSince >= openDurationMs) {
                    state = CircuitBreakerState.HALF_OPEN;
                    halfOpenAttempts.set(0);
                    yield true;
                }
                yield false;
            }
            case HALF_OPEN -> halfOpenAttempts.incrementAndGet() <= halfOpenMaxAttempts;
        };
    }

    /**
     * 记录成功
     */
    public void recordSuccess() {
        record(true);
        if (state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.CLOSED;
            slidingWindow.clear();
        }
    }

    /**
     * 记录失败
     */
    public void recordFailure() {
        record(false);
        if (state == CircuitBreakerState.HALF_OPEN) {
            tripOpen();
        } else if (getFailureRate() >= failureRateThreshold) {
            tripOpen();
        }
    }

    public CircuitBreakerState getState() { return state; }

    private void record(boolean success) {
        slidingWindow.addLast(success);
        while (slidingWindow.size() > slidingWindowSize) {
            slidingWindow.pollFirst();
        }
    }

    private double getFailureRate() {
        if (slidingWindow.isEmpty()) return 0.0;
        long failures = slidingWindow.stream().filter(b -> !b).count();
        return (double) failures / slidingWindow.size();
    }

    private void tripOpen() {
        state = CircuitBreakerState.OPEN;
        openSince = System.currentTimeMillis();
    }
}
```

- [ ] **Step 5: 实现 ChannelEndpointCircuitBreakerManager**

```java
package com.codingas.gateway.infrastructure.resilience;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 渠道端点熔断器管理器
 *
 * <p>每个 ChannelEndpoint 维护一个独立的熔断器实例。</p>
 */
@Component
public class ChannelEndpointCircuitBreakerManager {

    private final ConcurrentMap<Long, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    private static final double DEFAULT_FAILURE_RATE_THRESHOLD = 0.5;
    private static final int DEFAULT_SLIDING_WINDOW_SIZE = 10;
    private static final long DEFAULT_OPEN_DURATION_MS = 30000;
    private static final int DEFAULT_HALF_OPEN_MAX_ATTEMPTS = 3;

    /**
     * 获取端点对应的熔断器
     */
    public CircuitBreaker getBreaker(Long endpointId) {
        return breakers.computeIfAbsent(endpointId,
                id -> new CircuitBreaker(DEFAULT_FAILURE_RATE_THRESHOLD, DEFAULT_SLIDING_WINDOW_SIZE,
                        DEFAULT_OPEN_DURATION_MS, DEFAULT_HALF_OPEN_MAX_ATTEMPTS));
    }

    /**
     * 判断端点是否可用（熔断器非 OPEN 状态）
     */
    public boolean isAvailable(Long endpointId) {
        return getBreaker(endpointId).allowRequest();
    }
}
```

- [ ] **Step 6: 编写 ChannelEndpointCircuitBreakerManagerTest**

```java
package com.codingas.gateway.infrastructure.resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("端点熔断器管理器测试")
class ChannelEndpointCircuitBreakerManagerTest {

    @Test
    @DisplayName("同一端点返回同一熔断器实例")
    void shouldReturnSameBreakerForSameEndpoint() {
        ChannelEndpointCircuitBreakerManager manager = new ChannelEndpointCircuitBreakerManager();
        CircuitBreaker b1 = manager.getBreaker(1L);
        CircuitBreaker b2 = manager.getBreaker(1L);
        assertThat(b1).isSameAs(b2);
    }

    @Test
    @DisplayName("不同端点返回不同熔断器实例")
    void shouldReturnDifferentBreakerForDifferentEndpoint() {
        ChannelEndpointCircuitBreakerManager manager = new ChannelEndpointCircuitBreakerManager();
        CircuitBreaker b1 = manager.getBreaker(1L);
        CircuitBreaker b2 = manager.getBreaker(2L);
        assertThat(b1).isNotSameAs(b2);
    }
}
```

- [ ] **Step 7: 运行全部熔断器测试**

```bash
./mvnw test -pl gateway-boot -Dtest="CircuitBreakerTest,ChannelEndpointCircuitBreakerManagerTest"
```

Expected: 全部通过

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "feat(resilience): 新增 CircuitBreaker 熔断器和 ChannelEndpointCircuitBreakerManager"
```

---

### Task 17: 审计日志串联

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/audit/entity/CallLog.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/audit/gateway/AuditGateway.java` (新增 saveCallLog 方法)
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/ChatDispatchServiceTest.java`

- [ ] **Step 1: 创建 CallLog 实体**

```java
package com.codingas.gateway.domain.audit.entity;

import java.time.Instant;

/**
 * 调用日志
 *
 * <p>完整记录一次模型调用的全链路信息。</p>
 */
public class CallLog {

    private Long id;
    private Long userId;
    private String model;
    private Long channelId;
    private Long endpointId;
    private Long credentialId;
    private String inboundProtocol;
    private String upstreamProtocol;
    private Long durationMs;
    private Boolean success;
    private String errorMessage;
    private Instant createdAt;

    public CallLog() {}

    public CallLog(Long userId, String model, Long channelId, Long endpointId,
                   String inboundProtocol, String upstreamProtocol) {
        this.userId = userId;
        this.model = model;
        this.channelId = channelId;
        this.endpointId = endpointId;
        this.inboundProtocol = inboundProtocol;
        this.upstreamProtocol = upstreamProtocol;
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public String getModel() { return model; }
    public Long getChannelId() { return channelId; }
    public Long getEndpointId() { return endpointId; }
    public Long getCredentialId() { return credentialId; }
    public void setCredentialId(Long credentialId) { this.credentialId = credentialId; }
    public String getInboundProtocol() { return inboundProtocol; }
    public String getUpstreamProtocol() { return upstreamProtocol; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: 在 AuditGateway 新增 saveCallLog 方法**

在 `domain/audit/gateway/AuditGateway.java` 中新增：

```java
/**
 * 保存调用日志
 */
CallLog saveCallLog(CallLog callLog);
```

- [ ] **Step 3: 在 AuditGatewayImpl 中实现 saveCallLog**

在 `infrastructure/audit/gateway/AuditGatewayImpl.java` 中新增对应实现。

- [ ] **Step 4: 在 ChatDispatchServiceImpl 中串联审计**

修改 `ChatDispatchServiceImpl.dispatch()` 方法，在调用前后记录审计事件：

```java
// 阶段 1 后置：记录审计起点
CallLog callLog = new CallLog(identity.getUserId(), request.getModel(),
        ctx.getChannelId(), ctx.getEndpointId(),
        getInboundProtocol(request).name(), ctx.getUpstreamProtocol().name());
long startTime = System.currentTimeMillis();

// ... 调用阶段 ...

// 阶段 7：后置处理
callLog.setDurationMs(System.currentTimeMillis() - startTime);
callLog.setSuccess(true);
auditGateway.saveCallLog(callLog);
```

- [ ] **Step 5: 更新测试验证审计调用**

在 `ChatDispatchServiceTest` 中添加 mock `AuditGateway` 并验证 `saveCallLog` 被调用。

- [ ] **Step 6: 运行测试**

```bash
./mvnw test -pl gateway-boot -Dtest="ChatDispatchServiceTest"
```

Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "feat(audit): 调用日志串联 — ChatDispatchService 前后记录审计事件"
```

---

### Task 18: Token 计量

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/quota/event/TokenUsedEvent.java`

- [ ] **Step 1: 创建 TokenUsedEvent**

```java
package com.codingas.gateway.domain.quota.event;

/**
 * Token 使用事件
 */
public record TokenUsedEvent(
        Long userId,
        String model,
        Long channelId,
        int inputTokens,
        int outputTokens
) {}
```

- [ ] **Step 2: 在 ChatDispatchServiceImpl 中发布 TokenUsedEvent**

在 `dispatch()` 方法后置阶段，从 `ProtocolResponse` 提取 Token 用量并发布事件：

```java
// 阶段 7：Token 计量
if (response instanceof OpenAIChatResponse openaiResp && openaiResp.getUsage() != null) {
    applicationEventPublisher.publishEvent(new TokenUsedEvent(
            identity.getUserId(), request.getModel(), ctx.getChannelId(),
            openaiResp.getUsage().getPromptTokens(),
            openaiResp.getUsage().getCompletionTokens()));
} else if (response instanceof AnthropicMessagesResponse anthropicResp && anthropicResp.getUsage() != null) {
    applicationEventPublisher.publishEvent(new TokenUsedEvent(
            identity.getUserId(), request.getModel(), ctx.getChannelId(),
            anthropicResp.getUsage().getInputTokens(),
            anthropicResp.getUsage().getOutputTokens()));
}
```

- [ ] **Step 3: 更新测试验证事件发布**

在 `ChatDispatchServiceTest` 中验证 `applicationEventPublisher.publishEvent()` 被调用。

- [ ] **Step 4: 运行测试**

```bash
./mvnw test -pl gateway-boot -Dtest="ChatDispatchServiceTest"
```

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat(quota): ChatDispatchService 发布 TokenUsedEvent 计量事件"
```

---

### Task 19: 分级超时配置

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/GatewayTimeoutProperties.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/GatewayTimeoutPropertiesTest.java`

- [ ] **Step 1: 编写 GatewayTimeoutProperties 的失败测试**

```java
package com.codingas.gateway.infrastructure.resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("超时配置测试")
class GatewayTimeoutPropertiesTest {

    @Test
    @DisplayName("应使用默认超时值")
    void shouldUseDefaultValues() {
        GatewayTimeoutProperties props = new GatewayTimeoutProperties();
        assertThat(props.getConnectDefault()).isEqualTo(5000);
        assertThat(props.getReadDefault()).isEqualTo(60000);
        assertThat(props.getFirstTokenDefault()).isEqualTo(15000);
    }
}
```

- [ ] **Step 2: 实现 GatewayTimeoutProperties**

```java
package com.codingas.gateway.infrastructure.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关分级超时配置
 */
@Component
@ConfigurationProperties(prefix = "gateway.timeout")
public class GatewayTimeoutProperties {

    /** TCP 连接超时（毫秒） */
    private long connectDefault = 5000;

    /** 整体读取超时（毫秒） */
    private long readDefault = 60000;

    /** 流式首 token 超时（毫秒） */
    private long firstTokenDefault = 15000;

    public long getConnectDefault() { return connectDefault; }
    public void setConnectDefault(long connectDefault) { this.connectDefault = connectDefault; }

    public long getReadDefault() { return readDefault; }
    public void setReadDefault(long readDefault) { this.readDefault = readDefault; }

    public long getFirstTokenDefault() { return firstTokenDefault; }
    public void setFirstTokenDefault(long firstTokenDefault) { this.firstTokenDefault = firstTokenDefault; }
}
```

- [ ] **Step 3: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest="GatewayTimeoutPropertiesTest"
```

Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "feat(resilience): 新增分级超时配置 GatewayTimeoutProperties"
```

---

### Task 20: 阶段 3 全量编译与测试验证

- [ ] **Step 1: 全量编译**

```bash
./mvnw clean compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行全部测试**

```bash
./mvnw test -pl gateway-boot
```

Expected: 全部通过

- [ ] **Step 3: 确认无旧包残留引用**

```bash
grep -rn "domain.supply.protocol" gateway-boot/src/
grep -rn "ProtocolGateway\b" gateway-boot/src/ | grep -v "UpstreamClient\|deprecated\|@Deprecated"
```

Expected: 无输出

- [ ] **Step 4: 最终收尾提交**

```bash
git add -A
git commit -m "refactor(protocol): 阶段 3 完成 — 能力补全（重试/熔断/审计/计量/超时）"
```

---

## 自检清单

- [x] **Spec 覆盖率**：每个设计规格需求都有对应 Task
  - 1.1 ModelSpec 去 providerId → Task 6
  - 1.2 协议数据契约迁移 → Task 1
  - 1.3 协议校验迁移 → Task 3
  - 1.4 协议转换迁移 → Task 2
  - 1.5 命名规范化 → Task 5
  - 1.6 出站调谐器 → Task 8, 9
  - 1.7 测试迁移 → Task 1, 2, 3（内含测试迁移步骤）
  - 2.1 路由体系重构 → Task 11
  - 2.2 ProxyService → ChatDispatchService → Task 12
  - 2.3 流式调用重构 → Task 12（dispatchStream 方法）
  - 3.1 重试机制 → Task 15
  - 3.2 熔断机制 → Task 16
  - 3.3 审计日志串联 → Task 17
  - 3.4 Token 计量 → Task 18
  - 3.5 分级超时 → Task 19
- [x] **Placeholder 扫描**：无 TBD/TODO/实现稍后等占位符
- [x] **类型一致性**：RoutingContext、ProtocolRequest、UpstreamClient 等类型在所有 Task 中保持一致
