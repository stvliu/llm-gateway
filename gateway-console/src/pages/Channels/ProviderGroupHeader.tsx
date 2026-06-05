import { Typography, Space, Tooltip, Dropdown, Modal, theme } from 'antd';
import {
  RightOutlined,
  EditOutlined,
  ApiOutlined,
  ExportOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderIcon } from '@/components/ui/ProviderIcon';
import type { MenuProps } from 'antd';
import type { FC } from 'react';

const { Text } = Typography;

export interface ProviderGroupHeaderProps {
  /** 供应商 ID */
  providerId?: string;
  /** 供应商名称 */
  providerName: string;
  /** 供应商状态 */
  providerState?: string;
  /** 渠道数量 */
  channelCount: number;
  /** 端点总数 */
  endpointCount: number;
  /** 凭证总数 */
  credentialCount: number;
  /** 模型总数 */
  modelCount: number;
  /** 是否折叠 */
  collapsed: boolean;
  /** 折叠/展开回调 */
  onToggle: () => void;
  /** 编辑供应商回调 */
  onEdit?: () => void;
  /** 停用/启用供应商回调 */
  onToggleEnabled?: () => void;
  /** 连通性测试回调 */
  onTestConnectivity?: () => void;
  /** 导出配置回调 */
  onExport?: () => void;
}

/**
 * 供应商分组头组件
 * 显示供应商 Logo + 名称 + 聚合统计 + 折叠箭头 + 操作菜单
 */
export const ProviderGroupHeader: FC<ProviderGroupHeaderProps> = ({
  providerId,
  providerName,
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
}) => {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const isActive = providerState !== 'INACTIVE';

  const handleMenuClick: MenuProps['onClick'] = (e) => {
    e.domEvent.stopPropagation();
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
      case 'toggle': {
        if (isActive) {
          Modal.confirm({
            title: t('group.confirmDisableTitle'),
            content: t('group.confirmDisableContent', { name: providerName, count: channelCount }),
            okText: t('group.confirmDisableOk'),
            okType: 'danger',
            cancelText: t('actions.cancel', { ns: 'common' }),
            onOk: () => onToggleEnabled?.(),
          });
        } else {
          onToggleEnabled?.();
        }
        break;
      }
    }
  };

  const menuItems: MenuProps['items'] = [
    { key: 'edit', label: t('group.editProvider'), icon: <EditOutlined /> },
    { key: 'test', label: t('group.connectivityTest'), icon: <ApiOutlined /> },
    { key: 'export', label: t('group.exportConfig'), icon: <ExportOutlined /> },
    { type: 'divider' },
    {
      key: 'toggle',
      label: isActive ? t('group.disableProvider') : t('group.enableProvider'),
      icon: isActive ? <PauseCircleOutlined /> : <PlayCircleOutlined />,
      danger: isActive,
    },
  ];

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
        borderRadius: '8px',
        cursor: 'pointer',
        marginBottom: '8px',
        transition: 'all 0.2s',
        opacity: isActive ? 1 : 0.6,
      }}
      className="provider-group-header"
    >
      {/* 左侧：供应商信息 */}
      <Space size={12}>
        <ProviderIcon providerId={providerId} size={32} />
        <div>
          <Text strong style={{ fontSize: '16px' }}>
            {providerName}
          </Text>
          <br />
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {t('group.channelCount', { count: channelCount })}
          </Text>
        </div>
      </Space>

      {/* 右侧：统计信息 + 操作菜单 + 折叠箭头 */}
      <Space size={16}>
        <Space size={16}>
          <Tooltip title={t('card.endpoints')}>
            <Text type="secondary" style={{ fontSize: '13px' }}>
              {`${t('card.endpoints')}: ${endpointCount}`}
            </Text>
          </Tooltip>
          <Tooltip title={t('card.keys')}>
            <Text type="secondary" style={{ fontSize: '13px' }}>
              {`Key: ${credentialCount}`}
            </Text>
          </Tooltip>
          <Tooltip title={t('card.models')}>
            <Text type="secondary" style={{ fontSize: '13px' }}>
              {`${t('card.models')}: ${modelCount}`}
            </Text>
          </Tooltip>
        </Space>

        {/* 更多操作菜单 */}
        <Dropdown
          menu={{ items: menuItems, onClick: handleMenuClick }}
          trigger={['click']}
        >
          <MoreOutlined
            style={{ fontSize: '18px', color: token.colorTextSecondary }}
            onClick={(e) => e.stopPropagation()}
          />
        </Dropdown>

        <RightOutlined
          rotate={collapsed ? 0 : 90}
          style={{ transition: 'transform 0.2s', color: token.colorTextSecondary }}
        />
      </Space>
    </div>
  );
};
