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
