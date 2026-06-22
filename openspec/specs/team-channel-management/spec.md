# Team Channel Management

## Purpose

团队渠道管理能力 — 管理员通过配置 Team ↔ Channel 关系来控制团队成员可访问的渠道集合，API Key 自动继承团队渠道权限。

## Requirements

### Requirement: 团队渠道配置

系统 SHALL 支持管理员为每个团队配置可访问的渠道集合，并通过 REST API 查询与全量更新团队渠道列表。

**API**:
- `GET /api/v1/teams/{teamId}/channels` — 查询团队渠道列表
- `PUT /api/v1/teams/{teamId}/channels` — 更新团队渠道列表（全量替换）
  - Request Body: `{ "channelIds": [1, 2, 3] }`

**规则**:
- 只有团队管理员或系统管理员可以配置团队渠道
- 渠道列表为空时，团队成员无法访问任何模型

### Requirement: 权限继承

API Key 的渠道访问权限 SHALL 完全继承自所属用户的团队，API Key 不得持有独立的渠道权限字段。

**权限链路**: `UserApiKey → User → Team → Channels`

**规则**:
- API Key 不再持有独立的 channelIds 字段
- 用户无团队时，API Key 无可用渠道（自然拒绝）
- 团队无渠道时，团队成员的 API Key 无可用渠道

### Requirement: 路由层权限过滤

ChannelSelector 在选择通道时 SHALL 注入团队渠道过滤，排除不在用户团队渠道集合内的通道。

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

### Requirement: API Key 渠道权限移除

系统 SHALL 移除 API Key 级别的渠道权限配置，渠道访问权限统一由用户团队渠道关系承载。

**规则**:
- 删除 `user_api_key_channels` 表
- UserApiKey 实体不再包含 channelIds 字段
- API Key 创建/更新接口不再接受 channelIds 参数
- 模型发现服务通过用户团队查渠道，而非 API Key 的 channelIds
