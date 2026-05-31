import { useState } from 'react';
import { Modal, Form, Input, Typography, Space, message } from 'antd';
import { MaskedKeyDisplay } from '../../components/MaskedKeyDisplay';

const { Text } = Typography;

export interface ApiKeyEditModalProps {
  open: boolean;
  channelId: number;
  credentialId: number;
  keyMasked: string;
  onClose: () => void;
  onSuccess: () => void;
  onUpdate: (channelId: number, credentialId: number, data: { apiKey: string }) => Promise<void>;
}

export const ApiKeyEditModal: React.FC<ApiKeyEditModalProps> = ({
  open,
  channelId,
  credentialId,
  keyMasked,
  onClose,
  onSuccess,
  onUpdate,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [newKeyMasked, setNewKeyMasked] = useState<string | null>(null);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await onUpdate(channelId, credentialId, { apiKey: values.apiKey });
      message.success('API Key 已更新');
      onSuccess();
      onClose();
    } catch {
      // 错误已在 mutation 中处理
    } finally {
      setLoading(false);
    }
  };

  const maskKey = (key: string): string => {
    if (!key || key.length < 12) return key + '****';
    return key.substring(0, 6) + '****' + key.substring(key.length - 4);
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
          <MaskedKeyDisplay keyMasked={keyMasked} mode="readonly" showCopy={false} />
        </Form.Item>
        <Form.Item
          name="apiKey"
          label="新 API Key"
          rules={[{ required: true, message: '请输入新的 API Key' }]}
        >
          <Input.Password
            placeholder="sk-..."
            onChange={(e) => {
              const val = e.target.value;
              setNewKeyMasked(val ? maskKey(val) : null);
            }}
          />
        </Form.Item>
        {newKeyMasked && (
          <Form.Item label="变更预览">
            <Space>
              <Text type="secondary">将替换：</Text>
              <Text code delete>{keyMasked}</Text>
              <Text type="secondary">→</Text>
              <Text code>{newKeyMasked}</Text>
            </Space>
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default ApiKeyEditModal;
