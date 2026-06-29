## Why

antd v5→v6 升级后，`gateway-console/src/pages/Channels/__tests__/` 下 23 个测试失败。根因是 antd v6 组件的可访问性树（accessible name）与 DOM 结构变化——按钮 accessible name 从中文「删除/编辑」变为英文「delete/edit」，Modal.confirm/message.error/Popconfirm 的 DOM 选择器也变化。**组件本身工作正常，仅测试选择器过时**，导致 `npm run test` 长期 23 个红，阻塞前端测试基线。

## What Changes

- 适配 23 个 Channels 测试选择器到 antd v6 可访问性树：
  - **Modal.confirm 危险确认**（ChannelCard.delete/suspend、CredentialSection.delete、EndpointSection.delete、ModelMappingSection.delete）：更新 Modal.confirm 文案与按钮选择器匹配 v6 DOM
  - **保存反馈脉冲**（CredentialSection/EndpointSection/ModelMappingSection/QuotaSettingsSection 的 pulse）：更新 `role "button" name /编\s*辑/` 等选择器匹配 v6 accessible name（英文 edit/delete），校验 save-tip-ok/save-pulse-error/乐观回滚断言
  - **message.error 错误反馈**（error-feedback.test）：更新 message.error DOM 选择器匹配 v6
  - **Popconfirm**（InlineEditableList）：v6 Popconfirm 可访问性变化，更新选择器
- 不改生产组件代码（仅修测试）
- 不改 antd 版本（已 v6.3.7）

## Capabilities

### New Capabilities
<!-- 无新 capability，纯测试修复 -->

### Modified Capabilities
<!-- 无 spec 级需求变更。测试选择器适配不改变组件行为契约，不产生 delta spec。 -->

### Removed Capabilities
<!-- 无 -->

## Impact

- **受影响文件**：`gateway-console/src/pages/Channels/__tests__/` 下约 10 个测试文件
- **风险**：低。纯测试改动，不影响运行时。需确保选择器适配后断言仍验证真实行为（非 tautology）
- **验证**：`cd gateway-console && npx vitest run` → 23 失败全过、0 回归；`npm run build` → 通过；git diff 仅测试文件
