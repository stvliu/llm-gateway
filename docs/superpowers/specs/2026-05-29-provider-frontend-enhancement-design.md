---
comet_change: provider-frontend-enhancement
role: technical-design
canonical_spec: openspec
---

# 供应商前端功能完整性提升 — 技术设计

## 1. 提供商统计标签页

**位置**: `ProviderManagementDrawer` 新增标签 "统计"（statistics）

**组件**: `ProviderStatisticsTab`

**后端新增**:

| 方法 | 端点 | 响应 |
|------|------|------|
| GET | `/api/v1/providers/{id}/stats` | `{ todayRequests, tokenUsage, errorRate, modelCount, channelCount, trend: [{ date, requests, tokens }] }` |

**数据流**: `ProviderStatisticsTab → useProviderStats(id) → statsApi.getByProvider(id)`

**UI 结构**: 4 个 Statistic 卡片（今日请求/Token 消耗/错误率/可用模型）+ 7 日趋势折线图（复用现有 TrendChart 组件）

**风险**: 统计聚合性能。方案：服务层做预聚合，前端只负责展示。

## 2. 批量启停操作

**后端新增**:

| 方法 | 端点 | 请求体 | 说明 |
|------|------|--------|------|
| PATCH | `/api/v1/providers/batch/state` | `{ ids: number[], state: "ACTIVE"\|"INACTIVE" }` | 全部成功或全部回滚 |

**前端变更**:
- `Providers/index.tsx`: 新增选择模式（rowSelection），Checkbox 列
- 工具栏新增"批量启用"/"批量停用"按钮（选中 ≥1 条时出现）

**UX**: 操作前确认弹窗，成功后刷新列表并清除选中状态。

## 3. 提供商复制

**前端变更**: `ProviderCreateModal` 新增步骤 "from-provider"

**流程**: 选择已有提供商 → `useProvider(originalId)` 获取数据 → 预填表单（清空 code/name） → 用户编辑 → `POST /api/v1/providers`

**约束**: code 必须唯一，创建后不可修改。

## 4. 创建流程扩展

物化/创建成功后，弹窗显示"是否立即配置渠道？" → 选择"配置"则打开渠道创建弹窗（复用 `ChannelFormModal`），完成后可继续配置凭证和模型关联。

## 5. ProviderCard 模型数据修复

将 `ProviderCard.tsx:25` 的 `useModels()` 替换为按 providerId 过滤的查询。后端 `GET /api/v1/models` 支持 `providerId` 查询参数。

## 6. 端点协议动态加载

`ChannelEndpointFormModal.tsx:46-49` 的 Select options 从 `protocolApi.list()` 动态获取，替换硬编码的 `[{ openai, anthropic }]`。

## 测试策略

| 功能 | 测试类型 | 方法 |
|------|---------|------|
| 统计标签页 | 单元 + 集成 | Mock stats API，验证图表渲染和数据绑定 |
| 批量启停 | 集成 | Mock 批量 API，验证选择→确认→反馈流程 |
| 提供商复制 | 集成 | Mock 源数据 API，验证表单预填和提交 |
| 创建流程扩展 | E2E | 模拟完整创建流程 |
| ProviderCard 修复 | 单元 | 验证 useProviderModels 返回正确过滤数据 |
| 渠道分页 | 集成 | 验证分页参数传递和页码切换 |

## 边界条件

- 统计接口返回空数据 → 图表显示空状态
- 批量操作部分失败 → 全部回滚，前端显示错误详情
- 复制时源提供商被删除 → 显示错误提示
- 零模型提供商 → 模型卡片区域显示空状态占位符