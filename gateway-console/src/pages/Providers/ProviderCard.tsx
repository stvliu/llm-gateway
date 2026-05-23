import { Card, Tag, Typography, Space, Tooltip, Spin, Dropdown } from 'antd';
import { GlobalOutlined, LinkOutlined, AppstoreOutlined, MoreOutlined, EyeOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { useTranslation } from 'react-i18next';
import { useProducts } from '@/services/query/useProducts';
import { ProviderIcon } from '@/components/ui';
import type { Provider } from '@/types/provider';

const { Text, Paragraph } = Typography;

interface Props {
  provider: Provider;
  onView: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onViewProducts?: () => void;
}

export default function ProviderCard({ provider, onView, onEdit, onDelete, onViewProducts }: Props) {
  const { t } = useTranslation('providers');

  // ensure provider.id is valid
  const providerId = provider?.id ?? 0;
  const { data: products, isLoading } = useProducts(providerId);

  // filter active products (backend uses lowercase "active")
  const activeProducts = products?.filter(p => p.state?.toUpperCase() === 'ACTIVE') || [];
  const displayProducts = activeProducts.slice(0, 3);
  const remainingCount = activeProducts.length - displayProducts.length;

  // dropdown menu items
  const menuItems: MenuProps['items'] = [
    {
      key: 'view',
      label: t('actions.view', { defaultValue: 'View' }),
      icon: <EyeOutlined />,
      onClick: (e) => {
        e.domEvent.stopPropagation();
        onView();
      },
    },
    {
      key: 'edit',
      label: t('actions.edit', { defaultValue: 'Edit' }),
      icon: <EditOutlined />,
      onClick: (e) => {
        e.domEvent.stopPropagation();
        onEdit();
      },
    },
    {
      type: 'divider',
    },
    {
      key: 'delete',
      label: t('actions.delete', { defaultValue: 'Delete' }),
      icon: <DeleteOutlined />,
      danger: true,
      onClick: (e) => {
        e.domEvent.stopPropagation();
        onDelete();
      },
    },
  ];

  // state indicator config: use colored Tag for better visibility
  const stateConfig: Record<string, { color: string; label: string }> = {
    ACTIVE: { color: 'success', label: t('state.active', { defaultValue: 'Active' }) },
    INACTIVE: { color: 'warning', label: t('state.inactive', { defaultValue: 'Inactive' }) },
  };
  const currentState = stateConfig[provider.state?.toUpperCase() as keyof typeof stateConfig] || { color: 'default', label: provider.state || 'Unknown' };

  return (
    <Card
      hoverable
      onClick={onView}
      style={{ height: '100%' }}
      title={
        <Space>
          <ProviderIcon providerId={provider.providerId} size={24} />
          <Text strong>{provider.providerName}</Text>
          <Tag color={currentState.color}>{currentState.label}</Tag>
        </Space>
      }
      extra={
        <Dropdown
          menu={{ items: menuItems }}
          trigger={['click']}
          placement="bottomRight"
        >
          <Tag
            onClick={(e) => e.stopPropagation()}
            style={{ cursor: 'pointer', marginRight: 0 }}
          >
            <MoreOutlined />
          </Tag>
        </Dropdown>
      }
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">

        {provider.description && (
          <Tooltip title={provider.description}>
            <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>
              {provider.description}
            </Text>
          </Tooltip>
        )}

        {provider.websiteUrl && (
          <Tooltip title={provider.websiteUrl}>
            <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>
              <GlobalOutlined /> {provider.websiteUrl}
            </Text>
          </Tooltip>
        )}

        {provider.apiDocUrl && (
          <Paragraph type="secondary" ellipsis style={{ maxWidth: 200, marginBottom: 0 }}>
            <LinkOutlined /> {provider.apiDocUrl}
          </Paragraph>
        )}

        {/* Products section */}
        <div style={{ marginTop: 8 }}>
          <div style={{ marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
            <AppstoreOutlined />
            <Text type="secondary">
              {t('card.products', { defaultValue: 'Products' })} ({activeProducts.length})
            </Text>
          </div>
          {isLoading ? (
            <Spin size="small" />
          ) : activeProducts.length === 0 ? (
            <Text type="secondary" style={{ fontSize: 12 }}>
              {t('card.noProducts', { defaultValue: 'No products' })}
            </Text>
          ) : (
            <Space wrap size={[4, 4]}>
              {displayProducts.map((product) => (
                <Tag
                  key={product.id}
                  color="blue"
                  style={{ cursor: 'pointer' }}
                  onClick={(e) => {
                    e.stopPropagation();
                    onViewProducts?.();
                  }}
                >
                  {product.productName}
                </Tag>
              ))}
              {remainingCount > 0 && (
                <Tag>+{remainingCount}</Tag>
              )}
            </Space>
          )}
        </div>
      </Space>
    </Card>
  );
}
