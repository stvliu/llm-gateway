import { useState, useMemo, useCallback } from 'react';
import { Table, Tag, Space, Button, Popconfirm, App, Typography, Spin, Input } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviders } from '@/services/query/useProviders';
import { useChannelsBatch, useChannelCredentialsBatch, useDeleteChannelCredential, useUpdateChannelCredential } from '@/services/query/useChannels';
import { ProviderIcon } from '@/components/ui';
import { MaskedKeyDisplay } from '@/components/MaskedKeyDisplay';
import { ApiKeyEditModal } from '@/pages/Channels/ApiKeyEditModal';
import type { Provider } from '@/types/provider';

const { Text } = Typography;

/** 上游凭证的展示状态映射（色盲友好） */
const statusConfig: Record<string, { label: string; color: string; bg: string }> = {
  ACTIVE: { label: '活跃', color: '#16a34a', bg: '#dcfce7' },
  INACTIVE: { label: '未激活', color: '#64748b', bg: '#f1f5f9' },
};

/** 聚合后的上游凭证行数据 */
interface AggregateCredential {
  key: string;
  credentialId: number;
  channelId: number;
  providerId: number;
  apiKeyPrefix: string;
  apiKeyPlain?: string;
  providerCode?: string;
  providerName: string;
  channelName: string;
  state: string;
  priority: number;
  weight: number;
}

export default function UpstreamKeysTable() {
  const { t } = useTranslation('apiKeys');
  const { message } = App.useApp();
  const { data: providersPage, isLoading: providersLoading } = useProviders();
  const deleteMutation = useDeleteChannelCredential();
  const updateCredential = useUpdateChannelCredential();

  const [search, setSearch] = useState('');
  const [editingCredential, setEditingCredential] = useState<AggregateCredential | null>(null);

  const providers: Provider[] = providersPage?.items ?? [];

  // 获取所有供应商的通道
  const channelQueries = useChannelsBatch(providers.map((p) => p.id));

  // 收集所有有通道的 providerId → channelId 映射
  const allChannelEntries = useMemo(() => {
    const entries: { providerId: number; channelId: number; channelName: string }[] = [];
    providers.forEach((provider, idx) => {
      const channels = channelQueries[idx]?.data;
      channels?.forEach((ch) => {
        entries.push({ providerId: provider.id, channelId: ch.id, channelName: ch.name });
      });
    });
    return entries;
  }, [providers, channelQueries]);

  // 获取所有通道的凭证
  const credentialQueries = useChannelCredentialsBatch(allChannelEntries.map((e) => e.channelId));

  // 聚合所有数据
  const allCredentials: AggregateCredential[] = useMemo(() => {
    if (providers.length === 0) return [];

    const rows: AggregateCredential[] = [];

    providers.forEach((provider, providerIdx) => {
      const providerChannels = channelQueries[providerIdx]?.data?.filter(
        (ch) => ch.providerId === provider.id
      );

      if (!providerChannels?.length) {
        // 供应商无通道，显示占位行
        rows.push({
          key: `placeholder-${provider.id}`,
          credentialId: 0,
          channelId: 0,
          providerId: provider.id,
          apiKeyPrefix: '-',
          providerCode: provider.providerId,
          providerName: provider.providerName,
          channelName: `${provider.providerName} ${t('channel', { defaultValue: '通道' })}`,
          state: '',
          priority: 0,
          weight: 0,
        });
        return;
      }

      providerChannels.forEach((ch) => {
        const entryIdx = allChannelEntries.findIndex((e) => e.channelId === ch.id);
        const creds = entryIdx >= 0 ? credentialQueries[entryIdx]?.data : undefined;
        const credList = creds?.filter((c) => c.channelId === ch.id) ?? [];

        if (credList.length === 0) {
          rows.push({
            key: `no-cred-${ch.id}`,
            credentialId: 0,
            channelId: ch.id,
            providerId: ch.providerId,
            apiKeyPrefix: '-',
            apiKeyPlain: '-',
            providerCode: provider.providerId,
            providerName: provider.providerName,
            channelName: ch.name,
            state: '',
            priority: 0,
            weight: 0,
          });
        } else {
          credList.forEach((cr) => {
            rows.push({
              key: `cred-${cr.id}`,
              credentialId: cr.id,
              channelId: cr.channelId,
              providerId: ch.providerId,
              apiKeyPrefix: cr.apiKeyPrefix,
              apiKeyPlain: cr.apiKeyPlain,
              providerCode: provider.providerId,
              providerName: provider.providerName,
              channelName: ch.name,
              state: cr.state,
              priority: cr.priority,
              weight: cr.weight,
            });
          });
        }
      });
    });

    return rows;
  }, [providers, channelQueries, credentialQueries, allChannelEntries, t]);

  const isLoading = providersLoading || channelQueries.some((q) => q.isLoading) || credentialQueries.some((q) => q.isLoading);

  /** 搜索过滤 */
  const filtered = useMemo(() => {
    if (!search) return allCredentials;
    const q = search.toLowerCase();
    return allCredentials.filter(
      (r) =>
        (r.apiKeyPrefix ?? '').toLowerCase().includes(q) ||
        (r.providerName ?? '').toLowerCase().includes(q) ||
        (r.channelName ?? '').toLowerCase().includes(q)
    );
  }, [allCredentials, search]);

  const handleDelete = useCallback(async (credentialId: number, channelId: number) => {
    if (!credentialId || !channelId) {
      message.warning(t('noDeleteForPlaceholder', { defaultValue: '示例数据不可删除' }));
      return;
    }
    try {
      await deleteMutation.mutateAsync({ channelId, id: credentialId });
      message.success(t('deleted', { defaultValue: '凭证已删除' }));
    } catch {
      message.error(t('deleteFailed', { defaultValue: '删除失败' }));
    }
  }, [deleteMutation, message, t]);

  const columns = useMemo(() => [
    {
      title: t('keyPrefix', { defaultValue: 'Key' }),
      dataIndex: 'apiKeyPlain',
      key: 'apiKeyPlain',
      width: 220,
      render: (_: string, record: AggregateCredential) => {
        if (!record.credentialId || !record.apiKeyPlain || record.apiKeyPlain === '-') {
          return <Text type="secondary">-</Text>;
        }
        return (
          <MaskedKeyDisplay
            keyPlain={record.apiKeyPlain}
            mode="editable"
            size="small"
            onEdit={() => setEditingCredential(record)}
          />
        );
      },
    },
    {
      title: t('provider', { defaultValue: '供应商' }),
      key: 'provider',
      width: 180,
      render: (_: unknown, record: AggregateCredential) => (
        <Space>
          <ProviderIcon providerId={record.providerCode} size={20} />
          <Text>{record.providerName}</Text>
        </Space>
      ),
    },
    {
      title: t('channel', { defaultValue: '通道' }),
      dataIndex: 'channelName',
      key: 'channelName',
      width: 160,
    },
    {
      title: t('status', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (state: string) => {
        if (!state) return <Text type="secondary">-</Text>;
        const cfg = statusConfig[state] || { label: state, color: '#64748b', bg: '#f1f5f9' };
        return (
          <Tag style={{ background: cfg.bg, color: cfg.color, border: 'none', borderRadius: 4, padding: '2px 8px' }}>
            {cfg.label}
          </Tag>
        );
      },
    },
    {
      title: t('priority', { defaultValue: '优先级/权重' }),
      key: 'priorityWeight',
      width: 120,
      render: (_: unknown, record: AggregateCredential) =>
        record.priority ? (
          <Text>
            P{record.priority} · {record.weight}%
          </Text>
        ) : (
          <Text type="secondary">-</Text>
        ),
    },
    {
      title: t('actions', { defaultValue: '操作' }),
      key: 'actions',
      width: 80,
      render: (_: unknown, record: AggregateCredential) => (
        <Popconfirm
          title={t('confirmDelete', { defaultValue: '确定删除此凭证？' })}
          onConfirm={() => handleDelete(record.credentialId, record.channelId)}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ], [t, handleDelete, setEditingCredential]);

  if (isLoading) {
    return <Spin />;
  }

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder={t('searchKeys', { defaultValue: '搜索 Key 前缀/供应商...' })}
          style={{ width: 320 }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
        />
      </div>
      <Table
        dataSource={filtered}
        columns={columns}
        rowKey="key"
        size="middle"
        pagination={{ pageSize: 15, showSizeChanger: true }}
        locale={{ emptyText: t('noUpstreamKeys', { defaultValue: '暂无上游 Key，请先配置供应商' }) }}
      />

      {/* API Key 编辑弹窗 */}
      {editingCredential && (
        <ApiKeyEditModal
          open={true}
          channelId={editingCredential.channelId}
          credentialId={editingCredential.credentialId}
          keyPlain={editingCredential.apiKeyPlain || ''}
          onClose={() => setEditingCredential(null)}
          onSuccess={() => {
            // 刷新凭证列表（通过 refetch）
            message.success('API Key 已更新');
          }}
          onUpdate={async (channelId, credentialId, data) => {
            await updateCredential.mutateAsync({ channelId, id: credentialId, data });
          }}
        />
      )}
    </div>
  );
}