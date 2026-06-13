import { Dropdown } from 'antd';
import {
  EditOutlined,
  ApiOutlined,
  ExportOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  DownOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { theme } from 'antd';
import { ProviderIcon } from '@/components/ui';

export interface ProviderGroupHeaderProps {
  providerName: string;
  providerCode?: string;
  channelCount: number;
  endpointCount: number;
  credentialCount: number;
  modelCount: number;
  collapsed: boolean;
  onToggle: () => void;
  onEdit?: () => void;
  onBatchSuspend?: () => void;
  onBatchResume?: () => void;
  onTestConnectivity?: () => void;
  onExport?: () => void;
}

/**
 * 供应商分组头组件
 * 显示供应商信息 + 聚合统计 + 折叠箭头 + 操作菜单
 * Provider 是纯组织分组实体，不展示状态
 */
export function ProviderGroupHeader({
  providerName,
  providerCode,
  channelCount,
  endpointCount,
  credentialCount,
  modelCount,
  collapsed,
  onToggle,
  onEdit,
  onBatchSuspend,
  onBatchResume,
  onTestConnectivity,
  onExport,
}: ProviderGroupHeaderProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();

  const menuItems = [
    { key: 'edit', label: t('group.editProvider'), icon: <EditOutlined /> },
    { key: 'test', label: t('group.connectivityTest'), icon: <ApiOutlined /> },
    { key: 'export', label: t('group.exportConfig'), icon: <ExportOutlined /> },
    { type: 'divider' as const },
    {
      key: 'batchSuspend',
      label: t('group.batchSuspend', '全部暂停'),
      icon: <PauseCircleOutlined />,
    },
    {
      key: 'batchResume',
      label: t('group.batchResume', '全部恢复'),
      icon: <PlayCircleOutlined />,
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
      case 'batchSuspend':
        onBatchSuspend?.();
        break;
      case 'batchResume':
        onBatchResume?.();
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
