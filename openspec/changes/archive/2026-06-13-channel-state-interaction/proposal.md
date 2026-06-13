## Why

后端已完成供应域状态枚举重构（Channel.State / ModelInstance.State 五状态生命周期），但前端仍停留在旧的 ACTIVE/INACTIVE 二值模型，存在 7 处结构性脱节：

1. 状态展示只有绿/灰两色，无法区分 PENDING/SUSPENDED/DEPRECATED/RETIRED
2. 启停是 Toggle 开关，无法映射到受 canTransitionTo() 约束的状态转换
3. Provider 有独立启停开关，但后端已无 state 字段
4. Credential 仍展示 ACTIVE/INACTIVE，但后端已无状态
5. 状态筛选只有 ACTIVE/INACTIVE 两个选项
6. ModelInstance 的启停是二值 Toggle
7. 激活 Channel（PENDING→ACTIVE）无前置校验，无级联激活 ModelInstance

## What Changes

### 后端

- **新增** Channel 状态转换 API：`PUT /channels/{id}/state`，接收 targetState + reason，由后端校验 canTransitionTo() 和前置条件
- **新增** ModelInstance 状态转换 API：`PUT /channels/{channelId}/models/{modelId}/state`
- **新增** PENDING→ACTIVE 前置校验：至少 1 Endpoint + 1 Credential + 1 ModelInstance，级联激活 PENDING ModelInstance
- **删除** Provider 启停 API：`PATCH /providers/{id}/state?enabled=`
- **删除** 旧二值开关 API：`PATCH /channels/{id}/state?enabled=`
- **删除** 旧模型映射启停 API：`PATCH /channels/{channelId}/models/{modelId}/state?enabled=`

### 前端

- **修改** ChannelState 类型从 2 值扩展为 5 值
- **删除** ProviderState 和 ChannelCredentialState 类型
- **新增** ChannelStateTag 统一状态展示组件（5 色 + 图标）
- **修改** Channel 卡片/表格/抽屉的操作按钮：Toggle 开关改为上下文操作按钮
- **修改** 高风险操作确认弹窗（标记下线、废弃需二次确认+原因/名称输入）
- **修改** Provider 展示：去掉启停开关和状态展示，改为纯组织分组 + 批量暂停/恢复 Channel
- **修改** 状态筛选器：单选改为多选，5 种状态带颜色标记
- **修改** ModelInstance 操作：Toggle 改为上下文操作按钮
- **修改** Credential 展示：去掉状态列和状态筛选
- **新增** 渠道激活前置校验提示 + 级联激活 ModelInstance 列表展示

## Capabilities

### New Capabilities
- `channel-state-transition`: 渠道状态转换操作，含前置校验、级联激活、受约束转换

### Modified Capabilities
- `entity-lifecycle`: 更新实体生命周期管理，新增 SUSPENDED→RETIRED 转换路径

## Impact

- **后端**：ChannelController、ModelInstanceController、ChannelServiceImpl、ModelInstanceServiceImpl、ChannelDomainService
- **前端**：channel.ts/provider.ts 类型定义、ChannelCard、ChannelTableView、ChannelDetailDrawer、ProviderGroupHeader、Models Tab、Credentials Tab、筛选器、API 服务层、React Query hooks
