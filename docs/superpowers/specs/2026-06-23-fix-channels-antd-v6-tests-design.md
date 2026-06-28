---
comet_change: fix-channels-antd-v6-tests
role: technical-design
canonical_spec: openspec
---

# Design: 修复 Channels 页 antd v6 测试选择器适配

## Context

antd v5→v6.3.7 升级后，`gateway-console/src/pages/Channels/__tests__/` 下 23 个测试失败。组件运行时行为正常（按钮渲染、Modal 弹出、message 提示均工作），失败根因是 testing-library 的可访问性查询与 v6 可访问性树不匹配：
- v5：按钮 accessible name 为中文（`删除`/`编辑`），测试用 `/删\s*除/` 正则匹配
- v6：按钮 accessible name 变为英文（`delete`/`edit`），中文正则失配

三类失败：Modal.confirm 危险确认（5 测试）、保存反馈脉冲 pulse（9 测试）、message.error 错误反馈（4 测试）、Popconfirm（1 测试）。

## Goals / Non-Goals

**Goals:**
- 23 个失败测试全过，`npx vitest run` 0 失败 0 回归
- 测试断言仍验证真实行为（非 tautology）
- 仅改测试，不改生产组件

**Non-Goals:**
- 不改 antd 版本、不改生产组件行为、不重构测试架构、不修复非 Channels 页测试

## Decisions

### D1：选择器适配优先用 accessible role + name
testing-library 推荐用 role/name 查询（与用户交互方式一致）。v6 的 accessible name 是英文。适配方向：中文正则改英文 name（`/edit/i`/`/delete/i`）。若 name 不稳定，用 `getAllByRole('button')[n]` 按位置 + adjacent text 断言。

### D2：Modal.confirm/message.error 文案断言保留中文
Modal.confirm 的 body 文案是组件传入的中文业务文案（如"不再参与任何流量分配"），v6 不改变传入文案，仅改变 Modal 容器 DOM。适配方向：更新 Modal 容器选择器，或用 `findByText` 直接找文案绕过容器选择器；文案断言不变。

### D3：pulse 乐观回滚个案确认
部分 pulse 测试涉及 form input 行为，需个案确认是选择器问题还是 v6 form 行为变化。若 v6 form 行为变化导致乐观更新逻辑失效，按 BLOCKED 上报（可能触及生产代码）——预期多数仍是选择器问题。

### D4：不建 delta spec
测试选择器适配不改变组件对外行为契约，无 spec 级需求变更。

## 适配策略（按失败类）

| 类别 | 失败数 | 适配策略 |
|------|--------|---------|
| 按钮 accessible name（pulse） | 9 | 中文正则 `/编\s*辑/`→英文 `/edit/i`；按位置 + 文案断言 save-tip-ok/save-pulse-error |
| Modal.confirm 容器（delete/suspend） | 5 | `findByText` 找中文业务文案绕过容器；确认按钮用 `getByRole('button', { name: /确认|ok/i })` |
| message.error DOM（error-feedback） | 4 | `findByText` 找错误原因文案；message 容器选择器更新 |
| Popconfirm（InlineEditableList） | 1 | 找删除按钮（英文 delete）点击，断言 onDelete 调用 + 无 Popconfirm |

## Risks / Trade-offs

- R1 个案可能触及生产代码（D3）：若 v6 form 行为变化使乐观更新逻辑失效，需修组件。预期低概率，遇则暂停上报。
- R2 选择器适配可能弱化断言：用英文 name 或按位置选择器可能不如中文正则精确。缓解：每个适配后确认四步断言不断（点删除→弹 Modal→含文案→确认→调回调）。
- R3 v6 可访问性树未来再变：本次适配到 v6.3.7，未来 v6 小版本可能再变。可接受（测试维护成本）。

## Testing

- 每个测试文件适配后单独跑 `npx vitest run <file>` 确认过
- 全量 `npx vitest run` → 23 失败全过 + 0 回归
- `npm run build` → 通过
- git diff 仅测试文件（无生产代码改动）

## 实现注意事项

- 适配时若发现某测试的失败根因不是选择器而是 v6 实际行为变化（如乐观更新回滚失效），暂停该测试，按 BLOCKED 上报协调者，不得自行改生产代码。
- 不引入 `aria-label` 到生产组件（保持纯测试改动）——若 name 不稳定，用按位置 + 文案组合断言。
