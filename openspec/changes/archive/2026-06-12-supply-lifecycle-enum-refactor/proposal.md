## Why

当前供应域（supply）存在 6 个状态枚举（ProviderState、ChannelState、ModelState、ChannelModelState、CredentialState、CatalogState），值几乎完全重复（ACTIVE/INACTIVE），但多数实体实际上不需要独立的状态管理。同时 Catalog 与 Runtime 的实体结构镜像但存在不必要的数据搬运。需要统一生命周期模型，消除重复，明确每个实体真正需要的状态管理。

## What Changes

- **新增** Channel.Phase 和 ModelInstance.Phase 两个独立枚举，各含 PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED 五个状态，附带 isRoutable()、isTerminal()、canTransitionTo() 方法
- **删除** 6 个旧枚举文件：ProviderState、ChannelState、ModelState、ChannelModelState、CredentialState、CatalogState
- **修改** Channel 实体：用 Channel.Phase 替换 ChannelState
- **修改** ModelInstance 实体：用 ModelInstance.Phase 替换 ChannelModelState
- **修改** Model 实体：去掉 ModelState，改为 deprecatedAt / scheduledRetiredAt / deprecationMessage 字段
- **修改** Provider 实体：去掉 ProviderState
- **修改** ChannelCredential 实体：去掉 CredentialState
- **修改** ChannelEndpoint 实体：去掉 ChannelEndpointState（若存在）
- **修改** 所有 Catalog 实体：去掉 CatalogState（Catalog 层合并后不再需要）
- **修改** 路由调度层（InstanceSelector 等）：适配新的 phase 判断逻辑
- **修改** 数据库迁移脚本：处理旧状态到新模型的映射

## Capabilities

### New Capabilities
- `entity-lifecycle`: 供应域实体生命周期管理，定义统一的状态转换规则和路由可见性

### Modified Capabilities
- （无 spec 级别的需求变更，本次仅为实现层重构）

## Impact

- **gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/**：删除 6 个枚举文件
- **gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/**：修改 Channel、ModelInstance、Model、Provider、ChannelCredential、ChannelEndpoint
- **gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity/**：修改所有 Catalog 实体（去掉 CatalogState）
- **gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/enums/**：删除 CatalogState
- **gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/**：修改 InstanceSelector 等调用方
- **数据库迁移脚本**：新增 migration 脚本处理状态映射
- 涉及约 15-20 个文件
