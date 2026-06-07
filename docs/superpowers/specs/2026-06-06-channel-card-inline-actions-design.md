---
comet_change: channel-card-inline-actions
role: technical-design
canonical_spec: openspec
---

# 渠道卡片行内操作 — 技术设计

补充 [channel-lifecycle-ui-design](./2026-06-04-channel-lifecycle-ui-design.md) 第 3.4 节，明确渠道卡片的行内操作布局与交互。

## 1. 问题

渠道操作入口分散：启停在分组头部菜单、凭证在详情抽屉、测试在详情抽屉、编辑也在详情抽屉。用户完成一个简单操作（如停用渠道）需要 3-4 步点击。

## 2. 方案：右上角图标组

操作按钮放在卡片右上角。高频操作常驻显示，低频操作收入 ⋮ Dropdown。状态标签移至第二行，避免与操作按钮空间竞争。

```
┌──────────────────────────────────────┐
│ openai-gpt4o-main          ⏯  🔗  ⋮ │
│ 🟢 活跃 · 3 端点 · 2 Key · 5 模型  │
│ [HTTPS] [GPT-4o] [Claude]           │
└──────────────────────────────────────┘
```

### 布局规则

- 第一行：渠道名称（左，`flex: 1`，溢出截断省略）+ 操作按钮组（右，固定宽度不收缩）
- 第二行：状态圆点 + 状态文字 + 统计信息，与操作按钮无空间竞争
- 状态圆点：活跃 `token.colorSuccess`（绿）、禁用 `token.colorTextQuaternary`（灰）
- 图标按钮带 Tooltip 辅助识别
- 所有颜色使用主题 token（`token.colorPrimary`、`token.colorTextSecondary` 等），禁用自定义颜色

### 响应式降级

窄屏下（< 640px）高频操作收入 ⋮ 菜单，卡片右上角只保留 ⋮ 一个入口，避免图标溢出。

## 3. 高频操作（常驻图标按钮）

### ⏯ 启停

| 项 | 说明 |
|----|------|
| 活跃时图标 | `PauseOutlined`，`token.colorTextSecondary` |
| 禁用时图标 | `PlayCircleOutlined`，`token.colorPrimary` |
| 交互 | 点击 → 确认弹窗（"确认停用该渠道？" / "确认启用该渠道？"）→ 确认后调用 API |
| 状态反馈 | 成功：按钮图标切换 + 状态圆点/文字更新；失败：`message.error` |
| Tooltip | "停用渠道" / "启用渠道" |

### 🔗 连通性测试

| 项 | 说明 |
|----|------|
| 图标 | `ThunderboltOutlined`，`token.colorTextSecondary` |
| 测试中图标 | `LoadingOutlined`，旋转动画，`token.colorPrimary` |
| 交互 | 点击 → 图标切换为 LoadingOutlined 旋转 → 调用测试 API → `message.success/error` 展示结果 → 图标恢复 |
| 成功提示 | `message.success('连通性测试通过，响应时间 xxxms')` |
| 失败提示 | `message.error('连通性测试失败：xxx')` |
| 防重复 | 测试中按钮 disabled + 图标旋转，防止重复发起 |
| 禁用态 | 渠道 INACTIVE 时测试按钮 disabled，Tooltip 显示"渠道已停用，无法测试" |
| Tooltip | "连通性测试" / "渠道已停用，无法测试" |

## 4. 低频操作（⋮ Dropdown）

| 操作 | 图标 | 说明 |
|------|------|------|
| 查看详情 | `EyeOutlined` | 打开详情抽屉（概览 Tab） |
| 编辑渠道 | `EditOutlined` | 编辑渠道基础属性 |
| 替换 Key | `SwapOutlined` | 跳转详情抽屉凭证 Tab |
| 添加端点 | `PlusCircleOutlined` | 跳转详情抽屉端点 Tab |
| 添加模型 | `FileAddOutlined` | 跳转详情抽屉模型 Tab |
| 复制主端点 URL | `CopyOutlined` | 复制主端点（第一个或标记为 default 的端点）地址到剪贴板，`message.success` 提示 |
| 删除渠道 | `DeleteOutlined` | 确认弹窗后删除，danger 样式 |

### 菜单分组

```typescript
const menuItems = [
  { key: 'detail', icon: <EyeOutlined />, label: '查看详情' },
  { key: 'edit', icon: <EditOutlined />, label: '编辑渠道' },
  { type: 'divider' },
  { key: 'credential', icon: <SwapOutlined />, label: '替换 Key' },
  { key: 'endpoint', icon: <PlusCircleOutlined />, label: '添加端点' },
  { key: 'model', icon: <FileAddOutlined />, label: '添加模型' },
  { type: 'divider' },
  { key: 'copyUrl', icon: <CopyOutlined />, label: '复制主端点 URL' },
  { type: 'divider' },
  { key: 'delete', icon: <DeleteOutlined />, label: '删除渠道', danger: true },
];
```

### 多端点场景

"复制主端点 URL"复制规则：优先复制标记为 `default` 的端点 URL；无 default 标记时复制第一个端点 URL；无端点时菜单项 disabled，Tooltip 显示"该渠道暂无端点"。

## 5. 卡片点击行为

- 点击卡片本身（非操作按钮区域）→ 打开详情抽屉（与现有行为一致）
- 操作按钮区域 `onClick` 阻止事件冒泡（`e.stopPropagation()`）
- ⋮ 按钮同样阻止冒泡

## 6. 与现有设计的关系

本设计替代 [channel-lifecycle-ui-design](./2026-06-04-channel-lifecycle-ui-design.md) 第 3.4 节中关于卡片交互的部分：

- **删除**：原"悬停边框变蓝 + 微阴影"保留，增加操作按钮区域
- **新增**：右上角图标组（启停 + 测试 + ⋮ Dropdown）
- **修改**：原"停用态 opacity 0.5"保留，启停按钮从分组头部移到卡片内
- **修改**：状态标签从右上角移至第二行，用圆点+文字替代 Badge
- **凭证/模型管理**：从仅详情抽屉可达 → 卡片 Dropdown 可直达对应 Tab

分组头部（ProviderGroupHeader）的操作菜单保持不变，其操作对象是供应商，与卡片操作（渠道级别）互不冲突。
