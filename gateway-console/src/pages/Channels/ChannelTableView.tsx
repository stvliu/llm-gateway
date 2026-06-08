import { useMemo } from 'react';
import { Table, Tag, Typography, Button, theme, App, Space, Tooltip } from 'antd';
import {
  PauseOutlined,
  PlayCircleOutlined,
  ThunderboltOutlined,
  DeleteOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { ChannelCard } from '@/types/channel';
import type { Provider } from '@/types/provider';
import type { FC } from 'react';

const { Text } = Typography;

interface ChannelTableViewProps {
  channels: ChannelCard[];
  providers: Provider[];
  onChannelClick: (channelId: number) => void;
  onToggleState: (id: number, enabled: boolean) => void;
  onDelete: (id: number) => void;
  onTest: (channel: ChannelCard) => void;
}

/**
 * 渠道列表视图
 * 紧凑表格模式，列为：供应商标签 / 渠道名 / 计费模式 / 优先级 / 端点数 / Key数 / 模型数 / 状态 / 操作
 */
export const ChannelTableView: FC<ChannelTableViewProps> = ({
  channels,
  providers,
  onChannelClick,
  onToggleState,
  onDelete,
  onTest,
}) => {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const { modal } = App.useApp();
  const providerMap = useMemo(() => {
    return new Map(providers.map((p) => [p.id, p]));
  }, [providers]);

  const handleToggleClick = (e: React.MouseEvent, record: ChannelCard) => {
    e.stopPropagation();
    const isActive = record.state === 'ACTIVE';
    modal.confirm({
      title: isActive ? t('card.confirmDisable') : t('card.confirmEnable'),
      onOk: () => onToggleState(record.id, !isActive),
    });
  };

  const handleDeleteClick = (record: ChannelCard) => {
    modal.confirm({
      title: t('card.deleteConfirmTitle'),
      content: t('card.deleteConfirmContent', { name: record.name }),
      okType: 'danger',
      onOk: () => onDelete(record.id),
    });
  };

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
          {r.state === 'ACTIVE' ? t('status.active') : t('status.inactive')}
        </Tag>
      ),
    },
    {
      title: t('table.actions'),
      key: 'actions',
      width: 120,
      render: (_: unknown, r: ChannelCard) => {
        const isActive = r.state === 'ACTIVE';
        return (
          <Space size={0} onClick={(e) => e.stopPropagation()}>
            <Tooltip title={isActive ? t('card.disable') : t('card.enable')}>
              <Button
                type="text"
                size="small"
                icon={isActive ? <PauseOutlined /> : <PlayCircleOutlined />}
                onClick={(e) => handleToggleClick(e, r)}
              />
            </Tooltip>
            <Tooltip title={isActive ? t('card.testConnect') : t('card.testDisabled')}>
              <Button
                type="text"
                size="small"
                icon={<ThunderboltOutlined />}
                disabled={!isActive}
                onClick={() => onTest(r)}
                style={{ opacity: isActive ? 1 : 0.4 }}
              />
            </Tooltip>
            <Tooltip title={t('card.viewDetail')}>
              <Button
                type="text"
                size="small"
                icon={<EyeOutlined />}
                onClick={() => onChannelClick(r.id)}
              />
            </Tooltip>
            <Tooltip title={t('card.delete')}>
              <Button
                type="text"
                size="small"
                icon={<DeleteOutlined />}
                danger
                onClick={() => handleDeleteClick(r)}
              />
            </Tooltip>
          </Space>
        );
      },
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
