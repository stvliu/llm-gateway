/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useMemo } from 'react';
import { Table, Typography, Button, theme, App, Space, Tooltip, Dropdown } from 'antd';
import {
  ThunderboltOutlined,
  EyeOutlined,
  MoreOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import ChannelStateTag from '@/components/common/ChannelStateTag';
import { getTransitionActionLabel } from '@/utils/stateTransitions';
import { getActionBarConfig } from '@/utils/channelActions';
import { useDangerConfirm } from '@/components/common/useDangerConfirm';
import type { ChannelCard, ChannelState } from '@/types/channel';
import type { Provider } from '@/types/provider';
import type { FC } from 'react';

interface ChannelTableViewProps {
  channels: ChannelCard[];
  providers: Provider[];
  onChannelClick: (channelId: number) => void;
  onDelete: (id: number) => void;
  /** 任务 9.1：测试回调可携带"打开抽屉到 credentials Tab + 高亮测试全部"意图 */
  onTest: (channel: ChannelCard, intent?: { tab: 'credentials'; highlightTestAll: boolean }) => void;
  /** 复制回调：打开复制弹窗（预填源渠道配置） */
  onCopy?: (channel: ChannelCard) => void;
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
  onDelete,
  onTest,
  onCopy,
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
    const actionLabel = t(getTransitionActionLabel(currentState, targetState));

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

  /** 构建表格行 Dropdown 菜单项 */
  function buildTableMenuItems(
    state: ChannelState,
    transitions: ChannelState[],
    delDisabled: boolean,
    tr: (key: string) => string,
  ) {
    const items: any[] = transitions.map(target => ({
      key: target,
      label: tr(getTransitionActionLabel(state, target)),
      danger: target === 'RETIRED',
    }));

    items.push({ type: 'divider' as const });

    items.push({
      key: 'delete',
      label: delDisabled
        ? <Tooltip title={tr('channel.action.deleteDisabledWhenActive')}>
            <span style={{ color: 'rgba(0,0,0,0.25)', cursor: 'not-allowed' }}>{tr('card.delete')}</span>
          </Tooltip>
        : tr('card.delete'),
      danger: true,
    });

    return items;
  }

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
        const { primaryAction, dropdownTransitions, deleteDisabled } = getActionBarConfig(currentState);

        return (
          <Space size={0} onClick={(e) => e.stopPropagation()}>
            <Tooltip title={currentState !== 'RETIRED' ? t('card.testConnect') : t('card.testDisabled')}>
              <Button
                type="text"
                size="small"
                icon={<ThunderboltOutlined />}
                disabled={currentState === 'RETIRED'}
                onClick={() => onTest(r, { tab: 'credentials', highlightTestAll: true })}
                style={{ opacity: currentState === 'RETIRED' ? 0.4 : 1 }}
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
            {onCopy && (
              <Tooltip title={t('channel.copy.action', { defaultValue: '复制' })}>
                <Button
                  type="text"
                  size="small"
                  icon={<CopyOutlined />}
                  onClick={() => onCopy(r)}
                />
              </Tooltip>
            )}
            {primaryAction && (
              <Tooltip title={t(getTransitionActionLabel(currentState, primaryAction))}>
                <Button
                  type="text"
                  size="small"
                  icon={primaryAction === 'SUSPENDED' ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                  onClick={() => handleTransition(r, primaryAction)}
                />
              </Tooltip>
            )}
            <Dropdown
              menu={{
                items: buildTableMenuItems(currentState, dropdownTransitions, deleteDisabled, t),
                onClick: ({ key }) => {
                  if (key === 'delete') {
                    if (!deleteDisabled) handleDeleteClick(r);
                  } else handleTransition(r, key as ChannelState);
                },
              }}
              trigger={['click']}
            >
              <Button type="text" size="small" icon={<MoreOutlined />} />
            </Dropdown>
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
