# 渠道卡片行内操作 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将渠道卡片操作从单一 ⋮ 下拉菜单重构为右上角图标组（启停+测试常驻，低频操作收入 ⋮ Dropdown），实现行内连通性测试和确认弹窗启停。

**Architecture:** 重构 ChannelCard 组件，增加启停和测试两个常驻图标按钮，将现有 ⋮ 菜单改为只含低频操作。连通性测试复用现有 `useTestChannelCredential` hook，测试渠道的第一个凭证。启停操作增加确认弹窗。渠道详情抽屉增加 `initialTab` prop 支持从卡片菜单直达特定 Tab。

**Tech Stack:** React + Ant Design 5 + react-i18next + @tanstack/react-query

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `gateway-console/src/pages/Channels/ChannelCard.tsx` | 修改 | 重构为右上角图标组布局，增加启停/测试常驻按钮 |
| `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx` | 修改 | 增加 `initialTab` prop，支持从卡片直达特定 Tab |
| `gateway-console/src/pages/Channels/index.tsx` | 修改 | 传递新 props，管理测试/启停/Tab 跳转逻辑 |
| `gateway-console/src/pages/Channels/ChannelGroupedList.tsx` | 修改 | 透传新 props 到 ChannelCard |
| `gateway-console/src/locales/zh-CN/channels.json` | 修改 | 新增翻译 key |
| `gateway-console/src/locales/en-US/channels.json` | 修改 | 新增翻译 key |

---

### Task 1: 新增 i18n 翻译 key

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/channels.json`
- Modify: `gateway-console/src/locales/en-US/channels.json`

- [ ] **Step 1: 在 zh-CN/channels.json 中新增 key**

在 `card.delete` 行之后新增以下 key：

```json
"card.testConnect": "连通性测试",
"card.testDisabled": "渠道已停用，无法测试",
"card.replaceKey": "替换 Key",
"card.addEndpoint": "添加端点",
"card.addModel": "添加模型",
"card.copyMainUrl": "复制主端点 URL",
"card.noEndpoint": "该渠道暂无端点",
"card.testSuccess": "连通性测试通过，响应时间 {{latency}}ms",
"card.testFail": "连通性测试失败：{{msg}}",
"card.testNoCredential": "暂无凭证，请先添加 API Key",
"card.urlCopied": "端点 URL 已复制到剪贴板"
```

- [ ] **Step 2: 在 en-US/channels.json 中新增对应 key**

在 `card.delete` 行之后新增以下 key：

```json
"card.testConnect": "Test Connectivity",
"card.testDisabled": "Channel is inactive, cannot test",
"card.replaceKey": "Replace Key",
"card.addEndpoint": "Add Endpoint",
"card.addModel": "Add Model",
"card.copyMainUrl": "Copy Main Endpoint URL",
"card.noEndpoint": "No endpoints for this channel",
"card.testSuccess": "Connectivity test passed, latency {{latency}}ms",
"card.testFail": "Connectivity test failed: {{msg}}",
"card.testNoCredential": "No credentials, please add API Key first",
"card.urlCopied": "Endpoint URL copied to clipboard"
```

- [ ] **Step 3: Commit**

```bash
git add gateway-console/src/locales/zh-CN/channels.json gateway-console/src/locales/en-US/channels.json
git commit -m "feat(console): 新增渠道卡片行内操作翻译 key"
```

---

### Task 2: ChannelDetailDrawer 增加 initialTab prop

**Files:**
- Modify: `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx`

- [ ] **Step 1: 修改 ChannelDetailDrawerProps 接口，增加 initialTab**

将 `ChannelDetailDrawerProps` 改为：

```typescript
interface ChannelDetailDrawerProps {
  channel: ChannelCard | null;
  open: boolean;
  onClose: () => void;
  initialTab?: string;
}
```

- [ ] **Step 2: 修改组件签名和 activeTab 初始化逻辑**

将组件签名改为：

```typescript
export function ChannelDetailDrawer({
  channel,
  open,
  onClose,
  initialTab,
}: ChannelDetailDrawerProps) {
```

将 `activeTab` 的初始化改为受 `initialTab` 和 `open` 驱动：

```typescript
const [activeTab, setActiveTab] = useState('overview');
```

在 `channel` 变化时（打开新渠道详情），同步 `activeTab` 到 `initialTab`：

```typescript
// 当抽屉打开且 initialTab 变化时，切换到目标 Tab
useState(() => {
  if (open && initialTab) {
    setActiveTab(initialTab);
  }
});
```

实际上应该用 `useEffect` 监听 `open` 和 `initialTab`：

```typescript
import { useState, useEffect } from 'react';

// 在组件内，activeTab 声明后添加：
useEffect(() => {
  if (open && initialTab) {
    setActiveTab(initialTab);
  }
}, [open, initialTab]);
```

- [ ] **Step 3: Commit**

```bash
git add gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx
git commit -m "feat(console): ChannelDetailDrawer 支持 initialTab prop，可从卡片直达特定 Tab"
```

---

### Task 3: 重构 ChannelCard 组件

**Files:**
- Modify: `gateway-console/src/pages/Channels/ChannelCard.tsx`

这是核心任务。重构卡片布局为右上角图标组，增加启停和测试常驻按钮。

- [ ] **Step 1: 重写 ChannelCard 组件**

完整替换 `ChannelCard.tsx` 的内容：

```tsx
import { useState } from 'react';
import { Card, Dropdown, App, Tooltip, message } from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  PauseOutlined,
  PlayCircleOutlined,
  ThunderboltOutlined,
  LoadingOutlined,
  MoreOutlined,
  EyeOutlined,
  SwapOutlined,
  PlusCircleOutlined,
  FileAddOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { theme } from 'antd';
import type { ChannelCard as ChannelCardType } from '@/types/channel';

interface ChannelCardProps {
  channel: ChannelCardType;
  onClick: (channel: ChannelCardType) => void;
  onDelete: (id: number) => void;
  onToggleState: (id: number, enabled: boolean) => void;
  onTest: (channel: ChannelCardType) => void;
  onOpenDrawerTab: (channel: ChannelCardType, tab: string) => void;
}

/**
 * 渠道卡片组件
 * 右上角图标组：启停 + 测试 常驻，低频操作收入 ⋮ Dropdown
 */
export function ChannelCard({ channel, onClick, onDelete, onToggleState, onTest, onOpenDrawerTab }: ChannelCardProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const { modal } = App.useApp();
  const [testing, setTesting] = useState(false);
  const isActive = channel.state === 'ACTIVE';

  /** 复制主端点 URL */
  const handleCopyUrl = () => {
    const url = getMainEndpointUrl();
    if (!url) return;
    navigator.clipboard.writeText(url).then(() => {
      message.success(t('card.urlCopied'));
    });
  };

  /** 获取主端点 URL */
  const getMainEndpointUrl = (): string | null => {
    if (!channel.endpoints || channel.endpoints.length === 0) return null;
    return channel.endpoints[0].endpointUrl;
  };

  /** 启停按钮点击 */
  const handleToggleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    modal.confirm({
      title: isActive ? t('card.confirmDisable') : t('card.confirmEnable'),
      onOk: () => onToggleState(channel.id, !isActive),
    });
  };

  /** 测试按钮点击 */
  const handleTestClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onTest(channel);
  };

  /** 低频菜单项 */
  const menuItems = [
    { key: 'detail', label: t('card.viewDetail'), icon: <EyeOutlined /> },
    { key: 'edit', label: t('card.edit'), icon: <EditOutlined /> },
    { type: 'divider' as const },
    { key: 'credential', label: t('card.replaceKey'), icon: <SwapOutlined /> },
    { key: 'endpoint', label: t('card.addEndpoint'), icon: <PlusCircleOutlined /> },
    { key: 'model', label: t('card.addModel'), icon: <FileAddOutlined /> },
    { type: 'divider' as const },
    {
      key: 'copyUrl',
      label: t('card.copyMainUrl'),
      icon: <CopyOutlined />,
      disabled: !getMainEndpointUrl(),
    },
    { type: 'divider' as const },
    { key: 'delete', label: t('card.delete'), icon: <DeleteOutlined />, danger: true },
  ];

  const handleMenuClick = (e: { key: string }) => {
    switch (e.key) {
      case 'detail':
        onClick(channel);
        break;
      case 'edit':
        onOpenDrawerTab(channel, 'quota');
        break;
      case 'credential':
        onOpenDrawerTab(channel, 'credentials');
        break;
      case 'endpoint':
        onOpenDrawerTab(channel, 'endpoints');
        break;
      case 'model':
        onOpenDrawerTab(channel, 'models');
        break;
      case 'copyUrl':
        handleCopyUrl();
        break;
      case 'delete':
        modal.confirm({
          title: t('card.deleteConfirmTitle'),
          content: t('card.deleteConfirmContent', { name: channel.name }),
          okType: 'danger',
          onOk: () => onDelete(channel.id),
        });
        break;
    }
  };

  return (
    <Card
      hoverable
      onClick={() => onClick(channel)}
      style={{
        opacity: isActive ? 1 : 0.6,
        borderLeft: isActive
          ? `3px solid ${token.colorSuccess}`
          : `3px solid ${token.colorTextQuaternary}`,
      }}
      styles={{ body: { padding: '16px' } }}
    >
      {/* 第一行：渠道名称 + 操作按钮 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{
          fontWeight: 600,
          fontSize: token.fontSizeLG,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          flex: 1,
          minWidth: 0,
          marginRight: token.marginSM,
        }}>
          {channel.name}
        </span>

        <div style={{ display: 'flex', alignItems: 'center', gap: 2, flexShrink: 0 }}>
          {/* 启停按钮 */}
          <Tooltip title={isActive ? t('card.disable') : t('card.enable')}>
            <span
              role="button"
              onClick={handleToggleClick}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 28,
                height: 28,
                borderRadius: token.borderRadiusSM,
                cursor: 'pointer',
                color: isActive ? token.colorTextSecondary : token.colorPrimary,
                transition: 'color 0.2s, background 0.2s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = token.colorBgTextHover;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent';
              }}
            >
              {isActive ? <PauseOutlined /> : <PlayCircleOutlined />}
            </span>
          </Tooltip>

          {/* 测试按钮 */}
          <Tooltip title={isActive ? t('card.testConnect') : t('card.testDisabled')}>
            <span
              role="button"
              onClick={isActive ? handleTestClick : undefined}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 28,
                height: 28,
                borderRadius: token.borderRadiusSM,
                cursor: isActive ? 'pointer' : 'not-allowed',
                color: testing ? token.colorPrimary : token.colorTextSecondary,
                opacity: isActive ? 1 : 0.4,
                transition: 'color 0.2s, background 0.2s',
              }}
              onMouseEnter={(e) => {
                if (isActive) e.currentTarget.style.background = token.colorBgTextHover;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent';
              }}
            >
              {testing ? <LoadingOutlined spin /> : <ThunderboltOutlined />}
            </span>
          </Tooltip>

          {/* ⋮ 更多菜单 */}
          <Dropdown menu={{ items: menuItems, onClick: handleMenuClick }} trigger={['click']}>
            <Tooltip title={t('card.more', '更多')}>
              <span
                role="button"
                onClick={(e) => e.stopPropagation()}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 28,
                  height: 28,
                  borderRadius: token.borderRadiusSM,
                  cursor: 'pointer',
                  color: token.colorTextSecondary,
                  transition: 'color 0.2s, background 0.2s',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = token.colorBgTextHover;
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'transparent';
                }}
              >
                <MoreOutlined />
              </span>
            </Tooltip>
          </Dropdown>
        </div>
      </div>

      {/* 第二行：状态圆点 + 状态文字 + 统计信息 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: token.marginXS, color: token.colorTextSecondary, fontSize: token.fontSizeSM, marginTop: token.marginXS }}>
        <span style={{
          display: 'inline-block',
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: isActive ? token.colorSuccess : token.colorTextQuaternary,
          flexShrink: 0,
        }} />
        <span>{isActive ? t('status.active') : t('status.inactive')}</span>
        <span style={{ margin: '0 2px' }}>·</span>
        <span>{channel.stats?.endpointCount ?? 0} {t('card.endpoints')}</span>
        <span>{channel.stats?.credentialCount ?? 0} Key</span>
        <span>{channel.stats?.modelCount ?? 0} {t('card.models')}</span>
      </div>
    </Card>
  );
}
```

- [ ] **Step 2: 验证 TypeScript 编译通过**

Run: `cd gateway-console && npx tsc --noEmit --pretty 2>&1 | head -30`
Expected: 无 ChannelCard 相关错误（可能有其他组件因 props 变化报错，Task 4 修复）

- [ ] **Step 3: Commit**

```bash
git add gateway-console/src/pages/Channels/ChannelCard.tsx
git commit -m "feat(console): 重构渠道卡片为右上角图标组布局，增加启停/测试常驻按钮"
```

---

### Task 4: 更新 Channels/index.tsx 传递新 props 和处理逻辑

**Files:**
- Modify: `gateway-console/src/pages/Channels/index.tsx`

- [ ] **Step 1: 增加状态和 hook**

在 `Channels` 组件内，现有状态声明之后新增：

```typescript
import { useTestChannelCredential, useChannelCredentials } from '@/services/query/useChannels';

// 在组件内部：
const testCredential = useTestChannelCredential();
const [drawerInitialTab, setDrawerInitialTab] = useState<string | undefined>(undefined);
```

- [ ] **Step 2: 新增 handleTestChannel 函数**

在 `handleDelete` 函数之后新增：

```typescript
/** 从卡片发起连通性测试 */
const handleTestChannel = async (channel: ChannelCard) => {
  if (channel.state !== 'ACTIVE') return;
  // 获取凭证列表，测试第一个
  try {
    const creds = await new Promise<ChannelCredential[]>((resolve) => {
      // 使用已有的 credentialsData 查找
      const idx = channels?.findIndex(c => c.id === channel.id) ?? -1;
      resolve(credentialsData[idx] || []);
    });

    if (!creds || creds.length === 0) {
      message.warning(t('card.testNoCredential'));
      return;
    }

    const result = await testCredential.mutateAsync({ channelId: channel.id, id: creds[0].id });
    if (result.success) {
      message.success(t('card.testSuccess', { latency: result.latency ?? 0 }));
    } else {
      message.error(t('card.testFail', { msg: result.error?.message || t('credential.unknownError') }));
    }
  } catch (err) {
    message.error(t('card.testFail', { msg: err instanceof Error ? err.message : t('credential.unknownError') }));
  }
};
```

实际上，由于凭证数据已经通过 `useChannelCredentialsBatch` 批量获取，可以直接从 `credentialsData` 中查找，无需重新获取。但 `credentialsData` 的索引与 `channels` 数组索引对应，需要用 channel.id 查找索引。

更简洁的实现：

```typescript
/** 从卡片发起连通性测试 */
const handleTestChannel = async (channel: ChannelCard) => {
  if (channel.state !== 'ACTIVE') return;
  const idx = channels?.findIndex(c => c.id === channel.id) ?? -1;
  const creds = credentialsData[idx];

  if (!creds || creds.length === 0) {
    message.warning(t('card.testNoCredential'));
    return;
  }

  try {
    const result = await testCredential.mutateAsync({ channelId: channel.id, id: creds[0].id });
    if (result.success) {
      message.success(t('card.testSuccess', { latency: result.latency ?? 0 }));
    } else {
      message.error(t('card.testFail', { msg: result.error?.message || t('credential.unknownError') }));
    }
  } catch (err) {
    message.error(t('card.testFail', { msg: err instanceof Error ? err.message : t('credential.unknownError') }));
  }
};
```

- [ ] **Step 3: 新增 handleOpenDrawerTab 函数**

```typescript
/** 从卡片菜单打开详情抽屉并跳转到指定 Tab */
const handleOpenDrawerTab = (channel: ChannelCard, tab: string) => {
  setSelectedChannel(channel);
  setDrawerInitialTab(tab);
  setDrawerVisible(true);
};
```

- [ ] **Step 4: 修改 ChannelDetailDrawer 调用，传递 initialTab**

将现有的 `<ChannelDetailDrawer>` 调用改为：

```tsx
<ChannelDetailDrawer
  channel={selectedChannel}
  open={drawerVisible}
  onClose={() => {
    setDrawerVisible(false);
    setDrawerInitialTab(undefined);
  }}
  initialTab={drawerInitialTab}
/>
```

- [ ] **Step 5: 修改 ChannelGroupedList 调用，传递新 props**

将 `<ChannelGroupedList>` 调用改为：

```tsx
<ChannelGroupedList
  groups={filteredGroups}
  onChannelClick={handleChannelClick}
  onChannelDelete={handleDelete}
  onChannelToggleState={(id, enabled) => {
    updateChannel.mutate({ id, data: { state: enabled ? 'ACTIVE' : 'INACTIVE' } });
  }}
  onTestChannel={handleTestChannel}
  onOpenDrawerTab={handleOpenDrawerTab}
  onEditProvider={(id) => {
    const p = providersData?.items?.find((p) => p.id === id);
    if (p) {
      setEditingProvider(p);
      setEditProviderModalOpen(true);
    }
  }}
  onToggleProviderEnabled={(id) => {
    const p = providersData?.items?.find((p) => p.id === id);
    if (p) {
      setEnabledProvider.mutate({ id, enabled: p.state !== 'ACTIVE' });
    }
  }}
  onTestProviderConnectivity={(providerCode) => {
    setConnectivityProviderId(providerCode);
  }}
  onExportProvider={() => {
    message.info(t('batch.exportHint'));
  }}
/>
```

- [ ] **Step 6: 验证 TypeScript 编译通过**

Run: `cd gateway-console && npx tsc --noEmit --pretty 2>&1 | head -30`
Expected: 无错误（ChannelGroupedList props 不匹配会在 Task 5 修复）

- [ ] **Step 7: Commit**

```bash
git add gateway-console/src/pages/Channels/index.tsx
git commit -m "feat(console): 页面级增加连通性测试和 Tab 跳转逻辑"
```

---

### Task 5: 更新 ChannelGroupedList 透传新 props

**Files:**
- Modify: `gateway-console/src/pages/Channels/ChannelGroupedList.tsx`

- [ ] **Step 1: 更新 ChannelGroupedListProps 接口**

在接口中新增两个 props：

```typescript
export interface ChannelGroupedListProps {
  groups: ChannelGroup[];
  onChannelClick: (channel: ChannelCardType) => void;
  onChannelDelete: (id: number) => void;
  onChannelToggleState: (id: number, enabled: boolean) => void;
  onTestChannel: (channel: ChannelCardType) => void;
  onOpenDrawerTab: (channel: ChannelCardType, tab: string) => void;
  onEditProvider?: (providerId: number) => void;
  onToggleProviderEnabled?: (providerId: number) => void;
  onTestProviderConnectivity?: (providerId: number) => void;
  onExportProvider?: (providerId: number) => void;
}
```

- [ ] **Step 2: 更新组件签名和 ChannelCard 调用**

组件签名增加解构：

```typescript
export const ChannelGroupedList: FC<ChannelGroupedListProps> = ({
  groups,
  onChannelClick,
  onChannelDelete,
  onChannelToggleState,
  onTestChannel,
  onOpenDrawerTab,
  onEditProvider,
  onToggleProviderEnabled,
  onTestProviderConnectivity,
  onExportProvider,
}) => {
```

ChannelCard 调用增加新 props：

```tsx
<ChannelCard
  key={channel.id}
  channel={channel}
  onClick={onChannelClick}
  onDelete={onChannelDelete}
  onToggleState={onChannelToggleState}
  onTest={onTestChannel}
  onOpenDrawerTab={onOpenDrawerTab}
/>
```

- [ ] **Step 3: 验证 TypeScript 编译通过**

Run: `cd gateway-console && npx tsc --noEmit --pretty 2>&1 | head -30`
Expected: 无错误

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/pages/Channels/ChannelGroupedList.tsx
git commit -m "feat(console): ChannelGroupedList 透传测试和 Tab 跳转 props"
```

---

### Task 6: 补充 i18n 中遗漏的 "更多" key 和构建验证

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/channels.json`
- Modify: `gateway-console/src/locales/en-US/channels.json`

- [ ] **Step 1: 在 zh-CN 补充 "更多" key**

在 `card.urlCopied` 后新增：

```json
"card.more": "更多操作"
```

- [ ] **Step 2: 在 en-US 补充 "更多" key**

在 `card.urlCopied` 后新增：

```json
"card.more": "More actions"
```

- [ ] **Step 3: 构建验证**

Run: `cd gateway-console && npm run build 2>&1 | tail -20`
Expected: 构建成功，无错误

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/locales/zh-CN/channels.json gateway-console/src/locales/en-US/channels.json
git commit -m "feat(console): 补充卡片「更多操作」翻译 key"
```

---

## Spec 覆盖自检

| Spec 要求 | 对应 Task |
|-----------|----------|
| 右上角图标组布局 | Task 3 |
| 状态标签移至第二行（圆点+文字） | Task 3 |
| 启停按钮：PauseOutlined/PlayCircleOutlined + 确认弹窗 | Task 3 |
| 测试按钮：ThunderboltOutlined + LoadingOutlined 旋转 | Task 3 |
| 测试按钮：INACTIVE 时 disabled | Task 3 |
| 测试结果用 message 顶部提示 | Task 4 |
| ⋮ Dropdown 低频操作 | Task 3 |
| 动词命名：替换 Key/添加端点/添加模型 | Task 3 |
| 复制主端点 URL + 多端点规则 | Task 3 |
| 删除 danger 样式 | Task 3 |
| 卡片点击打开详情抽屉 | Task 3（保留现有 onClick） |
| 操作按钮 stopPropagation | Task 3 |
| Dropdown 菜单直达详情抽屉特定 Tab | Task 2, Task 3, Task 4 |
| 响应式降级（窄屏高频收入菜单） | 未实现（V2，当前先不做） |

**Placeholder 扫描**：无 TBD/TODO。

**Type 一致性**：`ChannelCardProps` 的 `onTest` 和 `onOpenDrawerTab` 签名在 Task 3 定义，Task 4/5 使用时一致。`ChannelDetailDrawerProps` 的 `initialTab` 类型为 `string | undefined`，Task 2 定义，Task 4 传递时一致。
