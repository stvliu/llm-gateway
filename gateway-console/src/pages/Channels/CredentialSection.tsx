import { useState, useEffect, useCallback } from 'react';
import { Tag, Input, InputNumber, Button, Space, Form, message, theme } from 'antd';
import { useTranslation } from 'react-i18next';
import { InlineEditableList } from './InlineEditableList';
import { MaskedKeyDisplay } from '@/components/MaskedKeyDisplay';
import { ApiKeyEditModal } from './ApiKeyEditModal';
import type { ChannelCredential, CreateChannelCredentialRequest, UpdateChannelCredentialRequest } from '@/types/channel';
import {
  useCreateChannelCredential,
  useUpdateChannelCredential,
  useDeleteChannelCredential,
  useTestChannelCredential,
} from '@/services/query/useChannels';

interface CredentialSectionProps {
  channelId: number;
  credentials: ChannelCredential[];
}

/**
 * API Key 区组件
 * 展示渠道的凭证列表，支持行内编辑和测试
 */
export function CredentialSection({ channelId, credentials }: CredentialSectionProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [testingId, setTestingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);

  // Mutations
  const createCredential = useCreateChannelCredential();
  const updateCredential = useUpdateChannelCredential();
  const deleteCredential = useDeleteChannelCredential();
  const testCredential = useTestChannelCredential();

  /** 编辑时同步表单值 */
  useEffect(() => {
    if (editingId !== null) {
      const credential = credentials.find(c => c.id === editingId);
      if (credential) {
        form.setFieldsValue({
          priority: credential.priority,
          weight: credential.weight,
          description: credential.description || '',
        });
      }
    }
  }, [editingId, credentials, form]);

  /** 状态点颜色 */
  const getStateColor = (state: string) => {
    return state === 'ACTIVE' ? 'green' : 'default';
  };

  /** 渲染展示行 */
  const renderItem = useCallback((credential: ChannelCredential) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
      <MaskedKeyDisplay
        keyPlain={credential.apiKeyPlain}
        mode="editable"
        size="small"
        onEdit={() => setEditingId(credential.id)}
      />
      <Tag color="blue">P{credential.priority}</Tag>
      <Tag color="purple">W{credential.weight}</Tag>
      <span style={{ color: token.colorTextSecondary, fontSize: 12 }}>
        {t('credential.lastUsed')}: {t('credential.noData')}
      </span>
      <Tag color={getStateColor(credential.state)}>
        {credential.state === 'ACTIVE' ? t('status.active') : t('status.inactive')}
      </Tag>
      <Button
        type="link"
        size="small"
        loading={testingId === credential.id}
        onClick={async () => {
          setTestingId(credential.id);
          try {
            const result = await testCredential.mutateAsync({
              channelId,
              id: credential.id,
            });
            if (result.success) {
              message.success(t('credential.testSuccess', { latency: result.latency }));
            } else {
              message.error(t('credential.testFail', { msg: result.error?.message || t('credential.unknownError') }));
            }
          } catch (error) {
            message.error(t('credential.testRequestFail'));
          } finally {
            setTestingId(null);
          }
        }}
      >
        {t('credential.test')}
      </Button>
    </div>
  ), [channelId, token.colorTextSecondary, testingId, testCredential, setEditingId, t]);

  /** 渲染编辑表单 */
  const renderEditForm = (
    credential: ChannelCredential,
    onSave: (updated: ChannelCredential) => void,
    onCancel: () => void
  ) => {
    const handleSave = async () => {
      try {
        setLoading(true);
        const values = await form.validateFields();
        const data: UpdateChannelCredentialRequest = {
          priority: values.priority,
          weight: values.weight,
          description: values.description,
        };
        const result = await updateCredential.mutateAsync({
          channelId,
          id: credential.id,
          data,
        });
        message.success(t('credential.updateSuccess'));
        onSave(result);
      } catch (error) {
        message.error(t('credential.updateFail'));
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item
          name="priority"
          label={t('credential.priority')}
          rules={[{ required: true, message: t('credential.priorityRequired') }]}
        >
          <InputNumber min={1} max={10} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item
          name="weight"
          label={t('credential.weight')}
          rules={[{ required: true, message: t('credential.weightRequired') }]}
        >
          <InputNumber min={1} max={100} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item name="description" label={t('credential.description')}>
          <Input style={{ width: 200 }} placeholder={t('credential.descriptionPlaceholder')} />
        </Form.Item>
        <Space>
          <Button type="primary" size="small" onClick={handleSave} loading={loading}>
            {t('drawer.save')}
          </Button>
          <Button size="small" onClick={onCancel}>
            {t('drawer.cancel')}
          </Button>
        </Space>
      </Form>
    );
  };

  /** 渲染新增表单 */
  const renderAddForm = (
    onSave: (newItem: Partial<ChannelCredential>) => void,
    onCancel: () => void
  ) => {
    const handleSave = async () => {
      try {
        setLoading(true);
        const values = await form.validateFields();
        const data: CreateChannelCredentialRequest = {
          apiKey: values.apiKey,
          priority: values.priority,
          weight: values.weight,
          description: values.description,
        };
        const result = await createCredential.mutateAsync({ channelId, data });
        message.success(t('credential.addSuccess'));
        onSave({
          id: result.id,
          apiKeyPrefix: result.apiKeyPlain.substring(0, Math.min(10, result.apiKeyPlain.length)),
          apiKeyPlain: result.apiKeyPlain,
          name: '',
          description: values.description || null,
          weight: values.weight,
          priority: values.priority,
          state: 'ACTIVE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          channelId,
        });
      } catch (error) {
        message.error(t('credential.addFail'));
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item
          name="apiKey"
          label="API Key"
          rules={[{ required: true, message: t('credential.apiKeyRequired') }]}
        >
          <Input.Password style={{ width: 250 }} placeholder="sk-..." />
        </Form.Item>
        <Form.Item
          name="priority"
          label={t('credential.priority')}
          rules={[{ required: true, message: t('credential.priorityRequired') }]}
          initialValue={1}
        >
          <InputNumber min={1} max={10} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item
          name="weight"
          label={t('credential.weight')}
          rules={[{ required: true, message: t('credential.weightRequired') }]}
          initialValue={50}
        >
          <InputNumber min={1} max={100} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item name="description" label={t('credential.description')}>
          <Input style={{ width: 150 }} placeholder={t('credential.descriptionPlaceholder')} />
        </Form.Item>
        <Space>
          <Button type="primary" size="small" onClick={handleSave} loading={loading}>
            {t('drawer.save')}
          </Button>
          <Button size="small" onClick={onCancel}>
            {t('drawer.cancel')}
          </Button>
        </Space>
      </Form>
    );
  };

  /** 删除凭证 */
  const handleDelete = async (credential: ChannelCredential) => {
    try {
      await deleteCredential.mutateAsync({ channelId, id: credential.id });
      message.success(t('credential.deleteSuccess'));
    } catch (error) {
      message.error(t('credential.deleteFail'));
    }
  };

  return (
    <>
      <InlineEditableList
        items={credentials}
        renderItem={renderItem}
        renderEditForm={renderEditForm}
        renderAddForm={renderAddForm}
        onAdd={() => {
          form.resetFields();
        }}
        onDelete={handleDelete}
        getKey={(credential) => credential.id}
        addLabel={t('credential.addKey')}
      />

      {/* API Key 编辑弹窗 */}
      {editingId !== null && (
        <ApiKeyEditModal
          open={true}
          channelId={channelId}
          credentialId={editingId}
          keyPlain={credentials.find(c => c.id === editingId)?.apiKeyPlain || ''}
          onClose={() => setEditingId(null)}
          onSuccess={() => {
            message.success(t('credential.keyUpdated'));
          }}
          onUpdate={async (chId, credId, data) => {
            await updateCredential.mutateAsync({ channelId: chId, id: credId, data });
          }}
        />
      )}
    </>
  );
}
