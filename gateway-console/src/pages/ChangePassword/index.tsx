import { Form, Input, Button, Card, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { authApi } from '@/services/api/auth';
import { useState } from 'react';

export default function ChangePassword() {
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
    } catch {
      message.error(t('message.error'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title={t('changePassword.title')} style={{ maxWidth: 500 }}>
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item name="currentPassword" label={t('changePassword.currentPassword')} rules={[{ required: true }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item name="newPassword" label={t('changePassword.newPassword')} rules={[{ required: true, min: 6 }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item name="confirmPassword" label={t('changePassword.confirmPassword')} rules={[{ required: true }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading}>
            {t('actions.save')}
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
}
