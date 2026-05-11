import { useState, useCallback } from 'react';
import { Segmented, message, theme } from 'antd';
import { AppstoreOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderSidebar } from './ProviderSidebar';
import { ProviderDetail } from './ProviderDetail';
import { ModelsTableView } from './ModelsTableView';
import { ProviderModal } from './ProviderModal';
import { ModelAddModal } from './ModelAddModal';
import { ApiKeyModal } from './ApiKeyModal';
import {
  useProviders,
  useDeleteProvider,
} from '@/services/query';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';

type ViewMode = 'card' | 'table';

/**
 * 模型管理页面
 * 卡片视图：管理员视角，左侧供应商列表，右侧详情面板
 * 表格视图：用户视角，使用新的 EntityTable + EntityDrawer 组件
 */
export default function AdminModels() {
  const { t } = useTranslation('models');
  const { token } = theme.useToken();

  // 视图模式
  const [viewMode, setViewMode] = useState<ViewMode>('table');

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
    } catch {
      message.error(t('message.error', { ns: 'common' }));
    }
  }, [deleteProviderMutation, t]);

  const handleProviderSuccess = useCallback(() => {
    setProviderModalOpen(false);
    setEditingProvider(null);
  }, []);

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
  }, []);

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

  const handleModelSuccess = useCallback(() => {
    setModelModalOpen(false);
    setAddingModelProvider(null);
    setEditingModel(null);
  }, []);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 视图切换 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          padding: 16,
          background: token.colorBgContainer,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
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
              value: 'table',
              icon: <UnorderedListOutlined />,
              label: t('viewMode.table', { defaultValue: '表格' }),
            },
            {
              value: 'card',
              icon: <AppstoreOutlined />,
              label: t('viewMode.card', { defaultValue: '卡片' }),
            },
          ]}
        />
      </div>

      {/* 内容区域 */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {viewMode === 'table' ? (
          /* 表格视图：使用新的 EntityTable + EntityDrawer 组件 */
          <ModelsTableView />
        ) : (
          /* 卡片视图：左侧供应商列表 + 右侧详情面板 */
          <div style={{ display: 'flex', gap: 16, height: '100%', padding: 16 }}>
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