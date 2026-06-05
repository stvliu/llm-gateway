import { useMemo } from 'react';
import { Table, Tag, Typography, theme } from 'antd';
import { useTranslation } from 'react-i18next';
import type { ChannelCard } from '@/types/channel';
import type { Provider } from '@/types/provider';
import type { FC } from 'react';

const { Text } = Typography;

interface ChannelTableViewProps {
  channels: ChannelCard[];
  providers: Provider[];
  onChannelClick: (channelId: number) => void;
}

/**
 * 渠道列表视图
 * 紧凑表格模式，列为：供应商标签 / 渠道名 / 计费模式 / 优先级 / 端点数 / Key数 / 模型数 / 状态 / 操作
 */
export const ChannelTableView: FC<ChannelTableViewProps> = ({
  channels,
  providers,
  onChannelClick,
}) => {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const providerMap = useMemo(() => {
    return new Map(providers.map((p) => [p.id, p]));
  }, [providers]);

  const getBillingModeLabel = (mode: string) => {
    const labels: Record<string, string> = {
      pay_as_you_go: t('billing.payAsYouGo'),
      subscription: t('billing.subscription'),
      package: t('billing.package'),
    };
    return labels[mode] || t('billing.default', { mode });
  };

  const columns = [
    {
      title: t('table.provider'),
      dataIndex: 'providerId',
      width: 100,
      render: (id: number) => {
        const p = providerMap.get(id);
        return <Tag color="blue">{p?.providerName || '-'}</Tag>;
      },
    },
    {
      title: t('table.channelName'),
      dataIndex: 'name',
      render: (name: string, record: ChannelCard) => (
        <Typography.Link onClick={() => onChannelClick(record.id)}>
          {name}
        </Typography.Link>
      ),
    },
    {
      title: t('table.billingMode'),
      dataIndex: 'billingMode',
      width: 100,
      render: (mode: string) => getBillingModeLabel(mode),
    },
    {
      title: t('table.priority'),
      dataIndex: 'priority',
      width: 70,
      sorter: (a: ChannelCard, b: ChannelCard) => a.priority - b.priority,
    },
    {
      title: t('table.endpoints'),
      key: 'endpoints',
      width: 60,
      render: (_: unknown, r: ChannelCard) => r.stats.endpointCount,
    },
    {
      title: t('table.keys'),
      key: 'credentials',
      width: 60,
      render: (_: unknown, r: ChannelCard) => (
        <Text style={{ color: r.stats.credentialCount === 0 ? token.colorWarning : undefined }}>
          {r.stats.credentialCount}
        </Text>
      ),
    },
    {
      title: t('table.models'),
      key: 'models',
      width: 60,
      render: (_: unknown, r: ChannelCard) => r.stats.modelCount,
    },
    {
      title: t('table.status'),
      key: 'status',
      width: 80,
      render: (_: unknown, r: ChannelCard) => (
        <Tag color={r.state === 'ACTIVE' ? 'green' : 'orange'}>
          {r.state === 'ACTIVE' ? t('status.running') : t('status.stopped')}
        </Tag>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={channels}
      size="small"
      pagination={false}
    />
  );
};
