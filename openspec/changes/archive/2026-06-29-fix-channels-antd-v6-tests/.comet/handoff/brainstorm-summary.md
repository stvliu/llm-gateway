# Brainstorm Summary

- Change: fix-channels-antd-v6-tests
- Date: 2026-06-23

## 确认的技术方案

testing-library 查询从 antd v5 中文 accessible name 适配到 v6 英文 name / 稳定选择器。三类适配：
1. 按钮 accessible name（pulse 测试 `/编\s*辑/`/`/删\s*除/`）→ v6 英文 `edit`/`delete`，用 `getByRole('button', { name: /edit/i })` 或按位置 + 文案断言
2. Modal.confirm/message.error 容器（delete/error-feedback 测试）→ 文案断言保留中文，仅更新容器选择器或用 `findByText` 绕过容器
3. Popconfirm（InlineEditableList）→ 找删除按钮（英文 name delete）点击，断言 onDelete 调用 + 无 Popconfirm 弹出

关键原则：每个适配后断言语义不变（四步：点删除→弹 Modal→含文案→确认→调回调），避免 tautology。纯测试改动，不引入 aria-label 到生产组件。

## 关键取舍与风险

- R1 个案可能触及生产代码：pulse 乐观回滚若因 v6 form 行为变化失效需修组件，预期低概率，遇则 BLOCKED 上报
- R2 选择器弱化：英文 name/按位置不如中文正则精确，缓解：四步断言不断
- 取舍：不引入 aria-label 到生产组件（保持纯测试改动）

## 测试策略

- 每个测试文件适配后单独跑 `npx vitest run <file>` 确认过
- 全量 `npx vitest run` → 23 失败全过 + 0 回归
- `npm run build` → 通过
- git diff 仅测试文件（无生产代码改动）

## Spec Patch

无。测试选择器适配不改变组件行为契约，无 delta spec 变更。
