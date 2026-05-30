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
