# application-access-control Specification

## Purpose
TBD - created by archiving change resilience-architecture. Update Purpose after archive.
## Requirements
### Requirement: ApplicationChannel 渠道可见性

系统 SHALL 通过 `ApplicationChannel` 关联实体决定应用可见的渠道集合，取代原 `TeamChannel` 机制。

**实体字段**: `applicationId`（应用 ID）、`channelId`（渠道 ID）；唯一约束 `(application_id, channel_id)`。

**API**:
- `GET /api/v1/applications/{id}/channels` — 查询应用授权的渠道 ID 列表
- `PUT /api/v1/applications/{id}/channels` — 更新应用渠道授权（先清空旧关联，再批量保存新关联；HTTP 204）
  - Request Body: `{ "channelIds": [1, 2, 3] }`

**规则**:
- 模型可见性不独立配置——由「渠道上挂哪些 ModelInstance」隐式决定
- 要某模型就授权挂该模型的渠道；无法「授权渠道但限模型」

#### Scenario: 查询应用渠道授权

- **WHEN** 管理员调用 `GET /api/v1/applications/{id}/channels`
- **THEN** 系统 SHALL 返回该应用授权的渠道 ID 列表

#### Scenario: 更新应用渠道授权全量替换

- **WHEN** 管理员调用 `PUT /api/v1/applications/{id}/channels` 传入新的 `channelIds` 集合
- **THEN** 系统 SHALL 先清空该应用的原有 `ApplicationChannel` 关联
- **THEN** 系统 SHALL 批量保存新的 `ApplicationChannel` 关联
- **THEN** 系统 SHALL 返回 HTTP 204

#### Scenario: 渠道授权为空时无可用渠道

- **WHEN** 应用的 `ApplicationChannel` 授权集合为空
- **THEN** 该应用的所有 API Key 无可用渠道（自然拒绝）

### Requirement: 权限锚点为 apiKeyId → applicationId

数据面权限锚点 SHALL 从 `userId`（人）改为 `apiKeyId → applicationId`（应用）。`PermissionRouter` 通过 `RoutingRequest.applicationId` 查询 `ApplicationChannel` 过滤可见渠道。

**权限链路**: `UserApiKey → Application → ApplicationChannel → Channel`

**规则**:
- `PermissionRouter`（`@Order(100)`，`isForce=true`）通过 `ApplicationChannelGateway.findChannelIdsByApplicationId(applicationId)` 查询应用可见渠道集合
- 仅保留该集合内且 `Channel.state.isRoutable()` 的实例
- `applicationId` 为 null（无权限锚点）时直接返回空集

#### Scenario: 按应用授权过滤渠道

- **WHEN** 请求携带 `applicationId`，`PermissionRouter` 执行过滤
- **THEN** 系统 SHALL 仅保留 `ApplicationChannel` 授权集合内的 `ModelInstance`
- **THEN** 系统 SHALL 再过滤出 `Channel.state.isRoutable()` 的活跃渠道

#### Scenario: 无权限锚点时拒绝访问

- **WHEN** `RoutingRequest.applicationId` 为 null
- **THEN** `PermissionRouter` SHALL 返回空集
- **THEN** 路由链终止，请求被拒绝

### Requirement: 数据面无 ADMIN 跳过

数据面 `PermissionRouter` SHALL NOT 保留 ADMIN 跳过分支。任何角色（含 ADMIN）在数据面都按应用授权过滤，无特权旁路。

**规则**:
- ADMIN 角色退回管理面特权（`@SaCheckRole("ADMIN")` 管理应用/渠道/画像配置）
- 管理员调试使用专门的全渠道调试应用，而非 ADMIN Key 调任意渠道
- `migration-default` 兜底应用按原 Team 渠道集授权（非全局开放，不放大权限）

#### Scenario: ADMIN 数据面无特权

- **WHEN** ADMIN 角色用户通过 API Key 发起数据面请求
- **THEN** `PermissionRouter` SHALL 按 `applicationId → ApplicationChannel` 过滤
- **THEN** 系统 SHALL NOT 因角色为 ADMIN 跳过权限过滤

### Requirement: 数据迁移 1:1 平移

现有 `Team/TeamChannel` 数据 SHALL 1:1 平移到 `Application/ApplicationChannel`，授权不丢不失真。

**迁移规则**:
- 1 Team → 1 默认 Application（`code/name` 继承自 Team）
- `TeamChannel` → `ApplicationChannel` 1:1 平移
- 归属不明 Key（多 Team 用户等）归 `migration-default` 应用挂 default 画像
- 运行期无 `application_id` 的 Key 软兜底 + 告警
- 迁移脚本可重跑、幂等，迁移前后授权集合比对校验

#### Scenario: Team 渠道授权平移到 Application

- **WHEN** 执行数据迁移脚本
- **THEN** 每个原 Team SHALL 生成一个 Application，`code/name` 继承
- **THEN** 原 `TeamChannel` 关联 SHALL 1:1 平移为 `ApplicationChannel`
- **THEN** 迁移后应用授权渠道集合 SHALL 与原 Team 授权集合一致

#### Scenario: 归属不明 Key 归 migration-default

- **WHEN** 某 Key 归属无法明确映射到单一 Application（如多 Team 用户）
- **THEN** 该 Key SHALL 归属 `migration-default` 应用
- **THEN** `migration-default` 应用 SHALL 挂 default 画像
- **THEN** `migration-default` 渠道范围 SHALL 按原 Team 渠道集授权（非全局开放）

