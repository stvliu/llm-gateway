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
