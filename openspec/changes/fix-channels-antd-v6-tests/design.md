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
