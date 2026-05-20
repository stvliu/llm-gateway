import { useEffect } from 'react';
import { Modal, Form, Input, Select, InputNumber, Button, Space, App } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useCreateProduct, useUpdateProduct } from '@/services/query/useProducts';
import type { Product } from '@/types/product';

interface ProductFormModalProps {
  visible: boolean;
  providerId: number;
  product?: Product;
  onClose: () => void;
}

const PRODUCT_TYPES = [
  { value: 'pay_as_you_go', labelKey: 'typePayAsYouGo' },
  { value: 'subscription_coding', labelKey: 'typeSubscriptionCoding' },
  { value: 'subscription_token', labelKey: 'typeSubscriptionToken' },
];

export default function ProductFormModal({ visible, providerId, product, onClose }: ProductFormModalProps) {
  const { t } = useTranslation('products');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const isEdit = !!product;

  const createMutation = useCreateProduct();
  const updateMutation = useUpdateProduct();
  const loading = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (visible) {
      if (product) {
        form.setFieldsValue({
          name: product.name,
          productType: product.productType,
          models: product.models?.join(', '),
          quotaLimit: product.quotaLimit,
          endpoints: Object.entries(product.endpoints || {}).map(([key, url]) => ({ key, url })),
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ endpoints: [{ key: '', url: '' }] });
      }
    }
  }, [visible, product, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const endpoints: Record<string, string> = {};
    (values.endpoints || []).forEach((e: { key: string; url: string }) => {
      if (e.key && e.url) endpoints[e.key] = e.url;
    });
    const models = values.models
      ? values.models.split(',').map((m: string) => m.trim()).filter(Boolean)
      : [];

    const payload = {
      providerId,
      name: values.name,
      productType: values.productType,
      models,
      endpoints,
      quotaLimit: values.quotaLimit,
    };

    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: product!.id, data: payload });
        message.success(t('product.editProduct'));
      } else {
        await createMutation.mutateAsync(payload);
        message.success(t('product.addProduct'));
      }
      onClose();
    } catch {
      message.error(isEdit ? t('product.editProduct') : t('product.addProduct'));
    }
  };

  return (
    <Modal
      title={isEdit ? t('product.editProduct') : t('product.addProduct')}
      open={visible}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnHidden
      width={600}
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label={t('product.name')} rules={[{ required: true, message: t('product.nameRequired') }]}>
          <Input />
        </Form.Item>

        <Form.Item name="productType" label={t('product.type')} rules={[{ required: true, message: t('product.typeRequired') }]}>
          <Select>
            {PRODUCT_TYPES.map((pt) => (
              <Select.Option key={pt.value} value={pt.value}>
                {t(`product.${pt.labelKey}`)}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item name="models" label={t('product.models')} extra={t('product.modelsSeparatorHint')}>
          <Input placeholder="gpt-4o, gpt-4o-mini" />
        </Form.Item>

        <Form.Item label={t('product.endpoints')} required>
          <Form.List name="endpoints">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...rest }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item {...rest} name={[name, 'key']} rules={[{ required: true }]}>
                      <Input placeholder="Key" style={{ width: 120 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, 'url']} rules={[{ required: true }]}>
                      <Input placeholder="https://api.example.com" style={{ width: 300 }} />
                    </Form.Item>
                    {fields.length > 1 && (
                      <MinusCircleOutlined onClick={() => remove(name)} />
                    )}
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  {t('product.addEndpoint')}
                </Button>
              </>
            )}
          </Form.List>
        </Form.Item>

        <Form.Item name="quotaLimit" label={t('product.quotaLimit')}>
          <InputNumber style={{ width: '100%' }} min={0} />
        </Form.Item>
      </Form>
    </Modal>
  );
}