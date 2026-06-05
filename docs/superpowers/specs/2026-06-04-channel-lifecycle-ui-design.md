---
comet_change: channel-lifecycle-ui
role: technical-design
canonical_spec: openspec
---

# 渠道生命周期管理页面 — 技术设计

## 1. 组件架构与文件组织

移除 `src/pages/Providers/`（25个组件），重构 `src/pages/Channels/` 为统一入口。

### 保留的 Channels 组件（增强）

| 组件 | 改动 |
|------|------|
| index.tsx | 增加筛选栏、视图切换、批量操作按钮、搜索增强 |
| ChannelGroupedList.tsx | 保留，配合增强的 ProviderGroupHeader |
| ChannelCard.tsx | 增加端点/Key/模型统计 + 用量展示 |
| ChannelDetailDrawer.tsx | 新增概览Tab，头部增加快捷操作条和供应商信息 |
| ChannelCreateWizard.tsx | 保留不变 |
| QuickOnboardMode.tsx | 保留不变 |
| EndpointSection.tsx | 保留不变 |
| CredentialSection.tsx | 保留不变 |
| ModelMappingSection.tsx | 保留不变 |
| QuotaSettingsSection.tsx | 保留不变 |
| InlineEditableList.tsx | 保留不变 |
| ProviderGroupHeader.tsx | 增加hover操作按钮、常驻更多菜单 |

### 从 Providers 迁移的组件（4个）

| 组件 | 迁移方式 |
|------|---------|
| TemplateLibrary.tsx | 直接迁移，集成到快捷接入流程 |
| BatchImportModal.tsx | 直接迁移，移至顶部操作栏 |
| BatchExportButton.tsx | 直接迁移，移至顶部操作栏 |
| ConnectivityTestPanel.tsx | 直接迁移，集成到分组头部操作 |

### 新增组件（3个）

| 组件 | 职责 |
|------|------|
| ChannelOverviewTab.tsx | 渠道概览Tab：连通状态、Token/成本、资源摘要4卡片、活动时间线 |
| ChannelTableView.tsx | 列表视图：紧凑表格模式 |
| ProviderEditModal.tsx | 供应商品牌信息编辑弹窗 |

### 移除的 Providers 组件（21个，功能由 Channels 组件取代）

ProviderCardView、ProvidersTableView、ProviderCard、ProviderManagementDrawer、ProviderBasicInfoTab、ProviderChannelTab、ExpertEndpointTab、ExpertCredentialTab、ExpertModelMappingTab、ExpertQuotaTab、ExpertAdvancedTab、ProviderCreateModal、BasicInfoStep、CredentialStep、ModelSetupStep、ChannelFormModal、ChannelEndpointFormModal、CredentialFormModal、ChannelModelsPanel、YamlPreview、index.tsx

## 2. 数据流与状态管理

### 列表页数据流

```
Channels/index.tsx
  ├── useProviders({ size: 100 })         → 供应商列表（分组+筛选）
  ├── useAllChannels()                     → 所有渠道（核心数据）
  ├── useChannelCredentialsBatch(ids)      → 批量凭证数量（统计）
  │
  ├── 筛选/搜索 → filteredGroups
  │     ├── ChannelGroupedList             → 分组视图
  │     │     ├── ProviderGroupHeader      → provider + 聚合统计
  │     │     └── ChannelCard              → channel + stats
  │     └── ChannelTableView               → 列表视图
  │
  ├── ChannelCreateWizard                  → useMaterializePlan / catalog API
  ├── BatchImportModal                     → 批量创建
  └── BatchExportButton                    → 导出 YAML
```

### 渠道详情抽屉数据流

```
ChannelDetailDrawer
  ├── useChannel(channelId)               → 渠道详情+端点
  ├── useChannelCredentials(channelId)     → 凭证列表
  ├── useChannelModels(channelId)          → 模型映射
  │
  ├── ChannelOverviewTab
  │     ├── 连通状态 → 本地缓存
  │     ├── Token/成本 → 预留，暂显示"--"
  │     ├── 资源摘要 → 从上述 hooks 提取
  │     └── 最近活动 → 预留
  ├── EndpointSection                     → 增删改查端点
  ├── CredentialSection                   → 增删改查Key
  ├── ModelMappingSection                 → 增删改查模型映射
  └── QuotaSettingsSection                → 配额设置
```

### 视图切换持久化

```typescript
type ViewMode = 'grouped' | 'table';
// localStorage key: 'channel-view-mode'
// 默认值: 'grouped'
```

### 搜索增强

前端过滤，不新增后端 API。匹配渠道名称和端点URL。模型名搜索懒加载（用户输入3+字符时触发，debounce 300ms），避免批量查询。

## 3. 交互细节

### 3.1 供应商生命周期

- 停用供应商 → 联动停用其下所有渠道（确认弹窗提示影响范围）
- 启用供应商 → 不联动启用渠道（需手动逐个启用）
- 删除供应商 → 前提无渠道，否则禁止并提示

### 3.2 创建供应商入口

- **顶部"快捷接入"按钮**：从模板一键创建供应商+渠道+端点+Key+模型（现有 ChannelCreateWizard）
- **底部"新增供应商"虚线卡片**：自定义创建供应商品牌信息，创建后需手动添加渠道

### 3.3 渠道详情抽屉

- 宽度 720px，从右侧滑入
- 头部：供应商Logo(32px) + 渠道名称 + 元信息行（供应商名 | 计费模式 | 优先级 | 权重）+ 状态标签 + 快捷操作条
- 5个Tab：概览(默认) / 端点 / API Key / 模型映射 / 配额与设置
- 概览Tab资源摘要4卡片(2×2)，点击"查看详情→"跳转对应Tab

### 3.4 渠道卡片

- 悬停：边框变蓝 + 微阴影
- 内容：名称+状态 + 计费/优先级 + 端点/Key/模型三列统计 + 今日Token/成本
- 停用：opacity 0.5，灰色数字，显示"已停用"+"最后活跃时间"
- Key=0：Key数字警告色 + "⚠ 配置中"标签

### 3.5 分组头部

- 常驻右侧"⋯"更多菜单图标
- 悬停展开：编辑供应商 / 停用 / 连通性测试
- 更多菜单：编辑 / 连通性测试 / 导出配置 / 停用

## 4. 测试策略

### 单元测试

- ChannelOverviewTab：摘要卡片渲染、Tab跳转、空数据占位
- ChannelTableView：表格列渲染、排序、筛选
- ProviderEditModal：表单校验、API调用
- ProviderGroupHeader：操作按钮显示/隐藏、确认弹窗

### 集成测试

- 供应商生命周期：创建→编辑→停用→启用→删除
- 渠道详情概览：打开→默认Tab→摘要卡片跳转
- 快捷接入：模板→四步向导→创建→列表刷新
- 批量导入：YAML→解析→创建→刷新
- 视图切换：分组↔列表→刷新恢复

### E2E关键路径

- 完整接入：新增供应商→快捷接入→配置→测试连通性
- 详情全Tab：概览→端点→Key→模型→配额
- 供应商停用联动：停用→渠道全部停用→启用供应商→渠道仍停用

## 5. 边界条件

| 条件 | 处理 |
|------|------|
| 供应商无渠道 | 分组正常显示，卡片区"暂无渠道" |
| 渠道无端点 | 概览端点数0，灰色 |
| 渠道无Key | 警告色+"⚠ 配置中" |
| 渠道无模型 | "暂无模型映射，点击添加" |
| 删除有渠道的供应商 | 禁止，提示先删除/迁移 |
| 停用供应商有运行中渠道 | 确认弹窗列出受影响渠道数 |
| 搜索无结果 | 空状态插画+"未找到匹配" |
| 连通性测试中 | loading+"测试中..."，禁止重复 |
| 批量导入格式错误 | 前端校验，高亮问题行 |
| Token/成本不可用 | 显示"--"占位符（非0） |
| localStorage损坏 | 回退默认"分组"视图 |

## 6. 性能考量

- 列表页一次加载，前端分组/筛选/搜索（预期 <100 渠道）
- 模型名搜索懒加载，3+字符触发，debounce 300ms
- 概览Tab连通状态本地缓存
- 用量数据预留接口，后续对接stats API
