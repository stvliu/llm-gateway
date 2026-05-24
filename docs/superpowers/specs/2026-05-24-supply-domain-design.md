# 大模型供给体系重构设计

## 重构目标

将分散在 `domain/model`、`domain/product`、`domain/proxy`、`domain/metadata` 四个子域的供给体系，合并为统一的 `domain/supply` 供给域。

核心原则：
- 命名对齐业界惯例，消除语义歧义（Product → Channel）
- 分离模型规格与渠道配置（Model → ModelSpec + ChannelModel）
- 协议适配归入供给链末端（proxy/protocol → supply/protocol）
- 路由调度归入应用层（proxy/routing → application/proxy）
- 元数据目录归入供给域子包（metadata → supply/catalog）

## 命名映射表

| 当前 | 目标 | 理由 |
|------|------|------|
| `domain/model` | `domain/supply` | "model" 不表达供给语义 |
| `domain/product` | 合入 `domain/supply` | Product 是 Channel 的误命名 |
| `domain/proxy`（protocol 部分） | `domain/supply/protocol` | 协议是供给链末端 |
| `domain/proxy`（routing 部分） | `application/proxy` | 调度归应用层 |
| `domain/metadata` | `domain/supply/catalog` | 元数据是供给目录 |
| `Provider` | `Provider` | 保持不变 |
| `Model` | `ModelSpec` | 分离规格与渠道关联 |
| `Product` | `Channel` | 消除语义歧义 |
| `ProductApiKey` | `ChannelCredential` | 抽象化认证方式 |
| `ProductModel` | `ChannelModel` | 对齐 Channel 命名 |
| `ProductType` | `BillingMode` | 表达计费模式 |
| `ModelState` | `ModelSpecState` | 对齐 |
| `ProductState` | `ChannelState` | 对齐 |
| `ProductApiKeyState` | `CredentialState` | 对齐 |
| `ProviderMetadata` | `ProviderCatalog` | 元数据→目录 |
| `ModelMetadata` | `ModelCatalog` | 元数据→目录 |
| `ProductMetadata` | `ChannelCatalog` | 元数据→目录 |
| `ProductModelMetadata` | `ChannelModelCatalog` | 对齐 |

## 核心实体关系

```
Provider (1) ──→ (N) Channel (1) ──→ (N) ChannelCredential
                       │
                       └──→ (N) ChannelModel ──→ ModelSpec
```

- Provider 是品牌级实体
- Channel 是供应商下的接入通道，持有端点、协议、路由策略
- ChannelCredential 是渠道的认证密钥
- ChannelModel 是渠道-模型部署关联，持有定价和额度
- ModelSpec 是模型固有规格，与渠道无关

## 实体字段设计

### Provider（增加 code 字段）

```java
public class Provider extends BaseEntity {
    private String code;            // 程序标识（如 "openai", "anthropic", "zhipu"）
    private String name;            // 显示名（如 "OpenAI", "智谱AI"）
    private String logoUrl;
    private String websiteUrl;
    private String description;
    private ProviderState state;
}
```

### Channel（替代 Product）

```java
public class Channel extends BaseEntity {
    private Long providerId;                                    // 不冗余 providerName
    private String name;
    private String endpointUrl;                                 // 单一端点 URL
    private Protocol protocol;                                  // 单一协议类型
    private BillingMode billingMode;                            // 原 ProductType
    private Integer priority;
    private Integer weight;
    private Integer timeout;
    private Integer maxRetries;
    private ChannelState state;
}
```

关键设计决策：
- `endpoints: Map<Protocol, String>` → `endpointUrl + protocol` 扁平字段。一个渠道对应一个端点一个协议，多协议需求通过建多个 Channel 解决
- `pricing: Map<Long, Map<String, BigDecimal>>` → 定价下沉到 ChannelModel。Channel 只关注接入配置，定价是商业属性随模型独立变更
- `providerName` 不冗余存储，通过 providerId 关联查询

### ChannelCredential（替代 ProductApiKey）

```java
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

### ChannelModel（渠道模型部署，替代 ProductModel）

```java
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
    private Long quotaLimit;                // 订阅模式下的 Token 额度限制
    private ChannelModelState state;
}
```

关键设计决策：
- 从纯关联实体升级为带定价的关联实体
- 定价字段显式声明（inputPrice, outputPrice 等），类型安全，优于 Map<String, BigDecimal>
- 定价随模型独立变更，不需要更新整个渠道

### ModelSpec（从 Model 拆出规格部分）

```java
public class ModelSpec extends BaseEntity {
    private String providerModelId;         // 供应商侧标识（如 "gpt-4o"，路由匹配用）
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

## 领域服务与 Gateway 接口

### 领域服务

| 服务 | 职责 |
|---------|------|
| `ProviderDomainService` | 供应商 CRUD、状态管理 |
| `ChannelDomainService` | 渠道 CRUD、连通性测试 |
| `ChannelCredentialDomainService` | 凭证 CRUD、密钥加解密 |
| `ModelSpecDomainService` | 模型规格 CRUD、能力查询 |
| `CatalogDomainService` | 元数据目录同步、查询 |

### Gateway 接口

| Gateway | 职责 |
|---------|------|
| `ProviderGateway` | 供应商持久化 |
| `ChannelGateway` | 渠道持久化 |
| `ChannelCredentialGateway` | 凭证持久化 |
| `ModelSpecGateway` | 模型规格持久化 |
| `ProviderCatalogGateway` | 供应商目录持久化 |
| `ModelCatalogGateway` | 模型目录持久化 |
| `ConnectivityTester` | 连通性测试 |

## 最终包结构

### domain/supply

```
domain/supply/
├── entity/
│   ├── Provider.java
│   ├── Channel.java
│   ├── ChannelCredential.java
│   ├── ChannelModel.java
│   └── ModelSpec.java
├── catalog/
│   ├── entity/
│   │   ├── ProviderCatalog.java
│   │   ├── ModelCatalog.java
│   │   ├── ChannelCatalog.java
│   │   └── ChannelModelCatalog.java
│   ├── gateway/
│   │   ├── ProviderCatalogGateway.java
│   │   └── ModelCatalogGateway.java
│   ├── service/
│   │   └── CatalogDomainService.java
│   └── enums/
│       ├── MetadataSource.java
│       └── CatalogState.java
├── protocol/
│   ├── ProtocolGateway.java
│   ├── ProtocolGatewayFactory.java
│   ├── ProtocolConverter.java
│   ├── ProtocolValidator.java
│   ├── StreamCallback.java
│   ├── StreamCallbackFactory.java
│   ├── ProtocolRequest.java
│   ├── ProtocolResponse.java
│   ├── StreamChunkResult.java
│   ├── OpenAIChatRequest.java
│   ├── OpenAIChatResponse.java
│   ├── AnthropicMessagesRequest.java
│   ├── AnthropicMessagesResponse.java
│   ├── OpenAIProtocolValidator.java
│   └── AnthropicProtocolValidator.java
├── service/
│   ├── ProviderDomainService.java
│   ├── ChannelDomainService.java
│   ├── ChannelCredentialDomainService.java
│   └── ModelSpecDomainService.java
├── gateway/
│   ├── ProviderGateway.java
│   ├── ChannelGateway.java
│   ├── ChannelCredentialGateway.java
│   ├── ModelSpecGateway.java
│   └── ConnectivityTester.java
├── valueobject/
│   ├── RoutingContext.java
│   └── ConnectivityTestResultVO.java
├── enums/
│   ├── ProviderState.java
│   ├── ChannelState.java
│   ├── CredentialState.java
│   ├── ChannelModelState.java
│   ├── ModelSpecState.java
│   ├── BillingMode.java
│   ├── RoutingStrategy.java
│   ├── Protocol.java
│   └── ProviderErrorType.java
└── exception/
    ├── ProviderException.java
    ├── ProtocolValidationException.java
    └── ChannelException.java
```

### infrastructure/supply

```
infrastructure/supply/
├── gateway/
│   ├── protocol/
│   │   ├── OpenAIProtocolGateway.java
│   │   ├── AnthropicProtocolGateway.java
│   │   └── ProtocolGatewayFactoryImpl.java
│   ├── ProviderGatewayImpl.java
│   ├── ChannelGatewayImpl.java
│   ├── ChannelCredentialGatewayImpl.java
│   ├── ModelSpecGatewayImpl.java
│   ├── ConnectivityTesterImpl.java
│   ├── ProviderCatalogGatewayImpl.java
│   ├── ModelCatalogGatewayImpl.java
│   ├── ChannelCatalogGatewayImpl.java
│   ├── ChannelModelCatalogGatewayImpl.java
│   └── ModelsDevDataGatewayImpl.java
└── database/
    ├── ModelSpecDo.java
    ├── ChannelDo.java
    ├── ChannelCredentialDo.java
    ├── ChannelModelDo.java
    ├── ChannelCatalogDo.java
    ├── ModelCatalogDo.java
    ├── ProviderCatalogDo.java
    └── ...Repository.java
```

### application 层

```
application/proxy/      ProxyApplicationService, ChannelRoutingService, SupplyRoutingService
application/channel/    ChannelService, ChannelCredentialService, dto/...
application/modelspec/  ModelSpecService, dto/...
application/catalog/    CatalogSyncService, ModelCatalogService, ProviderCatalogService, ChannelCatalogService
application/provider/   ProviderService, dto/...
```

## 请求处理数据流

```
用户请求 → AdapterController
  → 拦截器链（认证→限流→脱敏）
  → ProxyApplicationService
    → ChannelRoutingService.resolve(identity, model, protocol)
      → SupplyRoutingService.resolve(userApiKeyId, model, protocol)
        → UserApiKeyGateway / ChannelGateway / ModelSpecGateway
    → ProtocolGatewayFactory.create(protocol, endpoint, credential)
    → ProtocolGateway.chat(request) 或 chatStream(request, callback)
    → [ProtocolConverter] 跨协议转换
  → 响应返回
```
