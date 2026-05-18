import { useState } from 'react';
import { Card, Button, Tag, Space, Empty, Spin, App, Collapse, InputNumber, Typography, Modal } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import {
  useProducts,
  useDeleteProduct,
  useProductApiKeys,
  useCreateProductApiKey,
  useDeleteProductApiKey,
} from '@/services/query/useProducts';
import type { Product, CreateProductApiKeyRequest } from '@/types/product';
import ProductFormModal from './ProductFormModal';

interface ProviderProductsTabProps {
  providerId: number;
}

const PRODUCT_TYPE_COLOR: Record<string, string> = {
  pay_as_you_go: 'blue',
  subscription_coding: 'green',
  subscription_token: 'orange',
};

const TYPE_LABEL_KEY: Record<string, string> = {
  pay_as_you_go: 'typePayAsYouGo',
  subscription_coding: 'TypeSubscriptionCoding',
  subscription_token: 'TypeSubscriptionToken',
};

function ProductApiKeySection({ productId }: { productId: number }) {
  const { t } = useTranslation('products');
  const { message, modal } = App.useApp();
  const { data: apiKeys, isLoading } = useProductApiKeys(productId);
  const createKeyMutation = useCreateProductApiKey();
  const deleteKeyMutation = useDeleteProductApiKey();
  const [adding, setAdding] = useState(false);
  const [newKeyData, setNewKeyData] = useState<Partial<CreateProductApiKeyRequest>>({});
  const [createdKey, setCreatedKey] = useState<string | null>(null);

  const handleCreateKey = async () => {
    if (!newKeyData.apiKey) {
      message.warning(t('product.apiKeyRequired'));
      return;
    }
    try {
      const result = await createKeyMutation.mutateAsync({
        productId,
        apiKey: newKeyData.apiKey,
        baseUrl: newKeyData.baseUrl,
        priority: newKeyData.priority,
        weight: newKeyData.weight,
        description: newKeyData.description,
      });
      message.success(t('product.addApiKey'));
      setCreatedKey(result.apiKeyPlain);
      setAdding(false);
      setNewKeyData({});
    } catch {
      message.error(t('product.addApiKey'));
    }
  };

  const handleDeleteKey = (keyId: number, keyName: string) => {
    if (apiKeys && apiKeys.length <= 1) {
      message.warning(t('product.lastApiKeyWarning'));
      return;
    }
    modal.confirm({
      title: t('product.deleteApiKey'),
      content: t('product.deleteApiKeyConfirm', { name: keyName }),
      okType: 'danger',
      onOk: () => deleteKeyMutation.mutateAsync({ id: keyId, productId }),
    });
  };

  if (isLoading) return <Spin size="small" />;

  return (
    <div style={{ marginTop: 8 }}>
      <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontWeight: 500 }}>{t('product.apiKey')} ({apiKeys?.length ?? 0})</span>
        <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={() => setAdding(true)}>
          {t('product.addApiKey')}
        </Button>
      </div>

      {adding && (
        <Card size="small" style={{ marginBottom: 8 }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <div>
              <span style={{ width: 80, display: 'inline-block' }}>API Key:</span>
              <input
                style={{ width: 300, padding: '4px 8px', border: '1px solid #d9d9d9', borderRadius: 4 }}
                placeholder="sk-..."
                value={newKeyData.apiKey || ''}
                onChange={(e) => setNewKeyData({ ...newKeyData, apiKey: e.target.value })}
              />
            </div>
            <div>
              <span style={{ width: 80, display: 'inline-block' }}>Base URL:</span>
              <input
                style={{ width: 300, padding: '4px 8px', border: '1px solid #d9d9d9', borderRadius: 4 }}
                placeholder="https://api.example.com"
                value={newKeyData.baseUrl || ''}
                onChange={(e) => setNewKeyData({ ...newKeyData, baseUrl: e.target.value })}
              />
            </div>
            <div>
              <span style={{ width: 80, display: 'inline-block' }}>{t('product.quotaLimit')}:</span>
              <InputNumber size="small" min={0} max={100} value={newKeyData.priority} onChange={(v) => setNewKeyData({ ...newKeyData, priority: v ?? undefined })} />
              <span style={{ marginLeft: 8 }}>权重:</span>
              <InputNumber size="small" min={0} value={newKeyData.weight} onChange={(v) => setNewKeyData({ ...newKeyData, weight: v ?? undefined })} />
            </div>
            <Space>
              <Button size="small" type="primary" onClick={handleCreateKey} loading={createKeyMutation.isPending}>确定</Button>
              <Button size="small" onClick={() => { setAdding(false); setNewKeyData({}); }}>取消</Button>
            </Space>
          </Space>
        </Card>
      )}

      {apiKeys?.map((key) => (
        <Card key={key.id} size="small" style={{ marginBottom: 4 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Space>
              <KeyOutlined />
              <Typography.Text code>{key.apiKeyMasked}</Typography.Text>
              {key.baseUrl && <Tag>{key.baseUrl}</Tag>}
              <Tag>优先级: {key.priority}</Tag>
              <Tag>权重: {key.weight}</Tag>
              {key.state === 'active' ? <Tag color="green">{t('product.stateActive')}</Tag> : <Tag>{t('product.stateInactive')}</Tag>}
            </Space>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDeleteKey(key.id, key.apiKeyMasked)} />
          </div>
          {key.description && <div style={{ color: '#888', fontSize: 12, marginTop: 4 }}>{key.description}</div>}
        </Card>
      ))}

      <Modal
        title="API Key 创建成功"
        open={!!createdKey}
        onOk={() => setCreatedKey(null)}
        onCancel={() => setCreatedKey(null)}
      >
        <Typography.Paragraph>
          请妥善保存以下 API Key，关闭后将无法再次查看：
        </Typography.Paragraph>
        <Typography.Paragraph copyable={{ text: createdKey ?? '' }} code>
          {createdKey}
        </Typography.Paragraph>
      </Modal>
    </div>
  );
}

export default function ProviderProductsTab({ providerId }: ProviderProductsTabProps) {
  const { t } = useTranslation('products');
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  const { data: products, isLoading } = useProducts(providerId);
  const deleteMutation = useDeleteProduct();

  const [formVisible, setFormVisible] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | undefined>();

  const handleAdd = () => {
    setEditingProduct(undefined);
    setFormVisible(true);
  };

  const handleEdit = (product: Product) => {
    setEditingProduct(product);
    setFormVisible(true);
  };

  const handleDelete = (product: Product) => {
    if (products && products.length <= 1) {
      message.warning(t('product.lastProductWarning'));
      return;
    }
    modal.confirm({
      title: t('product.deleteProduct'),
      content: t('product.deleteConfirm', { name: product.name }),
      okType: 'danger',
      onOk: () => deleteMutation.mutateAsync({ id: product.id, providerId }),
    });
  };

  if (isLoading) return <Spin />;

  return (
    <>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('product.addProduct')}
          </Button>
        )}
      </div>

      {!products?.length ? (
        <Empty description={t('product.title')} />
      ) : (
        <Collapse
          defaultActiveKey={products.map((p) => String(p.id))}
          items={products.map((product) => ({
            key: String(product.id),
            label: (
              <Space>
                {product.name}
                <Tag color={PRODUCT_TYPE_COLOR[product.productType]}>
                  {t(`product.${TYPE_LABEL_KEY[product.productType]}`)}
                </Tag>
                <Tag>{product.models?.length ?? 0} 模型</Tag>
              </Space>
            ),
            extra: canWrite ? (
              <Space onClick={(e) => e.stopPropagation()}>
                <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(product)} />
                <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(product)} />
              </Space>
            ) : undefined,
            children: (
              <div>
                <p>{t('product.models')}: {product.models?.join(', ') || '-'}</p>
                <p>
                  {t('product.endpoints')}:{' '}
                  {Object.entries(product.endpoints || {})
                    .map(([k, v]) => `${k}: ${v}`)
                    .join(', ') || '-'}
                </p>
                {product.quotaLimit != null && <p>{t('product.quotaLimit')}: {product.quotaLimit}</p>}
                <ProductApiKeySection productId={product.id} />
              </div>
            ),
          }))}
        />
      )}

      <ProductFormModal
        visible={formVisible}
        providerId={providerId}
        product={editingProduct}
        onClose={() => setFormVisible(false)}
      />
    </>
  );
}