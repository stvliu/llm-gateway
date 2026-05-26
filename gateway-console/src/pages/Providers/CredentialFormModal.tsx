import { useEffect, useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Alert, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateChannelCredential } from '@/services/query/useChannels';

interface CredentialFormModalProps {
  open: boolean;
  channelId: number;
  onClose: () => void;
}

/** 凭证创建弹窗，创建成功后展示一次明文 Key */
export default function CredentialFormModal({ open, channelId, onClose }: CredentialFormModalProps) {
  const { t } = useTranslation('providers');
  const [form] = Form.useForm();
  const createMutation = useCreateChannelCredential();
  const [createdKey, setCreatedKey] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      form.resetFields();
      setCreatedKey(null);
    }
  }, [open, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const result = await createMutation.mutateAsync({
      channelId,
      data: { ...values, channelId },
    });
    setCreatedKey(result.apiKeyPlain);
  };

  const handleClose = () => {
    setCreatedKey(null);
    onClose();
  };

  return (
    <Modal
      title={t('credential.add')}
      open={open}
      onCancel={handleClose}
      onOk={createdKey ? undefined : handleSubmit}
      okText={createdKey ? undefined : undefined}
      footer={createdKey ? [
        <Button key="close" type="primary" onClick={handleClose}>
          {t('actions.close', { ns: 'common' })}
        </Button>,
      ] : undefined}
      confirmLoading={createMutation.isPending}
      destroyOnClose
    >
      {createdKey ? (
        <Alert
          type="success"
          message={t('credential.createdSuccess')}
          description={
            <div>
              <Typography.Paragraph type="warning" strong>
                {t('credential.createdHint')}
              </Typography.Paragraph>
              <Typography.Text code copyable>{createdKey}</Typography.Text>
            </div>
          }
        />
      ) : (
        <Form form={form} layout="vertical">
          <Form.Item name="name" label={t('credential.name')}>
            <Input />
          </Form.Item>
          <Form.Item
            name="apiKey"
            label={t('credential.apiKey')}
            rules={[{ required: true, message: t('credential.apiKeyRequired') as string }]}
          >
            <Input.Password placeholder="sk-..." />
          </Form.Item>
          <Form.Item name="priority" label={t('channel.priority')}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="weight" label={t('channel.weight')}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label={t('credential.description')}>
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}