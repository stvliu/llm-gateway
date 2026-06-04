import { useEffect } from 'react';
import { Modal, Form, Input } from 'antd';
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
      title="新增供应商"
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
          label="品牌标识"
          rules={[
            { required: true, message: '请输入品牌标识' },
            { pattern: /^[a-z0-9_-]+$/, message: '仅支持小写字母、数字、下划线、中划线' },
          ]}
          extra="全局唯一标识，如 openai、anthropic"
        >
          <Input placeholder="如 openai" />
        </Form.Item>
        <Form.Item
          name="providerName"
          label="供应商名称"
          rules={[{ required: true, message: '请输入供应商名称' }]}
        >
          <Input placeholder="如 OpenAI" />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="websiteUrl" label="官网地址">
          <Input placeholder="https://..." />
        </Form.Item>
        <Form.Item name="apiDocUrl" label="API 文档地址">
          <Input placeholder="https://..." />
        </Form.Item>
      </Form>
    </Modal>
  );
};
