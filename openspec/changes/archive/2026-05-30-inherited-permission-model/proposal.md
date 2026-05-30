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
