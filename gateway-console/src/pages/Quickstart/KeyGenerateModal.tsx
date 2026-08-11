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
import { Modal, App, Result, Button, Typography, Form, Select } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useCreateUserApiKey } from '@/services/query/useUserApiKeys';
import { useApplications } from '@/services/query/useApplications';
import { useAuthStore } from '@/stores/authStore';
import type { CreateUserApiKeyResponse } from '@/types/userApiKey';
import type { Application } from '@/types/application';

const { Paragraph } = Typography;

interface Props {
  open: boolean;
  onClose: () => void;
  onKeyCreated?: (key: string) => void;
}

export default function KeyGenerateModal({ open, onClose, onKeyCreated }: Props) {
  const { t } = useTranslation('quickstart');
  const { message } = App.useApp();
  const { user } = useAuthStore();
  const createMutation = useCreateUserApiKey();
  const { data: applications } = useApplications();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CreateUserApiKeyResponse | null>(null);

  const handleCreate = async (values: { applicationId: number }) => {
    if (!user) return;
    setLoading(true);
    try {
      const res = await createMutation.mutateAsync({
        userId: user.id,
        applicationId: values.applicationId,
        name: t('keyNamePrefix', { date: new Date().toLocaleDateString() }),
      });
      setResult(res);
      onKeyCreated?.(res.keyPlain);
      message.success(t('keyCreated'));
    } catch {
      message.error(t('keyCreateFailed'));
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setResult(null);
    form.resetFields();
    onClose();
  };

  const handleCopyKey = async () => {
    try {
      await navigator.clipboard.writeText(result!.keyPlain);
      message.success(t('keyCopied'));
    } catch {
      message.error(t('keyCopyFailed'));
    }
  };

  return (
    <Modal
      title={t('createKey')}
      open={open}
      onCancel={handleClose}
      footer={result ? (
        <Button type="primary" onClick={handleClose}>
          {t('done')}
        </Button>
      ) : (
        <Button type="primary" loading={loading} onClick={() => form.submit()}>
          {t('generate')}
        </Button>
      )}
      width={560}
      destroyOnHidden
    >
      {result ? (
        <Result
          status="success"
          title={t('keyCreatedTitle')}
          subTitle={t('keySaveHint')}
          extra={[
            <div key="key" style={{ display: 'flex', alignItems: 'center', gap: 8, background: '#1e293b', color: '#e2e8f0', padding: '12px 16px', borderRadius: 8 }}>
              <span style={{ flex: 1, fontFamily: 'monospace', fontSize: 13, wordBreak: 'break-all' }}>
                {result.keyPlain}
              </span>
              <Button type="text" icon={<CopyOutlined />} style={{ color: '#e2e8f0' }} onClick={handleCopyKey} />
            </div>,
          ]}
        />
      ) : (
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Paragraph type="secondary">
            {t('createKeyHint')}
          </Paragraph>
          <Form.Item
            name="applicationId"
            label={t('application', { defaultValue: '所属应用' })}
            rules={[{ required: true, message: t('selectApplication', { defaultValue: '请选择应用' }) }]}
          >
            <Select
              showSearch
              placeholder={t('selectApplication', { defaultValue: '选择应用' })}
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(applications ?? []).map((a: Application) => ({
                label: `${a.name} (${a.id})`,
                value: a.id,
              }))}
            />
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}