import { useCallback, useMemo } from 'react';
import { Table, Tag, Button, Popconfirm, App, Typography, Modal } from 'antd';
import { DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUserApiKeys, useDeleteUserApiKey, useRotateUserApiKey } from '@/services/query/useUserApiKeys';
import { useAuthStore } from '@/stores/authStore';
import { userApiKeyApi } from '@/services/api/userApiKey';
import { MaskedKeyDisplay } from '@/components/MaskedKeyDisplay';
import type { UserApiKey } from '@/types/team';

const { Text } = Typography;

const statusConfig: Record<string, { label: string; color: string }> = {
  ACTIVE: { label: '活跃', color: 'green' },
  INACTIVE: { label: '未激活', color: 'default' },
  DEGRADED: { label: '已降级', color: 'orange' },
  EXPIRED: { label: '过期', color: 'red' },
};

export default function DeveloperKeyList() {
  const { t } = useTranslation('developer');
  const { message } = App.useApp();
  const currentUser = useAuthStore((s) => s.user);
  const userId = currentUser?.id ?? 0;

  const { data: keys, isLoading } = useUserApiKeys(userId);
  const deleteMutation = useDeleteUserApiKey(userId);
  const rotateMutation = useRotateUserApiKey();

  const handleRevoke = useCallback(async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success(t('keyRevoked', { defaultValue: 'Key 已吊销' }));
    } catch {
      message.error(t('keyRevokeFailed', { defaultValue: '吊销失败' }));
    }
  }, [deleteMutation, message, t]);

  const handleRotate = useCallback(async (id: number) => {
    try {
      const result = await rotateMutation.mutateAsync(id);
      message.success(t('keyRotated', { defaultValue: 'Key 已轮换，新 Key 已生成' }));
      Modal.info({
        title: t('newKey', { defaultValue: '新 API Key' }),
        content: (
          <div>
            <Text code style={{ wordBreak: 'break-all' }}>{result.keyPlain}</Text>
            <div style={{ marginTop: 8, color: '#64748b', fontSize: 12 }}>
              请立即复制新 Key，关闭后不再显示。
            </div>
          </div>
        ),
      });
    } catch {
      message.error(t('keyRotateFailed', { defaultValue: '轮换失败' }));
    }
  }, [rotateMutation, message, t]);

  const columns = useMemo(() => [
    {
      title: t('keyPrefix', { defaultValue: 'Key 前缀' }),
      dataIndex: 'keyMasked',
      key: 'keyMasked',
      render: (keyMasked: string, record: UserApiKey) => (
        <MaskedKeyDisplay
          keyMasked={keyMasked || record.keyPrefix}
          mode="readonly"
          size="small"
          onFetchPlain={async () => {
            const detail = await userApiKeyApi.getDetail(record.id);
            return detail.keyPlain;
          }}
        />
      ),
    },
    {
      title: t('keyName', { defaultValue: '名称' }),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('keyStatus', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 80,
      render: (state: string) => {
        const cfg = statusConfig[state] || { label: state, color: 'default' };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: t('keyCreated', { defaultValue: '创建时间' }),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (val: string) => val ? new Date(val).toLocaleString('zh-CN') : '-',
    },
    {
      title: t('keyActions', { defaultValue: '操作' }),
      key: 'actions',
      width: 120,
      render: (_: unknown, record: UserApiKey) => (
        <div style={{ display: 'flex', gap: 4 }}>
          <Button type="link" size="small" icon={<ReloadOutlined />} onClick={() => handleRotate(record.id)}>
            {t('rotate', { defaultValue: '轮换' })}
          </Button>
          <Popconfirm
            title={t('confirmRevoke', { defaultValue: '确定吊销此 Key？' })}
            onConfirm={() => handleRevoke(record.id)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </div>
      ),
    },
  ], [t, handleRotate, handleRevoke]);

  return (
    <Table
      dataSource={keys ?? []}
      columns={columns}
      rowKey="id"
      size="small"
      loading={isLoading}
      pagination={false}
      locale={{ emptyText: t('noKeys', { defaultValue: '暂无 API Key' }) }}
    />
  );
}
