# Comet Design Handoff

- Change: provider-frontend-enhancement
- Phase: design
- Mode: compact
- Context hash: 96bf5a3cd2dfcfac630d4e285b59598ea130a195fe6c6202ac5c6da0e6ab1023

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/provider-frontend-enhancement/proposal.md

- Source: openspec/changes/provider-frontend-enhancement/proposal.md
- Lines: 1-41
- SHA256: 2a7d46638ac59e9d8a3c6e294192ad44ed1f94a56a6b15e5717de1502088effc

```md
## Why

供应商模块是 LLM-Gateway 的核心管理功能，但前端存在多项功能缺口和 UX 问题：无法查看提供商级统计、无批量操作、创建流程断层、部分页面模型数据错误、渠道无分页等。这些问题影响运营效率，需要在现有基础上补齐关键功能并优化交互体验。

## What Changes

### 功能补齐
- 新增提供商级统计面板（请求量/Token 消耗/错误率）
- 提供商列表支持批量启用/停用
- 提供商复制功能（以现有提供商为模板快速创建新提供商）
- 创建向导扩展：支持在创建提供商时一并完成渠道和初始模型关联
- 渠道列表添加服务端分页

### Bug 修复
- 修复 ProviderCard 中 `useModels()` 全量拉取问题，改为按提供商过滤

### 体验优化
- 端点协议选项从 `ProtocolController` 动态加载，替换硬编码
- 补充缺失的 mutation 成功反馈消息
- 优化渠道标签点击跳转到抽屉的渠道标签页

**BREAKING**: 无

## Capabilities

### New Capabilities
- `provider-statistics`: 提供商级统计信息（请求量、Token消耗、错误率），在提供商详情抽屉中新增标签页
- `batch-operations`: 提供商的批量启用/停用功能
- `provider-duplicate`: 提供商复制功能

### Modified Capabilities

无（项目中尚无正式 specs 定义）

## Impact

| 影响范围 | 说明 |
|---------|------|
| gateway-console/src/pages/Providers/ | 主页面、CardView、Drawer、ChannelTab 等 13 个文件，新增 3-4 个组件 |
| gateway-console/src/services/query/ | 新增 `useProviderStats` hook，扩展现有 hooks |
| gateway-console/src/locales/ | 补充新的 i18n 翻译键 |
| gateway-boot ProviderController | 可能需要新增统计相关 API 端点 |```

## openspec/changes/provider-frontend-enhancement/design.md

- Source: openspec/changes/provider-frontend-enhancement/design.md
- Lines: 1-38
- SHA256: 71e6be02bf0dc085dd979ea54416808d9c31d3dc734e5380408c37499923b0bc

```md
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
- **冗余文件**: `BasicInfoStep.tsx` 和 `ModelSetupStep.tsx` 暂不删除（保留旧创建流），标记为 deprecation```

## openspec/changes/provider-frontend-enhancement/tasks.md

- Source: openspec/changes/provider-frontend-enhancement/tasks.md
- Lines: 1-48
- SHA256: afc03b5baaa73929fbde5f72d49b57295b948345acb3278a0f6bd43a2f581cfe

```md
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

- [ ] **P2 渠道列表服务端分页**
  - [ ] ProviderChannelTab 改为分页加载
  - [ ] 扩展 `useChannels` 支持分页参数
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
  - [ ] ProviderCard 渠道标签点击后自动切换到抽屉渠道标签页```

