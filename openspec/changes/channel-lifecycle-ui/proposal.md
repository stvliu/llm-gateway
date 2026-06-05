## Why

当前前端供应商管理存在两个独立页面（Providers 和 Channels），功能严重重叠且层级关系不清晰。核心问题：Channel 才是实际可配置、可路由、可授权的聚合单元（端点/Key/模型映射都挂在 Channel 下），但当前 UI 以 Provider 为中心，导致用户需要频繁在两个页面间切换才能完成渠道配置。技术管理者需要以渠道为核心视角，在一个页面内完成供应商接入、渠道配置、运营监控的全生命周期管理。

## What Changes

- **BREAKING**: 移除独立的 Providers 页面，将供应商管理功能合并到 Channels 页面
- 供应商作为渠道列表的分组头部展示，支持 hover 操作（编辑品牌信息、停用、连通性测试）
- 渠道详情抽屉新增「概览」Tab 作为默认页，一屏展示连通状态、Token/成本、资源摘要、最近活动
- 渠道卡片增强：展示端点/Key/模型数量统计 + 今日用量和成本
- 供应商创建向导合并到"快捷接入"流程（底部虚线卡片入口）
- 批量导入/导出功能移至渠道管理页面顶部操作栏
- 新增分组/列表视图切换
- 搜索增强：支持搜索渠道名称、模型名、端点 URL

## Capabilities

### New Capabilities
- `channel-lifecycle-page`: 渠道生命周期管理页面，整合供应商分组、渠道列表、渠道详情、快捷接入、批量操作
- `channel-overview-tab`: 渠道详情概览 Tab，展示连通状态、用量成本、资源摘要、活动时间线

### Modified Capabilities

## Impact

- **前端页面**：移除 `src/pages/Providers/` 目录，重构 `src/pages/Channels/` 为统一入口
- **路由**：移除 `/providers` 路由，`/channels` 成为主入口
- **侧边栏导航**：合并"供应商"和"渠道"为单一"渠道管理"入口
- **组件迁移**：ProviderCreateModal → 渠道页面内快捷接入；TemplateLibrary → 快捷接入流程；BatchImportModal/BatchExportButton → 渠道页面操作栏
- **API 调用**：无新增 API，仅前端调用方式调整（Provider API 在渠道页面内按需调用）
