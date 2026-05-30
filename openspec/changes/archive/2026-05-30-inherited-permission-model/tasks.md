## Tasks

### Phase 1: 数据库迁移 & Domain 层清理

- [x] 创建迁移脚本 V43__drop_user_api_key_channels.sql
- [x] UserApiKey 实体移除 channelIds 字段
- [x] UserApiKeyGateway 接口移除 findIdsByChannelId 方法
- [x] 删除 UserApiKeyChannelDo 和 UserApiKeyChannelRepository

### Phase 2: Infrastructure 层清理

- [x] UserApiKeyGatewayImpl 移除渠道关联的 save/load 逻辑
- [x] UserApiKeyServiceImpl 移除 channelIds 相关逻辑（create/update/toResponse/toDetailResponse）
- [x] DataInitializer 移除 API Key 渠道关联初始化

### Phase 3: 路由层权限注入

- [x] UserTeamGateway 新增 findTeamIdByUserId 便捷方法
- [x] TeamChannelGateway 新增 findChannelIdsByTeamId 便捷方法
- [x] ChannelSelector.select() 增加 userId 参数，注入团队渠道过滤
- [x] RoutingResolver.resolve() 传递 userId 到 ChannelSelector
- [x] ChatDispatchServiceImpl 从 Identity 取 userId 传给路由
- [x] ModelDiscoveryService 改为通过用户团队查渠道

### Phase 4: API 层调整

- [x] UserApiKeyCreateRequest/UpdateRequest 移除 channelIds
- [x] TeamController 新增 GET/PUT /api/v1/teams/{teamId}/channels 端点
- [x] TeamController.createApiKey() 移除 channelIds 参数

### Phase 5: 前端适配

- [x] types/team.ts 删除 channelIds/channelBriefs 相关类型
- [x] services/api/team.ts 新增 listChannels/updateChannels 方法
- [x] 新建 ChannelManageModal.tsx 团队渠道管理弹窗
- [x] Teams/index.tsx 操作列新增"渠道管理"按钮

### Phase 6: 验证

- [x] 构建通过（mvnw clean install -DskipTests）
- [x] 单元测试通过（438 run, 1 error 为已有的 H2/JSONB 兼容问题，与本次修改无关）
- [x] API Key 不再持有渠道权限，权限完全继承自团队
- [x] 团队渠道管理 API 可用
- [x] 前端渠道管理弹窗功能已实现