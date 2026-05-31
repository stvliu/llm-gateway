# 供应商前端交互优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构供应商相关页面，将渠道提升为核心实体，供应商降级为分组维度

**Architecture:** 新增渠道管理页面(`/channels`)，按供应商分组展示渠道卡片，点击卡片打开详情抽屉展示四宫格。供应商目录页(`/providers`)简化为Catalog浏览+物化入口。移除独立API Key和模型页面。

**Tech Stack:** React 18 + TypeScript + Ant Design 5 + TanStack Query + Zustand + i18next

---

### Task 1: 导航结构重构

**Files:**
- Modify: `gateway-console/src/constants/menuConfig.tsx`
- Modify: `gateway-console/src/router/index.tsx`
- Modify: `gateway-console/src/constants/permissions.ts` (如存在)

- [ ] **Step 1: 更新菜单配置**

修改 `gateway-console/src/constants/menuConfig.tsx`，将菜单项替换为：

```tsx
export const menuConfig: MenuItemConfig[] = [
  { key: '/dashboard', icon: DashboardOutlined, label: 'menu.home' },
  { key: '/channels', icon: ThunderboltOutlined, label: 'menu.channels', permission: 'channel:read' },
  { key: '/providers', icon: AppstoreOutlined, label: 'menu.providerDirectory', permission: 'provider:read' },
  { key: '/teams', icon: TeamOutlined, label: 'menu.teams', permission: 'user:read' },
  { key: '/developer', icon: CodeOutlined, label: 'menu.developer', permission: 'developer:access' },
];
```

移除 `/models` 和 `/keys` 菜单项。将供应商的 `CloudServerOutlined` 改为 `AppstoreOutlined`，标签从 `menu.providers` 改为 `menu.providerDirectory`。新增 `/channels` 使用 `ThunderboltOutlined`。

- [ ] **Step 2: 更新路由配置**

修改 `gateway-console/src/router/index.tsx`，新增渠道路由，调整供应商路由：

```tsx
// 新增
{ path: '/channels', element: <PermissionGuard permission={P.CHANNEL_READ}><Channels /></PermissionGuard> },

// 供应商路由保留，指向目录页（后续Task 5精简内容）
// /models 和 /keys 路由保留但不加入菜单，避免深层链接失效
```

- [ ] **Step 3: 新增权限常量**

在 `gateway-console/src/constants/permissions.ts`（或对应的权限文件）中新增：

```ts
CHANNEL_READ: 'channel:read',
CHANNEL_WRITE: 'channel:write',
```

- [ ] **Step 4: 新增 i18n 键**

在 `gateway-console/src/locales/zh-CN/common.json` 和 `gateway-console/src/locales/en-US/common.json` 中添加：

```json
{
  "menu.channels": "渠道管理",
  "menu.providerDirectory": "供应商目录"
}
```

- [ ] **Step 5: Commit**

```bash
git add gateway-console/src/constants/menuConfig.tsx gateway-console/src/router/index.tsx gateway-console/src/constants/permissions.ts gateway-console/src/locales/zh-CN/common.json gateway-console/src/locales/en-US/common.json
git commit -m "feat: 重构侧边栏导航——渠道管理取代供应商为一级菜单"
```

---

### Task 2: 渠道管理页面骨架与渠道卡片

**Files:**
- Create: `gateway-console/src/pages/Channels/index.tsx`
- Create: `gateway-console/src/pages/Channels/ChannelCard.tsx`
- Create: `gateway-console/src/pages/Channels/ChannelGroupedList.tsx`
- Create: `gateway-console/src/pages/Channels/ProviderGroupHeader.tsx`
- Modify: `gateway-console/src/services/query/useChannels.ts` (增加不带 providerId 的查询)
- Modify: `gateway-console/src/services/api/channel.ts` (增加 list 不带 providerId 的支持)

- [ ] **Step 1: 扩展 channelApi.list 支持全量查询**

修改 `gateway-console/src/services/api/channel.ts`，让 `list` 方法支持不传 `providerId`，返回所有渠道：

```ts
list: (params?: { providerId?: number; page?: number; size?: number; state?: string }) =>
  api.get<PageResponse<Channel>>('/channels', { params }),
```

- [ ] **Step 2: 新增 useAllChannels hook**

修改 `gateway-console/src/services/query/useChannels.ts`，新增：

```ts
export function useAllChannels(params?: { state?: string }) {
  return useQuery({
    queryKey: ['channels', 'all', params],
    queryFn: () => channelApi.list(params),
  });
}
```

- [ ] **Step 3: 创建 ProviderGroupHeader 组件**

创建 `gateway-console/src/pages/Channels/ProviderGroupHeader.tsx`：

```tsx
import { Provider } from '@/types/provider';
import { ProviderIcon } from '@/components/ui/ProviderIcon';
import { Typography } from 'antd';

interface ProviderGroupHeaderProps {
  provider: Pick<Provider, 'id' | 'providerId' | 'providerName' | 'logoUrl'>;
  channelCount: number;
  endpointCount: number;
  credentialCount: number;
  modelCount: number;
  collapsed: boolean;
  onToggle: () => void;
}

export function ProviderGroupHeader({ provider, channelCount, endpointCount, credentialCount, modelCount, collapsed, onToggle }: ProviderGroupHeaderProps) {
  return (
    <div
      style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 0', cursor: 'pointer' }}
      onClick={onToggle}
    >
      <ProviderIcon providerId={provider.providerId} logoUrl={provider.logoUrl} size={20} />
      <Typography.Text strong style={{ fontSize: 13 }}>{provider.providerName}</Typography.Text>
      <Typography.Text type="secondary" style={{ fontSize: 11 }}>
        {channelCount} 渠道 · {endpointCount} 端点 · {credentialCount} Key · {modelCount} 模型
      </Typography.Text>
      <Typography.Text type="secondary" style={{ fontSize: 11, marginLeft: 'auto' }}>
        {collapsed ? '▶' : '▼'}
      </Typography.Text>
    </div>
  );
}
```

- [ ] **Step 4: 创建 ChannelCard 组件**

创建 `gateway-console/src/pages/Channels/ChannelCard.tsx`：

```tsx
import { Channel } from '@/types/channel';
import { Tag, Typography } from 'antd';

interface ChannelCardProps {
  channel: Channel & {
    endpointCount?: number;
    credentialCount?: number;
    modelCount?: number;
    responseTime?: number | null;
  };
  onClick: () => void;
}

const RESPONSE_TIME_COLORS: Record<string, string> = {
  green: '#52c41a',
  yellow: '#faad14',
  red: '#cf1322',
  gray: '#8c8c8c',
};

function getResponseTimeColor(ms: number | null | undefined): string {
  if (ms == null) return 'gray';
  if (ms <= 500) return 'green';
  if (ms <= 2000) return 'yellow';
  return 'red';
}

const STATUS_CONFIG = {
  ACTIVE: { color: 'success', label: '可用' },
  INACTIVE: { color: 'error', label: '停用' },
  CONFIGURING: { color: 'warning', label: '配置中' },
} as const;

export function ChannelCard({ channel, onClick }: ChannelCardProps) {
  const statusKey = channel.credentialCount === 0 ? 'CONFIGURING' : channel.state;
  const status = STATUS_CONFIG[statusKey] || STATUS_CONFIG.ACTIVE;
  const rtColor = getResponseTimeColor(channel.responseTime);

  return (
    <div
      onClick={onClick}
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '14px 18px',
        background: statusKey === 'CONFIGURING' ? '#fffbe6' : '#fff',
        border: `1px solid ${statusKey === 'CONFIGURING' ? '#ffe58f' : '#d9d9d9'}`,
        borderRadius: 8,
        cursor: 'pointer',
        transition: 'all 0.2s',
        opacity: channel.state === 'INACTIVE' ? 0.6 : 1,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 180 }}>
          <span style={{ color: status.color === 'success' ? '#52c41a' : status.color === 'error' ? '#cf1322' : '#fa8c16', fontSize: 8 }}>●</span>
          <Typography.Text strong style={{ fontSize: 14 }}>{channel.name}</Typography.Text>
        </div>
        <Tag color={status.color}>{status.label}</Tag>
        <div style={{ display: 'flex', gap: 16, fontSize: 12, color: '#595959' }}>
          <span>🌐 {channel.endpointCount ?? 0}</span>
          <span style={{ color: channel.credentialCount === 0 ? '#fa8c16' : undefined }}>🔑 {channel.credentialCount ?? 0}</span>
          <span>🤖 {channel.modelCount ?? 0}</span>
        </div>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {channel.billingMode} · P{channel.priority} W{channel.weight}
        </Typography.Text>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Typography.Text style={{ fontSize: 12, color: RESPONSE_TIME_COLORS[rtColor], fontWeight: 500 }}>
          ⚡ {channel.responseTime != null ? `${channel.responseTime}ms` : '未测试'}
        </Typography.Text>
        <Typography.Link style={{ fontSize: 12, fontWeight: 500 }}>详情 →</Typography.Link>
      </div>
    </div>
  );
}
```

- [ ] **Step 5: 创建 ChannelGroupedList 组件**

创建 `gateway-console/src/pages/Channels/ChannelGroupedList.tsx`，按供应商分组渲染渠道卡片：

```tsx
import { useState } from 'react';
import { Channel } from '@/types/channel';
import { Provider } from '@/types/provider';
import { ProviderGroupHeader } from './ProviderGroupHeader';
import { ChannelCard } from './ChannelCard';

interface GroupedChannels {
  provider: Pick<Provider, 'id' | 'providerId' | 'providerName' | 'logoUrl'>;
  channels: Channel[];
  stats: { endpointCount: number; credentialCount: number; modelCount: number };
}

interface ChannelGroupedListProps {
  groups: GroupedChannels[];
  onChannelClick: (channel: Channel) => void;
}

export function ChannelGroupedList({ groups, onChannelClick }: ChannelGroupedListProps) {
  const [collapsedGroups, setCollapsedGroups] = useState<Set<number>>(new Set());

  const toggleGroup = (providerId: number) => {
    setCollapsedGroups(prev => {
      const next = new Set(prev);
      if (next.has(providerId)) next.delete(providerId);
      else next.add(providerId);
      return next;
    });
  };

  return (
    <div>
      {groups.map(group => (
        <div key={group.provider.id} style={{ marginBottom: 20 }}>
          <ProviderGroupHeader
            provider={group.provider}
            channelCount={group.channels.length}
            endpointCount={group.stats.endpointCount}
            credentialCount={group.stats.credentialCount}
            modelCount={group.stats.modelCount}
            collapsed={collapsedGroups.has(group.provider.id)}
            onToggle={() => toggleGroup(group.provider.id)}
          />
          {!collapsedGroups.has(group.provider.id) && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {group.channels.map(channel => (
                <ChannelCard key={channel.id} channel={channel} onClick={() => onChannelClick(channel)} />
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 6: 创建渠道管理页面**

创建 `gateway-console/src/pages/Channels/index.tsx`：

```tsx
import { useState, useMemo } from 'react';
import { Button, Input, Select, Space, App } from 'antd';
import { useAllChannels } from '@/services/query/useChannels';
import { useProviders } from '@/services/query/useProviders';
import { ChannelGroupedList } from './ChannelGroupedList';
import { ChannelDetailDrawer } from './ChannelDetailDrawer';
import { ChannelCreateWizard } from './ChannelCreateWizard';
import { Channel } from '@/types/channel';

export default function Channels() {
  const { message } = App.useApp();
  const [search, setSearch] = useState('');
  const [providerFilter, setProviderFilter] = useState<number | undefined>();
  const [stateFilter, setStateFilter] = useState<string | undefined>();
  const [selectedChannel, setSelectedChannel] = useState<Channel | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

  const { data: channelsData } = useAllChannels({ state: stateFilter });
  const { data: providersData } = useProviders();

  const channels = channelsData?.content ?? [];
  const providers = providersData?.content ?? [];

  // 按供应商分组 + 过滤
  const groups = useMemo(() => {
    const providerMap = new Map(providers.map(p => [p.id, p]));
    const filtered = channels.filter(ch => {
      if (providerFilter && ch.providerId !== providerFilter) return false;
      if (search && !ch.name.toLowerCase().includes(search.toLowerCase())) return false;
      return true;
    });
    const grouped = new Map<number, Channel[]>();
    filtered.forEach(ch => {
      const list = grouped.get(ch.providerId) ?? [];
      list.push(ch);
      grouped.set(ch.providerId, list);
    });
    return Array.from(grouped.entries()).map(([providerId, chs]) => {
      const provider = providerMap.get(providerId);
      return {
        provider: provider ? { id: provider.id, providerId: provider.providerId ?? '', providerName: provider.providerName, logoUrl: provider.logoUrl } : { id: providerId, providerId: '', providerName: '未知供应商', logoUrl: undefined },
        channels: chs,
        stats: { endpointCount: 0, credentialCount: 0, modelCount: 0 }, // 后续Task 3中从channel详情聚合
      };
    });
  }, [channels, providers, providerFilter, search]);

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Space>
          <Input.Search placeholder="搜索渠道名称、模型..." style={{ width: 220 }} onSearch={setSearch} allowClear />
          <Select placeholder="全部供应商" style={{ width: 160 }} allowClear onChange={setProviderFilter}
            options={providers.map(p => ({ label: p.providerName, value: p.id }))} />
          <Select placeholder="全部状态" style={{ width: 120 }} allowClear onChange={setStateFilter}
            options={[{ label: '可用', value: 'ACTIVE' }, { label: '停用', value: 'INACTIVE' }]} />
        </Space>
        <Button type="primary" onClick={() => setCreateOpen(true)}>+ 新建渠道</Button>
      </Space>

      <ChannelGroupedList groups={groups} onChannelClick={setSelectedChannel} />

      <ChannelDetailDrawer
        channel={selectedChannel}
        onClose={() => setSelectedChannel(null)}
      />

      <ChannelCreateWizard
        open={createOpen}
        onClose={() => setCreateOpen(false)}
      />
    </div>
  );
}
```

- [ ] **Step 7: Commit**

```bash
git add gateway-console/src/pages/Channels/ gateway-console/src/services/query/useChannels.ts gateway-console/src/services/api/channel.ts
git commit -m "feat: 创建渠道管理页面骨架——分组卡片列表+详情入口"
```

---

### Task 3: 渠道详情抽屉 — 四宫格与行内编辑

**Files:**
- Create: `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx`
- Create: `gateway-console/src/pages/Channels/InlineEditableList.tsx`
- Create: `gateway-console/src/pages/Channels/EndpointSection.tsx`
- Create: `gateway-console/src/pages/Channels/CredentialSection.tsx`
- Create: `gateway-console/src/pages/Channels/ModelMappingSection.tsx`
- Create: `gateway-console/src/pages/Channels/QuotaSettingsSection.tsx`

- [ ] **Step 1: 创建 InlineEditableList 通用组件**

创建 `gateway-console/src/pages/Channels/InlineEditableList.tsx`，统一的行内编辑列表：

```tsx
import { useState, useEffect } from 'react';
import { Button, Popconfirm, Space } from 'antd';

export interface InlineEditableListProps<T> {
  items: T[];
  renderItem: (item: T) => React.ReactNode;
  renderEditForm: (item: T, onSave: (updated: T) => void, onCancel: () => void) => React.ReactNode;
  renderAddForm: (onSave: (newItem: T) => void, onCancel: () => void) => React.ReactNode;
  onAdd: () => void;
  onDelete?: (item: T) => void;
  getKey: (item: T) => string | number;
  addLabel?: string;
}

export function InlineEditableList<T>({
  items, renderItem, renderEditForm, renderAddForm, onAdd, onDelete, getKey, addLabel = '+ 添加',
}: InlineEditableListProps<T>) {
  const [editingKey, setEditingKey] = useState<string | number | null>(null);
  const [adding, setAdding] = useState(false);

  const handleAdd = () => {
    setAdding(true);
    setEditingKey(null);
  };

  const handleEdit = (key: string | number) => {
    setEditingKey(key);
    setAdding(false);
  };

  const handleSaveEdit = (updated: T) => {
    setEditingKey(null);
  };

  const handleCancelEdit = () => {
    setEditingKey(null);
  };

  const handleSaveAdd = (newItem: T) => {
    setAdding(false);
  };

  const handleCancelAdd = () => {
    setAdding(false);
  };

  return (
    <div>
      {items.map(item => {
        const key = getKey(item);
        if (editingKey === key) {
          return <div key={key}>{renderEditForm(item, handleSaveEdit, handleCancelEdit)}</div>;
        }
        return (
          <div key={key} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 8px', background: '#fff', borderRadius: 4, marginBottom: 4, border: '1px solid #f0f0f0' }}>
            <div>{renderItem(item)}</div>
            <Space size={4}>
              <Button type="link" size="small" onClick={() => handleEdit(key)}>编辑</Button>
              {onDelete && (
                <Popconfirm title="确定删除？" onConfirm={() => onDelete(item)}>
                  <Button type="link" size="small" danger>删除</Button>
                </Popconfirm>
              )}
            </Space>
          </div>
        );
      })}
      {adding && <div>{renderAddForm(handleSaveAdd, handleCancelAdd)}</div>}
      <Button type="link" size="small" onClick={handleAdd} style={{ padding: '4px 0' }}>
        {addLabel}
      </Button>
    </div>
  );
}
```

- [ ] **Step 2: 创建 EndpointSection 组件**

创建 `gateway-console/src/pages/Channels/EndpointSection.tsx`，使用 `InlineEditableList`：

```tsx
import { Channel, ChannelEndpointResponse } from '@/types/channel';
import { useChannelEndpoints, useAddChannelEndpoint, useRemoveChannelEndpoint } from '@/services/query/useChannels';
import { InlineEditableList } from './InlineEditableList';
import { Tag, Input, Select, Button, Space } from 'antd';
import { useState } from 'react';

interface EndpointSectionProps {
  channel: Channel;
}

const PROTOCOL_OPTIONS = [
  { value: 'openai', label: 'OpenAI', color: '#1890ff' },
  { value: 'anthropic', label: 'Anthropic', color: '#eb2f96' },
  { value: 'gemini', label: 'Gemini', color: '#fa8c16' },
  { value: 'native', label: 'Native', color: '#52c41a' },
];

export function EndpointSection({ channel }: EndpointSectionProps) {
  const { data: endpoints } = useChannelEndpoints(channel.id);
  const addEndpoint = useAddChannelEndpoint();
  const removeEndpoint = useRemoveChannelEndpoint();

  const [addFormState, setAddFormState] = useState({ protocol: 'openai', endpointUrl: '' });

  return (
    <div>
      <InlineEditableList
        items={endpoints ?? []}
        getKey={(ep) => ep.id}
        renderItem={(ep) => (
          <>
            <Tag color={PROTOCOL_OPTIONS.find(p => p.value === ep.protocol)?.color}>{ep.protocol.toUpperCase()}</Tag>
            <span>{ep.endpointUrl}</span>
          </>
        )}
        renderEditForm={(ep, onSave, onCancel) => (
          <div style={{ padding: 10, background: '#e6f7ff', borderRadius: 6, border: '1px solid #91d5ff' }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Space>
                <Select value={ep.protocol} options={PROTOCOL_OPTIONS} style={{ width: 120 }} />
                <Input value={ep.endpointUrl} style={{ flex: 1 }} />
              </Space>
              <Space>
                <Button size="small" type="primary">保存</Button>
                <Button size="small" onClick={onCancel}>取消</Button>
              </Space>
            </Space>
          </div>
        )}
        renderAddForm={(onSave, onCancel) => (
          <div style={{ padding: 10, background: '#e6f7ff', borderRadius: 6, border: '1px solid #91d5ff' }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Space>
                <Select value={addFormState.protocol} onChange={v => setAddFormState(s => ({ ...s, protocol: v }))} options={PROTOCOL_OPTIONS} style={{ width: 120 }} />
                <Input value={addFormState.endpointUrl} onChange={e => setAddFormState(s => ({ ...s, endpointUrl: e.target.value }))} placeholder="Endpoint URL" style={{ flex: 1 }} />
              </Space>
              <Space>
                <Button size="small" type="primary" onClick={() => { addEndpoint.mutate({ channelId: channel.id, data: addFormState }); onCancel(); }}>保存</Button>
                <Button size="small" onClick={onCancel}>取消</Button>
              </Space>
            </Space>
          </div>
        )}
        onAdd={() => setAddFormState({ protocol: 'openai', endpointUrl: '' })}
        onDelete={(ep) => removeEndpoint.mutate({ channelId: channel.id, endpointId: ep.id })}
        addLabel="+ 添加端点"
      />
    </div>
  );
}
```

- [ ] **Step 3: 创建 CredentialSection 组件**

创建 `gateway-console/src/pages/Channels/CredentialSection.tsx`，类似 EndpointSection 但增加测试按钮和最后使用时间展示。

- [ ] **Step 4: 创建 ModelMappingSection 组件**

创建 `gateway-console/src/pages/Channels/ModelMappingSection.tsx`，包含"从上游获取"按钮和"查看全部"链接。列表项展示模型名→上游名+定价。

- [ ] **Step 5: 创建 QuotaSettingsSection 组件**

创建 `gateway-console/src/pages/Channels/QuotaSettingsSection.tsx`，键值对展示，点击"编辑"整体切换为编辑模式。

- [ ] **Step 6: 创建 ChannelDetailDrawer 组件**

创建 `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx`，组装头部+四宫格：

```tsx
import { Drawer, Button, Space, Tag, Typography } from 'antd';
import { Channel } from '@/types/channel';
import { useProvider } from '@/services/query/useProviders';
import { ProviderIcon } from '@/components/ui/ProviderIcon';
import { EndpointSection } from './EndpointSection';
import { CredentialSection } from './CredentialSection';
import { ModelMappingSection } from './ModelMappingSection';
import { QuotaSettingsSection } from './QuotaSettingsSection';

interface ChannelDetailDrawerProps {
  channel: Channel | null;
  onClose: () => void;
}

export function ChannelDetailDrawer({ channel, onClose }: ChannelDetailDrawerProps) {
  const { data: provider } = useProvider(channel?.providerId ?? 0);

  if (!channel) return null;

  return (
    <Drawer open={!!channel} onClose={onClose} width={720} title={null} closable>
      {/* 头部：供应商内联 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16, paddingBottom: 12, borderBottom: '1px solid #f0f0f0' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
            <ProviderIcon providerId={provider?.providerId} logoUrl={provider?.logoUrl} size={28} />
            <Typography.Title level={4} style={{ margin: 0 }}>{channel.name}</Typography.Title>
            <Tag color={channel.state === 'ACTIVE' ? 'success' : 'error'}>{channel.state === 'ACTIVE' ? '● 活跃' : '● 停用'}</Tag>
          </div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            供应商: <Typography.Link>{provider?.providerName}</Typography.Link> · {channel.billingMode} · P{channel.priority} W{channel.weight}
          </Typography.Text>
          {provider && (
            <Typography.Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 2 }}>
              官网: {provider.websiteUrl} · 文档: {provider.apiDocUrl}
            </Typography.Text>
          )}
        </div>
        <Space>
          <Button>🔄 测试</Button>
          <Button>编辑</Button>
          <Button danger type="text">停用</Button>
        </Space>
      </div>

      {/* 四宫格 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <div style={{ background: '#fafafa', border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }}>
          <Typography.Text strong style={{ fontSize: 13 }}>🌐 端点</Typography.Text>
          <EndpointSection channel={channel} />
        </div>
        <div style={{ background: '#fafafa', border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }}>
          <Typography.Text strong style={{ fontSize: 13 }}>🔑 API Key</Typography.Text>
          <CredentialSection channel={channel} />
        </div>
        <div style={{ background: '#fafafa', border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }}>
          <Typography.Text strong style={{ fontSize: 13 }}>🤖 模型映射</Typography.Text>
          <ModelMappingSection channel={channel} />
        </div>
        <div style={{ background: '#fafafa', border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }}>
          <Typography.Text strong style={{ fontSize: 13 }}>⚙️ 配额与设置</Typography.Text>
          <QuotaSettingsSection channel={channel} />
        </div>
      </div>
    </Drawer>
  );
}
```

- [ ] **Step 7: Commit**

```bash
git add gateway-console/src/pages/Channels/
git commit -m "feat: 渠道详情抽屉——四宫格+行内编辑组件"
```

---

### Task 4: 双模式创建向导

**Files:**
- Create: `gateway-console/src/pages/Channels/ChannelCreateWizard.tsx`
- Create: `gateway-console/src/pages/Channels/QuickOnboardMode.tsx`
- Create: `gateway-console/src/pages/Channels/ExpertConfigMode.tsx`

- [ ] **Step 1: 创建 QuickOnboardMode 组件**

创建 `gateway-console/src/pages/Channels/QuickOnboardMode.tsx`，三步向导：选模板 → 粘贴Key → 确认模型。复用现有 `TemplateLibrary` 和 `CredentialStep` 组件。

- [ ] **Step 2: 创建 ExpertConfigMode 组件**

创建 `gateway-console/src/pages/Channels/ExpertConfigMode.tsx`，五步向导。复用现有 `BasicInfoStep`、`CredentialStep`、`ModelSetupStep` 组件。

- [ ] **Step 3: 创建 ChannelCreateWizard 容器**

创建 `gateway-console/src/pages/Channels/ChannelCreateWizard.tsx`，用 Segmented 切换快速/专家模式，URL step param 管理步骤状态。

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/pages/Channels/ChannelCreateWizard.tsx gateway-console/src/pages/Channels/QuickOnboardMode.tsx gateway-console/src/pages/Channels/ExpertConfigMode.tsx
git commit -m "feat: 双模式渠道创建向导——快速接入+专家配置"
```

---

### Task 5: 供应商详情页精简

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx`
- Modify: `gateway-console/src/pages/Providers/ProviderBasicInfoTab.tsx`

- [ ] **Step 1: 精简 ProviderManagementDrawer Tab 结构**

修改 `ProviderManagementDrawer.tsx`，将 6 Tab 精简为 3 Tab：
- `basic`：基本信息（保留现有 ProviderBasicInfoTab）
- `channels`：渠道管理（复用 ChannelGroupedList 组件）
- `connectivity`：连通性测试（保留现有 ConnectivityTestPanel）

移除 `endpoints`、`credentials`、`models`、`quota`、`advanced` 五个 Tab。在 channels Tab 中点击渠道卡片时打开 ChannelDetailDrawer。

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx
git commit -m "feat: 供应商详情页精简——6 Tab→3 Tab，渠道资源归入渠道详情"
```

---

### Task 6: 收尾 — 开发者门户下游Key + i18n + 清理

**Files:**
- Modify: `gateway-console/src/pages/Developer/DeveloperKeyList.tsx` (增加下游Key CRUD)
- Modify: i18n 文件
- Delete: `gateway-console/src/pages/ApiKeys/index.tsx` (移除独立页面入口，路由保留)
- Modify: `gateway-console/src/pages/Models/index.tsx` (移除菜单入口，页面保留)

- [ ] **Step 1: 增强开发者门户 Key 管理**

修改 `gateway-console/src/pages/Developer/DeveloperKeyList.tsx`，增加完整的下游 Key CRUD 功能（创建、吊销、查看用量、权限关联）。

- [ ] **Step 2: 更新 i18n 国际化键**

在 `zh-CN/common.json` 和 `en-US/common.json` 中添加渠道管理相关键，清理废弃的供应商详情 Tab 和 API Key 相关键。

- [ ] **Step 3: 清理废弃代码**

确认 `/api-keys` 和 `/models` 路由仍可用（深层链接兼容），但从 `menuConfig` 中移除入口。不删除 Models 和 ApiKeys 目录，仅从菜单隐藏。

- [ ] **Step 4: 最终 Commit**

```bash
git add gateway-console/src/pages/Developer/ gateway-console/src/locales/ gateway-console/src/constants/menuConfig.tsx
git commit -m "feat: 收尾——开发者门户Key管理+i18n更新+清理废弃菜单"
```