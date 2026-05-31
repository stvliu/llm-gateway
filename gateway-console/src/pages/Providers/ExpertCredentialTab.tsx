import { useState, useEffect, useMemo } from 'react';
import {
  Typography,
  Empty,
  Table,
  Button,
  Tag,
  Space,
  Popconfirm,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  Result,
  App,
  Spin,
  Tooltip,
} from 'antd';
import type { ApiKeyTestResponse } from '@/types/channel';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ThunderboltOutlined,
  CopyOutlined,
  StopOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useChannels,
  useChannelCredentials,
  useCreateChannelCredential,
  useUpdateChannelCredential,
  useDeleteChannelCredential,
  useTestChannelCredential,
} from '@/services/query/useChannels';
import type {
  Channel,
  ChannelCredential,
  CreateChannelCredentialResponse,
  UpdateChannelCredentialRequest,
} from '@/types/channel';
import type { Provider } from '@/types/provider';

const { Text } = Typography;

interface Props {
  provider: Provider | null;
}

/** 状态 Tag 颜色映射 */
function stateTagColor(state: string): 'success' | 'default' {
  return state === 'ACTIVE' ? 'success' : 'default';
}

/**
 * 专家模式 - API Key 管理标签页
 * 管理供应商渠道下的 API Key 凭证，支持增删改查和连通性测试
 */
export default function ExpertCredentialTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  const { message, modal } = App.useApp();

  // ---- 数据查询 ----
  const { data: channels, isLoading: channelsLoading } = useChannels(provider?.id ?? 0);

  // 当前选中的渠道 ID
  const [selectedChannelId, setSelectedChannelId] = useState<number | null>(null);

  // 当渠道列表加载后，默认选中第一个渠道
  useEffect(() => {
    if (channels && channels.length > 0) {
      setSelectedChannelId((prev) => {
        // 如果之前选中的渠道仍在列表中，保持不变
        if (prev && channels.some((ch: Channel) => ch.id === prev)) return prev;
        return channels[0].id;
      });
    } else {
      setSelectedChannelId(null);
    }
  }, [channels]);

  const { data: credentials, isLoading: credentialsLoading } = useChannelCredentials(selectedChannelId ?? 0);

  // ---- 变更操作 ----
  const createMutation = useCreateChannelCredential();
  const updateMutation = useUpdateChannelCredential();
  const deleteMutation = useDeleteChannelCredential();
  const testMutation = useTestChannelCredential();

  // ---- 弹窗状态 ----
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingCredential, setEditingCredential] = useState<ChannelCredential | null>(null);
  const [createdResult, setCreatedResult] = useState<CreateChannelCredentialResponse | null>(null);
  const [resultModalOpen, setResultModalOpen] = useState(false);
  const [testingId, setTestingId] = useState<number | null>(null);

  // ---- 创建凭证 ----
  const [addForm] = Form.useForm();

  useEffect(() => {
    if (addModalOpen) {
      addForm.resetFields();
      // 默认选中当前渠道
      if (selectedChannelId) {
        addForm.setFieldValue('channelId', selectedChannelId);
      }
    }
  }, [addModalOpen, addForm, selectedChannelId]);

  const handleAdd = async () => {
    const values = await addForm.validateFields();
    try {
      const result = await createMutation.mutateAsync({
        channelId: values.channelId,
        data: {
          channelId: values.channelId,
          apiKey: values.apiKey,
          priority: values.priority,
          weight: values.weight,
          description: values.description,
        },
      });
      setCreatedResult(result);
      setAddModalOpen(false);
      setResultModalOpen(true);
      // 切换到新创建凭证所在的渠道
      setSelectedChannelId(values.channelId);
      message.success(t('credential.createdSuccess', { defaultValue: 'API Key 创建成功' }));
    } catch {
      message.error(t('credential.createFailed', { defaultValue: '创建失败' }));
    }
  };

  // ---- 编辑凭证 ----
  const [editForm] = Form.useForm();

  const handleOpenEdit = (cred: ChannelCredential) => {
    setEditingCredential(cred);
    editForm.setFieldsValue({
      priority: cred.priority,
      weight: cred.weight,
      description: cred.description,
    });
    setEditModalOpen(true);
  };

  const handleEdit = async () => {
    if (!editingCredential || !selectedChannelId) return;
    const values = await editForm.validateFields();
    try {
      const data: UpdateChannelCredentialRequest = {
        priority: values.priority,
        weight: values.weight,
        description: values.description,
      };
      await updateMutation.mutateAsync({
        channelId: selectedChannelId,
        id: editingCredential.id,
        data,
      });
      setEditModalOpen(false);
      setEditingCredential(null);
      message.success(t('credential.updateSuccess', { defaultValue: '凭证更新成功' }));
    } catch {
      message.error(t('credential.updateFailed', { defaultValue: '更新失败' }));
    }
  };

  // ---- 删除凭证 ----
  const handleDelete = async (cred: ChannelCredential) => {
    try {
      await deleteMutation.mutateAsync({ channelId: cred.channelId, id: cred.id });
      message.success(t('credential.deleteSuccess', { defaultValue: '凭证已删除' }));
    } catch {
      message.error(t('credential.deleteFailed', { defaultValue: '删除失败' }));
    }
  };

  // ---- 切换凭证状态 ----
  const handleToggleState = async (cred: ChannelCredential) => {
    const newState = cred.state === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await updateMutation.mutateAsync({
        channelId: cred.channelId,
        id: cred.id,
        data: { state: newState },
      });
      message.success(
        newState === 'ACTIVE'
          ? t('credential.enabled', { defaultValue: '凭证已启用' })
          : t('credential.disabled', { defaultValue: '凭证已停用' })
      );
    } catch {
      message.error(t('credential.updateFailed', { defaultValue: '状态切换失败' }));
    }
  };

  // ---- 测试凭证 ----
  const handleTest = async (cred: ChannelCredential) => {
    setTestingId(cred.id);
    try {
      const result = await testMutation.mutateAsync({ channelId: cred.channelId, id: cred.id }) as ApiKeyTestResponse;
      if (result.success) {
        modal.success({
          title: t('credential.testSuccess', { defaultValue: '连通性测试通过' }),
          content: (
            <div>
              <div>{t('credential.testLatency', { defaultValue: '延迟' })}: {result.latency ?? '-'}ms</div>
              {result.modelName && <div>{t('credential.testModel', { defaultValue: '模型' })}: {result.modelName}</div>}
              {result.responsePreview && (
                <div style={{ marginTop: 8 }}>
                  <Text type="secondary">{t('credential.testPreview', { defaultValue: '响应预览' })}:</Text>
                  <div style={{ background: '#f5f5f5', padding: 8, borderRadius: 4, marginTop: 4, fontSize: 12, wordBreak: 'break-all' }}>
                    {result.responsePreview}
                  </div>
                </div>
              )}
            </div>
          ),
        });
      } else {
        modal.error({
          title: t('credential.testFailed', { defaultValue: '连通性测试失败' }),
          content: result.error?.message ?? t('credential.testFailedUnknown', { defaultValue: '未知错误' }),
        });
      }
    } catch {
      message.error(t('credential.testFailed', { defaultValue: '测试请求失败' }));
    } finally {
      setTestingId(null);
    }
  };

  // ---- 复制明文 Key ----
  const handleCopyKey = async (keyPlain: string) => {
    try {
      await navigator.clipboard.writeText(keyPlain);
      message.success(t('credential.copied', { defaultValue: '已复制到剪贴板' }));
    } catch {
      message.error(t('credential.copyFailed', { defaultValue: '复制失败' }));
    }
  };

  // ---- 渠道下拉选项 ----
  const channelOptions = useMemo(
    () => (channels ?? []).map((ch: Channel) => ({ label: ch.name, value: ch.id })),
    [channels]
  );

  // ---- 表格列定义 ----
  const columns = [
    {
      title: t('credential.apiKeyPrefix', { defaultValue: 'Key 前缀' }),
      dataIndex: 'apiKeyPrefix',
      key: 'apiKeyPrefix',
      width: 180,
      render: (prefix: string) => (
        <Text code style={{ fontSize: 12 }}>{prefix}****</Text>
      ),
    },
    {
      title: t('credential.name', { defaultValue: '名称' }),
      dataIndex: 'name',
      key: 'name',
      width: 120,
      render: (name: string) => name || <Text type="secondary">-</Text>,
    },
    {
      title: t('channel.priority', { defaultValue: '优先级' }),
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      sorter: (a: ChannelCredential, b: ChannelCredential) => a.priority - b.priority,
    },
    {
      title: t('channel.weight', { defaultValue: '权重' }),
      dataIndex: 'weight',
      key: 'weight',
      width: 80,
      sorter: (a: ChannelCredential, b: ChannelCredential) => a.weight - b.weight,
    },
    {
      title: t('fields.status', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 90,
      render: (state: string) => (
        <Tag color={stateTagColor(state)}>
          {state === 'ACTIVE'
            ? t('state.active', { defaultValue: '活跃' })
            : t('state.inactive', { defaultValue: '停用' })}
        </Tag>
      ),
    },
    {
      title: t('fields.action', { defaultValue: '操作' }),
      key: 'action',
      width: 180,
      render: (_: unknown, record: ChannelCredential) => (
        <Space size="small">
          <Tooltip title={t('credential.test', { defaultValue: '测试' })}>
            <Button
              type="text"
              size="small"
              icon={<ThunderboltOutlined />}
              loading={testingId === record.id}
              onClick={() => handleTest(record)}
            />
          </Tooltip>
          <Tooltip
            title={
              record.state === 'ACTIVE'
                ? t('actions.disable', { ns: 'common', defaultValue: '停用' })
                : t('actions.enable', { ns: 'common', defaultValue: '启用' })
            }
          >
            <Button
              type="text"
              size="small"
              icon={record.state === 'ACTIVE' ? <StopOutlined /> : <CheckCircleOutlined />}
              onClick={() => handleToggleState(record)}
            />
          </Tooltip>
          <Tooltip title={t('actions.edit', { ns: 'common', defaultValue: '编辑' })}>
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleOpenEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title={t('credential.confirmDelete', { defaultValue: '确定删除此凭证？删除后不可恢复。' })}
            onConfirm={() => handleDelete(record)}
            okText={t('actions.confirm', { ns: 'common', defaultValue: '确定' })}
            cancelText={t('actions.cancel', { ns: 'common', defaultValue: '取消' })}
          >
            <Tooltip title={t('actions.delete', { ns: 'common', defaultValue: '删除' })}>
              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // ---- 空状态判断 ----
  if (!provider) {
    return <Empty description={t('noProviderData', { defaultValue: '暂无供应商数据' })} />;
  }

  if (channelsLoading) {
    return <Spin style={{ display: 'block', margin: '40px auto' }} />;
  }

  if (!channels || channels.length === 0) {
    return (
      <div>
        <Text strong style={{ fontSize: 16 }}>
          {t('credential.title', { defaultValue: 'API Key 管理' })}
        </Text>
        <div style={{ marginTop: 16 }}>
          <Empty description={t('credential.noChannels', { defaultValue: '暂无渠道，请先创建渠道' })} />
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* 标题栏 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Text strong style={{ fontSize: 16 }}>
            {t('credential.title', { defaultValue: 'API Key 管理' })}
          </Text>
          <div style={{ marginTop: 4, color: '#64748b', fontSize: 13 }}>
            {t('credential.desc', { defaultValue: '管理供应商 API Key，支持多 Key 负载均衡和轮换操作。' })}
          </div>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddModalOpen(true)}>
          {t('credential.add', { defaultValue: '添加 Key' })}
        </Button>
      </div>

      {/* 渠道选择器 */}
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
        <Text style={{ flexShrink: 0 }}>
          {t('credential.selectChannel', { defaultValue: '选择渠道' })}:
        </Text>
        <Select
          value={selectedChannelId ?? undefined}
          onChange={(val: number) => setSelectedChannelId(val)}
          options={channelOptions}
          style={{ minWidth: 200 }}
          placeholder={t('credential.selectChannelPlaceholder', { defaultValue: '请选择渠道' })}
        />
      </div>

      {/* 凭证列表 */}
      <Table
        dataSource={credentials ?? []}
        columns={columns}
        rowKey="id"
        loading={credentialsLoading}
        size="middle"
        pagination={false}
        locale={{
          emptyText: (
            <Empty
              description={t('credential.noCredentials', { defaultValue: '暂无 API Key，点击"添加 Key"创建' })}
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ),
        }}
      />

      {/* ---- 创建凭证弹窗 ---- */}
      <Modal
        title={t('credential.add', { defaultValue: '添加 Key' })}
        open={addModalOpen}
        onCancel={() => setAddModalOpen(false)}
        onOk={handleAdd}
        okText={t('actions.confirm', { ns: 'common', defaultValue: '确定' })}
        cancelText={t('actions.cancel', { ns: 'common', defaultValue: '取消' })}
        confirmLoading={createMutation.isPending}
        destroyOnClose
        width={520}
      >
        <Form form={addForm} layout="vertical">
          <Form.Item
            name="channelId"
            label={t('credential.channel', { defaultValue: '所属渠道' })}
            rules={[{ required: true, message: t('credential.channelRequired', { defaultValue: '请选择渠道' }) as string }]}
          >
            <Select
              options={channelOptions}
              placeholder={t('credential.selectChannelPlaceholder', { defaultValue: '请选择渠道' })}
            />
          </Form.Item>
          <Form.Item
            name="apiKey"
            label={t('credential.apiKey', { defaultValue: 'API Key' })}
            rules={[{ required: true, message: t('credential.apiKeyRequired', { defaultValue: '请输入 API Key' }) as string }]}
          >
            <Input.Password placeholder="sk-..." />
          </Form.Item>
          <Form.Item name="priority" label={t('channel.priority', { defaultValue: '优先级' })}>
            <InputNumber min={0} style={{ width: '100%' }} placeholder={t('credential.priorityPlaceholder', { defaultValue: '数值越大优先级越高' })} />
          </Form.Item>
          <Form.Item name="weight" label={t('channel.weight', { defaultValue: '权重' })}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('credential.weightPlaceholder', { defaultValue: '负载均衡权重' })} />
          </Form.Item>
          <Form.Item name="description" label={t('credential.description', { defaultValue: '描述' })}>
            <Input.TextArea rows={2} placeholder={t('credential.descriptionPlaceholder', { defaultValue: '可选备注信息' })} />
          </Form.Item>
        </Form>
      </Modal>

      {/* ---- 创建成功 - 明文 Key 展示弹窗 ---- */}
      <Modal
        title={t('credential.createdTitle', { defaultValue: 'API Key 创建成功' })}
        open={resultModalOpen}
        onCancel={() => setResultModalOpen(false)}
        footer={
          <Button type="primary" onClick={() => setResultModalOpen(false)}>
            {t('actions.close', { ns: 'common', defaultValue: '关闭' })}
          </Button>
        }
        width={560}
        destroyOnClose
      >
        {createdResult && (
          <Result
            status="success"
            title={t('credential.createdSuccess', { defaultValue: 'API Key 创建成功' })}
            subTitle={t('credential.createdHint', { defaultValue: '请立即复制此 Key，关闭后将无法再次查看！' })}
            extra={[
              <div
                key="key-display"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  background: '#1e293b',
                  color: '#e2e8f0',
                  padding: '12px 16px',
                  borderRadius: 8,
                  width: '100%',
                }}
              >
                <span
                  style={{
                    flex: 1,
                    fontFamily: 'monospace',
                    fontSize: 13,
                    wordBreak: 'break-all',
                  }}
                >
                  {createdResult.apiKeyPlain}
                </span>
                <Button
                  type="text"
                  icon={<CopyOutlined />}
                  style={{ color: '#e2e8f0', flexShrink: 0 }}
                  onClick={() => handleCopyKey(createdResult.apiKeyPlain)}
                />
              </div>,
            ]}
          />
        )}
      </Modal>

      {/* ---- 编辑凭证弹窗 ---- */}
      <Modal
        title={t('credential.edit', { defaultValue: '编辑凭证' })}
        open={editModalOpen}
        onCancel={() => {
          setEditModalOpen(false);
          setEditingCredential(null);
        }}
        onOk={handleEdit}
        okText={t('actions.confirm', { ns: 'common', defaultValue: '确定' })}
        cancelText={t('actions.cancel', { ns: 'common', defaultValue: '取消' })}
        confirmLoading={updateMutation.isPending}
        destroyOnClose
        width={480}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item name="priority" label={t('channel.priority', { defaultValue: '优先级' })}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="weight" label={t('channel.weight', { defaultValue: '权重' })}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label={t('credential.description', { defaultValue: '描述' })}>
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
