# 渠道操作区布局重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 重构渠道页面三处视图（卡片、表格、抽屉）的操作区布局，统一操作按钮排列、修正概念标签、提高易用性

**Architecture:** 新建 `channelActions.ts` 共享工具函数提供操作区配置，三视图各自消费但保持布局一致。操作标签使用 i18n key 替代硬编码中文。

**Tech Stack:** TypeScript, React, Ant Design v5, React-i18next, Vitest

---
## File Structure

| 文件 | 职责 | 变更类型 |
|------|------|---------|
| `gateway-console/src/utils/channelActions.ts` | 操作区配置生成器（getActionBarConfig） | **新建** |
| `gateway-console/src/utils/stateTransitions.ts` | 转换规则委托 lifecycle.ts SSOT | 修改 |
| `gateway-console/src/utils/errorMessage.ts` | 新增 extractErrorCode/extractErrorMessageI18n | 修改 |
| `gateway-console/src/pages/Channels/ChannelCard.tsx` | 卡片操作区重构 | 修改 |
| `gateway-console/src/pages/Channels/ChannelTableView.tsx` | 表格操作区重构 | 修改 |
| `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx` | 抽屉操作区重构 | 修改 |
| `gateway-console/src/pages/Channels/index.tsx` | 错误处理使用 i18n 映射 | 修改 |
| `gateway-console/src/locales/zh-CN/channels.json` | 新增 i18n key | 修改 |
| `gateway-console/src/locales/en-US/channels.json` | 新增 i18n key | 修改 |
| `gateway-boot/.../domain/supply/entity/ChannelActions.java` | 新增 DEPRECATE/RETIRE | 修改 |

---

### Task 1: 消除 stateTransitions.ts 冗余

**Files:**
- Modify: `gateway-console/src/utils/stateTransitions.ts`
- Test: `gateway-console/src/domain/channel/__tests__/lifecycle.test.ts`

- [x] **Step 1: 修改 stateTransitions.ts，将三个函数改为委托 lifecycle.ts**

```typescript
import type { ChannelState } from '@/types/channel';
import { allowedTransitions, isRoutable, CHANNEL_LIFECYCLE } from '@/domain/channel/lifecycle';

export function getAvailableTransitions(currentState: ChannelState): ChannelState[] {
  return [...allowedTransitions(currentState)];
}

export function isTerminalState(state: ChannelState): boolean {
  return CHANNEL_LIFECYCLE[state].nextStates.length === 0;
}

export function isRoutableState(state: ChannelState): boolean {
  return isRoutable(state);
}
```

- [x] **Step 2: 修正"废弃"→"退役"标签**

```typescript
TRANSITION_ACTION_LABELS['SUSPENDED_RETIRED'] = '退役';
TRANSITION_ACTION_LABELS['DEPRECATED_RETIRED'] = '退役';
```

- [x] **Step 3: lifecycle.test.ts 添加一致性回归测试**

在文件末尾添加：
```typescript
import { getAvailableTransitions } from '@/utils/stateTransitions';
import type { ChannelState } from '@/types/channel';

it('allowedTransitions 与 stateTransitions getAvailableTransitions 一致（防漂移）', () => {
  const states: ChannelState[] = ['PENDING', 'ACTIVE', 'SUSPENDED', 'DEPRECATED', 'RETIRED'];
  for (const s of states) {
    expect([...allowedTransitions(s)]).toEqual(getAvailableTransitions(s));
  }
});
```

- [x] **Step 4: 运行测试验证**

Run: `npx vitest run src/domain/channel/__tests__/lifecycle.test.ts`
Expected: ALL PASS

- [x] **Step 5: Commit**

```bash
git add gateway-console/src/utils/stateTransitions.ts gateway-console/src/domain/channel/__tests__/lifecycle.test.ts
git commit -m "refactor(channel): stateTransitions 委托 lifecycle SSOT，修正退役标签"
```

---

### Task 2: 后端补充 ChannelActions.java

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ChannelActions.java`

- [x] **Step 1: 新增 DEPRECATE/RETIRE 常量**

在 `DELETE` 之后添加：
```java
/** 废弃渠道（仍可路由） */
public static final String DEPRECATE = "DEPRECATE";
/** 退役渠道（终态） */
public static final String RETIRE = "RETIRE";
```

在 `getLabel()` 的 switch 中添加：
```java
case DEPRECATE -> "废弃渠道";
case RETIRE -> "退役渠道";
```

在 `getLevel()` 的 switch 的 WARNING 分支中添加：
```java
case DEPRECATE, RETIRE -> LEVEL_WARNING;
```

- [x] **Step 2: 编译验证**

Run: `mvn compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [x] **Step 3: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ChannelActions.java
git commit -m "feat(channel): 新增 DEPRECATE/RETIRE 操作常量"
```

---

### Task 3: 新建 channelActions.ts 共享工具

**Files:**
- Create: `gateway-console/src/utils/channelActions.ts`

- [x] **Step 1: 创建 channelActions.ts**

```typescript
import { allowedTransitions } from '@/domain/channel/lifecycle';
import type { ChannelState } from '@/types/channel';

const SEVERITY_ORDER: Record<string, number> = {
  ACTIVE: 1,
  SUSPENDED: 2,
  DEPRECATED: 3,
  RETIRED: 4,
};

export interface ChannelActionBarConfig {
  /** Primary 按钮目标状态，null 表示不显示 Primary 按钮 */
  primaryAction: ChannelState | null;
  /** Dropdown 菜单项（已按严重度排序，排除 primaryAction） */
  dropdownTransitions: ChannelState[];
  /** 删除按钮是否禁用 */
  deleteDisabled: boolean;
  /** 禁用时 Tooltip 的 i18n key */
  deleteDisabledTooltipKey: string;
}

/**
 * 根据渠道状态生成操作区配置
 *
 * @param state 当前渠道状态
 * @returns 操作区配置（Primary 按钮、Dropdown 菜单项、删除状态）
 */
export function getActionBarConfig(state: ChannelState): ChannelActionBarConfig {
  const transitions = [...allowedTransitions(state)];

  // 按严重度排序：ACTIVE → SUSPENDED → DEPRECATED → RETIRED
  transitions.sort((a, b) => (SEVERITY_ORDER[a] ?? 99) - (SEVERITY_ORDER[b] ?? 99));

  // 确定 Primary 按钮
  let primaryAction: ChannelState | null = null;
  if (state === 'PENDING') primaryAction = 'ACTIVE';
  else if (state === 'ACTIVE') primaryAction = 'SUSPENDED';
  else if (state === 'SUSPENDED') primaryAction = 'ACTIVE';

  // Dropdown = 全部转换 - Primary（Primary 作为独立按钮展示）
  const dropdownTransitions = primaryAction
    ? transitions.filter(t => t !== primaryAction)
    : transitions;

  return {
    primaryAction,
    dropdownTransitions,
    deleteDisabled: state === 'ACTIVE',
    deleteDisabledTooltipKey: 'channel.action.deleteDisabledWhenActive',
  };
}
```

- [x] **Step 2: Commit**

```bash
git add gateway-console/src/utils/channelActions.ts
git commit -m "feat(channel): 新建 channelActions.ts 共享操作区配置工具"
```

---

### Task 4: errorMessage.ts 新增错误码映射

**Files:**
- Modify: `gateway-console/src/utils/errorMessage.ts`

- [x] **Step 1: 新增 extractErrorCode 和 extractErrorMessageI18n**

在 `extractErrorMessage` 函数之后添加：

```typescript
const ERROR_CODE_I18N_MAP: Record<string, string> = {
  CHANNEL_NOT_FOUND: 'channel.error.CHANNEL_NOT_FOUND',
  CHANNEL_NAME_DUPLICATE: 'channel.error.CHANNEL_NAME_DUPLICATE',
  INVALID_STATE_TRANSITION: 'channel.error.INVALID_STATE_TRANSITION',
  CHANNEL_NO_ENDPOINT: 'channel.error.CHANNEL_NO_ENDPOINT',
  CHANNEL_NO_CREDENTIAL: 'channel.error.CHANNEL_NO_CREDENTIAL',
  CHANNEL_NO_MODEL_INSTANCE: 'channel.error.CHANNEL_NO_MODEL_INSTANCE',
};

/**
 * 从 AxiosError 响应中提取后端错误码
 */
export function extractErrorCode(err: unknown): string | null {
  const maybeAxios = err as {
    isAxiosError?: boolean;
    response?: { data?: { error?: { code?: string } } };
  };
  if (maybeAxios.isAxiosError && maybeAxios.response?.data) {
    const data = maybeAxios.response.data as { error?: { code?: string } };
    if (data.error?.code) return data.error.code;
  }
  return null;
}

/**
 * 使用 i18n 提取后端错误码对应的用户友好提示
 * 无映射时回退到 extractErrorMessage
 */
export function extractErrorMessageI18n(
  err: unknown,
  t: (key: string, defaultValue?: string) => string,
): string {
  const code = extractErrorCode(err);
  if (code && ERROR_CODE_I18N_MAP[code]) {
    return t(ERROR_CODE_I18N_MAP[code]);
  }
  return extractErrorMessage(err);
}
```

- [x] **Step 2: Commit**

```bash
git add gateway-console/src/utils/errorMessage.ts
git commit -m "feat(channel): 新增错误码 i18n 映射工具函数"
```

---

### Task 5: i18n locales 新增 key

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/channels.json`
- Modify: `gateway-console/src/locales/en-US/channels.json`

- [x] **Step 1: zh-CN/channels.json 新增操作标签和错误码 key**

在 `channel` 命名空间下添加：

```json
{
  "channel": {
    "action": {
      "activate": "激活",
      "enable": "恢复",
      "suspend": "暂停",
      "deprecate": "标记下线",
      "retire": "退役",
      "deleteDisabledWhenActive": "请先暂停渠道再删除"
    },
    "error": {
      "CHANNEL_NOT_FOUND": "渠道不存在",
      "CHANNEL_NAME_DUPLICATE": "渠道名称已存在",
      "INVALID_STATE_TRANSITION": "当前状态不允许此操作",
      "CHANNEL_NO_ENDPOINT": "激活失败：请先添加端点",
      "CHANNEL_NO_CREDENTIAL": "激活失败：请先添加 API Key",
      "CHANNEL_NO_MODEL_INSTANCE": "激活失败：请先关联模型实例"
    }
  }
}
```

- [x] **Step 2: en-US/channels.json 新增对应英文 key**

```json
{
  "channel": {
    "action": {
      "activate": "Activate",
      "enable": "Enable",
      "suspend": "Suspend",
      "deprecate": "Deprecate",
      "retire": "Retire",
      "deleteDisabledWhenActive": "Please suspend the channel before deleting"
    },
    "error": {
      "CHANNEL_NOT_FOUND": "Channel not found",
      "CHANNEL_NAME_DUPLICATE": "Channel name already exists",
      "INVALID_STATE_TRANSITION": "This operation is not allowed in the current state",
      "CHANNEL_NO_ENDPOINT": "Activation failed: please add an endpoint first",
      "CHANNEL_NO_CREDENTIAL": "Activation failed: please add an API Key first",
      "CHANNEL_NO_MODEL_INSTANCE": "Activation failed: please link a model instance first"
    }
  }
}
```

- [x] **Step 3: Commit**

```bash
git add gateway-console/src/locales/zh-CN/channels.json gateway-console/src/locales/en-US/channels.json
git commit -m "feat(channel): 新增操作标签和错误码 i18n key"
```

---

### Task 6: getTransitionActionLabel 使用 i18n key

**Files:**
- Modify: `gateway-console/src/utils/stateTransitions.ts`

- [x] **Step 1: 替换 TRANSITION_ACTION_LABELS 为 i18n key 映射**

删除 `TRANSITION_ACTION_LABELS` 对象及其初始化代码，替换为：

```typescript
const TRANSITION_ACTION_I18N_KEYS: Record<string, string> = {
  PENDING_ACTIVE: 'channel.action.activate',
  ACTIVE_SUSPENDED: 'channel.action.suspend',
  ACTIVE_DEPRECATED: 'channel.action.deprecate',
  SUSPENDED_ACTIVE: 'channel.action.enable',
  SUSPENDED_DEPRECATED: 'channel.action.deprecate',
  SUSPENDED_RETIRED: 'channel.action.retire',
  DEPRECATED_RETIRED: 'channel.action.retire',
};

/**
 * 获取状态转换操作对应的 i18n key
 * 调用方需用 t() 翻译
 */
export function getTransitionActionLabel(from: ChannelState, to: ChannelState): string {
  return TRANSITION_ACTION_I18N_KEYS[`${from}_${to}`] ?? to;
}
```

- [x] **Step 2: Commit**

```bash
git add gateway-console/src/utils/stateTransitions.ts
git commit -m "refactor(channel): getTransitionActionLabel 使用 i18n key"
```

---

### Task 7: 修正 DEPRECATED danger 语义 + 改造三视图操作区

**Files:**
- Modify: `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx`（第 377 行）
- Modify: `gateway-console/src/pages/Channels/ChannelCard.tsx`
- Modify: `gateway-console/src/pages/Channels/ChannelTableView.tsx`
- Modify: `gateway-console/src/pages/Channels/index.tsx`

- [x] **Step 1: 修正 DEPRECATED danger 语义（ChannelDetailDrawer.tsx L377）**

```diff
- danger: target === 'DEPRECATED' || target === 'RETIRED',
+ danger: target === 'RETIRED',
```

- [x] **Step 2: 改造 ChannelCard.tsx 操作区**

关键变更：
1. 导入 `getActionBarConfig` from `@/utils/channelActions`
2. 获取配置：`const { primaryAction, dropdownTransitions, deleteDisabled } = getActionBarConfig(currentState)`
3. 测试按钮：`disabled` 从 `!isRoutable` 改为 `currentState === 'RETIRED'`
4. 操作区 JSX 改为：

```tsx
<Space size={2} style={{ flexShrink: 0 }} onClick={(e) => e.stopPropagation()}>
  {/* 1. 测试按钮 — 所有非 RETIRED 可用 */}
  <Tooltip title={currentState !== 'RETIRED' ? t('card.testConnect') : t('card.testDisabled')}>
    <Button type="text" size="small" icon={<ThunderboltOutlined />}
      disabled={currentState === 'RETIRED'}
      onClick={handleTestClick}
      style={{ opacity: currentState === 'RETIRED' ? 0.4 : 1 }}
    />
  </Tooltip>

  {/* 2. 详情按钮 */}
  <Tooltip title={t('card.viewDetail')}>
    <Button type="text" size="small" icon={<EyeOutlined />} onClick={handleDetailClick} />
  </Tooltip>

  {/* 3. Primary 按钮 */}
  {primaryAction && (
    <Button type="primary" size="small"
      onClick={() => handleTransition(primaryAction)}
    >
      {t(getTransitionActionLabel(currentState, primaryAction))}
    </Button>
  )}

  {/* 4. Dropdown — 剩余转换 + 删除 */}
  <Dropdown
    menu={{
      items: buildMenuItems(currentState, dropdownTransitions, deleteDisabled, t),
      onClick: ({ key }) => {
        if (key === 'delete') handleDeleteClick(e as unknown as React.MouseEvent);
        else handleTransition(key as ChannelState);
      },
    }}
    trigger={['click']}
  >
    <Button type="text" size="small" icon={<MoreOutlined />} />
  </Dropdown>
</Space>
```

新增 `buildMenuItems` 辅助函数：
```tsx
function buildMenuItems(
  currentState: ChannelState,
  transitions: ChannelState[],
  deleteDisabled: boolean,
  t: (key: string, fallback?: string) => string,
) {
  const items: any[] = transitions.map(target => ({
    key: target,
    label: t(getTransitionActionLabel(currentState, target)),
    danger: target === 'RETIRED',
  }));

  items.push({ type: 'divider' as const });

  items.push({
    key: 'delete',
    label: deleteDisabled
      ? <Tooltip title={t('channel.action.deleteDisabledWhenActive')}>
          <span style={{ color: 'rgba(0,0,0,0.25)', cursor: 'not-allowed' }}>{t('card.delete')}</span>
        </Tooltip>
      : t('card.delete'),
    danger: true,
  });

  return items;
}
```

注意：删除项的 `onClick` 在守卫中处理 — 当 `deleteDisabled` 为 true 时直接 return，不触发确认。

- [x] **Step 3: 改造 ChannelTableView.tsx 操作区**

与 ChannelCard 相同模式的操作区变更。在 Actions 列渲染中：
1. 导入 `getActionBarConfig` + `getTransitionActionLabel`（已导入）
2. 使用 `getActionBarConfig(currentState)` 获取配置
3. 渲染 [⚡测试] [👁详情] [▶Primary] [⋮更多 ▼] 布局
4. 删除按钮移入 Dropdown

- [x] **Step 4: 改造 ChannelDetailDrawer.tsx 操作区**

Drawer 的 `extra` 区域改为：
```tsx
<Space size={8}>
  {/* 1. 连通性测试 */}
  <Button icon={<ApiOutlined />} onClick={handleTest} loading={matrixLoading}
    type={testAllHighlight ? 'primary' : 'default'}
    data-testid="drawer-connectivity-test-btn"
  >
    {t('drawer.connectivityTest')}
  </Button>

  {/* 2. Primary 按钮 */}
  {primaryAction && (
    <Button type="primary" loading={transitionChannelState.isPending}
      onClick={() => handleTransition(primaryAction)}
    >
      {t(getTransitionActionLabel(currentState, primaryAction))}
    </Button>
  )}

  {/* 3. Dropdown — 剩余转换 + 删除 */}
  {dropdownTransitions.length > 0 && (
    <Dropdown
      menu={{
        items: buildMenuItems(currentState, dropdownTransitions, deleteDisabled, t),
        onClick: ({ key }) => {
          if (key === 'delete') handleDelete();
          else handleTransition(key as ChannelState);
        },
      }}
    >
      <Button loading={transitionChannelState.isPending}>
        <Space>
          {t('drawer.changeState')}
          <DownOutlined />
        </Space>
      </Button>
    </Dropdown>
  )}
</Space>
```

移除原有的独立 `DeleteOutlined` 按钮（第 391-405 行）。

添加 `buildMenuItems` 辅助函数（与 ChannelCard 中的相同）。

- [x] **Step 5: index.tsx 错误处理使用 i18n 映射**

在 `onStateTransition` 回调的 catch 块中：
```typescript
onStateTransition={async (id, targetState, reason) => {
  try {
    await transitionChannelState.mutateAsync({ id, targetState, reason });
    message.success(t('statusToggle.enabled'));
  } catch (err) {
    const msg = extractErrorMessageI18n(err, t);
    message.error(msg);
  }
}}
```

确保顶部已导入 `extractErrorMessageI18n`：
```typescript
import { extractErrorMessageI18n } from '@/utils/errorMessage';
```

- [x] **Step 6: 编译检查**

Run: `cd gateway-console && npx tsc --noEmit`
Expected: No TypeScript errors

- [x] **Step 7: Commit**

```bash
git add gateway-console/src/pages/Channels/ChannelCard.tsx gateway-console/src/pages/Channels/ChannelTableView.tsx gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx gateway-console/src/pages/Channels/index.tsx
git commit -m "feat(channel): 重构三视图操作区布局，统一 Primary 按钮 + Dropdown + 删除规则"
```

---

### Task 8: 更新测试

**Files:**
- Modify: `gateway-console/src/pages/Channels/__tests__/ChannelCard.delete.test.tsx`
- Modify: `gateway-console/src/pages/Channels/__tests__/ChannelCard.suspend.test.tsx`
- Modify: `gateway-console/src/pages/Channels/__tests__/ChannelCard.testIcon.test.tsx`
- Modify: `gateway-console/src/pages/Channels/__tests__/ChannelDetailDrawer.healthMatrix.test.tsx`
- Create: `gateway-console/src/utils/__tests__/channelActions.test.ts`

- [x] **Step 1: 新建 channelActions.test.ts**

```typescript
import { describe, it, expect } from 'vitest';
import { getActionBarConfig } from '@/utils/channelActions';

describe('getActionBarConfig', () => {
  it('PENDING → primaryAction=ACTIVE, dropdown 无 ACTIVE, delete 不禁用', () => {
    const config = getActionBarConfig('PENDING');
    expect(config.primaryAction).toBe('ACTIVE');
    expect(config.dropdownTransitions).not.toContain('ACTIVE');
    expect(config.deleteDisabled).toBe(false);
  });

  it('ACTIVE → primaryAction=SUSPENDED, delete 禁用', () => {
    const config = getActionBarConfig('ACTIVE');
    expect(config.primaryAction).toBe('SUSPENDED');
    expect(config.deleteDisabled).toBe(true);
    expect(config.dropdownTransitions).toEqual(['DEPRECATED']);
  });

  it('SUSPENDED → primaryAction=ACTIVE, dropdown 包含 DEPRECATED 和 RETIRED（按序）', () => {
    const config = getActionBarConfig('SUSPENDED');
    expect(config.primaryAction).toBe('ACTIVE');
    expect(config.dropdownTransitions).toEqual(['DEPRECATED', 'RETIRED']);
    expect(config.deleteDisabled).toBe(false);
  });

  it('DEPRECATED → primaryAction=null, dropdown 含 RETIRED', () => {
    const config = getActionBarConfig('DEPRECATED');
    expect(config.primaryAction).toBeNull();
    expect(config.dropdownTransitions).toEqual(['RETIRED']);
    expect(config.deleteDisabled).toBe(false);
  });

  it('RETIRED → primaryAction=null, dropdown 空, delete 不禁用', () => {
    const config = getActionBarConfig('RETIRED');
    expect(config.primaryAction).toBeNull();
    expect(config.dropdownTransitions).toEqual([]);
    expect(config.deleteDisabled).toBe(false);
  });

  it('dropdown 排序：ACTIVE(1) → SUSPENDED(2) → DEPRECATED(3) → RETIRED(4)', () => {
    // 直接构造一个有多项的场景验证排序
    // SUSPENDED 状态有 3 个可转换目标，排除 primary=ACTIVE 后剩 DEPRECATED 和 RETIRED
    const config = getActionBarConfig('SUSPENDED');
    const order = config.dropdownTransitions;
    expect(order.indexOf('DEPRECATED')).toBeLessThan(order.indexOf('RETIRED'));
  });
});
```

- [x] **Step 2: 更新 ChannelCard.delete.test.tsx**

删除按钮已从独立按钮移入 Dropdown。更新测试逻辑：
- 不再直接找 `.anticon-delete` 按钮
- 改为找到 `MoreOutlined` 触发按钮并点击展开 Dropdown
- 在 Dropdown 中找到"删除"菜单项点击
- 后续确认弹窗逻辑不变

关键变更（第 81-85 行附近）：
```tsx
// 找到 MoreOutlined 按钮（Dropdown 触发器）并点击
const moreBtns = screen.getAllByRole('button');
const dropdownTrigger = moreBtns.find((b) => b.querySelector('.anticon-more'));
expect(dropdownTrigger).toBeDefined();
await user.click(dropdownTrigger!);

// 在 Dropdown 中选择"删除"
const deleteItem = await screen.findByText('删除');
await user.click(deleteItem);
```

- [x] **Step 3: 更新 ChannelCard.suspend.test.tsx**

"暂停"从 Dropdown 变为 Primary 按钮。更新测试逻辑：
- 不再通过 Dropdown 找"暂停"
- 直接找 Primary 按钮（含有"暂停"文本的 `type="primary"` 按钮）

关键变更：
```tsx
// Primary 按钮直接可见，不需要展开 Dropdown
const suspendBtn = screen.getByRole('button', { name: /暂停/ });
await user.click(suspendBtn);

// 应弹出二次确认
await waitFor(() => {
  expect(screen.getByText(/暂停后该渠道不再分配流量/)).toBeInTheDocument();
});
```

- [x] **Step 4: 更新 ChannelCard.testIcon.test.tsx**

测试按钮可用性变化：ACTIVE 状态应可点击，RETIRED 状态应禁用。
- 现有 ACTIVE 测试（按钮可点击）应保持不变
- 可添加一个 RETIRED 状态的额外测试

- [x] **Step 5: 更新 ChannelDetailDrawer.healthMatrix.test.tsx**

Drawer extra 区域 layout 变化：删除按钮不再独立存在。
- 确认测试按钮（"连通性测试"）仍可找到
- Dropdown 触发器仍可找到
- 如果测试中涉及删除按钮的查找，需要调整

- [x] **Step 6: 运行全部测试**

```bash
cd gateway-console && npx vitest run src/domain/channel/__tests__/lifecycle.test.ts src/pages/Channels/__tests__/ChannelCard.test.tsx src/pages/Channels/__tests__/ChannelCard.delete.test.tsx src/pages/Channels/__tests__/ChannelCard.suspend.test.tsx src/pages/Channels/__tests__/ChannelCard.testIcon.test.tsx src/pages/Channels/__tests__/ChannelDetailDrawer.healthMatrix.test.tsx src/utils/__tests__/channelActions.test.ts
```
Expected: ALL PASS

- [x] **Step 7: Commit**

```bash
git add gateway-console/src/utils/__tests__/channelActions.test.ts gateway-console/src/pages/Channels/__tests__/ChannelCard.delete.test.tsx gateway-console/src/pages/Channels/__tests__/ChannelCard.suspend.test.tsx gateway-console/src/pages/Channels/__tests__/ChannelCard.testIcon.test.tsx gateway-console/src/pages/Channels/__tests__/ChannelDetailDrawer.healthMatrix.test.tsx
git commit -m "test(channel): 更新测试适配操作区重构，新增 channelActions 测试"
```
