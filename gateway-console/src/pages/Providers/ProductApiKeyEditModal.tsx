import { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { useUpdateChannelCredential } from '@/services/query/useProducts';
import type { ProductApiKey, UpdateProductApiKeyRequest } from '@/types/product';

interface ProductApiKeyEditModalProps {
  open: boolean;
  productId: number;
  apiKey?: ProductApiKey;
  onClose: () => void;
}

export default function ProductApiKeyEditModal({ open, productId, apiKey, onClose }: ProductApiKeyEditModalProps) {
  const { t } = useTranslation('products');
  const [form] = Form.useForm<UpdateProductApiKeyRequest>();
  const updateMutation = useUpdateChannelCredential();

  useEffect(() => {
    if (open && apiKey) {
      form.setFieldsValue({
        priority: apiKey.priority,
        weight: apiKey.weight,
        description: apiKey.description,
        state: apiKey.state,
      });
    }
  }, [open, apiKey, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await updateMutation.mutateAsync({ channelId: productId, id: apiKey!.id, data: values });
    onClose();
  };

  return (
    <Modal
      title={t('product.editApiKey')}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={updateMutation.isPending}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="priority" label={t('product.priorityLabel')}>
          <InputNumber min={0} max={100} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="weight" label={t('product.weightLabel')}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="description" label={t('product.description')}>
          <Input.TextArea rows={2} />
        </Form.Item>
        <Form.Item name="state" label={t('product.state')}>
          <Select>
            <Select.Option value="ACTIVE">{t('product.stateActive')}</Select.Option>
            <Select.Option value="INACTIVE">{t('product.stateInactive')}</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
}
