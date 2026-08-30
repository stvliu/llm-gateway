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
import { Modal, Form, Input, Checkbox, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCopyChannel } from '@/services/query/useChannels';
import { extractErrorMessage } from '@/utils/errorMessage';
import type { ChannelCard } from '@/types/channel';

interface Props {
  open: boolean;
  /** 源渠道（null 时不渲染表单） */
  source: ChannelCard | null;
  onClose: () => void;
}

/** 复制渠道对话框：预填源渠道名称，凭证（API Key）复制由复选框控制（默认不勾选） */
export default function CopyChannelModal({ open, source, onClose }: Props) {
  const { t } = useTranslation('channels');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const copyMutation = useCopyChannel();

  const handleOk = async () => {
    if (!source) return;
    try {
      const values = await form.validateFields();
      await copyMutation.mutateAsync({
        id: source.id,
        data: {
          name: values.name,
          copyCredentials: !!values.copyCredentials,
        },
      });
      message.success(t('channel.copy.copied', { defaultValue: '渠道复制成功' }));
      form.resetFields();
      onClose();
    } catch (e: unknown) {
      // extractErrorMessage 优先提取后端 ApiResponse.error.message（409 重名等业务原因）；
      // 表单校验失败（errorFields）返回空串，跳过 toast（由表单就地展示行内错误）
      const errMsg = extractErrorMessage(e);
      if (!errMsg) return;
      message.error(errMsg);
    }
  };

  return (
    <Modal
      title={t('channel.copy.title', { defaultValue: '复制渠道' })}
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
            name: source.name,
            copyCredentials: false,
          }}
        >
          <Form.Item
            name="name"
            label={t('channel.copy.newName', { defaultValue: '新渠道名称' })}
            rules={[
              {
                required: true,
                message: t('channel.copy.newNameRequired', { defaultValue: '请输入新渠道名称' }),
              },
            ]}
          >
            <Input placeholder={source.name} />
          </Form.Item>
          <Form.Item name="copyCredentials" valuePropName="checked" style={{ marginBottom: 0 }}>
            <Checkbox>
              {t('channel.copy.copyCredentials', { defaultValue: '同时复制 API Key（凭证）' })}
            </Checkbox>
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}
