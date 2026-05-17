import { useState, useMemo } from 'react';
import { Card, Button, Empty } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useConfirm } from '@/hooks/useConfirm';
import {
  useApiKeys,
  useCreateApiKey,
  useUpdateApiKey,
  useDeleteApiKey,
  useSetEnabledApiKey,
  useApiKeyUsageBatch,
} from '@/services/query';
import { ApiKeyCard } from './ApiKeyCard';
import { ApiKeyFormModal, type FormValues } from './ApiKeyFormModal';
import type { ApiKey, ApiKeyUsage } from '@/types/apiKey';

/**
 * 用户视角 API Key 管理页面
 */
export default function UserApiKeysView() {
  const { t } = useTranslation('apiKeys');
  const { confirm } = useConfirm();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<ApiKey | null>(null);
  const [newKey, setNewKey] = useState<string | null>(null);

  const { data } = useApiKeys({ size: 100 });
  const { data: usageData } = useApiKeyUsageBatch();

  const createMutation = useCreateApiKey();
  const updateMutation = useUpdateApiKey();
  const deleteMutation = useDeleteApiKey();
  const toggleEnabledMutation = useSetEnabledApiKey();

  // 构建 usageMap
  const usageMap = useMemo(() => {
    const map = new Map<number, ApiKeyUsage>();
    usageData?.forEach((usage) => {
      map.set(usage.apiKeyId, usage);
    });
    return map;
  }, [usageData]);

  const apiKeys = data?.items || [];

  const handleAdd = () => {
    setEditingApiKey(null);
    setNewKey(null);
    setModalOpen(true);
  };

  const handleEdit = (record: ApiKey) => {
    setEditingApiKey(record);
    setNewKey(null);
    setModalOpen(true);
  };

  const handleDelete = (id: number) => {
    confirm({
      type: 'danger',
      entityName: 'API Key',
      onConfirm: async () => {
        await deleteMutation.mutateAsync(id);
      },
    });
  };

  const handleToggleEnabled = (id: number, enabled: boolean) => {
    toggleEnabledMutation.mutate({ id, enabled });
  };

  const handleSubmit = async (values: FormValues) => {
    if (editingApiKey) {
      await updateMutation.mutateAsync({ id: editingApiKey.id, data: values });
    } else {
      const result = await createMutation.mutateAsync({
        name: values.name,
        userId: 0, // 后端自动填充当前用户
        expiresAt: values.expiresAt,
      });
      setNewKey(result.rawKey);
    }
    setModalOpen(false);
  };

  return (
    <Card title={t('title', { defaultValue: 'API Key 管理' })}>
      {/* 操作栏 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          {t('add', { defaultValue: '创建 API Key' })}
        </Button>
      </div>

      {/* 卡片网格 */}
      {apiKeys.length === 0 ? (
        <Empty description={t('empty.noApiKey', { defaultValue: '暂无 API Key' })} />
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
            gap: 16,
          }}
        >
          {apiKeys.map((key) => (
            <ApiKeyCard
              key={key.id}
              apiKey={key}
              usage={usageMap.get(key.id)}
              onEdit={handleEdit}
              onDelete={handleDelete}
              onToggleEnabled={handleToggleEnabled}
            />
          ))}
        </div>
      )}

      {/* 表单弹窗 */}
      <ApiKeyFormModal
        open={modalOpen}
        editingApiKey={editingApiKey}
        newKey={newKey}
        isAdmin={false}
        onSubmit={handleSubmit}
        onCancel={() => setModalOpen(false)}
        loading={createMutation.isPending || updateMutation.isPending}
      />
    </Card>
  );
}
