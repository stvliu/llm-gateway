import { useState } from 'react';
import { Card, Button, Tag, Space, Spin, Typography, Collapse } from 'antd';
import { EditOutlined, DeleteOutlined, PlusOutlined, ApiOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProductApiKeys } from '@/services/query/useProducts';
import type { Product, ProductApiKey } from '@/types/product';
import ProductApiKeyCreateModal from './ProductApiKeyCreateModal';
import ProductApiKeyEditModal from './ProductApiKeyEditModal';
import ProductApiKeyTestButton from './ProductApiKeyTestButton';

const { Text } = Typography;

interface Props {
  product: Product;
  onEdit: () => void;
  onDelete: () => void;
}

export default function ProductCard({ product, onEdit, onDelete }: Props) {
  const { t } = useTranslation('products');
  const { data: apiKeys, isLoading } = useProductApiKeys(product.id);

  const activeKeys = apiKeys?.filter(k => k.state === 'ACTIVE') || [];
  const endpointCount = Object.keys(product.endpoints || {}).length;

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<ProductApiKey | undefined>();

  return (
    <Card
      title={
        <Space>
          <Text strong>{product.productName}</Text>
          <Tag color={product.state === 'ACTIVE' ? 'green' : 'default'}>
            {product.state}
          </Tag>
        </Space>
      }
      extra={
        <Space>
          <Button type="text" icon={<EditOutlined />} onClick={onEdit} />
          <Button type="text" danger icon={<DeleteOutlined />} onClick={onDelete} />
        </Space>
      }
      style={{ marginBottom: 16 }}
    >
      {/* 统计信息 */}
      <Space style={{ marginBottom: 12 }}>
        <Tag>{t('product.endpoints')}: {endpointCount}</Tag>
        <Tag>{t('product.keys')}: {activeKeys.length}/{apiKeys?.length || 0}</Tag>
      </Space>

      {/* 端点配置 */}
      {endpointCount > 0 && (
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary" style={{ marginBottom: 4, display: 'block' }}>
            {t('product.endpoints')}
          </Text>
          <Collapse
            ghost
            items={Object.entries(product.endpoints || {}).map(([protocol, url]) => ({
              key: protocol,
              label: protocol,
              children: <Text code>{url}</Text>,
            }))}
          />
        </div>
      )}

      {/* API Keys 列表 */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
          <Text type="secondary">{t('product.apiKey')}</Text>
          <Button
            type="link"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => setCreateModalOpen(true)}
          >
            {t('product.addApiKey')}
          </Button>
        </div>

        {isLoading ? (
          <Spin size="small" />
        ) : (
          <div style={{ border: '1px solid #f0f0f0', borderRadius: 4 }}>
            {apiKeys?.map((key, index) => (
              <div
                key={key.id}
                style={{
                  padding: '8px 12px',
                  borderBottom: index < apiKeys.length - 1 ? '1px solid #f0f0f0' : 'none',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <Space>
                  <ApiOutlined />
                  {key.name && <Text strong>{key.name}</Text>}
                  <Text code>{key.apiKeyPrefix}</Text>
                  <Tag>{t('product.priorityLabel')}: {key.priority}</Tag>
                  <Tag>{t('product.weightLabel')}: {key.weight}</Tag>
                  <Tag color={key.state === 'ACTIVE' ? 'green' : 'default'}>
                    {key.state}
                  </Tag>
                </Space>
                <Space>
                  <ProductApiKeyTestButton productId={product.id} keyId={key.id} />
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => setEditingKey(key)}
                  />
                </Space>
              </div>
            ))}
            {(!apiKeys || apiKeys.length === 0) && (
              <div style={{ padding: 12, textAlign: 'center', color: '#999' }}>
                {t('product.addApiKey')}
              </div>
            )}
          </div>
        )}
      </div>

      {/* API Key 创建弹窗 */}
      <ProductApiKeyCreateModal
        open={createModalOpen}
        productId={product.id}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={() => setCreateModalOpen(false)}
      />

      {/* API Key 编辑弹窗 */}
      <ProductApiKeyEditModal
        open={!!editingKey}
        productId={product.id}
        apiKey={editingKey}
        onClose={() => setEditingKey(undefined)}
      />
    </Card>
  );
}
