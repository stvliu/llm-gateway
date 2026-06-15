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
