/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useEffect } from 'react';
import { Drawer, Form, Input, InputNumber, Select, Tag, Switch, Button, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useUpdateModel, useSetEnabledModel } from '@/services/query/useModels';
import type { Model } from '@/types/model';

interface Props {
  open: boolean;
  model: Model | null;
  onClose: () => void;
}

const capabilityOptions = [
  { value: 'vision', label: '图像识别' },
  { value: 'function_calling', label: '函数调用' },
  { value: 'streaming', label: '流式' },
];

const modalityOptions = [
  { value: 'text', label: '文本' },
  { value: 'image', label: '图像' },
  { value: 'audio', label: '音频' },
];

export default function ModelEditDrawer({ open, model, onClose }: Props) {
  const { t } = useTranslation('models');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const updateMutation = useUpdateModel();
  const setEnabledMutation = useSetEnabledModel();

  useEffect(() => {
    if (open && model) {
      form.setFieldsValue({
        modelName: model.modelName,
        displayName: model.displayName,
        modelFamily: model.modelFamily,
        contextWindow: model.contextWindow,
        capabilities: model.capabilities ? Object.entries(model.capabilities).filter(([, v]) => v).map(([k]) => k) : [],
        modalities: model.modalities || [],
      });
    }
  }, [open, model, form]);

  const handleSave = async () => {
    if (!model) return;
    try {
      const values = await form.validateFields();
      await updateMutation.mutateAsync({
        id: model.id,
        data: {
          ...values,
          capabilities: values.capabilities?.reduce((acc: Record<string, boolean>, k: string) => {
            acc[k] = true;
            return acc;
          }, {}) || {},
        },
      });
      message.success(t('updated', { defaultValue: '模型更新成功' }));
      onClose();
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return;
      message.error(t('updateFailed', { defaultValue: '更新失败' }));
    }
  };

  const handleToggleState = async (checked: boolean) => {
    if (!model) return;
    try {
      await setEnabledMutation.mutateAsync({ id: model.id, enabled: checked });
      message.success(checked
        ? t('enabled', { defaultValue: '模型已启用' })
        : t('disabled', { defaultValue: '模型已禁用' }));
    } catch {
      message.error(t('stateToggleFailed', { defaultValue: '状态切换失败' }));
    }
  };

  return (
    <Drawer
      title={t('editModel', { defaultValue: '编辑模型' })}
      open={open}
      onClose={onClose}
      width={560}
      footer={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button onClick={onClose}>{t('cancel', { defaultValue: '取消' })}</Button>
          <Button type="primary" onClick={handleSave} loading={updateMutation.isPending}>
            {t('save', { defaultValue: '保存' })}
          </Button>
        </div>
      }
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <span style={{ fontSize: 13, color: '#64748b' }}>{t('state', { defaultValue: '状态' })}</span>
        <Switch
          checked={model?.state === 'ACTIVE'}
          onChange={handleToggleState}
          checkedChildren={t('active', { defaultValue: '启用' })}
          unCheckedChildren={t('inactive', { defaultValue: '禁用' })}
        />
      </div>
      <Form form={form} layout="vertical">
        <Form.Item name="modelName" label={t('modelName', { defaultValue: '模型标识' })} rules={[{ required: true }]}>
          <Input placeholder="gpt-4o" />
        </Form.Item>
        <Form.Item name="displayName" label={t('displayName', { defaultValue: '显示名称' })}>
          <Input placeholder="GPT-4o" />
        </Form.Item>
        <Form.Item name="modelFamily" label={t('modelFamily', { defaultValue: '模型族' })}>
          <Input placeholder="gpt-4" />
        </Form.Item>
        <Form.Item name="contextWindow" label={t('contextWindow', { defaultValue: '上下文窗口' })}>
          <InputNumber style={{ width: '100%' }} placeholder="128000" />
        </Form.Item>
        <Form.Item name="capabilities" label={t('capabilities', { defaultValue: '能力' })}>
          <Select mode="multiple" options={capabilityOptions} tagRender={(props) => <Tag closable={props.closable} onClose={props.onClose}>{props.label}</Tag>} />
        </Form.Item>
        <Form.Item name="modalities" label={t('modalities', { defaultValue: '模态' })}>
          <Select mode="multiple" options={modalityOptions} />
        </Form.Item>
      </Form>
      <div style={{ marginTop: 16, fontSize: 12, color: '#94a3b8' }}>
        {t('createdAt', { defaultValue: '创建时间' })}: {model?.createdAt ? new Date(model.createdAt).toLocaleString('zh-CN') : '-'}
      </div>
    </Drawer>
  );
}
