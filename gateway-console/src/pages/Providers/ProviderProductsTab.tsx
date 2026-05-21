import { useState } from 'react';
import { Button, Empty, Spin, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useProducts, useDeleteProduct } from '@/services/query/useProducts';
import type { Product } from '@/types/product';
import ProductCard from './ProductCard';
import { ProductFormModal } from './ProductFormModal';

interface ProviderProductsTabProps {
  providerId: number;
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
      content: t('product.deleteConfirm', { name: product.productName }),
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
        products.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            onEdit={() => handleEdit(product)}
            onDelete={() => handleDelete(product)}
          />
        ))
      )}

      <ProductFormModal
        open={formVisible}
        providerId={providerId}
        providerName={editingProduct?.providerName ?? ''}
        editingProduct={editingProduct ?? null}
        onClose={() => setFormVisible(false)}
        onSaved={() => setFormVisible(false)}
      />
    </>
  );
}
