# 大模型网关协议体系重构设计

## 概述

对 LLM-Gateway 的大模型调用体系进行全链路重构，解决以下问题：

1. 协议数据类错放在 `domain/supply/protocol/`，归属不清
2. 出站适配与上游调用混在 `ProtocolGateway` 中，职责不清
3. 命名不规范（ProtocolGateway 不是 DDD Gateway、ProtocolGatewayFactory 每次创建实例）
4. 路由策略未生效，模型匹配逻辑缺失
5. 缺少重试/熔断/审计/计量等生产必备能力
6. 供给域实体冗余字段（ModelSpec.providerId、Provider.baseUrl）

## 实施策略

采用**大步重构**，按逻辑聚合为 3 个阶段交付：

| 阶段 | 内容 | 可编译 |
|------|------|--------|
| 1 | 模型重构：实体调整 + 协议分层迁移 + 命名规范化 + 出站调谐分离 | 是 |
| 2 | 调用链重构：路由体系拆分 + ProxyService → ChatDispatchService + 流式调用重构 | 是 |
| 3 | 能力补全：重试/熔断/审计/计量/分级超时 | 是 |

---

## 阶段 1：模型重构

### 1.1 供给域实体调整

#### ModelSpec 去 providerId

ModelSpec 应为全局模型注册表，同一模型（如 `gpt-4o`）不应因供应商不同而重复定义。供应商映射通过 `ChannelModel` 关联。

变更清单：

- `ModelSpec` 删除 `providerId` 字段
- `ModelSpecGateway` 删除 `findByProviderId()` 方法
- `ModelSpecGatewayImpl` 删除对应实现
- `ModelSpecRepository` 删除 `findByProviderId()`
- `DataInitializer.createModelSpec()` 去掉 providerId 参数
- `CatalogMaterializeService` 调整：查模型按 provider 转为通过 Channel → ChannelModel → ModelSpec
- `ConfigCacheService.getModelsByProviderId()` 调整：改为通过 ChannelModel 间接获取
- DB 迁移：`model_specs` 表删除 `provider_id` 列

#### Provider 去 baseUrl

baseUrl 已下沉到 `ChannelEndpoint`，Provider 级别的 baseUrl 与之语义重叠。一个 Provider 可能有多个 API 端点（如 OpenAI 的 US/EU），连接配置应由 Channel + ChannelEndpoint 承载。

变更清单：

- `Provider` 删除 `baseUrl` 字段
- `ProviderCatalog` / `ProviderCatalogGatewayImpl` 删除 baseUrl 相关逻辑
- `CatalogDomainService` 删除 baseUrl 映射
- `CatalogMaterializeService` 调整：Provider 创建不再设 baseUrl
- DB 迁移：`providers` 表删除 `base_url` 列

### 1.2 协议数据契约迁移

从 `domain/supply/protocol/` 迁移到 `domain/protocol/contract/`：

| 原位置 | 目标位置 |
|--------|---------|
| `domain/supply/protocol/ProtocolRequest.java` | `domain/protocol/contract/ProtocolRequest.java` |
| `domain/supply/protocol/ProtocolResponse.java` | `domain/protocol/contract/ProtocolResponse.java` |
| `domain/supply/protocol/OpenAIChatRequest.java` | `domain/protocol/contract/OpenAIChatRequest.java` |
| `domain/supply/protocol/OpenAIChatResponse.java` | `domain/protocol/contract/OpenAIChatResponse.java` |
| `domain/supply/protocol/AnthropicMessagesRequest.java` | `domain/protocol/contract/AnthropicMessagesRequest.java` |
| `domain/supply/protocol/AnthropicMessagesResponse.java` | `domain/protocol/contract/AnthropicMessagesResponse.java` |
| `domain/supply/protocol/StreamChunkResult.java` | `domain/protocol/contract/StreamChunkResult.java` |

理由：协议数据契约是网关的领域语言，相当于银行领域的"金额"——跨层传递的公共概念。DTO 放在 domain 层可避免 application → adapter 的反向依赖。

### 1.3 协议校验迁移

| 原位置 | 目标位置 | 理由 |
|--------|---------|------|
| `domain/supply/protocol/ProtocolValidator.java` | `domain/protocol/validation/ProtocolValidator.java` | 校验接口保留在 domain |
| `domain/supply/protocol/OpenAIProtocolValidator.java` | `adapter/protocol/openai/OpenAIProtocolValidator.java` | 校验实现是适配层关注点 |
| `domain/supply/protocol/AnthropicProtocolValidator.java` | `adapter/protocol/anthropic/AnthropicProtocolValidator.java` | 校验实现是适配层关注点 |

### 1.4 协议转换迁移

| 原位置 | 目标位置 | 理由 |
|--------|---------|------|
| `domain/supply/protocol/ProtocolConverter.java` | `domain/protocol/conversion/ProtocolConverter.java` | 跨协议语义映射是网关核心业务逻辑 |

### 1.5 命名规范化

| 原命名 | 新命名 | 理由 |
|--------|--------|------|
| `domain/supply/gateway/ProtocolGateway.java` | `infrastructure/upstream/UpstreamClient.java` | Gateway 在 DDD 中指仓储接口，此处是 HTTP 客户端 |
| `infrastructure/supply/gateway/protocol/OpenAIProtocolGateway.java` | `infrastructure/upstream/OpenAIUpstreamClient.java` | 同上 |
| `infrastructure/supply/gateway/protocol/AnthropicProtocolGateway.java` | `infrastructure/upstream/AnthropicUpstreamClient.java` | 同上 |
| `domain/supply/gateway/ProtocolGatewayFactory.java` | `infrastructure/upstream/UpstreamClientRegistry.java` | 注册式优于工厂式，避免每次请求创建实例 |
| `domain/supply/gateway/StreamCallback.java` | `domain/protocol/contract/StreamCallback.java` | 回调接口是协议契约的一部分 |

### 1.6 新增：出站调谐器

新增 `application/proxy/OutboundTuner.java`，将当前混在 `ProtocolGateway` 中的出站适配逻辑抽取出来。

两层 Tuner 的职责边界：

| 层 | 类 | 职责 | 输入 | 输出 |
|---|---|------|------|------|
| `adapter/protocol/openai/` | `OpenAIOutboundTuner` | 协议级调谐：填充协议默认值（如 max_tokens）、格式修正 | `OpenAIChatRequest` | `OpenAIChatRequest` |
| `application/proxy/` | `OutboundTuner` | 渠道级调谐编排：模型名替换、字段覆盖、敏感字段剥离，调用协议级 Tuner | `ProtocolRequest` + `RoutingContext` | `ProtocolRequest` |

执行顺序：`OutboundTuner.tune()` → 调用对应协议的 `OutboundTuner.tune()` → 返回最终出站请求。

依赖路由上下文（RoutingContext），因此 `OutboundTuner` 放在 application 层。命名用 `Tuner`（调谐器）而非 `Adapter`，避免与 DDD 适配层命名冲突，语义更精准——按渠道要求微调请求参数。

### 1.7 测试迁移

| 原位置 | 目标位置 |
|--------|---------|
| `test/.../domain/supply/protocol/ProtocolConverterTest.java` | `test/.../domain/protocol/conversion/ProtocolConverterTest.java` |
| `test/.../domain/supply/protocol/OpenAIProtocolValidatorTest.java` | `test/.../adapter/protocol/openai/OpenAIProtocolValidatorTest.java` |

---

## 阶段 2：调用链重构

### 2.1 路由体系重构

当前 `SupplyRoutingService` 同时承担路由选择和凭证解析，且路由策略未生效。拆分为 5 个组件：

| 新组件 | 职责 | 位置 |
|--------|------|------|
| `ModelMatcher` | modelName → ChannelModel → Channel | `application/routing/` |
| `ChannelSelector` | 按 RoutingStrategy 选择渠道 | `application/routing/` |
| `CredentialResolver` | 从选中渠道解析凭证 | `application/routing/` |
| `EndpointResolver` | 从选中渠道解析协议端点 | `application/routing/` |
| `RoutingResolver` | 编排以上四步，返回 RoutingContext | `application/routing/` |

#### RoutingStrategy 生效方式

`ChannelSelector` 接收 `RoutingStrategy` 参数，内部按策略分发：

| 策略 | 选择算法 |
|------|---------|
| `WEIGHTED` | 按 ChannelCredential.weight 加权随机 |
| `FAILOVER` | 按 Channel.priority 排序，逐个尝试 |
| `COST_OPTIMIZED` | 按 ChannelModel.inputPrice 排序 |
| `LATENCY_OPTIMIZED` | 按历史延迟排序（需要埋点数据） |
| `RANDOM` | 均匀随机 |

### 2.2 ProxyService → ChatDispatchService

`ProxyService` 重命名为 `ChatDispatchService`，职责简化为纯编排：

```
ChatDispatchService.dispatch(ProtocolRequest, Identity, RoutingStrategy):
  1. InboundValidator.validate(request)                    // adapter/protocol
  2. RoutingContext ctx = routingResolver.resolve(...)     // application/routing
  3. ProtocolRequest outboundReq = outboundTuner.tune(request, ctx)  // application/proxy
  4. if (needsConversion)
       outboundReq = protocolConverter.convert(request, ctx)            // domain/protocol
  5. ProtocolResponse response = upstreamClient.chat(outboundReq)      // infrastructure/upstream
  6. if (needsConversion)
       response = protocolConverter.convert(response, ctx)              // domain/protocol
  7. postProcess(request, response, ctx)                   // 审计、计量
  8. return response
```

### 2.3 流式调用重构

当前 `proxyStream` 中的 SSE 拼接逻辑混在 ProxyServiceImpl 中。重构后：

- `UpstreamClient.chatStream()` 只负责上游 SSE 读取，通过 `StreamCallback` 交付原始 chunk
- 协议转换在 `ProtocolConverter` 中统一处理
- SSE 格式化由 `SseStreamHelper` 负责（当前位置已正确）

---

## 阶段 3：能力补全

### 3.1 重试机制

| 组件 | 位置 | 说明 |
|------|------|------|
| `RetryPolicy` | `infrastructure/resilience/` | 可配置的重试策略 |

配置项（`@ConfigurationProperties`）：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `gateway.retry.max-attempts` | 3 | 最大重试次数 |
| `gateway.retry.backoff-initial` | 1000ms | 初始退避时间 |
| `gateway.retry.backoff-multiplier` | 2 | 退避倍数 |
| `gateway.retry.retryable-status-codes` | 429, 500, 502, 503 | 可重试的 HTTP 状态码 |

### 3.2 熔断机制

| 组件 | 位置 | 说明 |
|------|------|------|
| `CircuitBreaker` | `infrastructure/resilience/` | 每个 ChannelEndpoint 一个熔断器 |
| `ChannelEndpointCircuitBreakerManager` | `infrastructure/resilience/` | 管理 Endpoint → CircuitBreaker 映射 |

熔断参数：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 失败率阈值 | 50% | 最近 N 次请求中失败率超过此值触发熔断 |
| 滑动窗口大小 | 10 | 统计窗口内的请求数 |
| OPEN 持续时间 | 30s | 熔断后多久进入 HALF_OPEN |
| HALF_OPEN 试探次数 | 3 | 允许的试探请求数 |

与路由集成：`ChannelSelector` 选渠道时跳过 OPEN 状态的渠道，FAILOVER 策略下优先选 CLOSED 的备用渠道。

### 3.3 审计日志串联

- `ChatDispatchService` 在调用前后记录审计事件
- `CallLog` 完整记录：model、channelId、endpointId、credentialId、inboundProtocol、upstreamProtocol、duration、success/failure
- 新增 `AuditGateway.saveCallLog()` 方法

### 3.4 Token 计量

- `ChatDispatchService` 在响应后发布 `TokenUsedEvent`，包含 userId、model、inputTokens、outputTokens、channelId
- 从 `ProtocolResponse` 提取 Token 用量：OpenAI 用 `usage.promptTokens/completionTokens`，Anthropic 用 `usage.inputTokens/outputTokens`
- 与现有 `TokenUsageEventListener` 衔接

### 3.5 分级超时

| 超时类型 | 默认值 | 说明 |
|---------|--------|------|
| `connectTimeout` | 5s | TCP 连接超时 |
| `readTimeout` | 60s | 整体读取超时 |
| `firstTokenTimeout` | 15s | 流式首 token 超时 |

配置项（`@ConfigurationProperties`）：

| 配置项 | 默认值 |
|--------|--------|
| `gateway.timeout.connect-default` | 5s |
| `gateway.timeout.read-default` | 60s |
| `gateway.timeout.first-token-default` | 15s |

---

## 目标目录结构

```
domain/protocol/
├── contract/                     ← 协议数据契约
│   ├── ProtocolRequest.java
│   ├── ProtocolResponse.java
│   ├── OpenAIChatRequest.java
│   ├── OpenAIChatResponse.java
│   ├── AnthropicMessagesRequest.java
│   ├── AnthropicMessagesResponse.java
│   ├── StreamChunkResult.java
│   └── StreamCallback.java
├── conversion/                   ← 跨协议转换（核心业务逻辑）
│   └── ProtocolConverter.java
└── validation/                   ← 校验接口
    └── ProtocolValidator.java

adapter/protocol/
├── openai/
│   ├── OpenAIProtocolValidator.java   ← 入站校验实现
│   └── OpenAIOutboundTuner.java       ← 出站调谐实现（协议级默认值）
└── anthropic/
    ├── AnthropicProtocolValidator.java
    └── AnthropicOutboundTuner.java

application/routing/
├── RoutingResolver.java
├── ModelMatcher.java
├── ChannelSelector.java
├── CredentialResolver.java
└── EndpointResolver.java

application/proxy/
├── ChatDispatchService.java          ← 原 ProxyService
├── OutboundTuner.java                ← 出站调谐编排
└── SupplyRoutingService.java         ← 废弃，由 RoutingResolver 替代

infrastructure/upstream/
├── UpstreamClient.java               ← 接口，原 ProtocolGateway
├── OpenAIUpstreamClient.java         ← 实现，原 OpenAIProtocolGateway
├── AnthropicUpstreamClient.java      ← 实现，原 AnthropicProtocolGateway
└── UpstreamClientRegistry.java       ← 注册表，原 ProtocolGatewayFactory

infrastructure/resilience/
├── RetryPolicy.java
├── CircuitBreaker.java
└── ChannelEndpointCircuitBreakerManager.java
```

## 完整调用链路

```
请求入口 (adapter/api)
  │
  ▼
SecurityFilter Chain (adapter/filter)
  │  Auth → RateLimit → IpBlock → DataMasking → ModelAccess
  ▼
ChatDispatchService (application/proxy)
  │
  │  ┌─ 前置阶段 ────────────────────────────────────────┐
  │  │  1. 校验：InboundValidator.validate(request)      │
  │  │     位置：adapter/protocol/                       │
  │  │  2. 路由：RoutingResolver.resolve(identity, model)│
  │  │     位置：application/routing/                    │
  │  │     ModelMatcher → ChannelSelector(strategy)      │
  │  │       → CredentialResolver → EndpointResolver    │
  │  │  3. 记录审计起点：auditGateway.logRequest(...)    │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 转换阶段（仅跨协议时执行）───────────────────────┐
  │  │  4. 请求转换：protocolConverter.convertRequest() │
  │  │     位置：domain/protocol/conversion/             │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 调谐阶段 ────────────────────────────────────────┐
  │  │  5. 调谐：outboundTuner.tune(request, ctx)        │
  │  │     位置：application/proxy/                      │
  │  │     职责：模型名替换、默认值填充、字段覆盖、      │
  │  │           敏感字段剥离                              │
  │  │     调谐必须按目标协议要求执行，而非入站协议       │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 调用阶段 ────────────────────────────────────────┐
  │  │  6. 上游调用：upstreamClient.chat(request)        │
  │  │     位置：infrastructure/upstream/                 │
  │  │     韧性包装：RetryPolicy + CircuitBreaker        │
  │  │     纯 HTTP 调用 + SSE 解析，不含业务逻辑         │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 转换阶段（仅跨协议时执行）───────────────────────┐
  │  │  7. 响应转换：protocolConverter.convertResponse()│
  │  │     位置：domain/protocol/conversion/             │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 后置阶段 ────────────────────────────────────────┐
  │  │  8. Token 计量：publish TokenUsedEvent            │
  │  │     位置：application/proxy/                      │
  │  │  9. 记录审计终点：auditGateway.logResponse(...)   │
  │  │     包含：duration、success/failure、Token 用量    │
  │  └──────────────────────────────────────────────────┘
  │
  ▼
响应返回
```
