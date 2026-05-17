import { useState, useCallback, useMemo } from 'react';
import { Empty, Spin, Segmented, Space, Button } from 'antd';
import { AppstoreOutlined, UnorderedListOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { PageHeader } from '@/components/ui';
import { FilterPanel, FilterTags } from '@/components/common';
import { useConfirm } from '@/hooks/useConfirm';
import { ProviderCard } from './ProviderCard';
import { ApiKeyModal } from './ApiKeyModal';
import { ModelAddModal } from './ModelAddModal';
import {
  useProviders,
  useProviderKeys,
  useModels,
  useDeleteProvider,
} from '@/services/query';
import type { Provider } from '@/types/provider';
import type { ProviderApiKey } from '@/types/providerApiKey';
import type { Model } from '@/types/model';

interface ProviderCardViewProps {
  onProviderSelect?: (provider: Provider) => void;
  onProviderExperience?: (provider: Provider) => void;
  viewMode: 'card' | 'table';
  onViewModeChange: (mode: 'card' | 'table') => void;
  onAddProvider: () => void;
}

/**
 * 供应商卡片视图
 * 使用 CSS Grid 自适应布局，卡片宽度在 240px-320px 之间自动调整
 */
export function ProviderCardView({ onProviderSelect, onProviderExperience, viewMode, onViewModeChange, onAddProvider }: ProviderCardViewProps) {
  const { t } = useTranslation('providers');
  const { t: tc } = useTranslation('common');
  const { confirm } = useConfirm();

  // API Key 弹窗状态
  const [apiKeyModalOpen, setApiKeyModalOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<{ provider: Provider; key: ProviderApiKey | null } | null>(null);

  // 模型弹窗状态
  const [modelModalOpen, setModelModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<{ provider: Provider; model: Model | null } | null>(null);

  // 过滤器状态
  const [filterValues, setFilterValues] = useState<Record<string, string>>({});

  // 查询数据
  const { data: providersData, isLoading: providersLoading, refetch } = useProviders({ size: 100 });
  const providers = providersData?.items || [];

  // Mutations
  const deleteProviderMutation = useDeleteProvider();

  // 页面操作按钮（不包含视图切换）
  const pageActions = useMemo(() => [
    {
      key: 'add',
      label: tc('actions.add'),
      type: 'primary' as const,
      icon: <PlusOutlined />,
      onClick: onAddProvider,
      danger: false,
      disabled: false,
    },
    {
      key: 'refresh',
      label: tc('actions.refresh'),
      icon: <ReloadOutlined />,
      onClick: () => refetch(),
      loading: providersLoading,
      danger: false,
      disabled: false,
    },
  ], [tc, onAddProvider, refetch, providersLoading]);

  // 视图切换组件
  const viewModeSwitcher = useMemo(() => (
    <Segmented
      value={viewMode}
      onChange={(value) => onViewModeChange(value as 'card' | 'table')}
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
  ), [viewMode, onViewModeChange, t]);

  // 过滤字段配置
  const filterFields = useMemo(() => [
    {
      name: 'providerType',
      label: t('type.label', { defaultValue: '类型' }),
      options: [
        { value: 'all', label: t('filter.all', { defaultValue: '全部' }) },
        { value: 'OPENAI', label: 'OpenAI' },
        { value: 'ANTHROPIC', label: 'Anthropic' },
        { value: 'GOOGLE', label: 'Google' },
        { value: 'AZURE', label: 'Azure' },
        { value: 'DEEPSEEK', label: 'DeepSeek' },
        { value: 'QWEN', label: 'Qwen' },
        { value: 'ZHIPU', label: '智谱' },
        { value: 'MOONSHOT', label: 'Moonshot' },
        { value: 'BAICHUAN', label: '百川' },
        { value: 'MINIMAX', label: 'MiniMax' },
        { value: 'WENXIN', label: '文心' },
        { value: 'VOLCENGINE', label: '火山引擎' },
        { value: 'TENCENT', label: '腾讯' },
        { value: 'XUNFEI', label: '讯飞' },
        { value: 'CUSTOM', label: '其他' },
      ],
    },
    {
      name: 'state',
      label: t('state', { defaultValue: '状态' }),
      options: [
        { value: 'all', label: t('filter.all', { defaultValue: '全部' }) },
        { value: 'ACTIVE', label: tc('state.active') },
        { value: 'DISABLED', label: tc('state.disabled') },
      ],
    },
  ], [t, tc]);

  // 过滤处理
  const handleFilterChange = useCallback((values: Record<string, string>) => {
    setFilterValues(values);
  }, []);

  const handleFilterReset = useCallback(() => {
    setFilterValues({});
  }, []);

  // 过滤标签
  const filterTags = useMemo(() => {
    const tags: Array<{ key: string; label: string; value: string }> = [];
    if (filterValues.providerType && filterValues.providerType !== 'all') {
      const option = filterFields[0].options.find((o) => o.value === filterValues.providerType);
      tags.push({
        key: 'providerType',
        label: filterFields[0].label,
        value: option?.label || filterValues.providerType,
      });
    }
    if (filterValues.state && filterValues.state !== 'all') {
      const option = filterFields[1].options.find((o) => o.value === filterValues.state);
      tags.push({
        key: 'state',
        label: filterFields[1].label,
        value: option?.label || filterValues.state,
      });
    }
    return tags;
  }, [filterValues, filterFields]);

  // 前端过滤
  const filteredProviders = useMemo(() => {
    const hasFilters = Object.values(filterValues).some((v) => v && v !== 'all');
    if (!hasFilters) return providers;

    return providers.filter((provider) => {
      // 按类型过滤
      if (filterValues.providerType && filterValues.providerType !== 'all') {
        if (provider.providerType !== filterValues.providerType) {
          return false;
        }
      }
      // 按状态过滤
      if (filterValues.state && filterValues.state !== 'all') {
        if (provider.state !== filterValues.state) {
          return false;
        }
      }
      return true;
    });
  }, [providers, filterValues]);

  // 按状态排序：活跃优先
  const sortedProviders = useMemo(() => {
    return [...filteredProviders].sort((a, b) => {
      if (a.state === 'ACTIVE' && b.state !== 'ACTIVE') return -1;
      if (a.state !== 'ACTIVE' && b.state === 'ACTIVE') return 1;
      return a.providerName.localeCompare(b.providerName);
    });
  }, [filteredProviders]);

  // 打开查看抽屉
  const handleViewDetail = useCallback((provider: Provider) => {
    onProviderSelect?.(provider);
  }, [onProviderSelect]);

  // 删除供应商
  const handleDelete = useCallback((provider: Provider) => {
    confirm({
      type: 'danger',
      entityName: provider.providerName,
      onConfirm: () => deleteProviderMutation.mutateAsync(provider.id),
    });
  }, [confirm, deleteProviderMutation]);

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

  // 加载中
  if (providersLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ height: '100%', width: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 页面标题 */}
      <PageHeader
        title={t('title', { defaultValue: '供应商管理' })}
        actions={
          <Space>
            {viewModeSwitcher}
            {pageActions.map((action) => (
              <Button
                key={action.key}
                type={action.type || 'default'}
                danger={action.danger}
                icon={action.icon}
                onClick={action.onClick}
                loading={action.loading}
                disabled={action.disabled}
              >
                {action.label}
              </Button>
            ))}
          </Space>
        }
        extra={
          <FilterPanel
            fields={filterFields}
            values={filterValues}
            onChange={handleFilterChange}
            onReset={handleFilterReset}
            title={t('filter.providerFilter', { defaultValue: '供应商筛选器' })}
          />
        }
      />

      {/* 过滤标签 */}
      {filterTags.length > 0 && (
        <FilterTags
          filters={filterTags}
          onRemove={(key) => setFilterValues((prev) => ({ ...prev, [key]: 'all' }))}
          onClearAll={handleFilterReset}
        />
      )}

      {/* 卡片内容 - CSS Grid 自适应布局 */}
      <div
        style={{
          flex: 1,
          width: '100%',
          overflow: 'auto',
          padding: sortedProviders.length > 0 ? '16px 0' : 0,
          display: sortedProviders.length > 0 ? 'grid' : 'flex',
          gridTemplateColumns: sortedProviders.length > 0 ? 'repeat(auto-fill, minmax(300px, 1fr))' : undefined,
          gap: sortedProviders.length > 0 ? 16 : undefined,
          alignContent: sortedProviders.length > 0 ? 'start' : undefined,
          justifyContent: sortedProviders.length === 0 ? 'center' : undefined,
          alignItems: sortedProviders.length === 0 ? 'center' : undefined,
        }}
      >
        {sortedProviders.length === 0 ? (
          <Empty
            description={t('empty.noProvider', { defaultValue: '暂无供应商' })}
          />
        ) : (
          sortedProviders.map((provider) => (
            <ProviderCardContent
              key={provider.id}
              provider={provider}
              onViewDetail={handleViewDetail}
              onExperience={onProviderExperience}
              onDelete={handleDelete}
              onAddApiKey={handleAddApiKey}
              onAddModel={handleAddModel}
              onEditApiKey={handleEditApiKey}
              onEditModel={handleEditModel}
            />
          ))
        )}
      </div>

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
  onExperience,
  ...props
}: {
  provider: Provider;
  onViewDetail: (provider: Provider) => void;
  onExperience?: (provider: Provider) => void;
  onDelete: (provider: Provider) => void;
  onAddApiKey: (provider: Provider) => void;
  onAddModel: (provider: Provider) => void;
  onEditApiKey: (key: ProviderApiKey) => void;
  onEditModel: (model: Model) => void;
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
      onExperience={onExperience}
      {...props}
    />
  );
}

export type { ProviderCardViewProps };
