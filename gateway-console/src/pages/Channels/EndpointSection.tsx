import { useState } from 'react';
import { Tag, Select, Input, Button, Space, Form, message, Tooltip } from 'antd';
import { EditOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import type { ChannelEndpointResponse, CreateChannelEndpointRequest } from '@/types/channel';
import {
  useAddChannelEndpoint,
  useUpdateChannelEndpoint,
  useRemoveChannelEndpoint,
  channelKeys,
} from '@/services/query/useChannels';
import { extractErrorMessage } from '@/utils/errorMessage';
import { useSavePulse } from '@/components/common/useSavePulse';
import { useDangerConfirm } from '@/components/common/useDangerConfirm';
import '@/components/common/SavePulse.css';

interface EndpointSectionProps {
  channelId: number;
  endpoints: ChannelEndpointResponse[];
}

const PROTOCOL_OPTIONS = [
  { value: 'openai', label: 'OpenAI' },
  { value: 'anthropic', label: 'Anthropic' },
];

/**
 * 单个端点行的展示 / 编辑容器组件。
 * 拆出独立子组件，使 useSavePulse Hook 与每行 endpoint 1:1 绑定，互不污染。
 */
function EndpointRow({
  channelId,
  endpoint,
  isEditing,
  onStartEdit,
  onCancelEdit,
  onSaveEdit,
  onDelete,
  editForm,
  saving,
}: {
  channelId: number;
  endpoint: ChannelEndpointResponse;
  isEditing: boolean;
  onStartEdit: () => void;
  onCancelEdit: () => void;
  onSaveEdit: (
    endpointId: number,
    pulse: ReturnType<typeof useSavePulse>
  ) => Promise<void> | void;
  onDelete: (ep: ChannelEndpointResponse) => Promise<void> | void;
  editForm: ReturnType<typeof Form.useForm>[0];
  saving: boolean;
}) {
  const { t } = useTranslation('channels');
  const pulse = useSavePulse();

  /** 端点协议色 */
  const getProtocolColor = (protocol: string) => {
    const lower = protocol.toLowerCase();
    if (lower === 'openai') return 'blue';
    if (lower === 'anthropic') return 'magenta';
    return 'default';
  };

  // 编辑模式：表单容器同样附 className，让用户在保存失败后立即看到红框
  if (isEditing) {
    return (
      <div className={pulse.className} style={{ padding: 4, borderRadius: 4 }}>
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
              onClick={() => onSaveEdit(endpoint.id, pulse)}
              loading={saving}
            >
              {t('drawer.save')}
            </Button>
            <Button size="small" onClick={onCancelEdit}>
              {t('drawer.cancel')}
            </Button>
          </Space>
          {pulse.state === 'success' && (
            <span className="save-tip-ok">✓ {t('common:message.saved', { defaultValue: '已保存' })}</span>
          )}
          {pulse.state === 'error' && (
            <span className="save-tip-err">✗ {pulse.errorMsg}</span>
          )}
        </Form>
      </div>
    );
  }

  return (
    <div
      className={pulse.className}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '8px 12px',
        borderRadius: 4,
      }}
      data-testid={`endpoint-row-${endpoint.id}`}
      data-channel-id={channelId}
    >
      <Tag color={getProtocolColor(endpoint.protocol)}>
        {endpoint.protocol.toUpperCase()}
      </Tag>
      <span style={{ fontFamily: 'monospace', flex: 1, fontSize: 13 }}>
        {endpoint.endpointUrl}
      </span>
      {pulse.state === 'success' && (
        <span className="save-tip-ok">✓ {t('common:message.saved', { defaultValue: '已保存' })}</span>
      )}
      {pulse.state === 'error' && (
        <span className="save-tip-err">✗ {pulse.errorMsg}</span>
      )}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
        <Tooltip title={t('drawer.edit')}>
          <Button type="text" size="small" icon={<EditOutlined />} onClick={onStartEdit} />
        </Tooltip>
        <Tooltip title={t('drawer.delete')}>
          <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={() => onDelete(endpoint)} />
        </Tooltip>
      </div>
    </div>
  );
}

/**
 * 端点区组件
 * 展示渠道的端点列表，支持行内编辑、添加、删除。
 * 编辑保存采用乐观更新 + 失败回滚 + 行内脉冲反馈策略：
 * - onMutate：备份 detail 缓存并提前渲染新值
 * - onSuccess：触发 success 脉冲
 * - onError：回滚缓存 + 触发 error 脉冲（toast 仍由调用方保留）
 */
export function EndpointSection({ channelId, endpoints }: EndpointSectionProps) {
  const { t } = useTranslation('channels');
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isAdding, setIsAdding] = useState(false);
  const [editForm] = Form.useForm();
  const [addForm] = Form.useForm();

  // 危险删除确认（任务 8.5）：注意必须把 contextHolder 渲染到组件树
  const { confirm: confirmDelete, contextHolder: dangerContextHolder } = useDangerConfirm();

  const addEndpoint = useAddChannelEndpoint();
  const updateEndpoint = useUpdateChannelEndpoint();
  const removeEndpoint = useRemoveChannelEndpoint();

  /** 开始编辑 */
  const handleStartEdit = (ep: ChannelEndpointResponse) => {
    editForm.setFieldsValue({ protocol: ep.protocol, endpointUrl: ep.endpointUrl });
    setEditingId(ep.id);
    setIsAdding(false);
  };

  /**
   * 保存编辑：执行表单校验 + 乐观更新 + mutation。
   * 失败时通过 pulse.triggerError 给行内反馈，同时保留 message.error 作为全局提示。
   */
  const handleSaveEdit = async (
    endpointId: number,
    pulse: ReturnType<typeof useSavePulse>
  ) => {
    let values: { protocol: string; endpointUrl: string };
    try {
      values = await editForm.validateFields();
    } catch (err) {
      // 表单校验失败：AntD 已在表单内行内展示
      const reason = extractErrorMessage(err);
      if (reason) {
        message.error(t('common:message.saveFailed', { reason }));
      }
      return;
    }
    const data: CreateChannelEndpointRequest = {
      protocol: values.protocol,
      endpointUrl: values.endpointUrl,
    };

    // 乐观更新：备份 detail 缓存中的 channel.endpoints，并把对应行替换为新值
    const detailKey = channelKeys.detail(channelId);
    await queryClient.cancelQueries({ queryKey: detailKey });
    const prev = queryClient.getQueryData<{ endpoints?: ChannelEndpointResponse[] }>(detailKey);
    if (prev?.endpoints) {
      const optimistic = {
        ...prev,
        endpoints: prev.endpoints.map((ep) =>
          ep.id === endpointId ? { ...ep, ...data } : ep
        ),
      };
      queryClient.setQueryData(detailKey, optimistic);
    }

    try {
      await updateEndpoint.mutateAsync({ channelId, endpointId, data });
      pulse.triggerSuccess();
      message.success(t('drawer.endpointUpdated'));
      setEditingId(null);
    } catch (err) {
      // 失败：回滚缓存 + 触发 error 脉冲 + 保留全局 toast
      if (prev !== undefined) {
        queryClient.setQueryData(detailKey, prev);
      }
      const reason = extractErrorMessage(err);
      pulse.triggerError(reason || t('common:message.saveFailed', { reason: '' }));
      if (reason) {
        message.error(t('common:message.saveFailed', { reason }));
      }
      // 失败后退出编辑态，让父组件 props 渲染回滚后的展示态
      setEditingId(null);
    } finally {
      // 无论成功失败都重新拉取最新数据
      queryClient.invalidateQueries({ queryKey: detailKey });
    }
  };

  /**
   * 删除端点：弹危险确认 Modal（任务 8.5）。
   * description 含 baseUrl + "删除该端点后，路由到 baseUrl=… 的流量将立即失败"。
   */
  const handleDelete = (ep: ChannelEndpointResponse) => {
    confirmDelete({
      titleKey: 'endpoint.deleteTitle',
      descriptionKey: 'endpoint.deleteDescription',
      descriptionParams: { baseUrl: ep.endpointUrl },
      onOk: async () => {
        try {
          await removeEndpoint.mutateAsync({ channelId, endpointId: ep.id });
          message.success(t('drawer.endpointDeleted'));
        } catch (err) {
          // 兜底 toast：未挂 useSavePulse onError，由本处反馈
          const reason = extractErrorMessage(err);
          message.error(
            reason
              ? t('common:message.saveFailed', { reason })
              : t('drawer.endpointDeleteFailed')
          );
          // 必须 throw，让 useDangerConfirm 阻止 modal 关闭
          throw err;
        }
      },
    });
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
    } catch (err) {
      // 同上：校验失败静默（行内已显示），其它错误必须给用户反馈
      const reason = extractErrorMessage(err);
      if (reason) {
        message.error(t('common:message.saveFailed', { reason }));
      }
    }
  };

  return (
    <div>
      {/* useDangerConfirm 的 contextHolder 必须挂载到组件树，否则 modal 不出现 */}
      {dangerContextHolder}
      {endpoints.map((ep) => (
        <div key={ep.id} style={{ marginBottom: 8 }}>
          <EndpointRow
            channelId={channelId}
            endpoint={ep}
            isEditing={editingId === ep.id}
            onStartEdit={() => handleStartEdit(ep)}
            onCancelEdit={() => setEditingId(null)}
            onSaveEdit={handleSaveEdit}
            onDelete={handleDelete}
            editForm={editForm}
            saving={updateEndpoint.isPending}
          />
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
        <Button type="dashed" block icon={<PlusOutlined />} onClick={() => setIsAdding(true)} style={{ marginTop: 8 }}>
          {t('drawer.addEndpoint')}
        </Button>
      )}
    </div>
  );
}
