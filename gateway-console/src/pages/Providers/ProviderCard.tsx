import { Card, Tag, Typography, Space, Tooltip, Spin } from 'antd';
import { GlobalOutlined, LinkOutlined, AppstoreOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProducts } from '@/services/query/useProducts';
import type { Provider } from '@/types/provider';

const { Text, Paragraph } = Typography;

interface Props {
  provider: Provider;
  onClick: () => void;
  onViewProducts?: () => void;
}

export default function ProviderCard({ provider, onClick, onViewProducts }: Props) {
  const { t } = useTranslation('providers');
  const { data: products, isLoading } = useProducts(provider.id);

  const activeProducts = products?.filter(p => p.state === 'ACTIVE') || [];
  const displayProducts = activeProducts.slice(0, 3);
  const remainingCount = activeProducts.length - displayProducts.length;

  return (
    <Card
      hoverable
      onClick={onClick}
      style={{ height: '100%' }}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {/* 基本信息 */}
        <Space>
          <Text strong>{provider.providerName}</Text>
          <Tag color={provider.state === 'ACTIVE' ? 'green' : 'default'}>
            {provider.state}
          </Tag>
        </Space>

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

        {/* 产品展示区域 */}
        <div style={{ marginTop: 8 }}>
          <div style={{ marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
            <AppstoreOutlined />
            <Text type="secondary">
              {t('card.products', { defaultValue: '产品' })} ({activeProducts.length})
            </Text>
          </div>
          {isLoading ? (
            <Spin size="small" />
          ) : activeProducts.length === 0 ? (
            <Text type="secondary" style={{ fontSize: 12 }}>
              {t('card.noProducts', { defaultValue: '暂无产品' })}
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
