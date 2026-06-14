# Comet Design Handoff

- Change: channel-action-bar-redesign
- Phase: design
- Mode: compact
- Context hash: 2bb19ccb88f1f8bbecd65fe8066c1aafedc5e9edf6405aa46ef80a9918f8a78f

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/channel-action-bar-redesign/proposal.md

- Source: openspec/changes/channel-action-bar-redesign/proposal.md
- Lines: 1-36
- SHA256: 2668beaca060db62feb01d0d95bb50cdd6f74edb2bff43bfb419ef5f3a711ff5

```md
## Why

渠道页面的三处视图（卡片、表格行、详情抽屉）操作区存在布局不一致、概念标签混淆、高频操作隐藏等问题。管理员在管理渠道时需要频繁执行启用/停用等操作，但当前这些操作隐藏在 Dropdown 菜单中，且测试按钮对 PENDING/SUSPENDED 状态不可用。这降低了运维效率。

## What Changes

- **操作区布局重构**：三视图统一为 [⚡测试] [👁详情] [▶启用/⏸停用 Primary] [⋮更多 ▼] 布局，启用/停用从 Dropdown 独立为 Primary 按钮
- **删除按钮移入 Dropdown**：不再作为独立 danger 按钮，统一放入 Dropdown 底部（带分隔线），ACTIVE 状态禁用并提示"请先暂停渠道再删除"
- **测试按钮可用性放开**：从仅 `isRoutable`（ACTIVE/DEPRECATED）扩大到所有非 RETIRED 状态
- **Dropdown 按严重度排序**：ACTIVE → SUSPENDED → DEPRECATED → RETIRED
- **DEPRECATED 语义修正**：从 danger 色降为 warning 色（仍可路由，非危险操作）
- **标签统一**：SUSPENDED→RETIRED 和 DEPRECATED→RETIRED 从"废弃"改为"退役"
- **冗余消除**：`stateTransitions.ts` 委托给 `lifecycle.ts` SSOT
- **后端补充**：`ChannelActions.java` 增加 DEPRECATE/RETIRE 操作常量
- **国际化**：操作标签使用 i18n key，状态转换错误码映射到 i18n 提示

## Capabilities

### New Capabilities
- `channel-action-bar`: 渠道操作区布局组件，包含 Primary 按钮自动选择、Dropdown 排序、删除禁用规则

### Modified Capabilities
- 无（本次不改动 spec 级行为，仅调整 UI 布局和标签）

## Impact

- **gateway-console/src/utils/stateTransitions.ts** — 函数改为委托 lifecycle.ts
- **gateway-console/src/utils/channelActions.ts** — 新建文件，导出 getActionBarConfig()
- **gateway-console/src/utils/errorMessage.ts** — 新增错误码 i18n 映射
- **gateway-console/src/pages/Channels/ChannelCard.tsx** — 操作区重构
- **gateway-console/src/pages/Channels/ChannelTableView.tsx** — 操作区重构
- **gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx** — 操作区重构
- **gateway-console/src/pages/Channels/index.tsx** — 错误处理使用 i18n 映射
- **gateway-console/src/locales/*/channels.json** — 新增 i18n key
- **gateway-boot/.../entity/ChannelActions.java** — 新增 DEPRECATE/RETIRE
- **测试文件** — 更新 5 个 + 新建 1 个
```

## openspec/changes/channel-action-bar-redesign/design.md

- Source: openspec/changes/channel-action-bar-redesign/design.md
- Lines: 1-54
- SHA256: 4455448d9491b1c752274b127445fce7384f51ad9f71890ff2bf4659fe534647

```md
## Context

渠道页面当前有三处视图展示渠道信息并允许操作：卡片视图（`ChannelCard.tsx`）、表格视图（`ChannelTableView.tsx`）、详情抽屉（`ChannelDetailDrawer.tsx`）。每处视图都有测试、状态转换、删除等操作按钮，但布局和可用性规则不一致：

- 测试按钮仅在 `isRoutable`（ACTIVE/DEPRECATED）时可用，PENDING/SUSPENDED 不可测试
- 启用/停用等高频操作隐藏在"⋮ 更多"Dropdown 中
- 删除按钮始终以独立 danger 按钮显示
- DEPRECATED 被标记为 danger（语义错误——废弃仍可路由）
- Dropdown 菜单项无排序
- `stateTransitions.ts` 中转换规则与 `lifecycle.ts` 重复

## Goals / Non-Goals

**Goals:**
- 三视图操作区布局统一为 [⚡测试] [👁详情] [▶Primary] [⋮更多]
- 启用/停用作为 Primary 按钮独立展示
- 删除按钮移入 Dropdown，ACTIVE 时禁用+提示
- 测试按钮对所有非 RETIRED 状态可用
- Dropdown 按严重度排序：ACTIVE→SUSPENDED→DEPRECATED→RETIRED
- DEPRECATED 从 danger 改为 warning
- `stateTransitions.ts` 委托 `lifecycle.ts` SSOT
- 后端 `ChannelActions.java` 补充 DEPRECATE/RETIRE
- 操作标签使用 i18n key

**Non-Goals:**
- 不改动后端 `canTransitionTo()` 规则
- 不改动健康检查流程
- 不新增后端 API 端点

## Decisions

### 1. 操作区布局模式
**方案**：每个视图维护独立 JSX，但共享 `getActionBarConfig()` 工具函数
**理由**：三视图使用不同的 Ant Design 容器（Card/Table/Drawer），无法直接复用组件。共享工具函数可保证规则一致，各视图负责渲染

### 2. Primary 按钮确定规则
PENDING → ACTIVE（启用），ACTIVE → SUSPENDED（停用），SUSPENDED → ACTIVE（恢复）
DEPRECATED 和 RETIRED 无 Primary 按钮

### 3. 删除按钮放入 Dropdown
- Dropdown 底部加 `type: 'divider'` 分隔线
- ACTIVE 状态的删除项禁用 + Tooltip "请先暂停渠道再删除"
- 不设置 `disabled: true`（Ant Design Menu disabled 项不触发 Tooltip），改为在 onClick 中守卫

### 4. Dropdown 排序
按严重程度升序：ACTIVE(1) → SUSPENDED(2) → DEPRECATED(3) → RETIRED(4)

### 5. 错误码映射
后端错误码通过 `extractErrorCode()` 抽取，映射到 i18n key，通过 `t()` 翻译

## Risks / Trade-offs

- **[Dropdown disabled Tooltip 失效]** Ant Design v5 的 Menu disabled 项不触发鼠标事件 → 不使用 disabled 属性，改为 onClick 守卫 + label 内嵌 Tooltip
- **[测试按钮可用性放开]** PENDING/SUSPENDED 状态测试可能失败，但这是预期行为——测试失败也是信息，不影响渠道状态
```

## openspec/changes/channel-action-bar-redesign/tasks.md

- Source: openspec/changes/channel-action-bar-redesign/tasks.md
- Lines: 1-29
- SHA256: 24b88e286808d8a52a84b9fa66d2db92f60c4db835cca178da504929df1ab8e9

```md
## 1. 基础重构

- [ ] 1.1 消除 stateTransitions.ts 冗余 — 函数改为委托 lifecycle.ts，修正"废弃"→"退役"标签
- [ ] 1.2 后端补充 ChannelActions.java — 新增 DEPRECATE/RETIRE 操作常量

## 2. 共享工具

- [ ] 2.1 新建 channelActions.ts — 导出 getActionBarConfig()（primaryAction、dropdownTransitions、deleteDisabled）
- [ ] 2.2 errorMessage.ts 新增 extractErrorCode() 和 extractErrorMessageI18n()

## 3. 操作区重构

- [ ] 3.1 修正 DEPRECATED danger 语义 — ChannelDetailDrawer.tsx 第 377 行
- [ ] 3.2 改造 ChannelCard.tsx 操作区 — Primary 按钮 + 删除移入 Dropdown + 测试按钮放开
- [ ] 3.3 改造 ChannelTableView.tsx 操作区 — 与卡片对齐
- [ ] 3.4 改造 ChannelDetailDrawer.tsx 操作区 — 与卡片对齐

## 4. 国际化

- [ ] 4.1 i18n locales — zh-CN/en-US 新增操作标签和错误码 key
- [ ] 4.2 getTransitionActionLabel 使用 i18n key — 调用方改为 t() 翻译
- [ ] 4.3 index.tsx 错误处理 — 使用 extractErrorMessageI18n()

## 5. 测试

- [ ] 5.1 lifecycle.test.ts 添加一致性回归测试
- [ ] 5.2 新建 channelActions.test.ts — 测试 getActionBarConfig 五态输出
- [ ] 5.3 更新 ChannelCard 测试（delete/suspend/testIcon）
- [ ] 5.4 更新 ChannelDetailDrawer.healthMatrix 测试
```

## openspec/changes/channel-action-bar-redesign/specs/channel-action-bar/spec.md

- Source: openspec/changes/channel-action-bar-redesign/specs/channel-action-bar/spec.md
- Lines: 1-51
- SHA256: 0353270a58bbb232041f6eea9a67e8a0ff4ca04fab4a394dfbafca960de34127

```md
# 渠道操作区布局规范

## 操作区布局

三处视图（卡片、表格行、详情抽屉）统一使用以下布局：

```
[⚡ 测试] [👁 详情] [▶ 启用/⏸ 停用 Primary] [⋮ 更多 ▼]
                                              ┌──────────┐
                                              │ 废弃     │
                                              │ 退役     │
                                              │ ──────── │
                                              │ 删除     │
                                              └──────────┘
```

## Primary 按钮规则

| 当前状态 | Primary 按钮 | 操作 |
|---------|-------------|------|
| PENDING | ▶ 启用 | → ACTIVE |
| ACTIVE | ⏸ 停用 | → SUSPENDED |
| SUSPENDED | ▶ 恢复 | → ACTIVE |
| DEPRECATED | 无 | — |
| RETIRED | 无 | — |

## 测试按钮可用性

所有非 RETIRED 状态均可点击测试按钮。RETIRED 状态下按钮禁用+Tooltip。

## Dropdown 排序规则

按严重程度升序：ACTIVE(1) → SUSPENDED(2) → DEPRECATED(3) → RETIRED(4)

## 删除按钮规则

- 统一放在 Dropdown 底部，带分隔线
- ACTIVE 状态的删除项禁用，显示 Tooltip "请先暂停渠道再删除"
- 使用 `useDangerConfirm` 统一确认弹窗

## 操作标签

| 转换路径 | i18n key | 中文 |
|---------|---------|------|
| PENDING→ACTIVE | channel.action.activate | 激活 |
| ACTIVE→SUSPENDED | channel.action.suspend | 暂停 |
| ACTIVE→DEPRECATED | channel.action.deprecate | 标记下线 |
| SUSPENDED→ACTIVE | channel.action.enable | 恢复 |
| SUSPENDED→DEPRECATED | channel.action.deprecate | 标记下线 |
| SUSPENDED→RETIRED | channel.action.retire | 退役 |
| DEPRECATED→RETIRED | channel.action.retire | 退役 |
```

