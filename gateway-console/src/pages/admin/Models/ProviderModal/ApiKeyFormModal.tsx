import { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Space, Button, Switch } from 'antd';
import { useTranslation } from 'react-i18next';

export interface ApiKeyFormItem {
  id?: number;
  keyName: string;
  keyHint?: string;
  apiKey: string;
  priority?: number;
  weight?: number;
  isDefault?: boolean;
  state?: string;
}

interface ApiKeyFormModalProps {
  open: boolean;
  editingKey: ApiKeyFormItem | null;
  onClose: () => void;
  onSubmit: (values: ApiKeyFormItem) => void;
}

/**
 * API Key 表单弹窗
 */
export function ApiKeyFormModal({
  open,
  editingKey,
  onClose,
  onSubmit,
}: ApiKeyFormModalProps) {
  const { t } = useTranslation('models');
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      if (editingKey) {
        form.setFieldsValue({
          keyName: editingKey.keyName,
          apiKey: editingKey.apiKey,
          priority: editingKey.priority || 100,
          weight: editingKey.weight || 100,
          isDefault: editingKey.isDefault || false,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ priority: 100, weight: 100, isDefault: false });
      }
    }
  }, [open, editingKey, form]);

  const handleSubmit = async (values: ApiKeyFormItem) => {
    onSubmit({
      ...editingKey,
      keyName: values.keyName,
      apiKey: values.apiKey,
      priority: values.priority,
      weight: values.weight,
      isDefault: values.isDefault,
    });
  };

  return (
    <Modal
      title={editingKey ? t('actions.edit', { ns: 'common' }) : t('provider.addApiKey', { defaultValue: '添加 API Key' })}
      open={open}
      onCancel={onClose}
      footer={null}
      width={480}
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item
          name="keyName"
          label={t('provider.keyName', { defaultValue: 'Key 名称' })}
          rules={[{ required: true }]}
        >
          <Input placeholder="Production Key" />
        </Form.Item>

        <Form.Item
          name="apiKey"
          label={t('provider.apiKey', { defaultValue: 'API Key' })}
          rules={[{ required: !editingKey?.id }]}
        >
          <Input.Password placeholder="sk-..." disabled={!!editingKey?.id} />
        </Form.Item>

        <Form.Item name="priority" label={t('provider.priority', { defaultValue: '优先级' })}>
          <InputNumber style={{ width: '100%' }} min={1} max={1000} />
        </Form.Item>

        <Form.Item name="weight" label={t('provider.weight', { defaultValue: '权重' })}>
          <InputNumber style={{ width: '100%' }} min={1} max={1000} />
        </Form.Item>

        <Form.Item name="isDefault" label={t('provider.isDefault', { defaultValue: '设为默认' })} valuePropName="checked">
          <Switch />
        </Form.Item>

        <Form.Item style={{ marginBottom: 0 }}>
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={onClose}>
              {t('actions.cancel', { ns: 'common' })}
            </Button>
            <Button type="primary" htmlType="submit">
              {t('actions.save', { ns: 'common' })}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
}
