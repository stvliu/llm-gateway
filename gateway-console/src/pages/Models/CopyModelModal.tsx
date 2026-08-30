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
import { Modal, Form, Input, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCopyModel } from '@/services/query/useModels';
import { extractErrorMessage } from '@/utils/errorMessage';
import type { Model } from '@/types/model';

interface Props {
  open: boolean;
  /** 源模型（null 时不渲染表单） */
  source: Model | null;
  onClose: () => void;
  /** 复制成功回调（携带新模型） */
  onCopied: (created: Model) => void;
}

/** 复制模型对话框：预填源模型字段，提交调用复制接口 */
export default function CopyModelModal({ open, source, onClose, onCopied }: Props) {
  const { t } = useTranslation('models');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const copyMutation = useCopyModel();

  const handleOk = async () => {
    if (!source) return;
    try {
      const values = await form.validateFields();
      const created = await copyMutation.mutateAsync({
        id: source.id,
        data: {
          modelName: values.modelName,
          displayName: values.displayName,
          modelFamily: values.modelFamily,
        },
      });
      message.success(t('copied', { defaultValue: '模型复制成功' }));
      form.resetFields();
      onClose();
      onCopied(created);
    } catch (e: unknown) {
      // extractErrorMessage 优先提取后端 ApiResponse.error.message（400 业务原因）；
      // 表单校验失败（errorFields）返回空串，跳过 toast（由表单就地展示行内错误）
      const errMsg = extractErrorMessage(e);
      if (!errMsg) return;
      message.error(errMsg);
    }
  };

  return (
    <Modal
      title={t('copyModel', { defaultValue: '复制模型' })}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={copyMutation.isPending}
      destroyOnHidden
      width={480}
    >
      {source && (
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            modelName: source.modelName,
            displayName: source.displayName,
            modelFamily: source.modelFamily,
          }}
        >
          <Form.Item
            name="modelName"
            label={t('modelName', { defaultValue: '模型标识' })}
            rules={[{ required: true, message: t('modelNameRequired', { defaultValue: '请输入模型标识' }) }]}
          >
            <Input placeholder="gpt-4o" />
          </Form.Item>
          <Form.Item name="displayName" label={t('displayName', { defaultValue: '显示名称' })}>
            <Input />
          </Form.Item>
          <Form.Item name="modelFamily" label={t('modelFamily', { defaultValue: '模型族' })}>
            <Input />
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}
