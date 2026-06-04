import { useMemo } from 'react';
import { Table, Tag, Typography } from 'antd';
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
  const providerMap = useMemo(() => {
    return new Map(providers.map((p) => [p.id, p]));
  }, [providers]);

  const getBillingModeLabel = (mode: string) => {
    const labels: Record<string, string> = {
      pay_as_you_go: '按量付费',
      subscription: '订阅',
      package: '套餐',
    };
    return labels[mode] || mode;
  };

  const columns = [
    {
      title: '供应商',
      dataIndex: 'providerId',
      width: 100,
      render: (id: number) => {
        const p = providerMap.get(id);
        return <Tag color="blue">{p?.providerName || '-'}</Tag>;
      },
    },
    {
      title: '渠道名称',
      dataIndex: 'name',
      render: (name: string, record: ChannelCard) => (
        <Typography.Link onClick={() => onChannelClick(record.id)}>
          {name}
        </Typography.Link>
      ),
    },
    {
      title: '计费模式',
      dataIndex: 'billingMode',
      width: 100,
      render: (mode: string) => getBillingModeLabel(mode),
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 70,
      sorter: (a: ChannelCard, b: ChannelCard) => a.priority - b.priority,
    },
    {
      title: '端点',
      key: 'endpoints',
      width: 60,
      render: (_: unknown, r: ChannelCard) => r.stats.endpointCount,
    },
    {
      title: 'Key',
      key: 'credentials',
      width: 60,
      render: (_: unknown, r: ChannelCard) => (
        <Text style={{ color: r.stats.credentialCount === 0 ? '#fa8c16' : undefined }}>
          {r.stats.credentialCount}
        </Text>
      ),
    },
    {
      title: '模型',
      key: 'models',
      width: 60,
      render: (_: unknown, r: ChannelCard) => r.stats.modelCount,
    },
    {
      title: '状态',
      key: 'status',
      width: 80,
      render: (_: unknown, r: ChannelCard) => (
        <Tag color={r.state === 'ACTIVE' ? 'green' : 'orange'}>
          {r.state === 'ACTIVE' ? '运行中' : '已停用'}
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
