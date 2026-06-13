## 1. 后端：状态转换 API

- [x] 1.1 新增 ChannelStateTransitionRequest DTO（targetState: String, reason: String?）
- [x] 1.2 修改 ChannelController：将 PATCH /{id}/state?enabled= 替换为 PUT /{id}/state + ChannelStateTransitionRequest
- [x] 1.3 修改 ChannelServiceImpl.setState()：接收 targetState，校验 canTransitionTo()；PENDING→ACTIVE 时校验前置条件（≥1 Endpoint + ≥1 Credential + ≥1 ModelInstance，不满足返回 400）+ 级联激活 PENDING ModelInstance；SUSPENDED→ACTIVE 时检查完整性（仅警告不阻塞）
- [x] 1.4 新增 ModelInstanceStateTransitionRequest DTO（targetState: String）
- [x] 1.5 修改 ModelInstanceController：将 PATCH /{channelId}/models/{modelId}/state?enabled= 替换为 PUT /{channelId}/models/{modelId}/state + ModelInstanceStateTransitionRequest
- [x] 1.6 修改 ModelInstanceServiceImpl：接收 targetState，校验 canTransitionTo()
- [x] 1.7 删除 ProviderController.setEnabled() 端点
- [x] 1.8 删除 ProviderUpdateRequest 和 ProviderQueryRequest 中的 state 字段

## 2. 后端：测试

- [x] 2.1 为 Channel.State 编写单元测试（canTransitionTo 全路径、isRoutable、isTerminal）
- [x] 2.2 为 ModelInstance.State 编写单元测试
- [x] 2.3 为 ChannelServiceImpl.setState() 编写测试：7 条转换路径的前置校验（PENDING→ACTIVE 强制校验、SUSPENDED→ACTIVE 警告）、级联激活、非法转换拒绝（400）、后置条件验证
- [x] 2.4 为 ModelInstanceServiceImpl 状态转换编写测试
- [x] 2.5 更新现有 Controller/Service 测试适配新 API

## 3. 前端：类型定义与基础组件

- [x] 3.1 更新 channel.ts：ChannelState 扩展为 5 值，删除 ChannelCredentialState，Channel/ChannelModel 的 state 字段类型更新
- [x] 3.2 更新 provider.ts：删除 ProviderState，Provider 接口移除 state 字段，UpdateProviderRequest 移除 state
- [x] 3.3 新增 ChannelStateTag 统一组件（5 种配色 + 图标 + 文字）
- [x] 3.4 新增 getAvailableTransitions(state) 工具函数

## 4. 前端：API 服务层

- [x] 4.1 channel.ts API：setState(id, enabled) → transitionState(id, targetState, reason?)
- [x] 4.2 channel.ts API：setModelEnabled() → transitionModelState(channelId, modelId, targetState)
- [x] 4.3 provider.ts API：删除 setEnabled()
- [x] 4.4 更新 React Query hooks：useSetChannelState → useTransitionChannelState，useSetChannelModelEnabled → useTransitionChannelModelState，删除 useSetEnabledProvider

## 5. 前端：Channel 卡片与表格

- [x] 5.1 ChannelCard：状态视觉适配（5 色左边框、透明度、Badge 角标）
- [x] 5.2 ChannelCard：操作按钮从 Toggle 改为上下文操作（根据 getAvailableTransitions 动态渲染）
- [x] 5.3 ChannelTableView：Status 列使用 ChannelStateTag，Actions 列改为上下文操作按钮
- [x] 5.4 ChannelTableView：列宽和排序适配

## 6. 前端：状态筛选器

- [x] 6.1 index.tsx：状态筛选从单选改为多选，5 种状态带颜色标记
- [x] 6.2 筛选逻辑从 ch.state === statusFilter 改为 selectedStates.includes(ch.state)

## 7. 前端：Channel 详情抽屉

- [x] 7.1 Header：状态 Tag 使用 ChannelStateTag，操作按钮根据当前 State 动态渲染
- [x] 7.2 DEPRECATED 状态下展示警告 Alert
- [x] 7.3 Models Tab：模型映射行操作从 Toggle 改为上下文操作按钮
- [x] 7.4 Credentials Tab：去掉状态列和状态筛选
- [x] 7.5 Overview Tab：新增配置就绪度检查（Endpoint/Key/Model 完整性）

## 8. 前端：确认弹窗组件

- [x] 8.1 新增 StateTransitionConfirmModal 通用组件（支持 Popconfirm/Modal 两种模式）
- [x] 8.2 激活确认弹窗：展示级联激活的 ModelInstance 列表
- [x] 8.3 标记下线确认弹窗：展示影响范围（含"唯一供给"模型警告）+ 原因输入
- [x] 8.4 废弃确认弹窗：展示影响范围（含"将失去供给"的模型列表）+ 名称输入确认
- [x] 8.5 激活前置校验失败提示弹窗

## 9. 前端：Provider 交互重构

- [x] 9.1 ProviderGroupHeader：去掉状态展示（删除 providerState prop、透明度逻辑）
- [x] 9.2 ProviderGroupHeader：下拉菜单启停开关改为"全部暂停"/"全部恢复"
- [x] 9.3 ProviderGroupHeader：统计行增加 Channel 状态分布小圆点
- [x] 9.4 批量操作确认弹窗（展示影响范围）

## 10. 集成验证

- [x] 10.1 后端编译通过 + 全部测试通过
- [x] 10.2 前端编译通过
- [x] 10.3 前后端联调：状态转换全路径（5 状态 × 合法转换）
