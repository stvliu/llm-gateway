import { Card, App, Tooltip, Dropdown, Button, Space } from 'antd';
import {
  ThunderboltOutlined,
  EyeOutlined,
  MoreOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { theme } from 'antd';
import ChannelStateTag from '@/components/common/ChannelStateTag';
import { HealthDot } from '@/components/common/HealthDot';
import { getTransitionActionLabel } from '@/utils/stateTransitions';
import { getActionBarConfig } from '@/utils/channelActions';
import { CHANNEL_LIFECYCLE } from '@/domain/channel/lifecycle';
import { useDangerConfirm } from '@/components/common/useDangerConfirm';
import type { ChannelCard as ChannelCardType, ChannelState } from '@/types/channel';

/**
 * 卡片闪电图标的"测试意图"语义（任务 9.1）。
 * <p>卡片不再就地弹 toast / 调用 testCredential，而是把意图透传给父级，
 * 父级负责打开详情抽屉到 Credentials Tab 并对"测试全部"按钮做 800ms 高亮。</p>
 */
export interface ChannelTestIntent {
  /** 期望抽屉打开后的 Tab key，目前固定为 'credentials' */
  tab: 'credentials';
  /** 是否对"测试全部"按钮做 800ms 高亮 */
  highlightTestAll: boolean;
}

interface ChannelCardProps {
  channel: ChannelCardType;
  onClick: (channel: ChannelCardType) => void;
  onDelete: (id: number) => void;
  onToggleState: (id: number, enabled: boolean) => void;
  /** 卡片闪电图标点击回调；intent 描述父级该如何打开抽屉（任务 9.1） */
  onTest: (channel: ChannelCardType, intent?: ChannelTestIntent) => void;
  onStateTransition?: (id: number, targetState: string, reason?: string) => void;
}

/**
 * 渠道卡片组件
 *
 * <p>状态展示由 SSOT `CHANNEL_LIFECYCLE` 派生：</p>
 * <ul>
 *   <li>左边框颜色：meta.color</li>
 *   <li>RETIRED：visualStyle='strikethrough'，渠道名加删除线 + 灰色 #8c8c8c，
 *       卡片不再统一 opacity 0.5 降透（提高可读性）</li>
 *   <li>DEPRECATED：在渠道名下方展示副标题"仍参与流量分配，但已标记为不推荐"</li>
 *   <li>SUSPENDED：visualStyle='muted'，沿用轻度透明（0.85）作为低饱和提示</li>
 * </ul>
 */
export function ChannelCard({ channel, onClick, onDelete, onTest, onStateTransition }: ChannelCardProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const { modal } = App.useApp();
  // 删除整个渠道（任务 8.7）：与删除 API Key/端点/模型映射统一为 useDangerConfirm
  const { confirm: confirmDeleteChannel, contextHolder: dangerContextHolder } =
    useDangerConfirm();
  const currentState = channel.state as ChannelState;
  const meta = CHANNEL_LIFECYCLE[currentState] ?? CHANNEL_LIFECYCLE.SUSPENDED;
  const { primaryAction, dropdownTransitions, deleteDisabled } = getActionBarConfig(currentState);
  // visualStyle 派生卡片整体透明度：muted 状态保留轻度低饱和，其它状态保持 1
  // RETIRED 不再用 opacity 整体降透，改为渠道名 line-through + 灰色（见下方 nameStyle）
  const cardOpacity = meta.visualStyle === 'muted' ? 0.85 : 1;
  const isStrikethrough = meta.visualStyle === 'strikethrough';

  /** 状态转换点击 */
  const handleTransition = (targetState: ChannelState) => {
    const actionLabel = t(getTransitionActionLabel(currentState, targetState));

    // RETIRED：高危操作，红色确认
    if (targetState === 'RETIRED') {
      modal.confirm({
        title: t('channel.action.retire.confirmTitle', '停用渠道？'),
        content: t(
          'channel.action.retire.confirmDescription',
          '停用后该渠道不再参与任何流量分配，且无法恢复，已建立的指标历史保留'
        ),
        okType: 'danger',
        onOk: () => onStateTransition?.(channel.id, targetState, ''),
      });
      return;
    }

    // DEPRECATED：警告确认（非危险）
    if (targetState === 'DEPRECATED') {
      modal.confirm({
        title: actionLabel,
        content: t('card.confirmDeprecateContent', '确定要将此渠道标记为下线？'),
        onOk: () => onStateTransition?.(channel.id, targetState, ''),
      });
      return;
    }

    // 暂停操作（→ SUSPENDED）：轻量二次确认（默认按钮，非红色）
    if (targetState === 'SUSPENDED') {
      modal.confirm({
        title: t('channel.action.suspend.confirmTitle', '暂停渠道？'),
        content: t(
          'channel.action.suspend.confirmDescription',
          '暂停后该渠道不再分配流量，但保留配置'
        ),
        okType: 'default',
        onOk: () => onStateTransition?.(channel.id, targetState, ''),
      });
      return;
    }

    // 低风险操作直接执行（如 SUSPENDED→ACTIVE 恢复、PENDING→ACTIVE 激活）
    onStateTransition?.(channel.id, targetState, '');
  };

  /** 测试按钮点击：透传"打开详情抽屉到凭据 Tab + 高亮测试全部"意图（任务 9.1） */
  const handleTestClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onTest(channel, { tab: 'credentials', highlightTestAll: true });
  };

  /** 详情按钮点击 */
  const handleDetailClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onClick(channel);
  };

  /** 删除按钮点击：使用 useDangerConfirm 与 RETIRED 文案对齐（任务 8.7） */
  const handleDeleteClick = (e?: React.MouseEvent) => {
    if (deleteDisabled) return;
    e?.stopPropagation();
    confirmDeleteChannel({
      titleKey: 'channel.deleteDangerTitle',
      descriptionKey: 'channel.deleteDangerDescription',
      descriptionParams: { name: channel.name },
      onOk: () => onDelete(channel.id),
    });
  };

  /** 构建 Dropdown 菜单项 */
  function buildMenuItems(
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

  /** 渠道名样式：RETIRED 加删除线 + 灰色 */
  const nameStyle: React.CSSProperties = {
    fontWeight: 600,
    fontSize: token.fontSizeLG,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    ...(isStrikethrough
      ? { textDecoration: 'line-through', color: '#8c8c8c' }
      : {}),
  };

  return (
    <>
      {/* useDangerConfirm 的 contextHolder 必须挂载到组件树，否则 modal 不出现 */}
      {dangerContextHolder}
      <Card
        hoverable
        onClick={() => onClick(channel)}
        style={{
          opacity: cardOpacity,
          borderLeft: `3px solid ${meta.color}`,
        }}
        styles={{ body: { padding: '16px' } }}
      >
      {/* 第一行：渠道名称 + 操作按钮 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1, minWidth: 0 }}>
          {/* 任务 9.3：状态 Tag 右侧 6px 处嵌入 HealthDot */}
          <Space size={6}>
            <ChannelStateTag state={currentState} />
            <HealthDot
              status={channel.lastHealthStatus ?? null}
              lastCheckAt={channel.lastHealthCheckAt ?? null}
              source={channel.lastHealthSource ?? null}
            />
          </Space>
          <span style={nameStyle}>{channel.name}</span>
        </div>

        <Space size={2} style={{ flexShrink: 0 }} onClick={(e) => e.stopPropagation()}>
          {/* 测试按钮 — 所有非 RETIRED 可用 */}
          <Tooltip title={currentState !== 'RETIRED' ? t('card.testConnect') : t('card.testDisabled')}>
            <Button
              type="text"
              size="small"
              icon={<ThunderboltOutlined />}
              disabled={currentState === 'RETIRED'}
              onClick={handleTestClick}
              style={{ opacity: currentState === 'RETIRED' ? 0.4 : 1 }}
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

          {/* Primary 按钮 — 图标 + Tooltip，使用 text 类型与其他操作按钮风格统一 */}
          {primaryAction && (
            <Tooltip title={t(getTransitionActionLabel(currentState, primaryAction))}>
              <Button
                type="text"
                size="small"
                icon={primaryAction === 'SUSPENDED' ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                onClick={() => handleTransition(primaryAction)}
              />
            </Tooltip>
          )}

          {/* Dropdown — 剩余转换 + 删除 */}
          <Dropdown
            menu={{
              items: buildMenuItems(currentState, dropdownTransitions, deleteDisabled, t),
              onClick: ({ key }) => {
                if (key === 'delete') handleDeleteClick();
                else handleTransition(key as ChannelState);
              },
            }}
            trigger={['click']}
          >
            <Button type="text" size="small" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      </div>

      {/* DEPRECATED 副标题：仍参与流量分配，但已标记为不推荐 */}
      {currentState === 'DEPRECATED' && (
        <div
          style={{
            color: token.colorTextTertiary,
            fontSize: token.fontSizeSM,
            marginTop: 4,
          }}
        >
          <small>{t('channel.state.deprecatedSubtitle')}</small>
        </div>
      )}

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
    </>
  );
}
