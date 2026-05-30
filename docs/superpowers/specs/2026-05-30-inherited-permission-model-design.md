---
comet_change: inherited-permission-model
role: technical-design
canonical_spec: openspec
---

# 纯继承式权限模型 — 技术设计

## 1. 权限模型变更

**变更后权限链路**：`UserApiKey → User → Team → Channels`

```
┌──────────┐     ┌──────┐     ┌──────┐     ┌──────────┐
│ UserApiKey│────▶│ User │────▶│ Team │────▶│ Channels │
└──────────┘     └──────┘     └──────┘     └──────────┘
    1:N             N:1           N:N           1:N
  (userId)      (user_teams)  (team_channels)
```

API Key 不再持有渠道权限，完全通过用户所属团队继承。User → Team 保留多对多表，业务层限制单团队。

## 2. 路由层权限过滤

权限校验在 ChannelSelector 中注入，无权限自然无可用通道：

```
Request → Auth(Identity) → RoutingResolver
  → ChannelSelector.select(modelId, userId)
    → UserTeamGateway.findTeamIdByUserId(userId)
    → TeamChannelGateway.findChannelIdsByTeamId(teamId)
    → Filter ChannelModel by team channel set
    → No match → ResourceNotFoundException
  → Selected ChannelModel → CredentialResolver → EndpointResolver → Upstream
```

**ChannelSelector 签名变更**：
```java
public ChannelModel select(Long modelId, Long userId)
```

**RoutingResolver 签名变更**：
```java
public RoutingContext resolve(String modelName, Protocol protocol, Long userId)
```

**ChatDispatchServiceImpl**：从 `Identity.userId()` 取 userId 传给 `RoutingResolver.resolve()`。

**ModelDiscoveryService**：原逻辑 `apiKey.getChannelIds()` → 查模型，改为 `userId → teamId → teamChannelIds` → 查模型。

## 3. 删除清单

| 删除项 | 文件 |
|--------|------|
| `user_api_key_channels` 表 | V43 迁移脚本 |
| `UserApiKey.channelIds` | `domain/iam/entity/UserApiKey.java` |
| `UserApiKeyChannelDo` | `infrastructure/iam/gateway/database/dataobject/` |
| `UserApiKeyChannelRepository` | `infrastructure/iam/gateway/database/repository/` |
| `UserApiKeyGateway.findIdsByChannelId()` | `domain/iam/gateway/UserApiKeyGateway.java` |
| 渠道关联 save/load 逻辑 | `UserApiKeyGatewayImpl.java` |
| `UserApiKeyCreateRequest.channelIds` | `application/userapikey/dto/` |
| `UserApiKeyUpdateRequest.channelIds` | `application/userapikey/dto/` |
| `ChannelBrief` 类型 | 前端 `types/team.ts` |
| `UserApiKeyDetail.channels` | 前端 `types/team.ts` |
| `UserApiKey.teamId` | 前端 `types/team.ts` |
| `CreateUserApiKeyRequest.teamId/productIds` | 前端 `types/team.ts` |

## 4. 新增端点

**团队渠道管理 API**（TeamController）：

```java
GET  /api/v1/teams/{teamId}/channels       → List<Long>
PUT  /api/v1/teams/{teamId}/channels       → void
     Body: { "channelIds": [1, 2, 3] }
```

**Gateway 便捷方法**：
```java
// UserTeamGateway
Long findTeamIdByUserId(Long userId);

// TeamChannelGateway
List<Long> findChannelIdsByTeamId(Long teamId);
```

## 5. 边界条件与错误处理

| 场景 | 处理 |
|------|------|
| 用户无团队 | `findTeamIdByUserId` 返回 null → ChannelSelector 过滤结果为空 → `ResourceNotFoundException` |
| 团队无渠道 | `findChannelIdsByTeamId` 返回空列表 → 同上 |
| 用户属于多个团队 | `findTeamIdByUserId` 返回第一个团队 ID（业务层限制单团队） |
| 团队渠道不覆盖请求模型 | 过滤后无匹配 ChannelModel → `ResourceNotFoundException` |

## 6. 前端变更

**新增** `ChannelManageModal.tsx`：
- 展示所有渠道的 Checkbox 列表
- 勾选 = 团队可访问渠道
- 保存调用 `teamApi.updateChannels(teamId, channelIds)`
- 提示文案："配置该团队可访问的渠道，团队成员的 API Key 将继承这些渠道权限"

**修改** `Teams/index.tsx`：操作列新增"渠道管理"按钮

**修改** `services/api/team.ts`：新增 `listChannels`、`updateChannels` 方法

**无需改动**：两个 API Key Modal 已无渠道选择器，已有继承提示

## 7. 测试策略

| 测试目标 | 方法 |
|----------|------|
| ChannelSelector 团队过滤 | 单元测试：mock UserTeamGateway/TeamChannelGateway，验证过滤逻辑 |
| 用户无团队/团队无渠道 | 单元测试：验证返回 ResourceNotFoundException |
| ModelDiscoveryService 继承 | 单元测试：验证通过团队查渠道 |
| UserApiKeyGatewayImpl 清理 | 单元测试：验证 save/load 不包含渠道逻辑 |
| 团队渠道管理 API | 集成测试：验证 GET/PUT 端点 |
| 端到端 | 手动验证：创建团队→分配渠道→创建 Key→调用 API→确认权限继承生效 |

## 8. Spec Patch

delta spec `team-channel-management/spec.md` 中 REQ-3（路由层权限过滤）缺少验收场景，补充：

### REQ-3 验收场景补充

**Scenario: 用户无团队时请求模型**
- WHEN 用户无团队关联，使用 API Key 请求模型
- THEN ChannelSelector 返回 ResourceNotFoundException
- THEN 错误信息包含模型 ID

**Scenario: 团队渠道不覆盖请求模型**
- WHEN 用户团队仅关联渠道 A，但请求的模型仅在渠道 B 上可用
- THEN ChannelSelector 过滤后无匹配，返回 ResourceNotFoundException