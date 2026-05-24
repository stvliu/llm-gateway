# 供给域重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将分散在 model/product/proxy/metadata 四个子域的供给体系，合并为统一的 `domain/supply` 供给域，并对齐业界命名惯例。

**Architecture:** 采用逐层迁移策略：先创建新包结构和枚举（无编译依赖风险），再迁移实体，再迁移 Gateway 接口和 Domain Service，最后迁移基础设施实现和应用层。每步保持编译通过，每 Task 产出一个可提交的增量。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, PostgreSQL

---

## 文件变更总览

### 新建文件

| 文件 | 职责 |
|------|------|
| `domain/supply/enums/BillingMode.java` | 计费模式枚举（替代 ProductType） |
| `domain/supply/enums/ChannelState.java` | 渠道状态枚举（替代 ProductState） |
| `domain/supply/enums/CredentialState.java` | 凭证状态枚举（替代 ProductApiKeyState） |
| `domain/supply/enums/ChannelModelState.java` | 渠道模型状态枚举（新增） |
| `domain/supply/enums/ModelSpecState.java` | 模型规格状态枚举（替代 ModelState） |
| `domain/supply/enums/ProviderState.java` | 供应商状态枚举（从 model 迁移） |
| `domain/supply/enums/Protocol.java` | 协议枚举（从 metadata 迁移） |
| `domain/supply/enums/ProviderErrorType.java` | 供应商错误类型（从 proxy 迁移） |
| `domain/supply/enums/RoutingStrategy.java` | 路由策略（从 proxy 迁移） |
| `domain/supply/entity/Provider.java` | 供应商实体（增加 code 字段） |
| `domain/supply/entity/Channel.java` | 渠道实体（替代 Product） |
| `domain/supply/entity/ChannelCredential.java` | 凭证实体（替代 ProductApiKey） |
| `domain/supply/entity/ChannelModel.java` | 渠道模型关联实体（替代 ProductModel，增加定价字段） |
| `domain/supply/entity/ModelSpec.java` | 模型规格实体（从 Model 拆分规格部分） |
| `domain/supply/valueobject/RoutingContext.java` | 路由上下文值对象（从 proxy 迁移） |
| `domain/supply/valueobject/ConnectivityTestResultVO.java` | 连通性测试结果值对象 |
| `domain/supply/gateway/ProviderGateway.java` | 供应商持久化接口 |
| `domain/supply/gateway/ChannelGateway.java` | 渠道持久化接口 |
| `domain/supply/gateway/ChannelCredentialGateway.java` | 凭证持久化接口 |
| `domain/supply/gateway/ModelSpecGateway.java` | 模型规格持久化接口 |
| `domain/supply/gateway/ConnectivityTester.java` | 连通性测试接口 |
| `domain/supply/gateway/ProtocolGateway.java` | 协议网关接口（从 proxy 迁移） |
| `domain/supply/gateway/ProtocolGatewayFactory.java` | 协议网关工厂接口（从 proxy 迁移） |
| `domain/supply/gateway/StreamCallback.java` | 流式回调接口（从 proxy 迁移） |
| `domain/supply/protocol/ProtocolConverter.java` | 协议转换器（从 proxy 迁移） |
| `domain/supply/protocol/ProtocolValidator.java` | 协议校验器接口（从 proxy 迁移） |
| `domain/supply/protocol/ProtocolRequest.java` | 协议请求基类（从 proxy 迁移） |
| `domain/supply/protocol/ProtocolResponse.java` | 协议响应基类（从 proxy 迁移） |
| `domain/supply/protocol/StreamChunkResult.java` | 流式块结果（从 proxy 迁移） |
| `domain/supply/protocol/OpenAIChatRequest.java` | OpenAI 请求（从 proxy 迁移） |
| `domain/supply/protocol/OpenAIChatResponse.java` | OpenAI 响应（从 proxy 迁移） |
| `domain/supply/protocol/AnthropicMessagesRequest.java` | Anthropic 请求（从 proxy 迁移） |
| `domain/supply/protocol/AnthropicMessagesResponse.java` | Anthropic 响应（从 proxy 迁移） |
| `domain/supply/protocol/OpenAIProtocolValidator.java` | OpenAI 协议校验器（从 proxy 迁移） |
| `domain/supply/protocol/AnthropicProtocolValidator.java` | Anthropic 协议校验器（从 proxy 迁移） |
| `domain/supply/service/ProviderDomainService.java` | 供应商领域服务 |
| `domain/supply/service/ChannelDomainService.java` | 渠道领域服务 |
| `domain/supply/service/ChannelCredentialDomainService.java` | 凭证领域服务 |
| `domain/supply/service/ModelSpecDomainService.java` | 模型规格领域服务 |
| `domain/supply/catalog/entity/ProviderCatalog.java` | 供应商目录实体 |
| `domain/supply/catalog/entity/ModelCatalog.java` | 模型目录实体 |
| `domain/supply/catalog/entity/ChannelCatalog.java` | 渠道目录实体 |
| `domain/supply/catalog/entity/ChannelModelCatalog.java` | 渠道模型目录实体 |
| `domain/supply/catalog/gateway/ProviderCatalogGateway.java` | 供应商目录持久化接口 |
| `domain/supply/catalog/gateway/ModelCatalogGateway.java` | 模型目录持久化接口 |
| `domain/supply/catalog/service/CatalogDomainService.java` | 目录领域服务 |
| `domain/supply/catalog/enums/MetadataSource.java` | 元数据来源枚举 |
| `domain/supply/catalog/enums/CatalogState.java` | 目录状态枚举 |
| `domain/supply/exception/ProviderException.java` | 供应商异常 |
| `domain/supply/exception/ChannelException.java` | 渠道异常 |
| `domain/supply/exception/ProtocolValidationException.java` | 协议校验异常 |
| `infrastructure/gateway/protocol/OpenAIProtocolGateway.java` | OpenAI 协议实现（从 infrastructure/gateway/llm 迁移） |
| `infrastructure/gateway/protocol/AnthropicProtocolGateway.java` | Anthropic 协议实现 |
| `infrastructure/gateway/protocol/ProtocolGatewayFactoryImpl.java` | 协议工厂实现 |
| `infrastructure/gateway/ProviderGatewayImpl.java` | 供应商持久化实现 |
| `infrastructure/gateway/ChannelGatewayImpl.java` | 渠道持久化实现 |
| `infrastructure/gateway/ChannelCredentialGatewayImpl.java` | 凭证持久化实现 |
| `infrastructure/gateway/ModelSpecGatewayImpl.java` | 模型规格持久化实现 |
| `infrastructure/gateway/ConnectivityTesterImpl.java` | 连通性测试实现 |
| `infrastructure/gateway/ProviderCatalogGatewayImpl.java` | 供应商目录持久化实现 |
| `infrastructure/gateway/ModelCatalogGatewayImpl.java` | 模型目录持久化实现 |
| `infrastructure/database/ModelSpecDo.java` | 模型规格 DO |
| `infrastructure/database/ChannelDo.java` | 渠道 DO |
| `infrastructure/database/ChannelCredentialDo.java` | 凭证 DO |
| `infrastructure/database/ChannelModelDo.java` | 渠道模型 DO |

### 迁移后删除的文件（旧包）

- `domain/model/` — 整个子包删除
- `domain/product/` — 整个子包删除
- `domain/proxy/` — 整个子包删除
- `domain/metadata/` — 整个子包删除
- `infrastructure/gateway/llm/` — 旧协议实现目录删除
- 旧 DO 类和 Repository 类

---

## Task 1: 创建 supply 枚举

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/BillingMode.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelState.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/CredentialState.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelModelState.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ModelSpecState.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ProviderState.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/Protocol.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ProviderErrorType.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/RoutingStrategy.java`
- Test: 手动编译验证

- [ ] **Step 1: 创建枚举包目录**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums
```

- [ ] **Step 2: 创建 BillingMode 枚举**

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 计费模式
 */
public enum BillingMode {
    /** 按量计费 */
    PAY_AS_YOU_GO,
    /** 订阅模式-编程 */
    SUBSCRIPTION_CODING,
    /** 订阅模式-Token */
    SUBSCRIPTION_TOKEN
}
```

- [ ] **Step 3: 创建 ChannelState 枚举**

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道状态
 */
public enum ChannelState {
    ACTIVE, DISABLED
}
```

- [ ] **Step 4: 创建 CredentialState 枚举**

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 凭证状态
 */
public enum CredentialState {
    ACTIVE, DISABLED
}
```

- [ ] **Step 5: 创建 ChannelModelState 枚举**

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道模型状态
 */
public enum ChannelModelState {
    ACTIVE, DISABLED
}
```

- [ ] **Step 6: 创建 ModelSpecState 枚举**

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 模型规格状态
 */
public enum ModelSpecState {
    ACTIVE, DISABLED
}
```

- [ ] **Step 7: 创建 ProviderState 枚举**

读取当前 `domain/model/enums/ProviderState.java` 的内容，复制到新路径，包名改为 `com.codingas.gateway.domain.supply.enums`。

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 供应商状态
 */
public enum ProviderState {
    ACTIVE, DISABLED
}
```

- [ ] **Step 8: 创建 Protocol 枚举**

读取当前 `domain/metadata/enums/Protocol.java` 的内容，复制到新路径，包名改为 `com.codingas.gateway.domain.supply.enums`。

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 协议类型
 */
public enum Protocol {
    OPENAI, ANTHROPIC
}
```

- [ ] **Step 9: 创建 ProviderErrorType 枚举**

读取当前 `domain/proxy/enums/ProviderErrorType.java` 的内容，复制到新路径，包名改为 `com.codingas.gateway.domain.supply.enums`。

- [ ] **Step 10: 创建 RoutingStrategy 枚举**

读取当前 `domain/proxy/entity/RoutingStrategy.java` 的内容，复制到新路径，包名改为 `com.codingas.gateway.domain.supply.enums`。

- [ ] **Step 11: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 12: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/
git commit -m "refactor: 创建 supply 域枚举类型（BillingMode/ChannelState/CredentialState 等）"
```

---

## Task 2: 创建 supply 实体

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Provider.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Channel.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ChannelCredential.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ChannelModel.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ModelSpec.java`

- [ ] **Step 1: 创建实体包目录**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity
```

- [ ] **Step 2: 创建 Provider 实体**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Provider extends BaseEntity {
    /** 程序标识（如 "openai", "anthropic", "zhipu"） */
    private String code;
    /** 显示名（如 "OpenAI", "智谱AI"） */
    private String name;
    private String logoUrl;
    private String websiteUrl;
    private String description;
    private ProviderState state;
}
```

- [ ] **Step 3: 创建 Channel 实体**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道实体（替代 Product）
 * <p>
 * 一个渠道对应一个端点和一个协议，多协议需求通过建多个 Channel 解决。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Channel extends BaseEntity {
    private Long providerId;
    private String name;
    /** 单一端点 URL */
    private String endpointUrl;
    /** 单一协议类型 */
    private Protocol protocol;
    /** 计费模式 */
    private BillingMode billingMode;
    private Integer priority;
    private Integer weight;
    private Integer timeout;
    private Integer maxRetries;
    private ChannelState state;
}
```

- [ ] **Step 4: 创建 ChannelCredential 实体**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 渠道凭证实体（替代 ProductApiKey）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelCredential extends BaseEntity {
    private Long channelId;
    private String name;
    private String apiKeyEncrypted;
    private String apiKeyPrefix;
    private String keyAlias;
    private Integer weight;
    private Integer priority;
    private CredentialState state;
    private Instant lastUsedAt;
}
```

- [ ] **Step 5: 创建 ChannelModel 实体**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 渠道模型关联实体（替代 ProductModel）
 * <p>
 * 从纯关联实体升级为带定价的关联实体，定价随模型独立变更。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelModel extends BaseEntity {
    private Long channelId;
    private Long modelSpecId;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private BigDecimal reasoningPrice;
    private BigDecimal cacheReadPrice;
    private BigDecimal cacheWritePrice;
    private BigDecimal inputAudioPrice;
    private BigDecimal outputAudioPrice;
    /** 订阅模式下的 Token 额度限制 */
    private Long quotaLimit;
    private ChannelModelState state;
}
```

- [ ] **Step 6: 创建 ModelSpec 实体**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 模型规格实体（从 Model 拆出规格部分）
 * <p>
 * ModelSpec 是模型固有规格，与渠道无关。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelSpec extends BaseEntity {
    /** 供应商侧标识（如 "gpt-4o"，路由匹配用） */
    private String providerModelId;
    private String displayName;
    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Map<String, Boolean> capabilities;
    private List<String> modalities;
    private ModelSpecState state;
}
```

- [ ] **Step 7: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/
git commit -m "refactor: 创建 supply 域实体（Provider/Channel/ChannelCredential/ChannelModel/ModelSpec）"
```

---

## Task 3: 创建 supply 值对象与异常

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject/RoutingContext.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject/ConnectivityTestResultVO.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/exception/ProviderException.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/exception/ChannelException.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/exception/ProtocolValidationException.java`

- [ ] **Step 1: 创建目录**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/exception
```

- [ ] **Step 2: 创建 RoutingContext 值对象**

读取当前 `domain/proxy/entity/RoutingContext.java`，将其从实体转为值对象并适配新字段名：

```java
package com.codingas.gateway.domain.supply.valueobject;

import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;

/**
 * 路由上下文值对象
 */
public record RoutingContext(
        Long channelId,
        String endpoint,
        Protocol protocol,
        String providerApiKey,
        RoutingStrategy strategy
) {}
```

- [ ] **Step 3: 创建 ConnectivityTestResultVO 值对象**

```java
package com.codingas.gateway.domain.supply.valueobject;

/**
 * 连通性测试结果值对象
 */
public record ConnectivityTestResultVO(
        boolean success,
        Long channelId,
        String errorMessage,
        long latencyMs
) {}
```

- [ ] **Step 4: 创建 ProviderException**

读取当前 `domain/model/exception/ProviderException.java`（或 `infrastructure/exception/ProviderException.java`），将其移到新路径：

```java
package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 供应商异常
 */
public class ProviderException extends GatewayException {
    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 5: 创建 ChannelException**

```java
package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 渠道异常
 */
public class ChannelException extends GatewayException {
    public ChannelException(String message) {
        super(message);
    }

    public ChannelException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 6: 创建 ProtocolValidationException**

读取当前 `domain/proxy/exception/ProtocolValidationException.java`，迁移到新路径：

```java
package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 协议校验异常
 */
public class ProtocolValidationException extends GatewayException {
    public ProtocolValidationException(String message) {
        super(message);
    }

    public ProtocolValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 7: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject/
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/exception/
git commit -m "refactor: 创建 supply 域值对象与异常（RoutingContext/ConnectivityTestResultVO/ProviderException/ChannelException/ProtocolValidationException）"
```

---

## Task 4: 迁移协议相关类到 supply/protocol

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolConverter.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolValidator.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/ProtocolResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/StreamChunkResult.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIChatRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIChatResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicMessagesRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicMessagesResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/OpenAIProtocolValidator.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/AnthropicProtocolValidator.java`

- [ ] **Step 1: 创建协议包目录**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol
```

- [ ] **Step 2: 逐个迁移协议类**

对 `domain/proxy/protocol/` 下的每个 `.java` 文件执行以下操作：
1. 读取源文件内容
2. 将包名从 `com.codingas.gateway.domain.proxy.protocol` 改为 `com.codingas.gateway.domain.supply.protocol`
3. 如有引用 `domain.proxy.enums.Protocol` 等旧引用，更新为 `domain.supply.enums.Protocol`
4. 写入新路径

涉及文件列表（读取源文件后逐个迁移）：
- `ProtocolConverter.java`
- `ProtocolValidator.java`
- `ProtocolRequest.java`
- `ProtocolResponse.java`
- `StreamChunkResult.java`
- `OpenAIChatRequest.java`
- `OpenAIChatResponse.java`
- `AnthropicMessagesRequest.java`
- `AnthropicMessagesResponse.java`
- `OpenAIProtocolValidator.java`
- `AnthropicProtocolValidator.java`

- [ ] **Step 3: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS（此时旧文件仍在，新文件也在，但新文件使用新包名，互不冲突）

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/protocol/
git commit -m "refactor: 迁移协议相关类到 supply/protocol 子包"
```

---

## Task 5: 创建 supply Gateway 接口

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ProviderGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelCredentialGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ModelSpecGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ConnectivityTester.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ProtocolGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ProtocolGatewayFactory.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/StreamCallback.java`

- [ ] **Step 1: 创建 Gateway 包目录**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway
```

- [ ] **Step 2: 创建 ProviderGateway**

读取当前 `domain/model/gateway/ProviderGateway.java`，重命名方法签名以适配新实体：

```java
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Provider;

import java.util.List;
import java.util.Optional;

/**
 * 供应商持久化接口
 */
public interface ProviderGateway {
    Provider save(Provider provider);
    Optional<Provider> findById(Long id);
    Optional<Provider> findByCode(String code);
    List<Provider> findAll();
    void deleteById(Long id);
}
```

- [ ] **Step 3: 创建 ChannelGateway**

读取当前 `domain/product/gateway/ProductGateway.java`，适配新命名：

```java
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.Protocol;

import java.util.List;
import java.util.Optional;

/**
 * 渠道持久化接口
 */
public interface ChannelGateway {
    Channel save(Channel channel);
    Optional<Channel> findById(Long id);
    List<Channel> findByProviderId(Long providerId);
    List<Channel> findByProtocol(Protocol protocol);
    List<Channel> findAll();
    void deleteById(Long id);
}
```

- [ ] **Step 4: 创建 ChannelCredentialGateway**

```java
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelCredential;

import java.util.List;
import java.util.Optional;

/**
 * 渠道凭证持久化接口
 */
public interface ChannelCredentialGateway {
    ChannelCredential save(ChannelCredential credential);
    Optional<ChannelCredential> findById(Long id);
    List<ChannelCredential> findByChannelId(Long channelId);
    void deleteById(Long id);
}
```

- [ ] **Step 5: 创建 ModelSpecGateway**

读取当前 `domain/model/gateway/ModelGateway.java`，适配新命名：

```java
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ModelSpec;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格持久化接口
 */
public interface ModelSpecGateway {
    ModelSpec save(ModelSpec modelSpec);
    Optional<ModelSpec> findById(Long id);
    Optional<ModelSpec> findByProviderModelId(String providerModelId);
    List<ModelSpec> findAll();
    void deleteById(Long id);
}
```

- [ ] **Step 6: 创建 ConnectivityTester**

读取当前 `domain/model/gateway/ConnectivityTester.java`，适配新命名：

```java
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResultVO;

/**
 * 连通性测试接口
 */
public interface ConnectivityTester {
    ConnectivityTestResultVO test(Long channelId);
}
```

- [ ] **Step 7: 迁移 ProtocolGateway 接口**

读取当前 `domain/proxy/gateway/ProtocolGateway.java`，包名改为 `com.codingas.gateway.domain.supply.gateway`，import 中的 `domain.proxy.protocol` 改为 `domain.supply.protocol`：

```java
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.protocol.ProtocolRequest;
import com.codingas.gateway.domain.supply.protocol.ProtocolResponse;

/**
 * 协议网关接口
 */
public interface ProtocolGateway {
    ProtocolResponse chat(ProtocolRequest request);
    void chatStream(ProtocolRequest request, StreamCallback callback);
}
```

- [ ] **Step 8: 迁移 ProtocolGatewayFactory 接口**

读取当前 `domain/proxy/gateway/ProtocolGatewayFactory.java`，包名改为 `com.codingas.gateway.domain.supply.gateway`：

```java
package com.codingas.gateway.domain.supply.gateway;

/**
 * 协议网关工厂接口
 */
public interface ProtocolGatewayFactory {
    ProtocolGateway create(String protocol, String baseUrl, String apiKey, int timeout);
}
```

- [ ] **Step 9: 迁移 StreamCallback 接口**

读取当前 `domain/proxy/gateway/StreamCallback.java`，包名改为 `com.codingas.gateway.domain.supply.gateway`：

```java
package com.codingas.gateway.domain.supply.gateway;

/**
 * 流式回调接口
 */
public interface StreamCallback {
    void onChunk(String data);
    void onComplete();
    void onError(Throwable error);
}
```

- [ ] **Step 10: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 11: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/
git commit -m "refactor: 创建 supply 域 Gateway 接口（ProviderGateway/ChannelGateway/ModelSpecGateway/ProtocolGateway 等）"
```

---

## Task 6: 创建 supply Domain Service

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/service/ProviderDomainService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/service/ChannelDomainService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/service/ChannelCredentialDomainService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/service/ModelSpecDomainService.java`

- [ ] **Step 1: 创建服务包目录**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/service
```

- [ ] **Step 2: 创建 ProviderDomainService**

读取当前 `domain/model/service/ProviderDomainService.java`，适配新实体和 Gateway：

```java
package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应商领域服务
 */
@Service
public class ProviderDomainService {

    private final ProviderGateway providerGateway;

    public ProviderDomainService(ProviderGateway providerGateway) {
        this.providerGateway = providerGateway;
    }

    public Provider create(Provider provider) {
        return providerGateway.save(provider);
    }

    public Provider update(Provider provider) {
        return providerGateway.save(provider);
    }

    public Provider activate(Long id) {
        Provider provider = providerGateway.findById(id)
                .orElseThrow(() -> new ProviderException("供应商不存在: " + id));
        provider.setState(ProviderState.ACTIVE);
        return providerGateway.save(provider);
    }

    public Provider disable(Long id) {
        Provider provider = providerGateway.findById(id)
                .orElseThrow(() -> new ProviderException("供应商不存在: " + id));
        provider.setState(ProviderState.DISABLED);
        return providerGateway.save(provider);
    }

    public List<Provider> findAll() {
        return providerGateway.findAll();
    }
}
```

- [ ] **Step 3: 创建 ChannelDomainService**

```java
package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.exception.ChannelException;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResultVO;
import com.codingas.gateway.domain.supply.gateway.ConnectivityTester;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 渠道领域服务
 */
@Service
public class ChannelDomainService {

    private final ChannelGateway channelGateway;
    private final ConnectivityTester connectivityTester;

    public ChannelDomainService(ChannelGateway channelGateway, ConnectivityTester connectivityTester) {
        this.channelGateway = channelGateway;
        this.connectivityTester = connectivityTester;
    }

    public Channel create(Channel channel) {
        return channelGateway.save(channel);
    }

    public Channel update(Channel channel) {
        return channelGateway.save(channel);
    }

    public Channel activate(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("渠道不存在: " + id));
        channel.setState(ChannelState.ACTIVE);
        return channelGateway.save(channel);
    }

    public Channel disable(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("渠道不存在: " + id));
        channel.setState(ChannelState.DISABLED);
        return channelGateway.save(channel);
    }

    public List<Channel> findByProviderId(Long providerId) {
        return channelGateway.findByProviderId(providerId);
    }

    public ConnectivityTestResultVO testConnectivity(Long channelId) {
        return connectivityTester.test(channelId);
    }
}
```

- [ ] **Step 4: 创建 ChannelCredentialDomainService**

```java
package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.exception.ChannelException;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 凭证领域服务
 */
@Service
public class ChannelCredentialDomainService {

    private final ChannelCredentialGateway channelCredentialGateway;

    public ChannelCredentialDomainService(ChannelCredentialGateway channelCredentialGateway) {
        this.channelCredentialGateway = channelCredentialGateway;
    }

    public ChannelCredential create(ChannelCredential credential) {
        return channelCredentialGateway.save(credential);
    }

    public ChannelCredential activate(Long id) {
        ChannelCredential credential = channelCredentialGateway.findById(id)
                .orElseThrow(() -> new ChannelException("凭证不存在: " + id));
        credential.setState(CredentialState.ACTIVE);
        return channelCredentialGateway.save(credential);
    }

    public ChannelCredential disable(Long id) {
        ChannelCredential credential = channelCredentialGateway.findById(id)
                .orElseThrow(() -> new ChannelException("凭证不存在: " + id));
        credential.setState(CredentialState.DISABLED);
        return channelCredentialGateway.save(credential);
    }

    public List<ChannelCredential> findByChannelId(Long channelId) {
        return channelCredentialGateway.findByChannelId(channelId);
    }
}
```

- [ ] **Step 5: 创建 ModelSpecDomainService**

读取当前 `domain/model/service/ModelDomainService.java`，适配新实体和 Gateway：

```java
package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型规格领域服务
 */
@Service
public class ModelSpecDomainService {

    private final ModelSpecGateway modelSpecGateway;

    public ModelSpecDomainService(ModelSpecGateway modelSpecGateway) {
        this.modelSpecGateway = modelSpecGateway;
    }

    public ModelSpec create(ModelSpec modelSpec) {
        return modelSpecGateway.save(modelSpec);
    }

    public ModelSpec update(ModelSpec modelSpec) {
        return modelSpecGateway.save(modelSpec);
    }

    public ModelSpec activate(Long id) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
                .orElseThrow(() -> new ProviderException("模型规格不存在: " + id));
        modelSpec.setState(ModelSpecState.ACTIVE);
        return modelSpecGateway.save(modelSpec);
    }

    public ModelSpec disable(Long id) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
                .orElseThrow(() -> new ProviderException("模型规格不存在: " + id));
        modelSpec.setState(ModelSpecState.DISABLED);
        return modelSpecGateway.save(modelSpec);
    }

    public List<ModelSpec> findAll() {
        return modelSpecGateway.findAll();
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/service/
git commit -m "refactor: 创建 supply 域领域服务（ProviderDomainService/ChannelDomainService/ChannelCredentialDomainService/ModelSpecDomainService）"
```

---

## Task 7: 创建 supply/catalog 子包

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity/ProviderCatalog.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity/ModelCatalog.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity/ChannelCatalog.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity/ChannelModelCatalog.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/gateway/ProviderCatalogGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/gateway/ModelCatalogGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/service/CatalogDomainService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/enums/MetadataSource.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/enums/CatalogState.java`

- [ ] **Step 1: 创建目录**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/gateway
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/service
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/enums
```

- [ ] **Step 2: 创建 catalog 枚举**

读取当前 `domain/metadata/enums/` 下的枚举类，迁移并重命名：

```java
// MetadataSource.java
package com.codingas.gateway.domain.supply.catalog.enums;

/**
 * 元数据来源
 */
public enum MetadataSource {
    MANUAL, MODELS_DEV, API_SYNC
}
```

```java
// CatalogState.java
package com.codingas.gateway.domain.supply.catalog.enums;

/**
 * 目录状态
 */
public enum CatalogState {
    ACTIVE, DISABLED
}
```

- [ ] **Step 3: 创建 catalog 实体**

逐个读取 `domain/metadata/entity/` 下的实体，迁移并重命名（ProviderMetadata → ProviderCatalog, ModelMetadata → ModelCatalog, ProductMetadata → ChannelCatalog, ProductModelMetadata → ChannelModelCatalog）：

```java
// ProviderCatalog.java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.MetadataSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商目录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderCatalog extends BaseEntity {
    private Long providerId;
    private String providerCode;
    private String providerName;
    private String logoUrl;
    private String websiteUrl;
    private String description;
    private MetadataSource source;
    private CatalogState state;
}
```

```java
// ModelCatalog.java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.MetadataSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 模型目录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelCatalog extends BaseEntity {
    private String providerModelId;
    private String displayName;
    private String modelFamily;
    private String providerCode;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Map<String, Boolean> capabilities;
    private List<String> modalities;
    private MetadataSource source;
    private CatalogState state;
}
```

```java
// ChannelCatalog.java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.MetadataSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道目录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelCatalog extends BaseEntity {
    private Long channelId;
    private String channelName;
    private String providerCode;
    private MetadataSource source;
    private CatalogState state;
}
```

```java
// ChannelModelCatalog.java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.MetadataSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 渠道模型目录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelModelCatalog extends BaseEntity {
    private Long channelModelId;
    private String providerModelId;
    private String channelName;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private MetadataSource source;
    private CatalogState state;
}
```

- [ ] **Step 4: 创建 catalog Gateway 接口**

```java
// ProviderCatalogGateway.java
package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;

import java.util.List;
import java.util.Optional;

/**
 * 供应商目录持久化接口
 */
public interface ProviderCatalogGateway {
    ProviderCatalog save(ProviderCatalog catalog);
    Optional<ProviderCatalog> findByProviderCode(String providerCode);
    List<ProviderCatalog> findAll();
    void deleteByProviderCode(String providerCode);
}
```

```java
// ModelCatalogGateway.java
package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;

import java.util.List;
import java.util.Optional;

/**
 * 模型目录持久化接口
 */
public interface ModelCatalogGateway {
    ModelCatalog save(ModelCatalog catalog);
    Optional<ModelCatalog> findByProviderModelId(String providerModelId);
    List<ModelCatalog> findAll();
    List<ModelCatalog> findByProviderCode(String providerCode);
    void deleteByProviderModelId(String providerModelId);
}
```

- [ ] **Step 5: 创建 CatalogDomainService**

读取当前 `domain/metadata/service/` 下的服务，迁移并重命名：

```java
package com.codingas.gateway.domain.supply.catalog.service;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 目录领域服务
 */
@Service
public class CatalogDomainService {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final ModelCatalogGateway modelCatalogGateway;

    public CatalogDomainService(ProviderCatalogGateway providerCatalogGateway,
                                 ModelCatalogGateway modelCatalogGateway) {
        this.providerCatalogGateway = providerCatalogGateway;
        this.modelCatalogGateway = modelCatalogGateway;
    }

    public ProviderCatalog saveProviderCatalog(ProviderCatalog catalog) {
        return providerCatalogGateway.save(catalog);
    }

    public List<ProviderCatalog> findAllProviderCatalogs() {
        return providerCatalogGateway.findAll();
    }

    public ModelCatalog saveModelCatalog(ModelCatalog catalog) {
        return modelCatalogGateway.save(catalog);
    }

    public List<ModelCatalog> findAllModelCatalogs() {
        return modelCatalogGateway.findAll();
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/
git commit -m "refactor: 创建 supply/catalog 子包（目录实体/Gateway/服务/枚举）"
```

---

## Task 8: 创建基础设施层 Gateway 实现

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ProviderGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ChannelGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ChannelCredentialGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ModelSpecGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ConnectivityTesterImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ProviderCatalogGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ModelCatalogGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ChannelCatalogGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/ChannelModelCatalogGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/protocol/OpenAIProtocolGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/protocol/AnthropicProtocolGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/protocol/ProtocolGatewayFactoryImpl.java`

- [ ] **Step 1: 创建目录**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/protocol
```

- [ ] **Step 2: 创建 DO 类**

读取现有 DO 类（如 `infrastructure/database/ProviderDo.java`, `ProductDo.java` 等），基于新实体创建对应的 DO。DO 类使用 JPA 注解，与数据库表映射。

需要创建的 DO 类（在 `infrastructure/database/` 下）：
- `ModelSpecDo.java` — 对应 `model_specs` 表
- `ChannelDo.java` — 对应 `channels` 表
- `ChannelCredentialDo.java` — 对应 `channel_credentials` 表
- `ChannelModelDo.java` — 对应 `channel_models` 表

每个 DO 类的字段与对应 Entity 匹配，表名使用 snake_case 复数形式。

- [ ] **Step 3: 创建 Repository 接口**

在 DO 同包下创建 Spring Data JPA Repository 接口。

- [ ] **Step 4: 创建 Gateway 实现类**

逐个读取现有 Gateway 实现（`infrastructure/gateway/provider/` 下），基于新实体和 Gateway 接口创建新实现。每个实现负责 DO ↔ Entity 转换。

示例模式：
```java
@Repository
public class ProviderGatewayImpl implements ProviderGateway {
    private final ProviderRepository repository;

    @Override
    public Provider save(Provider provider) {
        ProviderDo do_ = toDo(provider);
        return toEntity(repository.save(do_));
    }
    // ... toDo/toEntity 转换方法
}
```

- [ ] **Step 5: 迁移协议实现**

读取现有 `infrastructure/gateway/llm/` 下的协议实现，迁移到 `infrastructure/gateway/supply/protocol/`：
- `OpenAIProtocolGateway.java` — 更新 import 指向 `domain.supply.protocol` 和 `domain.supply.gateway`
- `AnthropicProtocolGateway.java` — 同上
- `ProtocolGatewayFactoryImpl.java` — 同上

- [ ] **Step 6: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/supply/
git commit -m "refactor: 创建 supply 基础设施层 Gateway 实现（含 DO/Repository/协议实现）"
```

---

## Task 9: 迁移应用层引用

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProxyServiceImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChannelRoutingService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProductRoutingService.java` → 重命名为 `SupplyRoutingService.java`
- Modify: 所有引用旧包的 application 层文件

- [ ] **Step 1: 更新 ProxyServiceImpl**

将所有 import 从旧包改为新包：
- `domain.proxy.entity.RoutingContext` → `domain.supply.valueobject.RoutingContext`
- `domain.proxy.entity.RoutingStrategy` → `domain.supply.enums.RoutingStrategy`
- `domain.proxy.gateway.ProtocolGateway` → `domain.supply.gateway.ProtocolGateway`
- `domain.proxy.gateway.ProtocolGatewayFactory` → `domain.supply.gateway.ProtocolGatewayFactory`
- `domain.proxy.gateway.StreamCallback` → `domain.supply.gateway.StreamCallback`
- `domain.proxy.protocol.*` → `domain.supply.protocol.*`

- [ ] **Step 2: 更新 ChannelRoutingService**

将所有 import 从旧包改为新包：
- `domain.proxy.entity.RoutingContext` → `domain.supply.valueobject.RoutingContext`
- `domain.iam.valueobject.Identity` 保持不变

- [ ] **Step 3: 重命名 ProductRoutingService 为 SupplyRoutingService**

1. 读取 `ProductRoutingService.java` 内容
2. 重命名类为 `SupplyRoutingService`
3. 更新所有 import 指向新包
4. 更新内部逻辑：`productId` → `channelId`, `Product` → `Channel` 等
5. 删除旧文件，创建新文件

- [ ] **Step 4: 更新其他 application 层文件**

扫描所有 application 层文件，将引用旧包的 import 更新为新包路径。

- [ ] **Step 5: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/
git commit -m "refactor: 迁移应用层引用到 supply 域（ProductRoutingService→SupplyRoutingService）"
```

---

## Task 10: 迁移适配器层引用

**Files:**
- Modify: 所有引用旧包的 adapter 层文件

- [ ] **Step 1: 扫描适配器层旧引用**

```bash
grep -rn "domain\.model\.\|domain\.product\.\|domain\.proxy\.\|domain\.metadata\." gateway-boot/src/main/java/com/codingas/gateway/adapter/ | grep "\.java:" | head -30
```

- [ ] **Step 2: 逐文件更新 import**

对扫描到的每个文件，将旧包引用替换为新包：
- `domain.model.entity.Provider` → `domain.supply.entity.Provider`
- `domain.model.entity.Model` → `domain.supply.entity.ModelSpec`
- `domain.product.entity.Product` → `domain.supply.entity.Channel`
- `domain.product.entity.ProductApiKey` → `domain.supply.entity.ChannelCredential`
- `domain.product.entity.ProductModel` → `domain.supply.entity.ChannelModel`
- `domain.product.enums.ProductType` → `domain.supply.enums.BillingMode`
- `domain.product.enums.ProductState` → `domain.supply.enums.ChannelState`
- `domain.metadata.enums.Protocol` → `domain.supply.enums.Protocol`
- `domain.proxy.protocol.*` → `domain.supply.protocol.*`
- 等等

- [ ] **Step 3: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/
git commit -m "refactor: 迁移适配器层引用到 supply 域"
```

---

## Task 11: 迁移跨域 Gateway 引用

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/gateway/` 下的跨域 Gateway 接口
- Modify: 其他域中引用旧包的文件

- [ ] **Step 1: 扫描跨域引用**

```bash
grep -rn "domain\.model\.\|domain\.product\.\|domain\.proxy\.\|domain\.metadata\." gateway-boot/src/main/java/com/codingas/gateway/domain/ | grep -v "domain/supply/" | grep "\.java:" | head -30
```

- [ ] **Step 2: 逐文件更新 import**

对扫描到的每个文件，将旧包引用替换为新包路径。

- [ ] **Step 3: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/
git commit -m "refactor: 迁移跨域 Gateway 引用到 supply 域"
```

---

## Task 12: 删除旧包

**Files:**
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/model/`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/`
- Delete: 旧的 infrastructure Gateway 实现和 DO 类

- [ ] **Step 1: 确认旧包无引用**

```bash
grep -rn "com\.codingas\.gateway\.domain\.model\.\|com\.codingas\.gateway\.domain\.product\.\|com\.codingas\.gateway\.domain\.proxy\.\|com\.codingas\.gateway\.domain\.metadata\." gateway-boot/src/main/java/ | grep "\.java:" | head -20
```

Expected: 无结果（所有引用已迁移）

- [ ] **Step 2: 删除旧 domain 子包**

```bash
rm -rf gateway-boot/src/main/java/com/codingas/gateway/domain/model/
rm -rf gateway-boot/src/main/java/com/codingas/gateway/domain/product/
rm -rf gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/
rm -rf gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/
```

- [ ] **Step 3: 删除旧 infrastructure 实现**

删除 `infrastructure/gateway/provider/`、`infrastructure/gateway/llm/` 等旧实现目录（如果不再被引用）。

- [ ] **Step 4: 编译验证**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add -A gateway-boot/src/main/java/com/codingas/gateway/
git commit -m "refactor: 删除旧域子包（model/product/proxy/metadata），供给体系统一归入 supply 域"
```

---

## Task 13: 数据库迁移脚本

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V<next>__supply_domain_refactor.sql`

- [ ] **Step 1: 确定迁移版本号**

```bash
ls gateway-boot/src/main/resources/db/migration/ 2>/dev/null | sort | tail -3
```

- [ ] **Step 2: 编写迁移 SQL**

```sql
-- 供给域重构：表重命名与字段调整

-- products → channels
ALTER TABLE products RENAME TO channels;
ALTER TABLE channels RENAME COLUMN product_type TO billing_mode;
ALTER TABLE channels RENAME COLUMN product_state TO channel_state;
-- endpoints JSON 字段替换为扁平字段
ALTER TABLE channels ADD COLUMN endpoint_url VARCHAR(512);
ALTER TABLE channels ADD COLUMN protocol VARCHAR(32);
-- 从 endpoints JSON 迁移数据到扁平字段（需根据实际数据格式调整）
-- UPDATE channels SET endpoint_url = ... , protocol = ... WHERE endpoints IS NOT NULL;
ALTER TABLE channels DROP COLUMN endpoints;
-- pricing JSON 字段已下沉到 channel_models，删除 channels 上的 pricing
ALTER TABLE channels DROP COLUMN pricing;
ALTER TABLE channels DROP COLUMN provider_name;

-- product_api_keys → channel_credentials
ALTER TABLE product_api_keys RENAME TO channel_credentials;
ALTER TABLE channel_credentials RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_credentials RENAME COLUMN api_key_encrypted TO api_key_encrypted;
ALTER TABLE channel_credentials RENAME COLUMN api_key_state TO credential_state;

-- product_models → channel_models
ALTER TABLE product_models RENAME TO channel_models;
ALTER TABLE product_models RENAME COLUMN product_id TO channel_id;
ALTER TABLE product_models RENAME COLUMN model_id TO model_spec_id;
ALTER TABLE product_models RENAME COLUMN product_model_state TO channel_model_state;
-- 增加定价字段
ALTER TABLE channel_models ADD COLUMN input_price DECIMAL(20,10);
ALTER TABLE channel_models ADD COLUMN output_price DECIMAL(20,10);
ALTER TABLE channel_models ADD COLUMN reasoning_price DECIMAL(20,10);
ALTER TABLE channel_models ADD COLUMN cache_read_price DECIMAL(20,10);
ALTER TABLE channel_models ADD COLUMN cache_write_price DECIMAL(20,10);
ALTER TABLE channel_models ADD COLUMN input_audio_price DECIMAL(20,10);
ALTER TABLE channel_models ADD COLUMN output_audio_price DECIMAL(20,10);
ALTER TABLE channel_models ADD COLUMN quota_limit BIGINT;

-- models → model_specs
ALTER TABLE models RENAME TO model_specs;
ALTER TABLE model_specs RENAME COLUMN model_state TO model_spec_state;
ALTER TABLE model_specs ADD COLUMN provider_model_id VARCHAR(128);
-- 从现有数据迁移 provider_model_id（需根据实际数据调整）

-- providers 增加 code 字段
ALTER TABLE providers ADD COLUMN code VARCHAR(64) UNIQUE;
-- 从现有数据迁移 code（需根据实际数据调整）
-- UPDATE providers SET code = LOWER(REPLACE(name, ' ', '_')) WHERE code IS NULL;

-- 元数据表重命名
ALTER TABLE provider_metadata RENAME TO provider_catalogs;
ALTER TABLE model_metadata RENAME TO model_catalogs;
ALTER TABLE product_metadata RENAME TO channel_catalogs;
ALTER TABLE product_model_metadata RENAME TO channel_model_catalogs;
```

- [ ] **Step 3: 验证迁移脚本语法**

```bash
cd gateway-boot && mvn compile -q 2>&1 | tail -5
```

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/resources/db/migration/
git commit -m "refactor: 添加供给域重构数据库迁移脚本"
```

---

## Task 14: 更新测试

**Files:**
- Modify: 所有引用旧包的测试文件

- [ ] **Step 1: 扫描测试文件旧引用**

```bash
grep -rn "domain\.model\.\|domain\.product\.\|domain\.proxy\.\|domain\.metadata\." gateway-boot/src/test/ | grep "\.java:" | head -30
```

- [ ] **Step 2: 逐文件更新测试 import 和断言**

对扫描到的每个测试文件：
1. 更新 import 路径
2. 更新类名引用（Product → Channel, Model → ModelSpec 等）
3. 更新断言中的字段名（productId → channelId, productType → billingMode 等）
4. 更新 mock 类型

- [ ] **Step 3: 运行全部测试**

```bash
cd gateway-boot && mvn test -q 2>&1 | tail -20
```

Expected: 所有测试通过

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/test/
git commit -m "refactor: 更新测试引用到 supply 域"
```

---

## Task 15: 更新章程文档

**Files:**
- Modify: `docs/constitution.md`

- [ ] **Step 1: 更新项目结构**

将章程中的项目结构从旧的 `domain/model/`、`domain/proxy/` 等替换为新的 `domain/supply/` 结构：

```
domain/
├── supply/                     # 供给域（核心域）
│   ├── entity/
│   ├── service/
│   ├── gateway/
│   ├── protocol/
│   ├── catalog/
│   ├── valueobject/
│   ├── enums/
│   └── exception/
├── iam/                        # 身份与访问控制领域
├── threat/                     # 威胁防护领域
├── dataprotection/             # 数据保护领域
├── quota/                      # 限额配额领域
├── audit/                      # 审计合规领域
└── alert/                      # 告警管理领域
```

- [ ] **Step 2: 更新异常分层**

将章程中的异常分层更新：
- `ProviderException` 从 infrastructure 层移到 `domain/supply/exception/`
- 新增 `ChannelException`、`ProtocolValidationException`

- [ ] **Step 3: 更新版本号和变更记录**

```markdown
| v2.5.0 | 2026-05-24 | **供给域统一重构**：model/product/proxy/metadata 四域合并为 supply 域；Product→Channel 重命名；Model→ModelSpec+ChannelModel 拆分；proxy 协议归入 supply/protocol；metadata 归入 supply/catalog |
```

- [ ] **Step 4: 提交**

```bash
git add docs/constitution.md
git commit -m "docs: 更新架构章程 v2.5.0 — 供给域统一重构"
```

---

## Task 16: 全量编译与测试验证

**Files:**
- 无新建/修改

- [ ] **Step 1: 全量编译**

```bash
cd gateway-boot && mvn clean compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行全部测试**

```bash
cd gateway-boot && mvn test 2>&1 | tail -20
```

Expected: 所有测试通过

- [ ] **Step 3: 检查无残留旧引用**

```bash
grep -rn "domain\.model\.\|domain\.product\.\|domain\.proxy\.\|domain\.metadata\." gateway-boot/src/main/java/ | grep "\.java:" | head -10
```

Expected: 无结果

- [ ] **Step 4: 最终提交（如有遗漏修复）**

```bash
git add -A
git commit -m "refactor: 供给域重构完成 — 清理残留引用"
```

---

## 自审清单

### 1. 规格覆盖

| 规格要求 | 对应 Task |
|---------|-----------|
| Provider 增加 code 字段 | Task 2 |
| Product → Channel 重命名 | Task 2 |
| ProductApiKey → ChannelCredential | Task 2 |
| ProductModel → ChannelModel（带定价） | Task 2 |
| Model → ModelSpec 拆分 | Task 2 |
| endpoints Map → endpointUrl + protocol 扁平字段 | Task 2 |
| pricing Map → ChannelModel 显式字段 | Task 2 |
| providerName 不冗余存储 | Task 2 |
| ProductType → BillingMode | Task 1 |
| State 枚举重命名 | Task 1 |
| Protocol 迁移到 supply | Task 1 |
| Protocol 相关类迁移 | Task 4 |
| supply/protocol 子包 | Task 4 |
| supply/catalog 子包 | Task 7 |
| Gateway 接口迁移 | Task 5 |
| Domain Service 迁移 | Task 6 |
| 基础设施层实现 | Task 8 |
| 应用层引用更新 | Task 9 |
| 适配器层引用更新 | Task 10 |
| 跨域引用更新 | Task 11 |
| 旧包删除 | Task 12 |
| 数据库迁移 | Task 13 |
| 测试更新 | Task 14 |
| 章程更新 | Task 15 |

### 2. 占位符扫描

- 无 "TBD"、"TODO"、"implement later" 等
- Task 8 和 Task 13 中的数据迁移 SQL 需要根据实际数据格式调整，已标注"需根据实际数据调整"

### 3. 类型一致性

- 所有实体字段名、枚举名、Gateway 方法签名在 Task 1-7 中定义，Task 8-14 引用一致
- RoutingContext 在 Task 3 定义为 record，在 Task 9 应用层使用时字段名一致
- Channel 的 endpointUrl + protocol 扁平字段在 Task 2 定义，在数据库迁移 Task 13 中一致
