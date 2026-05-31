import { useState, useEffect } from 'react';
import { Tag, Input, InputNumber, Button, Space, Form, message, Popconfirm } from 'antd';
import { InlineEditableList } from './InlineEditableList';
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

  /** 格式化 API Key 前缀显示 */
  const formatApiKeyPrefix = (prefix: string) => {
    return `${prefix}****`;
  };

  /** 渲染展示行 */
  const renderItem = (credential: ChannelCredential) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
      {/* Key 前缀 */}
      <span style={{ fontFamily: 'monospace', minWidth: 150 }}>
        {formatApiKeyPrefix(credential.apiKeyPrefix)}
      </span>
      {/* 优先级/权重 */}
      <Tag color="blue">P{credential.priority}</Tag>
      <Tag color="purple">W{credential.weight}</Tag>
      {/* 最后使用时间（暂无字段，显示占位） */}
      <span style={{ color: '#999', fontSize: 12 }}>
        最后使用: 暂无数据
      </span>
      {/* 状态点 */}
      <Tag color={getStateColor(credential.state)}>
        {credential.state === 'ACTIVE' ? '已启用' : '已停用'}
      </Tag>
      {/* 测试按钮 */}
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
              message.success(`测试成功，延迟: ${result.latency}ms`);
            } else {
              message.error(`测试失败: ${result.error?.message || '未知错误'}`);
            }
          } catch (error) {
            message.error('测试请求失败');
          } finally {
            setTestingId(null);
          }
        }}
      >
        测试
      </Button>
    </div>
  );

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
        message.success('凭证更新成功');
        onSave(result);
      } catch (error) {
        message.error('凭证更新失败');
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item
          name="priority"
          label="优先级"
          rules={[{ required: true, message: '请输入优先级' }]}
        >
          <InputNumber min={1} max={10} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item
          name="weight"
          label="权重"
          rules={[{ required: true, message: '请输入权重' }]}
        >
          <InputNumber min={1} max={100} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input style={{ width: 200 }} placeholder="凭证描述" />
        </Form.Item>
        <Space>
          <Button type="primary" size="small" onClick={handleSave} loading={loading}>
            保存
          </Button>
          <Button size="small" onClick={onCancel}>
            取消
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
          channelId,
          apiKey: values.apiKey,
          priority: values.priority,
          weight: values.weight,
          description: values.description,
        };
        const result = await createCredential.mutateAsync({ channelId, data });
        message.success('凭证添加成功');
        onSave({
          id: result.id,
          apiKeyPrefix: result.apiKeyMasked.substring(0, 10),
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
        message.error('凭证添加失败');
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item
          name="apiKey"
          label="API Key"
          rules={[{ required: true, message: '请输入 API Key' }]}
        >
          <Input.Password style={{ width: 250 }} placeholder="sk-..." />
        </Form.Item>
        <Form.Item
          name="priority"
          label="优先级"
          rules={[{ required: true, message: '请输入优先级' }]}
          initialValue={1}
        >
          <InputNumber min={1} max={10} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item
          name="weight"
          label="权重"
          rules={[{ required: true, message: '请输入权重' }]}
          initialValue={50}
        >
          <InputNumber min={1} max={100} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input style={{ width: 150 }} placeholder="凭证描述" />
        </Form.Item>
        <Space>
          <Button type="primary" size="small" onClick={handleSave} loading={loading}>
            保存
          </Button>
          <Button size="small" onClick={onCancel}>
            取消
          </Button>
        </Space>
      </Form>
    );
  };

  /** 删除凭证 */
  const handleDelete = async (credential: ChannelCredential) => {
    try {
      await deleteCredential.mutateAsync({ channelId, id: credential.id });
      message.success('凭证删除成功');
    } catch (error) {
      message.error('凭证删除失败');
    }
  };

  return (
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
      addLabel="添加 API Key"
    />
  );
}