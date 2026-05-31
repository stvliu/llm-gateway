# 任务清单

## 功能补齐

- [ ] **P1 提供商统计标签页**
  - [ ] 后端新增 `GET /api/v1/providers/{id}/stats` 端点
  - [ ] 前端创建 `ProviderStatisticsTab` 组件（请求量/Token/错误率图表）
  - [ ] 在 `ProviderManagementDrawer` 中注册新标签页
  - [ ] 补充 i18n 翻译

- [ ] **P1 批量启停操作**
  - [ ] 后端新增 `PATCH /api/v1/providers/batch/state` 端点
  - [ ] 前端主页面增加行选择模式（Checkbox 列）
  - [ ] 工具栏新增"批量启用"/"批量停用"按钮
  - [ ] 补充 i18n 翻译

- [ ] **P2 提供商复制功能**
  - [ ] ProviderCreateModal 新增"从已有提供商创建"步骤
  - [ ] 选择数据源→预填表单→可编辑后提交
  - [ ] 补充 i18n 翻译

- [ ] **P2 创建流程扩展（渠道配置）**
  - [ ] 物化完成后新增"配置渠道"可选步骤
  - [ ] 创建 ProviderCreateChannelStep 组件
  - [ ] 补充 i18n 翻译

## Bug 修复

- [ ] **P0 ProviderCard 模型数据错误**
  - [ ] 修复 `ProviderCard.tsx` 中 `useModels()` 全量拉取问题
  - [ ] 改为按 providerId 过滤或新增 `useProviderModels(providerId)` hook

## 体验优化

- [ ] **P2 端点协议动态加载**
  - [ ] ChannelEndpointFormModal 中替换硬编码协议选项
  - [ ] 从 `protocolApi.list()` 动态获取协议列表

- [ ] **P3 补充 mutation 成功反馈**
  - [ ] 审计所有 mutation 操作
  - [ ] 补充缺失的 `message.success()` 调用

- [ ] **P3 优化渠道标签跳转**
  - [ ] ProviderCard 渠道标签点击后自动切换到抽屉渠道标签页