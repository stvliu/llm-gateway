import { useState, useMemo, useCallback } from 'react';
import {
  Card,
  Button,
  Space,
  Tag,
  Empty,
  Spin,
  theme,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  StarFilled,
  ApiOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { StatusIndicator } from '@/components/common';
import { useConfirm } from '@/hooks/useConfirm';
import { ApiKeyModal } from './ApiKeyModal';
import {
  useProviderKeys,
  useDeleteProviderApiKey,
} from '@/services/query';
import type { Provider } from '@/types/provider';
import type { ProviderApiKey } from '@/types/providerApiKey';

interface ProviderApiKeysTabProps {
  provider: Provider | null;
}

/**
 * API Keys 管理标签页
 * 卡片列表展示，支持增删改和设为默认
 */
export function ProviderApiKeysTab({ provider }: ProviderApiKeysTabProps) {
  const { t } = useTranslation('models');
  const { token } = theme.useToken();
  const { confirm } = useConfirm();

  const [apiKeyModalOpen, setApiKeyModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<{
    id: number;
    keyName: string;
    priority: number;
    weight: number;
    isDefault: boolean;
  } | null>(null);

  // 查询数据
  const { data: keysData, isLoading } = useProviderKeys(
    provider?.id || 0,
    { enabled: !!provider }
  );

  // Mutations
  const deleteKeyMutation = useDeleteProviderApiKey();

  const keys = keysData?.keys || [];

  // 按默认和优先级排序
  const sortedKeys = useMemo(() => {
    return [...keys].sort((a, b) => {
      if (a.isDefault && !b.isDefault) return -1;
      if (!a.isDefault && b.isDefault) return 1;
      return (b.priority || 0) - (a.priority || 0);
    });
  }, [keys]);

  // 添加 API Key
  const handleAddKey = useCallback(() => {
    setEditingKey(null);
    setApiKeyModalOpen(true);
  }, []);

  // 编辑 API Key
  const handleEditKey = useCallback((key: ProviderApiKey) => {
    setEditingKey({
      id: key.id,
      keyName: key.keyName,
      priority: key.priority || 100,
      weight: key.weight || 100,
      isDefault: key.isDefault || false,
    });
    setApiKeyModalOpen(true);
  }, []);

  // 删除 API Key
  const handleDeleteKey = useCallback((keyId: number) => {
    confirm({
      type: 'danger',
      onConfirm: () => deleteKeyMutation.mutateAsync(keyId),
    });
  }, [confirm, deleteKeyMutation]);

  // 成功回调
  const handleSuccess = useCallback(() => {
    setApiKeyModalOpen(false);
    setEditingKey(null);
  }, []);

  // 无供应商
  if (!provider) {
    return (
      <Empty
        description={t('provider.selectHint', { defaultValue: '请选择一个供应商' })}
      />
    );
  }

  // 加载中
  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
        <Spin />
      </div>
    );
  }

  return (
    <div>
      {/* 头部 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 16,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <ApiOutlined />
          <span style={{ fontWeight: 600, fontSize: 15 }}>
            {t('provider.apiKeys', { defaultValue: 'API Keys' })}
          </span>
          <Tag color="blue">{keys.length}</Tag>
        </div>
        <Button type="primary" ghost icon={<PlusOutlined />} onClick={handleAddKey}>
          {t('actions.add', { ns: 'common' })}
        </Button>
      </div>

      {/* 卡片列表 */}
      {keys.length === 0 ? (
        <Empty
          description={t('provider.noApiKeys', { defaultValue: '暂无 API Key' })}
        >
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAddKey}>
            {t('actions.add', { ns: 'common' })}
          </Button>
        </Empty>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          {sortedKeys.map((key) => (
            <Card
              key={key.id}
              size="small"
              style={{
                background: token.colorFillAlter,
                borderRadius: 8,
              }}
              styles={{ body: { padding: '12px 16px' } }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                {/* 左侧信息 */}
                <Space>
                  {key.isDefault && (
                    <StarFilled style={{ color: token.colorWarning }} />
                  )}
                  <span style={{ fontWeight: 500 }}>{key.keyName}</span>
                  {key.keyHint && <Tag>{key.keyHint}</Tag>}
                  <StatusIndicator
                    status={key.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'}
                    showLabel={false}
                  />
                </Space>

                {/* 右侧信息 */}
                <Space>
                  <span style={{ fontSize: 12, color: token.colorTextSecondary }}>
                    {t('provider.priority', { defaultValue: '优先级' })}: {key.priority || 100}
                  </span>
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => handleEditKey(key)}
                  />
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => handleDeleteKey(key.id)}
                  />
                </Space>
              </div>
            </Card>
          ))}
        </Space>
      )}

      {/* API Key 弹窗 */}
      <ApiKeyModal
        open={apiKeyModalOpen}
        provider={provider}
        editingKey={editingKey}
        onClose={() => {
          setApiKeyModalOpen(false);
          setEditingKey(null);
        }}
        onSuccess={handleSuccess}
      />
    </div>
  );
}

export type { ProviderApiKeysTabProps };
