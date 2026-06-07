import { useState } from 'react';
import { Tag, Select, Input, Button, Space, Form, message, Popconfirm } from 'antd';
import { useTranslation } from 'react-i18next';
import type { ChannelEndpointResponse, CreateChannelEndpointRequest } from '@/types/channel';
import {
  useAddChannelEndpoint,
  useUpdateChannelEndpoint,
  useRemoveChannelEndpoint,
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
 * 展示渠道的端点列表，支持行内编辑、添加、删除
 */
export function EndpointSection({ channelId, endpoints }: EndpointSectionProps) {
  const { t } = useTranslation('channels');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isAdding, setIsAdding] = useState(false);
  const [editForm] = Form.useForm();
  const [addForm] = Form.useForm();

  const addEndpoint = useAddChannelEndpoint();
  const updateEndpoint = useUpdateChannelEndpoint();
  const removeEndpoint = useRemoveChannelEndpoint();

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
      message.success(t('drawer.endpointUpdated'));
      setEditingId(null);
    } catch {
      // 校验失败或 API 错误
    }
  };

  /** 删除端点 */
  const handleDelete = async (ep: ChannelEndpointResponse) => {
    try {
      await removeEndpoint.mutateAsync({ channelId, endpointId: ep.id });
      message.success(t('drawer.endpointDeleted'));
    } catch {
      message.error(t('drawer.endpointDeleteFailed'));
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
      message.success(t('drawer.endpointAdded'));
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
                  { required: true, message: t('drawer.endpointUrlRequired') },
                  { type: 'url', message: t('drawer.endpointUrlInvalid') },
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
                  {t('drawer.save')}
                </Button>
                <Button size="small" onClick={() => setEditingId(null)}>
                  {t('drawer.cancel')}
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
                <Button type="link" size="small" onClick={() => handleStartEdit(ep)}>
                  {t('drawer.edit')}
                </Button>
                <Popconfirm
                  title={t('drawer.confirmDeleteEndpoint')}
                  onConfirm={() => handleDelete(ep)}
                  okText={t('actions.delete', { ns: 'common' })}
                  cancelText={t('actions.cancel', { ns: 'common' })}
                  okButtonProps={{ danger: true }}
                >
                  <Button type="link" size="small" danger>
                    {t('drawer.delete')}
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
            rules={[{ required: true, message: t('drawer.protocolRequired') }]}
            initialValue="openai"
          >
            <Select style={{ width: 120 }} options={PROTOCOL_OPTIONS} />
          </Form.Item>
          <Form.Item
            name="endpointUrl"
            rules={[
              { required: true, message: t('drawer.endpointUrlRequired') },
              { type: 'url', message: t('drawer.endpointUrlInvalid') },
            ]}
          >
            <Input style={{ width: 280 }} placeholder="https://api.example.com/v1" />
          </Form.Item>
          <Space>
            <Button type="primary" size="small" onClick={handleAdd} loading={addEndpoint.isPending}>
              {t('drawer.save')}
            </Button>
            <Button size="small" onClick={() => { setIsAdding(false); addForm.resetFields(); }}>
              {t('drawer.cancel')}
            </Button>
          </Space>
        </Form>
      )}

      {!isAdding && editingId === null && (
        <Button type="dashed" block onClick={() => setIsAdding(true)} style={{ marginTop: 8 }}>
          {t('drawer.addEndpoint')}
        </Button>
      )}
    </div>
  );
}