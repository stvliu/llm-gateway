# Comet Design Handoff

- Change: fix-channels-antd-v6-tests
- Phase: design
- Mode: compact
- Context hash: d0797f2463a2dc8020168c97cb3b4b0492e06ca5a5e0a7aacd2487eba4c616b4

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/fix-channels-antd-v6-tests/proposal.md

- Source: openspec/changes/fix-channels-antd-v6-tests/proposal.md
- Lines: 1-30
- SHA256: b8aa42da20c00249055957d32c58d8f89fb2184e56f140ef946d9513a6afbca7

```md
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
```

## openspec/changes/fix-channels-antd-v6-tests/design.md

- Source: openspec/changes/fix-channels-antd-v6-tests/design.md
- Lines: 1-33
- SHA256: 747e4299e3afe686f8bb2051572bc512fc73b83ed69ffff3854a532504f396c5

```md
## Context

antd v6.3.7 升级后，Channels 页 23 个测试失败。组件运行时行为正常（按钮渲染、Modal 弹出、message 提示均工作），失败根因是 testing-library 的可访问性查询与 v6 可访问性树不匹配：
- v5：按钮 accessible name 为中文（`删除`/`编辑`），测试用 `/删\s*除/` 正则匹配
- v6：按钮 accessible name 变为英文（`delete`/`edit`），中文正则失配

三类失败：Modal.confirm 危险确认、保存反馈脉冲（pulse）、message.error 错误反馈、Popconfirm。

## Goals / Non-Goals

**Goals:**
- 23 个失败测试全过，`npx vitest run` 0 失败 0 回归
- 测试断言仍验证真实行为（非 tautology）：选择器适配后，断言语义不变（如"点删除弹 Modal.confirm 含业务文案"仍验证该行为）
- 仅改测试，不改生产组件

**Non-Goals:**
- 不改 antd 版本
- 不改生产组件行为
- 不重构测试架构（不引入新测试工具/模式）
- 不修复非 Channels 页的测试（本次范围仅 Channels）

## Decisions

- **D1：选择器适配优先用 accessible role + name，而非 CSS class**。testing-library 推荐用 role/name 查询（与用户交互方式一致），v6 的 accessible name 是英文。适配方向：把中文正则改为英文 name 或 `aria-label`。若组件未设 `aria-label` 导致 name 不稳定，可在测试中用更稳定的选择器（如 `getAllByRole('button')` 按位置 + adjacent text 断言）。
- **D2：Modal.confirm/message.error 文案断言保留中文**。Modal.confirm 的 body 文案是组件传入的中文业务文案（如"不再参与任何流量分配"），v6 不改变传入文案，仅改变 Modal 容器 DOM。适配方向：更新 Modal 容器选择器（v6 的 `.ant-modal-confirm-body` 等），文案断言不变。
- **D3：乐观回滚/pulse 测试个案确认**。部分 pulse 测试涉及 form input 行为，需个案确认是选择器问题还是 v6 form 行为变化。若 v6 form 行为变化导致乐观更新逻辑需调整，按 BLOCKED 上报（可能触及生产代码）——但预期多数仍是选择器问题。
- **D4：不建 delta spec**。测试选择器适配不改变组件对外行为契约，无 spec 级需求变更。

## Risks / Trade-offs

- R1 个案可能触及生产代码（D3）：若 v6 form 行为变化使乐观更新逻辑失效，需修组件。预期低概率，遇则暂停上报。
- R2 选择器适配可能弱化断言：用英文 name 或按位置选择器可能不如中文正则精确。缓解：每个适配后确认断言仍验证原行为（如点删除→弹 Modal→含文案→确认→调回调，四步不断）。
- R3 v6 可访问性树未来再变：本次适配到 v6.3.7，未来 v6 小版本可能再变。可接受（测试维护成本）。
```

## openspec/changes/fix-channels-antd-v6-tests/tasks.md

- Source: openspec/changes/fix-channels-antd-v6-tests/tasks.md
- Lines: 1-28
- SHA256: 05f1623b2662239f6227db3103ad967c78c4ef6382f16ce030543f70373acb67

```md
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
```

