/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Modal, Form, Input, InputNumber, Select, Tag, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateModel } from '@/services/query/useModels';
import { useProviders } from '@/services/query/useProviders';
import { useChannels, useCreateChannelModel } from '@/services/query/useChannels';
import type { CreateModelRequest } from '@/types/model';

interface Props {
  open: boolean;
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

export default function ModelCreateModal({ open, onClose }: Props) {
  const { t } = useTranslation('models');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const createMutation = useCreateModel();
  const createChannelModelMutation = useCreateChannelModel();

  const { data: providersPage } = useProviders();
  const providers = providersPage?.items ?? [];

  const selectedProviderId = Form.useWatch('providerId', form);
  const { data: channels } = useChannels(selectedProviderId || 0);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      const { providerId, ...modelFields } = values;
      const payload: CreateModelRequest = {
        ...modelFields,
        capabilities: modelFields.capabilities?.reduce((acc: Record<string, boolean>, k: string) => {
          acc[k] = true;
          return acc;
        }, {}) || {},
      };
      const model = await createMutation.mutateAsync(payload);

      // 自动关联到供应商的第一个通道
      if (providerId && channels?.length) {
        const firstChannel = channels[0];
        await createChannelModelMutation.mutateAsync({
          channelId: firstChannel.id,
          data: {
            modelId: model.id,
            upstreamModelName: model.modelName,
          },
        });
      }

      message.success(t('created', { defaultValue: '模型创建成功' }));
      form.resetFields();
      onClose();
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return;
      const errMsg = e instanceof Error ? e.message : '';
      message.error(errMsg || t('createFailed', { defaultValue: '创建失败' }));
    }
  };

  return (
    <Modal
      title={t('createModel', { defaultValue: '新增模型' })}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      width={560}
      confirmLoading={createMutation.isPending}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="providerId"
          label={t('provider', { defaultValue: '归属供应商' })}
          rules={[{ required: true, message: t('providerRequired', { defaultValue: '请选择归属供应商' }) }]}
        >
          <Select
            placeholder={t('selectProvider', { defaultValue: '选择供应商' })}
            showSearch
            optionFilterProp="label"
            options={providers.map((p) => ({
              value: p.id,
              label: p.providerName,
            }))}
          />
        </Form.Item>
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
    </Modal>
  );
}
