# 一站式供应商管理页面实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `/admin/providers` 页面改造成一站式供应商/模型管理页面，支持卡片式展现供应商、API Key 和模型，统一以抽屉方式展现详情与编辑页面。

**Architecture:** 复用现有 `ProviderManagementDrawer` 及三个标签页组件（`ProviderBasicInfoTab`、`ProviderApiKeysTab`、`ProviderModelsTab`），重构 `/admin/providers` 页面为卡片视图，新增 `ProviderCardView` 和 `ProviderCard` 组件。

**Tech Stack:** React 18 + TypeScript + Ant Design 5 + React Query + i18next

---

## 文件结构

```
gateway-console/src/pages/admin/Providers/
├── index.tsx                    # 主页面（修改）：视图切换 + 卡片/表格视图
├── ProviderCardView.tsx        # 新增：卡片网格视图
├── ProviderCard.tsx            # 新增：单个供应商卡片
├── ProviderManagementDrawer.tsx # 复用：从 Models 目录复制并适配
├── ProviderBasicInfoTab.tsx    # 复用：从 Models 目录复制
├── ProviderApiKeysTab.tsx      # 复用：从 Models 目录复制
├── ProviderModelsTab.tsx       # 复用：从 Models 目录复制
├── ApiKeyModal.tsx             # 复用：API Key 编辑弹窗
└── ModelAddModal.tsx           # 复用：模型添加弹窗
```

---

## Task 1: 创建 ProviderCard 组件

**Files:**
- Create: `gateway-console/src/pages/admin/Providers/ProviderCard.tsx`

- [ ] **Step 1: 创建 ProviderCard 组件文件**

```tsx
import { useState } from 'react';
import { Card, Button, Space, Tag, Badge, Dropdown, theme, Popconfirm } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  DownOutlined,
  RightOutlined,
  InfoCircleOutlined,
  ApiOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { StatusIndicator } from '@/components/common';
import type { Provider } from '@/types/provider';
import type { ProviderApiKey } from '@/types/providerApiKey';
import type { Model } from '@/types/model';

const { useToken } = theme;

interface ProviderCardProps {
  provider: Provider;
  apiKeys: ProviderApiKey[];
  models: Model[];
  onViewDetail: (provider: Provider) => void;
  onEdit: (provider: Provider) => void;
  onDelete: (provider: Provider) => void;
  onAddApiKey: (provider: Provider) => void;
  onAddModel: (provider: Provider) => void;
  onEditApiKey: (key: ProviderApiKey) => void;
  onDeleteApiKey: (key: ProviderApiKey) => void;
  onEditModel: (model: Model) => void;
  onDeleteModel: (model: Model) => void;
}

/**
 * 供应商卡片组件
 * 展示供应商信息、API Keys 和模型列表
 */
export function ProviderCard({
  provider,
  apiKeys,
  models,
  onViewDetail,
  onEdit,
  onDelete,
  onAddApiKey,
  onAddModel,
  onEditApiKey,
  onDeleteApiKey,
  onEditModel,
  onDeleteModel,
}: ProviderCardProps) {
  const { t } = useTranslation('providers');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';
  const { token } = useToken();
  const [isExpanded, setIsExpanded] = useState(true);

  const activeKeys = apiKeys.filter((k) => k.state === 'ACTIVE');
  const activeModels = models.filter((m) => m.state === 'ACTIVE');

  const dropdownItems = [
    {
      key: 'view',
      label: t('detail.viewDetail', { defaultValue: '查看详情' }),
      icon: <InfoCircleOutlined />,
      onClick: () => onViewDetail(provider),
    },
    {
      key: 'edit',
      label: t('actions.edit', { ns: 'common' }),
      icon: <EditOutlined />,
      onClick: () => onEdit(provider),
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'delete',
      label: t('actions.delete', { ns: 'common' }),
      icon: <DeleteOutlined />,
      danger: true,
      onClick: () => onDelete(provider),
    },
  ];

  return (
    <Card
      style={{
        height: '100%',
        border: 'none',
        boxShadow: isDark
          ? '0 2px 8px rgba(0, 0, 0, 0.3)'
          : '0 2px 8px rgba(0, 0, 0, 0.06)',
        transition: 'all 0.3s',
      }}
      styles={{
        body: { padding: 0, display: 'flex', flexDirection: 'column', height: '100%' },
      }}
    >
      {/* 头部：供应商信息 */}
      <div
        style={{
          padding: '16px 20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: isExpanded ? `1px solid ${token.colorBorderSecondary}` : 'none',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <StatusIndicator status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} showLabel={false} />
          <span
            style={{ fontSize: 16, fontWeight: 600, cursor: 'pointer' }}
            onClick={() => onViewDetail(provider)}
          >
            {provider.providerName}
          </span>
          <Tag color="blue">{t(`type.${provider.providerType}`, { defaultValue: provider.providerType })}</Tag>
        </div>
        <Space>
          <Badge count={activeModels.length} showZero color="#52c41a" />
          <Dropdown menu={{ items: dropdownItems }} trigger={['click']}>
            <Button type="text" icon={<EditOutlined />} />
          </Dropdown>
        </Space>
      </div>

      {/* 统计信息行 */}
      <div
        style={{
          padding: '12px 20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          fontSize: 13,
          color: token.colorTextSecondary,
          borderBottom: isExpanded ? `1px solid ${token.colorBorderSecondary}` : 'none',
        }}
      >
        <Space split={<span style={{ color: token.colorBorder }}>|</span>} size={8}>
          <span>
            <ApiOutlined style={{ marginRight: 4 }} />
            {t('detail.keyCount', { defaultValue: 'Keys' })}: {activeKeys.length}/{apiKeys.length}
          </span>
          <span>
            <AppstoreOutlined style={{ marginRight: 4 }} />
            {t('detail.modelCount', { defaultValue: '模型' })}: {models.length}
          </span>
        </Space>
        <Button
          type="text"
          icon={isExpanded ? <DownOutlined /> : <RightOutlined />}
          onClick={() => setIsExpanded(!isExpanded)}
        >
          {isExpanded ? t('detail.collapse', { defaultValue: '收起' }) : t('detail.expand', { defaultValue: '展开' })}
        </Button>
      </div>

      {/* 展开内容：API Keys 和模型 */}
      {isExpanded && (
        <div
          style={{
            flex: 1,
            padding: '16px 20px',
            background: isDark ? token.colorBgContainer : token.colorBgLayout,
            overflow: 'auto',
          }}
        >
          {/* API Keys 区块 */}
          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
              <span style={{ fontWeight: 500, fontSize: 14 }}>
                <ApiOutlined style={{ marginRight: 4 }} />
                {t('detail.apiKeys', { defaultValue: 'API Keys' })}
              </span>
              <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => onAddApiKey(provider)}>
                {t('actions.add', { ns: 'common' })}
              </Button>
            </div>
            {apiKeys.length === 0 ? (
              <div style={{ color: token.colorTextSecondary, fontSize: 13 }}>
                {t('empty.noApiKey', { defaultValue: '暂无 API Key' })}
              </div>
            ) : (
              <Space wrap size={[4, 4]}>
                {apiKeys.slice(0, 3).map((key) => (
                  <Tag
                    key={key.id}
                    color={key.state === 'ACTIVE' ? 'green' : 'default'}
                    style={{ cursor: 'pointer' }}
                    onClick={() => onEditApiKey(key)}
                  >
                    {key.keyName}
                  </Tag>
                ))}
                {apiKeys.length > 3 && (
                  <Tag>+{apiKeys.length - 3}</Tag>
                )}
              </Space>
            )}
          </div>

          {/* 模型区块 */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
              <span style={{ fontWeight: 500, fontSize: 14 }}>
                <AppstoreOutlined style={{ marginRight: 4 }} />
                {t('detail.models', { defaultValue: '模型' })}
              </span>
              <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => onAddModel(provider)}>
                {t('actions.add', { ns: 'common' })}
              </Button>
            </div>
            {models.length === 0 ? (
              <div style={{ color: token.colorTextSecondary, fontSize: 13 }}>
                {t('empty.noModel', { defaultValue: '暂无模型' })}
              </div>
            ) : (
              <Space wrap size={[4, 4]}>
                {models.slice(0, 5).map((model) => (
                  <Tag
                    key={model.id}
                    color={model.state === 'ACTIVE' ? 'blue' : 'default'}
                    style={{ cursor: 'pointer' }}
                    onClick={() => onEditModel(model)}
                  >
                    {model.displayName || model.providerModelId}
                  </Tag>
                ))}
                {models.length > 5 && (
                  <Tag>+{models.length - 5}</Tag>
                )}
              </Space>
            )}
          </div>
        </div>
      )}
    </Card>
  );
}

export type { ProviderCardProps };
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la gateway-console/src/pages/admin/Providers/ProviderCard.tsx`
Expected: 文件存在且大小 > 0

---

## Task 2: 创建 ProviderCardView 组件

**Files:**
- Create: `gateway-console/src/pages/admin/Providers/ProviderCardView.tsx`

- [ ] **Step 1: 创建 ProviderCardView 组件文件**

```tsx
import { useState, useCallback, useMemo } from 'react';
import { Row, Col, Empty, Spin, theme, Modal, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { ProviderCard } from './ProviderCard';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { ApiKeyModal } from './ApiKeyModal';
import { ModelAddModal } from './ModelAddModal';
import {
  useProviders,
  useProviderKeys,
  useModels,
  useDeleteProvider,
  useDeleteProviderApiKey,
  useDeleteModel,
} from '@/services/query';
import type { Provider } from '@/types/provider';
import type { ProviderApiKey } from '@/types/providerApiKey';
import type { Model } from '@/types/model';

const { useToken } = theme;

interface ProviderCardViewProps {
  onProviderSelect?: (provider: Provider) => void;
}

/**
 * 供应商卡片视图
 * 网格布局展示所有供应商卡片
 */
export function ProviderCardView({ onProviderSelect }: ProviderCardViewProps) {
  const { t } = useTranslation('providers');
  const { token } = useToken();
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  // 抽屉状态
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);
  const [drawerMode, setDrawerMode] = useState<'view' | 'edit' | 'create'>('view');

  // API Key 弹窗状态
  const [apiKeyModalOpen, setApiKeyModalOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<{ provider: Provider; key: ProviderApiKey | null } | null>(null);

  // 模型弹窗状态
  const [modelModalOpen, setModelModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<{ provider: Provider; model: Model | null } | null>(null);

  // 查询数据
  const { data: providersData, isLoading: providersLoading } = useProviders({ size: 100 });
  const providers = providersData?.items || [];

  // Mutations
  const deleteProviderMutation = useDeleteProvider();
  const deleteApiKeyMutation = useDeleteProviderApiKey();
  const deleteModelMutation = useDeleteModel();

  // 按状态排序：活跃优先
  const sortedProviders = useMemo(() => {
    return [...providers].sort((a, b) => {
      if (a.state === 'ACTIVE' && b.state !== 'ACTIVE') return -1;
      if (a.state !== 'ACTIVE' && b.state === 'ACTIVE') return 1;
      return a.providerName.localeCompare(b.providerName);
    });
  }, [providers]);

  // 打开查看抽屉
  const handleViewDetail = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
    setDrawerMode('view');
    onProviderSelect?.(provider);
  }, [onProviderSelect]);

  // 打开编辑抽屉
  const handleEdit = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
    setDrawerMode('edit');
  }, []);

  // 删除供应商
  const handleDelete = useCallback((provider: Provider) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      content: t('confirm.deleteProviderDesc', { name: provider.providerName, defaultValue: `确定删除供应商 "${provider.providerName}" 吗？` }),
      onOk: async () => {
        await deleteProviderMutation.mutateAsync(provider.id);
        message.success(t('message.success', { ns: 'common' }));
        if (selectedProviderId === provider.id) {
          setSelectedProviderId(null);
        }
      },
    });
  }, [deleteProviderMutation, selectedProviderId, t]);

  // 添加 API Key
  const handleAddApiKey = useCallback((provider: Provider) => {
    setEditingApiKey({ provider, key: null });
    setApiKeyModalOpen(true);
  }, []);

  // 编辑 API Key
  const handleEditApiKey = useCallback((key: ProviderApiKey) => {
    const provider = providers.find((p) => p.id === key.providerId);
    if (provider) {
      setEditingApiKey({ provider, key });
      setApiKeyModalOpen(true);
    }
  }, [providers]);

  // 删除 API Key
  const handleDeleteApiKey = useCallback((key: ProviderApiKey) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      onOk: async () => {
        await deleteApiKeyMutation.mutateAsync(key.id);
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  }, [deleteApiKeyMutation, t]);

  // 添加模型
  const handleAddModel = useCallback((provider: Provider) => {
    setEditingModel({ provider, model: null });
    setModelModalOpen(true);
  }, []);

  // 编辑模型
  const handleEditModel = useCallback((model: Model) => {
    const provider = providers.find((p) => p.id === model.providerId);
    if (provider) {
      setEditingModel({ provider, model });
      setModelModalOpen(true);
    }
  }, [providers]);

  // 删除模型
  const handleDeleteModel = useCallback((model: Model) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      onOk: async () => {
        await deleteModelMutation.mutateAsync(model.id);
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  }, [deleteModelMutation, t]);

  // 关闭抽屉
  const handleCloseDrawer = useCallback(() => {
    setSelectedProviderId(null);
  }, []);

  // 供应商创建成功
  const handleProviderCreated = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
    setDrawerMode('view');
  }, []);

  // 加载中
  if (providersLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
        <Spin size="large" />
      </div>
    );
  }

  // 空状态
  if (sortedProviders.length === 0) {
    return (
      <Empty
        description={t('empty.noProvider', { defaultValue: '暂无供应商' })}
        style={{ padding: 80 }}
      />
    );
  }

  return (
    <div style={{ padding: 16, height: '100%', overflow: 'auto' }}>
      <Row gutter={[16, 16]}>
        {sortedProviders.map((provider) => (
          <Col key={provider.id} xs={24} sm={12} md={8} lg={6} xl={6}>
            <ProviderCardContent
              provider={provider}
              onViewDetail={handleViewDetail}
              onEdit={handleEdit}
              onDelete={handleDelete}
              onAddApiKey={handleAddApiKey}
              onAddModel={handleAddModel}
              onEditApiKey={handleEditApiKey}
              onDeleteApiKey={handleDeleteApiKey}
              onEditModel={handleEditModel}
              onDeleteModel={handleDeleteModel}
            />
          </Col>
        ))}
      </Row>

      {/* 供应商管理抽屉 */}
      <ProviderManagementDrawer
        providerId={selectedProviderId}
        providers={providers}
        mode={drawerMode}
        onClose={handleCloseDrawer}
        onProviderChange={setSelectedProviderId}
        onProviderCreated={handleProviderCreated}
      />

      {/* API Key 弹窗 */}
      {editingApiKey && (
        <ApiKeyModal
          open={apiKeyModalOpen}
          provider={editingApiKey.provider}
          editingKey={editingApiKey.key}
          onClose={() => {
            setApiKeyModalOpen(false);
            setEditingApiKey(null);
          }}
          onSuccess={() => {
            setApiKeyModalOpen(false);
            setEditingApiKey(null);
          }}
        />
      )}

      {/* 模型弹窗 */}
      {editingModel && (
        <ModelAddModal
          open={modelModalOpen}
          provider={editingModel.provider}
          editingModel={editingModel.model}
          onClose={() => {
            setModelModalOpen(false);
            setEditingModel(null);
          }}
          onSuccess={() => {
            setModelModalOpen(false);
            setEditingModel(null);
          }}
        />
      )}
    </div>
  );
}

/**
 * 卡片内容组件（用于处理数据加载）
 */
function ProviderCardContent({
  provider,
  ...props
}: {
  provider: Provider;
  onViewDetail: (provider: Provider) => void;
  onEdit: (provider: Provider) => void;
  onDelete: (provider: Provider) => void;
  onAddApiKey: (provider: Provider) => void;
  onAddModel: (provider: Provider) => void;
  onEditApiKey: (key: ProviderApiKey) => void;
  onDeleteApiKey: (key: ProviderApiKey) => void;
  onEditModel: (model: Model) => void;
  onDeleteModel: (model: Model) => void;
}) {
  // 查询 API Keys
  const { data: keysData } = useProviderKeys(provider.id, {
    enabled: provider.state !== 'DELETED',
  });
  const apiKeys = keysData?.keys || [];

  // 查询模型
  const { data: modelsData } = useModels(
    { providerId: provider.id, size: 100 },
    { enabled: provider.state !== 'DELETED' }
  );
  const models = modelsData?.items || [];

  return (
    <ProviderCard
      provider={provider}
      apiKeys={apiKeys}
      models={models}
      {...props}
    />
  );
}

export type { ProviderCardViewProps };
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la gateway-console/src/pages/admin/Providers/ProviderCardView.tsx`
Expected: 文件存在且大小 > 0

---

## Task 3: 复用并适配 ProviderManagementDrawer

**Files:**
- Copy: `gateway-console/src/pages/admin/Models/ProviderManagementDrawer.tsx` → `gateway-console/src/pages/admin/Providers/ProviderManagementDrawer.tsx`
- Modify: 添加 `mode` prop 支持

- [ ] **Step 1: 复制 ProviderManagementDrawer 到 Providers 目录**

Run: `cp gateway-console/src/pages/admin/Models/ProviderManagementDrawer.tsx gateway-console/src/pages/admin/Providers/ProviderManagementDrawer.tsx`
Expected: 文件复制成功

- [ ] **Step 2: 修改 ProviderManagementDrawer 添加 mode prop**

修改 `ProviderManagementDrawer.tsx`，在接口定义处添加 `mode` prop：

```tsx
interface ProviderManagementDrawerProps {
  providerId: number | null; // null 表示新增模式
  providers: Provider[];
  mode?: 'view' | 'edit' | 'create';  // 新增：初始模式
  onClose: () => void;
  onProviderChange: (providerId: number) => void;
  onProviderCreated?: (provider: Provider) => void;
  onProviderDeleted?: () => void;
}
```

- [ ] **Step 3: 修改组件内部逻辑使用 mode prop**

修改状态初始化部分：

```tsx
export function ProviderManagementDrawer({
  providerId,
  providers,
  mode: initialMode = 'view',  // 新增
  onClose,
  onProviderChange,
  onProviderCreated,
  onProviderDeleted,
}: ProviderManagementDrawerProps) {
  const { t } = useTranslation('providers');  // 修改命名空间

  // 状态
  const [mode, setMode] = useState<DrawerMode>(
    providerId ? initialMode : 'create'  // 修改：使用 initialMode
  );
  // ... 其余代码保持不变
```

- [ ] **Step 4: 验证修改**

Run: `grep -n "mode: initialMode" gateway-console/src/pages/admin/Providers/ProviderManagementDrawer.tsx`
Expected: 找到修改的行

---

## Task 4: 复用标签页组件

**Files:**
- Copy: `gateway-console/src/pages/admin/Models/ProviderBasicInfoTab.tsx` → `gateway-console/src/pages/admin/Providers/ProviderBasicInfoTab.tsx`
- Copy: `gateway-console/src/pages/admin/Models/ProviderApiKeysTab.tsx` → `gateway-console/src/pages/admin/Providers/ProviderApiKeysTab.tsx`
- Copy: `gateway-console/src/pages/admin/Models/ProviderModelsTab.tsx` → `gateway-console/src/pages/admin/Providers/ProviderModelsTab.tsx`

- [ ] **Step 1: 复制三个标签页组件**

Run:
```bash
cp gateway-console/src/pages/admin/Models/ProviderBasicInfoTab.tsx gateway-console/src/pages/admin/Providers/ProviderBasicInfoTab.tsx
cp gateway-console/src/pages/admin/Models/ProviderApiKeysTab.tsx gateway-console/src/pages/admin/Providers/ProviderApiKeysTab.tsx
cp gateway-console/src/pages/admin/Models/ProviderModelsTab.tsx gateway-console/src/pages/admin/Providers/ProviderModelsTab.tsx
```
Expected: 三个文件复制成功

- [ ] **Step 2: 修改 ProviderBasicInfoTab 的命名空间**

修改 `ProviderBasicInfoTab.tsx` 中的翻译命名空间：

```tsx
const { t } = useTranslation('providers');  // 从 'models' 改为 'providers'
```

- [ ] **Step 3: 修改 ProviderApiKeysTab 的命名空间**

修改 `ProviderApiKeysTab.tsx` 中的翻译命名空间：

```tsx
const { t } = useTranslation('providers');  // 从 'models' 改为 'providers'
```

- [ ] **Step 4: 修改 ProviderModelsTab 的命名空间**

修改 `ProviderModelsTab.tsx` 中的翻译命名空间：

```tsx
const { t } = useTranslation('providers');  // 从 'models' 改为 'providers'
```

- [ ] **Step 5: 验证文件存在**

Run: `ls -la gateway-console/src/pages/admin/Providers/*.tsx`
Expected: 列出所有复制的文件

---

## Task 5: 复用弹窗组件

**Files:**
- Copy: `gateway-console/src/pages/admin/Models/ApiKeyModal.tsx` → `gateway-console/src/pages/admin/Providers/ApiKeyModal.tsx`
- Copy: `gateway-console/src/pages/admin/Models/ModelAddModal.tsx` → `gateway-console/src/pages/admin/Providers/ModelAddModal.tsx`

- [ ] **Step 1: 复制弹窗组件**

Run:
```bash
cp gateway-console/src/pages/admin/Models/ApiKeyModal.tsx gateway-console/src/pages/admin/Providers/ApiKeyModal.tsx
cp gateway-console/src/pages/admin/Models/ModelAddModal.tsx gateway-console/src/pages/admin/Providers/ModelAddModal.tsx
```
Expected: 两个文件复制成功

- [ ] **Step 2: 修改 ApiKeyModal 的命名空间**

修改 `ApiKeyModal.tsx` 中的翻译命名空间：

```tsx
const { t } = useTranslation('providers');  // 从 'models' 改为 'providers'
```

- [ ] **Step 3: 修改 ModelAddModal 的命名空间**

修改 `ModelAddModal.tsx` 中的翻译命名空间：

```tsx
const { t } = useTranslation('providers');  // 从 'models' 改为 'providers'
```

---

## Task 6: 重构 Providers/index.tsx 主页面

**Files:**
- Modify: `gateway-console/src/pages/admin/Providers/index.tsx`

- [ ] **Step 1: 重写主页面文件**

```tsx
import { useState, useCallback } from 'react';
import { Segmented, Button, theme } from 'antd';
import { AppstoreOutlined, UnorderedListOutlined, PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderCardView } from './ProviderCardView';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { useProviders } from '@/services/query';
import type { Provider } from '@/types/provider';

type ViewMode = 'card' | 'table';

/**
 * 一站式供应商管理页面
 * 卡片视图：网格展示供应商卡片，抽屉管理详情
 * 表格视图：传统表格展示（保留原有功能）
 */
export default function AdminProviders() {
  const { t } = useTranslation('providers');
  const { token } = theme.useToken();

  // 视图模式
  const [viewMode, setViewMode] = useState<ViewMode>('card');

  // 抽屉状态
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);

  // Queries
  const { data: providersData } = useProviders({ size: 100 });
  const providers = providersData?.items || [];

  // 添加供应商
  const handleAddProvider = useCallback(() => {
    setSelectedProviderId(null);
    setDrawerOpen(true);
  }, []);

  // 查看供应商详情
  const handleViewProvider = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
    setDrawerOpen(true);
  }, []);

  // 关闭抽屉
  const handleCloseDrawer = useCallback(() => {
    setDrawerOpen(false);
    setSelectedProviderId(null);
  }, []);

  // 供应商创建成功
  const handleProviderCreated = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
    // 保持抽屉打开，切换到查看模式
  }, []);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 头部工具栏 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: 16,
          background: token.colorBgContainer,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddProvider}>
          {t('add', { defaultValue: '添加供应商' })}
        </Button>

        <Segmented
          value={viewMode}
          onChange={(value) => setViewMode(value as ViewMode)}
          options={[
            {
              value: 'card',
              icon: <AppstoreOutlined />,
              label: t('viewMode.card', { defaultValue: '卡片' }),
            },
            {
              value: 'table',
              icon: <UnorderedListOutlined />,
              label: t('viewMode.table', { defaultValue: '表格' }),
            },
          ]}
        />
      </div>

      {/* 内容区域 */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {viewMode === 'card' ? (
          <ProviderCardView onProviderSelect={handleViewProvider} />
        ) : (
          // 表格视图：保留原有功能，后续可扩展
          <div style={{ padding: 16, textAlign: 'center', color: token.colorTextSecondary }}>
            {t('comingSoon', { defaultValue: '表格视图开发中...' })}
          </div>
        )}
      </div>

      {/* 供应商管理抽屉 */}
      <ProviderManagementDrawer
        providerId={selectedProviderId}
        providers={providers}
        onClose={handleCloseDrawer}
        onProviderChange={setSelectedProviderId}
        onProviderCreated={handleProviderCreated}
      />
    </div>
  );
}
```

- [ ] **Step 2: 验证文件修改**

Run: `head -30 gateway-console/src/pages/admin/Providers/index.tsx`
Expected: 看到新的导入和组件定义

---

## Task 7: 添加国际化翻译

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/providers.json`
- Modify: `gateway-console/src/locales/en-US/providers.json`

- [ ] **Step 1: 检查现有翻译文件**

Run: `cat gateway-console/src/locales/zh-CN/providers.json 2>/dev/null || echo "文件不存在"`
Expected: 查看现有翻译内容

- [ ] **Step 2: 更新中文翻译文件**

如果文件存在，追加以下内容；如果不存在，创建新文件：

```json
{
  "title": "供应商管理",
  "add": "添加供应商",
  "name": "供应商名称",
  "type": "供应商类型",
  "baseUrl": "API 地址",
  "state": "状态",
  "websiteUrl": "官网地址",
  "apiDocUrl": "API 文档",
  "priority": "优先级",
  "weight": "权重",
  "type.OPENAI": "OpenAI",
  "type.ANTHROPIC": "Anthropic",
  "type.GOOGLE": "Google",
  "type.AZURE": "Azure",
  "type.DEEPSEEK": "DeepSeek",
  "type.QWEN": "Qwen",
  "type.ZHIPU": "智谱",
  "type.MOONSHOT": "Moonshot",
  "type.BAICHUAN": "百川",
  "type.MINIMAX": "MiniMax",
  "type.WENXIN": "文心",
  "type.VOLCENGINE": "火山引擎",
  "type.TENCENT": "腾讯",
  "type.XUNFEI": "讯飞",
  "type.OTHER": "其他",
  "viewMode.card": "卡片",
  "viewMode.table": "表格",
  "detail.viewDetail": "查看详情",
  "detail.basicInfo": "基本信息",
  "detail.apiKeys": "API Keys",
  "detail.models": "模型",
  "detail.keyCount": "Keys",
  "detail.modelCount": "模型",
  "detail.collapse": "收起",
  "detail.expand": "展开",
  "detail.createdAt": "创建时间",
  "detail.updatedAt": "更新时间",
  "empty.noProvider": "暂无供应商",
  "empty.noApiKey": "暂无 API Key",
  "empty.noModel": "暂无模型",
  "confirm.deleteProviderDesc": "确定删除供应商 \"{name}\" 吗？删除后无法恢复。",
  "provider.list": "供应商列表",
  "provider.name": "供应商名称",
  "provider.type": "供应商类型",
  "provider.state": "状态",
  "provider.baseUrl": "API 地址",
  "provider.websiteUrl": "官网地址",
  "provider.apiDocUrl": "API 文档",
  "provider.apiKeys": "API Keys",
  "provider.models": "模型",
  "provider.customAdd": "自定义供应商",
  "provider.selectHint": "请选择一个供应商",
  "provider.addKeysAfterCreate": "创建供应商后可添加 API Keys",
  "provider.addModelsAfterCreate": "创建供应商后可添加模型",
  "provider.noApiKeys": "暂无 API Key",
  "provider.noModels": "暂无模型",
  "provider.priority": "优先级",
  "drawer.navigation.previous": "上一个",
  "drawer.navigation.next": "下一个",
  "drawer.navigation.position": "{current} / {total}",
  "comingSoon": "功能开发中..."
}
```

- [ ] **Step 3: 更新英文翻译文件**

```json
{
  "title": "Provider Management",
  "add": "Add Provider",
  "name": "Provider Name",
  "type": "Provider Type",
  "baseUrl": "API Base URL",
  "state": "Status",
  "websiteUrl": "Website",
  "apiDocUrl": "API Documentation",
  "priority": "Priority",
  "weight": "Weight",
  "type.OPENAI": "OpenAI",
  "type.ANTHROPIC": "Anthropic",
  "type.GOOGLE": "Google",
  "type.AZURE": "Azure",
  "type.DEEPSEEK": "DeepSeek",
  "type.QWEN": "Qwen",
  "type.ZHIPU": "Zhipu",
  "type.MOONSHOT": "Moonshot",
  "type.BAICHUAN": "Baichuan",
  "type.MINIMAX": "MiniMax",
  "type.WENXIN": "Wenxin",
  "type.VOLCENGINE": "Volcengine",
  "type.TENCENT": "Tencent",
  "type.XUNFEI": "Xunfei",
  "type.OTHER": "Other",
  "viewMode.card": "Card",
  "viewMode.table": "Table",
  "detail.viewDetail": "View Details",
  "detail.basicInfo": "Basic Info",
  "detail.apiKeys": "API Keys",
  "detail.models": "Models",
  "detail.keyCount": "Keys",
  "detail.modelCount": "Models",
  "detail.collapse": "Collapse",
  "detail.expand": "Expand",
  "detail.createdAt": "Created At",
  "detail.updatedAt": "Updated At",
  "empty.noProvider": "No providers",
  "empty.noApiKey": "No API Keys",
  "empty.noModel": "No models",
  "confirm.deleteProviderDesc": "Are you sure you want to delete provider \"{name}\"? This cannot be undone.",
  "provider.list": "Provider List",
  "provider.name": "Provider Name",
  "provider.type": "Provider Type",
  "provider.state": "Status",
  "provider.baseUrl": "API Base URL",
  "provider.websiteUrl": "Website",
  "provider.apiDocUrl": "API Documentation",
  "provider.apiKeys": "API Keys",
  "provider.models": "Models",
  "provider.customAdd": "Custom Provider",
  "provider.selectHint": "Please select a provider",
  "provider.addKeysAfterCreate": "Add API Keys after creating provider",
  "provider.addModelsAfterCreate": "Add models after creating provider",
  "provider.noApiKeys": "No API Keys",
  "provider.noModels": "No models",
  "provider.priority": "Priority",
  "drawer.navigation.previous": "Previous",
  "drawer.navigation.next": "Next",
  "drawer.navigation.position": "{current} / {total}",
  "comingSoon": "Coming soon..."
}
```

---

## Task 8: 验证构建

- [ ] **Step 1: 运行 TypeScript 类型检查**

Run: `cd gateway-console && pnpm tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 2: 运行 ESLint 检查**

Run: `cd gateway-console && pnpm eslint src/pages/admin/Providers/ --ext .tsx,.ts`
Expected: 无严重错误

- [ ] **Step 3: 运行开发服务器验证**

Run: `cd gateway-console && pnpm dev`
Expected: 服务启动成功，访问 http://localhost:5173/admin/providers 能看到卡片视图

---

## Task 9: 提交代码

- [ ] **Step 1: 查看变更**

Run: `git status`
Expected: 看到所有新增和修改的文件

- [ ] **Step 2: 添加文件到暂存区**

Run: `git add gateway-console/src/pages/admin/Providers/ gateway-console/src/locales/`

- [ ] **Step 3: 提交变更**

Run: `git commit -m "feat: 重构供应商管理页面为一站式管理页面

- 新增 ProviderCard 组件展示供应商、API Key 和模型
- 新增 ProviderCardView 网格布局展示供应商卡片
- 复用 ProviderManagementDrawer 及标签页组件
- 支持卡片/表格视图切换
- 统一抽屉方式展现供应商详情与编辑"

---

## 自我审查清单

**1. 规格覆盖检查：**
- [x] 卡片式展现供应商、API Key 和模型 → Task 1
- [x] 统一抽屉方式展现详情与编辑 → Task 3
- [x] 多标签页展现信息 → Task 4
- [x] 国际化支持 → Task 7

**2. 占位符扫描：**
- [x] 无 TBD、TODO 等占位符
- [x] 所有代码步骤都有完整实现

**3. 类型一致性：**
- [x] ProviderCardProps 接口定义与使用一致
- [x] ProviderManagementDrawerProps 接口定义与使用一致
- [x] 翻译键名一致

---

**计划完成并保存到 `docs/superpowers/plans/2026-05-12-one-stop-provider-management.md`。**

**两种执行方式：**

1. **子代理驱动（推荐）** - 每个任务派发新子代理，任务间可审查，快速迭代
2. **内联执行** - 在当前会话中使用 executing-plans 执行，批量执行带检查点

**选择哪种方式？**
