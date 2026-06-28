---
change: fix-channels-antd-v6-tests
design-doc: docs/superpowers/specs/2026-06-23-fix-channels-antd-v6-tests-design.md
base-ref: 4d6161430db8af04d84fe29807081d7678118fa6
---

# 修复 Channels 页 antd v6 测试选择器适配 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 antd v5→v6.3.7 升级导致的 Channels 页 23 个前端测试失败，使 `npx vitest run` 0 失败 0 回归，且不改动任何生产代码。

**Architecture:** 纯测试选择器适配。v6 改变了图标按钮的 accessible name 计算方式（从 Tooltip 中文 title 改为图标 aria-label 英文 edit/delete），并调整了部分 className/容器 DOM。适配方向：图标按钮选择器中文正则改英文；Modal.confirm 的 OK 按钮改用中文文案 name 定位（绕过 className/容器变化）；文案断言保留中文。ChannelCard 的删除/暂停入口在 Dropdown 菜单内，需改用 Dropdown 流程定位。

**Tech Stack:** Vitest 4.1.8 + @testing-library/react 14 + antd 6.3.7 + React 18 + i18next（zh-CN）。

---

## 关键参考：v6 适配规则总表（已实测确认）

执行任何任务前，先理解本表。所有规则已通过运行失败测试确认。

### v6 accessible name 变化（实测）

| 组件按钮 | 渲染方式 | v5 accessible name | v6 accessible name（实测）|
|---------|---------|--------------------|----|
| InlineEditableList 编辑 | `Button icon=<EditOutlined/>` 外包 `Tooltip title=t('inlineList.edit')` | `编辑` | `edit` |
| InlineEditableList 删除 | `Button icon=<DeleteOutlined/> danger` 外包 `Tooltip title=t('inlineList.delete')` | `删除` | `delete` |
| EndpointSection 编辑 | `Button icon=<EditOutlined/>` 外包 `Tooltip title=t('drawer.edit')` | `编辑` | `edit` |
| EndpointSection 删除 | `Button icon=<DeleteOutlined/> danger` 外包 `Tooltip title=t('drawer.delete')` | `删除` | `delete` |
| QuotaSettingsSection 编辑设置 | `Button icon=<EditOutlined/>` 外包 `Tooltip title=t('quota.editSettings')` | `编辑设置` | `edit` |
| ChannelCard 测试/详情/More/Primary | `Button icon=<XxxOutlined/>` 外包 `Tooltip` | 中文 Tooltip title | 英文图标 aria-label（如 `more`/`edit`/`eye`） |
| 文字按钮（保存/取消/添加/Modal OK）| `Button` 含文本子节点 | 文本（如 `保存`）| 文本（v6 不变）|

> 根因：v5 时 antd Tooltip 把 `title` 注入为按钮的 accessible name；v6 不再注入，按钮 accessible name 回落到图标 `<span aria-label="edit">` 的英文值。文字按钮的 accessible name 始终来自文本子节点，不受影响。

### 选择器适配规则

| 场景 | v5 旧选择器 | v6 新选择器 |
|-----|-----------|-----------|
| 图标编辑按钮 | `getAllByRole('button', { name: /编\s*辑/ })` | `getAllByRole('button', { name: /edit/i })` |
| 图标删除按钮（行内）| `getAllByRole('button', { name: /删\s*除/ })` | `getAllByRole('button', { name: /delete/i })` |
| 图标编辑设置按钮 | `getByRole('button', { name: /编辑设置/ })` | `getByRole('button', { name: /edit/i })` |
| 文字保存按钮 | `getByRole('button', { name: /保\s*存/ })` | **不变**（文字按钮 v6 不变）|
| Modal.confirm OK（useDangerConfirm，okText=`删除`）| `getAllByRole('button').find(b => b.className.includes('ant-btn-dangerous') && !b.className.includes('ant-btn-link') && b.closest('.ant-modal-confirm-btns'))` | `await screen.findByRole('button', { name: /^删\s*除$/ })`（用中文文案 name，不依赖 className）|
| Modal.confirm content 文案 | `screen.getByText(/中文业务文案/)` | **不变**（content 是组件传入的中文，v6 不改文案）|
| message.error 断言 | `vi.spyOn(message, 'error')` + `errorSpy.mock.calls` | **不变**（spy 单例 message，不依赖 DOM）|

### 重要边界

- **不引入 `aria-label` 到生产组件**（Design Doc 实现注意事项）。若 name 不稳定，用按位置 + 文案组合断言。
- **不改生产代码**。所有改动仅限 `gateway-console/src/pages/Channels/__tests__/` 下测试文件。`git diff` 应只含测试文件。
- **若某测试失败根因不是选择器而是 v6 实际行为变化**（如乐观更新回滚失效），暂停该测试，按 BLOCKED 上报协调者，不得自行改生产代码（Design Doc D3/R1）。

---

## 前置约定

- **工作目录**：所有 vitest 命令在 `gateway-console` 目录下执行：`cd gateway-console && npx vitest run <file>`
- **基线**：`4d6161430db8af04d84fe29807081d7678118fa6`（当前 HEAD，已是基线）
- **commit 规范**：每个任务结束 commit 一次，中文 message，格式 `test(channels): <适配描述>`
- **TDD 节奏**：每个任务先运行测试确认失败 → 适配选择器 → 运行确认通过 → commit

---

## Task 1: ChannelCard.delete.test.tsx —— 删除渠道危险确认（Dropdown 流程）

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/ChannelCard.delete.test.tsx`

**失败根因（实测）：** 测试用 `allBtns.find((b) => b.querySelector('.anticon-delete'))` 找行内删除按钮，但生产代码 `ChannelCard.tsx` 的删除入口在 Dropdown 菜单内（`buildMenuItems` 推入 `key:'delete'` 菜单项，label 是中文"删除"文本），DOM 中不存在含 `.anticon-delete` 的按钮。需改用 Dropdown 流程：点开 More Dropdown → 点"删除"菜单项 → 弹 Modal.confirm → 点 OK。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/ChannelCard.delete.test.tsx
```
预期：FAIL，`AssertionError: expected undefined to be defined`（第 84 行 `expect(deleteBtn).toBeDefined()`）

- [ ] **Step 2: 适配删除入口为 Dropdown 流程**

把第 81-85 行（找 `.anticon-delete` 按钮并点击）替换为点开 Dropdown + 点"删除"菜单项：

```tsx
    // 行内删除入口在 More Dropdown 菜单内（buildMenuItems 推入 key:'delete' 菜单项）
    // v6 下 MoreOutlined 按钮仍含 .anticon-more，用图标 class 定位触发按钮
    const allBtns = screen.getAllByRole('button');
    const dropdownTrigger = allBtns.find((b) => b.querySelector('.anticon-more'));
    expect(dropdownTrigger).toBeDefined();
    await user.click(dropdownTrigger!);

    // 等待 Dropdown 菜单展开，点"删除"菜单项（label 为中文"删除"文本）
    const deleteItem = await screen.findByRole('menuitem', { name: /删\s*除/ });
    await user.click(deleteItem);
```

- [ ] **Step 3: 适配 Modal.confirm OK 按钮（用中文文案 name 替代 className matcher）**

把第 96-105 行（className matcher 找 dangerOk）替换为用 OK 按钮中文文案定位。`useDangerConfirm` 的 `okText = t('actions.delete', { ns: 'common' })` = 中文"删除"：

```tsx
    // 点击 modal footer 中 dangerous OK 按钮
    // useDangerConfirm okText 为 common.actions.delete = "删除"（中文），用 name 定位绕过 className/容器变化
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);
```

> 注：菜单项"删除"是 `menuitem` role，Modal OK"删除"是 `button` role，两者不冲突。`findByRole('button', { name: /^删\s*除$/ })` 只匹配 Modal OK 按钮。

- [ ] **Step 4: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/ChannelCard.delete.test.tsx
```
预期：PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/ChannelCard.delete.test.tsx
git commit -m "test(channels): 适配 ChannelCard.delete 至 v6 Dropdown 流程与 Modal OK 文案定位"
```

---

## Task 2: ChannelCard.suspend.test.tsx —— 暂停二次确认（Dropdown 菜单展开）

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/ChannelCard.suspend.test.tsx`

**失败根因（实测）：** `await screen.findByText('暂停')` 找不到菜单项文本，Dropdown 菜单未展开（失败信息显示 DOM 中无菜单项）。v6 下 Dropdown click 触发后菜单渲染时机/方式变化，`findByText` 在菜单项上不可靠。改用 `findByRole('menuitem', { name: /暂停/ })`（findBy 系列自带重试，且 menuitem role 更精确）。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/ChannelCard.suspend.test.tsx
```
预期：FAIL，`TestingLibraryElementError: Unable to find an element with the text: 暂停`（第 88 行）

- [ ] **Step 2: 适配菜单项定位为 menuitem role**

把第 87-89 行（`findByText('暂停')` + click）替换为：

```tsx
    // 等待菜单展开后选 "暂停"（v6 Dropdown 菜单项用 menuitem role 定位更可靠）
    const suspendItem = await screen.findByRole('menuitem', { name: /暂停/ });
    await user.click(suspendItem);
```

- [ ] **Step 3: 适配 Modal 确认按钮定位（放宽 name 正则）**

把第 99-102 行（`getAllByRole('button', { name: /^确定$|^OK$|^确认$/ })` 取最后一个）替换为按 menuitem 之后弹出的 Modal footer 内 OK 按钮定位。`modal.confirm`（App.useApp）默认 OK 文案为中文"确定"：

```tsx
    // 点击确认按钮（OK）。v6 modal.confirm 默认 OK 文案"确定"，用 findByRole 等待 Modal 渲染
    const confirmBtn = await screen.findByRole('button', { name: /确\s*定|OK|确\s*认/i });
    await user.click(confirmBtn);
```

> 注：若 Step 2 运行后仍报"找不到 menuitem 暂停"，说明 v6 Dropdown 在 jsdom 下 click 未触发菜单展开。改用 `pointerDown` 触发：在 `await user.click(dropdownTrigger!)` 后追加 `await user.pointer({ target: dropdownTrigger!, keys: '[MouseLeft]' })`。若菜单项 label 非"暂停"，查 `getTransitionActionLabel(ACTIVE, SUSPENDED)` 对应 i18n 值。

- [ ] **Step 4: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/ChannelCard.suspend.test.tsx
```
预期：PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/ChannelCard.suspend.test.tsx
git commit -m "test(channels): 适配 ChannelCard.suspend 至 v6 Dropdown menuitem 与 Modal OK 定位"
```

---

## Task 3: CredentialSection.delete.test.tsx —— 删除凭证危险确认

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/CredentialSection.delete.test.tsx`

**失败根因：** 行内删除按钮是 InlineEditableList 的图标按钮，v6 accessible name 由"删除"变"delete"，`getAllByRole('button', { name: /删\s*除/ })` 返回空数组。Modal OK 按钮用 className matcher，v6 下 className/容器可能变化。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/CredentialSection.delete.test.tsx
```
预期：FAIL，`Unable to find an accessible element with the role "button" and name /删\s*除/`

- [ ] **Step 2: 适配行内删除按钮选择器（中文→英文）**

把第 108 行（行内删除按钮定位）：

```tsx
    // 旧
    const delBtns = screen.getAllByRole('button', { name: /删\s*除/ });
```
改为：

```tsx
    // v6 图标删除按钮 accessible name 为英文 "delete"
    const delBtns = screen.getAllByRole('button', { name: /delete/i });
```

- [ ] **Step 3: 适配 Modal OK 按钮（className matcher → 中文文案 name）**

把第 121-131 行（className matcher 找 dangerOk）替换为：

```tsx
    // 点击 OK 按钮（useDangerConfirm okText = common.actions.delete = "删除" 中文，用 name 定位）
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);
```

> 注：行内删除按钮 v6 name="delete"（英文），Modal OK 文案"删除"（中文），`/^删\s*除$/` 只匹配 Modal OK，不冲突。Modal content 文案断言（`getByText(/删除后无法恢复/)`、`getByText(/sk-abc123/)`）保持不变。

- [ ] **Step 4: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/CredentialSection.delete.test.tsx
```
预期：PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/CredentialSection.delete.test.tsx
git commit -m "test(channels): 适配 CredentialSection.delete 至 v6 图标按钮英文 name 与 Modal OK 文案定位"
```

---

## Task 4: EndpointSection.delete.test.tsx —— 删除端点危险确认

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/EndpointSection.delete.test.tsx`

**失败根因：** 同 Task 3。行内删除按钮 v6 name="delete"；Modal OK 用 className matcher。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/EndpointSection.delete.test.tsx
```
预期：FAIL，`Unable to find an accessible element with the role "button" and name /删\s*除/`

- [ ] **Step 2: 适配行内删除按钮选择器**

把第 98 行：

```tsx
    // 旧
    const delBtns = screen.getAllByRole('button', { name: /删\s*除/ });
```
改为：

```tsx
    const delBtns = screen.getAllByRole('button', { name: /delete/i });
```

- [ ] **Step 3: 适配 Modal OK 按钮**

把第 110-118 行（className matcher）替换为：

```tsx
    // 点击 modal footer 中的 dangerous OK 按钮（okText="删除" 中文）
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);
```

> Modal content 文案断言（`getByText(/流量将立即失败/)`、`getAllByText(/api\.example\.com/)`）保持不变。

- [ ] **Step 4: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/EndpointSection.delete.test.tsx
```
预期：PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/EndpointSection.delete.test.tsx
git commit -m "test(channels): 适配 EndpointSection.delete 至 v6 图标按钮英文 name 与 Modal OK 文案定位"
```

---

## Task 5: ModelMappingSection.delete.test.tsx —— 删除模型映射危险确认

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/ModelMappingSection.delete.test.tsx`

**失败根因：** 同 Task 3。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/ModelMappingSection.delete.test.tsx
```
预期：FAIL，`Unable to find an accessible element with the role "button" and name /删\s*除/`

- [ ] **Step 2: 适配行内删除按钮选择器**

把第 103 行：

```tsx
    // 旧
    const delBtns = screen.getAllByRole('button', { name: /删\s*除/ });
```
改为：

```tsx
    const delBtns = screen.getAllByRole('button', { name: /delete/i });
```

- [ ] **Step 3: 适配 Modal OK 按钮**

把第 117-125 行（className matcher）替换为：

```tsx
    // 点击 modal footer 中的 dangerous OK 按钮（okText="删除" 中文）
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);
```

> Modal content 文案断言（`getByText(/不再被路由到此渠道/)`、`getAllByText(/gpt-4o/)`）保持不变。

- [ ] **Step 4: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/ModelMappingSection.delete.test.tsx
```
预期：PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/ModelMappingSection.delete.test.tsx
git commit -m "test(channels): 适配 ModelMappingSection.delete 至 v6 图标按钮英文 name 与 Modal OK 文案定位"
```

---

## Task 6: CredentialSection.pulse.test.tsx —— 保存反馈脉冲

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/CredentialSection.pulse.test.tsx`

**失败根因：** 3 个用例都用 `getAllByRole('button', { name: /编\s*辑/ })` 找 InlineEditableList 的编辑图标按钮，v6 name="edit"。保存按钮是文字按钮（v6 不变）。脉冲 DOM 选择器（`.save-tip-ok`/`.save-tip-err`/`.save-pulse-error`）和 input querySelector 不受 v6 影响。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/CredentialSection.pulse.test.tsx
```
预期：FAIL，3 个用例均报 `Unable to find an accessible element with the role "button" and name /编\s*辑/`

- [ ] **Step 2: 适配 3 个用例的编辑按钮选择器**

文件中 `getAllByRole('button', { name: /编\s*辑/ })` 出现 3 次（第 116、140、164 行），全部替换：

```tsx
    // 旧
    const editBtns = screen.getAllByRole('button', { name: /编\s*辑/ });
```
改为：

```tsx
    // v6 图标编辑按钮 accessible name 为英文 "edit"
    const editBtns = screen.getAllByRole('button', { name: /edit/i });
```

> 用 `replace_all` 一次性替换 3 处。保存按钮 `getAllByRole('button', { name: /保\s*存/ })` 保持不变（文字按钮）。

- [ ] **Step 3: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/CredentialSection.pulse.test.tsx
```
预期：PASS（3 个用例全过）

> 若"乐观更新失败应回滚"用例仍失败，先确认是选择器问题还是 v6 form 行为变化（Design Doc D3）。若 `priorityInput` 的 `user.clear` + `user.type` 后值未变，属 v6 form 行为变化，按 BLOCKED 上报，不得改生产代码。

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/CredentialSection.pulse.test.tsx
git commit -m "test(channels): 适配 CredentialSection.pulse 编辑按钮至 v6 英文 name"
```

---

## Task 7: EndpointSection.pulse.test.tsx —— 保存反馈脉冲

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/EndpointSection.pulse.test.tsx`

**失败根因：** 3 个用例用 `screen.getByRole('button', { name: /编\s*辑/ })` 找 EndpointSection 的编辑图标按钮（`Tooltip title=t('drawer.edit')` + `EditOutlined`），v6 name="edit"。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/EndpointSection.pulse.test.tsx
```
预期：FAIL，3 个用例均报 `Unable to find an accessible element with the role "button" and name /编\s*辑/`

- [ ] **Step 2: 适配 3 个用例的编辑按钮选择器**

`getByRole('button', { name: /编\s*辑/ })` 出现 3 次（第 118、141、166 行），全部替换：

```tsx
    // 旧
    await user.click(screen.getByRole('button', { name: /编\s*辑/ }));
```
改为：

```tsx
    await user.click(screen.getByRole('button', { name: /edit/i }));
```

> 用 `replace_all` 一次性替换 3 处。保存按钮 `getByRole('button', { name: /保\s*存/ })` 保持不变。

- [ ] **Step 3: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/EndpointSection.pulse.test.tsx
```
预期：PASS（3 个用例全过）

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/EndpointSection.pulse.test.tsx
git commit -m "test(channels): 适配 EndpointSection.pulse 编辑按钮至 v6 英文 name"
```

---

## Task 8: ModelMappingSection.pulse.test.tsx —— 保存反馈脉冲

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/ModelMappingSection.pulse.test.tsx`

**失败根因：** 同 Task 6。3 个用例用 `getAllByRole('button', { name: /编\s*辑/ })`，v6 name="edit"。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/ModelMappingSection.pulse.test.tsx
```
预期：FAIL，3 个用例均报 `Unable to find an accessible element with the role "button" and name /编\s*辑/`

- [ ] **Step 2: 适配 3 个用例的编辑按钮选择器**

`getAllByRole('button', { name: /编\s*辑/ })` 出现 3 次（第 115、144、172 行），全部替换：

```tsx
    // 旧
    const editBtns = screen.getAllByRole('button', { name: /编\s*辑/ });
```
改为：

```tsx
    const editBtns = screen.getAllByRole('button', { name: /edit/i });
```

> 用 `replace_all` 一次性替换 3 处。保存按钮保持不变。

- [ ] **Step 3: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/ModelMappingSection.pulse.test.tsx
```
预期：PASS（3 个用例全过）

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/ModelMappingSection.pulse.test.tsx
git commit -m "test(channels): 适配 ModelMappingSection.pulse 编辑按钮至 v6 英文 name"
```

---

## Task 9: QuotaSettingsSection.pulse.test.tsx —— 保存反馈脉冲

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/QuotaSettingsSection.pulse.test.tsx`

**失败根因：** 2 个用例用 `screen.getByRole('button', { name: /编辑设置/ })` 找 QuotaSettingsSection 的编辑图标按钮（`Tooltip title=t('quota.editSettings')` + `EditOutlined`），v6 name="edit"（不是"edit settings"）。保存按钮是文字按钮（`{t('drawer.save')}` = "保存"），v6 不变。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/QuotaSettingsSection.pulse.test.tsx
```
预期：FAIL，2 个用例均报 `Unable to find an accessible element with the role "button" and name /编辑设置/`

- [ ] **Step 2: 适配 2 个用例的编辑设置按钮选择器**

`getByRole('button', { name: /编辑设置/ })` 出现 2 次（第 109、131 行），全部替换：

```tsx
    // 旧
    await user.click(screen.getByRole('button', { name: /编辑设置/ }));
```
改为：

```tsx
    // v6 图标编辑按钮 accessible name 为英文 "edit"（非 "edit settings"）
    await user.click(screen.getByRole('button', { name: /edit/i }));
```

> 用 `replace_all` 一次性替换 2 处。保存按钮 `getByRole('button', { name: /保\s*存/ })` 保持不变。

- [ ] **Step 3: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/QuotaSettingsSection.pulse.test.tsx
```
预期：PASS（2 个用例全过）

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/QuotaSettingsSection.pulse.test.tsx
git commit -m "test(channels): 适配 QuotaSettingsSection.pulse 编辑设置按钮至 v6 英文 name"
```

---

## Task 10: error-feedback.test.tsx —— message.error 错误反馈

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/error-feedback.test.tsx`

**失败根因（实测）：** 4 个用例的失败根因是**图标按钮选择器**（编辑/删除/编辑设置），不是 message.error DOM。`message.error` 用 `vi.spyOn(message, 'error')` 监听单例（Section 组件也 import 单例 `message`），spy 有效，断言保持不变。实测失败信息：`Unable to find an accessible element with the role "button" and name /编\s*辑/`，v6 name="edit"。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/error-feedback.test.tsx
```
预期：FAIL，4 个用例均报找不到 `/编\s*辑/` 或 `/删\s*除/` 或 `/编辑设置/`

- [ ] **Step 2: 适配 EndpointSection 用例的编辑按钮（第 149 行）**

```tsx
    // 旧
    await user.click(screen.getByRole('button', { name: /编\s*辑/ }));
```
改为：

```tsx
    await user.click(screen.getByRole('button', { name: /edit/i }));
```

> 该用例第 151 行保存按钮 `getByRole('button', { name: /保\s*存/ })` 保持不变。message.error spy 断言（第 153-161 行）保持不变。

- [ ] **Step 3: 适配 CredentialSection 用例的删除按钮 + Modal OK（第 197-208 行）**

行内删除按钮（第 197 行）：

```tsx
    // 旧
    const deleteBtns = screen.getAllByRole('button', { name: /删\s*除/ });
```
改为：

```tsx
    const deleteBtns = screen.getAllByRole('button', { name: /delete/i });
```

Modal OK 按钮（第 199-207 行 className matcher）改为：

```tsx
    // Modal 弹出后，footer 的 OK 是 useDangerConfirm okText="删除"（中文），用 name 定位
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);
```

> message.error spy 断言（第 210-214 行）保持不变。

- [ ] **Step 4: 适配 ModelMappingSection 用例的删除按钮 + Modal OK（第 246-257 行）**

行内删除按钮（第 246 行）：

```tsx
    // 旧
    const deleteBtns = screen.getAllByRole('button', { name: /删\s*除/ });
```
改为：

```tsx
    const deleteBtns = screen.getAllByRole('button', { name: /delete/i });
```

Modal OK 按钮（第 249-256 行 className matcher）改为：

```tsx
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);
```

> message.error spy 断言（第 259-263 行）保持不变。

- [ ] **Step 5: 适配 QuotaSettingsSection 用例的编辑设置按钮（第 294 行）**

```tsx
    // 旧
    await user.click(screen.getByRole('button', { name: /编辑设置/ }));
```
改为：

```tsx
    await user.click(screen.getByRole('button', { name: /edit/i }));
```

> 该用例第 296 行保存按钮保持不变。message.error spy 断言（第 298-302 行）保持不变。

- [ ] **Step 6: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/error-feedback.test.tsx
```
预期：PASS（4 个用例全过）

> 若某用例 message.error spy 未被调用（`expect(errorSpy).toHaveBeenCalled()` 超时），确认 Section 组件 import 的是单例 `message`（已核实：CredentialSection/EndpointSection/ModelMappingSection/QuotaSettingsSection 均 `import { ... message ... } from 'antd'`，与 spy 同一单例）。若仍失败，按 BLOCKED 上报。

- [ ] **Step 7: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/error-feedback.test.tsx
git commit -m "test(channels): 适配 error-feedback 图标按钮至 v6 英文 name，Modal OK 改文案定位"
```

---

## Task 11: InlineEditableList.test.tsx —— 删除回调（无 Popconfirm）

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/InlineEditableList.test.tsx`

**失败根因（实测）：** `getAllByRole('button', { name: /删\s*除/ })` 找 InlineEditableList 删除图标按钮，v6 name="delete"。实测失败信息确认 v6 按钮 name 为 `delete`。

- [ ] **Step 1: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/InlineEditableList.test.tsx
```
预期：FAIL，`Unable to find an accessible element with the role "button" and name /删\s*除/`

- [ ] **Step 2: 适配删除按钮选择器（中文→英文）**

把第 76 行：

```tsx
    // 旧
    const deleteBtns = screen.getAllByRole('button', { name: /删\s*除/ });
```
改为：

```tsx
    // v6 图标删除按钮 accessible name 为英文 "delete"
    const deleteBtns = screen.getAllByRole('button', { name: /delete/i });
```

> Popconfirm 断言（第 87-88 行 `queryByRole('button', { name: /^确认$/ })`）保持不变——它验证无 Popconfirm 二次确认，v6 下 Popconfirm 未插入，断言仍为 null。

- [ ] **Step 3: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/InlineEditableList.test.tsx
```
预期：PASS

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/pages/Channels/__tests__/InlineEditableList.test.tsx
git commit -m "test(channels): 适配 InlineEditableList 删除按钮至 v6 英文 name"
```

---

## Task 12: 全量 vitest 回归

**Files:**
- 无文件改动（验证任务）

**目标：** 确认 23 个失败测试全过 + 0 回归。

- [ ] **Step 1: 运行 Channels 页全量测试**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/
```
预期：所有测试 PASS，0 失败 0 回归。失败数应从 23 降为 0。

- [ ] **Step 2: 运行前端全量测试（确认无跨页回归）**

```bash
cd gateway-console && npx vitest run
```
预期：全量 PASS，0 失败。

> 若出现非 Channels 页的失败，先确认是否本次改动引入（本次仅改测试文件，不应影响其他页）。若失败与本次无关，记录但不算回归；若相关，回退排查。

- [ ] **Step 3: Commit（如有回归修复）**

若 Step 1-2 全过，无需额外 commit。若修复了额外回归：

```bash
git add gateway-console/src/pages/Channels/__tests__/
git commit -m "test(channels): 修复 v6 测试选择器适配回归"
```

---

## Task 13: 前端构建验证

**Files:**
- 无文件改动（验证任务）

- [ ] **Step 1: 运行前端构建**

```bash
cd gateway-console && npm run build
```
预期：构建通过，无 TypeScript / 编译错误。

> 本次仅改测试文件，构建不应受影响。此步骤确认测试改动未意外引入类型错误（如选择器正则类型）。

---

## Task 14: 确认 git diff 仅测试文件

**Files:**
- 无文件改动（验证任务）

**目标：** 确认未改动任何生产代码（Design Doc Non-Goal）。

- [ ] **Step 1: 检查 git diff 文件范围**

```bash
git diff --name-only 4d6161430db8af04d84fe29807081d7678118fa6 HEAD
```
预期：所有变更文件均位于 `gateway-console/src/pages/Channels/__tests__/` 下，无生产代码文件（`.tsx` 非 `__tests__` 路径、`useDangerConfirm.tsx`、`InlineEditableList.tsx` 等）。

- [ ] **Step 2: 若发现生产代码改动，回退**

若 Step 1 输出含非测试文件：

```bash
git checkout <生产代码文件路径>
```
重新评估该改动是否必要。Design Doc 明确 Non-Goal：不改生产组件。若确需改生产代码（如 v6 form 行为变化使乐观更新失效），按 BLOCKED 上报协调者，不得自行推进。

---

## Self-Review 总结

**Spec 覆盖：** Design Doc 适配策略表 4 类失败 + tasks.md 13 任务全部覆盖：
- Modal.confirm 危险确认（5 测试）→ Task 1-5
- 保存反馈脉冲 pulse（9 测试，跨 4 文件）→ Task 6-9
- message.error 错误反馈（4 测试）→ Task 10
- Popconfirm（1 测试）→ Task 11
- 验证（3 任务）→ Task 12-14

**v6 适配规则已实测确认：**
- 图标按钮 accessible name 中文→英文（edit/delete）—— InlineEditableList + error-feedback 失败信息确认
- 文字按钮 / Modal content 文案 / message.error spy —— v6 不变
- ChannelCard 删除在 Dropdown 菜单 —— ChannelCard.tsx 源码 + ChannelCard.delete 失败确认
- Modal OK 按钮改用中文文案 name 定位（绕过 className/容器变化）

**类型一致：** 所有 `/edit/i`、`/delete/i`、`/^删\s*除$/` 选择器在跨任务中用法一致。`findByRole`（异步等待）用于 Modal/菜单展开后定位，`getByRole`/`getAllByRole`（同步）用于初始渲染存在的按钮。
