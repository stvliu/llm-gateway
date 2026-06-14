# Brainstorm Summary

- Change: channel-action-bar-redesign
- Date: 2026-06-15

## 确认的技术方案

1. **共享工具**：新建 `channelActions.ts` 导出 `getActionBarConfig(state)`，统一三视图的操作区规则
2. **三视图重构**：卡片/表格/抽屉统一布局 [⚡测试] [👁详情] [▶Primary] [⋮更多]，删除移入 Dropdown 底部
3. **Primary 按钮**：PENDING→启用、ACTIVE→停用、SUSPENDED→恢复、DEPRECATED/RETIRED→无
4. **测试按钮**：对所有非 RETIRED 状态可用
5. **Dropdown 排序**：ACTIVE(1)→SUSPENDED(2)→DEPRECATED(3)→RETIRED(4)
6. **DEPRECATED**：从 danger 改为 warning
7. **stateTransitions.ts**：委托 lifecycle.ts SSOT，"废弃"→"退役"标签修正
8. **后端**：ChannelActions.java 补充 DEPRECATE/RETIRE
9. **国际化**：操作标签和错误码使用 i18n key

## 关键取舍与风险

- Ant Design Menu disabled 不触发 Tooltip → 不设 disabled，用 onClick 守卫
- 测试按钮放开后 PENDING/SUSPENDED 测试可能报错 → 预期行为，Toast 展示即可

## 测试策略

- 单元测试 `getActionBarConfig` 五态输出
- 更新 `ChannelCard.delete.test.tsx` 适配删除移入 Dropdown
- 更新 `ChannelCard.suspend.test.tsx` 适配暂停为 Primary 按钮
- 更新 `ChannelCard.testIcon.test.tsx` 适配测试按钮可用性
- 更新 `ChannelDetailDrawer.healthMatrix.test.tsx` 适配 extra 区布局
- `lifecycle.test.ts` 添加一致性回归测试

## Spec Patch

- 新建 `specs/channel-action-bar/spec.md`（已创建）
