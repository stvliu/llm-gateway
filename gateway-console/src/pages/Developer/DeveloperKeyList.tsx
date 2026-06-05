import { useCallback, useMemo } from 'react';
import { Table, Tag, Button, Popconfirm, App } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUserApiKeys, useDeleteUserApiKey } from '@/services/query/useUserApiKeys';
import { useAuthStore } from '@/stores/authStore';
import { userApiKeyApi } from '@/services/api/userApiKey';
import { MaskedKeyDisplay } from '@/components/MaskedKeyDisplay';
import type { UserApiKey } from '@/types/team';

const statusColors: Record<string, string> = {
  ACTIVE: 'green',
  INACTIVE: 'default',
  DEGRADED: 'orange',
  EXPIRED: 'red',
};

export default function DeveloperKeyList() {
  const { t } = useTranslation('developer');
  const { message } = App.useApp();
  const currentUser = useAuthStore((s) => s.user);
  const userId = currentUser?.id ?? 0;

  const { data: keys, isLoading } = useUserApiKeys(userId);
  const deleteMutation = useDeleteUserApiKey(userId);

  const handleRevoke = useCallback(async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success(t('keyRevoked'));
    } catch {
      message.error(t('keyRevokeFailed'));
    }
  }, [deleteMutation, message, t]);

  const columns = useMemo(() => [
    {
      title: t('keyPrefix'),
      dataIndex: 'keyPlain',
      key: 'keyPlain',
      render: (_: string, record: UserApiKey) => (
        <MaskedKeyDisplay
          keyPlain={record.keyPlain}
          mode="readonly"
          size="small"
        />
      ),
    },
    {
      title: t('keyName'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('keyStatus'),
      dataIndex: 'state',
      key: 'state',
      width: 80,
      render: (state: string) => {
        const statusKeyMap: Record<string, string> = {
          ACTIVE: 'status.active',
          INACTIVE: 'status.inactive',
          DEGRADED: 'status.degraded',
          EXPIRED: 'status.expired',
        };
        const color = statusColors[state] || 'default';
        const label = statusKeyMap[state] ? t(statusKeyMap[state]) : state;
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: t('keyCreatedAt'),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (val: string) => val ? new Date(val).toLocaleString('zh-CN') : '-',
    },
    {
      title: t('keyActions'),
      key: 'actions',
      width: 80,
      render: (_: unknown, record: UserApiKey) => (
        <Popconfirm
          title={t('confirmRevoke')}
          onConfirm={() => handleRevoke(record.id)}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ], [t, handleRevoke]);

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
