import { useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { MaskedKeyDisplay } from '../../components/MaskedKeyDisplay';

export interface ApiKeyEditModalProps {
  open: boolean;
  channelId: number;
  credentialId: number;
  keyPlain: string;
  onClose: () => void;
  onSuccess: () => void;
  onUpdate: (channelId: number, credentialId: number, data: { apiKey: string }) => Promise<void>;
}

export const ApiKeyEditModal: React.FC<ApiKeyEditModalProps> = ({
  open,
  channelId,
  credentialId,
  keyPlain,
  onClose,
  onSuccess,
  onUpdate,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await onUpdate(channelId, credentialId, { apiKey: values.apiKey });
      message.success('API Key 已更新');
      onSuccess();
      onClose();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'API Key 更新失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="替换 API Key"
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item label="当前 API Key">
          <MaskedKeyDisplay keyPlain={keyPlain} mode="readonly" showCopy={false} />
        </Form.Item>
        <Form.Item
          name="apiKey"
          label="新 API Key"
          rules={[{ required: true, message: '请输入新的 API Key' }]}
        >
          <Input.Password placeholder="sk-..." />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ApiKeyEditModal;
