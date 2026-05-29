# 纯继承式权限模型设计

日期：2026-05-30

## 目标

将权限模型从"API Key 自带渠道权限"改为"API Key 完全继承团队渠道权限"，简化权限链路。

**变更后的权限链路**：`UserApiKey → User → Team → Channels`

## 设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| User → Team 关系 | 保留 `user_teams` 多对多表，业务层限制单团队 | 未来可扩展多团队 |
| 权限校验时机 | ChannelSelector 中注入团队渠道过滤 | 无权限自然无可用通道，简洁不侵入 |
| API Key channelIds | 彻底删除 | 避免残留代码造成混淆 |
| 前端渠道管理 | 团队页面新增"渠道管理"弹窗 | 权限由 Team ↔ Channel 决定，需管理入口 |

## 后端变更

### 删除

| 内容 | 路径 |
|------|------|
| `user_api_key_channels` 表 | 迁移脚本 V43 |
| `UserApiKey.channelIds` 字段 | `domain/iam/entity/UserApiKey.java` |
| `UserApiKeyChannelDo` | `infrastructure/iam/gateway/database/dataobject/` |
| `UserApiKeyChannelRepository` | `infrastructure/iam/gateway/database/repository/` |
| `UserApiKeyGateway.findIdsByChannelId()` | `domain/iam/gateway/UserApiKeyGateway.java` |
| `UserApiKeyGatewayImpl` 中渠道关联逻辑 | `infrastructure/iam/gateway/UserApiKeyGatewayImpl.java` |
| `UserApiKeyCreateRequest.channelIds` | `application/userapikey/dto/` |
| `UserApiKeyUpdateRequest.channelIds` | `application/userapikey/dto/` |

### 修改

**ChannelSelector** — 注入团队渠道过滤

```java
// 签名变更：新增 userId 参数
public ChannelModel select(Long modelId, Long userId)
```

逻辑：通过 `UserTeamGateway.findTeamIdByUserId(userId)` → `TeamChannelGateway.findChannelIdsByTeamId(teamId)` 获取团队渠道集合，过滤 ChannelModel 结果中不在该集合的通道。

**RoutingResolver** — 传递 userId

```java
// 签名变更
public RoutingContext resolve(String modelName, Protocol protocol, Long userId)
```

**ChatDispatchServiceImpl** — 从 Identity 取 userId 传给路由

**ModelDiscoveryService** — 通过用户团队查渠道

原逻辑：`apiKey.getChannelIds()` → 查模型
新逻辑：`userId → teamId → teamChannelIds` → 查模型

**UserApiKeyServiceImpl** — 移除 channelIds 相关逻辑

- `create()`: 移除 `setChannelIds`
- `update()`: 移除 channelIds 更新
- `toResponse()`: 移除 channelIds 和 channelBriefs
- `toDetailResponse()`: 移除 channelIds 和 channelBriefs
- 删除 `toChannelBriefs()` 方法，移除 `ChannelGateway` 依赖

**TeamController.createApiKey()** — 移除 channelIds 参数

**UserTeamGateway** — 新增便捷方法

```java
Long findTeamIdByUserId(Long userId);
// 返回用户加入的第一个团队 ID（业务层限制单团队）
```

**TeamChannelGateway** — 新增便捷方法

```java
List<Long> findChannelIdsByTeamId(Long teamId);
```

**DataInitializer** — 移除 API Key 渠道关联初始化

### 新增

**迁移脚本 V43__drop_user_api_key_channels.sql**

```sql
-- 1. 删除关联表
DROP TABLE IF EXISTS user_api_key_channels;

-- 2. 删除 UserApiKeyGateway.findIdsByChannelId 对应的查询（代码层面）
```

**团队渠道管理 API**（TeamController 新增端点）

```java
// 查询团队的渠道列表
GET /api/v1/teams/{teamId}/channels

// 更新团队的渠道列表（全量替换）
PUT /api/v1/teams/{teamId}/channels
// Body: { "channelIds": [1, 2, 3] }
```

## 前端变更

### 修改

**`types/team.ts`**

- 删除 `UserApiKeyDetail.channels`（`ChannelBrief[]`）
- 删除 `UserApiKey.teamId`
- 删除 `CreateUserApiKeyRequest.teamId`、`productIds`
- 删除 `ChannelBrief` 类型

**`services/api/team.ts`**

- 新增 `listChannels(teamId)` → `GET /teams/{teamId}/channels`
- 新增 `updateChannels(teamId, channelIds)` → `PUT /teams/{teamId}/channels`

**`pages/Teams/index.tsx`**

- 操作列新增"渠道管理"按钮（`<ApiOutlined />` 图标）
- 引入 `ChannelManageModal`

### 新增

**`pages/Teams/ChannelManageModal.tsx`**

团队渠道管理弹窗：
- 展示所有渠道的 Checkbox 列表（从现有渠道 API 获取）
- 勾选的渠道 = 团队可访问的渠道
- 保存时调用 `teamApi.updateChannels(teamId, channelIds)`
- 标题：`{team.name} - 渠道管理`
- 提示文案："配置该团队可访问的渠道，团队成员的 API Key 将继承这些渠道权限"

### 无需改动

- `UserApiKeyManageModal.tsx` — 已无渠道选择器，已有继承提示
- `UserApiKeyModal.tsx` — 已无渠道选择器，已有继承提示

## 权限校验流程（变更后）

```
请求 → 认证(Identity) → 路由解析
  → ChannelSelector.select(modelId, userId)
    → 查用户团队(teamId)
    → 查团队渠道(channelIds)
    → 过滤：只保留团队渠道内的 ChannelModel
    → 无匹配 → ResourceNotFoundException（自然拒绝）
  → 选中的 ChannelModel → 凭证/端点解析 → 上游调用
```

## 影响范围

| 层 | 影响文件数 | 风险 |
|---|---|---|
| 迁移脚本 | 1 | 低 — 仅删表 |
| Domain 实体 | 2 | 低 — 删字段 |
| Domain Gateway | 2 | 低 — 增删方法 |
| Infrastructure Gateway | 2 | 中 — 重写 save/toEntity 逻辑 |
| Application Service | 3 | 中 — 核心路由逻辑变更 |
| Controller | 1 | 低 — 参数调整 |
| 前端类型 | 1 | 低 — 删字段 |
| 前端页面 | 2 | 低 — 新增弹窗 + 按钮入口 |
| 前端 API | 1 | 低 — 新增 2 个方法 |
