import { useState } from 'react';
import { Modal, Form, Input, Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { authApi } from '@/services/api/auth';
import { useMessage } from '@/hooks/useMessage';

interface ChangePasswordModalProps {
  open: boolean;
  onClose: () => void;
}

export function ChangePasswordModal({ open, onClose }: ChangePasswordModalProps) {
  const message = useMessage();
  const { t } = useTranslation('common');
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const handleSubmit = async (values: { currentPassword: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error(t('changePassword.passwordMismatch'));
      return;
    }

    setLoading(true);
    try {
      await authApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      message.success(t('message.success'));
      form.resetFields();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : t('message.error');
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title={t('changePassword.title')}
      open={open}
      onCancel={handleCancel}
      footer={null}
      destroyOnClose
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item
          name="currentPassword"
          label={t('changePassword.currentPassword')}
          rules={[{ required: true }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label={t('changePassword.newPassword')}
          rules={[{ required: true, min: 6 }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label={t('changePassword.confirmPassword')}
          rules={[{ required: true }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Button onClick={handleCancel} style={{ marginRight: 8 }}>
            {t('actions.cancel')}
          </Button>
          <Button type="primary" htmlType="submit" loading={loading}>
            {t('actions.save')}
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
}
