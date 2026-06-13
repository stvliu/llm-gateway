## 供应商前端功能完整性提升 — 设计

### 架构决策

#### 1. 组件扩展方式
所有变更均在现有供应商模块内扩展，不引入新页面路由：

| 功能 | 位置 | 方式 |
|------|------|------|
| 提供商统计 | ProviderManagementDrawer 新增 "statistics" 标签页 | 新组件 ProviderStatisticsTab |
| 批量操作 | Providers 主页面工具栏 | 扩展现有工具栏，新增选择模式 |
| 提供商复制 | ProviderCreateModal 扩展 | 新增 "from-template" 步骤 |
| 创建流程扩展 | ProviderCreateModal 物化步骤后 | 新增渠道配置步骤 |
| 渠道分页 | ProviderChannelTab | 替换 Table 为服务端分页 |
| 端点协议加载 | ChannelEndpointFormModal | Select options 从 protocolApi 动态获取 |

#### 2. 数据流
```
提供商统计:
  ProviderStatisticsTab → useProviderStats(id) → statsApi.getByProvider(id) → GET /api/v1/providers/{id}/stats

批量操作:
  主页面选择模式 → ProviderBatchActions → batchUpdateApi → PATCH /api/v1/providers/batch/state

提供商复制:
  CreateModal → "from-provider" 步骤 → useProvider(originalId) → 预填表单 → POST /api/v1/providers
```

#### 3. 后端 API 新增

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/providers/{id}/stats` | 提供商级统计（需后端新增） |
| PATCH | `/api/v1/providers/batch/state` | 批量启停（需后端新增） |

#### 4. 现有 Bug 修复方案

- **ProviderCard 模型数据**: 将 `useModels()` 替换为按 providerId 过滤的查询，或添加 `useProviderModels(providerId)` hook
- **冗余文件**: `BasicInfoStep.tsx` 和 `ModelSetupStep.tsx` 暂不删除（保留旧创建流），标记为 deprecation