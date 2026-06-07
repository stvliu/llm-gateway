import { Card, Dropdown, App, Tooltip, message } from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  PauseOutlined,
  PlayCircleOutlined,
  ThunderboltOutlined,
  MoreOutlined,
  EyeOutlined,
  SwapOutlined,
  PlusCircleOutlined,
  FileAddOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { theme } from 'antd';
import type { ChannelCard as ChannelCardType } from '@/types/channel';

interface ChannelCardProps {
  channel: ChannelCardType;
  onClick: (channel: ChannelCardType) => void;
  onDelete: (id: number) => void;
  onToggleState: (id: number, enabled: boolean) => void;
  onTest: (channel: ChannelCardType) => void;
  onOpenDrawerTab: (channel: ChannelCardType, tab: string) => void;
}

/**
 * 渠道卡片组件
 * 右上角图标组：启停 + 测试 常驻，低频操作收入 ⋮ Dropdown
 */
export function ChannelCard({ channel, onClick, onDelete, onToggleState, onTest, onOpenDrawerTab }: ChannelCardProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const { modal } = App.useApp();
  const isActive = channel.state === 'ACTIVE';

  /** 获取主端点 URL */
  const getMainEndpointUrl = (): string | null => {
    if (!channel.endpoints || channel.endpoints.length === 0) return null;
    return channel.endpoints[0].endpointUrl;
  };

  /** 复制主端点 URL */
  const handleCopyUrl = () => {
    const url = getMainEndpointUrl();
    if (!url) return;
    navigator.clipboard.writeText(url).then(() => {
      message.success(t('card.urlCopied'));
    });
  };

  /** 启停按钮点击 */
  const handleToggleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    modal.confirm({
      title: isActive ? t('card.confirmDisable') : t('card.confirmEnable'),
      onOk: () => onToggleState(channel.id, !isActive),
    });
  };

  /** 测试按钮点击 */
  const handleTestClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onTest(channel);
  };

  /** 低频菜单项 */
  const menuItems = [
    { key: 'detail', label: t('card.viewDetail'), icon: <EyeOutlined /> },
    { key: 'edit', label: t('card.edit'), icon: <EditOutlined /> },
    { type: 'divider' as const },
    { key: 'credential', label: t('card.replaceKey'), icon: <SwapOutlined /> },
    { key: 'endpoint', label: t('card.addEndpoint'), icon: <PlusCircleOutlined /> },
    { key: 'model', label: t('card.addModel'), icon: <FileAddOutlined /> },
    { type: 'divider' as const },
    {
      key: 'copyUrl',
      label: t('card.copyMainUrl'),
      icon: <CopyOutlined />,
      disabled: !getMainEndpointUrl(),
    },
    { type: 'divider' as const },
    { key: 'delete', label: t('card.delete'), icon: <DeleteOutlined />, danger: true },
  ];

  const handleMenuClick = (e: { key: string }) => {
    switch (e.key) {
      case 'detail':
        onClick(channel);
        break;
      case 'edit':
        onOpenDrawerTab(channel, 'quota');
        break;
      case 'credential':
        onOpenDrawerTab(channel, 'credentials');
        break;
      case 'endpoint':
        onOpenDrawerTab(channel, 'endpoints');
        break;
      case 'model':
        onOpenDrawerTab(channel, 'models');
        break;
      case 'copyUrl':
        handleCopyUrl();
        break;
      case 'delete':
        modal.confirm({
          title: t('card.deleteConfirmTitle'),
          content: t('card.deleteConfirmContent', { name: channel.name }),
          okType: 'danger',
          onOk: () => onDelete(channel.id),
        });
        break;
    }
  };

  /** 图标按钮通用样式 */
  const iconBtnStyle = (extra?: React.CSSProperties): React.CSSProperties => ({
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: 28,
    height: 28,
    borderRadius: token.borderRadiusSM,
    cursor: 'pointer',
    color: token.colorTextSecondary,
    transition: 'color 0.2s, background 0.2s',
    ...extra,
  });

  return (
    <Card
      hoverable
      onClick={() => onClick(channel)}
      style={{
        opacity: isActive ? 1 : 0.6,
        borderLeft: isActive
          ? `3px solid ${token.colorSuccess}`
          : `3px solid ${token.colorTextQuaternary}`,
      }}
      styles={{ body: { padding: '16px' } }}
    >
      {/* 第一行：渠道名称 + 操作按钮 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{
          fontWeight: 600,
          fontSize: token.fontSizeLG,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          flex: 1,
          minWidth: 0,
          marginRight: token.marginSM,
        }}>
          {channel.name}
        </span>

        <div style={{ display: 'flex', alignItems: 'center', gap: 2, flexShrink: 0 }}>
          {/* 启停按钮 */}
          <Tooltip title={isActive ? t('card.disable') : t('card.enable')}>
            <span
              role="button"
              onClick={handleToggleClick}
              style={iconBtnStyle({ color: isActive ? token.colorTextSecondary : token.colorPrimary })}
              onMouseEnter={(e) => { e.currentTarget.style.background = token.colorBgTextHover; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
            >
              {isActive ? <PauseOutlined /> : <PlayCircleOutlined />}
            </span>
          </Tooltip>

          {/* 测试按钮 */}
          <Tooltip title={isActive ? t('card.testConnect') : t('card.testDisabled')}>
            <span
              role="button"
              onClick={isActive ? handleTestClick : undefined}
              style={iconBtnStyle({
                cursor: isActive ? 'pointer' : 'not-allowed',
                opacity: isActive ? 1 : 0.4,
              })}
              onMouseEnter={(e) => { if (isActive) e.currentTarget.style.background = token.colorBgTextHover; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
            >
              <ThunderboltOutlined />
            </span>
          </Tooltip>

          {/* ⋮ 更多菜单 */}
          <Dropdown menu={{ items: menuItems, onClick: handleMenuClick }} trigger={['click']}>
            <Tooltip title={t('card.more')}>
              <span
                role="button"
                onClick={(e) => e.stopPropagation()}
                style={iconBtnStyle()}
                onMouseEnter={(e) => { e.currentTarget.style.background = token.colorBgTextHover; }}
                onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
              >
                <MoreOutlined />
              </span>
            </Tooltip>
          </Dropdown>
        </div>
      </div>

      {/* 第二行：状态圆点 + 状态文字 + 统计信息 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: token.marginXS, color: token.colorTextSecondary, fontSize: token.fontSizeSM, marginTop: token.marginXS }}>
        <span style={{
          display: 'inline-block',
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: isActive ? token.colorSuccess : token.colorTextQuaternary,
          flexShrink: 0,
        }} />
        <span>{isActive ? t('status.active') : t('status.inactive')}</span>
        <span style={{ margin: '0 2px' }}>·</span>
        <span>{channel.stats?.endpointCount ?? 0} {t('card.endpoints')}</span>
        <span>{channel.stats?.credentialCount ?? 0} Key</span>
        <span>{channel.stats?.modelCount ?? 0} {t('card.models')}</span>
      </div>
    </Card>
  );
}
