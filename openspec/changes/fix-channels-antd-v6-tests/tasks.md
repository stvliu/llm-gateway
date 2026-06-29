## 1. Modal.confirm 危险确认测试适配

- [x] 1.1 ChannelCard.delete.test.tsx：适配"点击删除弹 Modal.confirm 含业务文案"——删除入口改 Dropdown 流程（点 More Dropdown→点删除菜单项）+ Modal OK 中文文案 name 定位；mock state ACTIVE→SUSPENDED（生产代码 ACTIVE 禁用删除）
- [x] 1.2 ChannelCard.suspend.test.tsx：适配"Dropdown 选暂停弹二次确认"——"暂停"是 primaryAction 主按钮（非 Dropdown 菜单项），用 .anticon-pause-circle 图标 class 定位 + Modal OK 放宽 name 正则
- [x] 1.3 CredentialSection.delete.test.tsx：适配"弹 Modal.confirm 含 keyMasked + 删除后无法恢复"——行内删除按钮 /删\s*除/→/delete/i + Modal OK /^删\s*除$/
- [x] 1.4 EndpointSection.delete.test.tsx：同 1.3 适配
- [x] 1.5 ModelMappingSection.delete.test.tsx：同 1.3 适配

## 2. 保存反馈脉冲测试适配

- [x] 2.1 CredentialSection.pulse.test.tsx：适配 save-tip-ok / save-pulse-error / 乐观回滚——编辑按钮 /编\s*辑/→/edit/i（3 处）；v6 form 行为正常无 BLOCKED
- [x] 2.2 EndpointSection.pulse.test.tsx：同 2.1 适配
- [x] 2.3 ModelMappingSection.pulse.test.tsx：同 2.1 适配 + 补 updateModel mock（生产代码 a9e54f1 后调 updateModel，测试 mock 未同步，绑定到 updateUpstreamMock 保持断言语义）
- [x] 2.4 QuotaSettingsSection.pulse.test.tsx：适配 /编辑设置/→/edit/i（2 处）

## 3. message.error 错误反馈测试适配

- [x] 3.1 error-feedback.test.tsx：4 个用例失败根因是图标按钮选择器（非 message DOM）——编辑/删除/编辑设置按钮 name 英文化适配；message.error spy 断言不变

## 4. Popconfirm 测试适配

- [x] 4.1 InlineEditableList.test.tsx：适配"点击删除按钮直接调 onDelete"——删除按钮 /删\s*除/→/delete/i；Popconfirm 断言不变

## 5. 连通性测试按钮适配（plan 遗漏补修）

- [x] 5.1 ChannelDetailDrawer.healthMatrix.test.tsx：连通性测试按钮（ApiOutlined 图标）v6 accessible name='api'（Tooltip title 不再注入 aria-label），/连通性测试|测试全部/→/api/i（2 用例）

## 6. 验证

- [x] 6.1 全量 vitest 回归：`cd gateway-console && npx vitest run` → 35 文件 134 测试全过、0 失败 0 回归
- [x] 6.2 前端构建：`cd gateway-console && npm run build` → 通过（39.70s）
- [x] 6.3 确认 git diff 仅测试文件（无生产代码改动）
