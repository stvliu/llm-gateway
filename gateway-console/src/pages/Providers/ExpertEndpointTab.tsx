import { useState, useCallback, useMemo } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Select,
  Input,
  Tag,
  Popconfirm,
  Space,
  Typography,
  Empty,
  App,
} from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  StopOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useChannels,
  useAddChannelEndpoint,
  useRemoveChannelEndpoint,
  useEnableChannelEndpoint,
  useDisableChannelEndpoint,
} from '@/services/query/useChannels';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import type { Provider } from '@/types/provider';
import type { ChannelEndpointResponse } from '@/types/channel';

const { Text } = Typography;

/** 状态 Tag 颜色映射 */
function stateColor(state: string): 'success' | 'default' {
  return state === 'ACTIVE' ? 'success' : 'default';
}

/** 协议 Tag 颜色映射 */
function protocolColor(protocol: string): string {
  if (protocol === 'openai') return 'green';
  if (protocol === 'anthropic') return 'purple';
  return 'blue';
}

interface Props {
  provider: Provider | null;
}

/** 展平后的端点行数据（携带渠道名称用于展示） */
interface FlatEndpointRow extends ChannelEndpointResponse {
  channelName: string;
  channelId: number;
}

/**
 * 专家模式 - 接入点管理 Tab
 * 汇总展示供应商下所有渠道的端点列表，支持增删启停操作
 */
export default function ExpertEndpointTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  // 数据查询
  const { data: channels, isLoading } = useChannels(provider?.id ?? 0);

  // 变更操作
  const addEndpointMutation = useAddChannelEndpoint();
  const removeEndpointMutation = useRemoveChannelEndpoint();
  const enableEndpointMutation = useEnableChannelEndpoint();
  const disableEndpointMutation = useDisableChannelEndpoint();

  // 弹窗状态
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [form] = Form.useForm();

  // 将所有渠道的端点展平为一维列表
  const flatEndpoints: FlatEndpointRow[] = useMemo(() => {
    if (!channels) return [];
    return channels.flatMap((ch) =>
      (ch.endpoints ?? []).map((ep) => ({
        ...ep,
        channelName: ch.name,
        channelId: ch.id,
      })),
    );
  }, [channels]);

  // 渠道选项（用于新增端点弹窗中的渠道选择）
  const channelOptions = useMemo(
    () => (channels ?? []).map((ch) => ({ value: ch.id, label: ch.name })),
    [channels],
  );

  // --- 新增端点 ---

  const handleOpenAddModal = useCallback(() => {
    form.resetFields();
    // 如果只有一个渠道，自动选中
    if (channels?.length === 1) {
      form.setFieldValue('channelId', channels[0].id);
    }
    setAddModalOpen(true);
  }, [form, channels]);

  const handleAddEndpoint = useCallback(async () => {
    const values = await form.validateFields();
    const { channelId, protocol, endpointUrl } = values;
    await addEndpointMutation.mutateAsync({
      channelId,
      data: { protocol, endpointUrl },
    });
    message.success(
      t('endpoint.addSuccess', { defaultValue: '接入点添加成功' }),
    );
    setAddModalOpen(false);
  }, [form, addEndpointMutation, message, t]);

  // --- 删除端点 ---

  const handleDeleteEndpoint = useCallback(
    async (channelId: number, endpointId: number) => {
      await removeEndpointMutation.mutateAsync({ channelId, endpointId });
      message.success(
        t('endpoint.deleteSuccess', { defaultValue: '接入点已删除' }),
      );
    },
    [removeEndpointMutation, message, t],
  );

  // --- 启用/停用端点 ---

  const handleToggleEndpoint = useCallback(
    async (channelId: number, endpoint: ChannelEndpointResponse) => {
      if (endpoint.state === 'ACTIVE') {
        await disableEndpointMutation.mutateAsync({
          channelId,
          endpointId: endpoint.id,
        });
        message.success(
          t('endpoint.disabled', { defaultValue: '接入点已停用' }),
        );
      } else {
        await enableEndpointMutation.mutateAsync({
          channelId,
          endpointId: endpoint.id,
        });
        message.success(
          t('endpoint.enabled', { defaultValue: '接入点已启用' }),
        );
      }
    },
    [enableEndpointMutation, disableEndpointMutation, message, t],
  );

  // --- 表格列定义 ---

  const columns = [
    {
      title: t('endpoint.channelName', { defaultValue: '渠道' }),
      dataIndex: 'channelName',
      key: 'channelName',
      width: 140,
      render: (name: string) => <Text strong>{name}</Text>,
    },
    {
      title: t('channel.protocol', { defaultValue: '协议' }),
      dataIndex: 'protocol',
      key: 'protocol',
      width: 110,
      render: (protocol: string) => (
        <Tag color={protocolColor(protocol)}>{protocol.toUpperCase()}</Tag>
      ),
    },
    {
      title: t('channel.endpointUrl', { defaultValue: 'Endpoint URL' }),
      dataIndex: 'endpointUrl',
      key: 'endpointUrl',
      render: (url: string) => (
        <Text copyable={{ text: url }} style={{ maxWidth: 400 }} ellipsis>
          {url}
        </Text>
      ),
    },
    {
      title: t('fields.status', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 90,
      render: (state: string) => (
        <Tag color={stateColor(state)}>
          {state === 'ACTIVE'
            ? t('state.active', { defaultValue: '启用' })
            : t('state.inactive', { defaultValue: '停用' })}
        </Tag>
      ),
    },
    ...(canWrite
      ? [
          {
            title: t('fields.action', { defaultValue: '操作' }),
            key: 'action',
            width: 120,
            render: (_: unknown, record: FlatEndpointRow) => (
              <Space size="small">
                <Button
                  type="text"
                  size="small"
                  icon={
                    record.state === 'ACTIVE' ? (
                      <StopOutlined />
                    ) : (
                      <CheckCircleOutlined />
                    )
                  }
                  onClick={() => handleToggleEndpoint(record.channelId, record)}
                />
                <Popconfirm
                  title={t(
                    'endpoint.confirmDelete',
                    { defaultValue: '确定删除该接入点？' },
                  )}
                  onConfirm={() =>
                    handleDeleteEndpoint(record.channelId, record.id)
                  }
                  okText={t('actions.confirm', { ns: 'common' })}
                  cancelText={t('actions.cancel', { ns: 'common' })}
                >
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                  />
                </Popconfirm>
              </Space>
            ),
          },
        ]
      : []),
  ];

  if (!provider) {
    return (
      <Empty
        description={t('noProviderData', { defaultValue: '暂无供应商数据' })}
      />
    );
  }

  return (
    <div>
      {/* 标题区域 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <div>
          <Text strong style={{ fontSize: 16 }}>
            {t('endpoint.title', { defaultValue: '接入点管理' })}
          </Text>
          <div style={{ marginTop: 4, color: '#64748b', fontSize: 13 }}>
            {t('endpoint.desc', {
              defaultValue: '管理协议类型和 Base URL 等接入点信息。',
            })}
          </div>
        </div>
        {canWrite && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleOpenAddModal}
            disabled={!channels || channels.length === 0}
          >
            {t('endpoint.add', { defaultValue: '添加接入点' })}
          </Button>
        )}
      </div>

      {/* 端点列表 */}
      <Table
        dataSource={flatEndpoints}
        loading={isLoading}
        rowKey="id"
        pagination={false}
        columns={columns}
        locale={{
          emptyText: (
            <Empty
              description={t('endpoint.empty', {
                defaultValue: '暂无接入点，请先创建渠道并添加接入点',
              })}
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ),
        }}
      />

      {/* 新增接入点弹窗 */}
      <Modal
        title={t('endpoint.addTitle', { defaultValue: '添加接入点' })}
        open={addModalOpen}
        onOk={handleAddEndpoint}
        onCancel={() => setAddModalOpen(false)}
        confirmLoading={addEndpointMutation.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="channelId"
            label={t('endpoint.channelName', { defaultValue: '渠道' })}
            rules={[
              {
                required: true,
                message: t('endpoint.channelRequired', {
                  defaultValue: '请选择渠道',
                }) as string,
              },
            ]}
          >
            <Select
              options={channelOptions}
              placeholder={t('endpoint.selectChannel', {
                defaultValue: '请选择渠道',
              })}
            />
          </Form.Item>
          <Form.Item
            name="protocol"
            label={t('channel.protocol', { defaultValue: '协议' })}
            rules={[
              {
                required: true,
                message: t('channel.protocolRequired', {
                  defaultValue: '请选择协议',
                }) as string,
              },
            ]}
          >
            <Select
              options={[
                { value: 'openai', label: 'OpenAI' },
                { value: 'anthropic', label: 'Anthropic' },
              ]}
              placeholder={t('endpoint.selectProtocol', {
                defaultValue: '请选择协议',
              })}
            />
          </Form.Item>
          <Form.Item
            name="endpointUrl"
            label={t('channel.endpointUrl', { defaultValue: 'Endpoint URL' })}
            rules={[
              {
                required: true,
                message: t('channel.endpointUrlRequired', {
                  defaultValue: '请输入 Endpoint URL',
                }) as string,
              },
              {
                type: 'url',
                message: t('endpoint.urlInvalid', {
                  defaultValue: '请输入有效的 URL',
                }) as string,
              },
            ]}
          >
            <Input placeholder="https://api.openai.com/v1" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
