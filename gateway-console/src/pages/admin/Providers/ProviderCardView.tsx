import { useState, useCallback, useMemo } from 'react';
import { Row, Col, Empty, Spin, Modal, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { ProviderCard } from './ProviderCard';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
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
}

/**
 * 供应商卡片视图
 * 网格布局展示所有供应商卡片
 */
export function ProviderCardView({ onProviderSelect }: ProviderCardViewProps) {
  const { t } = useTranslation('providers');

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
              onEditModel={handleEditModel}
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
      {...props}
    />
  );
}

export type { ProviderCardViewProps };
