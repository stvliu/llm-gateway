# 渠道操作区布局规范

## ADDED Requirements

### Requirement: 操作区布局统一

三处视图（卡片、表格行、详情抽屉）SHALL 统一使用以下布局：
```
[⚡ 测试] [👁 详情] [▶ 启用/⏸ 停用 Primary] [⋮ 更多 ▼]
                                              ┌──────────┐
                                              │ 废弃     │
                                              │ 退役     │
                                              │ ──────── │
                                              │ 删除     │
                                              └──────────┘
```

#### Scenario: 卡片视图布局正确
When 查看渠道卡片，Then 操作区从左到右为 ⚡测试 → 👁详情 → ▶Primary 按钮 → ⋮更多 Dropdown，且 Dropdown 底部为删除项

#### Scenario: 表格视图布局正确
When 查看渠道表格行，Then 操作列与卡片视图布局一致

#### Scenario: 详情抽屉布局正确
When 打开渠道详情抽屉，Then extra 区为 ⚡连通性测试 → ▶Primary 按钮 → ⋮更多 Dropdown

### Requirement: Primary 按钮规则

渠道操作区 SHALL 按以下规则显示 Primary 按钮：
|---------|-------------|
| PENDING | ▶ 启用（→ACTIVE） |
| ACTIVE | ⏸ 停用（→SUSPENDED） |
| SUSPENDED | ▶ 恢复（→ACTIVE） |
| DEPRECATED | 无 |
| RETIRED | 无 |

#### Scenario: PENDING 卡片显示启用按钮
Given 渠道为 PENDING 状态，When 渲染卡片，Then 显示 Primary 按钮"激活"且点击后调用 ACTIVE 转换

#### Scenario: ACTIVE 卡片显示停用按钮
Given 渠道为 ACTIVE 状态，When 渲染卡片，Then 显示 Primary 按钮"暂停"且点击后调用 SUSPENDED 转换

#### Scenario: SUSPENDED 卡片显示恢复按钮
Given 渠道为 SUSPENDED 状态，When 渲染卡片，Then 显示 Primary 按钮"恢复"且点击后调用 ACTIVE 转换

#### Scenario: DEPRECATED 和 RETIRED 无 Primary 按钮
Given 渠道为 DEPRECATED 或 RETIRED 状态，When 渲染卡片，Then 不显示 Primary 按钮

### Requirement: 测试按钮可用性
渠道操作区 SHALL 确保所有非 RETIRED 状态可点击测试按钮

所有非 RETIRED 状态均可点击测试按钮。RETIRED 状态下按钮禁用并显示 Tooltip。

#### Scenario: ACTIVE 可测试
Given 渠道为 ACTIVE 状态，When 点击测试按钮，Then 触发测试回调

#### Scenario: PENDING 可测试
Given 渠道为 PENDING 状态，When 点击测试按钮，Then 触发测试回调

#### Scenario: RETIRED 不可测试
Given 渠道为 RETIRED 状态，When 鼠标悬停在测试按钮上，Then 显示 Tooltip 提示不可测试

### Requirement: 删除按钮放入 Dropdown
删除按钮 SHALL 统一放在 Dropdown 底部带分隔线

删除按钮统一放在 Dropdown 底部，带分隔线。ACTIVE 状态下删除项禁用并显示 Tooltip。

#### Scenario: ACTIVE 删除禁用
Given 渠道为 ACTIVE 状态，When 展开 Dropdown 菜单，Then 删除项显示为禁用样式且 hover 显示 Tooltip

#### Scenario: SUSPENDED 可删除
Given 渠道为 SUSPENDED 状态，When 点击 Dropdown 中的删除项，Then 弹出 useDangerConfirm 确认弹窗

### Requirement: Dropdown 按严重度排序
Dropdown 菜单项 SHALL 按 ACTIVE→SUSPENDED→DEPRECATED→RETIRED 顺序排列

Dropdown 菜单项按 ACTIVE → SUSPENDED → DEPRECATED → RETIRED 顺序排列。

#### Scenario: SUSPENDED Dropdown 顺序
Given 渠道为 SUSPENDED 状态，When 展开 Dropdown，Then 菜单项顺序为 DEPRECATED → RETIRED → 分隔线 → 删除

### Requirement: 操作标签国际化
状态转换操作标签 SHALL 使用 i18n key

状态转换操作标签使用 i18n key。

#### Scenario: 转换标签映射正确
When 调用 getTransitionActionLabel 函数，Then 返回对应的 i18n key 而非硬编码中文

### Requirement: 错误码国际化
后端状态转换错误码 SHALL 映射到 i18n 提示

后端状态转换错误码映射到 i18n 提示。

#### Scenario: 错误码映射
When 状态转换返回 CHANNEL_NO_ENDPOINT 错误，Then extractErrorMessageI18n 返回对应的 i18n 翻译
