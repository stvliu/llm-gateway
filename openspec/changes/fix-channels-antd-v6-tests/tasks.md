## 1. Modal.confirm 危险确认测试适配

- [ ] 1.1 ChannelCard.delete.test.tsx：适配"点击删除弹 Modal.confirm 含业务文案"——v6 Modal 容器选择器更新，文案断言保留
- [ ] 1.2 ChannelCard.suspend.test.tsx：适配"Dropdown 选暂停弹二次确认"——Dropdown + Modal.confirm v6 选择器
- [ ] 1.3 CredentialSection.delete.test.tsx：适配"弹 Modal.confirm 含 keyMasked + 删除后无法恢复"
- [ ] 1.4 EndpointSection.delete.test.tsx：适配"弹 Modal.confirm 含 baseUrl + 路由到...流量将立即失败"
- [ ] 1.5 ModelMappingSection.delete.test.tsx：适配"弹 Modal.confirm 含 modelId + 不再被路由"

## 2. 保存反馈脉冲测试适配

- [ ] 2.1 CredentialSection.pulse.test.tsx：适配 save-tip-ok / save-pulse-error / 乐观回滚——按钮 accessible name 英文 edit/delete，更新选择器；个案确认 v6 form 行为
- [ ] 2.2 EndpointSection.pulse.test.tsx：同 2.1 适配
- [ ] 2.3 ModelMappingSection.pulse.test.tsx：同 2.1 适配
- [ ] 2.4 QuotaSettingsSection.pulse.test.tsx：同 2.1 适配

## 3. message.error 错误反馈测试适配

- [ ] 3.1 error-feedback.test.tsx：适配 4 个 message.error 断言（CredentialSection/EndpointSection/ModelMappingSection/QuotaSettingsSection 失败时弹含后端原因的 message.error）——v6 message DOM 选择器

## 4. Popconfirm 测试适配

- [ ] 4.1 InlineEditableList.test.tsx：适配"点击删除按钮直接调 onDelete（不插入 Popconfirm 二次确认）"——v6 Popconfirm 可访问性，按钮 name 英文 delete

## 5. 验证

- [ ] 5.1 全量 vitest 回归：`cd gateway-console && npx vitest run` → 23 失败全过、0 回归
- [ ] 5.2 前端构建：`cd gateway-console && npm run build` → 通过
- [ ] 5.3 确认 git diff 仅测试文件（无生产代码改动）
