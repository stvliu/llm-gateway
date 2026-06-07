## 1. 枚举与基础设施清理

- [ ] 1.1 删除 CatalogSource 枚举类及所有引用
- [ ] 1.2 删除 ProviderType 枚举类及所有引用

## 2. Provider 合并

- [ ] 2.1 Provider 实体新增字段对齐（确认 code/name/logoUrl/websiteUrl/description/apiDocUrl/priority/state 完整）
- [ ] 2.2 删除 ProviderCatalog 领域实体
- [ ] 2.3 删除 ProviderCatalogGateway 接口
- [ ] 2.4 删除 ProviderCatalogGatewayImpl 基础设施实现
- [ ] 2.5 删除 ProviderCatalogRepository JPA 接口
- [ ] 2.6 删除 ProviderCatalogDo JPA 实体
- [ ] 2.7 ProviderGateway 新增 existsByCode、findByKeyword 方法
- [ ] 2.8 ProviderGatewayImpl 实现新增方法

## 3. Model 合并

- [ ] 3.1 Model 实体新增 knowledgeCutoff 字段
- [ ] 3.2 Model.capabilities/modalities 统一为结构化类型（Map/List），基础设施层处理 JSON 转换
- [ ] 3.3 删除 ModelCatalog 领域实体
- [ ] 3.4 删除 ModelCatalogGateway 接口
- [ ] 3.5 删除 ModelCatalogGatewayImpl 基础设施实现
- [ ] 3.6 删除 ModelCatalogRepository JPA 接口
- [ ] 3.7 删除 ModelCatalogDo JPA 实体
- [ ] 3.8 ModelGateway 新增 existsByModelName、findByKeyword、findByCapability 方法
- [ ] 3.9 ModelGatewayImpl 实现新增方法

## 4. ChannelModel → ModelInstance

- [ ] 4.1 创建 ModelInstance 领域实体（channelId, modelId, upstreamModelName, capabilitiesOverride, contextWindowOverride, priority, weight, quotaLimit, state）
- [ ] 4.2 创建 ModelInstanceGateway 接口（含 findActiveByModelIdOrderByPriority）
- [ ] 4.3 创建 ModelInstanceDo JPA 实体（表名 model_instances）
- [ ] 4.4 创建 ModelInstanceRepository JPA 接口
- [ ] 4.5 创建 ModelInstanceGatewayImpl 基础设施实现
- [ ] 4.6 删除 ChannelModel 领域实体
- [ ] 4.7 删除 ChannelModelGateway 接口
- [ ] 4.8 删除 ChannelModelDo JPA 实体
- [ ] 4.9 删除 ChannelModelRepository JPA 接口
- [ ] 4.10 删除 ChannelModelGatewayImpl 基础设施实现
- [ ] 4.11 所有引用 ChannelModel 的代码改为 ModelInstance

## 5. Channel 精简

- [ ] 5.1 Channel 实体删除 priority、weight 字段
- [ ] 5.2 ChannelDo JPA 实体删除 priority、weight 列映射
- [ ] 5.3 ChannelGatewayImpl 删除 priority/weight 相关转换
- [ ] 5.4 ChannelCreateRequest/ChannelUpdateRequest DTO 删除 priority/weight
- [ ] 5.5 ChannelResponse DTO 删除 priority/weight

## 6. PlanCatalog 精简

- [ ] 6.1 PlanCatalog 实体删除 source、syncedAt 字段
- [ ] 6.2 PlanModelCatalog 实体删除 source、syncedAt 字段
- [ ] 6.3 PlanCatalogDo/PlanModelCatalogDo JPA 实体删除对应列映射
- [ ] 6.4 PlanCatalogGateway 删除 source 相关方法（findBySource、findBySourceExcludingKeys）
- [ ] 6.5 PlanModelCatalogGateway 删除 source 相关方法
- [ ] 6.6 PlanCatalogGatewayImpl/PlanModelCatalogGatewayImpl 删除对应实现

## 7. 衍生目录删除

- [ ] 7.1 删除 ChannelCatalog 领域实体及 Gateway/Repository/DO
- [ ] 7.2 删除 ChannelModelCatalog 领域实体及 Gateway/Repository/DO
- [ ] 7.3 删除 ChannelEndpointCatalog 领域实体及 Gateway/Repository/DO

## 8. 服务层重构

- [ ] 8.1 删除 CatalogMaterializeService 及相关 DTO（MaterializeResult/MaterializeBatchResult/PlanResult/MaterializeBatchRequest/MaterializePlanRequest）
- [ ] 8.2 删除 CatalogSyncService
- [ ] 8.3 删除 CatalogService/CatalogServiceImpl 及相关 DTO（ProviderCatalogResponse/PlanCatalogResponse/ModelCatalogResponse/PlanDetailResponse）
- [ ] 8.4 删除 ModelsDevSyncClient
- [ ] 8.5 删除 CatalogDomainService
- [ ] 8.6 创建 ChannelProvisionService（provisionFromPlan、provisionBatch、ensureProvider、ensureModel）
- [ ] 8.7 创建 ProvisionResult/BatchProvisionResult/ProvisionRequest/BatchProvisionRequest DTO
- [ ] 8.8 创建 PlanCatalogService（listPlanCatalogs、getPlanDetail、getPricing）

## 9. BuiltinDataLoader 重构

- [ ] 9.1 重构 BuiltinCatalogLoader → BuiltinDataLoader
- [ ] 9.2 loadProviders() 改为直接加载 Provider（从 providers.json）
- [ ] 9.3 loadModels() 改为直接加载 Model（从 model-specs.json，含 knowledgeCutoff、capabilities/modalities 类型转换）
- [ ] 9.4 loadPlanCatalogs()/loadPlanModelCatalogs() 删除 source 赋值

## 10. Controller 层重构

- [ ] 10.1 删除 CatalogController
- [ ] 10.2 创建 PlanCatalogController（GET /api/v1/plan-catalogs、GET /{planCode}、GET /{planCode}/pricing）
- [ ] 10.3 创建 ChannelProvisionController（POST /api/v1/provision/from-plan/{planCode}、POST /api/v1/provision/batch/{providerCode}）
- [ ] 10.4 ProviderController 新增 list 端点（GET /api/v1/providers，替代原 catalog/providers）
- [ ] 10.5 ModelController 新增 list 端点（GET /api/v1/models，替代原 catalog/models）

## 11. 路由代码适配

- [ ] 11.1 RoutingResolver 中 ChannelModelGateway 替换为 ModelInstanceGateway
- [ ] 11.2 ChannelSelector 重构为 InstanceSelector，基于 ModelInstance.priority 排序选择
- [ ] 11.3 OutboundTuner 适配 ModelInstance（逻辑不变，类型替换）
- [ ] 11.4 ModelDiscoveryService 适配 ModelInstanceGateway
- [ ] 11.5 ChannelModelService → ModelInstanceService 重命名及适配

## 12. 数据库迁移

- [ ] 12.1 编写 Flyway 迁移脚本：Provider 合并（从 provider_catalogs 迁移数据）
- [ ] 12.2 编写 Flyway 迁移脚本：Model 合并（新增 knowledge_cutoff 列，从 model_spec_catalogs 迁移数据）
- [ ] 12.3 编写 Flyway 迁移脚本：ChannelModel → ModelInstance（重命名表、删除定价列、新增覆盖列和 priority/weight）
- [ ] 12.4 编写 Flyway 迁移脚本：Channel 删除 priority/weight 列
- [ ] 12.5 编写 Flyway 迁移脚本：PlanCatalog/PlanModelCatalog 删除 source/synced_at 列
- [ ] 12.6 编写 Flyway 迁移脚本：删除衍生目录表
- [ ] 12.7 迁移 Channel.priority/weight 数据到 ModelInstance

## 13. 前端适配

- [ ] 13.1 删除 ProviderCatalogView.tsx、ModelCatalogView.tsx、MaterializeModal.tsx、CascadeMaterializeDialog.tsx
- [ ] 13.2 Catalog/index.tsx 改为 PlanCatalog 页面（移除 Provider/Model 目录 Tab）
- [ ] 13.3 PlanCatalogView.tsx 物化按钮改为"创建渠道"按钮，调用 provision API
- [ ] 13.4 创建 ChannelProvisionDialog.tsx（从套餐创建渠道弹窗）
- [ ] 13.5 services/api/catalog.ts 重构（删除 providerCatalogApi/modelCatalogApi/catalogMaterializeApi，新增 provisionApi）
- [ ] 13.6 services/query/useCatalog.ts 重构（删除相关 hooks，新增 useProvision）
- [ ] 13.7 types/catalog.ts 适配（删除 MaterializeType/Status，新增 Provision 类型）

## 14. 测试验证

- [ ] 14.1 编译通过，无编译错误
- [ ] 14.2 应用启动正常，BuiltinDataLoader 加载内置数据成功
- [ ] 14.3 验证 Provider/Model CRUD API 正常
- [ ] 14.4 验证 ChannelProvisionService 从 PlanCatalog 创建 Channel 流程正常
- [ ] 14.5 验证路由流程正常（ModelMatcher → InstanceSelector → CredentialResolver → EndpointResolver）
- [ ] 14.6 验证 PlanCatalog 查询和定价查询 API 正常
