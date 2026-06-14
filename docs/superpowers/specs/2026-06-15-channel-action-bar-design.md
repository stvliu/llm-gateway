---
comet_change: channel-action-bar-redesign
role: technical-design
canonical_spec: openspec
---

# 渠道操作区布局重构 — Design Doc

## 背景

渠道页面的卡片视图（`ChannelCard.tsx`）、表格视图（`ChannelTableView.tsx`）和详情抽屉（`ChannelDetailDrawer.tsx`）三处操作区布局不一致，高频操作（启用/停用）隐藏在 Dropdown 中，测试按钮可用性过于保守，删除按钮始终暴露为独立 danger 按钮。本设计统一三视图操作区布局并修正概念标签。

## 实现方案

### 1. 新建共享工具 `channelActions.ts`

**路径**：`gateway-console/src/utils/channelActions.ts`

导出 `getActionBarConfig(state: ChannelState): ChannelActionBarConfig`：

```typescript
interface ChannelActionBarConfig {
  primaryAction: ChannelState | null;        // Primary 按钮目标状态
  dropdownTransitions: ChannelState[];       // Dropdown 菜单项（已排序，排除 primaryAction）
  deleteDisabled: boolean;                   // 删除是否禁用
  deleteDisabledTooltipKey: string;          // 禁用时 Tooltip i18n key
}
```

排序规则：`SEVERITY_ORDER = { ACTIVE: 1, SUSPENDED: 2, DEPRECATED: 3, RETIRED: 4 }`

### 2. 三视图操作区布局

**卡片（ChannelCard.tsx）**：
```
[⚡测试] [👁详情] [▶Primary] [⋮更多 ▼]
                         ┌──────────┐
                         │ 废弃     │
                         │ 退役     │
                         │ ──────── │
                         │ 删除     │
                         └──────────┘
```

**表格行（ChannelTableView.tsx）**：与卡片相同布局。

**详情抽屉（ChannelDetailDrawer.tsx）**：
```
[⚡连通性测试] [▶Primary] [⋮更多 ▼]
```

### 3. 删除按钮处理

- 使用 `type: 'divider'` 分隔线将删除与状态转换项隔开
- 不设 `disabled: true`（Ant Design v5 disabled 项不触发 Tooltip），在 onClick 中守卫
- ACTIVE 状态：在 label 中包裹 Tooltip，样式设为灰色模拟禁用

### 4. DEPRECATED 从 danger 改为 warning

仅在 `ChannelDetailDrawer.tsx` 第 377 行修正：
```diff
- danger: target === 'DEPRECATED' || target === 'RETIRED',
+ danger: target === 'RETIRED',
```

### 5. stateTransitions.ts 消除冗余

`getAvailableTransitions` → 委托 `allowedTransitions`（lifecycle.ts）
`isRoutableState` → 委托 `isRoutable`（lifecycle.ts）
`isTerminalState` → 委托 `CHANNEL_LIFECYCLE[s].nextStates.length === 0`

### 6. 后端 ChannelActions.java

新增 `DEPRECATE`、`RETIRE` 操作常量，级别 WARNING。

### 7. i18n

新增操作标签 key 和错误码映射 key。

## 涉及文件

| 文件 | 变更类型 |
|------|---------|
| `gateway-console/src/utils/channelActions.ts` | **新建** |
| `gateway-console/src/utils/stateTransitions.ts` | 修改 |
| `gateway-console/src/utils/errorMessage.ts` | 修改 |
| `gateway-console/src/pages/Channels/ChannelCard.tsx` | 修改 |
| `gateway-console/src/pages/Channels/ChannelTableView.tsx` | 修改 |
| `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx` | 修改 |
| `gateway-console/src/pages/Channels/index.tsx` | 修改 |
| `gateway-console/src/locales/zh-CN/channels.json` | 修改 |
| `gateway-console/src/locales/en-US/channels.json` | 修改 |
| `gateway-boot/.../entity/ChannelActions.java` | 修改 |
| 测试文件 5 个 + 新建 1 个 | 修改/新建 |

## 风险

- Dropdown disabled Tooltip 在 Ant Design v5 中无效 → 不使用 disabled 属性
- PENDING/SUSPENDED 测试可能失败 → 预期行为，Toast 展示后端错误
