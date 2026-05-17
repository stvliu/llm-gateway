# 模型路由系统设计

## Context

当前 LLM-Gateway 的模型与供应商是 1:1 硬绑定关系：

```
用户请求 model="gpt-4o"
    │
    ▼
Model.provider_id → 唯一 Provider → 唯一 Adapter（静态 apiKey）
```

**问题**：
1. **无法表达"一个模型多供应商"**：`UNIQUE(provider_id, provider_model_id)` 约束导致同一个 `gpt-4o` 只能关联一个供应商，无法支持"OpenAI 直连、Azure、国内代理"多渠道
2. **API Key 静态配置**：适配器的 `apiKey` 在构造时注入，无法从数据库动态选择
3. **路由策略未生效**：`RouteGroup.RoutingStrategy` 已定义但未实现

**借鉴 APIPark**：通过 `Balance` 表表达"模型-供应商"的多对多关系，用户只传模型名，系统按策略选择渠道。

---

## 设计方案

### 核心思路：复用 models 表作为渠道表

同一个 `provider_model_id` 允许多条 Model 记录，每条记录关联不同 `provider_id`，代表一个渠道：

```
用户请求 model="gpt-4o"
    │
    ▼
SELECT * FROM models WHERE provider_model_id = 'gpt-4o' AND state = 'ACTIVE' ORDER BY priority
    │
    ├── id=1 | provider_id=1(OpenAI) | priority=10 | weight=60
    ├── id=2 | provider_id=2(Azure)  | priority=20 | weight=30
    └── id=3 | provider_id=3(代理)   | priority=30 | weight=10
            │
            │ 按策略选渠道 → provider_id
            ▼
    SELECT * FROM provider_api_keys WHERE provider_id = ? AND state = 'ACTIVE'
            │
            │ 按策略选 Key
            ▼
    AdapterBuilderFactory.createAdapter(type, baseUrl, apiKey)
```

**优势**：
- 不需要新表，改动最小
- 每条 Model 记录天然带价格/能力，不同渠道可以有不同价格
- 现有 `UNIQUE(provider_id, provider_model_id)` 约束刚好满足需求

### 路由策略

| 策略 | 选择逻辑 | 适用场景 |
|------|---------|---------|
| **FAILOVER** | 按 `priority` 升序选第一个（默认） | 主备架构 |
| **WEIGHTED** | 按 `weight` 加权随机 | 负载均衡 |
| **RANDOM** | 随机选择 | 测试环境 |
| **COST_OPTIMIZED** | 选 `input_price` 最低的 | 成本敏感（Phase 2） |
| **LATENCY_OPTIMIZED** | 选延迟最低的 | 实时性要求高（Phase 2） |

### API Key 选择策略

```
1. 优先 isDefault=true 的 Key
2. 否则按 weight 加权随机选择
3. 无可用 Key 返回 null（抛异常）
```

---

## 后端变更

### 1. 数据库迁移

**文件**: `V15__add_model_routing_fields.sql`

```sql
-- 添加路由字段
ALTER TABLE models ADD COLUMN priority INT NOT NULL DEFAULT 100;
ALTER TABLE models ADD COLUMN weight INT NOT NULL DEFAULT 100;

-- 添加索引优化查询
CREATE INDEX idx_models_provider_model_id ON models(provider_model_id);
CREATE INDEX idx_models_provider_model_id_state ON models(provider_model_id, state);
```

**说明**：现有 `UNIQUE(provider_id, provider_model_id)` 约束保持不变，天然支持同一模型多供应商。

### 2. 领域层变更

#### 2.1 Model 实体新增字段

**文件**: `domain/model/entity/Model.java`

```java
private Integer priority = 100;  // 渠道优先级（越小越优先）
private Integer weight = 100;    // 渠道权重（加权随机用）
```

#### 2.2 RoutingContext 值对象

**文件**: `domain/proxy/entity/RoutingContext.java`

```java
/**
 * 路由决策结果
 */
public record RoutingContext(
    Model model,           // 选中的渠道
    Provider provider,     // 关联的供应商
    ProviderApiKey apiKey, // 选中的 API Key
    String providerModelId // 实际发给供应商的模型标识
) {}
```

#### 2.3 ModelGateway 接口扩展

**文件**: `domain/model/gateway/ModelGateway.java`

```java
// 新增：查找同名模型的所有活跃渠道
List<Model> findActiveByProviderModelId(String providerModelId);

// 新增：查找同名模型的所有记录
List<Model> findAllByProviderModelId(String providerModelId);

// 保留：向后兼容，返回第一个匹配
Optional<Model> findByProviderModelId(String providerModelId);
```

#### 2.4 ApiKeySelectionService

**文件**: `domain/model/service/ApiKeySelectionService.java`

```java
/**
 * API Key 动态选择服务
 */
@Service
public class ApiKeySelectionService {
    
    private final ProviderApiKeyGateway providerApiKeyGateway;

    /**
     * 为指定供应商选择一个可用的 API Key
     * 
     * @param providerId 供应商ID
     * @return 选中的 API Key，无可用 Key 返回 null
     */
    public ProviderApiKey selectApiKey(Long providerId) {
        // 1. 优先选择 isDefault=true 的 Key
        // 2. 否则按 weight 加权随机
    }
}
```

#### 2.5 ModelDomainService 扩展

**文件**: `domain/model/service/ModelDomainService.java`

```java
/**
 * 查找模型的所有活跃渠道
 */
public List<Model> findActiveChannels(String providerModelId) {
    return modelGateway.findActiveByProviderModelId(providerModelId);
}
```

### 3. 应用层变更

#### 3.1 ChannelRoutingService（核心）

**文件**: `application/proxy/ChannelRoutingService.java`

```java
/**
 * 渠道路由服务
 * 
 * 职责：模型名 → 渠道列表 → 策略选择 → Provider → API Key
 */
@Service
public class ChannelRoutingService {
    
    private final ModelDomainService modelDomainService;
    private final ProviderGateway providerGateway;
    private final ApiKeySelectionService apiKeySelectionService;

    /**
     * 解析路由
     * 
     * @param modelName 用户传入的模型名（如 "gpt-4o"）
     * @param strategy 路由策略
     * @return 路由上下文
     */
    public RoutingContext resolve(String modelName, RoutingStrategy strategy) {
        // 1. 查找活跃渠道
        List<Model> channels = modelDomainService.findActiveChannels(modelName);
        
        // 2. 无渠道时 fallback 到旧逻辑
        if (channels.isEmpty()) {
            return resolveLegacy(modelName);
        }
        
        // 3. 单渠道直接返回
        if (channels.size() == 1) {
            return buildContext(channels.get(0));
        }
        
        // 4. 多渠道按策略选择
        Model selected = selectChannel(channels, strategy);
        return buildContext(selected);
    }
    
    private Model selectChannel(List<Model> channels, RoutingStrategy strategy) {
        return switch (strategy) {
            case FAILOVER -> channels.get(0); // 已按 priority ASC 排序
            case WEIGHTED -> selectByWeight(channels);
            case RANDOM -> channels.get(ThreadLocalRandom.current().nextInt(channels.size()));
            default -> channels.get(0);
        };
    }
    
    private RoutingContext buildContext(Model model) {
        Provider provider = providerGateway.findById(model.getProviderId()).orElseThrow(...);
        ProviderApiKey apiKey = apiKeySelectionService.selectApiKey(provider.getId());
        if (apiKey == null) {
            throw new IllegalStateException("No available API key for provider: " + provider.getName());
        }
        return new RoutingContext(model, provider, apiKey, model.getProviderModelId());
    }
}
```

#### 3.2 ProxyServiceImpl 重写

**文件**: `application/proxy/ProxyServiceImpl.java`

```java
@Service
public class ProxyServiceImpl implements ProxyService {

    private final ChannelRoutingService channelRoutingService;
    private final AdapterBuilderFactory adapterBuilderFactory;
    private final StreamCallbackFactory streamCallbackFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public LLMResponse proxy(LLMRequest request, RoutingStrategy strategy) {
        // 1. 路由解析
        RoutingContext ctx = channelRoutingService.resolve(request.getModel(), strategy);

        // 2. 创建临时适配器（动态 apiKey + baseUrl）
        LLMAdapter adapter = adapterBuilderFactory.createAdapter(
            ctx.provider().getType(),
            ctx.provider().getBaseUrl(),
            ctx.apiKey().getApiKey(),
            ctx.provider().getTimeout() != null ? ctx.provider().getTimeout() / 1000 : 30
        );

        // 3. 执行请求
        LLMResponse response = adapter.chat(request);

        // 4. 记录用量
        publishTokenUsedEvent(request, response);

        return response;
    }
    
    // proxyStream() 同理
}
```

### 4. 基础设施层变更

#### 4.1 ModelDo 新增字段

**文件**: `infrastructure/model/gateway/database/dataobject/ModelDo.java`

```java
@Column(name = "priority")
private Integer priority = 100;

@Column(name = "weight")
private Integer weight = 100;
```

#### 4.2 ModelRepository 新增查询

**文件**: `infrastructure/model/gateway/database/ModelRepository.java`

```java
// 查找同名模型的所有记录（按 priority 排序）
@Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider " +
       "WHERE m.providerModelId = :providerModelId ORDER BY m.priority ASC")
List<ModelDo> findAllByProviderModelId(@Param("providerModelId") String providerModelId);

// 查找同名模型的活跃记录
@Query("SELECT m FROM ModelDo m LEFT JOIN FETCH m.provider " +
       "WHERE m.providerModelId = :providerModelId AND m.state = 'ACTIVE' ORDER BY m.priority ASC")
List<ModelDo> findActiveByProviderModelId(@Param("providerModelId") String providerModelId);
```

#### 4.3 ModelGatewayImpl 实现新方法

**文件**: `infrastructure/model/gateway/ModelGatewayImpl.java`

- 实现 `findActiveByProviderModelId()` 和 `findAllByProviderModelId()`
- `toEntity()` / `toDo()` 同步 priority/weight 映射

### 5. DTO 变更

#### 5.1 ModelResponse

**文件**: `application/model/dto/ModelResponse.java`

```java
private Integer priority;  // 新增
private Integer weight;    // 新增
```

#### 5.2 ModelCreateRequest

**文件**: `application/model/dto/ModelCreateRequest.java`

```java
private Integer priority;  // 新增，可选
private Integer weight;    // 新增，可选
```

---

## 前端变更

### 1. 类型定义

**文件**: `gateway-console/src/types/model.ts`

```typescript
interface Model {
  // ...existing
  priority: number;   // 新增
  weight: number;     // 新增
}
```

### 2. UI 变更

- **模型列表页**：同名模型按 `providerModelId` 分组展示，显示渠道数量
- **模型详情**：新增"渠道管理"视图，展示同名模型的所有渠道
- **创建模型**：新增 priority/weight 输入，当供应商下已存在同名模型时提示"将作为新渠道添加"

---

## 关键复用

| 组件 | 文件 | 用途 |
|------|------|------|
| `AdapterBuilderFactory` | `infrastructure/proxy/gateway/rpc/AdapterBuilderFactory.java` | 动态创建适配器，支持运行时传入 apiKey/baseUrl |
| `ProviderApiKeyGateway` | `domain/model/gateway/ProviderApiKeyGateway.java` | 已有 `findActiveKeysByProviderId()` / `findDefaultKeyByProviderId()` |
| `ApiKeyEncryptionDomainService` | 基础设施层 | API Key 解密 |

---

## 向后兼容

1. **查询兼容**：`ModelGateway.findByProviderModelId()` 保留，返回 `Optional<Model>`（取第一个）
2. **单渠道行为不变**：无多渠道时，行为与改造前完全一致
3. **API 格式不变**：`/v1/chat/completions` 和 `/v1/messages` 端点和请求格式不变
4. **适配器保留**：`AdapterRegistry` 和 `LLMAdapterConfig` 保留，用于连通性测试

---

## 验证方式

### 单元测试

- `ChannelRoutingServiceTest`：渠道选择策略（FAILOVER/WEIGHTED/RANDOM）
- `ApiKeySelectionServiceTest`：API Key 选择策略（默认Key/加权随机）
- `ModelGatewayImplTest`：priority/weight 字段映射

### 手动验证

1. 创建模型 `gpt-4o`（provider=OpenAI, priority=10）
2. 再创建模型 `gpt-4o`（provider=Azure, priority=20）
3. 发送 `POST /v1/chat/completions` body `{"model":"gpt-4o",...}`
4. 验证请求路由到 priority=10 的渠道
5. 禁用高优先级模型，验证故障转移到 priority=20 的渠道

---

## Phase 2 展望

1. **故障转移**：请求失败时自动尝试下一个渠道
2. **COST_OPTIMIZED**：按 `input_price` 选择最便宜渠道
3. **LATENCY_OPTIMIZED**：按运行时延迟统计选择最快渠道
4. **API Key 健康管理**：熔断器标记不健康的 Key，自动禁用
5. **路由组 UI**：前端路由组管理页面
