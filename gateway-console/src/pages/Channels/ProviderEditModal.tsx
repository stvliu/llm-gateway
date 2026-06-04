import { useEffect } from 'react';
import { Modal, Form, Input } from 'antd';
import { useUpdateProvider } from '@/services/query/useProviders';
import type { Provider } from '@/types/provider';
import type { FC } from 'react';

interface ProviderEditModalProps {
  open: boolean;
  provider: Provider | null;
  onClose: () => void;
}

/**
 * 供应商编辑弹窗
 * 轻量弹窗，仅修改供应商品牌信息
 */
export const ProviderEditModal: FC<ProviderEditModalProps> = ({ open, provider, onClose }) => {
  const [form] = Form.useForm();
  const updateProvider = useUpdateProvider();

  useEffect(() => {
    if (open && provider) {
      form.setFieldsValue({
        providerName: provider.providerName,
        description: provider.description,
        websiteUrl: provider.websiteUrl,
        apiDocUrl: provider.apiDocUrl,
      });
    }
  }, [open, provider, form]);

  const handleSave = async () => {
    if (!provider) return;
    const values = await form.validateFields();
    await updateProvider.mutateAsync({
      id: provider.id,
      data: {
        providerName: values.providerName,
        description: values.description,
        websiteUrl: values.websiteUrl,
        apiDocUrl: values.apiDocUrl,
      },
    });
    onClose();
  };

  return (
    <Modal
      title="编辑供应商"
      open={open}
      onCancel={onClose}
      onOk={handleSave}
      confirmLoading={updateProvider.isPending}
      width={480}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="providerName"
          label="供应商名称"
          rules={[{ required: true, message: '请输入供应商名称' }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="websiteUrl" label="官网地址">
          <Input />
        </Form.Item>
        <Form.Item name="apiDocUrl" label="API 文档地址">
          <Input />
        </Form.Item>
      </Form>
    </Modal>
  );
};
