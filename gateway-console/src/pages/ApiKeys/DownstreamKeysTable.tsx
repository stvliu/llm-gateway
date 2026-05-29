import { useState } from 'react';
import { Table, Tag, Button, Popconfirm, App, Input, Typography } from 'antd';
import { DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUserApiKeys, useDeleteUserApiKey } from '@/services/query/useUserApiKeys';
import { useAuthStore } from '@/stores/authStore';
import type { UserApiKey } from '@/types/team';

const { Text } = Typography;

/** 下游 Key 状态映射（色盲友好） */
const statusConfig: Record<string, { label: string; color: string; bg: string; icon: string }> = {
  ACTIVE: { label: '活跃', color: '#16a34a', bg: '#dcfce7', icon: '✅' },
  INACTIVE: { label: '未激活', color: '#64748b', bg: '#f1f5f9', icon: '⏸️' },
};

export default function DownstreamKeysTable() {
  const { t } = useTranslation('apiKeys');
  const { message } = App.useApp();
  const currentUser = useAuthStore((s) => s.user);
  const userId = currentUser?.id ?? 0;

  const { data: keys, isLoading } = useUserApiKeys(userId);
  const deleteMutation = useDeleteUserApiKey(userId);
  const [search, setSearch] = useState('');

  const filtered = (keys ?? []).filter((k: UserApiKey) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return k.keyPrefix?.toLowerCase().includes(q) || k.name?.toLowerCase().includes(q);
  });

  const handleRevoke = async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success(t('revoked', { defaultValue: 'Key 已吊销' }));
    } catch {
      message.error(t('revokeFailed', { defaultValue: '吊销失败' }));
    }
  };

  const columns = [
    {
      title: t('keyPrefix', { defaultValue: 'Key 前缀' }),
      dataIndex: 'keyPrefix',
      key: 'keyPrefix',
      width: 200,
      render: (prefix: string) => (
        <Text code style={{ fontSize: 12 }}>
          {prefix}
          <Button type="link" size="small" icon={<EyeOutlined />} style={{ marginLeft: 4 }} />
        </Text>
      ),
    },
    {
      title: t('keyName', { defaultValue: '名称' }),
      dataIndex: 'name',
      key: 'name',
      width: 160,
    },
    {
      title: t('user', { defaultValue: '所属用户' }),
      dataIndex: 'userId',
      key: 'userId',
      width: 100,
    },
    {
      title: t('status', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (state: string) => {
        const cfg = statusConfig[state] || { label: state, color: '#64748b', bg: '#f1f5f9', icon: '❓' };
        return (
          <Tag style={{ background: cfg.bg, color: cfg.color, border: 'none', borderRadius: 4, padding: '2px 8px' }}>
            {cfg.icon} {cfg.label}
          </Tag>
        );
      },
    },
    {
      title: t('createdAt', { defaultValue: '创建时间' }),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val: string) => (val ? new Date(val).toLocaleString('zh-CN') : <Text type="secondary">-</Text>),
    },
    {
      title: t('updatedAt', { defaultValue: '更新时间' }),
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 170,
      render: (val: string) => (val ? new Date(val).toLocaleString('zh-CN') : <Text type="secondary">-</Text>),
    },
    {
      title: t('actions', { defaultValue: '操作' }),
      key: 'actions',
      width: 80,
      render: (_: unknown, record: UserApiKey) => (
        <Popconfirm
          title={t('confirmRevoke', { defaultValue: '确定吊销此 Key？' })}
          onConfirm={() => handleRevoke(record.id)}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder={t('searchKeys', { defaultValue: '搜索 Key 前缀/名称...' })}
          style={{ width: 320 }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
        />
      </div>
      <Table
        dataSource={filtered}
        columns={columns}
        rowKey="id"
        size="middle"
        loading={isLoading}
        pagination={{ pageSize: 15, showSizeChanger: true }}
        locale={{ emptyText: t('noDownstreamKeys', { defaultValue: '暂无下游 Key' }) }}
      />
    </div>
  );
}