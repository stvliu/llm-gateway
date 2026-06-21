# Team Channel Management Delta Spec

## REMOVED Requirements

### Requirement: 团队渠道配置
**Reason**: Team 体系移除，权限主体从团队改为应用。渠道可见性归 `Application`（`ApplicationChannel`），由 `application-access-control` capability 取代。管理员通过 `PUT /api/v1/applications/{id}/channels` 配置应用渠道授权，替代原 `PUT /api/v1/teams/{teamId}/channels`。
**Migration**: 现有 `TeamChannel` 数据 1:1 平移到 `ApplicationChannel`（1 Team → 1 默认 Application，`TeamChannel` → `ApplicationChannel` 1:1）。迁移脚本可重跑、幂等，迁移前后授权集合比对校验。原 `GET/PUT /api/v1/teams/{teamId}/channels` 端点移除。

### Requirement: 权限继承
**Reason**: 权限链从 `UserApiKey → User → Team → Channels` 重写为 `UserApiKey → Application → ApplicationChannel → Channel`。API Key 的渠道访问权限不再继承自所属用户的团队，而是继承自所属应用（`UserApiKey.applicationId`）。
**Migration**: `UserApiKey` 增加 `application_id` 字段作为权限锚点。现有 Key 归属按 Team 迁移：1 Team → 1 Application，Team 成员的 Key 归属到对应 Application。归属不明 Key 归 `migration-default` 应用。原 `User → Team → Channels` 链路移除。

### Requirement: 路由层权限过滤
**Reason**: `ChannelSelector`/`PermissionRouter` 过滤依据从 `UserTeam → TeamChannel` 改为 `UserApiKey.application_id → ApplicationChannel`。数据面无 ADMIN 跳过分支。
**Migration**: `PermissionRouter`（`@Order(100)`，`isForce=true`）通过 `ApplicationChannelGateway.findChannelIdsByApplicationId(applicationId)` 查询应用可见渠道集合。`applicationId` 为 null 时返回空集。ADMIN 角色不再跳过过滤，退回管理面特权。

### Requirement: API Key 渠道权限移除
**Reason**: 本项原已移除 API Key 级 `channelIds`，改由团队继承。现 Team 体系整体移除，渠道权限归 Application。原「API Key 渠道权限移除」的语义被「权限锚点改为 applicationId」取代。
**Migration**: `UserApiKey` 不再持有独立 `channelIds`，渠道可见性完全由 `applicationId → ApplicationChannel` 决定。原 `user_api_key_channels` 表已在前序 change 移除，本 change 不再涉及。

### Requirement: 团队模型可见性（隐性能力）
**Reason**: 移除 Team 后无「团队模型可见性」概念。模型可见性不独立配置——由「渠道上挂哪些 ModelInstance」隐式决定。要某模型就授权挂该模型的渠道，无法「授权渠道但限模型」。
**Migration**: 废弃现有「团队模型可见性」机制（`listAllowedModels` + 前端 `ModelVisibilityModal`）。过滤链单层 `ApplicationChannel`。若确需应用级模型隔离，建不同渠道挂不同模型（记录为已知取舍）。
