# 供应域合并方案 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将供应域从 14 个实体合并为 8 个，消除 Catalog/Runtime 双轨数据冗余，ChannelModel 重命名为 ModelInstance

**Architecture:** Provider/Model 合并 Catalog 字段为统一实体；ChannelModel 重命名为 ModelInstance 并删除定价字段；Channel 删除 priority/weight 下沉到 ModelInstance；物化概念替换为 ChannelProvisionService；删除 source/syncedAt 和衍生目录实体

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, PostgreSQL, React/TypeScript

---

## 阶段总览

本计划按依赖顺序分为 8 个大任务，每个大任务独立可提交：

| Task | 内容 | 预计文件数 |
|------|------|-----------|
| 1 | 删除 CatalogSource/ProviderType 枚举 + 清理引用 | ~15 |
| 2 | Provider 合并（ProviderCatalog → Provider） | ~10 |
| 3 | Model 合并（ModelCatalog → Model） | ~10 |
| 4 | ChannelModel → ModelInstance（重命名+删除定价+新增覆盖字段） | ~15 |
| 5 | Channel 精简 + PlanCatalog 精简 + 衍生目录删除 | ~15 |
| 6 | 服务层重构（删除旧服务+创建新服务） | ~15 |
| 7 | Controller 层重构 + 路由适配 + BuiltinDataLoader | ~15 |
| 8 | 数据库迁移 + 前端适配 | ~15 |

---

### Task 1: 删除 CatalogSource/ProviderType 枚举 + 清理引用

**Files:**
- Delete: `gateway-boot/.../domain/supply/catalog/enums/CatalogSource.java`
- Delete: `gateway-boot/.../domain/supply/catalog/enums/ProviderType.java`
- Modify: 所有引用这两个枚举的文件（实体、Gateway、Service、DO、Repository）

- [ ] **Step 1: 搜索所有引用 CatalogSource 的文件**

Run: `grep -rl "CatalogSource" gateway-boot/src/main/java/`
预期返回约 15 个文件

- [ ] **Step 2: 搜索所有引用 ProviderType 的文件**

Run: `grep -rl "ProviderType" gateway-boot/src/main/java/`
预期返回约 5 个文件

- [ ] **Step 3: 从所有实体中删除 CatalogSource/ProviderType 字段**

需要修改的领域实体：
- `ProviderCatalog.java` — 删除 `source` 和 `providerType` 字段
- `ModelCatalog.java` — 删除 `source` 字段
- `PlanCatalog.java` — 删除 `source` 字段
- `PlanModelCatalog.java` — 删除 `source` 字段
- `ChannelCatalog.java` — 删除 `source` 字段
- `ChannelModelCatalog.java` — 删除 `source` 字段

- [ ] **Step 4: 从所有 Gateway 接口中删除 CatalogSource/ProviderType 相关方法**

需要修改的接口：
- `ProviderCatalogGateway.java` — 删除 `findBySource`、`findByProviderType`、`findBySourceExcludingKeys`
- `ModelCatalogGateway.java` — 删除 `findBySource`、`findBySourceExcludingKeys`
- `PlanCatalogGateway.java` — 删除 `findBySource`、`findBySourceExcludingKeys`
- `PlanModelCatalogGateway.java` — 删除 `findBySource`、`findBySourceExcludingKeys`

- [ ] **Step 5: 从所有 GatewayImpl 中删除对应实现**

- `ProviderCatalogGatewayImpl.java` — 删除 `findBySource`、`findByProviderType`、`findBySourceExcludingKeys` 实现
- `ModelCatalogGatewayImpl.java` — 删除 `findBySource`、`findBySourceExcludingKeys` 实现
- `PlanCatalogGatewayImpl.java` — 删除 `findBySource`、`findBySourceExcludingKeys` 实现
- `PlanModelCatalogGatewayImpl.java` — 删除 `findBySource`、`findBySourceExcludingKeys` 实现

- [ ] **Step 6: 从所有 JPA DO 中删除 CatalogSource/ProviderType 列映射**

- `ProviderCatalogDo.java` — 删除 `source`、`providerType` 列
- `ModelCatalogDo.java` — 删除 `source` 列
- `PlanCatalogDo.java` — 删除 `source` 列
- `PlanModelCatalogDo.java` — 删除 `source` 列

- [ ] **Step 7: 从所有 JPA Repository 中删除 CatalogSource/ProviderType 查询方法**

- `ProviderCatalogRepository.java` — 删除 `findBySource`、`findByProviderType`
- `ModelCatalogRepository.java` — 删除 `findBySource`
- `PlanCatalogRepository.java` — 删除 `findBySource`
- `PlanModelCatalogRepository.java` — 删除 `findBySource`

- [ ] **Step 8: 从 CatalogDomainService 中删除 source 优先级逻辑**

`CatalogDomainService.java` — 删除 `canOverride` 逻辑，简化 upsert 为"存在则更新，不存在则创建"

- [ ] **Step 9: 从 BuiltinCatalogLoader 中删除 source 赋值**

`BuiltinCatalogLoader.java` — 所有 `setSource(CatalogSource.BUILTIN)` 调用删除

- [ ] **Step 10: 从 ModelsDevSyncClient 中删除 source 赋值和标记废弃逻辑**

`ModelsDevSyncClient.java` — 删除 `setSource` 和 `markDeprecated` 逻辑

- [ ] **Step 11: 从 CatalogServiceImpl 中删除 source 相关参数**

`CatalogServiceImpl.java` — 删除 DTO 中的 `source` 字段赋值

- [ ] **Step 12: 从 CatalogController 中删除 providerType 参数**

`CatalogController.java` — `listProviderCatalogs` 方法删除 `providerType` 参数

- [ ] **Step 13: 从 DTO 中删除 source/providerType 字段**

- `ProviderCatalogResponse.java` — 删除 `source`、`providerType` 字段
- `PlanCatalogResponse.java` — 删除 `source` 字段
- `ModelCatalogResponse.java` — 删除 `source` 字段
- `PlanDetailResponse.java` — 删除 `source` 字段

- [ ] **Step 14: 删除枚举文件**

Delete: `CatalogSource.java`
Delete: `ProviderType.java`

- [ ] **Step 15: 编译验证**

Run: `./mvnw compile -pl gateway-boot -q`
预期：BUILD SUCCESS

- [ ] **Step 16: 提交**

```bash
git add -A
git commit -m "refactor: 删除 CatalogSource/ProviderType 枚举及相关字段"
```

---

### Task 2: Provider 合并（ProviderCatalog → Provider）

**Files:**
- Delete: `gateway-boot/.../domain/supply/catalog/entity/ProviderCatalog.java`
- Delete: `gateway-boot/.../domain/supply/catalog/gateway/ProviderCatalogGateway.java`
- Delete: `gateway-boot/.../infrastructure/supply/catalog/gateway/ProviderCatalogGatewayImpl.java`
- Delete: `gateway-boot/.../infrastructure/supply/catalog/database/repository/ProviderCatalogRepository.java`
- Delete: `gateway-boot/.../infrastructure/supply/catalog/database/dataobject/ProviderCatalogDo.java`
- Modify: `Provider.java` — 确认字段完整
- Modify: `ProviderGateway.java` — 新增方法
- Modify: `ProviderGatewayImpl.java` — 实现新增方法
- Modify: 所有引用 ProviderCatalogGateway 的文件

- [ ] **Step 1: 确认 Provider 实体字段完整**

读取 `Provider.java`，确认包含：code, name, logoUrl, websiteUrl, description, apiDocUrl, priority, state

- [ ] **Step 2: ProviderGateway 新增方法**

在 `ProviderGateway.java` 中新增：
- `boolean existsByCode(String code)`
- `List<Provider> findByKeyword(String keyword)`

- [ ] **Step 3: ProviderGatewayImpl 实现新增方法**

在 `ProviderGatewayImpl.java` 中实现 `existsByCode` 和 `findByKeyword`，并在 `ProviderRepository` 中添加对应 JPA 查询

- [ ] **Step 4: 搜索所有引用 ProviderCatalogGateway 的文件**

Run: `grep -rl "ProviderCatalogGateway" gateway-boot/src/main/java/`

- [ ] **Step 5: 替换所有 ProviderCatalogGateway 引用**

主要涉及：
- `CatalogMaterializeService.java` — 重写为使用 ProviderGateway
- `CatalogServiceImpl.java` — 重写为使用 ProviderGateway
- `CatalogDomainService.java` — 重写为使用 ProviderGateway
- `BuiltinCatalogLoader.java` — 重写为使用 ProviderGateway
- `ModelsDevSyncClient.java` — 重写为使用 ProviderGateway

- [ ] **Step 6: 删除 ProviderCatalog 相关文件**

```bash
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity/ProviderCatalog.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/gateway/ProviderCatalogGateway.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/catalog/gateway/ProviderCatalogGatewayImpl.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/catalog/database/repository/ProviderCatalogRepository.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/catalog/database/dataobject/ProviderCatalogDo.java
```

- [ ] **Step 7: 编译验证**

Run: `./mvnw compile -pl gateway-boot -q`
预期：BUILD SUCCESS（可能有其他文件仍引用已删除类，需逐个修复）

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "refactor: 合并 ProviderCatalog 到 Provider，删除 ProviderCatalog 相关类"
```

---

### Task 3: Model 合并（ModelCatalog → Model）

**Files:**
- Modify: `Model.java` — 新增 knowledgeCutoff 字段，capabilities/modalities 统一为结构化类型
- Delete: `gateway-boot/.../domain/supply/catalog/entity/ModelCatalog.java`
- Delete: `gateway-boot/.../domain/supply/catalog/gateway/ModelCatalogGateway.java`
- Delete: `gateway-boot/.../infrastructure/supply/catalog/gateway/ModelCatalogGatewayImpl.java`
- Delete: `gateway-boot/.../infrastructure/supply/catalog/database/repository/ModelCatalogRepository.java`
- Delete: `gateway-boot/.../infrastructure/supply/catalog/database/dataobject/ModelCatalogDo.java`
- Modify: `ModelGateway.java` — 新增方法
- Modify: `ModelGatewayImpl.java` — 实现新增方法
- Modify: 所有引用 ModelCatalogGateway 的文件

- [ ] **Step 1: Model 实体新增 knowledgeCutoff 字段**

在 `Model.java` 中新增 `private String knowledgeCutoff;`

- [ ] **Step 2: ModelGateway 新增方法**

在 `ModelGateway.java` 中新增：
- `boolean existsByModelName(String modelName)`
- `List<Model> findByKeyword(String keyword)`
- `List<Model> findByCapability(String capability)`

- [ ] **Step 3: ModelGatewayImpl 实现新增方法**

在 `ModelGatewayImpl.java` 中实现，并在 `ModelRepository` 中添加对应 JPA 查询

- [ ] **Step 4: 搜索所有引用 ModelCatalogGateway 的文件**

Run: `grep -rl "ModelCatalogGateway" gateway-boot/src/main/java/`

- [ ] **Step 5: 替换所有 ModelCatalogGateway 引用**

主要涉及：
- `CatalogMaterializeService.java` — 重写为使用 ModelGateway
- `CatalogServiceImpl.java` — 重写为使用 ModelGateway
- `CatalogDomainService.java` — 重写为使用 ModelGateway
- `BuiltinCatalogLoader.java` — 重写为使用 ModelGateway
- `ModelsDevSyncClient.java` — 重写为使用 ModelGateway

- [ ] **Step 6: 删除 ModelCatalog 相关文件**

```bash
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity/ModelCatalog.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/gateway/ModelCatalogGateway.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/catalog/gateway/ModelCatalogGatewayImpl.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/catalog/database/repository/ModelCatalogRepository.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/catalog/database/dataobject/ModelCatalogDo.java
```

- [ ] **Step 7: 编译验证**

Run: `./mvnw compile -pl gateway-boot -q`

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "refactor: 合并 ModelCatalog 到 Model，新增 knowledgeCutoff 字段"
```

---

### Task 4: ChannelModel → ModelInstance

**Files:**
- Create: `ModelInstance.java` (领域实体)
- Create: `ModelInstanceGateway.java` (Gateway 接口)
- Create: `ModelInstanceGatewayImpl.java` (Gateway 实现)
- Create: `ModelInstanceDo.java` (JPA DO)
- Create: `ModelInstanceRepository.java` (JPA Repository)
- Delete: `ChannelModel.java` 及相关文件
- Modify: 所有引用 ChannelModel/ChannelModelGateway 的文件

- [ ] **Step 1: 创建 ModelInstance 领域实体**

在 `domain/supply/entity/ModelInstance.java` 创建新实体，包含字段：channelId, modelId, upstreamModelName, capabilitiesOverride (Map<String,Boolean>), contextWindowOverride (Integer), priority (Integer, 默认100), weight (Integer, 默认100), quotaLimit (Long), state (ChannelModelState)

- [ ] **Step 2: 创建 ModelInstanceGateway 接口**

在 `domain/supply/gateway/ModelInstanceGateway.java` 创建接口，包含：save, findById, findByChannelId, findActiveByChannelId, findActiveByModelId, findActiveByModelIdOrderByPriority, existsByChannelIdAndModelId, saveAll, deleteById

- [ ] **Step 3: 创建 ModelInstanceDo JPA 实体**

在 `infrastructure/supply/gateway/database/dataobject/ModelInstanceDo.java` 创建 JPA 实体，表名 `model_instances`，字段映射包括 capabilities_override (jsonb)

- [ ] **Step 4: 创建 ModelInstanceRepository JPA 接口**

在 `infrastructure/supply/gateway/database/repository/ModelInstanceRepository.java` 创建 JPA Repository，包含 `findActiveByModelIdOrderByPriority` 查询

- [ ] **Step 5: 创建 ModelInstanceGatewayImpl 基础设施实现**

在 `infrastructure/supply/gateway/ModelInstanceGatewayImpl.java` 创建实现，包含 Entity ↔ DO 双向转换

- [ ] **Step 6: 搜索所有引用 ChannelModel/ChannelModelGateway 的文件**

Run: `grep -rl "ChannelModel\|channelModel" gateway-boot/src/main/java/ | grep -v "ChannelModelCatalog"`

- [ ] **Step 7: 替换所有 ChannelModel → ModelInstance 引用**

主要涉及：
- `RoutingResolver.java` — ChannelModelGateway → ModelInstanceGateway
- `ChannelSelector.java` — 重命名为 InstanceSelector，ChannelModel → ModelInstance
- `OutboundTuner.java` — 无逻辑变化，类型替换
- `ModelDiscoveryService.java` — ChannelModelGateway → ModelInstanceGateway
- `ChannelModelService.java` → `ModelInstanceService.java` — 重命名
- `ChannelModelServiceImpl.java` → `ModelInstanceServiceImpl.java` — 重命名
- `ChannelController.java` — ChannelModelService → ModelInstanceService
- `ChatDispatchServiceImpl.java` — 如有引用则替换
- `CatalogMaterializeService.java` — ChannelModel → ModelInstance
- 所有 DTO 中的 ChannelModel 引用

- [ ] **Step 8: 删除 ChannelModel 相关文件**

```bash
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ChannelModel.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelModelGateway.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ChannelModelGatewayImpl.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/dataobject/ChannelModelDo.java
rm gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/repository/ChannelModelRepository.java
rm gateway-boot/src/main/java/com/codingas/gateway/application/channel/ChannelModelService.java
rm gateway-boot/src/main/java/com/codingas/gateway/application/channel/ChannelModelServiceImpl.java
```

- [ ] **Step 9: 编译验证**

Run: `./mvnw compile -pl gateway-boot -q`

- [ ] **Step 10: 提交**

```bash
git add -A
git commit -m "refactor: 重命名 ChannelModel 为 ModelInstance，删除定价字段，新增覆盖字段"
```

---

### Task 5: Channel 精简 + PlanCatalog 精简 + 衍生目录删除

**Files:**
- Modify: `Channel.java` — 删除 priority/weight
- Modify: `ChannelDo.java` — 删除 priority/weight 列映射
- Modify: `PlanCatalog.java` — 删除 source/syncedAt
- Modify: `PlanModelCatalog.java` — 删除 source/syncedAt
- Modify: PlanCatalogDo/PlanModelCatalogDo — 删除对应列
- Modify: PlanCatalogGateway/PlanModelCatalogGateway — 删除 source 方法
- Delete: ChannelCatalog, ChannelModelCatalog, ChannelEndpointCatalog 及相关文件

- [ ] **Step 1: Channel 实体删除 priority/weight**

在 `Channel.java` 中删除 `priority` 和 `weight` 字段

- [ ] **Step 2: ChannelDo 删除 priority/weight 列映射**

在 `ChannelDo.java` 中删除 `priority` 和 `weight` 的 `@Column` 映射

- [ ] **Step 3: ChannelGatewayImpl 删除 priority/weight 转换**

在 `ChannelGatewayImpl.java` 的 Entity ↔ DO 转换中删除 priority/weight

- [ ] **Step 4: Channel DTO 删除 priority/weight**

在 ChannelCreateRequest/ChannelUpdateRequest/ChannelResponse 等 DTO 中删除 priority/weight

- [ ] **Step 5: PlanCatalog/PlanModelCatalog 删除 source/syncedAt**

在 `PlanCatalog.java` 和 `PlanModelCatalog.java` 中删除 `source` 和 `syncedAt` 字段

- [ ] **Step 6: PlanCatalogDo/PlanModelCatalogDo 删除对应列**

在 DO 类中删除 `source` 和 `synced_at` 的 `@Column` 映射

- [ ] **Step 7: PlanCatalogGateway/Impl 删除 source 方法**

删除 `findBySource`、`findBySourceExcludingKeys` 方法及实现

- [ ] **Step 8: 删除衍生目录实体**

```bash
# 实体
rm gateway-boot/.../domain/supply/catalog/entity/ChannelCatalog.java
rm gateway-boot/.../domain/supply/catalog/entity/ChannelModelCatalog.java
rm gateway-boot/.../domain/supply/catalog/entity/ChannelEndpointCatalog.java
# Gateway 接口（如果存在独立接口）
# Gateway 实现（如果存在独立实现）
# Repository（如果存在独立 Repository）
# DO（如果存在独立 DO）
```

注意：先搜索这些文件是否有被引用，如有需先替换

- [ ] **Step 9: 编译验证**

Run: `./mvnw compile -pl gateway-boot -q`

- [ ] **Step 10: 提交**

```bash
git add -A
git commit -m "refactor: Channel 删除 priority/weight，PlanCatalog 删除 source/syncedAt，删除衍生目录"
```

---

### Task 6: 服务层重构

**Files:**
- Delete: `CatalogMaterializeService.java` 及相关 DTO
- Delete: `CatalogSyncService.java`
- Delete: `CatalogService.java` / `CatalogServiceImpl.java` 及相关 DTO
- Delete: `ModelsDevSyncClient.java`
- Delete: `CatalogDomainService.java`
- Create: `ChannelProvisionService.java`
- Create: `PlanCatalogService.java` / `PlanCatalogServiceImpl.java`
- Create: 新 DTO

- [ ] **Step 1: 创建 ChannelProvisionService**

在 `application/catalog/ChannelProvisionService.java` 创建服务，实现：
- `provisionFromPlan(String planCode, ProvisionRequest request)` — 从套餐创建渠道
- `provisionBatch(String providerCode, BatchProvisionRequest request)` — 批量创建
- `ensureProvider(String providerCode)` — 查找或创建 Provider
- `ensureModel(String modelName)` — 查找或创建 Model
- `resolveUpstreamModelName(String providerCode, String modelName)` — 上游模型名解析

- [ ] **Step 2: 创建 ChannelProvisionService DTO**

在 `application/catalog/dto/` 创建：
- `ProvisionResult.java` — planCode, channelId, endpointCount, instanceCount, status
- `BatchProvisionResult.java` — providerCode, totalCount, successCount, skippedCount, failedCount, results
- `ProvisionRequest.java` — apiKeys (List<String>)
- `BatchProvisionRequest.java` — planCodes (List<String>)

- [ ] **Step 3: 创建 PlanCatalogService**

在 `application/catalog/PlanCatalogService.java` 创建接口，`PlanCatalogServiceImpl.java` 创建实现：
- `listPlanCatalogs(String providerCode)` — 列出套餐
- `getPlanDetail(String planCode)` — 套餐详情
- `getPricing(String planCode)` — 套餐定价

- [ ] **Step 4: 删除旧服务**

```bash
rm gateway-boot/.../application/catalog/CatalogMaterializeService.java
rm gateway-boot/.../application/catalog/CatalogSyncService.java
rm gateway-boot/.../application/catalog/CatalogService.java
rm gateway-boot/.../application/catalog/CatalogServiceImpl.java
rm gateway-boot/.../infrastructure/supply/catalog/sync/ModelsDevSyncClient.java
rm gateway-boot/.../domain/supply/catalog/service/CatalogDomainService.java
rm gateway-boot/.../application/catalog/dto/MaterializeBatchRequest.java
rm gateway-boot/.../application/catalog/dto/MaterializeBatchResult.java
rm gateway-boot/.../application/catalog/dto/MaterializePlanRequest.java
rm gateway-boot/.../application/catalog/dto/MaterializeResult.java
rm gateway-boot/.../application/catalog/dto/ModelCatalogResponse.java
rm gateway-boot/.../application/catalog/dto/PlanCatalogResponse.java
rm gateway-boot/.../application/catalog/dto/PlanDetailResponse.java
rm gateway-boot/.../application/catalog/dto/PlanResult.java
rm gateway-boot/.../application/catalog/dto/ProviderCatalogResponse.java
```

- [ ] **Step 5: 编译验证**

Run: `./mvnw compile -pl gateway-boot -q`

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor: 删除旧 Catalog 服务，创建 ChannelProvisionService 和 PlanCatalogService"
```

---

### Task 7: Controller 层重构 + 路由适配 + BuiltinDataLoader

**Files:**
- Delete: `CatalogController.java`
- Create: `PlanCatalogController.java`
- Create: `ChannelProvisionController.java`
- Modify: `RoutingResolver.java` — ChannelModelGateway → ModelInstanceGateway
- Modify: `ChannelSelector.java` → `InstanceSelector.java` — 基于 ModelInstance.priority
- Modify: `BuiltinCatalogLoader.java` → `BuiltinDataLoader.java`
- Modify: `ProviderController.java` — 确保 list 端点
- Modify: `ModelController.java` — 确保 list 端点

- [ ] **Step 1: 创建 PlanCatalogController**

在 `adapter/api/PlanCatalogController.java` 创建，端点：
- GET `/api/v1/plan-catalogs` — 列出套餐
- GET `/api/v1/plan-catalogs/{planCode}` — 套餐详情
- GET `/api/v1/plan-catalogs/{planCode}/pricing` — 套餐定价

- [ ] **Step 2: 创建 ChannelProvisionController**

在 `adapter/api/ChannelProvisionController.java` 创建，端点：
- POST `/api/v1/provision/from-plan/{planCode}` — 从套餐创建渠道 (@SaCheckRole ADMIN)
- POST `/api/v1/provision/batch/{providerCode}` — 批量创建 (@SaCheckRole ADMIN)

- [ ] **Step 3: 重构 ChannelSelector → InstanceSelector**

将 `ChannelSelector.java` 重命名为 `InstanceSelector.java`：
- 依赖 `ModelInstanceGateway` 替代 `ChannelModelGateway`
- 使用 `findActiveByModelIdOrderByPriority(modelId)` 获取排序后的 ModelInstance
- 过滤用户团队权限和 Channel 活跃状态
- 返回 priority 最小的 ModelInstance

- [ ] **Step 4: 重构 RoutingResolver**

在 `RoutingResolver.java` 中：
- `ChannelModelGateway` → `ModelInstanceGateway`
- `ChannelSelector` → `InstanceSelector`
- `ChannelModel` → `ModelInstance`
- RoutingContext 组装时从 ModelInstance 读取 priority/weight/upstreamModelName

- [ ] **Step 5: 重构 BuiltinCatalogLoader → BuiltinDataLoader**

将 `BuiltinCatalogLoader.java` 重命名为 `BuiltinDataLoader.java`：
- 删除 ProviderCatalogGateway/ModelCatalogGateway 依赖
- `loadProviders()` 直接使用 ProviderGateway
- `loadModels()` 直接使用 ModelGateway
- `loadPlanCatalogs()` 保留使用 PlanCatalogGateway
- `loadPlanModelCatalogs()` 保留使用 PlanModelCatalogGateway

- [ ] **Step 6: 删除 CatalogController**

```bash
rm gateway-boot/.../adapter/api/CatalogController.java
```

- [ ] **Step 7: 确保 ProviderController/ModelController 的 list 端点完整**

检查 `ProviderController.java` 和 `ModelController.java` 是否已有 `GET /api/v1/providers` 和 `GET /api/v1/models` 端点。如无则新增。

- [ ] **Step 8: 编译验证**

Run: `./mvnw compile -pl gateway-boot -q`

- [ ] **Step 9: 提交**

```bash
git add -A
git commit -m "refactor: Controller 层重构，路由适配 ModelInstance，BuiltinDataLoader 重构"
```

---

### Task 8: 数据库迁移 + 前端适配

**Files:**
- Create: Flyway 迁移脚本
- Modify: 前端文件

- [ ] **Step 1: 编写 Flyway 迁移脚本**

创建新的迁移脚本，包含：
- Provider 合并（从 provider_catalogs 迁移数据）
- Model 合并（新增 knowledge_cutoff，从 model_spec_catalogs 迁移数据）
- ChannelModel → ModelInstance（重命名表、删除定价列、新增覆盖列和 priority/weight）
- Channel 删除 priority/weight
- PlanCatalog/PlanModelCatalog 删除 source/synced_at
- 删除衍生目录表

- [ ] **Step 2: 迁移 Channel.priority/weight 数据到 ModelInstance**

在迁移脚本中执行：
```sql
UPDATE model_instances mi
SET priority = c.priority, weight = c.weight
FROM channels c WHERE mi.channel_id = c.id;
```

- [ ] **Step 3: 前端 API 适配**

修改 `gateway-console/src/services/api/catalog.ts`：
- 删除 `providerCatalogApi`、`modelCatalogApi`、`catalogMaterializeApi`、`catalogSyncApi`
- 新增 `provisionApi`（fromPlan、batch）
- 修改 `planCatalogApi` 使用新端点

- [ ] **Step 4: 前端页面适配**

- 删除 `ProviderCatalogView.tsx`、`ModelCatalogView.tsx`、`MaterializeModal.tsx`、`CascadeMaterializeDialog.tsx`
- 修改 `Catalog/index.tsx` 移除 Provider/Model 目录 Tab
- 修改 `PlanCatalogView.tsx` 物化按钮改为"创建渠道"按钮
- 创建 `ChannelProvisionDialog.tsx`

- [ ] **Step 5: 前端类型适配**

修改 `types/catalog.ts`：
- 删除 MaterializeType/Status
- 新增 ProvisionResult/BatchProvisionResult 类型

- [ ] **Step 6: 编译验证后端**

Run: `./mvnw compile -pl gateway-boot -q`

- [ ] **Step 7: 编译验证前端**

Run: `cd gateway-console && npm run build`

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "refactor: 数据库迁移脚本，前端适配供应域合并"
```
