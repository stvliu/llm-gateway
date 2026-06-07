## Why

供应域存在 14 个实体、双轨数据结构（Catalog + Runtime），导致 Provider/Model 在两边字段 85%~90% 重叠，物化流程需同步两套数据，衍生目录实体（ChannelCatalog/ChannelModelCatalog/ChannelEndpointCatalog）完全冗余。合并后消除数据冗余、简化维护，并将 ChannelModel 语义升级为 ModelInstance 以支持实例级能力覆盖和路由决策。

## What Changes

- **BREAKING** 合并 ProviderCatalog → Provider，删除 ProviderCatalog 实体
- **BREAKING** 合并 ModelCatalog → Model，删除 ModelCatalog 实体
- **BREAKING** 重命名 ChannelModel → ModelInstance，删除全部 7 个定价字段（inputPrice/outputPrice/reasoningPrice/cacheReadPrice/cacheWritePrice/inputAudioPrice/outputAudioPrice）
- ModelInstance 新增 capabilitiesOverride（Map<String,Boolean>）和 contextWindowOverride（Integer）字段，支持实例级能力/规格覆盖
- Channel.priority/weight 下沉到 ModelInstance，Channel 职责收窄为连接配置容器
- Model 新增 knowledgeCutoff 字段（从 ModelCatalog 合并）
- PlanCatalog/PlanModelCatalog 删除 source/syncedAt 字段，放弃数据来源追踪
- 删除 CatalogSource、ProviderType 枚举
- 删除衍生目录实体：ChannelCatalog、ChannelModelCatalog、ChannelEndpointCatalog
- 删除物化概念，替换为 ChannelProvisionService（从 PlanCatalog 创建 Channel）
- 删除 CatalogMaterializeService、CatalogSyncService、ModelsDevSyncClient、CatalogDomainService
- 重构 BuiltinCatalogLoader → BuiltinDataLoader（直接加载 Provider/Model/PlanCatalog）
- 删除 CatalogController，新建 PlanCatalogController、ChannelProvisionController
- 定价数据唯一来源变更为 PlanCatalog.pricing (JSON)
- 前端目录页面重构，物化按钮改为"创建渠道"

## Capabilities

### New Capabilities
- `model-instance`: 模型实例概念——模型在某渠道上的具体化身，支持实例级能力覆盖、上下文窗口覆盖、路由优先级/权重
- `channel-provision`: 从套餐目录（PlanCatalog）创建渠道（Channel）的供给流程，替代原物化概念

### Modified Capabilities

## Impact

- **领域层**：Provider/Model 实体字段扩展，ChannelModel 重命名为 ModelInstance 并大幅精简，Channel 删除 priority/weight
- **应用层**：删除 CatalogMaterializeService/CatalogSyncService/CatalogService，新建 ChannelProvisionService/PlanCatalogService
- **基础设施层**：删除 5 个 Catalog Gateway 实现、5 个 JPA Repository、5 个 DO 类，新增 ModelInstance 相关基础设施
- **适配器层**：删除 CatalogController，新建 PlanCatalogController/ChannelProvisionController
- **数据库**：6 张表 DDL 变更 + 5 张表删除 + 数据迁移
- **前端**：目录页面重构，API 端点变更
- **路由**：ChannelSelector 重构为 InstanceSelector，基于 ModelInstance 单实体做路由决策
