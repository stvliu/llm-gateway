import { useState, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Typography, Alert } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateProductApiKey } from '@/services/query/useProducts';
import type { CreateProductApiKeyRequest } from '@/types/product';

const { Paragraph } = Typography;

interface Props {
  open: boolean;
  productId: number;
  onClose: () => void;
  onSuccess: () => void;
}

export default function ProductApiKeyCreateModal({ open, productId, onClose, onSuccess }: Props) {
  const { t } = useTranslation('products');
  const [form] = Form.useForm<CreateProductApiKeyRequest>();
  const createMutation = useCreateProductApiKey();
  const [createdKey, setCreatedKey] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      form.resetFields();
      setCreatedKey(null);
    }
  }, [open, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const result = await createMutation.mutateAsync({ productId, data: values });
    setCreatedKey(result.apiKeyPlain);
  };

  // 创建成功后展示密钥
  if (createdKey) {
    return (
      <Modal
        title={t('product.apiKeyCreated')}
        open={open}
        onOk={onSuccess}
        onCancel={onSuccess}
        okText={t('common:confirm')}
        cancelButtonProps={{ style: { display: 'none' } }}
      >
        <Alert
          type="warning"
          message={t('product.apiKeyCreatedHint')}
          style={{ marginBottom: 16 }}
        />
        <Paragraph copyable={{ text: createdKey }} code>
          {createdKey}
        </Paragraph>
      </Modal>
    );
  }

  return (
    <Modal
      title={t('product.addApiKey')}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={createMutation.isPending}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="apiKey"
          label={t('product.apiKeyLabel')}
          rules={[{ required: true, message: t('product.apiKeyRequired') }]}
        >
          <Input.Password placeholder="sk-..." />
        </Form.Item>
        <Form.Item name="name" label={t('product.apiKeyName')}>
          <Input placeholder={t('product.apiKeyName')} />
        </Form.Item>
        <Form.Item name="priority" label={t('product.priorityLabel')}>
          <InputNumber min={0} max={100} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="weight" label={t('product.weightLabel')}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="description" label={t('product.description')}>
          <Input.TextArea rows={2} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
