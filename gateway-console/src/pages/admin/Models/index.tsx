import { useState, useCallback, useMemo } from 'react';
import { Card, Segmented, message } from 'antd';
import { AppstoreOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { SearchFilterBar, type SearchFilters } from '@/components/common';
import { ProviderSidebar } from './ProviderSidebar';
import { ProviderDetail } from './ProviderDetail';
import { ModelTable } from './ModelTable';
import { ProviderModal } from './ProviderModal';
import { ModelAddModal } from './ModelAddModal';
import { ApiKeyModal } from './ApiKeyModal';
import {
  useProviders,
  useDeleteProvider,
  useDeleteModel,
} from '@/services/query';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';

type ViewMode = 'card' | 'table';

/**
 * 模型管理页面
 * 卡片视图：管理员视角，左侧供应商列表，右侧详情面板
 * 表格视图：用户视角，一行一个模型
 */
export default function AdminModels() {
  const { t } = useTranslation('models');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  // 视图模式
  const [viewMode, setViewMode] = useState<ViewMode>('card');

  // 搜索筛选（表格视图）
  const [, setFilters] = useState<SearchFilters>({ keyword: '' });

  // 卡片视图：选中的供应商
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null);

  // 弹窗状态
  const [providerModalOpen, setProviderModalOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);

  const [modelModalOpen, setModelModalOpen] = useState(false);
  const [, setEditingModel] = useState<Model | null>(null);
  const [addingModelProvider, setAddingModelProvider] = useState<Provider | null>(null);

  const [apiKeyModalOpen, setApiKeyModalOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<{ id: number; keyName: string; priority: number; weight: number; isDefault: boolean } | null>(null);

  // Queries
  const { data: providersData } = useProviders({ size: 100 });
  const providers = providersData?.items || [];

  // Mutations
  const deleteProviderMutation = useDeleteProvider();
  const deleteModelMutation = useDeleteModel();

  // 筛选选项（表格视图）
  const filterOptions = useMemo(
    () => [
      {
        key: 'providerType',
        label: t('search.filterByType'),
        options: [
          { value: 'OPENAI', label: t('type.OPENAI', { ns: 'providers' }) },
          { value: 'ANTHROPIC', label: t('type.ANTHROPIC', { ns: 'providers' }) },
          { value: 'GOOGLE', label: t('type.GEMINI', { ns: 'providers' }) },
          { value: 'AZURE', label: 'Azure' },
          { value: 'CUSTOM', label: t('type.OTHER', { ns: 'providers' }) },
        ],
      },
      {
        key: 'state',
        label: t('search.filterByStatus'),
        options: [
          { value: 'ACTIVE', label: t('state.active', { ns: 'common' }) },
          { value: 'DISABLED', label: t('state.disabled', { ns: 'common' }) },
        ],
      },
    ],
    [t]
  );

  // Provider 操作
  const handleAddProvider = useCallback(() => {
    setEditingProvider(null);
    setProviderModalOpen(true);
  }, []);

  const handleEditProvider = useCallback((provider: Provider) => {
    setEditingProvider(provider);
    setProviderModalOpen(true);
  }, []);

  const handleDeleteProvider = useCallback(async (provider: Provider) => {
    try {
      await deleteProviderMutation.mutateAsync(provider.id);
      message.success(t('message.success', { ns: 'common' }));
      setSelectedProvider(null);
    } catch (error) {
      console.error('Failed to delete provider:', error);
      message.error(t('message.failed', { ns: 'common', defaultValue: '操作失败' }));
    }
  }, [t, deleteProviderMutation]);

  const handleProviderSuccess = useCallback(() => {
    setProviderModalOpen(false);
    setEditingProvider(null);
    message.success(t('message.success', { ns: 'common' }));
  }, [t]);

  // API Key 操作
  const handleAddApiKey = useCallback(() => {
    if (!selectedProvider) return;
    setEditingApiKey(null);
    setApiKeyModalOpen(true);
  }, [selectedProvider]);

  const handleEditApiKey = useCallback((key: { id: number; keyName: string; priority: number; weight: number; isDefault: boolean }) => {
    setEditingApiKey(key);
    setApiKeyModalOpen(true);
  }, []);

  const handleApiKeySuccess = useCallback(() => {
    setApiKeyModalOpen(false);
    setEditingApiKey(null);
    message.success(t('message.success', { ns: 'common' }));
  }, [t]);

  // Model 操作
  const handleAddModel = useCallback(() => {
    if (!selectedProvider) return;
    setAddingModelProvider(selectedProvider);
    setEditingModel(null);
    setModelModalOpen(true);
  }, [selectedProvider]);

  const handleEditModel = useCallback((model: Model) => {
    const provider = providers.find(p => p.id === model.providerId);
    setAddingModelProvider(provider || null);
    setEditingModel(model);
    setModelModalOpen(true);
  }, [providers]);

  const handleDeleteModel = useCallback(async (model: Model) => {
    await deleteModelMutation.mutateAsync(model.id);
    message.success(t('message.success', { ns: 'common' }));
  }, [t, deleteModelMutation]);

  const handleModelSuccess = useCallback(() => {
    setModelModalOpen(false);
    setAddingModelProvider(null);
    setEditingModel(null);
    message.success(t('message.success', { ns: 'common' }));
  }, [t]);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 工具栏 */}
      <Card
        style={{
          marginBottom: 16,
          border: 'none',
          boxShadow: isDark
            ? '0 2px 8px rgba(0, 0, 0, 0.3)'
            : '0 2px 8px rgba(0, 0, 0, 0.06)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          {/* 左侧：搜索筛选（表格视图时显示） */}
          {viewMode === 'table' && (
            <SearchFilterBar
              placeholder={t('search.placeholder')}
              filters={filterOptions}
              onSearch={setFilters}
              onReset={() => setFilters({ keyword: '' })}
            />
          )}

          {/* 右侧：视图切换 */}
          <Segmented
            value={viewMode}
            onChange={(value) => {
              setViewMode(value as ViewMode);
              if (value === 'card') {
                setSelectedProvider(null);
              }
            }}
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
      </Card>

      {/* 内容区域 */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {viewMode === 'card' ? (
          /* 卡片视图：左侧供应商列表 + 右侧详情面板 */
          <div style={{ display: 'flex', gap: 16, height: '100%' }}>
            <div style={{ width: 280, height: '100%' }}>
              <ProviderSidebar
                selectedProviderId={selectedProvider?.id ?? null}
                onSelect={setSelectedProvider}
                onAdd={handleAddProvider}
              />
            </div>
            <div style={{ flex: 1, height: '100%' }}>
              <ProviderDetail
                provider={selectedProvider}
                onEditProvider={handleEditProvider}
                onDeleteProvider={handleDeleteProvider}
                onAddApiKey={handleAddApiKey}
                onEditApiKey={handleEditApiKey}
                onAddModel={handleAddModel}
                onEditModel={handleEditModel}
              />
            </div>
          </div>
        ) : (
          /* 表格视图：模型列表 */
          <div style={{ height: '100%', overflow: 'auto' }}>
            <ModelTable
              providers={providers}
              onEditModel={handleEditModel}
              onDeleteModel={handleDeleteModel}
            />
          </div>
        )}
      </div>

      {/* Provider 弹窗 */}
      <ProviderModal
        open={providerModalOpen}
        provider={editingProvider}
        onClose={() => {
          setProviderModalOpen(false);
          setEditingProvider(null);
        }}
        onSuccess={handleProviderSuccess}
      />

      {/* Model 弹窗 */}
      <ModelAddModal
        open={modelModalOpen}
        provider={addingModelProvider}
        onClose={() => {
          setModelModalOpen(false);
          setAddingModelProvider(null);
          setEditingModel(null);
        }}
        onSuccess={handleModelSuccess}
      />

      {/* API Key 弹窗 */}
      <ApiKeyModal
        open={apiKeyModalOpen}
        provider={selectedProvider}
        editingKey={editingApiKey}
        onClose={() => {
          setApiKeyModalOpen(false);
          setEditingApiKey(null);
        }}
        onSuccess={handleApiKeySuccess}
      />
    </div>
  );
}