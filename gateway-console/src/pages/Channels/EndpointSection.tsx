import { useState, useEffect } from 'react';
import { Tag, Switch, Input, Select, Button, Space, Form, message } from 'antd';
import { InlineEditableList } from './InlineEditableList';
import type { ChannelEndpointResponse, CreateChannelEndpointRequest } from '@/types/channel';
import { useAddChannelEndpoint, useRemoveChannelEndpoint, useEnableChannelEndpoint, useDisableChannelEndpoint } from '@/services/query/useChannels';

interface EndpointSectionProps {
  channelId: number;
  endpoints: ChannelEndpointResponse[];
}

/**
 * 端点区组件
 * 展示渠道的端点列表，支持行内编辑
 */
export function EndpointSection({ channelId, endpoints }: EndpointSectionProps) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  // Mutations
  const addEndpoint = useAddChannelEndpoint();
  const removeEndpoint = useRemoveChannelEndpoint();
  const enableEndpoint = useEnableChannelEndpoint();
  const disableEndpoint = useDisableChannelEndpoint();

  /** 协议标签颜色 */
  const getProtocolColor = (protocol: string) => {
    const lower = protocol.toLowerCase();
    if (lower === 'openai') return 'blue';
    if (lower === 'anthropic') return 'magenta';
    return 'default';
  };

  /** 状态点颜色 */
  const getStateColor = (state: string) => {
    return state === 'ACTIVE' ? 'green' : 'default';
  };

  /** 编辑时同步表单值 */
  useEffect(() => {
    if (editingId !== null) {
      const endpoint = endpoints.find(e => e.id === editingId);
      if (endpoint) {
        form.setFieldsValue({
          protocol: endpoint.protocol,
          endpointUrl: endpoint.endpointUrl,
          state: endpoint.state,
        });
      }
    }
  }, [editingId, endpoints, form]);

  /** 渲染展示行 */
  const renderItem = (endpoint: ChannelEndpointResponse) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      {/* 协议标签 */}
      <Tag color={getProtocolColor(endpoint.protocol)}>
        {endpoint.protocol.toUpperCase()}
      </Tag>
      {/* URL */}
      <span style={{ fontFamily: 'monospace', flex: 1 }}>
        {endpoint.endpointUrl}
      </span>
      {/* 状态点 */}
      <Tag color={getStateColor(endpoint.state)}>
        {endpoint.state === 'ACTIVE' ? '已启用' : '已停用'}
      </Tag>
    </div>
  );

  /** 渲染编辑表单 */
  const renderEditForm = (
    endpoint: ChannelEndpointResponse,
    onSave: (updated: ChannelEndpointResponse) => void,
    onCancel: () => void
  ) => {
    const handleSave = async () => {
      try {
        setLoading(true);
        const values = await form.validateFields();

        // 调用启用/停用接口
        if (values.state !== endpoint.state) {
          if (values.state === 'ACTIVE') {
            await enableEndpoint.mutateAsync({ channelId, endpointId: endpoint.id });
          } else {
            await disableEndpoint.mutateAsync({ channelId, endpointId: endpoint.id });
          }
        }

        message.success('端点更新成功');
        onSave({ ...endpoint, ...values });
      } catch (error) {
        message.error('端点更新失败');
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item name="protocol" label="协议">
          <Select style={{ width: 120 }} disabled>
            <Select.Option value="openai">OpenAI</Select.Option>
            <Select.Option value="anthropic">Anthropic</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item name="endpointUrl" label="URL">
          <Input style={{ width: 300 }} disabled />
        </Form.Item>
        <Form.Item name="state" label="状态" valuePropName="checked">
          <Switch
            checkedChildren="启用"
            unCheckedChildren="停用"
            style={{ width: 60 }}
          />
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
    onSave: (newItem: Partial<ChannelEndpointResponse>) => void,
    onCancel: () => void
  ) => {
    const handleSave = async () => {
      try {
        setLoading(true);
        const values = await form.validateFields();
        const data: CreateChannelEndpointRequest = {
          protocol: values.protocol,
          endpointUrl: values.endpointUrl,
        };
        const result = await addEndpoint.mutateAsync({ channelId, data });
        message.success('端点添加成功');
        onSave(result);
      } catch (error) {
        message.error('端点添加失败');
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item
          name="protocol"
          label="协议"
          rules={[{ required: true, message: '请选择协议' }]}
          initialValue="openai"
        >
          <Select style={{ width: 120 }} placeholder="选择协议">
            <Select.Option value="openai">OpenAI</Select.Option>
            <Select.Option value="anthropic">Anthropic</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item
          name="endpointUrl"
          label="URL"
          rules={[
            { required: true, message: '请输入端点 URL' },
            { type: 'url', message: '请输入有效的 URL' },
          ]}
        >
          <Input style={{ width: 300 }} placeholder="https://api.example.com/v1" />
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

  /** 删除端点 */
  const handleDelete = async (endpoint: ChannelEndpointResponse) => {
    try {
      await removeEndpoint.mutateAsync({ channelId, endpointId: endpoint.id });
      message.success('端点删除成功');
    } catch (error) {
      message.error('端点删除失败');
    }
  };

  return (
    <InlineEditableList
      items={endpoints}
      renderItem={renderItem}
      renderEditForm={renderEditForm}
      renderAddForm={renderAddForm}
      onAdd={() => {
        form.resetFields();
      }}
      onDelete={handleDelete}
      getKey={(endpoint) => endpoint.id}
      addLabel="添加端点"
    />
  );
}