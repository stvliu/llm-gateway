import { Card, Tag, Dropdown, App } from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  StopOutlined,
  CheckCircleOutlined,
  MoreOutlined,
  ApiOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { theme } from 'antd';
import type { ChannelCard as ChannelCardType } from '@/types/channel';

interface ChannelCardProps {
  channel: ChannelCardType;
  onClick: (channel: ChannelCardType) => void;
  onDelete: (id: number) => void;
  onToggleState: (id: number, enabled: boolean) => void;
}

/**
 * 渠道卡片组件
 */
export function ChannelCard({ channel, onClick, onDelete, onToggleState }: ChannelCardProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const { modal } = App.useApp();
  const isActive = channel.state === 'ACTIVE';

  const menuItems = [
    { key: 'edit', label: t('card.edit'), icon: <EditOutlined /> },
    { key: 'test', label: t('card.testConnectivity'), icon: <ApiOutlined /> },
    { type: 'divider' as const },
    {
      key: 'toggle',
      label: isActive ? t('card.disable') : t('card.enable'),
      icon: isActive ? <StopOutlined /> : <CheckCircleOutlined />,
    },
    { type: 'divider' as const },
    { key: 'delete', label: t('card.delete'), icon: <DeleteOutlined />, danger: true },
  ];

  const handleMenuClick = (e: { key: string }) => {
    switch (e.key) {
      case 'edit':
        onClick(channel);
        break;
      case 'test':
        onClick(channel);
        break;
      case 'toggle':
        onToggleState(channel.id, !isActive);
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: token.marginSM, marginBottom: token.marginXS }}>
            <span style={{ fontWeight: 600, fontSize: token.fontSizeLG, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {channel.name}
            </span>
            <Tag color={isActive ? 'success' : 'default'}>
              {isActive ? t('status.active') : t('status.inactive')}
            </Tag>
          </div>

          {channel.endpoints && channel.endpoints.length > 0 && (
            <div style={{ color: token.colorTextSecondary, fontSize: token.fontSizeSM, marginBottom: token.marginXS, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {channel.endpoints[0].endpointUrl}
            </div>
          )}

          <div style={{ display: 'flex', gap: token.marginSM, color: token.colorTextSecondary, fontSize: token.fontSizeSM }}>
            <span>{t('card.endpoints')}: {channel.stats?.endpointCount ?? 0}</span>
            <span>Key: {channel.stats?.credentialCount ?? 0}</span>
            <span>{t('card.models')}: {channel.stats?.modelCount ?? 0}</span>
          </div>
        </div>

        <Dropdown menu={{ items: menuItems, onClick: handleMenuClick }} trigger={['click']}>
          <MoreOutlined
            style={{ fontSize: '16px', color: token.colorTextSecondary }}
            onClick={(e) => e.stopPropagation()}
          />
        </Dropdown>
      </div>
    </Card>
  );
}
