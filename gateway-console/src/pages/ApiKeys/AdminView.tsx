import { useState, useMemo } from 'react';
import { Card, Button, Input, Row, Col, Empty } from 'antd';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useConfirm } from '@/hooks/useConfirm';
import {
  useApiKeys,
  useCreateApiKey,
  useUpdateApiKey,
  useDeleteApiKey,
  useSetEnabledApiKey,
  useApiKeyUsageBatch,
  useUsers,
} from '@/services/query';
import { ApiKeyCard } from './ApiKeyCard';
import { ApiKeyFormModal, type FormValues } from './ApiKeyFormModal';
import type { ApiKey, ApiKeyUsage } from '@/types/apiKey';

/**
 * 管理员视角 API Key 管理页面
 */
export default function AdminApiKeysView() {
  const { t } = useTranslation('apiKeys');
  const { confirm } = useConfirm();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<ApiKey | null>(null);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [searchKeyword, setSearchKeyword] = useState('');

  const { data } = useApiKeys();
  const { data: usageData } = useApiKeyUsageBatch();
  const { data: usersData } = useUsers({ size: 100 });

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
  const users = usersData?.items || [];

  // 前端过滤搜索
  const filteredApiKeys = useMemo(() => {
    if (!searchKeyword) return apiKeys;
    const keyword = searchKeyword.toLowerCase();
    return apiKeys.filter(
      (item) =>
        item.name.toLowerCase().includes(keyword) ||
        item.username.toLowerCase().includes(keyword)
    );
  }, [apiKeys, searchKeyword]);

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
        userId: values.userId!,
        expiresAt: values.expiresAt,
      });
      setNewKey(result.rawKey);
    }
    setModalOpen(false);
  };

  return (
    <Card title={t('title', { defaultValue: 'API Key 管理' })}>
      {/* 搜索和操作栏 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Input
            placeholder={t('searchPlaceholder', { defaultValue: '搜索名称或用户' })}
            prefix={<SearchOutlined />}
            allowClear
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
        </Col>
        <Col span={16} style={{ textAlign: 'right' }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('add', { defaultValue: '创建 API Key' })}
          </Button>
        </Col>
      </Row>

      {/* 卡片网格 */}
      {filteredApiKeys.length === 0 ? (
        <Empty description={t('empty.noApiKey', { defaultValue: '暂无 API Key' })} />
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
            gap: 16,
          }}
        >
          {filteredApiKeys.map((key) => (
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
        isAdmin={true}
        users={users.map((u) => ({ id: u.id, username: u.username }))}
        onSubmit={handleSubmit}
        onCancel={() => setModalOpen(false)}
        loading={createMutation.isPending || updateMutation.isPending}
      />
    </Card>
  );
}
