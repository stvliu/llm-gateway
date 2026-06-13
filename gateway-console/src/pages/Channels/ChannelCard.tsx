import { useState } from 'react';
import { Card, App, Tooltip, Dropdown, Button, Space } from 'antd';
import {
  DeleteOutlined,
  ThunderboltOutlined,
  EyeOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { theme } from 'antd';
import ChannelStateTag from '@/components/common/ChannelStateTag';
import { getAvailableTransitions, getTransitionActionLabel } from '@/utils/stateTransitions';
import type { ChannelCard as ChannelCardType, ChannelState } from '@/types/channel';

interface ChannelCardProps {
  channel: ChannelCardType;
  onClick: (channel: ChannelCardType) => void;
  onDelete: (id: number) => void;
  onToggleState: (id: number, enabled: boolean) => void;
  onTest: (channel: ChannelCardType) => void;
  onStateTransition?: (id: number, targetState: string, reason?: string) => void;
}

/** 状态边框颜色映射 */
const STATE_BORDER_COLORS: Record<ChannelState, string> = {
  PENDING: '#faad14',
  ACTIVE: '#52c41a',
  SUSPENDED: '#d9d9d9',
  DEPRECATED: '#fa8c16',
  RETIRED: '#ff4d4f',
};

/** 状态透明度映射 */
const STATE_OPACITY: Record<ChannelState, number> = {
  PENDING: 0.8,
  ACTIVE: 1,
  SUSPENDED: 0.6,
  DEPRECATED: 0.8,
  RETIRED: 0.5,
};

/**
 * 渠道卡片组件
 * 状态展示：5 色左边框 + 透明度 + ChannelStateTag
 * 操作：Toggle 改为上下文操作按钮
 */
export function ChannelCard({ channel, onClick, onDelete, onTest, onStateTransition }: ChannelCardProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const { modal } = App.useApp();
  const currentState = channel.state as ChannelState;
  const availableTransitions = getAvailableTransitions(currentState);
  const isRoutable = currentState === 'ACTIVE' || currentState === 'DEPRECATED';

  /** 状态转换点击 */
  const handleTransition = (targetState: ChannelState) => {
    const actionLabel = getTransitionActionLabel(currentState, targetState);

    // 高风险操作需要二次确认
    if (targetState === 'DEPRECATED' || targetState === 'RETIRED') {
      let content = t('card.confirmDeprecateContent', '确定要将此渠道标记为下线？');
      if (targetState === 'RETIRED') {
        content = t('card.confirmRetireContent', '此操作不可逆，确定要废弃此渠道？');
      }
      modal.confirm({
        title: actionLabel,
        content,
        okType: 'danger',
        onOk: () => onStateTransition?.(channel.id, targetState, ''),
      });
      return;
    }

    // 低风险操作直接执行
    onStateTransition?.(channel.id, targetState, '');
  };

  /** 测试按钮点击 */
  const handleTestClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onTest(channel);
  };

  /** 详情按钮点击 */
  const handleDetailClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onClick(channel);
  };

  /** 删除按钮点击 */
  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    modal.confirm({
      title: t('card.deleteConfirmTitle'),
      content: t('card.deleteConfirmContent', { name: channel.name }),
      okType: 'danger',
      onOk: () => onDelete(channel.id),
    });
  };

  return (
    <Card
      hoverable
      onClick={() => onClick(channel)}
      style={{
        opacity: STATE_OPACITY[currentState] ?? 0.6,
        borderLeft: `3px solid ${STATE_BORDER_COLORS[currentState] ?? token.colorTextQuaternary}`,
      }}
      styles={{ body: { padding: '16px' } }}
    >
      {/* 第一行：渠道名称 + 操作按钮 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1, minWidth: 0 }}>
          <ChannelStateTag state={currentState} />
          <span style={{
            fontWeight: 600,
            fontSize: token.fontSizeLG,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}>
            {channel.name}
          </span>
        </div>

        <Space size={2} style={{ flexShrink: 0 }} onClick={(e) => e.stopPropagation()}>
          {/* 测试按钮 */}
          <Tooltip title={isRoutable ? t('card.testConnect') : t('card.testDisabled')}>
            <Button
              type="text"
              size="small"
              icon={<ThunderboltOutlined />}
              disabled={!isRoutable}
              onClick={handleTestClick}
              style={{ opacity: isRoutable ? 1 : 0.4 }}
            />
          </Tooltip>

          {/* 详情按钮 */}
          <Tooltip title={t('card.viewDetail')}>
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={handleDetailClick}
            />
          </Tooltip>

          {/* 状态转换操作菜单 */}
          {availableTransitions.length > 0 && (
            <Dropdown
              menu={{
                items: availableTransitions.map((target) => ({
                  key: target,
                  label: getTransitionActionLabel(currentState, target),
                  danger: target === 'RETIRED',
                })),
                onClick: ({ key }) => handleTransition(key as ChannelState),
              }}
              trigger={['click']}
            >
              <Button type="text" size="small" icon={<MoreOutlined />} />
            </Dropdown>
          )}

          {/* 删除按钮 */}
          <Tooltip title={t('card.delete')}>
            <Button
              type="text"
              size="small"
              icon={<DeleteOutlined />}
              danger
              onClick={handleDeleteClick}
            />
          </Tooltip>
        </Space>
      </div>

      {/* 第二行：统计信息 */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: token.marginXS,
        color: token.colorTextSecondary,
        fontSize: token.fontSizeSM,
        marginTop: token.marginXS,
      }}>
        <span>{channel.stats?.endpointCount ?? 0} {t('card.endpoints')}</span>
        <span>·</span>
        <span>{channel.stats?.credentialCount ?? 0} Key</span>
        <span>·</span>
        <span>{channel.stats?.modelCount ?? 0} {t('card.models')}</span>
      </div>
    </Card>
  );
}
