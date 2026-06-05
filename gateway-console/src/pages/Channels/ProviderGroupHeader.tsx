import { Dropdown } from 'antd';
import {
  EditOutlined,
  ApiOutlined,
  ExportOutlined,
  StopOutlined,
  CheckCircleOutlined,
  DownOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { theme } from 'antd';
import { ProviderIcon } from '@/components/ui';

export interface ProviderGroupHeaderProps {
  providerName: string;
  providerCode?: string;
  providerState?: string;
  channelCount: number;
  endpointCount: number;
  credentialCount: number;
  modelCount: number;
  collapsed: boolean;
  onToggle: () => void;
  onEdit?: () => void;
  onToggleEnabled?: () => void;
  onTestConnectivity?: () => void;
  onExport?: () => void;
}

/**
 * 供应商分组头组件
 * 显示供应商信息 + 聚合统计 + 折叠箭头 + 操作菜单
 */
export function ProviderGroupHeader({
  providerName,
  providerCode,
  providerState,
  channelCount,
  endpointCount,
  credentialCount,
  modelCount,
  collapsed,
  onToggle,
  onEdit,
  onToggleEnabled,
  onTestConnectivity,
  onExport,
}: ProviderGroupHeaderProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const isActive = providerState !== 'INACTIVE';

  const menuItems = [
    { key: 'edit', label: t('group.editProvider'), icon: <EditOutlined /> },
    { key: 'test', label: t('group.connectivityTest'), icon: <ApiOutlined /> },
    { key: 'export', label: t('group.exportConfig'), icon: <ExportOutlined /> },
    { type: 'divider' as const },
    {
      key: 'toggle',
      label: isActive ? t('group.disableProvider') : t('group.enableProvider'),
      icon: isActive ? <StopOutlined /> : <CheckCircleOutlined />,
      danger: isActive,
    },
  ];

  const handleMenuClick = (e: { key: string }) => {
    switch (e.key) {
      case 'edit':
        onEdit?.();
        break;
      case 'test':
        onTestConnectivity?.();
        break;
      case 'export':
        onExport?.();
        break;
      case 'toggle':
        onToggleEnabled?.();
        break;
    }
  };

  return (
    <div
      onClick={onToggle}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onToggle();
        }
      }}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 16px',
        background: token.colorFillQuaternary,
        borderRadius: token.borderRadiusLG,
        cursor: 'pointer',
        marginBottom: token.marginSM,
        opacity: isActive ? 1 : 0.6,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: token.margin }}>
        {providerCode && <ProviderIcon providerId={providerCode} size={32} />}
        <div>
          <span style={{ fontWeight: 600, fontSize: token.fontSizeLG }}>{providerName}</span>
          <br />
          <span style={{ color: token.colorTextSecondary, fontSize: token.fontSizeSM }}>
            {t('group.channelCount', { count: channelCount })}
          </span>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: token.margin }}>
        <span style={{ color: token.colorTextSecondary, fontSize: token.fontSize }}>
          {t('card.endpoints')}: {endpointCount}
        </span>
        <span style={{ color: token.colorTextSecondary, fontSize: token.fontSize }}>
          Key: {credentialCount}
        </span>
        <span style={{ color: token.colorTextSecondary, fontSize: token.fontSize }}>
          {t('card.models')}: {modelCount}
        </span>

        <Dropdown
          menu={{ items: menuItems, onClick: handleMenuClick }}
          trigger={['click']}
        >
          <DownOutlined
            style={{ fontSize: '18px', color: token.colorTextSecondary }}
            onClick={(e) => e.stopPropagation()}
          />
        </Dropdown>

        <span
          style={{
            transform: collapsed ? 'rotate(0deg)' : 'rotate(90deg)',
            transition: 'transform 0.2s',
            color: token.colorTextSecondary,
          }}
        >
          ▶
        </span>
      </div>
    </div>
  );
}
