import { useEffect } from 'react';
import { Modal, Form, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateProvider } from '@/services/query/useProviders';
import type { FC } from 'react';

interface ProviderCreateModalProps {
  open: boolean;
  onClose: () => void;
}

/**
 * 供应商创建弹窗
 * 创建纯供应商品牌信息，创建后需手动添加渠道
 */
export const ProviderCreateModal: FC<ProviderCreateModalProps> = ({ open, onClose }) => {
  const { t } = useTranslation('channels');
  const [form] = Form.useForm();
  const createProvider = useCreateProvider();

  useEffect(() => {
    if (open) {
      form.resetFields();
    }
  }, [open, form]);

  const handleCreate = async () => {
    const values = await form.validateFields();
    await createProvider.mutateAsync({
      code: values.code,
      providerName: values.providerName,
      description: values.description,
      websiteUrl: values.websiteUrl,
      apiDocUrl: values.apiDocUrl,
    });
    onClose();
  };

  return (
    <Modal
      title={t('providerCreate.title')}
      open={open}
      onCancel={onClose}
      onOk={handleCreate}
      confirmLoading={createProvider.isPending}
      width={480}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="code"
          label={t('providerCreate.code')}
          rules={[
            { required: true, message: t('providerCreate.codeRequired') },
            { pattern: /^[a-z0-9_-]+$/, message: t('providerCreate.codePattern') },
          ]}
          extra={t('providerCreate.codeExtra')}
        >
          <Input placeholder={t('providerCreate.codePlaceholder')} />
        </Form.Item>
        <Form.Item
          name="providerName"
          label={t('providerCreate.name')}
          rules={[{ required: true, message: t('providerCreate.nameRequired') }]}
        >
          <Input placeholder={t('providerCreate.namePlaceholder')} />
        </Form.Item>
        <Form.Item name="description" label={t('providerCreate.description')}>
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="websiteUrl" label={t('providerCreate.websiteUrl')}>
          <Input placeholder={t('providerCreate.websiteUrlPlaceholder')} />
        </Form.Item>
        <Form.Item name="apiDocUrl" label={t('providerCreate.apiDocUrl')}>
          <Input placeholder={t('providerCreate.apiDocUrlPlaceholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
};
