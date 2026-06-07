## Context

供应域（Supply Domain）当前存在双轨数据结构：Catalog 层（7 个实体）和 Runtime 层（7 个实体），其中 Provider/Model 的 Catalog 和 Runtime 字段 85%~90% 重叠。物化流程（CatalogMaterializeService）负责从 Catalog 复制数据到 Runtime，增加了系统复杂度。ChannelModel 承载关联+定价+路由属性，职责不清。衍生目录实体（ChannelCatalog/ChannelModelCatalog/ChannelEndpointCatalog）仅为 Runtime 的只读投影，无独立价值。

## Goals / Non-Goals

**Goals:**
- 实体数从 14 减少到 8，消除双轨数据冗余
- ChannelModel 语义升级为 ModelInstance，支持实例级能力覆盖和路由决策
- Channel 职责收窄为连接配置容器，priority/weight 下沉到 ModelInstance
- 定价数据集中在 PlanCatalog.pricing，消除 ModelInstance 上的定价字段
- 物化概念替换为"从套餐创建渠道"（ChannelProvisionService）
- 删除 source/syncedAt，简化数据追踪

**Non-Goals:**
- 枚举不合并（ProviderState/ModelState/ChannelState 等各自保留）
- providerType 不下沉到 Provider（随 ProviderCatalog 一起删除）
- 不实现成本优化路由（按价格排序 ModelInstance），后续优化
- 不实现 Provider/Model 信息补全功能（物化后只创建最小信息），后续优化
- 前端只做 API 适配，不做 UI 重设计

## Decisions

### D1: Provider/Model 合并策略 — 直接合并（非引用模式）

**选择**：将 ProviderCatalog 字段合并进 Provider，ModelCatalog 字段合并进 Model，然后删除 Catalog 实体。

**替代方案**：
- B: Provider/Model 退化为引用（只保留 id + code，展示信息从 Catalog 读取）— 查询需要 JOIN，运营状态和目录状态冲突
- C: 保留双轨但消除冗余字段 — 仍有同步问题

**理由**：方案 A 最简单，单表查询，无同步问题。Provider/Model 本身就是"身份+属性"的统一概念，不需要模板-实例分离。

### D2: ModelInstance 定价字段删除 — 定价集中在 PlanCatalog.pricing

**选择**：删除 ModelInstance 上全部 7 个定价字段，定价数据唯一来源为 PlanCatalog.pricing (JSON)。

**理由**：定价是套餐级别的属性，不是模型实例的固有属性。同一模型在不同套餐中定价不同，定价应随套餐走。运营时通过 Channel.name = planCode 反查 PlanCatalog 获取定价。

**代价**：路由时如需按价格排序，需额外查询 PlanCatalog。可后续通过 Channel 上的 pricingSnapshot 缓存字段优化。

### D3: Channel.priority/weight 下沉到 ModelInstance

**选择**：priority/weight 从 Channel 移到 ModelInstance。

**理由**：路由决策的粒度是模型级别的——同一渠道的不同模型可能有不同的路由策略。下沉后 InstanceSelector 基于单实体排序，无需跨 Channel + ChannelModel 拼凑。

### D4: 物化概念替换为供给（Provision）

**选择**：删除 CatalogMaterializeService，新建 ChannelProvisionService。

**理由**：ProviderCatalog/ModelCatalog 删除后，Provider/Model 不再有"从模板创建"的语义，物化概念自然消失。PlanCatalog 仍然保留，其转化为 Channel 的过程是"从套餐创建渠道"，语义更直观。

### D5: BuiltinDataLoader 直接加载运营表

**选择**：BuiltinCatalogLoader 重构为 BuiltinDataLoader，直接加载 Provider/Model/PlanCatalog。

**理由**：没有 Catalog 中间层，内置数据直接写入运营表。upsert 逻辑简化为"按 code/modelName 检查是否存在，不存在则创建"。

### D6: source/syncedAt 全部删除

**选择**：所有实体的 source/syncedAt 字段删除，CatalogSource 枚举删除。

**理由**：数据来源追踪的复杂度（优先级覆盖、标记废弃、同步流程）远大于其当前价值。可通过 createdBy 区分系统创建和用户创建。

**代价**：无法区分内置数据、models.dev 同步数据、手动创建数据；无法从 models.dev 自动同步更新。

## Risks / Trade-offs

- [定价查询性能] 反查 PlanCatalog.pricing JSON 比直接查 ModelInstance 定价字段慢 → 可在 Channel 上加 pricingSnapshot 缓存字段（后续优化）
- [Provider/Model 信息不完整] 从 PlanCatalog 级联创建 Provider/Model 时只有最小信息（code/name）→ 用户需手动补充，或后续提供"补全"功能
- [数据库迁移停机] DDL 变更涉及 6 张表修改 + 5 张表删除 → 分阶段执行，每阶段独立可回滚
- [前端改动面大] 目录页面多个组件需重构 → 先完成 API 层，再逐步迁移前端
- [放弃 models.dev 同步] 删除 source 追踪后无法自动同步上游数据 → 如需恢复，后续可重新引入独立同步服务
