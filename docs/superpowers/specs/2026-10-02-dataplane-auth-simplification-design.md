# 数据面授权简化（认证即授权）设计文档

> 状态：已确认。对应深度 2 的收敛范围——控制面表化暂缓，数据面路由链收敛执行。

## 1. 背景与目标

经多轮评估（统一 API 认证 → 统一授权门面 → RBAC 语义正名）后，数据面授权链暴露出复杂性问题：授权与路由职责在 `RouterChain` 中交织（`PermissionRouter` 作为链成员做内存过滤，且授权后二次查询活跃渠道），概念上"授权"与"应用配置"混为一谈。

**本设计目标**（用户三项要求）：
1. **统一授权框架**：唯一门面 `AuthorizationService`（控制面 RBAC 判定）+ 数据面认证即授权（同一 `Identity` 承载主体与资源锚点，无第二套判定机制）
2. **概念清晰**：授权 = 认证产物（Key 绑定应用）；配置 = 渠道/实例/调用策略（非授权）
3. **使用简洁**：数据面调用链 = 认证 → 路由（配置作为查询条件），无独立授权调用

## 2. 核心洞察：认证即授权

数据面调用的授权锚点来自 **`Identity.applicationId()`——认证产物**（`ChatDispatchServiceImpl` 传 `identity.applicationId()` 给 `InstanceSelector`），而非请求参数。API Key 绑定的应用即唯一可用应用，用户无法伪造/选择任意应用。

因此：
- **数据面授权 = 认证即授权**：`AuthenticationService.authenticateUser` 验证 Key 时返回的 `Identity.applicationId` 即对「应用资源」的 invoke 授权（认证内完成）
- **`PermissionRouter` 的"应用→渠道"查询不是授权，是应用配置读取**：`application_channel` 是配置表，不是授权表
- **无独立数据面授权判定层**——控制面需要独立判定（角色→端点能力），数据面不需要（凭证天然绑定资源）

## 3. 设计

### 3.1 授权模型（最终形态）

| 概念 | 承载 | 是否授权 |
|------|------|---------|
| 主体 | `Identity.userId` | — |
| 数据面权限 | `Identity.applicationId`（Key 绑定） | ✅ 授权（认证内完成） |
| 控制面权限 | 角色 → 端点能力（`AuthorizationService.checkControl`，代码化规则） | ✅ 授权（RBAC，表化暂缓） |
| 应用渠道配置 | `application_channel` | ❌ 配置（跟随） |
| 渠道/实例状态 | `channels.state` / `model_instances.state` | ❌ 配置/ABAC 属性 |
| 调用策略 | `applications.timeout/failure_strategy` | ❌ 配置 |

### 3.2 数据面路由链收敛

```
现状：
  InstanceSelector.select(modelId, applicationId, ...)
    → 查询全部活跃实例（findActiveByModelIdOrderByPriority）
    → RouterChain.filter：PermissionRouter(100 授权过滤) → HealthRouter(200) → PriorityRouter(300) → LoadBalanceRouter(9999 透传)
    → 授权后活跃渠道二次查询（findByIds + isRoutable 内存过滤）

改造后：
  InstanceSelector.select(modelId, applicationId, ...)   ← 签名不变，调用方零改动
    → 应用渠道配置读取：ApplicationChannelRepository.findChannelIdsByApplicationId(applicationId)
       （配置读取，非授权判定——授权已蕴含在 Key 绑定应用中）
    → 活跃渠道过滤（proxy 层，isRoutable——ABAC 属性过滤）
    → 实例查询：ModelInstanceRepository.findActiveByModelIdAndChannelIds(modelId, 渠道集合)  ← 新查询，DB 层过滤
    → RouterChain（纯路由）：PriorityRouter(200 优先级) → HealthRouter(300 健康)
```

### 3.3 变更清单

| 文件 | 变更 |
|------|------|
| `gateway-provider/.../routing/PermissionRouter.java` | **删除** |
| `gateway-provider/.../routing/LoadBalanceRouter.java` | **删除**（透传无实义） |
| `gateway-provider/.../model/ModelInstanceRepository.java` | 新增 `findActiveByModelIdAndChannelIds(Long modelId, Collection<Long> channelIds)` |
| `gateway-proxy/.../routing/InstanceSelector.java` | 配置读取前移 + 实例查询带条件；移除授权链依赖 |
| `gateway-proxy/.../routing/RouterChain.java` | 成员自动收敛（删除的两个 Router 不在注入列表） |
| `PermissionRouterTest.java` | 删除（场景迁移至 InstanceSelectorTest） |
| `InstanceSelectorTest.java` | 新增配置读取 + 活跃过滤 + 路由链场景 |
| `RouterChainTest` 相关 | 成员变化同步 |

### 3.4 关键设计点

- 活跃渠道过滤（`isRoutable`）位于 `InstanceSelector` 配置读取后（proxy 层，ABAC 属性过滤语义；iam 不依赖 provider 的分层约束下不可下沉 iam）
- `InstanceSelector.select` 签名不变（`modelId, applicationId, userId, role, strategy, protocol`）——`ChatDispatchServiceImpl` 零改动
- 空渠道集合 → 空候选 → `ResourceNotFoundException`（现状语义保留）
- `AuthorizationService.permittedChannelIds` **删除**（连同其测试）：数据面认证即授权后无独立判定入口，配置读取由 `InstanceSelector` 直查 `ApplicationChannelRepository`（proxy → iam 现有依赖，无需门面中转）——YAGNI，避免无消费者死代码

### 3.5 明确不做（边界）

- 控制面 RBAC 表化（roles/user_roles/permissions/role_permissions 四表 + 多角色迁移）——**暂缓**，保持 `CONTROL_RULES` 代码化 + `users.role` 单字段
- 体验中心渠道授权补强（独立 follow-up）
- 授权/配置读取缓存（后置优化，10k QPS 时评估）
- `RoutingRequest` 冗余字段清理（保守保留）
- 管理 API（角色/权限 CRUD，表化暂缓后无对象）

## 4. 风险与验证

| 风险 | 缓解 |
|------|------|
| 实例查询语义差异（DB 过滤 vs 内存过滤） | TDD：新查询方法测试（渠道集合边界、空集合、活跃实例） |
| 活跃渠道过滤位置移动（PermissionRouter → InstanceSelector） | 原 PermissionRouter 测试场景迁移到 InstanceSelectorTest，行为等价验证 |
| 路由链成员变化（4→2） | `RouterChain` 初始化测试更新；全量回归 |
| 性能 | 查询次数净减（授权二次查询消失，无新增 N+1） |

**测试策略**：TDD 全程——仓储查询测试 → InstanceSelector 全链路场景 → 全量回归（`mvn -q test`，1466+ 基线）。

**零 schema 变更**：本设计不涉及任何表结构改动（新增的是查询方法，非表）。

## 5. 验收标准

1. `PermissionRouter`/`LoadBalanceRouter` 代码与测试删除，`RouterChain` 成员 = HealthRouter + PriorityRouter
2. `findActiveByModelIdAndChannelIds` 查询正确（渠道过滤 + 活跃 + 空集合边界）
3. `AuthorizationService.permittedChannelIds` 及其测试删除（无消费者）
4. 数据面调用链行为等价（候选实例集合与现状一致，含空候选 404 语义）
5. 全量测试通过（基线 1466+，无回归）
6. 文档（api-spec.md/架构文档）同步更新"认证即授权"语义
