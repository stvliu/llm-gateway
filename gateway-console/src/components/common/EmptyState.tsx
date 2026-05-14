import { theme } from 'antd';
import {
  CloudServerOutlined,
  AppstoreOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export type EmptyType = 'provider' | 'model' | 'search';

interface EmptyStateProps {
  type: EmptyType;
  action?: React.ReactNode;
  description?: string;
}

/**
 * 空状态组件
 */
export function EmptyState({ type, action, description }: EmptyStateProps) {
  const { t } = useTranslation('models');
  const { token } = theme.useToken();

  const getConfig = () => {
    switch (type) {
      case 'provider':
        return {
          icon: <CloudServerOutlined style={{ fontSize: 48, color: token.colorPrimary }} />,
          title: t('empty.noProvider'),
          description: description || t('empty.noProviderDesc'),
        };
      case 'model':
        return {
          icon: <AppstoreOutlined style={{ fontSize: 48, color: token.colorPrimary }} />,
          title: t('empty.noModel'),
          description: description || t('empty.noModelDesc'),
        };
      case 'search':
        return {
          icon: <SearchOutlined style={{ fontSize: 48, color: token.colorTextDisabled }} />,
          title: t('empty.noSearchResult'),
          description: description || t('empty.noSearchResultDesc'),
        };
      default:
        return {
          icon: null,
          title: '',
          description: '',
        };
    }
  };

  const config = getConfig();

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 24px',
        minHeight: 300,
      }}
    >
      <div style={{ marginBottom: 16 }}>{config.icon}</div>
      <h3 style={{ margin: '0 0 8px', fontSize: 16, fontWeight: 500 }}>
        {config.title}
      </h3>
      <p style={{ margin: '0 0 24px', color: token.colorTextSecondary, textAlign: 'center' }}>
        {config.description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
}
