import { useMemo } from 'react';
import { Table, Typography, Button, theme, App, Space, Tooltip, Dropdown } from 'antd';
import {
  ThunderboltOutlined,
  DeleteOutlined,
  EyeOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import ChannelStateTag from '@/components/common/ChannelStateTag';
import { getAvailableTransitions, getTransitionActionLabel } from '@/utils/stateTransitions';
import { useDangerConfirm } from '@/components/common/useDangerConfirm';
import type { ChannelCard, ChannelState } from '@/types/channel';
import type { Provider } from '@/types/provider';
import type { FC } from 'react';

interface ChannelTableViewProps {
  channels: ChannelCard[];
  providers: Provider[];
  onChannelClick: (channelId: number) => void;
  onToggleState: (id: number, enabled: boolean) => void;
  onDelete: (id: number) => void;
  onTest: (channel: ChannelCard) => void;
  onStateTransition?: (id: number, targetState: string, reason?: string) => void;
}

/**
 * 渠道列表视图
 * Status 列使用 ChannelStateTag，Actions 列改为上下文操作按钮
 */
export const ChannelTableView: FC<ChannelTableViewProps> = ({
  channels,
  providers,
  onChannelClick,
  onToggleState,
  onDelete,
  onTest,
  onStateTransition,
}) => {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const { modal } = App.useApp();
  // 删除整个渠道（任务 8.7）：使用 useDangerConfirm 与其他危险操作对齐
  const { confirm: confirmDeleteChannel, contextHolder: dangerContextHolder } =
    useDangerConfirm();
  const providerMap = useMemo(() => {
    return new Map(providers.map((p) => [p.id, p]));
  }, [providers]);

  const handleTransition = (record: ChannelCard, targetState: ChannelState) => {
    const currentState = record.state as ChannelState;
    const actionLabel = getTransitionActionLabel(currentState, targetState);

    if (targetState === 'DEPRECATED' || targetState === 'RETIRED') {
      let title = actionLabel;
      let content = t('card.confirmDeprecateContent', '确定要将此渠道标记为下线？');
      if (targetState === 'RETIRED') {
        title = t('channel.action.retire.confirmTitle', '停用渠道？');
        content = t(
          'channel.action.retire.confirmDescription',
          '停用后该渠道不再参与任何流量分配，且无法恢复，已建立的指标历史保留'
        );
      }
      modal.confirm({
        title,
        content,
        okType: 'danger',
        onOk: () => onStateTransition?.(record.id, targetState, ''),
      });
      return;
    }

    // 暂停操作（→ SUSPENDED）：轻量二次确认
    if (targetState === 'SUSPENDED') {
      modal.confirm({
        title: t('channel.action.suspend.confirmTitle', '暂停渠道？'),
        content: t(
          'channel.action.suspend.confirmDescription',
          '暂停后该渠道不再分配流量，但保留配置'
        ),
        okType: 'default',
        onOk: () => onStateTransition?.(record.id, targetState, ''),
      });
      return;
    }

    onStateTransition?.(record.id, targetState, '');
  };

  /** 删除整个渠道（任务 8.7）：使用 useDangerConfirm 与 RETIRED 文案对齐 */
  const handleDeleteClick = (record: ChannelCard) => {
    confirmDeleteChannel({
      titleKey: 'channel.deleteDangerTitle',
      descriptionKey: 'channel.deleteDangerDescription',
      descriptionParams: { name: record.name },
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
        return <Typography.Text ellipsis style={{ maxWidth: 100 }}>{p?.providerName || '-'}</Typography.Text>;
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
        <Typography.Text style={{ color: r.stats.credentialCount === 0 ? token.colorWarning : undefined }}>
          {r.stats.credentialCount}
        </Typography.Text>
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
      width: 100,
      render: (_: unknown, r: ChannelCard) => (
        <ChannelStateTag state={r.state as ChannelState} />
      ),
    },
    {
      title: t('table.actions'),
      key: 'actions',
      width: 120,
      render: (_: unknown, r: ChannelCard) => {
        const currentState = r.state as ChannelState;
        const transitions = getAvailableTransitions(currentState);
        const isRoutable = currentState === 'ACTIVE' || currentState === 'DEPRECATED';

        return (
          <Space size={0} onClick={(e) => e.stopPropagation()}>
            <Tooltip title={isRoutable ? t('card.testConnect') : t('card.testDisabled')}>
              <Button
                type="text"
                size="small"
                icon={<ThunderboltOutlined />}
                disabled={!isRoutable}
                onClick={() => onTest(r)}
                style={{ opacity: isRoutable ? 1 : 0.4 }}
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
            {transitions.length > 0 && (
              <Dropdown
                menu={{
                  items: transitions.map((target) => ({
                    key: target,
                    label: getTransitionActionLabel(currentState, target),
                    danger: target === 'RETIRED',
                  })),
                  onClick: ({ key }) => handleTransition(r, key as ChannelState),
                }}
                trigger={['click']}
              >
                <Button type="text" size="small" icon={<MoreOutlined />} />
              </Dropdown>
            )}
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
    <>
      {/* useDangerConfirm 的 contextHolder 必须挂载到组件树，否则 modal 不出现 */}
      {dangerContextHolder}
      <Table
        rowKey="id"
        columns={columns}
        dataSource={channels}
        size="small"
        pagination={false}
      />
    </>
  );
};
