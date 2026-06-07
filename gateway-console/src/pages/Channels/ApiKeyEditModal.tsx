import { useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { useTranslation } from 'react-i18next';
import type { UpdateChannelCredentialRequest } from '@/types/channel';

interface ApiKeyEditModalProps {
  open: boolean;
  channelId: number;
  credentialId: number;
  keyPlain: string;
  onClose: () => void;
  onSuccess: () => void;
  onUpdate: (channelId: number, credentialId: number, data: UpdateChannelCredentialRequest) => Promise<unknown>;
}

/**
 * API Key 替换弹窗
 * 允许用户替换某个凭证的 API Key
 */
export function ApiKeyEditModal({
  open,
  channelId,
  credentialId,
  keyPlain,
  onClose,
  onSuccess,
  onUpdate,
}: ApiKeyEditModalProps) {
  const { t } = useTranslation('channels');
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSave = async () => {
    try {
      setLoading(true);
      const values = await form.validateFields();
      await onUpdate(channelId, credentialId, {
        apiKey: values.newApiKey,
      });
      message.success(t('apiKeyModal.updateSuccess'));
      onSuccess();
      onClose();
    } catch {
      message.error(t('apiKeyModal.updateFail'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={t('apiKeyModal.title')}
      open={open}
      onOk={handleSave}
      onCancel={onClose}
      confirmLoading={loading}
      okText={t('drawer.save')}
      cancelText={t('drawer.cancel')}
    >
      <Form form={form} layout="vertical">
        <Form.Item label={t('apiKeyModal.currentKey')}>
          <Input
            value={keyPlain ? `${keyPlain.substring(0, 8)}...${keyPlain.substring(keyPlain.length - 4)}` : '****'}
            disabled
          />
        </Form.Item>
        <Form.Item
          name="newApiKey"
          label={t('apiKeyModal.newKey')}
          rules={[{ required: true, message: t('apiKeyModal.newKeyRequired') }]}
        >
          <Input.Password placeholder="sk-..." />
        </Form.Item>
      </Form>
    </Modal>
  );
}
