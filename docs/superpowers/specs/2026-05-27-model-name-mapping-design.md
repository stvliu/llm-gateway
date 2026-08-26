# 模型名映射设计

## 背景

不同 AI 供应商对同一模型可能使用不同的模型 ID。用户通过 llm-gateway 发送请求时使用统一的规范化模型名，网关在出站时将模型名改写为上游供应商期望的值。

## 核心原则

> 用户面的模型名是 Model 的属性，上游面的模型名是 ChannelModel 的属性。每个渠道-模型关联声明自己发给上游的参数，Catalog 同步负责预填，用户按需微调。

| 层 | 名称 | 例子 | 谁关心 |
|----|------|------|--------|
| 用户面 | 用户传的 model 参数 | `deepseek-v4-flash` | 应用开发者 |
| 上游面 | 发给供应商的真实模型 ID | `deepseek-v4-flash-260425` | 供应商 |

## 设计目标

覆盖以下四种场景，无需在 Provider 层面引入映射规则：

| # | 场景 | 例子 | 用户面 | 上游面 |
|---|------|------|--------|--------|
| 1 | 跨供应商 ID 相同 | DeepSeek / 百度 / 火山 Coding | `deepseek-v4-flash` | `deepseek-v4-flash` |
| 2 | 供应商级有规律差异 | Anthropic → Bedrock | `claude-opus-4-7` | `anthropic.claude-opus-4-7` |
| 3 | 供应商级个别差异 | OpenAI → Azure chat-latest | `chat-latest` | `gpt-chat-latest` |
| 4 | 同供应商不同渠道 ID 不同 | 火山 Coding → 火山 PAYG | `deepseek-v4-flash` | `deepseek-v4-flash-260425` |

## 数据模型

### Model（替代当前的 ModelSpec）

命名从 `ModelSpec` 改为 `Model`，表示"模型规格"而非"规格定义"的语义，更符合全局注册表的定位。

```java
public class Model extends BaseEntity {
    private String modelName;        // 用户面标识，唯一。如 "deepseek-v4-flash"
    private String displayName;      // 展示名
    private String modelFamily;      // 模型系列
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Map<String, Boolean> capabilities;
    private List<String> modalities;
    private ModelState state;
}
```

- `modelName` 是用户请求时传的值，路由匹配的唯一键
- 不关联任何供应商，纯粹的模型规格定义
- 全局唯一约束：`UNIQUE(model_name)`
- 当前 `ModelSpec` 中的 `priority` / `weight` 字段在此次设计中**从 Model 中移除**。这两个字段用于路由权重和优先级，属于运行时调度参数，不应放在纯模型定义上。其功能由 `Channel` 和 `ChannelSelector` 的路由策略承载，无需迁移。

### ChannelModel（调整）

在现有基础上增加 `upstreamModelName` 字段：

```java
public class ChannelModel {
    private Long id;
    private Long channelId;
    private Long modelId;            // 关联 Model，而非 ModelSpec
    private String upstreamModelName; // 发给上游的模型名，null 表示与 model.modelName 相同
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    // ... 定价字段
    private ChannelModelState state;
}
```

- `upstreamModelName` 是每个渠道-模型关联的核心字段
- null 表示上游名 = Model.modelName
- 非 null 表示需要改写

### Provider

不承担模型名映射职责，无需额外字段。

## 数据流

### 路由匹配

```
用户请求 model=deepseek-v4-flash
  ↓
ModelMatcher: modelName = "deepseek-v4-flash"
  → 命中 Model { modelName: "deepseek-v4-flash" }
  → 通过 ChannelModel 找到关联的 Channel 列表
    ├── DeepSeek 渠道（upstreamModelName: null）
    ├── 百度渠道（upstreamModelName: null）
    ├── 火山 Coding Plan（upstreamModelName: null）
    └── 火山 PAYG（upstreamModelName: "deepseek-v4-flash-260425"）
  ↓
ChannelSelector: 按路由策略选出一个 Channel
```

### RoutingContext 类型变更

`RoutingContext` 中增加两个 String 字段（保持不可变对象轻量，不持有实体引用）：

```java
// 当前
record RoutingContext(
        Long channelId,
        Long channelEndpointId,
        String endpointUrl,
        Protocol upstreamProtocol,
        String providerApiKey,
        Integer timeout,
        boolean needsProtocolAdaptation
) {}

// 目标：增加模型名字段
record RoutingContext(
        Long channelId,
        Long channelEndpointId,
        String endpointUrl,
        Protocol upstreamProtocol,
        String providerApiKey,
        Integer timeout,
        boolean needsProtocolAdaptation,
        String modelName,          // 用户传入的模型名，对应 Model.modelName
        String upstreamModelName   // 上游模型名，null 表示与 modelName 相同
) {}
```

RoutingResolver 组装 RoutingContext 时从 Model 和 ChannelModel 取出对应值填入。出站调谐时直接读取 `ctx.upstreamModelName()` 获取上游模型名。

### 出站调谐

```
OutboundTuner.resolveModelName(ctx):
    upstreamName = ctx.upstreamModelName()
    
    if (upstreamName != null)
        return upstreamName;       // 渠道级覆盖
    else
        return ctx.modelName();    // 走默认（= Model.modelName）
```

四种场景的执行结果：

| 场景 | Model.modelName | ChannelModel.upstreamModelName | 出站值 |
|------|----------------|-------------------------------|--------|
| DeepSeek 官方 | `deepseek-v4-flash` | null | `deepseek-v4-flash` |
| Bedrock Opus 4.7 | `claude-opus-4-7` | `anthropic.claude-opus-4-7` | `anthropic.claude-opus-4-7` |
| Azure chat-latest | `chat-latest` | `gpt-chat-latest` | `gpt-chat-latest` |
| 火山 PAYG | `deepseek-v4-flash` | `deepseek-v4-flash-260425` | `deepseek-v4-flash-260425` |

## Catalog 严格对齐

Catalog 实体与业务实体必须保持命名一致，所有 `ModelSpec` / `modelSpecId` 引用同步改为 `Model` / `modelId`。

### Catalog 实体更名对照

| 当前目录实体 | 目标目录实体 | 当前表名 | 目标表名 |
|-------------|-------------|---------|---------|
| `ModelSpecCatalog` | `ModelCatalog` | `model_spec_catalogs` | `model_catalogs` |
| `ChannelModelCatalog` | 保留（关联 `modelId` 字段） | `channel_model_catalogs` | 保留 |
| `PlanModelCatalog` | 保留（关联 `modelId` 字段） | `plan_model_catalogs` | 保留 |

### Catalog 字段更名对照

| 实体 | 当前字段 | 目标字段 |
|------|---------|---------|
| `ModelSpecCatalog` | `providerModelId` | `modelName` |
| `ModelSpecCatalog` | `displayName` | 保留 |
| `ChannelModelCatalog` | `modelSpecId` | `modelId` |
| `PlanModelCatalog` | `modelSpecId` | `modelId` |

**列名策略**：Java 字段名和数据库列名同步修改。字段 `modelSpecId` → `modelId` 对应列名 `model_spec_id` → `model_id`，需 Flyway 迁移。字段 `providerModelId` → `modelName` 对应列名 `provider_model_id` → `model_name`，需 Flyway 迁移。Catalog 严格对齐要求命名在代码和数据两个层面保持一致。

**`upstreamModelName` 不属于 Catalog**。Catalog 定义"有什么东西可买"，`upstreamModelName` 定义"调用时发什么名"——后者是运行时参数，归 `ChannelModel` 独有。Catalog 物化时从 PlanCatalog 或 ChannelModelCatalog 的上下文（如 plan 的协议/端点类型）推算默认值预填到 `ChannelModel.upstreamModelName`，但 Catalog 实体本身不持有该字段。

### Catalog 同步链

Catalog → 业务实体的物化链关系：

```
ProviderCatalog   → Provider
ModelCatalog      → Model                 （原 ModelSpecCatalog → ModelSpec）
PlanCatalog       → Channel（含端点、凭证）
PlanModelCatalog  → ChannelModel           （原 modelSpecId → modelId）
ChannelCatalog    → Channel
ChannelModelCatalog → ChannelModel         （原 modelSpecId → modelId）
ChannelEndpointCatalog → ChannelEndpoint
```

`upstreamModelName` 的物化推算规则：

```
CatalogMaterializeService 中维护一份内置的模型名映射规则表（非 Provider 字段，而是服务级常量）：

Map<String, Map<String, String>> UPSTREAM_MODEL_NAME_RULES:
  key = providerCode
  value = Map<modelName, upstreamModelName>

当前预置规则:
  "aws-bedrock": {
    "claude-opus-4-7": "anthropic.claude-opus-4-7",
    "claude-sonnet-4-6": "anthropic.claude-sonnet-4-6",
    "claude-haiku-4-5": "anthropic.claude-haiku-4-5-20251001-v1:0",
    "claude-3-opus-20240229": "anthropic.claude-3-opus-20240229-v1:0",
    ...
  }
  "azure-openai": {
    "chat-latest": "gpt-chat-latest"
  }

物化流程:
  PlanCatalog / ChannelCatalog 物化时:
    1. 查出关联的 Provider（通过 plan 的 provider 关联）
    2. 在 UPSTREAM_MODEL_NAME_RULES 中查找该 providerCode
    3. 如果命中 → 填入映射值
    4. 如果未命中 → null
```

**为什么用内置映射表而不是 Provider 字段？** 因为这份映射是 Catalog 发布者（系统维护者）管理的知识，不是用户配置。用户不需要知道 Bedrock 需要加 `anthropic.` 前缀——这是 llm-gateway 内置的适配能力。映射表随系统版本更新，上游模型名变化时通过 Catalog 同步推送到已有 ChannelModel。
	
	**已知局限**：内置映射表仅覆盖 llm-gateway 官方维护的供应商（如 aws-bedrock、azure-openai）。用户通过 Channel 模板自建的自定义供应商不在覆盖范围内。对于自定义供应商，用户需在 ChannelModel 创建或编辑时手动填写 `upstreamModelName`，或保持 null 走默认值（= Model.modelName）。未来可按需将映射表开放为可插拔扩展点。

### Catalog 相关服务更名

| 当前 | 目标 |
|------|------|
| `ModelSpecCatalogGateway` | `ModelCatalogGateway` |
| `ModelCatalogGateway`（已存，指旧概念） | 已存不冲突 |
| `CatalogMaterializeService.materializeModelSpec()` | `materializeModel()` |
| `CatalogMaterializeService.findOrCreateModelSpec()` | `findOrCreateModel()` |

**关键规则**：Catalog 中不存在 `ModelSpec` / `modelSpecId` 的任何残留。所有涉及 Catalog 同步的代码、注释、变量名均需对齐。

### BuiltinCatalogLoader 同步更新

`BuiltinCatalogLoader`（`infrastructure/supply/catalog/loader/`）加载内置目录数据，包括 ModelSpec 预置列表。变更点：

- 内部数据结构中的 `providerModelId` → `modelName`
- 内部引用 `ModelSpecCatalog` → `ModelCatalog`
- 内置数据文件/常量中涉及 model 标识的字段名同步更新

## 来源同步

`upstreamModelName` 是 `ChannelModel` 的业务字段，不属于 Catalog。其值的来源分三种场景：

- **Catalog 物化时预填**：推算规则见上文 [Catalog 同步链](#catalog-同步链) 的 `UPSTREAM_MODEL_NAME_RULES` 物化流程
- **用户手动创建**：默认为 null（走默认值 = Model.modelName），用户按需编辑
- **用户手动编辑**：管理界面直接编辑 `ChannelModel.upstreamModelName`

对于 `upstreamModelName` 为 null 的 ChannelModel，出站时上游模型名 = Model.modelName。

## 用户面模型列表 API

网关提供兼容 OpenAI `/v1/models` 格式的模型发现接口，用户通过该接口确定可用的 model 参数。

### 请求

`GET /v1/models`

### 鉴权

需要 API Key。仅返回该 Key 可见的模型——即 API Key 关联的渠道所引用的 Model，去重后返回。

### 响应格式

```json
{
  "object": "list",
  "data": [
    {
      "id": "deepseek-v4-flash",
      "object": "model",
      "created": 1700000000,
      "owned_by": "system"
    },
    {
      "id": "deepseek-v4-pro",
      "object": "model",
      "created": 1700000000,
      "owned_by": "system"
    }
  ]
}
```

### 数据来源

`Model` 实体表中 `state = ACTIVE` 的记录。`id` 字段对应 `Model.modelName`。`created` 字段对应 `Model.createdAt` 的 epoch 秒数。`owned_by` 取值固定为 `"system"`，表示网关系统预置。用户可见范围 = API Key 可访问的渠道（Channel.state = ACTIVE）→ 渠道关联的 ChannelModel（ChannelModel.state = ACTIVE）→ 关联的 Model，去重。任一环节状态非 ACTIVE 则该模型对该 Key 不可见。

### 兼容性

仅保证 OpenAI `/v1/models` 格式的子集（`id`、`object`、`created`、`owned_by`），不做全量兼容。用户通过此接口获取可用 model 列表后，传入 `/v1/chat/completions` 的 `model` 参数。

## 前端 ChannelModelsPanel 中的 upstreamModelName

渠道展开行的 Models tab 中，关联模型对话框除了选择模型外，应增加 `upstreamModelName` 输入框：

| 字段 | 类型 | 说明 |
|------|------|------|
| 选择模型 | Select | 从全局 Model 列表中搜索 |
| 上游模型名 | Input | 可选，默认与 Model.modelName 相同 |

已有关联的表格行中展示 `upstreamModelName` 列和操作列：

| 列 | 说明 |
|----|------|
| 上游模型名 | 显示当前值；为 null 时显示灰色标签"默认（= Model.modelName）" |
| 操作 | 点击编辑按钮 → 行内变为可编辑 Input → 保存/取消 |

行内编辑流程：点击编辑图标 → 该行 `upstreamModelName` 字段变为 Input（预填当前值）→ 用户修改 → 点击确认图标保存（调用 PATCH `/api/channels/{channelId}/models/{id}` 更新 `upstreamModelName`）→ 点击取消图标恢复原值。用户在 Input 中清空并保存等价于设为 null（走默认值）。

`upstreamModelName` 为 null 时，出站走默认（= Model.modelName）。用户显式留空和设为 null 等价。

## 命名变更总表

| 当前 | 目标 | 范围说明 |
|------|------|---------|
| `ModelSpec`（实体） | `Model` | `domain/supply/entity/` |
| `ModelSpecDo`（JPA DO） | `ModelDo` | `infrastructure/.../dataobject/` |
| `model_specs`（表） | `models` | Flyway 迁移（V41）。V35 将旧 `models` 改名为 `model_specs`，本次将其改回 `models`，属反向操作。需确认无其他模块残留引用旧 `models` 表名。 |
| `ModelSpecState`（枚举） | `ModelState` | `domain/supply/enums/` |
| `ModelSpecGateway` | `ModelGateway` | `domain/supply/gateway/` |
| `ModelSpecGatewayImpl` | `ModelGatewayImpl` | `infrastructure/supply/gateway/` |
| `ModelSpecRepository` | `ModelRepository` | `infrastructure/.../repository/` |
| `ModelSpecService` | `ModelService` | `application/model/` |
| `ModelSpecServiceImpl` | `ModelServiceImpl` | `application/model/` |
| `ModelSpecController` | `ModelController` | `adapter/api/` |
| `ModelSpecCreateRequest` | `ModelCreateRequest` | `application/model/dto/` |
| `ModelSpecUpdateRequest` | `ModelUpdateRequest` | `application/model/dto/` |
| `ModelSpecQueryRequest` | `ModelQueryRequest` | `application/model/dto/` |
| `ModelSpecResponse` | `ModelResponse` | `application/model/dto/` |
| `providerModelId`（字段） | `modelName` | Model 实体中的字段名 |
| `ModelSpecCatalog` | `ModelCatalog` | `domain/supply/catalog/entity/` |
| `model_spec_catalogs`（表） | `model_catalogs` | Flyway 迁移 |
| `ModelSpecCatalogGateway` | `ModelCatalogGateway` | `domain/supply/catalog/gateway/` |
| `ModelSpecCatalogGatewayImpl` | `ModelCatalogGatewayImpl` | `infrastructure/supply/gateway/` |
| `modelSpecId`（字段+列） | `modelId` | ChannelModel、ChannelModelCatalog、PlanModelCatalog 中的关联字段和列名 |
| `ModelSpecServiceImplTest` | `ModelServiceImplTest` | 后端测试类 |
| `ModelSpecControllerTest` | `ModelControllerTest` | 后端测试类 |
| `modelSpec.ts`（前端类型） | `model.ts` | `gateway-console/src/types/` |
| `ModelSpec`（前端 TS 接口） | `Model` | TS 接口名，字段 `providerModelId` → `modelName` |
| `useModelSpecs.ts`（前端 hook） | `useModels.ts` | `gateway-console/src/services/query/` |
| `modelSpec.ts`（前端 API） | `model.ts` | `gateway-console/src/services/api/` |

**命名冲突注意**：`domain/model/` 旧包已在重构中清理完毕，当前无残留引用。新 `Model` 类位于 `domain/supply/entity/Model.java`，包路径为 `com.codingas.gateway.domain.supply.entity.Model`，不存在命名冲突。

## 与当前代码的关系

当前实现（`refactor/template-to-metadata` 分支）已将 ModelSpec → Model 重命名基本完成。已在之前迭代中完成的部分：

| 范围 | 状态 | 说明 |
|------|------|------|
| 实体 `ModelSpec` → `Model` | ✅ 已完成 | `domain/supply/entity/Model.java` |
| DO `ModelSpecDo` → `ModelDo` | ✅ 已完成 | 映射表 `models`（V42 Flyway） |
| 枚举 `ModelSpecState` → `ModelState` | ✅ 已完成 | |
| Gateway / Repository | ✅ 已完成 | `ModelGateway`, `ModelRepository` |
| Service | ✅ 已完成 | `ModelServiceImpl`（原 `ModelSpecServiceImpl`） |
| Controller / DTO | ✅ 已完成 | `ModelController`, `ModelCreateRequest/Response` 等 |
| Catalog 对齐 | ✅ 已完成 | `ModelSpecCatalog` → `ModelCatalog`, `modelSpecId` → `modelId` |
| `providerModelId` → `modelName` | ✅ 已完成 | Model 实体及列名 |
| `domain/model/` 旧包清理 | ✅ 已完成 | 当前无残留引用 |
| SyncResult 字段 `ModelSpecs` → `Models` | ✅ 已完成 | `ModelsDevSyncClient.java` |
| `uk_cm_channel_model_spec` 约束名 | ✅ 已完成 | 与 V42 实际 DB 状态一致 |

当前分支本次迭代完成的变更：

| 变更 | 工作量 | 说明 | 提交 |
|------|--------|------|------|
| OutboundTuner 模型名改写 | 小 | RoutingContext 增加 String 字段，Tuner 从实体注入改为不可变对象读取 | `c35f8bf` |
| Catalog 预填 upstreamModelName | 小 | CatalogMaterializeService 增加 UPSTREAM_MODEL_NAME_RULES 内置映射表 + materializePlan 中预填逻辑 | `19276ed` |
| 用户面 `/v1/models` 接口 | 小 | 新建 ModelDiscoveryController，兼容 OpenAI 模型列表 API | `684a4db` |

> **注**：ChannelModel 的 `upstreamModelName` 字段已在实体、DO、Service、Controller、DTO 中完成实现，包括 `ChannelModelDo.upstream_model_name` 列（已在 V42 Flyway 中添加）。