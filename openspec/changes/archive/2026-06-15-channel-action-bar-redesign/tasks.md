## 1. 基础重构

- [x] 1.1 消除 stateTransitions.ts 冗余 — 函数改为委托 lifecycle.ts，修正"废弃"→"退役"标签
- [x] 1.2 后端补充 ChannelActions.java — 新增 DEPRECATE/RETIRE 操作常量

## 2. 共享工具

- [x] 2.1 新建 channelActions.ts — 导出 getActionBarConfig()（primaryAction、dropdownTransitions、deleteDisabled）
- [x] 2.2 errorMessage.ts 新增 extractErrorCode() 和 extractErrorMessageI18n()

## 3. 操作区重构

- [x] 3.1 修正 DEPRECATED danger 语义 — ChannelDetailDrawer.tsx 第 377 行
- [x] 3.2 改造 ChannelCard.tsx 操作区 — Primary 按钮 + 删除移入 Dropdown + 测试按钮放开
- [x] 3.3 改造 ChannelTableView.tsx 操作区 — 与卡片对齐
- [x] 3.4 改造 ChannelDetailDrawer.tsx 操作区 — 与卡片对齐

## 4. 国际化

- [x] 4.1 i18n locales — zh-CN/en-US 新增操作标签和错误码 key
- [x] 4.2 getTransitionActionLabel 使用 i18n key — 调用方改为 t() 翻译
- [x] 4.3 index.tsx 错误处理 — 使用 extractErrorMessageI18n()

## 5. 测试

- [x] 5.1 lifecycle.test.ts 添加一致性回归测试
- [x] 5.2 新建 channelActions.test.ts — 测试 getActionBarConfig 五态输出
- [x] 5.3 更新 ChannelCard 测试（delete/suspend/testIcon）
- [x] 5.4 更新 ChannelDetailDrawer.healthMatrix 测试
