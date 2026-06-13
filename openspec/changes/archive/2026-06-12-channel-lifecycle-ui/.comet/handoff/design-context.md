# Comet Design Handoff

- Change: channel-lifecycle-ui
- Phase: design
- Mode: compact
- Context hash: 34624a97db1505e772416d2828239977587ca60e277eb718e48e552eb260cf44

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/channel-lifecycle-ui/proposal.md

- Source: openspec/changes/channel-lifecycle-ui/proposal.md
- Lines: 1-30
- SHA256: 1b21321583b43324e3f751c0b4f6911d9d0c6168743483d7af5ee2552f6c8520

```md
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
```

## openspec/changes/channel-lifecycle-ui/design.md

- Source: openspec/changes/channel-lifecycle-ui/design.md
- Lines: 1-100
- SHA256: f9850ecef30989b6d3c66797b811e7af8fade82de4f91122091acb6ea22ba505

[TRUNCATED]

```md
## Context

当前前端供应商管理由两个独立页面承担：
- **Providers 页面**（`src/pages/Providers/`）：25个组件，供应商品牌信息管理、模板库、批量导入/导出、连通性测试、供应商详情抽屉（6个Tab）
- **Channels 页面**（`src/pages/Channels/`）：13个组件，按供应商分组的渠道列表、渠道详情抽屉（4个Tab）、快捷接入向导

后端数据模型层级关系：
```
Provider (供应商/品牌)
  └── Channel (渠道) ← 核心聚合单元
        ├── ChannelEndpoint (端点) [1:N]
        ├── ChannelCredential (APIKey) [1:N]
        └── ChannelModel (模型关联) [N:M with Model，带定价]
```

关键洞察：**Channel 是核心聚合单元**，端点/Key/模型映射都挂在 Channel 下。Provider 更像品牌分类标签，不直接承载运行时配置。

目标用户：**技术管理者**，关注信息透明度、供应商对比、全生命周期管理效率。

## Goals / Non-Goals

**Goals:**
- 合并 Providers 和 Channels 为单一"渠道管理"页面，消除功能重叠
- 以 Channel 为核心组织信息，供应商作为分组维度
- 渠道详情抽屉增加概览 Tab，一屏展示关键运营指标
- 保留所有现有功能（模板库、批量导入/导出、连通性测试等），仅调整入口和组织方式
- 交互设计遵循：清晰直观、高效易用、愉悦可靠、包容可及、灵活一致

**Non-Goals:**
- 不新增后端 API 或修改后端数据模型
- 不涉及 Model 全局注册表页面的改动
- 不涉及团队/用户权限管理页面的改动
- 不做国际化/多语言支持（本阶段）
- 不做移动端适配（本阶段）

## Decisions

### D1: 页面合并策略 — 移除 Providers，扩展 Channels

**选择**：取消 Providers 页面，全部功能归入 Channels

**替代方案**：
- A) 保留两页面但厘清边界 → 仍需用户跨页面切换，核心问题未解决
- B) 渠道列表为主、供应商作为筛选条件 → 供应商品牌感弱化，不适合技术管理者按供应商维度管理

**理由**：Channel 是核心聚合单元，用户实际操作的是 Channel 及其子资源。供应商作为分组头部即可满足品牌维度管理需求。

### D2: 供应商在列表页的呈现方式 — 分组头部

**选择**：供应商作为可折叠的分组头部，渠道卡片在分组内展示

**理由**：
- 现有 `ChannelGroupedList` + `ProviderGroupHeader` 已实现此模式
- 分组头部支持 hover 操作（编辑供应商/停用/连通性测试），无需独立页面
- 分组间切换比页面间切换更高效

### D3: 渠道详情抽屉结构 — 概览 Tab + 4个详情 Tab

**选择**：新增「概览」Tab 作为默认页，包含：
1. 连通状态 + 延迟指标卡
2. 今日 Token/成本统计卡
3. 端点/Key/模型/配额摘要卡片（可点击跳转对应 Tab）
4. 最近活动时间线

原有的4个 Tab 保留：端点、API Key、模型映射、配额与设置

**理由**：
- 技术管理者最常查看的是运营状态而非逐项配置
- 概览→详情的渐进披露符合"清晰与直观"标准
- 概览页的摘要卡片提供快速跳转，减少 Tab 切换次数

### D4: 渠道卡片设计 — 统计 + 用量

**选择**：卡片展示端点/Key/模型三列数字统计 + 今日 Token 和成本

**理由**：
- 技术管理者关注用量和成本透明度
- 数字统计一目了然，比纯文字描述更直观
- 停用渠道降低透明度（opacity:0.5），视觉层次分明

```

Full source: openspec/changes/channel-lifecycle-ui/design.md

## openspec/changes/channel-lifecycle-ui/tasks.md

- Source: openspec/changes/channel-lifecycle-ui/tasks.md
- Lines: 1-46
- SHA256: 49021ced1816c9191a12c97a339bdff014be8b1bf32a5600f2a6d29d65510b77

```md
## Tasks

### 阶段一：页面结构重构

- [ ] T1: 移除 Providers 路由和侧边栏入口，统一为 `/channels` 路由
- [ ] T2: 将 Providers 页面独有的组件迁移到 Channels 目录（TemplateLibrary、BatchImportModal、BatchExportButton、YamlPreview、ConnectivityTestPanel）
- [ ] T3: 重构渠道列表页顶部操作栏：新增"快捷接入"、"批量导入"、"导出"按钮，新增分组/列表视图切换
- [ ] T4: 重构筛选栏：新增供应商筛选下拉、全局搜索（支持渠道名/模型名/端点URL）

### 阶段二：供应商分组头部增强

- [ ] T5: 增强 ProviderGroupHeader 组件：hover 显示操作按钮（编辑供应商/停用/连通性测试），常驻"更多"菜单图标
- [ ] T6: 实现供应商编辑 Modal：轻量弹窗修改品牌信息（名称、描述、官网、API文档地址）
- [ ] T7: 实现供应商停用确认弹窗：提示影响范围（将停用其下所有渠道）
- [ ] T8: 在分组头部集成连通性测试：点击后测试该供应商下所有渠道的端点

### 阶段三：渠道卡片增强

- [ ] T9: 重构 ChannelCard 组件：新增端点/Key/模型三列数字统计区域
- [ ] T10: 渠道卡片新增今日 Token 和成本展示（预留位置，无数据时显示"--"）
- [ ] T11: 停用渠道卡片视觉降级（opacity:0.5，统计数字灰色），并显示"最后活跃时间"

### 阶段四：渠道详情抽屉改造

- [ ] T12: 新增概览 Tab 作为默认页：连通状态卡、Token/成本统计卡、资源摘要卡片（端点/Key/模型/配额）
- [ ] T13: 概览页资源摘要卡片实现点击跳转到对应 Tab
- [ ] T14: 概览页最近活动时间线（预留位置，后续对接审计日志）
- [ ] T15: 抽屉头部内嵌快捷操作条（连通性测试/停用/删除），危险操作二次确认
- [ ] T16: 抽屉头部显示所属供应商信息（Logo + 名称，点击可跳转编辑供应商）

### 阶段五：新增供应商入口

- [ ] T17: 实现底部"新增供应商"虚线卡片入口
- [ ] T18: 点击新增供应商后弹出创建向导（从模板库选择或自定义配置）
- [ ] T19: 将 TemplateLibrary 组件集成到创建向导流程中

### 阶段六：列表视图模式

- [ ] T20: 实现列表视图（紧凑表格模式）：列包含供应商标签、渠道名、计费模式、优先级、端点/Key/模型数量、状态、操作
- [ ] T21: 分组/列表视图切换持久化到 localStorage

### 阶段七：清理与验证

- [ ] T22: 删除 `src/pages/Providers/` 目录及所有引用
- [ ] T23: 更新侧边栏导航：合并"供应商"和"渠道"为"渠道管理"
- [ ] T24: 端到端验证：快捷接入流程、渠道详情各 Tab 操作、供应商分组操作、批量导入/导出
```

## openspec/changes/channel-lifecycle-ui/specs/channel-lifecycle-page/spec.md

- Source: openspec/changes/channel-lifecycle-ui/specs/channel-lifecycle-page/spec.md
- Lines: 1-120
- SHA256: 1e385bfc2e7867f59f3b9237fc2e228a32f9f5d7737cc2dcc56cabded8115b86

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 渠道管理页面统一入口
系统 SHALL 提供单一"渠道管理"页面作为供应商和渠道管理的统一入口，移除独立的供应商页面。

#### Scenario: 用户访问渠道管理页面
- **WHEN** 用户点击侧边栏"渠道管理"入口
- **THEN** 系统展示渠道管理页面，URL 为 `/channels`，页面包含按供应商分组的渠道列表

#### Scenario: 供应商分组展示
- **WHEN** 渠道管理页面加载完成
- **THEN** 渠道按所属供应商分组展示，每个分组头部显示供应商 Logo、名称、渠道数量、模型数量、健康状态

#### Scenario: 供应商分组折叠
- **WHEN** 用户点击供应商分组头部
- **THEN** 该分组下的渠道卡片折叠/展开，折叠时仅显示分组头部

### Requirement: 供应商分组头部操作
供应商分组头部 SHALL 提供供应商级别的管理操作。

#### Scenario: 编辑供应商品牌信息
- **WHEN** 用户点击分组头部的"编辑供应商"操作
- **THEN** 系统弹出轻量 Modal，可修改供应商名称、描述、官网地址、API 文档地址

#### Scenario: 停用供应商
- **WHEN** 用户点击分组头部的"停用"操作
- **THEN** 系统弹出确认弹窗，提示"停用供应商将同时停用其下所有渠道"，明确列出受影响的渠道数量，用户确认后调用 Provider 停用 API

#### Scenario: 启用供应商
- **WHEN** 用户点击已停用供应商分组头部的"启用"操作
- **THEN** 系统启用该供应商，但不联动启用其下渠道（渠道需单独手动启用）

#### Scenario: 删除有渠道的供应商
- **WHEN** 用户尝试删除仍有渠道的供应商
- **THEN** 系统禁止删除操作，提示"请先删除或迁移该供应商下的 N 个渠道"

#### Scenario: 删除无渠道的供应商
- **WHEN** 供应商下无任何渠道且用户点击删除
- **THEN** 系统弹出确认弹窗，用户确认后删除供应商

#### Scenario: 供应商连通性测试
- **WHEN** 用户点击分组头部的"连通性测试"操作
- **THEN** 系统遍历该供应商下所有渠道的端点，逐个执行连通性测试，汇总展示结果

### Requirement: 渠道卡片增强展示
渠道卡片 SHALL 展示端点/Key/模型数量统计和运营指标。

#### Scenario: 渠道卡片统计展示
- **WHEN** 渠道处于启用状态
- **THEN** 卡片展示端点数量、Key 数量、模型数量三列数字统计，以及今日 Token 用量和成本

#### Scenario: 停用渠道视觉降级
- **WHEN** 渠道处于停用状态
- **THEN** 卡片整体透明度降低（opacity:0.5），统计数字变为灰色，显示"已停用"标签和最后活跃时间

#### Scenario: 配置不完整渠道警告
- **WHEN** 渠道的凭证数量为 0
- **THEN** 卡片显示"配置中"警告标签，Key 数量文字使用警告色

### Requirement: 分组与列表视图切换
渠道管理页面 SHALL 支持分组视图和列表视图两种展示模式。

#### Scenario: 切换到列表视图
- **WHEN** 用户点击视图切换按钮的"列表"选项
- **THEN** 渠道以紧凑表格展示，列包含：供应商标签、渠道名、计费模式、优先级、端点数、Key数、模型数、状态、操作

#### Scenario: 切换到分组视图
- **WHEN** 用户点击视图切换按钮的"分组"选项
- **THEN** 渠道恢复按供应商分组卡片展示

#### Scenario: 视图偏好持久化
- **WHEN** 用户切换视图模式
- **THEN** 偏好保存到 localStorage，下次进入页面自动恢复

### Requirement: 新增供应商入口
渠道管理页面 SHALL 提供新增供应商入口。

#### Scenario: 顶部新增供应商按钮
- **WHEN** 用户点击顶部操作栏的"新增供应商"按钮
- **THEN** 系统弹出供应商创建向导，支持从模板库选择或自定义配置
```

Full source: openspec/changes/channel-lifecycle-ui/specs/channel-lifecycle-page/spec.md

## openspec/changes/channel-lifecycle-ui/specs/channel-overview-tab/spec.md

- Source: openspec/changes/channel-lifecycle-ui/specs/channel-overview-tab/spec.md
- Lines: 1-83
- SHA256: f4a795fb490f9cfa8c168195f262fbc228daaf98606145765bc6206af986cab2

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 渠道概览 Tab
渠道详情抽屉 SHALL 新增"概览"Tab 作为默认页，一屏展示渠道关键运营指标。

#### Scenario: 打开渠道详情默认展示概览
- **WHEN** 用户点击渠道卡片打开详情抽屉
- **THEN** 系统默认展示"概览"Tab，包含连通状态、Token/成本统计、资源摘要、最近活动

### Requirement: 连通状态卡片
概览 Tab SHALL 展示渠道的连通性状态。

#### Scenario: 连通正常
- **WHEN** 渠道最近一次连通性测试通过
- **THEN** 显示绿色"连通正常"标识和延迟数值（如"延迟 230ms"）

#### Scenario: 连通异常
- **WHEN** 渠道最近一次连通性测试失败
- **THEN** 显示红色"连通异常"标识和失败原因

#### Scenario: 未测试
- **WHEN** 渠道从未执行连通性测试
- **THEN** 显示灰色"未测试"标识，并提供"立即测试"链接

### Requirement: Token/成本统计卡片
概览 Tab SHALL 展示今日 Token 用量和成本。

#### Scenario: 有用量数据
- **WHEN** 后端统计 API 返回用量数据
- **THEN** 显示今日 Token 总量（区分输入/输出）和今日成本，以及本月累计成本

#### Scenario: 暂无用量数据
- **WHEN** 后端统计 API 暂不可用或无数据
- **THEN** 显示"--"占位符，不影响其他信息展示

### Requirement: 资源摘要卡片
概览 Tab SHALL 以四宫格卡片展示端点/Key/模型/配额的摘要信息。

#### Scenario: 端点摘要卡片
- **WHEN** 概览 Tab 加载
- **THEN** 端点卡片显示端点总数，列出前 2 个端点地址和连通状态，底部"查看详情→"链接

#### Scenario: API Key 摘要卡片
- **WHEN** 概览 Tab 加载
- **THEN** Key 卡片显示 Key 总数，列出前 3 个 Key 的脱敏值和权重，底部"查看详情→"链接

#### Scenario: 模型映射摘要卡片
- **WHEN** 概览 Tab 加载
- **THEN** 模型卡片显示映射总数，列出前 2 个模型映射关系，底部"查看全部 N 个→"链接

#### Scenario: 配额与设置摘要卡片
- **WHEN** 概览 Tab 加载
- **THEN** 配额卡片显示配额上限状态和关键配置（超时、重试次数），底部"查看详情→"链接

#### Scenario: 摘要卡片跳转
- **WHEN** 用户点击任一摘要卡片的"查看详情"链接
- **THEN** 抽屉切换到对应的详情 Tab（端点/API Key/模型映射/配额与设置）

### Requirement: 最近活动时间线
概览 Tab SHALL 展示渠道的最近操作记录。

#### Scenario: 有活动记录
- **WHEN** 渠道存在最近操作记录
- **THEN** 按时间倒序展示最近 5 条活动，每条包含时间、操作描述

#### Scenario: 暂无活动记录
- **WHEN** 渠道暂无操作记录
- **THEN** 显示"暂无活动记录"占位文案

### Requirement: 抽屉头部快捷操作
渠道详情抽屉头部 SHALL 内嵌快捷操作条。

#### Scenario: 快捷操作展示
- **WHEN** 渠道详情抽屉打开
- **THEN** 头部展示"连通性测试"（蓝色主按钮）、"停用渠道"（灰色按钮，运行中时显示；已停用时显示"启用渠道"）、"删除"（红色按钮）

#### Scenario: 危险操作确认
- **WHEN** 用户点击"删除"按钮
- **THEN** 系统弹出 Popconfirm 二次确认，确认后才执行删除

```

Full source: openspec/changes/channel-lifecycle-ui/specs/channel-overview-tab/spec.md

