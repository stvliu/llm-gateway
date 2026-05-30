# Comet Design Handoff

- Change: inherited-permission-model
- Phase: design
- Mode: compact
- Context hash: 82f258f21d8f668356f5435f98303c88b04862ff49eab4652b2e0d1fe1e76c18

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/inherited-permission-model/proposal.md

- Source: openspec/changes/inherited-permission-model/proposal.md
- Lines: 1-29
- SHA256: 9cbbabd4b4be811fc994f59048223bb9404fd08b72910d4f36846f079d5336db

```md
## Why

当前 UserApiKey 自带 channelIds 渠道权限，与 Team ↔ Channel 的团队级权限形成冗余的双轨模型。管理员需要在两个层面分别配置权限，且 API Key 的渠道权限可能与团队权限不一致，造成管理混乱和安全风险。统一为纯继承式模型（API Key 完全继承团队渠道权限）可消除权限冗余、简化管理、保证一致性。

## What Changes

- **BREAKING**: 移除 UserApiKey 的 channelIds 字段及相关关联表 `user_api_key_channels`，API Key 不再持有独立渠道权限
- **BREAKING**: ChannelSelector 路由选择增加团队渠道过滤，无团队渠道权限的请求自然无可用通道
- **BREAKING**: ModelDiscoveryService 改为通过用户团队查渠道，而非 API Key 的 channelIds
- User → Team 关系保留多对多表，业务层限制单团队
- 新增团队渠道管理 API（GET/PUT `/api/v1/teams/{teamId}/channels`）
- 前端团队页面新增"渠道管理"弹窗，替代 API Key 级别的渠道选择
- 前端移除 API Key 相关的 channelIds/channel 类型定义

## Capabilities

### New Capabilities
- `team-channel-management`: 团队渠道管理能力 — 管理员可配置团队可访问的渠道集合，API Key 自动继承

### Modified Capabilities

## Impact

- **数据库**: 删除 `user_api_key_channels` 表（迁移脚本 V43）
- **Domain 层**: UserApiKey 实体移除 channelIds 字段；UserApiKeyGateway 移除 findIdsByChannelId
- **Application 层**: ChannelSelector、RoutingResolver、ModelDiscoveryService、UserApiKeyServiceImpl 核心逻辑变更
- **Infrastructure 层**: UserApiKeyGatewayImpl 重写 save/toEntity 逻辑；删除 UserApiKeyChannelDo 和 UserApiKeyChannelRepository
- **API**: TeamController 新增渠道管理端点；UserApiKeyCreateRequest/UpdateRequest 移除 channelIds
- **前端**: types/team.ts 删除 channelIds 相关类型；Teams 页面新增渠道管理弹窗
```

## openspec/changes/inherited-permission-model/design.md

- Source: openspec/changes/inherited-permission-model/design.md
- Lines: 1-44
- SHA256: 1ca9ad563bc9fd790972960a5a245dae82033420ef093b83ea9049bb9528bfb3

```md
## Architecture Decision: Pure Inherited Permission Model

Permission chain: `UserApiKey → User → Team → Channels`

```
┌──────────┐     ┌──────┐     ┌──────┐     ┌──────────┐
│ UserApiKey│────▶│ User │────▶│ Team │────▶│ Channels │
└──────────┘     └──────┘     └──────┘     └──────────┘
    1:N             N:1           N:N           1:N
  (userId)      (user_teams)  (team_channels)
```

### Key Decisions

1. **User → Team: N:1 via user_teams table** — 保留多对多表，业务层限制单团队，未来可扩展
2. **Permission enforcement in ChannelSelector** — 注入团队渠道过滤，无权限自然无可用通道，无需额外拦截器
3. **Complete removal of apiKey.channelIds** — 删除表、实体、DO、Repository、DTO 字段，不留残留
4. **Team channel management API** — 新增 GET/PUT 端点，前端新增弹窗管理入口

### Data Flow (After Change)

```
Request → Auth(Identity) → Routing
  → ChannelSelector.select(modelId, userId)
    → UserTeamGateway.findTeamIdByUserId(userId)
    → TeamChannelGateway.findChannelIdsByTeamId(teamId)
    → Filter ChannelModel by team channels
    → No match → ResourceNotFoundException (natural deny)
  → Selected ChannelModel → Credential/Endpoint → Upstream
```

### Removed Components

- `user_api_key_channels` table
- `UserApiKeyChannelDo`, `UserApiKeyChannelRepository`
- `UserApiKey.channelIds` field
- `UserApiKeyGateway.findIdsByChannelId()`
- `UserApiKeyCreateRequest.channelIds`, `UserApiKeyUpdateRequest.channelIds`

### New Components

- Migration V43: `DROP TABLE user_api_key_channels`
- `TeamController`: `GET/PUT /api/v1/teams/{teamId}/channels`
- `ChannelManageModal.tsx`: 团队渠道管理弹窗
```

## openspec/changes/inherited-permission-model/tasks.md

- Source: openspec/changes/inherited-permission-model/tasks.md
- Lines: 1-43
- SHA256: bb2900d3d8fea459fa0df523e8cfc4ef22aa32cfa5fc5841da93f4ad0fdd23ab

```md
## Tasks

### Phase 1: 数据库迁移 & Domain 层清理

- [ ] 创建迁移脚本 V43__drop_user_api_key_channels.sql
- [ ] UserApiKey 实体移除 channelIds 字段
- [ ] UserApiKeyGateway 接口移除 findIdsByChannelId 方法
- [ ] 删除 UserApiKeyChannelDo 和 UserApiKeyChannelRepository

### Phase 2: Infrastructure 层清理

- [ ] UserApiKeyGatewayImpl 移除渠道关联的 save/load 逻辑
- [ ] UserApiKeyServiceImpl 移除 channelIds 相关逻辑（create/update/toResponse/toDetailResponse）
- [ ] DataInitializer 移除 API Key 渠道关联初始化

### Phase 3: 路由层权限注入

- [ ] UserTeamGateway 新增 findTeamIdByUserId 便捷方法
- [ ] TeamChannelGateway 新增 findChannelIdsByTeamId 便捷方法
- [ ] ChannelSelector.select() 增加 userId 参数，注入团队渠道过滤
- [ ] RoutingResolver.resolve() 传递 userId 到 ChannelSelector
- [ ] ChatDispatchServiceImpl 从 Identity 取 userId 传给路由
- [ ] ModelDiscoveryService 改为通过用户团队查渠道

### Phase 4: API 层调整

- [ ] UserApiKeyCreateRequest/UpdateRequest 移除 channelIds
- [ ] TeamController 新增 GET/PUT /api/v1/teams/{teamId}/channels 端点
- [ ] TeamController.createApiKey() 移除 channelIds 参数

### Phase 5: 前端适配

- [ ] types/team.ts 删除 channelIds/channelBriefs 相关类型
- [ ] services/api/team.ts 新增 listChannels/updateChannels 方法
- [ ] 新建 ChannelManageModal.tsx 团队渠道管理弹窗
- [ ] Teams/index.tsx 操作列新增"渠道管理"按钮

### Phase 6: 验证

- [ ] 构建通过（mvnw clean install）
- [ ] API Key 不再持有渠道权限，权限完全继承自团队
- [ ] 团队渠道管理 API 可用
- [ ] 前端渠道管理弹窗功能正常
```

## openspec/changes/inherited-permission-model/specs/team-channel-management/spec.md

- Source: openspec/changes/inherited-permission-model/specs/team-channel-management/spec.md
- Lines: 1-59
- SHA256: 526226f22bed86645e3d6f1fd91c04a4a1731888ac6905764efd396e5e40c242

```md
# Team Channel Management

## Summary

团队渠道管理能力 — 管理员通过配置 Team ↔ Channel 关系来控制团队成员可访问的渠道集合，API Key 自动继承团队渠道权限。

## Requirements

### REQ-1: 团队渠道配置

管理员可以为每个团队配置可访问的渠道集合。

**API**:
- `GET /api/v1/teams/{teamId}/channels` — 查询团队渠道列表
- `PUT /api/v1/teams/{teamId}/channels` — 更新团队渠道列表（全量替换）
  - Request Body: `{ "channelIds": [1, 2, 3] }`

**规则**:
- 只有团队管理员或系统管理员可以配置团队渠道
- 渠道列表为空时，团队成员无法访问任何模型

### REQ-2: 权限继承

API Key 的渠道访问权限完全继承自所属用户的团队。

**权限链路**: `UserApiKey → User → Team → Channels`

**规则**:
- API Key 不再持有独立的 channelIds 字段
- 用户无团队时，API Key 无可用渠道（自然拒绝）
- 团队无渠道时，团队成员的 API Key 无可用渠道

### REQ-3: 路由层权限过滤

ChannelSelector 在选择通道时注入团队渠道过滤。

**规则**:
- ChannelSelector.select(modelId, userId) 根据用户团队渠道过滤可用通道
- 不在团队渠道集合内的 ChannelModel 被排除
- 无匹配通道时抛出 ResourceNotFoundException

#### Scenario: 用户无团队时请求模型
- **WHEN** 用户无团队关联，使用 API Key 请求模型
- **THEN** ChannelSelector 返回 ResourceNotFoundException
- **THEN** 错误信息包含模型 ID

#### Scenario: 团队渠道不覆盖请求模型
- **WHEN** 用户团队仅关联渠道 A，但请求的模型仅在渠道 B 上可用
- **THEN** ChannelSelector 过滤后无匹配，返回 ResourceNotFoundException

### REQ-4: API Key 渠道权限移除

移除 API Key 级别的渠道权限配置。

**规则**:
- 删除 `user_api_key_channels` 表
- UserApiKey 实体不再包含 channelIds 字段
- API Key 创建/更新接口不再接受 channelIds 参数
- 模型发现服务通过用户团队查渠道，而非 API Key 的 channelIds
```

