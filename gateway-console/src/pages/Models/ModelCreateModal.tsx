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
import { useState } from 'react';
import { Modal, Form, Input, InputNumber, Select, Tag, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateModel, useModels } from '@/services/query/useModels';
import { useProviders } from '@/services/query/useProviders';
import { useChannels, useCreateChannelModel } from '@/services/query/useChannels';
import type { CreateModelRequest, Model } from '@/types/model';
import CopyModelModal from './CopyModelModal';

interface Props {
  open: boolean;
  onClose: () => void;
}

/** 按模型族分组 + 名称搜索的源模型选择项（unknownFamily 为未分组的兜底文案） */
function groupByFamily(
  models: Model[],
  unknownFamily: string,
): { label: string; options: { value: number; label: string }[] }[] {
  const groups = new Map<string, { value: number; label: string }[]>();
  for (const m of models) {
    const family = m.modelFamily || unknownFamily;
    const list = groups.get(family) ?? [];
    list.push({ value: m.id, label: `${m.modelName}${m.displayName ? ` (${m.displayName})` : ''}` });
    groups.set(family, list);
  }
  return [...groups.entries()].map(([family, options]) => ({
    label: family,
    options: options.sort((a, b) => a.label.localeCompare(b.label)),
  }));
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
  // 复制选择器选中的源模型（非空时打开复制对话框）
  const [copySource, setCopySource] = useState<Model | null>(null);

  const { data: providersPage } = useProviders();
  const providers = providersPage?.items ?? [];

  // 全量模型列表供「从已有模型复制」选择器使用（按模型族分组 + 名称搜索）
  const { data: allModelsPage } = useModels({ limit: 1000 });
  const allModels = allModelsPage?.items ?? [];

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
        {/* 从已有模型复制选择器：按模型族分组 + 名称搜索，选中后打开复制对话框 */}
        <Form.Item label={t('copyFrom', { defaultValue: '从已有模型复制' })}>
          <Select
            allowClear
            placeholder={t('copyFromPlaceholder', { defaultValue: '选择源模型（可选）' })}
            showSearch
            optionFilterProp="label"
            value={copySource?.id}
            onChange={(sourceId: number | undefined) => {
              const source = allModels.find((m) => m.id === sourceId);
              setCopySource(source ?? null);
            }}
            options={groupByFamily(allModels, t('unknownFamily', { defaultValue: '未分组' }))}
          />
        </Form.Item>
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
      {/* 复制对话框：成功提示已由 CopyModelModal 内部展示，这里只关闭选择器 */}
      <CopyModelModal
        open={!!copySource}
        source={copySource}
        onClose={() => setCopySource(null)}
        onCopied={() => setCopySource(null)}
      />
    </Modal>
  );
}
