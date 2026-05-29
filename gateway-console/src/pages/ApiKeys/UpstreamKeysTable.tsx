import { useState, useMemo } from 'react';
import { Table, Tag, Space, Button, Popconfirm, App, Typography, Spin, Input } from 'antd';
import { DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviders } from '@/services/query/useProviders';
import { useChannels, useChannelCredentials, useDeleteChannelCredential } from '@/services/query/useChannels';
import { ProviderIcon } from '@/components/ui';
import type { Provider } from '@/types/provider';

const { Text } = Typography;

/** 上游凭证的展示状态映射（色盲友好） */
const statusConfig: Record<string, { label: string; color: string; bg: string; icon: string }> = {
  ACTIVE: { label: '活跃', color: '#16a34a', bg: '#dcfce7', icon: '✅' },
  INACTIVE: { label: '未激活', color: '#64748b', bg: '#f1f5f9', icon: '⏸️' },
};

/** 聚合后的上游凭证行数据 */
interface AggregateCredential {
  key: string;
  credentialId: number;
  channelId: number;
  providerId: number;
  apiKeyPrefix: string;
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

  const [search, setSearch] = useState('');

  // PageResponse<Provider> → Provider[]
  const providers: Provider[] = providersPage?.items ?? [];

  // 取第一个供应商加载通道（简化版，生产环境应聚合所有供应商）
  const firstProviderId = providers[0]?.id ?? 0;
  const { data: channels, isLoading: channelsLoading } = useChannels(firstProviderId);

  // 取第一个通道的凭证（简化版）
  const firstChannelId = channels?.[0]?.id ?? 0;
  const { data: credentials, isLoading: credentialsLoading } = useChannelCredentials(firstChannelId);

  /** 聚合所有级别数据为平面行 */
  const allCredentials: AggregateCredential[] = useMemo(() => {
    if (providers.length === 0) return [];
    // 没有通道时，按供应商显示占位行
    if (!channels?.length) {
      return providers.map((p) => ({
        key: `placeholder-${p.id}`,
        credentialId: 0,
        channelId: 0,
        providerId: p.id,
        apiKeyPrefix: '-',
        providerCode: p.providerId,
        providerName: p.providerName,
        channelName: `${p.providerName} ${t('channel', { defaultValue: '通道' })}`,
        state: '',
        priority: 0,
        weight: 0,
      }));
    }
    // 有通道 + 凭证时做关联
    const rows: AggregateCredential[] = [];
    channels.forEach((ch) => {
      const provider = providers.find((p) => p.id === ch.providerId);
      const creds = credentials?.filter((c) => c.channelId === ch.id) ?? [];
      if (creds.length === 0) {
        // 通道无凭证，显示空行
        rows.push({
          key: `no-cred-${ch.id}`,
          credentialId: 0,
          channelId: ch.id,
          providerId: ch.providerId,
          apiKeyPrefix: '-',
          providerCode: provider?.providerId,
          providerName: provider?.providerName ?? String(ch.providerId),
          channelName: ch.name,
          state: '',
          priority: 0,
          weight: 0,
        });
      } else {
        creds.forEach((cr) => {
          rows.push({
            key: `cred-${cr.id}`,
            credentialId: cr.id,
            channelId: cr.channelId,
            providerId: ch.providerId,
            apiKeyPrefix: cr.apiKeyPrefix,
            providerCode: provider?.providerId,
            providerName: provider?.providerName ?? String(ch.providerId),
            channelName: ch.name,
            state: cr.state,
            priority: cr.priority,
            weight: cr.weight,
          });
        });
      }
    });
    return rows;
  }, [providers, channels, credentials, t]);

  /** 搜索过滤 */
  const filtered = useMemo(() => {
    if (!search) return allCredentials;
    const q = search.toLowerCase();
    return allCredentials.filter(
      (r) =>
        r.apiKeyPrefix.toLowerCase().includes(q) ||
        r.providerName.toLowerCase().includes(q) ||
        r.channelName.toLowerCase().includes(q)
    );
  }, [allCredentials, search]);

  const handleDelete = async (credentialId: number, channelId: number) => {
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
  };

  const columns = [
    {
      title: t('keyPrefix', { defaultValue: 'Key 前缀' }),
      dataIndex: 'apiKeyPrefix',
      key: 'apiKeyPrefix',
      width: 180,
      render: (prefix: string) => (
        <Text code style={{ fontSize: 12 }}>
          {prefix || '-'}
          <Button type="link" size="small" icon={<EyeOutlined />} style={{ marginLeft: 4 }} />
        </Text>
      ),
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
        const cfg = statusConfig[state] || { label: state, color: '#64748b', bg: '#f1f5f9', icon: '❓' };
        return (
          <Tag style={{ background: cfg.bg, color: cfg.color, border: 'none', borderRadius: 4, padding: '2px 8px' }}>
            {cfg.icon} {cfg.label}
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
  ];

  if (providersLoading || channelsLoading || credentialsLoading) {
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
    </div>
  );
}