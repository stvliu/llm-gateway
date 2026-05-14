import { useEffect, useCallback } from 'react';
import { Modal, Form, Input, InputNumber, Switch, Space, Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateProviderApiKey, useUpdateProviderApiKey } from '@/services/query';
import type { Provider } from '@/types/provider';
import type { ProviderApiKey } from '@/types/providerApiKey';

interface ApiKeyModalProps {
  open: boolean;
  provider: Provider | null;
  editingKey: ProviderApiKey | null;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * API Key 弹窗
 * 新增或编辑 API Key
 */
export function ApiKeyModal({ open, provider, editingKey, onClose, onSuccess }: ApiKeyModalProps) {
  const { t } = useTranslation('providers');
  const [form] = Form.useForm();

  const createMutation = useCreateProviderApiKey();
  const updateMutation = useUpdateProviderApiKey();

  useEffect(() => {
    if (open) {
      if (editingKey) {
        form.setFieldsValue({
          keyName: editingKey.keyName,
          apiKey: '',
          priority: editingKey.priority ?? 100,
          weight: editingKey.weight ?? 100,
          isDefault: editingKey.isDefault ?? false,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ priority: 100, weight: 100, isDefault: false });
      }
    }
  }, [open, editingKey, form]);

  const handleSubmit = useCallback(async (values: {
    keyName: string;
    apiKey: string;
    priority: number;
    weight: number;
    isDefault: boolean;
  }) => {
    if (!provider) return;

    try {
      if (editingKey) {
        await updateMutation.mutateAsync({
          id: editingKey.id,
          data: {
            keyName: values.keyName,
            priority: values.priority,
            weight: values.weight,
            isDefault: values.isDefault,
          },
        });
      } else {
        await createMutation.mutateAsync({
          providerId: provider.id,
          keyName: values.keyName,
          apiKey: values.apiKey,
          priority: values.priority,
          weight: values.weight,
          isDefault: values.isDefault,
        });
      }
      onSuccess();
    } catch (error) {
      console.error('Failed to save API Key:', error);
    }
  }, [provider, editingKey, createMutation, updateMutation, onSuccess]);

  return (
    <Modal
      title={editingKey
        ? t('provider.editApiKey', { defaultValue: '编辑 API Key' })
        : t('provider.addApiKey', { defaultValue: '添加 API Key' })
      }
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

        {!editingKey && (
          <Form.Item
            name="apiKey"
            label={t('provider.apiKey', { defaultValue: 'API Key' })}
            rules={[{ required: true }]}
          >
            <Input.Password placeholder="sk-..." />
          </Form.Item>
        )}

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
            <Button type="primary" htmlType="submit" loading={createMutation.isPending || updateMutation.isPending}>
              {t('actions.save', { ns: 'common' })}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
}