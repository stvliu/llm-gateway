import { useState } from 'react';
import { Tag, Switch, Select, Input, Button, Space, Form, message, Popconfirm } from 'antd';
import type { ChannelEndpointResponse, CreateChannelEndpointRequest } from '@/types/channel';
import {
  useAddChannelEndpoint,
  useUpdateChannelEndpoint,
  useRemoveChannelEndpoint,
  useEnableChannelEndpoint,
  useDisableChannelEndpoint,
} from '@/services/query/useChannels';

interface EndpointSectionProps {
  channelId: number;
  endpoints: ChannelEndpointResponse[];
}

const PROTOCOL_OPTIONS = [
  { value: 'openai', label: 'OpenAI' },
  { value: 'anthropic', label: 'Anthropic' },
];

/**
 * 端点区组件
 * 展示渠道的端点列表，支持行内编辑、添加、启用/停用、删除
 */
export function EndpointSection({ channelId, endpoints }: EndpointSectionProps) {
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isAdding, setIsAdding] = useState(false);
  const [editForm] = Form.useForm();
  const [addForm] = Form.useForm();

  const addEndpoint = useAddChannelEndpoint();
  const updateEndpoint = useUpdateChannelEndpoint();
  const removeEndpoint = useRemoveChannelEndpoint();
  const enableEndpoint = useEnableChannelEndpoint();
  const disableEndpoint = useDisableChannelEndpoint();

  const getProtocolColor = (protocol: string) => {
    const lower = protocol.toLowerCase();
    if (lower === 'openai') return 'blue';
    if (lower === 'anthropic') return 'magenta';
    return 'default';
  };

  /** 开始编辑 */
  const handleStartEdit = (ep: ChannelEndpointResponse) => {
    editForm.setFieldsValue({ protocol: ep.protocol, endpointUrl: ep.endpointUrl });
    setEditingId(ep.id);
    setIsAdding(false);
  };

  /** 保存编辑 */
  const handleSaveEdit = async (endpointId: number) => {
    try {
      const values = await editForm.validateFields();
      const data: CreateChannelEndpointRequest = {
        protocol: values.protocol,
        endpointUrl: values.endpointUrl,
      };
      await updateEndpoint.mutateAsync({ channelId, endpointId, data });
      message.success('端点更新成功');
      setEditingId(null);
    } catch {
      // 校验失败或 API 错误
    }
  };

  /** 切换启用/停用 */
  const handleToggleState = async (ep: ChannelEndpointResponse, enabled: boolean) => {
    try {
      if (enabled) {
        await enableEndpoint.mutateAsync({ channelId, endpointId: ep.id });
      } else {
        await disableEndpoint.mutateAsync({ channelId, endpointId: ep.id });
      }
    } catch {
      message.error('状态切换失败');
    }
  };

  /** 删除端点 */
  const handleDelete = async (ep: ChannelEndpointResponse) => {
    try {
      await removeEndpoint.mutateAsync({ channelId, endpointId: ep.id });
      message.success('端点已删除');
    } catch {
      message.error('端点删除失败');
    }
  };

  /** 添加端点 */
  const handleAdd = async () => {
    try {
      const values = await addForm.validateFields();
      const data: CreateChannelEndpointRequest = {
        protocol: values.protocol,
        endpointUrl: values.endpointUrl,
      };
      await addEndpoint.mutateAsync({ channelId, data });
      message.success('端点添加成功');
      addForm.resetFields();
      setIsAdding(false);
    } catch {
      // 校验失败
    }
  };

  return (
    <div>
      {endpoints.map((ep) => (
        <div key={ep.id} style={{ marginBottom: 8 }}>
          {editingId === ep.id ? (
            /* 编辑模式 */
            <Form form={editForm} layout="inline" style={{ gap: 8 }}>
              <Form.Item name="protocol" rules={[{ required: true }]}>
                <Select style={{ width: 120 }} options={PROTOCOL_OPTIONS} />
              </Form.Item>
              <Form.Item
                name="endpointUrl"
                rules={[
                  { required: true, message: '请输入 URL' },
                  { type: 'url', message: '请输入有效 URL' },
                ]}
              >
                <Input style={{ width: 280 }} />
              </Form.Item>
              <Space>
                <Button
                  type="primary"
                  size="small"
                  onClick={() => handleSaveEdit(ep.id)}
                  loading={updateEndpoint.isPending}
                >
                  保存
                </Button>
                <Button size="small" onClick={() => setEditingId(null)}>
                  取消
                </Button>
              </Space>
            </Form>
          ) : (
            /* 展示模式 */
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '8px 12px',
                borderRadius: 4,
              }}
            >
              <Tag color={getProtocolColor(ep.protocol)}>
                {ep.protocol.toUpperCase()}
              </Tag>
              <span style={{ fontFamily: 'monospace', flex: 1, fontSize: 13 }}>
                {ep.endpointUrl}
              </span>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                <Switch
                  checked={ep.state === 'ACTIVE'}
                  onChange={(checked) => handleToggleState(ep, checked)}
                  checkedChildren="启用"
                  unCheckedChildren="停用"
                  loading={enableEndpoint.isPending || disableEndpoint.isPending}
                />
                <Button type="link" size="small" onClick={() => handleStartEdit(ep)}>
                  编辑
                </Button>
                <Popconfirm
                  title="确定删除此端点吗？"
                  onConfirm={() => handleDelete(ep)}
                  okText="删除"
                  cancelText="取消"
                  okButtonProps={{ danger: true }}
                >
                  <Button type="link" size="small" danger>
                    删除
                  </Button>
                </Popconfirm>
              </div>
            </div>
          )}
        </div>
      ))}

      {isAdding && (
        <Form form={addForm} layout="inline" style={{ gap: 8, marginTop: 8 }}>
          <Form.Item
            name="protocol"
            rules={[{ required: true, message: '请选择协议' }]}
            initialValue="openai"
          >
            <Select style={{ width: 120 }} options={PROTOCOL_OPTIONS} />
          </Form.Item>
          <Form.Item
            name="endpointUrl"
            rules={[
              { required: true, message: '请输入 URL' },
              { type: 'url', message: '请输入有效 URL' },
            ]}
          >
            <Input style={{ width: 280 }} placeholder="https://api.example.com/v1" />
          </Form.Item>
          <Space>
            <Button type="primary" size="small" onClick={handleAdd} loading={addEndpoint.isPending}>
              保存
            </Button>
            <Button size="small" onClick={() => { setIsAdding(false); addForm.resetFields(); }}>
              取消
            </Button>
          </Space>
        </Form>
      )}

      {!isAdding && editingId === null && (
        <Button type="dashed" block onClick={() => setIsAdding(true)} style={{ marginTop: 8 }}>
          + 添加端点
        </Button>
      )}
    </div>
  );
}