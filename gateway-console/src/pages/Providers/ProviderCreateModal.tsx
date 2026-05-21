import { useState, useCallback } from 'react';
import { Modal, Form, Input, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateProvider } from '@/services/query';
import type { Provider, CreateProviderRequest } from '@/types/provider';

interface Props {
  open: boolean;
  providers: Provider[];
  onClose: () => void;
  onCreated: () => void;
}

export function ProviderCreateModal({ open, providers, onClose, onCreated }: Props) {
  const { t } = useTranslation('providers');
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const createMutation = useCreateProvider();
  const [saving, setSaving] = useState(false);

  const handleClose = useCallback(() => {
    form.resetFields();
    onClose();
  }, [form, onClose]);

  const handleCreate = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const name = values.providerName as string;

      if (providers.some(p => p.providerName.toLowerCase() === name.toLowerCase())) {
        message.warning(t('validation.nameDuplicate', { defaultValue: '供应商名称已存在' }));
        return;
      }

      setSaving(true);
      const request: CreateProviderRequest = {
        providerName: name,
        websiteUrl: values.websiteUrl,
        apiDocUrl: values.apiDocUrl,
        priority: values.priority,
      };

      await createMutation.mutateAsync(request);
      message.success(t('message.createSuccess', { defaultValue: '供应商创建成功' }));
      onCreated();
      handleClose();
    } catch {
      // 表单验证失败
    } finally {
      setSaving(false);
    }
  }, [form, providers, createMutation, message, t, onCreated, handleClose]);

  return (
    <Modal
      title={t('addProvider', { defaultValue: '新增供应商' })}
      open={open}
      onOk={handleCreate}
      onCancel={handleClose}
      confirmLoading={saving}
      width={560}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="providerName"
          label={t('form.providerName', { defaultValue: '供应商名称' })}
          rules={[{ required: true, message: t('validation.nameRequired', { defaultValue: '请输入供应商名称' }) }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="websiteUrl" label={t('form.websiteUrl', { defaultValue: '官网地址' })}>
          <Input />
        </Form.Item>
        <Form.Item name="apiDocUrl" label={t('form.apiDocUrl', { defaultValue: 'API 文档地址' })}>
          <Input />
        </Form.Item>
        <Form.Item name="priority" label={t('form.priority', { defaultValue: '优先级' })}>
          <Input type="number" />
        </Form.Item>
      </Form>
    </Modal>
  );
}